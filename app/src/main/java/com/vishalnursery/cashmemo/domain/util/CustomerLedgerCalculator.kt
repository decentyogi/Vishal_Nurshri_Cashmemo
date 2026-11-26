package com.vishalnursery.cashmemo.domain.util

import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerPayment
import com.vishalnursery.cashmemo.domain.model.round2
import kotlin.math.max

data class CustomerLedgerSummary(
    val outstanding: Double,
    val totalPurchased: Double,
    val totalPaid: Double,
    val lastStatusNote: String,
    val lastTransactionAt: Long
)

object CustomerLedgerCalculator {

    private sealed interface TimelineEvent {
        val timestamp: Long
        val sortOrder: Int

        data class MemoEvent(val memo: CashMemo) : TimelineEvent {
            override val timestamp: Long get() = memo.createdAt
            override val sortOrder: Int get() = 0
        }

        data class PaymentEvent(val payment: CustomerPayment) : TimelineEvent {
            override val timestamp: Long get() = payment.createdAt
            override val sortOrder: Int get() = 1
        }
    }

    fun calculate(
        memos: List<CashMemo>,
        payments: List<CustomerPayment>
    ): CustomerLedgerSummary {
        if (memos.isEmpty() && payments.isEmpty()) {
            return CustomerLedgerSummary(
                outstanding = 0.0,
                totalPurchased = 0.0,
                totalPaid = 0.0,
                lastStatusNote = "Fully Paid",
                lastTransactionAt = 0L
            )
        }

        val events = buildList {
            memos.forEach { add(TimelineEvent.MemoEvent(it)) }
            payments.forEach { add(TimelineEvent.PaymentEvent(it)) }
        }.sortedWith(compareBy<TimelineEvent> { it.timestamp }.thenBy { it.sortOrder })

        var balance = 0.0
        var totalPurchased = 0.0
        var totalPaid = 0.0
        var lastNote = "Fully Paid"
        var lastTxAt = 0L

        for (event in events) {
            when (event) {
                is TimelineEvent.MemoEvent -> {
                    val m = event.memo
                    totalPurchased += m.grandTotal
                    totalPaid += m.paidAmount
                    balance = if (m.previousDues > 0.009) {
                        round2(max(0.0, m.balanceDue))
                    } else {
                        round2(max(0.0, balance + m.grandTotal - m.paidAmount))
                    }
                    lastTxAt = maxOf(lastTxAt, m.createdAt, m.updatedAt)
                    lastNote = if (balance > 0.009) {
                        m.paymentStatusNote.ifBlank { "Carry Forward" }
                    } else {
                        "Fully Paid"
                    }
                }
                is TimelineEvent.PaymentEvent -> {
                    val p = event.payment
                    totalPaid += p.amount
                    balance = round2(max(0.0, balance - p.amount))
                    lastTxAt = maxOf(lastTxAt, p.createdAt)
                    lastNote = if (balance <= 0.009) {
                        "Fully Paid"
                    } else {
                        p.note.ifBlank { "Carry Forward" }
                    }
                }
            }
        }

        val cleanBalance = round2(max(0.0, balance))
        return CustomerLedgerSummary(
            outstanding = cleanBalance,
            totalPurchased = round2(totalPurchased),
            totalPaid = round2(totalPaid),
            lastStatusNote = if (cleanBalance <= 0.009) "Fully Paid" else lastNote,
            lastTransactionAt = lastTxAt
        )
    }
}
