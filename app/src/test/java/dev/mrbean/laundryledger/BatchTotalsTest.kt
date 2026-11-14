package dev.mrbean.laundryledger

import dev.mrbean.laundryledger.data.Batch
import dev.mrbean.laundryledger.data.BatchItem
import dev.mrbean.laundryledger.data.BatchWithItems
import dev.mrbean.laundryledger.data.ClothType
import dev.mrbean.laundryledger.data.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchTotalsTest {
    private fun item(type: ClothType, sent: Int, received: Int) =
        BatchItem(batchId = 1, type = type.name, sent = sent, received = received)

    @Test
    fun totalsCapReceivedAtSent() {
        val b = BatchWithItems(
            Batch(id = 1),
            listOf(item(ClothType.SHIRT, 3, 2), item(ClothType.SOCKS, 2, 5)),
        )
        assertEquals(5, b.totalSent)
        assertEquals(4, b.totalReceived)
        assertFalse(b.isReturned)
    }

    @Test
    fun returnedFlag() {
        assertTrue(BatchWithItems(Batch(id = 1, returnedAt = 10L), emptyList()).isReturned)
    }

    @Test
    fun unknownTypeFallsBack() {
        assertEquals(ClothType.SHIRT, ClothType.of("NOPE"))
        assertEquals(ClothType.TOWEL, ClothType.of("TOWEL"))
    }
}

class VersionCompareTest {
    private fun newer(a: String, b: String) = dev.mrbean.laundryledger.update.Updater.isNewer(a, b)

    @Test fun basic() {
        assertTrue(newer("1.1.1", "1.1.0"))
        assertTrue(newer("v1.10.0", "1.9.9"))
        assertTrue(newer("2.0", "1.99.99"))
        assertFalse(newer("1.1.0", "1.1.0"))
        assertFalse(newer("1.0.9", "1.1.0"))
        assertFalse(newer("1.1", "1.1.0"))
    }
}

class CatalogTest {
    @Test fun genderFiltering() {
        val male = ClothType.catalog(Gender.MALE)
        val female = ClothType.catalog(Gender.FEMALE)
        val all = ClothType.catalog(Gender.UNSPECIFIED)
        assertTrue(ClothType.SAREE !in male && ClothType.BANIAN in male)
        assertTrue(ClothType.SAREE in female && ClothType.BANIAN !in female)
        assertEquals(ClothType.entries.size, all.size)
        Gender.entries.forEach { gender -> assertTrue(ClothType.catalog(gender).containsAll(ClothType.defaults(gender))) }
    }
}
