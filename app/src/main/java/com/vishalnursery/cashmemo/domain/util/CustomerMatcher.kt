package com.vishalnursery.cashmemo.domain.util

import com.vishalnursery.cashmemo.domain.model.CustomerProfile

object CustomerMatcher {

    fun matches(profile: CustomerProfile, name: String, phone: String): Boolean {
        return matches(profile.name, profile.phoneNumber, name, phone)
    }

    fun matches(nameA: String, phoneA: String, nameB: String, phoneB: String): Boolean {
        val cleanPhoneA = phoneA.trim()
        val cleanPhoneB = phoneB.trim()
        if (cleanPhoneA.isNotEmpty() && cleanPhoneB.isNotEmpty()) {
            return cleanPhoneA == cleanPhoneB
        }
        val cleanNameA = nameA.trim()
        val cleanNameB = nameB.trim()
        return cleanNameA.isNotEmpty() && cleanNameA.equals(cleanNameB, ignoreCase = true)
    }

    fun findMatch(customers: List<CustomerProfile>, name: String, phone: String): CustomerProfile? {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isEmpty() && cleanPhone.isEmpty()) return null
        return customers.firstOrNull { matches(it, cleanName, cleanPhone) }
    }
}
