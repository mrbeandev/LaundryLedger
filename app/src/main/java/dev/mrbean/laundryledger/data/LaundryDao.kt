package dev.mrbean.laundryledger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LaundryDao {
    @Transaction
    @Query("SELECT * FROM batches ORDER BY (returnedAt IS NOT NULL), createdAt DESC")
    fun observeBatches(): Flow<List<BatchWithItems>>

    @Transaction
    @Query("SELECT * FROM batches WHERE id = :id")
    fun observeBatch(id: Long): Flow<BatchWithItems?>

    @Insert
    suspend fun insertBatch(batch: Batch): Long

    @Query("DELETE FROM batches WHERE id = :id")
    suspend fun deleteBatch(id: Long)

    @Query("UPDATE batches SET returnedAt = :at WHERE id = :id")
    suspend fun setReturned(id: Long, at: Long?)

    @Query("SELECT * FROM items WHERE batchId = :batchId AND type = :type LIMIT 1")
    suspend fun findItem(batchId: Long, type: String): BatchItem?

    @Insert
    suspend fun insertItem(item: BatchItem): Long

    @Insert
    suspend fun insertItems(items: List<BatchItem>)

    @Query("SELECT COUNT(*) FROM items WHERE batchId = :batchId")
    suspend fun itemCount(batchId: Long): Int

    @Transaction
    suspend fun restore(b: BatchWithItems) {
        insertBatch(b.batch)
        insertItems(b.items)
    }

    @Query("UPDATE items SET sent = :sent, received = :received, updatedAt = :now WHERE id = :id")
    suspend fun updateCounts(id: Long, sent: Int, received: Int, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("UPDATE items SET received = sent WHERE batchId = :batchId")
    suspend fun receiveAll(batchId: Long)

    @Transaction
    suspend fun addToBatch(batchId: Long, type: ClothType, qty: Int) {
        val existing = findItem(batchId, type.name)
        if (existing == null) insertItem(BatchItem(batchId = batchId, type = type.name, sent = qty))
        else updateCounts(existing.id, existing.sent + qty, existing.received)
    }
}
