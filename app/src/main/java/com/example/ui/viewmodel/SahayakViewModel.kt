package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.SahayakRepository
import com.example.data.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LedgerFilter {
    ALL,
    JAMA_CREDIT,
    UDHAAR_DEBIT,
    PENDING_OCR
}

data class GrowthSimResult(
    val investmentAmount: Double,
    val monthlyRevenueAdded: Double,
    val monthlyNetProfitAdded: Double,
    val annualProfitIncrease: Double,
    val roiPercent: Double,
    val paybackMonths: Double,
    val mudraCategoryFit: String,
    val adviceNotes: String,
    val adviceNotesHi: String
)

data class ProfitSplitMember(
    val id: String,
    val name: String,
    val role: String,
    val contributionPercent: Double,
    val payoutAmount: Double = 0.0
)

data class ProfitSplitResult(
    val grossRevenue: Double,
    val totalExpenses: Double,
    val netDistributableProfit: Double,
    val reserveFundAmount: Double,
    val reservePercent: Double,
    val memberPayouts: List<ProfitSplitMember>
)

data class BreakEvenResult(
    val fixedCostsMonthly: Double,
    val unitSellingPrice: Double,
    val unitVariableCost: Double,
    val contributionMarginPerUnit: Double,
    val contributionMarginRatio: Double,
    val unitsNeededMonthly: Int,
    val unitsNeededDaily: Int,
    val salesRevenueNeededMonthly: Double,
    val adviceSummary: String,
    val adviceSummaryHi: String
)

class SahayakViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = SahayakRepository(database)
    private val geminiService = GeminiAdvisorService()
    private val scoringService = FinancialScoringService()
    private val ocrParser = OcrKhataParser()
    private val ttsManager = TtsManager(application)
    private val kycProvider = repository.kycProvider
    private val feasibilityService = FeasibilityService()
    private val feasibilityDao = database.feasibilityDao()

    val pastFeasibilityChecks: StateFlow<List<FeasibilityCheckEntity>> = feasibilityDao.getAllChecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _feasibilityReport = MutableStateFlow<FeasibilityReport?>(null)
    val feasibilityReport: StateFlow<FeasibilityReport?> = _feasibilityReport

    private val _feasibilityLoading = MutableStateFlow(false)
    val feasibilityLoading: StateFlow<Boolean> = _feasibilityLoading

    private val _feasibilityError = MutableStateFlow<String?>(null)
    val feasibilityError: StateFlow<String?> = _feasibilityError

    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val allLedgerEntries: StateFlow<List<LedgerEntry>> = repository.allLedgerEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSchemes: StateFlow<List<Scheme>> = repository.allSchemes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders: StateFlow<List<BusinessReminder>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPosts: StateFlow<List<CommunityPost>> = repository.allPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCommunityUsers: StateFlow<List<CommunityUser>> = repository.allCommunityUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConversations: StateFlow<List<DirectConversation>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeConversationMessages: StateFlow<List<DirectMessage>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId == null) flowOf(emptyList()) else repository.getMessagesForConversation(convId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activePostForComments = MutableStateFlow<CommunityPost?>(null)
    val activePostForComments: StateFlow<CommunityPost?> = _activePostForComments

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activePostComments: StateFlow<List<PostComment>> = _activePostForComments
        .flatMapLatest { post ->
            if (post == null) flowOf(emptyList()) else repository.getCommentsForPost(post.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCommunityUser = MutableStateFlow<CommunityUser?>(null)
    val selectedCommunityUser: StateFlow<CommunityUser?> = _selectedCommunityUser

    private val _moderationNotice = MutableStateFlow<String?>(null)
    val moderationNotice: StateFlow<String?> = _moderationNotice

    val mandiPrices: StateFlow<List<MandiCommodity>> = repository.mandiPrices

    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDigitalDocuments: StateFlow<List<DigitalDocument>> = repository.allDigitalDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScannedKhatas: StateFlow<List<ScannedKhata>> = repository.allScannedKhatas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Machine Learning Forecasting & Demand Models
    val allCommodityForecasts: List<com.example.data.ml.CommodityMlForecast> =
        com.example.data.ml.MlForecastingEngine.getAllCommodityForecasts()

    private val _selectedForecastCommodity = MutableStateFlow(allCommodityForecasts.first())
    val selectedForecastCommodity: StateFlow<com.example.data.ml.CommodityMlForecast> = _selectedForecastCommodity

    val mlPredictionSummary: com.example.data.ml.MlPredictionSummary =
        com.example.data.ml.MlForecastingEngine.getMlPredictionSummary()

    private val _selectedLedgerFilter = MutableStateFlow(LedgerFilter.ALL)
    val selectedLedgerFilter: StateFlow<LedgerFilter> = _selectedLedgerFilter

    private val _selectedSchemeCategory = MutableStateFlow<SchemeCategory?>(null)
    val selectedSchemeCategory: StateFlow<SchemeCategory?> = _selectedSchemeCategory

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking

    val isTtsSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    // OCR Pending review items (Human-in-the-loop)
    private val _ocrReviewItems = MutableStateFlow<List<OcrParsedItem>?>(null)
    val ocrReviewItems: StateFlow<List<OcrParsedItem>?> = _ocrReviewItems

    // Calculators state
    private val _growthSimResult = MutableStateFlow<GrowthSimResult?>(null)
    val growthSimResult: StateFlow<GrowthSimResult?> = _growthSimResult

    private val _profitSplitResult = MutableStateFlow<ProfitSplitResult?>(null)
    val profitSplitResult: StateFlow<ProfitSplitResult?> = _profitSplitResult

    private val _breakEvenResult = MutableStateFlow<BreakEvenResult?>(null)
    val breakEvenResult: StateFlow<BreakEvenResult?> = _breakEvenResult

    // KYC Mock States
    private val _panDetails = MutableStateFlow<PanDetails?>(null)
    val panDetails: StateFlow<PanDetails?> = _panDetails

    private val _aadhaarDetails = MutableStateFlow<AadhaarDetails?>(null)
    val aadhaarDetails: StateFlow<AadhaarDetails?> = _aadhaarDetails

    private val _bankAccount = MutableStateFlow<BankAccount?>(null)
    val bankAccount: StateFlow<BankAccount?> = _bankAccount

    private val _cibilReport = MutableStateFlow<CibilReport?>(null)
    val cibilReport: StateFlow<CibilReport?> = _cibilReport

    // Reactive Financial Health Score & Bank Report
    val financialHealthScore: StateFlow<FinancialHealthScore> = combine(userProfile, allLedgerEntries) { profile, ledger ->
        scoringService.calculateScore(profile, ledger)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        scoringService.calculateScore(UserProfile(), emptyList())
    )

    val bankReportSummary: StateFlow<BankReportSummary> = combine(userProfile, financialHealthScore, allLedgerEntries) { profile, score, ledger ->
        scoringService.generateBankReportSummary(profile, score, ledger)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        scoringService.generateBankReportSummary(UserProfile(), scoringService.calculateScore(UserProfile(), emptyList()), emptyList())
    )

    init {
        // Run initial default calculations so screens have immediate rich values
        runGrowthSimulation(15000.0, 450.0, 25.0)
        runBreakEven(4500.0, 50.0, 32.0)
        loadInitialMockKyc()
    }

    private fun loadInitialMockKyc() {
        viewModelScope.launch(Dispatchers.IO) {
            _panDetails.value = kycProvider.verifyPan("ABCPS1234F")
            _aadhaarDetails.value = kycProvider.verifyAadhaar("987654324892", "123456")
            _bankAccount.value = kycProvider.linkBank("ACC-9048102")
            _cibilReport.value = kycProvider.fetchCibil("usr_demo_01")
        }
    }

    fun setLedgerFilter(filter: LedgerFilter) {
        _selectedLedgerFilter.value = filter
    }

    fun setSchemeCategory(category: SchemeCategory?) {
        _selectedSchemeCategory.value = category
    }

    fun loginWithPhone(phone: String, isRegistered: Boolean = true, profile: UserProfile? = null) {
        if (profile != null) {
            repository.updateUserProfile(profile.copy(isLoggedIn = true))
        } else {
            val current = userProfile.value
            repository.updateUserProfile(
                current.copy(
                    phone = phone,
                    isLoggedIn = true,
                    profileComplete = isRegistered && current.name.isNotBlank()
                )
            )
        }
    }

    fun completeProfileSetup(
        name: String,
        businessType: BusinessType,
        businessName: String,
        location: String,
        state: String,
        pincode: String,
        monthlyTurnover: Double,
        preferredLanguage: AppLanguage,
        shgName: String = ""
    ) {
        val current = userProfile.value
        val updated = current.copy(
            name = name.trim(),
            businessType = businessType,
            businessName = businessName.trim().ifBlank { "$name's Enterprise" },
            location = location.trim(),
            state = state.trim().ifBlank { "Uttar Pradesh" },
            pincode = pincode.trim().ifBlank { "221001" },
            monthlyTurnover = monthlyTurnover,
            preferredLanguage = preferredLanguage,
            shgName = shgName.trim(),
            isLoggedIn = true,
            profileComplete = true,
            onboardingCompletedAt = System.currentTimeMillis()
        )
        repository.updateUserProfile(updated)
    }

    fun setLanguage(language: AppLanguage) {
        val current = userProfile.value
        val updated = current.copy(preferredLanguage = language)
        repository.updateUserProfile(updated)
    }

    fun updateProfile(name: String, businessType: BusinessType, location: String, monthlyTurnover: Double, shgName: String) {
        val current = userProfile.value
        val updated = current.copy(
            name = name,
            businessType = businessType,
            location = location,
            monthlyTurnover = monthlyTurnover,
            shgName = shgName
        )
        repository.updateUserProfile(updated)
    }

    fun updateFullProfile(
        name: String,
        phone: String,
        email: String,
        businessName: String,
        businessType: BusinessType,
        location: String,
        state: String,
        pincode: String,
        monthlyTurnover: Double,
        dailyCustomers: Int,
        shgName: String,
        avatarIndex: Int,
        whatsappPhone: String
    ) {
        val current = userProfile.value
        val updated = current.copy(
            name = name.trim().ifBlank { current.name },
            phone = phone.trim().ifBlank { current.phone },
            email = email.trim().ifBlank { current.email },
            businessName = businessName.trim().ifBlank { current.businessName },
            businessType = businessType,
            location = location.trim().ifBlank { current.location },
            state = state.trim().ifBlank { current.state },
            pincode = pincode.trim().ifBlank { current.pincode },
            monthlyTurnover = monthlyTurnover,
            dailyCustomers = dailyCustomers,
            shgName = shgName.trim(),
            avatarIndex = avatarIndex,
            whatsappPhone = whatsappPhone.trim().ifBlank { phone }
        )
        repository.updateUserProfile(updated)
    }

    // --- CHAT & VOICE ADVISORY ---

    fun sendChatMessage(query: String, autoSpeak: Boolean = true) {
        if (query.isBlank()) return
        viewModelScope.launch {
            // 1. Save user query
            val userMsg = ChatMessage(text = query, isUser = true)
            repository.saveChatMessage(userMsg)

            _isAiThinking.value = true

            try {
                val profile = userProfile.value
                val ledger = allLedgerEntries.value

                // Call FastAPI POST /chatbot/query via geminiService
                val advisorResp = geminiService.queryChatbot(
                    userQuery = query,
                    userProfile = profile,
                    ledgerEntries = ledger,
                    forceOffline = false
                )

                // Log and handle validation failure flagged by backend
                if (!advisorResp.validationPassed) {
                    Log.w(
                        "SahayakViewModel",
                        "[ADVISOR VALIDATION ALERT] Backend flagged validation failure for ${advisorResp.primaryTitle}: violations=${advisorResp.validationViolations}, retries=${advisorResp.retryCount}"
                    )
                }

                val aiMsg = ChatMessage(
                    text = advisorResp.adviceText,
                    isUser = false,
                    isOfflineTier = advisorResp.isOfflineTier,
                    validationPassed = advisorResp.validationPassed,
                    validationViolations = if (advisorResp.validationViolations.isNotEmpty()) advisorResp.validationViolations.joinToString(", ") else null,
                    backendTradeTitle = advisorResp.primaryTitle
                )
                repository.saveChatMessage(aiMsg)

                if (autoSpeak) {
                    val speechText = advisorResp.audioTtsText.ifBlank { advisorResp.adviceText }
                    speakText(speechText, if (profile.preferredLanguage == AppLanguage.HINDI) "hi" else "en")
                }
            } catch (e: Exception) {
                val errorMsg = "माफ़ कीजिए, उत्तर प्राप्त करने में समस्या हुई। कृपया पुनः प्रयास करें।"
                repository.saveChatMessage(ChatMessage(text = errorMsg, isUser = false, isOfflineTier = true))
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun speakText(text: String, langCode: String = "hi") {
        ttsManager.speak(text, langCode)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    // --- KHATA & OCR DIGITIZATION ---

    fun getKhataById(id: Long): Flow<ScannedKhata?> = repository.getKhataById(id)

    fun getEntriesForKhata(khataId: Long): Flow<List<ScannedKhataEntry>> = repository.getEntriesForKhata(khataId)

    fun getAllKhataPresets() = ocrParser.getAllKhataPresets()

    fun structureAndProcessKhataScan(
        rawRows: List<String>,
        isOnline: Boolean = true,
        onResult: (List<OcrParsedItem>, Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAiThinking.value = true
            try {
                if (isOnline) {
                    val structured = geminiService.structureKhataTextWithGemini(rawRows)
                    withContext(Dispatchers.Main) {
                        onResult(structured, false)
                    }
                } else {
                    val localParsed = ocrParser.parseRowsPreservingLayout(rawRows)
                    withContext(Dispatchers.Main) {
                        onResult(localParsed, true)
                    }
                }
            } catch (e: Exception) {
                val fallback = ocrParser.parseRowsPreservingLayout(rawRows)
                withContext(Dispatchers.Main) {
                    onResult(fallback, true)
                }
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun saveScannedKhataWithEntries(
        label: String,
        description: String,
        imageUri: String?,
        presetId: String?,
        source: KhataSource,
        items: List<OcrParsedItem>,
        isOffline: Boolean = false,
        onSaved: (Long) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val today = sdf.format(Date())

            val totalCredit = items.filter { it.type == LedgerType.CREDIT }.sumOf { it.amount }
            val totalDebit = items.filter { it.type == LedgerType.DEBIT }.sumOf { it.amount }

            val khata = ScannedKhata(
                label = label.ifBlank { "Khata Scan $today" },
                description = description,
                imageUri = imageUri,
                imagePresetId = presetId,
                source = source,
                status = if (isOffline) KhataStatus.NEEDS_REVIEW else KhataStatus.CONFIRMED,
                entryCount = items.size,
                totalCredit = totalCredit,
                totalDebit = totalDebit,
                isParsedOffline = isOffline,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val subEntries = items.map { item ->
                ScannedKhataEntry(
                    khataId = 0, // will be mapped in repository
                    date = if (item.date.isNotBlank()) item.date else today,
                    partyName = item.partyName,
                    description = item.description,
                    amount = item.amount,
                    type = item.type,
                    category = item.category,
                    rawText = item.rawText,
                    confidence = item.confidence,
                    editedByUser = item.editedByUser
                )
            }

            val savedId = repository.saveConfirmedKhata(khata, subEntries, syncToAggregateLedger = true)
            withContext(Dispatchers.Main) {
                onSaved(savedId)
            }
        }
    }

    fun updateExistingKhata(
        khata: ScannedKhata,
        entries: List<ScannedKhataEntry>,
        onUpdated: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateScannedKhata(khata, entries)
            withContext(Dispatchers.Main) {
                onUpdated()
            }
        }
    }

    fun deleteScannedKhata(khataId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteScannedKhata(khataId)
            withContext(Dispatchers.Main) {
                onDeleted()
            }
        }
    }

    fun parseCsvOrStatement(
        content: String,
        dateCol: Int = 0,
        descCol: Int = 1,
        amountCol: Int = 2,
        typeCol: Int = 3
    ): List<OcrParsedItem> {
        return ocrParser.parseCsvOrStatement(content, dateCol, descCol, amountCol, typeCol)
    }

    /**
     * Checks if an entry's description relates to any monitored commodity in our Mandi ML forecasting model
     * (e.g. Onion, Wheat, Mustard Oil, Potato, Milk, Chana Dal, Fertilizer)
     */
    fun matchCommodityForecast(description: String): com.example.data.ml.CommodityMlForecast? {
        val lower = description.lowercase(Locale.ROOT)
        return allCommodityForecasts.firstOrNull { forecast ->
            val nameLower = forecast.nameEn.lowercase(Locale.ROOT)
            val nameHiLower = forecast.nameHi.lowercase(Locale.ROOT)
            when {
                lower.contains("onion") || lower.contains("pyaz") || lower.contains("प्याज") -> nameLower.contains("onion")
                lower.contains("wheat") || lower.contains("gehun") || lower.contains("गेहूं") || lower.contains("aata") || lower.contains("आटा") -> nameLower.contains("wheat")
                lower.contains("mustard") || lower.contains("sarson") || lower.contains("सरसों") || lower.contains("oil") || lower.contains("तेल") -> nameLower.contains("mustard")
                lower.contains("potato") || lower.contains("aaloo") || lower.contains("आलू") -> nameLower.contains("potato")
                lower.contains("milk") || lower.contains("doodh") || lower.contains("दूध") || lower.contains("curd") -> nameLower.contains("milk")
                lower.contains("chana") || lower.contains("dal") || lower.contains("दाल") -> nameLower.contains("chana")
                lower.contains("fertilizer") || lower.contains("urea") || lower.contains("dap") || lower.contains("खाद") -> nameLower.contains("fertilizer")
                else -> lower.contains(nameLower.split(" ").first()) || lower.contains(nameHiLower.split(" ").first())
            }
        }
    }

    fun startOcrScan(rawText: String) {
        val parsed = ocrParser.parseKhataText(rawText)
        _ocrReviewItems.value = parsed
    }

    fun clearOcrReview() {
        _ocrReviewItems.value = null
    }

    fun confirmOcrEntries(confirmedItems: List<OcrParsedItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val today = sdf.format(Date())

            val entries = confirmedItems.map { item ->
                LedgerEntry(
                    date = today,
                    partyName = item.partyName,
                    description = item.description,
                    amount = item.amount,
                    type = item.type,
                    category = item.category,
                    source = LedgerSource.OCR,
                    isConfirmed = true
                )
            }
            repository.addLedgerEntries(entries)
            _ocrReviewItems.value = null
        }
    }

    fun addManualLedgerEntry(
        partyName: String,
        description: String,
        amount: Double,
        type: LedgerType,
        category: LedgerCategory,
        phone: String? = null,
        source: LedgerSource = LedgerSource.MANUAL
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val today = sdf.format(Date())

            val entry = LedgerEntry(
                date = today,
                partyName = partyName,
                description = description,
                amount = amount,
                type = type,
                category = category,
                source = source,
                isConfirmed = true,
                customerPhone = phone
            )
            repository.addLedgerEntry(entry)
        }
    }

    fun deleteLedgerEntry(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLedgerEntry(id)
        }
    }

    fun generateWhatsAppReminderText(entry: LedgerEntry, lang: AppLanguage = AppLanguage.HINDI): String {
        return if (lang == AppLanguage.HINDI) {
            "नमस्ते ${entry.partyName} जी! 🙏\nआपकी दुकान '${userProfile.value.name}' का कुल बकाया ₹${String.format(Locale.ROOT, "%.0f", entry.amount)} बाकी है (${entry.description})। कृपया सुविधानुसार भुगतान कर दें। धन्यवाद! \n- ${userProfile.value.name}"
        } else {
            "Hello ${entry.partyName}! 🙏\nThis is a polite reminder from '${userProfile.value.name}' regarding outstanding balance of ₹${String.format(Locale.ROOT, "%.0f", entry.amount)} for ${entry.description}. Kindly settle at your earliest convenience. Thank you!"
        }
    }

    fun getSampleKhataPresets() = ocrParser.getSampleKhataPresets()

    // --- ML COMMODITY FORECASTING ---

    fun selectForecastCommodity(commodity: com.example.data.ml.CommodityMlForecast) {
        _selectedForecastCommodity.value = commodity
    }

    // --- AUTHENTICATION & LOGIN ---

    fun loginWithPhone(phone: String, userName: String = "Ramesh Kumar Sharma", businessType: BusinessType = BusinessType.KIRANA): Boolean {
        val current = userProfile.value
        val updated = current.copy(
            phone = if (phone.startsWith("+91")) phone else "+91 $phone",
            name = if (userName.isNotBlank()) userName else current.name,
            businessType = businessType,
            isLoggedIn = true
        )
        repository.updateUserProfile(updated)
        return true
    }

    fun loginWithOtp(phone: String, otp: String, role: BusinessType, name: String): Boolean {
        // Valid demo OTP: 749201 or any 6-digit number in demo mode
        val isValid = otp.trim().length == 6
        if (isValid) {
            val current = userProfile.value
            val updated = current.copy(
                phone = if (phone.startsWith("+91")) phone else "+91 $phone",
                businessType = role,
                name = if (name.isNotBlank()) name else current.name,
                isLoggedIn = true,
                authOtp = otp
            )
            repository.updateUserProfile(updated)
            return true
        }
        return false
    }

    fun loginWithPassword(identifier: String, password: String): Boolean {
        if (identifier.isNotBlank() && password.length >= 4) {
            val current = userProfile.value
            val isPhone = identifier.filter { it.isDigit() }.length >= 10
            val updated = current.copy(
                phone = if (isPhone) "+91 ${identifier.filter { it.isDigit() }.takeLast(10)}" else current.phone,
                email = if (!isPhone && identifier.contains("@")) identifier.trim() else current.email,
                isLoggedIn = true
            )
            repository.updateUserProfile(updated)
            return true
        }
        return false
    }

    fun registerUser(
        fullName: String,
        phone: String,
        email: String,
        businessName: String,
        businessType: BusinessType,
        location: String,
        state: String,
        monthlyTurnover: Double
    ): Boolean {
        if (fullName.isNotBlank() && phone.isNotBlank()) {
            val current = userProfile.value
            val cleanPhone = if (phone.startsWith("+91")) phone else "+91 ${phone.filter { it.isDigit() }}"
            val updated = current.copy(
                name = fullName.trim(),
                phone = cleanPhone,
                email = if (email.isNotBlank()) email.trim() else "${fullName.lowercase().replace(" ", "")}@sahayak.in",
                businessName = if (businessName.isNotBlank()) businessName.trim() else "${fullName.trim()}'s ${businessType.title}",
                businessType = businessType,
                location = location.trim().ifBlank { "Varanasi Rural, Uttar Pradesh" },
                state = state.trim().ifBlank { "Uttar Pradesh" },
                monthlyTurnover = if (monthlyTurnover > 0) monthlyTurnover else 45000.0,
                isLoggedIn = true
            )
            repository.updateUserProfile(updated)
            return true
        }
        return false
    }

    fun logout() {
        val current = userProfile.value
        val updated = current.copy(isLoggedIn = false, profileComplete = false)
        repository.updateUserProfile(updated)
    }

    // --- DIGITAL DOCUMENT LOCKER ---

    fun addScannedDigitalDocument(
        title: String,
        category: DocumentCategory,
        totalAmount: Double,
        extractedText: String,
        parsedEntryCount: Int,
        notes: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val today = sdf.format(Date())

            val doc = DigitalDocument(
                title = title,
                category = category,
                dateAdded = today,
                totalAmount = totalAmount,
                extractedText = extractedText,
                parsedEntryCount = parsedEntryCount,
                isSyncedToKhata = true,
                notes = notes
            )
            repository.addDigitalDocument(doc)
        }
    }

    fun deleteDigitalDocument(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDigitalDocument(id)
        }
    }

    fun processCameraScanAndAddToLocker(
        photoTitle: String,
        category: DocumentCategory,
        rawOcrText: String
    ) {
        val parsed = ocrParser.parseKhataText(rawOcrText)
        val totalAmount = parsed.sumOf { it.amount }
        addScannedDigitalDocument(
            title = photoTitle,
            category = category,
            totalAmount = totalAmount,
            extractedText = rawOcrText,
            parsedEntryCount = parsed.size,
            notes = "Scanned via camera & preserved in Digital Document Vault"
        )
        // Also load into OCR review for Khata ledger confirmation
        _ocrReviewItems.value = parsed
    }

    // --- REMINDERS ---

    fun addReminder(title: String, dueDate: String, type: ReminderType, amount: Double?, note: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val reminder = BusinessReminder(
                title = title,
                dueDate = dueDate,
                type = type,
                amount = amount,
                isCompleted = false,
                note = note
            )
            repository.addReminder(reminder)
        }
    }

    fun toggleReminder(reminder: BusinessReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleReminderComplete(reminder.id, !reminder.isCompleted)
        }
    }

    fun deleteReminder(reminder: BusinessReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteReminder(reminder)
        }
    }

    // --- COMMUNITY ---

    fun togglePostLike(post: CommunityPost) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.togglePostLike(post.id, post.isLikedByUser, post.likesCount)
        }
    }

    fun createCommunityPost(
        type: CommunityPostType,
        caption: String,
        tag: String,
        category: String,
        mediaUrl: String? = null,
        voiceSeconds: Int? = null
    ): Boolean {
        // Run deterministic keyword moderation check (No AI/chatbot)
        val modCheck = CommunityModerationService.checkContent(caption)
        if (!modCheck.isApproved) {
            val isHindi = userProfile.value.preferredLanguage == AppLanguage.HINDI
            _moderationNotice.value = if (isHindi) modCheck.reasonMessageHi else modCheck.reasonMessage
            return false
        }
        _moderationNotice.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val profile = userProfile.value
            val formattedTag = if (tag.startsWith("#")) tag else if (tag.isNotBlank()) "#$tag" else "#BusinessTip"
            val newPost = CommunityPost(
                postId = "post_${System.currentTimeMillis()}",
                userId = "user_current",
                authorName = profile.name,
                authorUsername = "ramesh_grocer",
                authorAvatarIndex = 0,
                authorRole = "${profile.businessType.title}, ${profile.location.substringBefore(",")}",
                type = type,
                caption = caption.trim(),
                mediaUrl = mediaUrl,
                tag = formattedTag,
                category = category,
                likesCount = 1,
                commentsCount = 0,
                isLikedByUser = true,
                voiceNoteSeconds = voiceSeconds,
                createdAtFormatted = "Just now",
                createdAtTimestamp = System.currentTimeMillis()
            )
            repository.addCommunityPost(newPost)
        }
        return true
    }

    fun deleteCommunityPost(postId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCommunityPost(postId)
            if (_activePostForComments.value?.id == postId) {
                _activePostForComments.value = null
            }
        }
    }

    fun addPostComment(postId: Long, content: String): Boolean {
        val modCheck = CommunityModerationService.checkContent(content)
        if (!modCheck.isApproved) {
            val isHindi = userProfile.value.preferredLanguage == AppLanguage.HINDI
            _moderationNotice.value = if (isHindi) modCheck.reasonMessageHi else modCheck.reasonMessage
            return false
        }
        _moderationNotice.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val profile = userProfile.value
            val comment = PostComment(
                commentId = "cmt_${System.currentTimeMillis()}",
                postId = postId,
                userId = "user_current",
                userName = profile.name,
                userUsername = "ramesh_grocer",
                userAvatarIndex = 0,
                userRole = "${profile.businessType.title}, ${profile.location.substringBefore(",")}",
                content = content.trim(),
                createdAtFormatted = "Just now",
                createdAtTimestamp = System.currentTimeMillis()
            )
            repository.addPostComment(comment)
        }
        return true
    }

    fun deletePostComment(commentId: Long, postId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePostComment(commentId, postId)
        }
    }

    fun toggleFollowUser(targetUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFollowUser(targetUid)
        }
    }

    fun setActivePostForComments(post: CommunityPost?) {
        _activePostForComments.value = post
    }

    fun setSelectedCommunityUser(user: CommunityUser?) {
        _selectedCommunityUser.value = user
    }

    fun setActiveConversation(conversationId: String?) {
        _activeConversationId.value = conversationId
        if (conversationId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.markConversationRead(conversationId)
            }
        }
    }

    fun startConversationWithUser(targetUser: CommunityUser, onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val convId = repository.getOrCreateConversation(targetUser, userProfile.value)
            withContext(Dispatchers.Main) {
                _activeConversationId.value = convId
                onReady(convId)
            }
        }
    }

    fun sendDirectMessage(conversationId: String, content: String, mediaUrl: String? = null): Boolean {
        val modCheck = CommunityModerationService.checkContent(content)
        if (!modCheck.isApproved) {
            val isHindi = userProfile.value.preferredLanguage == AppLanguage.HINDI
            _moderationNotice.value = if (isHindi) modCheck.reasonMessageHi else modCheck.reasonMessage
            return false
        }
        _moderationNotice.value = null

        viewModelScope.launch(Dispatchers.IO) {
            repository.sendDirectMessage(
                conversationId = conversationId,
                content = content.trim(),
                mediaUrl = mediaUrl,
                senderName = userProfile.value.name
            )
        }
        return true
    }

    fun clearModerationNotice() {
        _moderationNotice.value = null
    }

    fun updateOwnCommunityProfile(username: String, bio: String, role: String, location: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            val updatedProfile = current.copy(name = current.name, location = location)
            repository.updateUserProfile(updatedProfile)

            val communityUser = CommunityUser(
                uid = "user_current",
                username = username.trim().removePrefix("@"),
                displayName = current.name,
                displayNameHi = current.name,
                bio = bio.trim(),
                avatarColorIndex = 0,
                role = role.trim(),
                location = location.trim(),
                followersCount = 142,
                followingCount = 48,
                postsCount = 9,
                isFollowing = false,
                isCurrentUser = true,
                verifiedBadge = true,
                businessCategory = current.businessType.title
            )
            repository.updateCommunityUser(communityUser)
        }
    }

    // --- FINANCIAL CALCULATORS ---

    fun runGrowthSimulation(investment: Double, extraSalesDaily: Double, marginPercent: Double) {
        val extraMonthlyRevenue = extraSalesDaily * 30.0
        val extraMonthlyProfit = extraMonthlyRevenue * (marginPercent / 100.0)
        val annualProfitIncrease = extraMonthlyProfit * 12.0
        val roi = if (investment > 0) (annualProfitIncrease / investment) * 100.0 else 0.0
        val paybackMonths = if (extraMonthlyProfit > 0) investment / extraMonthlyProfit else 0.0

        val mudraCategory = when {
            investment <= 50000 -> "PM Mudra 'Shishu' (Up to ₹50,000 - Zero Collateral)"
            investment <= 500000 -> "PM Mudra 'Kishor' (₹50K to ₹5 Lakh)"
            else -> "PM Mudra 'Tarun' / PMEGP"
        }

        val adviceEn = "Investing ₹${String.format(Locale.ROOT, "%,.0f", investment)} pays back in ${String.format(Locale.ROOT, "%.1f", paybackMonths)} months with an annual return of ${String.format(Locale.ROOT, "%.0f", roi)}%. Highly recommended under $mudraCategory."
        val adviceHi = "₹${String.format(Locale.ROOT, "%,.0f", investment)} का निवेश ${String.format(Locale.ROOT, "%.1f", paybackMonths)} महीने में वापस वसूल हो जाएगा और वार्षिक रिटर्न ${String.format(Locale.ROOT, "%.0f", roi)}% रहेगा। $mudraCategory के तहत आवेदन करें।"

        _growthSimResult.value = GrowthSimResult(
            investmentAmount = investment,
            monthlyRevenueAdded = extraMonthlyRevenue,
            monthlyNetProfitAdded = extraMonthlyProfit,
            annualProfitIncrease = annualProfitIncrease,
            roiPercent = roi,
            paybackMonths = paybackMonths,
            mudraCategoryFit = mudraCategory,
            adviceNotes = adviceEn,
            adviceNotesHi = adviceHi
        )
    }

    fun runProfitSplit(
        grossRevenue: Double,
        expenses: Double,
        reservePercent: Double,
        members: List<ProfitSplitMember>
    ) {
        val netProfit = (grossRevenue - expenses).coerceAtLeast(0.0)
        val reserveAmount = netProfit * (reservePercent / 100.0)
        val distributable = (netProfit - reserveAmount).coerceAtLeast(0.0)

        val updatedMembers = members.map { member ->
            val share = distributable * (member.contributionPercent / 100.0)
            member.copy(payoutAmount = share)
        }

        _profitSplitResult.value = ProfitSplitResult(
            grossRevenue = grossRevenue,
            totalExpenses = expenses,
            netDistributableProfit = distributable,
            reserveFundAmount = reserveAmount,
            reservePercent = reservePercent,
            memberPayouts = updatedMembers
        )
    }

    fun runBreakEven(fixedMonthlyCost: Double, unitPrice: Double, variableCostPerUnit: Double) {
        val marginPerUnit = (unitPrice - variableCostPerUnit).coerceAtLeast(0.01)
        val marginRatio = (marginPerUnit / unitPrice)
        val unitsMonthly = if (marginPerUnit > 0) (fixedMonthlyCost / marginPerUnit).toInt() + 1 else 0
        val unitsDaily = (unitsMonthly / 30).coerceAtLeast(1)
        val revenueNeeded = unitsMonthly * unitPrice

        val adviceEn = "You need to sell at least $unitsDaily units per day (₹${String.format(Locale.ROOT, "%,.0f", revenueNeeded)}/month) to cover rent, utilities and break even."
        val adviceHi = "किराया व बिजली लागत निकालने के लिए आपको प्रतिदिन कम से कम $unitsDaily यूनिट (मासिक ₹${String.format(Locale.ROOT, "%,.0f", revenueNeeded)}) बेचना आवश्यक है।"

        _breakEvenResult.value = BreakEvenResult(
            fixedCostsMonthly = fixedMonthlyCost,
            unitSellingPrice = unitPrice,
            unitVariableCost = variableCostPerUnit,
            contributionMarginPerUnit = marginPerUnit,
            contributionMarginRatio = marginRatio,
            unitsNeededMonthly = unitsMonthly,
            unitsNeededDaily = unitsDaily,
            salesRevenueNeededMonthly = revenueNeeded,
            adviceSummary = adviceEn,
            adviceSummaryHi = adviceHi
        )
    }

    // ==========================================
    // PS26091 FEASIBILITY ANALYSIS & PLANNING
    // ==========================================

    fun runFeasibilityCheck(location: String, margin: Double, category: String) {
        viewModelScope.launch {
            _feasibilityLoading.value = true
            _feasibilityError.value = null
            try {
                val input = FeasibilityInput(
                    location = location.ifBlank { userProfile.value.location },
                    availableMargin = if (margin <= 0) 100000.0 else margin,
                    businessCategory = category
                )
                val report = feasibilityService.runFeasibilityCheck(input)
                val entity = feasibilityService.reportToEntity(report)
                feasibilityDao.insertCheck(entity)
                _feasibilityReport.value = report
            } catch (e: Exception) {
                Log.e("SahayakViewModel", "Feasibility run failed", e)
                _feasibilityError.value = e.localizedMessage ?: "Failed to generate feasibility report."
            } finally {
                _feasibilityLoading.value = false
            }
        }
    }

    fun loadPastCheck(entity: FeasibilityCheckEntity) {
        val report = feasibilityService.entityToReport(entity)
        _feasibilityReport.value = report
    }

    fun deletePastCheck(entity: FeasibilityCheckEntity) {
        viewModelScope.launch {
            feasibilityDao.deleteCheck(entity)
            if (_feasibilityReport.value?.checkId == entity.checkId) {
                _feasibilityReport.value = null
            }
        }
    }

    fun clearFeasibilityReport() {
        _feasibilityReport.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
