package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SchemeCategory(val label: String, val labelHi: String) {
    CREDIT_LOAN("Subsidized Loans", "रियायती ऋण"),
    WOMEN_EMPOWERMENT("Women & SHG", "महिला व स्वयं सहायता समूह"),
    MINORITY_SC_ST("Ministry of Social Justice", "सामाजिक न्याय व अधिकारिता"),
    STREET_VENDORS("Street Vendors & Micro", "रेहड़ी-पटरी व सूक्ष्म"),
    AGRICULTURE("Agri & Allied", "कृषि व संबद्ध"),
    SKILL_EQUIPMENT("Machinery & Toolkits", "मशीनरी व टूलकिट")
}

@Entity(tableName = "schemes")
data class Scheme(
    @PrimaryKey val id: String,
    val name: String,
    val nameHi: String,
    val ministry: String,
    val description: String,
    val descriptionHi: String,
    val subsidyPercent: Int, // e.g. 25% or 35%
    val maxLoanAmount: Double, // e.g. 500000.0 (5 Lakh)
    val eligibilityCriteria: String,
    val eligibilityCriteriaHi: String,
    val documentsRequired: String,
    val category: SchemeCategory,
    val deadline: String? = null, // e.g. "Ongoing" or "2026-10-31"
    val officialUrl: String = "https://www.myscheme.gov.in",
    val applicationUrl: String = "https://www.myscheme.gov.in",
    val eligibleProfessions: String = "ALL" // Comma-separated enum names e.g. "KIRANA,STREET_VENDOR" or "ALL"
) {
    fun isEligibleFor(businessType: BusinessType): Boolean {
        if (eligibleProfessions.equals("ALL", ignoreCase = true)) return true
        val list = eligibleProfessions.split(",").map { it.trim().uppercase() }
        return list.contains(businessType.name.uppercase()) || list.contains("ALL")
    }
}

enum class ReminderType(val label: String, val labelHi: String) {
    SCHEME_DEADLINE("Scheme Deadline", "योजना की अंतिम तिथि"),
    EMI_REPAYMENT("Loan / Bank EMI", "बैंक किस्त भुगतान"),
    UDHAAR_COLLECTION("Customer Udhaar Followup", "उधार वसूली तकादा"),
    MANDI_TAX("Mandi / License Renewal", "मंडी व लाइसेंस नवीनीकरण"),
    SHG_MEETING("SHG Group Meeting", "समूह बैठक व बचत")
}

@Entity(tableName = "reminders")
data class BusinessReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDate: String, // YYYY-MM-DD
    val type: ReminderType,
    val amount: Double? = null,
    val isCompleted: Boolean = false,
    val note: String = ""
)

data class MandiCommodity(
    val id: String,
    val name: String,
    val nameHi: String,
    val marketLocation: String,
    val pricePerUnit: Double, // in ₹ per Quintal or Kg
    val unit: String = "Quintal",
    val priceChangePercent: Double, // e.g. +4.2 or -1.8
    val trend: String = "UP", // UP, DOWN, STABLE
    val advisoryNote: String,
    val advisoryNoteHi: String
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isOfflineTier: Boolean = false,
    val sourceTag: String = if (isOfflineTier) "CACHED" else "LIVE",
    val suggestedActions: String? = null, // Comma-separated quick actions
    val validationPassed: Boolean = true,
    val validationViolations: String? = null,
    val backendTradeTitle: String? = null
)
