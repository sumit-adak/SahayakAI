package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    HINDI("hi", "Hindi", "हिन्दी"),
    ENGLISH("en", "English", "English"),
    HINGLISH("hi-en", "Hinglish", "Hinglish")
}

enum class BusinessType(val title: String, val titleHi: String, val icon: String) {
    KIRANA("Kirana / Grocery", "किराना दुकान", "🛒"),
    FOOD_STALL("Tea Shop / Tea Stall", "चाय दुकान व नाश्ता", "☕"),
    STREET_VENDOR("Street Vendor / Cart", "रेहड़ी-पटरी विक्रेता", "🍢"),
    DAIRY_FARMING("Dairy / Livestock", "डेयरी व पशुपालन", "🐄"),
    AGRICULTURE("Small Farmer / Agriculture", "लघु किसान व कृषि", "🌾"),
    TAILORING("Tailoring / Garments", "सिलाई व परिधान", "🧵"),
    HANDICRAFTS("Artisan / Handicrafts", "हस्तशिल्प / कारीगर", "🏺"),
    OTHER("Other Micro Enterprise", "अन्य सूक्ष्म उद्यम", "💼")
}

data class UserProfile(
    val id: String = "usr_01",
    val name: String = "Ramesh Kumar Sharma",
    val phone: String = "+91 98765 43210",
    val email: String = "ramesh.sharma@sahayak.in",
    val businessName: String = "Sharma General & Kirana Store",
    val businessType: BusinessType = BusinessType.KIRANA,
    val location: String = "Varanasi Rural, Uttar Pradesh",
    val state: String = "Uttar Pradesh",
    val pincode: String = "221001",
    val monthlyTurnover: Double = 45000.0,
    val dailyCustomers: Int = 85,
    val preferredLanguage: AppLanguage = AppLanguage.HINDI,
    val isKycMockLinked: Boolean = true,
    val shgName: String = "Shri Radha Mahila SHG / Gram Samiti",
    val avatarIndex: Int = 0,
    val whatsappPhone: String = "+91 98765 43210",
    val isLoggedIn: Boolean = true,
    val profileComplete: Boolean = true,
    val onboardingCompletedAt: Long = 0L,
    val authOtp: String = "749201"
)

enum class DocumentCategory(val label: String, val labelHi: String, val icon: String) {
    SUPPLIER_BILL("Supplier / Stock Invoice", "थोक पर्ची व बिल", "🧾"),
    CUSTOMER_CHIT("Customer Udhaar Chit", "ग्राहक पर्ची", "📝"),
    MANDI_RECEIPT("Mandi Sale Receipt", "मंडी पर्ची / रसीद", "🌾"),
    LOAN_DOC("Loan / KYC Document", "बैंक व ऋण दस्तावेज", "🏦"),
    EXPENSE_SLIP("Utility / Expense Slip", "किराया व खर्च पर्ची", "⚡")
}

enum class KhataSource(val title: String, val icon: String) {
    CAMERA("Camera Scan", "📷"),
    GALLERY("Gallery Upload", "🖼️"),
    IMPORT("CSV / Statement Import", "📥"),
    PRESET("Sample Handwritten Khata", "📄")
}

enum class KhataStatus(val label: String, val labelHi: String) {
    PROCESSING("Processing", "प्रक्रिया में"),
    NEEDS_REVIEW("Needs Review", "जांच आवश्यक"),
    CONFIRMED("Confirmed", "सत्यापित")
}

@Entity(tableName = "scanned_khatas")
data class ScannedKhata(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,                    // user-given name, e.g. "March 2026 — Grocery Shop"
    val description: String = "",         // optional free-text note
    val imageUri: String? = null,         // original photo URI or local file path
    val imagePresetId: String? = null,    // preset identifier for visual rendering
    val source: KhataSource = KhataSource.CAMERA,
    val status: KhataStatus = KhataStatus.CONFIRMED,
    val entryCount: Int = 0,
    val totalCredit: Double = 0.0,
    val totalDebit: Double = 0.0,
    val isParsedOffline: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "scanned_khata_entries",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = ScannedKhata::class,
            parentColumns = ["id"],
            childColumns = ["khataId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["khataId"])]
)
data class ScannedKhataEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val khataId: Long,
    val date: String,                     // YYYY-MM-DD or DD/MM
    val partyName: String,
    val description: String,
    val amount: Double,
    val type: LedgerType,
    val category: LedgerCategory = LedgerCategory.SALES,
    val rawText: String = "",             // original OCR line, kept for audit/traceability
    val confidence: Float = 0.95f,        // OCR confidence score, 0-1
    val editedByUser: Boolean = false     // true if user corrected the OCR/AI's original value
)

@Entity(tableName = "digital_documents")
data class DigitalDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: DocumentCategory = DocumentCategory.SUPPLIER_BILL,
    val dateAdded: String,
    val totalAmount: Double,
    val imageUri: String? = null,
    val extractedText: String = "",
    val parsedEntryCount: Int = 1,
    val isSyncedToKhata: Boolean = true,
    val notes: String = ""
)

enum class LedgerType {
    CREDIT, // Jama / Received (Income)
    DEBIT   // Udhaar / Given (Expense/Pending Collection)
}

enum class LedgerSource {
    OCR,      // Scanned from handwritten Khata
    MANUAL,   // Manually entered
    VOICE,    // Voice dictated
    AA_MOCK   // Bank Account Aggregator
}

enum class LedgerCategory(val label: String, val labelHi: String) {
    SALES("Sales Income", "दुकान बिक्री"),
    CUSTOMER_UDHAAR("Customer Udhaar", "ग्राहक उधार"),
    INVENTORY_BUY("Stock / Goods Purchase", "सामान / स्टॉक खरीद"),
    RENT_UTILITIES("Rent & Electricity", "किराया व बिजली"),
    LABOR_WAGES("Wages & Daily Pay", "मजदूरी / दिहाड़ी"),
    SHG_SAVINGS("SHG / Chit Deposit", "समूह बचत / बीसी"),
    LOAN_EMI("Loan / Interest EMI", "कर्ज किस्त"),
    OTHER("Other Expense", "अन्य खर्च")
}

@Entity(tableName = "ledger_entries")
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val partyName: String, // Customer or Supplier Name
    val description: String,
    val amount: Double,
    val type: LedgerType,
    val category: LedgerCategory = LedgerCategory.SALES,
    val source: LedgerSource = LedgerSource.MANUAL,
    val isConfirmed: Boolean = true, // Human-in-the-loop review flag
    val customerPhone: String? = null,
    val linkedKhataId: Long? = null, // Linked scanned khata ID for traceability
    val createdAt: Long = System.currentTimeMillis()
)

data class OcrParsedItem(
    val partyName: String,
    val description: String,
    val amount: Double,
    val type: LedgerType,
    val category: LedgerCategory,
    val date: String = "",
    val rawText: String = "",
    val confidence: Float = 0.95f,
    val editedByUser: Boolean = false
) {
    val isLowConfidence: Boolean get() = confidence < 0.85f
}
