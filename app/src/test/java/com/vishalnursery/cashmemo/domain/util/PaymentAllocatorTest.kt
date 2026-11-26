package com.vishalnursery.cashmemo.domain.util

import com.vishalnursery.cashmemo.domain.model.CashMemo
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentAllocatorTest {

    private fun openMemo(
        id: String,
        grandTotal: Double,
        previousDues: Double = 0.0,
        paidAmount: Double = 0.0,
        laterPaid: Double = 0.0,
        createdAt: Long
    ): CashMemo {
        val balanceDue = maxOf(0.0, grandTotal + previousDues - paidAmount)
        return CashMemo(
            id = id,
            memoNumber = id,
            grandTotal = grandTotal,
            subtotal = grandTotal,
            previousDues = previousDues,
            paidAmount = paidAmount,
            balanceDue = balanceDue,
            laterPaid = laterPaid,
            createdAt = createdAt
        )
    }

    @Test
    fun allocatesOldestFirstAcrossMultipleBillsPartial() {
        val b3 = openMemo("1020", grandTotal = 560.0, createdAt = 300L)
        val b1 = openMemo("1018", grandTotal = 4300.0, createdAt = 100L)
        val b2 = openMemo("1019", grandTotal = 175.0, createdAt = 200L)

        // Pass out of order to verify oldest-first sorting
        val result = PaymentAllocator.allocate(listOf(b3, b1, b2), 4000.0)
        assertEquals(4000.0, result.totalAllocated, 0.01)
        assertEquals(0.0, result.remainingUnallocated, 0.01)
        assertEquals(1, result.allocations.size)
        assertEquals("1018", result.allocations[0].memoId)
        assertEquals(4000.0, result.shareByMemoId["1018"]!!, 0.01)
        assertEquals(0.0, result.shareByMemoId["1019"]!!, 0.01)
        assertEquals(0.0, result.shareByMemoId["1020"]!!, 0.01)
    }

    @Test
    fun partialPaymentOverflowingFirstBillPartiallyPaysSecond() {
        val b1 = openMemo("1018", grandTotal = 4300.0, createdAt = 100L)
        val b2 = openMemo("1019", grandTotal = 175.0, createdAt = 200L)
        val b3 = openMemo("1020", grandTotal = 560.0, createdAt = 300L)

        val result = PaymentAllocator.allocate(listOf(b1, b2, b3), 4400.0)
        assertEquals(4400.0, result.totalAllocated, 0.01)
        assertEquals(4300.0, result.shareByMemoId["1018"]!!, 0.01)
        assertEquals(100.0, result.shareByMemoId["1019"]!!, 0.01)
        assertEquals(0.0, result.shareByMemoId["1020"]!!, 0.01)
    }

    @Test
    fun exactFullPaymentSettlesAllBills() {
        val b1 = openMemo("1018", grandTotal = 4300.0, laterPaid = 4000.0, createdAt = 100L) // due 300
        val b2 = openMemo("1019", grandTotal = 175.0, createdAt = 200L)                     // due 175
        val b3 = openMemo("1020", grandTotal = 560.0, createdAt = 300L)                     // due 560

        val result = PaymentAllocator.allocate(listOf(b1, b2, b3), 1035.0)
        assertEquals(1035.0, result.totalAllocated, 0.01)
        assertEquals(0.0, result.remainingUnallocated, 0.01)
        assertEquals(300.0, result.shareByMemoId["1018"]!!, 0.01)
        assertEquals(175.0, result.shareByMemoId["1019"]!!, 0.01)
        assertEquals(560.0, result.shareByMemoId["1020"]!!, 0.01)
    }

    @Test
    fun capsAllocationOnOverEntry() {
        val b1 = openMemo("1018", grandTotal = 4300.0, createdAt = 100L)
        val b2 = openMemo("1019", grandTotal = 175.0, createdAt = 200L)

        val result = PaymentAllocator.allocate(listOf(b1, b2), 6000.0)
        assertEquals(4475.0, result.totalAllocated, 0.01)
        assertEquals(1525.0, result.remainingUnallocated, 0.01)
        assertEquals(4300.0, result.shareByMemoId["1018"]!!, 0.01)
        assertEquals(175.0, result.shareByMemoId["1019"]!!, 0.01)
    }

    @Test
    fun findOpenBillsExcludesRolledForwardEarlierBills() {
        val b1 = openMemo("1", grandTotal = 900.0, previousDues = 0.0, paidAmount = 0.0, createdAt = 100L)
        val b2 = openMemo("2", grandTotal = 500.0, previousDues = 900.0, paidAmount = 0.0, createdAt = 200L)
        val b3 = openMemo("3", grandTotal = 200.0, previousDues = 0.0, paidAmount = 0.0, createdAt = 300L)

        val open = PaymentAllocator.findOpenBills(listOf(b1, b2, b3))
        assertEquals(listOf("2", "3"), open.map { it.id })
    }
}
