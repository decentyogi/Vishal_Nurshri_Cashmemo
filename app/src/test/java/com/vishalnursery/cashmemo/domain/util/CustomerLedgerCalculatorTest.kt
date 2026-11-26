package com.vishalnursery.cashmemo.domain.util

import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerPayment
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerLedgerCalculatorTest {

    private fun memo(
        id: String,
        grandTotal: Double,
        previousDues: Double = 0.0,
        paidAmount: Double,
        createdAt: Long
    ): CashMemo {
        val totalPayable = grandTotal + previousDues
        val balanceDue = maxOf(0.0, totalPayable - paidAmount)
        return CashMemo(
            id = id,
            memoNumber = id,
            grandTotal = grandTotal,
            subtotal = grandTotal,
            previousDues = previousDues,
            paidAmount = paidAmount,
            balanceDue = balanceDue,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }

    @Test
    fun scannedBillWithPreviousDuesAndPartialNetPaymentGivesCorrectDue() {
        val m = memo(
            id = "1001",
            grandTotal = 2060.0,
            previousDues = 900.0,
            paidAmount = 2360.0,
            createdAt = 100L
        )
        val result = CustomerLedgerCalculator.calculate(listOf(m), emptyList())
        assertEquals(600.0, result.outstanding, 0.01)
        assertEquals(2060.0, result.totalPurchased, 0.01)
        assertEquals(2360.0, result.totalPaid, 0.01)
    }

    @Test
    fun plainBillPaidInFullGivesZeroOutstanding() {
        val m = memo(
            id = "1002",
            grandTotal = 1500.0,
            previousDues = 0.0,
            paidAmount = 1500.0,
            createdAt = 100L
        )
        val result = CustomerLedgerCalculator.calculate(listOf(m), emptyList())
        assertEquals(0.0, result.outstanding, 0.01)
        assertEquals(1500.0, result.totalPurchased, 0.01)
        assertEquals(1500.0, result.totalPaid, 0.01)
        assertEquals("Fully Paid", result.lastStatusNote)
    }

    @Test
    fun twoBillsWhereSecondCarriesPreviousDuesDoesNotDoubleCount() {
        val m1 = memo("1", grandTotal = 1000.0, previousDues = 0.0, paidAmount = 400.0, createdAt = 100L)
        val m2 = memo("2", grandTotal = 500.0, previousDues = 600.0, paidAmount = 200.0, createdAt = 200L)
        val result = CustomerLedgerCalculator.calculate(listOf(m1, m2), emptyList())
        assertEquals(900.0, result.outstanding, 0.01)
        assertEquals(1500.0, result.totalPurchased, 0.01)
        assertEquals(600.0, result.totalPaid, 0.01)
    }

    @Test
    fun paymentBetweenTwoBillsReducesRunningBalance() {
        val m1 = memo("1", grandTotal = 4300.0, previousDues = 0.0, paidAmount = 0.0, createdAt = 100L)
        val p1 = CustomerPayment(id = "p1", amount = 4000.0, createdAt = 150L)
        val m2 = memo("2", grandTotal = 175.0, previousDues = 0.0, paidAmount = 0.0, createdAt = 200L)
        val result = CustomerLedgerCalculator.calculate(listOf(m1, m2), listOf(p1))
        assertEquals(475.0, result.outstanding, 0.01)
        assertEquals(4475.0, result.totalPurchased, 0.01)
        assertEquals(4000.0, result.totalPaid, 0.01)
    }

    @Test
    fun overpaymentNeverGoesBelowZero() {
        val m1 = memo("1", grandTotal = 500.0, previousDues = 0.0, paidAmount = 600.0, createdAt = 100L)
        val p1 = CustomerPayment(id = "p1", amount = 200.0, createdAt = 200L)
        val result = CustomerLedgerCalculator.calculate(listOf(m1), listOf(p1))
        assertEquals(0.0, result.outstanding, 0.01)
    }

    @Test
    fun editingBillAndRecomputingMatchesScratchComputation() {
        val m1 = memo("1", grandTotal = 4300.0, paidAmount = 0.0, createdAt = 100L)
        val m2 = memo("2", grandTotal = 175.0, paidAmount = 0.0, createdAt = 200L)
        val m3 = memo("3", grandTotal = 560.0, paidAmount = 0.0, createdAt = 300L)
        val p1 = CustomerPayment(id = "p1", amount = 4000.0, createdAt = 400L)

        // Simulate editing bill #2 note without changing amounts
        val editedM2 = m2.copy(notes = "updated note", updatedAt = 500L)
        val afterEdit = CustomerLedgerCalculator.calculate(listOf(m1, editedM2, m3), listOf(p1))
        val fromScratch = CustomerLedgerCalculator.calculate(listOf(m1, editedM2, m3), listOf(p1))
        assertEquals(1035.0, afterEdit.outstanding, 0.01)
        assertEquals(fromScratch.outstanding, afterEdit.outstanding, 0.01)
        assertEquals(fromScratch.totalPurchased, afterEdit.totalPurchased, 0.01)
        assertEquals(fromScratch.totalPaid, afterEdit.totalPaid, 0.01)
    }
}
