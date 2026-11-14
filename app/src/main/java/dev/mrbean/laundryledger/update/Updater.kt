package dev.mrbean.laundryledger.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dev.mrbean.laundryledger.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Release(val version: String, val notes: String, val apkUrl: String, val apkSize: Long, val pageUrl: String)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class Installing(val release: Release) : UpdateState
    data class Failed(val message: String, val release: Release? = null) : UpdateState
}

/**
 * The only networking in the app: reads the latest GitHub release of this repo and,
 * when the user taps Update, downloads its APK and hands it to the system installer.
 * No user data is ever sent anywhere.
 */
class Updater(private val context: Context) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    suspend fun check(): UpdateState {
        val cur = _state.value
        if (cur is UpdateState.Checking || cur is UpdateState.Downloading || cur is UpdateState.Installing) return cur
        _state.value = UpdateState.Checking
        val result = runCatching { fetchLatest() }.fold(
            onSuccess = { r ->
                if (r != null && isNewer(r.version, currentVersion)) UpdateState.Available(r) else UpdateState.UpToDate
            },
            onFailure = { UpdateState.Failed("Couldn't reach GitHub. Check your internet and try again.") },
        )
        _state.value = result
        return result
    }

    private suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        val conn = (URL(LATEST_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "LaundryLedger/$currentVersion")
        }
        try {
            if (conn.responseCode == 404) return@withContext null
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val assets = json.getJSONArray("assets")
            var apk: JSONObject? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk")) { apk = a; break }
            }
            apk ?: return@withContext null
            Release(
                version = json.getString("tag_name").removePrefix("v"),
                notes = json.optString("body").trim(),
                apkUrl = apk.getString("browser_download_url"),
                apkSize = apk.optLong("size"),
                pageUrl = json.optString("html_url"),
            )
        } finally {
            conn.disconnect()
        }
    }

    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /** Downloads the APK and starts a PackageInstaller session (system shows the confirm dialog). */
    suspend fun downloadAndInstall(release: Release) {
        _state.value = UpdateState.Downloading(release, 0f)
        try {
            val file = withContext(Dispatchers.IO) { download(release) }
            _state.value = UpdateState.Installing(release)
            withContext(Dispatchers.IO) { install(file) }
        } catch (e: Exception) {
            _state.value = UpdateState.Failed("Update failed: ${e.message ?: "download error"}", release)
        }
    }

    private fun download(release: Release): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "LaundryLedger-${release.version}.apk")
        var url = URL(release.apkUrl)
        var conn: HttpURLConnection
        // Follow redirects manually (GitHub -> objects.githubusercontent.com).
        var hops = 0
        while (true) {
            conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "LaundryLedger/$currentVersion")
            }
            val code = conn.responseCode
            if (code in 300..399 && hops++ < 5) {
                url = URL(url, conn.getHeaderField("Location")); conn.disconnect(); continue
            }
            if (code !in 200..299) error("HTTP $code")
            break
        }
        val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
        conn.inputStream.use { input ->
            out.outputStream().use { output ->
                val buf = ByteArray(64 * 1024)
                var read = 0L
                var lastEmit = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    output.write(buf, 0, n)
                    read += n
                    if (total > 0 && read - lastEmit > 32 * 1024) {
                        lastEmit = read
                        _state.value = UpdateState.Downloading(release, (read.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
        conn.disconnect()
        return out
    }

    private fun install(file: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            file.inputStream().use { input ->
                session.openWrite("base.apk", 0, file.length()).use { out ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }
            val intent = Intent(context, InstallReceiver::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val pi = PendingIntent.getBroadcast(context, id, intent, flags)
            session.commit(pi.intentSender)
        }
    }

    internal fun onInstallResult(status: Int, message: String?) {
        val release = (_state.value as? UpdateState.Installing)?.release
        if (status != PackageInstaller.STATUS_SUCCESS) {
            _state.value = if (status == PackageInstaller.STATUS_FAILURE_ABORTED) {
                release?.let { UpdateState.Available(it) } ?: UpdateState.Idle
            } else {
                UpdateState.Failed("Install failed${message?.let { ": $it" } ?: ""}", release)
            }
        }
    }

    companion object {
        const val REPO = "mrbeandev/LaundryLedger"
        const val REPO_URL = "https://github.com/$REPO"
        private const val LATEST_API = "https://api.github.com/repos/$REPO/releases/latest"

        /** Numeric dotted-version compare: "1.10.0" > "1.9". */
        fun isNewer(remote: String, local: String): Boolean {
            fun parts(v: String) = v.trim().removePrefix("v").split('.', '-').map { it.toIntOrNull() ?: 0 }
            val a = parts(remote); val b = parts(local)
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
            confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(confirm)
            return
        }
        val app = context.applicationContext as dev.mrbean.laundryledger.LaundryApp
        app.updater.onInstallResult(status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
    }
}
