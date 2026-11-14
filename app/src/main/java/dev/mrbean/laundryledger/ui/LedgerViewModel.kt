package dev.mrbean.laundryledger.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.mrbean.laundryledger.LaundryApp
import dev.mrbean.laundryledger.data.Batch
import dev.mrbean.laundryledger.data.BatchItem
import dev.mrbean.laundryledger.data.BatchWithItems
import dev.mrbean.laundryledger.data.ClothType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LedgerViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as LaundryApp).db.dao()
    val prefs = (app as LaundryApp).prefs
    val updater = (app as LaundryApp).updater

    fun changeGender(g: dev.mrbean.laundryledger.data.Gender) {
        if (g == prefs.gender.value) return
        prefs.setGender(g)
        prefs.setEnabled(prefs.enabled.value.toSet() + ClothType.defaults(g))
    }

    /** Auto-check at most once a day unless [manual]. Never throws. */
    suspend fun checkForUpdate(manual: Boolean = false) {
        if (!manual) {
            if (!prefs.autoUpdate.value) return
            if (System.currentTimeMillis() - prefs.lastUpdateCheck < 20 * 60 * 60 * 1000L) return
        }
        val r = updater.check()
        if (r !is dev.mrbean.laundryledger.update.UpdateState.Failed) prefs.lastUpdateCheck = System.currentTimeMillis()
    }

    fun dismissUpdate(version: String) { prefs.dismissedVersion = version }

    /** null while loading. */
    val batches: StateFlow<List<BatchWithItems>?> =
        dao.observeBatches().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun batch(id: Long): Flow<BatchWithItems?> = dao.observeBatch(id)

    suspend fun createBatch(): Long = dao.insertBatch(Batch())

    fun add(batchId: Long, type: ClothType, qty: Int) = viewModelScope.launch {
        prefs.lastType = type
        prefs.setLastQty(type, qty)
        dao.addToBatch(batchId, type, qty)
    }

    fun setSent(item: BatchItem, sent: Int) = viewModelScope.launch {
        if (sent <= 0) dao.deleteItem(item.id)
        else dao.updateCounts(item.id, sent, minOf(item.received, sent))
    }

    fun setReceived(item: BatchItem, received: Int) = viewModelScope.launch {
        dao.updateCounts(item.id, item.sent, received.coerceIn(0, item.sent))
    }

    fun restoreItem(item: BatchItem) = viewModelScope.launch {
        if (dao.findItem(item.batchId, item.type) == null) dao.insertItem(item)
    }

    fun receiveAll(batchId: Long) = viewModelScope.launch { dao.receiveAll(batchId) }

    fun setReturned(batchId: Long, returned: Boolean) = viewModelScope.launch {
        dao.setReturned(batchId, if (returned) System.currentTimeMillis() else null)
    }

    fun deleteBatch(batchId: Long) = viewModelScope.launch { dao.deleteBatch(batchId) }

    fun restoreBatch(b: BatchWithItems) = viewModelScope.launch { dao.restore(b) }

    /** Called when leaving a batch screen: drop batches that never got any items. */
    fun discardIfEmpty(batchId: Long) = viewModelScope.launch {
        if (dao.itemCount(batchId) == 0) dao.deleteBatch(batchId)
    }
}
