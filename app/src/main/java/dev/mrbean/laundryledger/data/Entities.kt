package dev.mrbean.laundryledger.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "batches")
data class Batch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val returnedAt: Long? = null,
    val note: String = "",
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(
        entity = Batch::class, parentColumns = ["id"], childColumns = ["batchId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["batchId", "type"], unique = true)],
)
data class BatchItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val type: String,
    val sent: Int,
    @ColumnInfo(defaultValue = "0") val received: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val clothType: ClothType get() = ClothType.of(type)
}

data class BatchWithItems(
    @Embedded val batch: Batch,
    @Relation(parentColumn = "id", entityColumn = "batchId") val items: List<BatchItem>,
) {
    val totalSent: Int get() = items.sumOf { it.sent }
    val totalReceived: Int get() = items.sumOf { minOf(it.received, it.sent) }
    val isReturned: Boolean get() = batch.returnedAt != null
}
