package dev.mrbean.laundryledger.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small key-value settings, exposed as StateFlows so the UI reacts to changes. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    private val _onboarded = MutableStateFlow(sp.getBoolean(K_ONBOARDED, false))
    val onboarded: StateFlow<Boolean> = _onboarded.asStateFlow()

    private val _gender = MutableStateFlow(
        Gender.entries.firstOrNull { it.name == sp.getString(K_GENDER, null) } ?: Gender.UNSPECIFIED,
    )
    val gender: StateFlow<Gender> = _gender.asStateFlow()

    private val _enabled = MutableStateFlow(readEnabled())
    /** Cloth types shown in the add panel, in display order. */
    val enabled: StateFlow<List<ClothType>> = _enabled.asStateFlow()

    private val _autoUpdate = MutableStateFlow(sp.getBoolean(K_AUTO_UPDATE, true))
    val autoUpdate: StateFlow<Boolean> = _autoUpdate.asStateFlow()

    private fun readEnabled(): List<ClothType> {
        val saved = sp.getStringSet(K_ENABLED, null)
        val set = saved?.mapNotNull { n -> ClothType.entries.firstOrNull { it.name == n } }?.toSet()
            ?: ClothType.defaults(Gender.MALE) // users of v1.0 had exactly the men's list
        return ClothType.entries.filter { it in set }
    }

    fun completeOnboarding(gender: Gender, types: Set<ClothType>) {
        setGender(gender)
        setEnabled(types)
        sp.edit().putBoolean(K_ONBOARDED, true).apply()
        _onboarded.value = true
    }

    fun setGender(g: Gender) {
        sp.edit().putString(K_GENDER, g.name).apply()
        _gender.value = g
    }

    fun setEnabled(types: Set<ClothType>) {
        sp.edit().putStringSet(K_ENABLED, types.map { it.name }.toSet()).apply()
        _enabled.value = ClothType.entries.filter { it in types }
    }

    fun setAutoUpdate(on: Boolean) {
        sp.edit().putBoolean(K_AUTO_UPDATE, on).apply()
        _autoUpdate.value = on
    }

    var lastType: ClothType
        get() = ClothType.of(sp.getString("last_type", ClothType.SHIRT.name)!!)
        set(v) = sp.edit().putString("last_type", v.name).apply()


    var lastUpdateCheck: Long
        get() = sp.getLong("last_update_check", 0L)
        set(v) = sp.edit().putLong("last_update_check", v).apply()

    /** Version the user dismissed the update banner for. */
    var dismissedVersion: String?
        get() = sp.getString("dismissed_version", null)
        set(v) = sp.edit().putString("dismissed_version", v).apply()

    private companion object {
        const val K_ONBOARDED = "onboarded"
        const val K_GENDER = "gender"
        const val K_ENABLED = "enabled_types"
        const val K_AUTO_UPDATE = "auto_update"
    }
}
