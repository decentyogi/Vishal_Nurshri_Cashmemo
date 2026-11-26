package com.vishalnursery.cashmemo.domain.util

import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.round2
import kotlin.math.max
import kotlin.math.min

data class BillShareAllocation(
    val memoId: String,
    val amount: Double
)

data class PaymentAllocationResult(
    val allocations: List<BillShareAllocation>,
    val shareByMemoId: Map<String, Double>,
    val totalAllocated: Double,
    val remainingUnallocated: Double
)

object PaymentAllocator {

    /**
     * An open bill is a cash memo with effectiveDue > 0.009 that has NOT been rolled forward.
     * A bill is rolled forward if a strictly later bill of the same customer has previousDues > 0.009.
     * Results are sorted oldest-first.
     */
    fun findOpenBills(customerMemos: List<CashMemo>): List<CashMemo> {
        val sorted = customerMemos.sortedWith(compareBy<CashMemo> { it.createdAt }.thenBy { it.memoNumber })
        val open = mutableListOf<CashMemo>()
        for (i in sorted.indices) {
            val current = sorted[i]
            if (current.effectiveDue <= 0.009) continue
            val rolledForward = ((i + 1) until sorted.size).any { j -> sorted[j].previousDues > 0.009 }
            if (!rolledForward) {
                open.add(current)
            }
        }
        return open
    }

    /**
     * Automatically distributes paymentAmount oldest-first across the selected openBills.
     * Caps total allocated at the sum of open bill dues.
     */
    fun allocate(openBills: List<CashMemo>, amount: Double): PaymentAllocationResult {
        val sorted = openBills.sortedWith(compareBy<CashMemo> { it.createdAt }.thenBy { it.memoNumber })
        val cleanAmount = round2(max(0.0, amount))
        var remaining = cleanAmount
        val allocations = mutableListOf<BillShareAllocation>()
        val shareMap = linkedMapOf<String, Double>()

        for (bill in sorted) {
            val due = round2(max(0.0, bill.effectiveDue))
            val share = if (remaining > 0.009 && due > 0.009) {
                round2(min(remaining, due))
            } else {
                0.0
            }
            shareMap[bill.id] = share
            if (share > 0.009) {
                allocations.add(BillShareAllocation(memoId = bill.id, amount = share))
                remaining = round2(max(0.0, remaining - share))
            }
        }

        val totalAllocated = round2(allocations.sumOf { it.amount })
        return PaymentAllocationResult(
            allocations = allocations,
            shareByMemoId = shareMap,
            totalAllocated = totalAllocated,
            remainingUnallocated = remaining
        )
    }
}
