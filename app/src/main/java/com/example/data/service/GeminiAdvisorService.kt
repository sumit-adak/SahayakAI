package com.example.data.service

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AppLanguage
import com.example.data.model.BusinessType
import com.example.data.model.LedgerCategory
import com.example.data.model.LedgerEntry
import com.example.data.model.LedgerType
import com.example.data.model.MandiCommodity
import com.example.data.model.OcrParsedItem
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AdvisorResponse(
    val adviceText: String,
    val isOfflineTier: Boolean = false,
    val sourceTag: String = if (isOfflineTier) "CACHED" else "LIVE",
    val validationPassed: Boolean = true,
    val validationViolations: List<String> = emptyList(),
    val retryCount: Int = 0,
    val businessType: String = "",
    val primaryTitle: String = "",
    val relevantCommodities: List<String> = emptyList(),
    val recommendedSchemes: List<String> = emptyList(),
    val audioTtsText: String = ""
)

/**
 * Trade specification metadata used for tailored prompt injection and guardrail validation.
 */
data class TradeMetadata(
    val title: String,
    val titleHi: String,
    val relevantCommodities: List<String>,
    val nonAgmarknetInputs: List<String>,
    val excludeTopics: List<String>,
    val applicableSchemes: List<String>,
    val focusAreas: List<String>
)

/**
 * GeminiAdvisorService (FastAPI Backend Routing + Resilient Offline Knowledge Base)
 *
 * Provides:
 * 1. Secure routing to SahayakAI FastAPI Backend (POST /chatbot/query) - zero client-side API keys
 * 2. Live Gemini 2.5 Flash reasoning via backend with strict trade boundaries and guardrails
 * 3. Explicit Source Tagging: 'LIVE' when connected, 'CACHED' for offline matches, 'QUEUED' for unmatched queries
 * 4. Resilient offline fallback that activates ONLY when the device has no internet connectivity
 */
class GeminiAdvisorService {

    companion object {
        private const val TAG = "GeminiAdvisorService"
        // Standard Android Emulator loopback alias for localhost host machine
        private const val BACKEND_URL_EMULATOR = "http://10.0.2.2:8000"
        // Secondary loopback for ADB reverse tcp:8000 tcp:8000 / local unit tests
        private const val BACKEND_URL_LOCALHOST = "http://127.0.0.1:8000"
        private const val BACKEND_URL_HOST = "http://localhost:8000"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Primary entry point: Generates real Gemini response from FastAPI backend when online,
     * or uses local knowledge base when offline.
     */
    suspend fun queryChatbot(
        userQuery: String,
        userProfile: UserProfile,
        ledgerEntries: List<LedgerEntry>,
        forceOffline: Boolean = false
    ): AdvisorResponse = withContext(Dispatchers.IO) {
        val isHindi = isQueryInHindi(userQuery, userProfile.preferredLanguage == AppLanguage.HINDI)
        val tradeMeta = getTradeMetadata(userProfile.businessType)

        // Calculate financial summaries for digitized context injection
        var totalCredit = 0.0
        var totalDebit = 0.0
        var pendingUdhaar = 0.0
        for (entry in ledgerEntries) {
            if (entry.type == LedgerType.CREDIT) {
                totalCredit += entry.amount
            } else {
                totalDebit += entry.amount
                if (entry.category == LedgerCategory.CUSTOMER_UDHAAR) {
                    pendingUdhaar += entry.amount
                }
            }
        }
        val netSavings = totalCredit - totalDebit

        // 1. Check if explicitly forced offline (e.g., airplane mode test)
        if (forceOffline) {
            Log.i(TAG, "[OFFLINE MODE FORCED] User requested offline tier.")
            return@withContext buildOfflineOrQueuedResponse(userQuery, userProfile, isHindi, pendingUdhaar, tradeMeta)
        }

        // 2. ONLINE PATH: Query SahayakAI FastAPI Backend (POST /chatbot/query)
        val backendUrls = listOf(BACKEND_URL_EMULATOR, BACKEND_URL_LOCALHOST, BACKEND_URL_HOST)
        var lastHttpError: Exception? = null

        for (baseUrl in backendUrls) {
            try {
                val endpoint = "$baseUrl/chatbot/query"
                val payload = JSONObject().apply {
                    put("query", userQuery)
                    put("user_profile", JSONObject().apply {
                        put("name", userProfile.name.ifBlank { "Rural Entrepreneur" })
                        put("business_type", userProfile.businessType.name)
                        put("location", userProfile.location.ifBlank { "Varanasi, UP" })
                        put("monthly_turnover", userProfile.monthlyTurnover)
                        put("shg_name", userProfile.shgName)
                        put("language", if (isHindi) "hi" else "en")
                    })
                    put("ledger_summary", JSONObject().apply {
                        put("total_inflow", totalCredit)
                        put("total_outflow", totalDebit)
                        put("net_savings", netSavings)
                        put("pending_udhaar", pendingUdhaar)
                        put("transaction_count", ledgerEntries.size)
                    })
                    put("force_offline", false)
                }

                val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(endpoint)
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBodyStr = response.body?.string() ?: ""

                if (response.isSuccessful && responseBodyStr.isNotBlank()) {
                    val respJson = JSONObject(responseBodyStr)
                    val adviceText = respJson.optString("advice_text", "")
                    val isOfflineTier = respJson.optBoolean("is_offline_tier", false)
                    val sourceTag = if (isOfflineTier) "CACHED" else respJson.optString("source_tag", "LIVE")
                    val validationPassed = respJson.optBoolean("validation_passed", true)
                    val retryCount = respJson.optInt("retry_count", 0)

                    val violationsArr = respJson.optJSONArray("validation_violations")
                    val violations = mutableListOf<String>()
                    if (violationsArr != null) {
                        for (i in 0 until violationsArr.length()) {
                            violations.add(violationsArr.getString(i))
                        }
                    }

                    val schemesArr = respJson.optJSONArray("recommended_schemes")
                    val schemes = mutableListOf<String>()
                    if (schemesArr != null) {
                        for (i in 0 until schemesArr.length()) {
                            schemes.add(schemesArr.getString(i))
                        }
                    }

                    val commoditiesArr = respJson.optJSONArray("relevant_commodities")
                    val commodities = mutableListOf<String>()
                    if (commoditiesArr != null) {
                        for (i in 0 until commoditiesArr.length()) {
                            commodities.add(commoditiesArr.getString(i))
                        }
                    }

                    val ttsText = respJson.optString("audio_tts_text", adviceText)

                    Log.i(TAG, "[BACKEND SUCCESS] $sourceTag response from $endpoint (validationPassed=$validationPassed)")
                    return@withContext AdvisorResponse(
                        adviceText = adviceText,
                        isOfflineTier = isOfflineTier,
                        sourceTag = sourceTag,
                        validationPassed = validationPassed,
                        validationViolations = violations,
                        retryCount = retryCount,
                        businessType = respJson.optString("business_type", userProfile.businessType.name),
                        primaryTitle = respJson.optString("primary_title", tradeMeta.title),
                        relevantCommodities = if (commodities.isNotEmpty()) commodities else tradeMeta.relevantCommodities,
                        recommendedSchemes = if (schemes.isNotEmpty()) schemes else tradeMeta.applicableSchemes,
                        audioTtsText = cleanForTts(ttsText)
                    )
                } else {
                    lastHttpError = IOException("Backend HTTP ${response.code}: $responseBodyStr")
                }
            } catch (e: Exception) {
                lastHttpError = e
                Log.w(TAG, "[BACKEND ATTEMPT FAILED] $baseUrl -> ${e.message}")
            }
        }

        // 3. OFFLINE FALLBACK: Device has no connectivity to backend (airplane mode or network disconnect)
        Log.w(TAG, "[FALLBACK ACTIVATED] All backend connections failed (${lastHttpError?.message}). Routing to local knowledge base.")
        return@withContext buildOfflineOrQueuedResponse(userQuery, userProfile, isHindi, pendingUdhaar, tradeMeta)
    }

    private fun isCachedKnowledgeMatch(query: String): Boolean {
        val q = query.lowercase(Locale.ROOT)
        val cachedKeywords = listOf(
            "mudra", "मुद्रा", "loan", "लोन", "कर्ज", "shishu", "kishor", "tarun",
            "svanidhi", "स्वनिधि", "vendor", "ठेला", "पटरी", "रेहड़ी",
            "vishwakarma", "विश्वकर्मा", "कारीगर", "दर्जी", "सिलाई",
            "udhaar", "उधार", "recovery", "वसूली", "बकाया", "khata", "खाता"
        )
        return cachedKeywords.any { q.contains(it) }
    }

    private fun buildOfflineOrQueuedResponse(
        userQuery: String,
        userProfile: UserProfile,
        isHindi: Boolean,
        pendingUdhaar: Double,
        tradeMeta: TradeMetadata
    ): AdvisorResponse {
        val isMatch = isCachedKnowledgeMatch(userQuery)

        return if (isMatch) {
            // Immediate match from on-device Knowledge Base -> CACHED
            val answer = generateLocalOfflineFallback(userQuery, userProfile, isHindi, pendingUdhaar, tradeMeta)
            AdvisorResponse(
                adviceText = answer,
                isOfflineTier = true,
                sourceTag = "CACHED",
                validationPassed = true,
                validationViolations = emptyList(),
                retryCount = 0,
                businessType = userProfile.businessType.name,
                primaryTitle = tradeMeta.title,
                relevantCommodities = tradeMeta.relevantCommodities,
                recommendedSchemes = tradeMeta.applicableSchemes,
                audioTtsText = cleanForTts(answer)
            )
        } else {
            // Unmatched offline query -> QUEUED for background sync
            val queuedMessage = if (isHindi) {
                "⏳ **सहायक AI (ऑफ़लाइन कतार):**\n\n" +
                        "आप अभी ऑफ़लाइन हैं और यह प्रश्न स्थानीय ऑफ़लाइन ज्ञानकोष में उपलब्ध नहीं है।\n" +
                        "• आपके सवाल को **सफलतापूर्वक कतारबद्ध (QUEUED)** कर लिया गया है।\n" +
                        "• जैसे ही इंटरनेट कनेक्टिविटी (Wi-Fi/डेटा) पुनः सक्रिय होगी, यह स्वचालित रूप से सर्वर से उत्तर प्राप्त कर आपको सूचित करेगा।"
            } else {
                "⏳ **SahayakAI (Offline Queue):**\n\n" +
                        "You are currently offline and this query is not in the cached offline knowledge base.\n" +
                        "• Your question has been **safely queued (QUEUED)** for background processing.\n" +
                        "• As soon as internet connectivity is restored, SahayakAI will automatically fetch the live response and notify you."
            }
            AdvisorResponse(
                adviceText = queuedMessage,
                isOfflineTier = true,
                sourceTag = "QUEUED",
                validationPassed = true,
                validationViolations = emptyList(),
                retryCount = 0,
                businessType = userProfile.businessType.name,
                primaryTitle = tradeMeta.title,
                relevantCommodities = tradeMeta.relevantCommodities,
                recommendedSchemes = tradeMeta.applicableSchemes,
                audioTtsText = cleanForTts(queuedMessage)
            )
        }
    }

    private fun cleanForTts(text: String): String {
        return text
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace("- ", "")
            .replace("• ", "")
            .trim()
    }

    private fun isQueryInHindi(query: String, userPrefHindi: Boolean): Boolean {
        if (userPrefHindi) return true
        // Detect Devanagari script characters
        val hasDevanagari = query.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari) return true
        val lower = query.lowercase()
        val hindiKeywords = listOf(
            "kya", "kaise", "mera", "meri", "mere", "naam", "dukaan", "dokan", "vyapar",
            "badhana", "badhaun", "badhaye", "batao", "bataiye", "chahiye", "kitna", "btao",
            "paisa", "rupaye", "udhaar", "munafa", "labh", "bachat", "kharcha", "sahayak", "namaste", "pranam"
        )
        return hindiKeywords.any { lower.contains(it) }
    }

    /**
     * Constructs concise, trade-tailored prompt for Gemini with complete user profile awareness.
     */
    private fun buildGeminiPrompt(
        query: String,
        profile: UserProfile,
        trade: TradeMetadata,
        totalCredit: Double,
        totalDebit: Double,
        netSavings: Double,
        pendingUdhaar: Double,
        transactionCount: Int,
        isHindi: Boolean
    ): String {
        val langName = if (isHindi) "Hindi (Devanagari script, हिन्दी)" else "English"
        val commoditiesContext = getMandiForecastsSnippet(trade.relevantCommodities)

        val excludeNotice = if (trade.excludeTopics.isNotEmpty()) {
            "NEGATIVE CONSTRAINTS: DO NOT suggest unrelated topics: [${trade.excludeTopics.joinToString(", ")}]."
        } else ""

        val ownerName = profile.name.ifBlank { "Ramesh Kumar Sharma" }
        val shopName = profile.businessName.ifBlank { "${ownerName}'s ${trade.title}" }
        val tradeTitle = "${trade.title} (${trade.titleHi})"
        val locationStr = "${profile.location.ifBlank { "Varanasi" }}, ${profile.state.ifBlank { "Uttar Pradesh" }}"
        val turnoverStr = "₹${profile.monthlyTurnover.toInt()}"
        val savingsStr = "₹${netSavings.toInt()}"
        val udhaarStr = "₹${pendingUdhaar.toInt()}"

        return """
You are SahayakAI (सहायक AI), a trusted, highly practical business growth advisor & financial mentor dedicated to rural and semi-urban Indian micro-entrepreneurs.

CURRENT ENTREPRENEUR & BUSINESS PROFILE (CRITICAL CONTEXT):
- Entrepreneur / Owner Name: $ownerName
- Business / Shop Name: $shopName
- Business Category / Trade: $tradeTitle (Type: ${profile.businessType.name})
- Location: $locationStr (Pincode: ${profile.pincode.ifBlank { "221001" }})
- Monthly Business Turnover: $turnoverStr / month
- Daily Footfall: ${if (profile.dailyCustomers > 0) profile.dailyCustomers else 85} customers/day
- Self-Help Group (SHG) / Mandal: ${profile.shgName.ifBlank { "Local Vyapar Samiti" }}
- Digitize Ledger Financials:
  * Recorded Income/Sales: ₹${totalCredit.toInt()}
  * Recorded Expenses/Outflow: ₹${totalDebit.toInt()}
  * Net Monthly Savings: $savingsStr
  * Pending Customer Udhaar: $udhaarStr (across $transactionCount entries)
- Key Operational Inputs: ${trade.nonAgmarknetInputs.joinToString(", ")}
- Best Applicable Govt Schemes: ${trade.applicableSchemes.joinToString(", ")}

$commoditiesContext
$excludeNotice

CRITICAL RESPONSE RULES:
1. PERSONAL RECOGNITION & RESPECT:
   - Greet the user respectfully by their registered name ($ownerName) and acknowledge their business ($shopName - $tradeTitle).
   - If asked about their identity, name, or business status, accurately present their profile: Owner ($ownerName), Business ($shopName), Category ($tradeTitle), Location ($locationStr), Monthly Turnover ($turnoverStr), Net Savings ($savingsStr), and Pending Udhaar ($udhaarStr).

2. THOROUGH, IN-DEPTH, AND PROPERLY EXPLAINED ADVISORY:
   - Take your time to provide a deep, well-rounded, and thorough answer. Do NOT artificially rush, truncate, or cut short your advice.
   - For business growth, profits, or expansion ("how to grow / बिज़नेस कैसे बढ़ाएं"):
     * Provide detailed, practical guidance tailored specifically to $tradeTitle.
     * High-Margin Opportunities: Specific products, combos, and offerings that increase average ticket size and margins.
     * Working Capital & Udhaar Strategy: Clear steps to recover pending customer udhaar (currently $udhaarStr) and manage daily cash flow without offending regular patrons.
     * Cost & Inventory Optimization: Prudent sourcing of key daily inputs (${trade.nonAgmarknetInputs.take(4).joinToString(", ")}).
     * Government Financial Support: Concrete eligibility, step-by-step application advice, and benefits of applicable schemes (${trade.applicableSchemes.joinToString("; ")}).

3. LANGUAGE & STRUCTURE:
   - Answer in $langName.
   - If Hindi: Use warm, encouraging, respectful, and natural Devanagari Hindi (हिन्दी) with relatable business terms that Indian micro-entrepreneurs easily understand and appreciate.
   - Structure your response properly with clear section headings, bullet points, and practical examples so it is comprehensive, pleasant, and easy to follow.

USER QUESTION:
"$query"
""".trimIndent()
    }

    /**
     * Trade metadata knowledge base.
     */
    private fun getTradeMetadata(type: BusinessType): TradeMetadata {
        return when (type) {
            BusinessType.FOOD_STALL -> TradeMetadata(
                title = "Tea Shop & Snack Stall",
                titleHi = "चाय दुकान व नाश्ता",
                relevantCommodities = listOf("Tea", "Sugar", "Ginger"),
                nonAgmarknetInputs = listOf("CTC tea leaves", "dairy milk", "refined sugar", "fresh ginger (adrak)", "green cardamom", "commercial LPG cylinder", "kulhad & paper cups", "rusks & snacks"),
                excludeTopics = listOf("heavy agricultural machinery", "tractor purchase", "crop sowing", "textile fabrics", "cattle breeding", "pesticides"),
                applicableSchemes = listOf("PM SVANidhi (₹10k to ₹50k collateral-free loan with 7% interest subsidy)", "PM Mudra Shishu (Up to ₹50,000)"),
                focusAreas = listOf("Daily morning footfall", "tea margin optimization", "inventory buffer of sugar/tea leaves", "quick digital QR payments")
            )
            BusinessType.KIRANA -> TradeMetadata(
                title = "Kirana & Grocery Store",
                titleHi = "किराना व परचून दुकान",
                relevantCommodities = listOf("Sugar", "Mustard Oil", "Wheat", "Chana Dal", "Onion", "Potato"),
                nonAgmarknetInputs = listOf("FMCG packaged items", "cooking oil", "pulses & grains", "spices & salt", "cleaning goods", "distributor credit terms"),
                excludeTopics = listOf("tailoring cloth", "handicraft clay", "tractor hire", "cattle feed"),
                applicableSchemes = listOf("PM Mudra Yojana (Shishu up to ₹50k, Kishor up to ₹5 Lakh)", "PM SVANidhi", "Digital Khata QR"),
                focusAreas = listOf("Inventory stock turnover", "reducing dead stock", "customer udhaar recovery", "wholesaler bulk discounts")
            )
            BusinessType.STREET_VENDOR -> TradeMetadata(
                title = "Street Vendor & Cart",
                titleHi = "रेहड़ी-पटरी विक्रेता",
                relevantCommodities = listOf("Potato", "Onion", "Tomato", "Green Chilli"),
                nonAgmarknetInputs = listOf("Daily fresh market produce", "cart maintenance", "certified weighing scale", "canopy/umbrella", "municipal vendor certificate"),
                excludeTopics = listOf("tractor", "industrial sewing machines", "large warehousing", "fertilizers"),
                applicableSchemes = listOf("PM SVANidhi (₹10,000 first tranche, ₹20,000 second, ₹50,000 third with 7% interest subsidy)", "PM Mudra Shishu"),
                focusAreas = listOf("Daily cashflow velocity", "avoiding evening distress sales", "securing SVANidhi working capital", "UPI cashback")
            )
            BusinessType.TAILORING -> TradeMetadata(
                title = "Tailoring & Garments",
                titleHi = "सिलाई व परिधान",
                relevantCommodities = emptyList(),
                nonAgmarknetInputs = listOf("Fabric meters (cotton, silk, synthetic)", "sewing thread spools", "zippers & buttons", "tailoring shears", "electric motor attachment", "steam iron", "machine oil"),
                excludeTopics = listOf("crop farming", "onion mandi prices", "mustard oil market", "wheat sacks", "fertilizer subsidy", "cattle feed"),
                applicableSchemes = listOf("PM Vishwakarma Yojana (₹15,000 modern toolkit grant + collateral-free credit at 5% interest)", "PM Mudra Shishu/Kishor"),
                focusAreas = listOf("Festive season advance bookings", "school uniform contracts", "upgrading to motorized sewing machines", "recovering alteration dues")
            )
            BusinessType.DAIRY_FARMING -> TradeMetadata(
                title = "Dairy & Livestock Enterprise",
                titleHi = "डेयरी व पशुपालन",
                relevantCommodities = emptyList(),
                nonAgmarknetInputs = listOf("Cattle feed (khal, churi, green fodder)", "veterinary medicines & vaccines", "stainless steel milk cans", "fat testing kit", "animal shelter"),
                excludeTopics = listOf("tailoring fabric", "street food stall", "vegetable cart"),
                applicableSchemes = listOf("Kisan Credit Card (Pashupalan KCC up to ₹2 Lakh at 4% effective interest)", "NABARD Dairy Entrepreneurship Scheme", "PM Mudra"),
                focusAreas = listOf("Optimizing feed cost to milk yield ratio", "direct dairy cooperative supply", "preventive seasonal vaccination")
            )
            BusinessType.AGRICULTURE -> TradeMetadata(
                title = "Small Farmer & Agriculture",
                titleHi = "लघु किसान व कृषि",
                relevantCommodities = listOf("Wheat", "Mustard Oil", "Chana Dal", "Potato"),
                nonAgmarknetInputs = listOf("Certified seeds", "Urea & DAP fertilizers", "organic compost", "drip irrigation pipes", "tractor rental"),
                excludeTopics = listOf("tailoring threads", "street food stall", "artisan clay"),
                applicableSchemes = listOf("PM-KISAN (₹6,000 annual direct support)", "Kisan Credit Card (KCC)", "PM Fasal Bima Yojana (PMFBY)"),
                focusAreas = listOf("Input cost reduction", "soil health testing", "selling at APMC mandi minimum support price (MSP)")
            )
            BusinessType.HANDICRAFTS -> TradeMetadata(
                title = "Artisan & Handicrafts",
                titleHi = "हस्तशिल्प व कारीगर",
                relevantCommodities = emptyList(),
                nonAgmarknetInputs = listOf("Raw crafting clay / brass / wood", "carving & shaping chisels", "natural pigment dyes", "kiln fuel", "exhibition stall registration"),
                excludeTopics = listOf("agriculture tractor", "vegetable mandi prices", "crop sowing"),
                applicableSchemes = listOf("PM Vishwakarma Yojana (₹15,000 toolkit incentive + ₹1 Lakh 1st tranche loan at 5% interest)", "Ambedkar Hastshilp Vikas Yojana"),
                focusAreas = listOf("Direct market exhibitions (SARAS/Hunar Haat)", "GI Tag certification", "packaging and digital cataloging")
            )
            BusinessType.OTHER -> TradeMetadata(
                title = "Micro Enterprise",
                titleHi = "सूक्ष्म उद्यम",
                relevantCommodities = emptyList(),
                nonAgmarknetInputs = listOf("Commercial supplies", "shop fixtures", "digital signage", "local distribution contacts"),
                excludeTopics = emptyList(),
                applicableSchemes = listOf("PM Mudra Yojana (PMMY)", "PM SVANidhi", "PMEGP (Up to 35% rural subsidy)"),
                focusAreas = listOf("Cashflow discipline", "working capital management", "customer retention")
            )
        }
    }

    private fun getMandiForecastsSnippet(commodities: List<String>): String {
        if (commodities.isEmpty()) {
            return "COMMODITY CONSTRAINTS: This enterprise uses commercial/manufactured inputs. No agricultural mandi commodities apply. Focus on wholesale commercial rates and trade discounts."
        }

        val forecasts = mapOf(
            "Tea" to "CTC Tea Leaf: Spot ₹240/kg | 15-Day Forecast: ₹258/kg (+7.5%, Bullish) | Advice: Pre-monsoon auction prices rising. Secure 15-20 days wholesale buffer.",
            "Sugar" to "Refined M-30 Sugar: Spot ₹41/kg | 15-Day Forecast: ₹41.5/kg (Stable) | Advice: Steady mill deliveries; purchase regular 50kg bag without speculative hoarding.",
            "Ginger" to "Fresh Ginger: Spot ₹65/kg | 15-Day Forecast: ₹60/kg (-7.7%, Softening) | Advice: Fresh regional arrivals increasing; purchase in smaller 3-5kg lots to avoid shrinkage.",
            "Onion" to "Red Onion: Spot ₹28/kg | 15-Day Forecast: ₹34/kg (+21.4%, Bullish) | Advice: Depleting storage supply; maintain dry ventilated 100kg stock.",
            "Potato" to "Jyoti Potato: Spot ₹19/kg | 15-Day Forecast: ₹19.2/kg (Stable) | Advice: Cold storage releases steady; buy on regular weekly cycle.",
            "Tomato" to "Tomato: Spot ₹22/kg | 15-Day Forecast: ₹18.0/kg (-18.2%, Bearish) | Advice: Peak harvest supply; buy strictly for daily 1-day turnover.",
            "Mustard Oil" to "Mustard Oil: Spot ₹138/Liter | 15-Day Forecast: ₹142/L (Stable) | Advice: Expeller crushing steady; procure standard 15-liter tins.",
            "Wheat" to "Clean Sharbati Wheat: Spot ₹26.5/kg | 15-Day Forecast: ₹27.0/kg (Stable) | Advice: Maintain 30-day clean dry storage.",
            "Chana Dal" to "Chana Dal: Spot ₹76/kg | 15-Day Forecast: ₹79/kg (+3.9%, Bullish) | Advice: Festive wholesale demand picking up; secure buffer."
        )

        val lines = mutableListOf("TRACKED WHOLESALE BENCHMARKS (Strictly filtered for this trade):")
        for (c in commodities) {
            forecasts[c]?.let { lines.add("- $it") }
        }
        return lines.joinToString("\n")
    }

    private fun validateTradeRelevance(response: String, trade: TradeMetadata): List<String> {
        val lower = response.lowercase()
        val violations = mutableListOf<String>()
        for (topic in trade.excludeTopics) {
            if (lower.contains(topic.lowercase())) {
                violations.add(topic)
            }
        }
        return violations
    }

    /**
     * Local Offline Knowledge Base (Acts when the device has NO internet connectivity)
     */
    private fun generateLocalOfflineFallback(
        query: String,
        profile: UserProfile,
        isHindi: Boolean,
        pendingUdhaar: Double,
        trade: TradeMetadata
    ): String {
        val q = query.lowercase()
        val ownerName = profile.name.ifBlank { "Ramesh Kumar Sharma" }
        val shopName = profile.businessName.ifBlank { "${ownerName}'s ${trade.title}" }
        val locationStr = "${profile.location.ifBlank { "Varanasi" }}, ${profile.state.ifBlank { "Uttar Pradesh" }}"
        val turnoverVal = profile.monthlyTurnover.toInt()

        // 1. Personal Identity & Profile Query
        if (q.contains("naam") || q.contains("name") || q.contains("who am i") || q.contains("mera") || q.contains("profile") || q.contains("dukandar")) {
            return if (isHindi) {
                "👤 **आपकी व्यक्तिगत व व्यापार प्रोफ़ाइल:**\n\n" +
                        "• **उद्यमी नाम:** $ownerName\n" +
                        "• **दुकान / उद्यम:** $shopName\n" +
                        "• **व्यवसाय श्रेणी:** ${trade.titleHi} (${trade.title})\n" +
                        "• **स्थान:** $locationStr\n" +
                        "• **मासिक टर्नओवर:** ₹$turnoverVal\n" +
                        "• **डिजिटल बहीखाता:** बकाया उधार ₹${pendingUdhaar.toInt()} दर्ज है।"
            } else {
                "👤 **Your Registered Business Profile:**\n\n" +
                        "• **Entrepreneur Name:** $ownerName\n" +
                        "• **Enterprise:** $shopName\n" +
                        "• **Trade Category:** ${trade.title}\n" +
                        "• **Location:** $locationStr\n" +
                        "• **Monthly Turnover:** ₹$turnoverVal\n" +
                        "• **Digitized Khata:** Pending customer udhaar ₹${pendingUdhaar.toInt()}."
            }
        }

        // 2. Business Growth & Scaling Query
        if (q.contains("grow") || q.contains("badha") || q.contains("badhana") || q.contains("badhaun") || q.contains("munafa") || q.contains("profit") || q.contains("scale") || q.contains("tarraki")) {
            return if (isHindi) {
                "📈 **नमस्ते $ownerName जी! $shopName (${trade.titleHi}) के लिए सम्पूर्ण व्यापार व मुनाफा वृद्धि योजना:**\n\n" +
                        "### 1. उच्च-मुनाफे (High-Margin) वाले उत्पाद जोड़ें:\n" +
                        "• मुख्य सामानों (${trade.nonAgmarknetInputs.take(3).joinToString(", ")}) के साथ कॉम्बो ऑफर और प्रीमियम विकल्प रखें जिनमें 25% से 35% शुद्ध मुनाफा मिले।\n" +
                        "• काउंटर पर फास्ट-मूविंग स्नैक्स या रोजमर्रा की उपभोग्य वस्तुएं प्रदर्शित करें जिससे हर ग्राहक का औसत बिल साइज बढ़े।\n\n" +
                        "### 2. उधारी (₹${pendingUdhaar.toInt()}) की चरणबद्ध वसूली:\n" +
                        "• आपके ₹${pendingUdhaar.toInt()} ग्राहकों के पास अटके हैं। नए ग्राहकों को उधार सीमित करें और पुराने उधार पर SahayakAI से विनम्र WhatsApp रिमाइंडर भेजें।\n" +
                        "• यह वसूल की गई राशि आपकी दुकान की वर्किंग कैपिटल बनेगी और आपको थोक डिस्काउंट दिलाने में मदद करेगी।\n\n" +
                        "### 3. डिजिटल भुगतान व लॉयल्टी:\n" +
                        "• दुकान पर प्रमुखता से UPI QR कोड लगाएं। डिजिटल लेनदेन से बैंक में आपका क्रेडिट स्कोर बनता है जिससे भविष्य में लोन आसानी से पास होता है।\n\n" +
                        "### 4. सरकारी योजना से पूंजी विस्तार:\n" +
                        "• **${trade.applicableSchemes.firstOrNull() ?: "PM Mudra Shishu"}** के तहत ₹50,000 की बिना गारंटी पूंजी लेकर नया माल व उपकरण जोड़ें।"
            } else {
                "📈 **Greetings $ownerName! Comprehensive Growth & Profit Plan for $shopName (${trade.title}):**\n\n" +
                        "### 1. High-Margin Product & Combo Expansion:\n" +
                        "• Introduce higher-margin bundled products alongside your standard items (${trade.nonAgmarknetInputs.take(3).joinToString(", ")}) targeting 25-35% gross profit.\n" +
                        "• Use counter-top placement for fast-moving items to increase your average transaction ticket.\n\n" +
                        "### 2. Udhaar Recovery & Capital Rotation (₹${pendingUdhaar.toInt()}):\n" +
                        "• Recover the ₹${pendingUdhaar.toInt()} pending customer credit using SahayakAI digital reminders.\n" +
                        "• Rotating this locked capital into fast-moving inventory allows you to purchase at bulk wholesale discounts.\n\n" +
                        "### 3. Digital UPI & Bank Footprint:\n" +
                        "• Encourage QR payments to maintain an active digital transaction trail, qualifying you for prioritized bank credit.\n\n" +
                        "### 4. Expansion Capital via Government Schemes:\n" +
                        "• Leverage **${trade.applicableSchemes.firstOrNull() ?: "PM Mudra Shishu"}** to access collateral-free working capital."
            }
        }

        // 3. Mudra Scheme
        if (q.contains("mudra") || q.contains("मुद्रा") || q.contains("loan") || q.contains("लोन") || q.contains("कर्ज")) {
            return if (isHindi) {
                "🏦 **प्रधानमंत्री मुद्रा योजना (PMMY) - ऑफ़लाइन गाइड:**\n\n" +
                        "1. **शिशु लोन:** ₹50,000 तक (नया सामान/उपकरण खरीदने हेतु, शून्य गारंटी)।\n" +
                        "2. **किशोर लोन:** ₹50,000 से ₹5 लाख तक (दुकान विस्तार हेतु)।\n" +
                        "3. **तरुण लोन:** ₹5 लाख से ₹10 लाख तक।\n\n" +
                        "💡 **आवेदन प्रक्रिया:** आधार कार्ड, पैन कार्ड, दुकान का प्रमाण व SahayakAI से जनरेटेड 'डिजिटल खाता रिपोर्ट' लेकर नजदीकी ग्रामीण या सरकारी बैंक में जाएं।"
            } else {
                "🏦 **PM Mudra Yojana (PMMY) - Offline Guide:**\n\n" +
                        "1. **Shishu Loan:** Up to ₹50,000 (For micro tools/inventory, zero collateral).\n" +
                        "2. **Kishor Loan:** ₹50,000 to ₹5,00,000 (For shop expansion).\n" +
                        "3. **Tarun Loan:** ₹5 Lakh to ₹10 Lakh.\n\n" +
                        "💡 **Application:** Visit your nearest rural or commercial bank with Aadhaar, PAN, and your SahayakAI 'Bank-Ready Report'."
            }
        }

        // 2. SVANidhi Scheme
        if (q.contains("svanidhi") || q.contains("स्वनिधि") || q.contains("vendor") || q.contains("ठेला") || q.contains("रेहड़ी")) {
            return if (isHindi) {
                "🛒 **पीएम स्वनिधि योजना (PM SVANidhi) - ऑफ़लाइन गाइड:**\n\n" +
                        "- **प्रथम किस्त:** ₹10,000 की कार्यशील पूंजी (बिना किसी गारंटी के)।\n" +
                        "- **अगली किश्तें:** समय पर अदायगी करने पर ₹20,000 और ₹50,000 का ऋण उपलब्ध।\n" +
                        "- **7% ब्याज सब्सिडी:** केंद्र सरकार द्वारा सीधे आपके बैंक खाते में जमा की जाती है।\n" +
                        "- डिजिटल लेनदेन (QR कोड) पर ₹1,200 तक का वार्षिक कैशबैक।"
            } else {
                "🛒 **PM SVANidhi Scheme - Offline Guide:**\n\n" +
                        "- **1st Tranche:** ₹10,000 collateral-free working capital loan.\n" +
                        "- **Graduation:** Repaying on time unlocks ₹20,000 and ₹50,000 higher tranches.\n" +
                        "- **7% Interest Subsidy:** Directly credited to your bank account.\n" +
                        "- Earn up to ₹1,200 annual cashback on UPI digital payments."
            }
        }

        // 3. Vishwakarma Scheme
        if (q.contains("vishwakarma") || q.contains("विश्वकर्मा") || q.contains("कारीगर") || q.contains("दर्जी") || q.contains("सिलाई")) {
            return if (isHindi) {
                "🪡 **पीएम विश्वकर्मा योजना (PM Vishwakarma):**\n\n" +
                        "- दर्जी, कारीगर और पारंपरिक हस्तशिल्पियों के लिए विशेष योजना।\n" +
                        "- **₹15,000 टूलकिट अनुदान:** आधुनिक उपकरण/मशीन खरीदने हेतु निशुल्क ग्रांट।\n" +
                        "- **सस्ता ऋण:** प्रथम चरण में ₹1 लाख और दूसरे में ₹2 लाख केवल 5% रियायती ब्याज दर पर।\n" +
                        "- CSC सेंटर पर जाकर आधार व बैंक पासबुक से निशुल्क पंजीकरण कराएं।"
            } else {
                "🪡 **PM Vishwakarma Scheme:**\n\n" +
                        "- Tailored for tailors, artisans, and traditional craftspeople.\n" +
                        "- **₹15,000 Free Toolkit Incentive:** Direct grant for modern tools and electric attachments.\n" +
                        "- **Concessional Credit:** ₹1 Lakh (1st tranche) & ₹2 Lakh (2nd tranche) at only 5% interest.\n" +
                        "- Register at any Common Service Center (CSC) with Aadhaar."
            }
        }

        // 4. Udhaar Recovery
        if (q.contains("udhaar") || q.contains("उधार") || q.contains("recovery") || q.contains("वसूली") || q.contains("बकाया")) {
            return if (isHindi) {
                "📋 **उधार वसूली रणनीति (SahayakAI डिजिटल खाता):**\n\n" +
                        "1. वर्तमान में आपका कुल बकाया उधार **₹${pendingUdhaar.toInt()}** दर्ज है।\n" +
                        "2. **WhatsApp तकादा संदेश:** खाता स्क्रीन से ग्राहक को विनम्र पेमेंट रिमाइंडर लिंक भेजें।\n" +
                        "3. **नियम:** कुल मासिक बिक्री के 20% से अधिक उधार कभी न रखें।\n" +
                        "4. समय पर नकद भुगतान करने वाले ग्राहकों को ₹2-₹5 की छूट दें।"
            } else {
                "📋 **Udhaar Recovery Strategy:**\n\n" +
                        "1. Your total recorded customer credit is **₹${pendingUdhaar.toInt()}**.\n" +
                        "2. Send polite payment reminders directly from SahayakAI Khata via WhatsApp.\n" +
                        "3. Cap customer credit to no more than 20% of your monthly turnover.\n" +
                        "4. Offer a nominal prompt-payment discount to encourage instant UPI settlements."
            }
        }

        // 5. Default trade-tailored offline advice
        return if (isHindi) {
            "🌿 **SahayakAI व्यापार सलाह (${trade.titleHi}):**\n\n" +
                    "नमस्ते ${profile.name.ifBlank { "साथी" }} जी! आपकी **${trade.titleHi}** के लिए:\n" +
                    "• **नकद प्रवाह:** दैनिक बिक्री और खर्च को SahayakAI खाते में दर्ज करते रहें ताकि बैंक लोन हेतु सिविल और वित्तीय रिपोर्ट मजबूत रहे।\n" +
                    "• **स्टॉक प्रबंधन:** मुख्य इनपुट (${trade.nonAgmarknetInputs.take(3).joinToString(", ")}) का उचित बफर रखें।\n" +
                    "• **सरकारी सहायता:** आपके व्यवसाय के लिए **${trade.applicableSchemes.firstOrNull() ?: "PM Mudra Yojana"}** सबसे उपयुक्त है।"
        } else {
            "🌿 **SahayakAI Business Advisory (${trade.title}):**\n\n" +
                    "Hello ${profile.name.ifBlank { "Entrepreneur" }}! Key guidelines for your **${trade.title}**:\n" +
                    "• **Cashflow Management:** Maintain daily ledger records to build a credit-worthy audit trail for collateral-free bank loans.\n" +
                    "• **Input Management:** Keep a steady turnover cycle for ${trade.nonAgmarknetInputs.take(3).joinToString(", ")}.\n" +
                    "• **Government Subsidies:** Leverage **${trade.applicableSchemes.firstOrNull() ?: "PM Mudra Yojana"}** to finance inventory expansion."
        }
    }

    /**
     * Backward-compatibility wrapper for legacy callers.
     */
    suspend fun getAdvice(
        userQuery: String,
        userProfile: UserProfile,
        ledgerEntries: List<LedgerEntry>,
        mandiPrices: List<MandiCommodity> = emptyList(),
        forceOffline: Boolean = false
    ): Pair<String, Boolean> {
        val resp = queryChatbot(userQuery, userProfile, ledgerEntries, forceOffline)
        return Pair(resp.adviceText, resp.isOfflineTier)
    }

    /**
     * Native Android Khata OCR parser integration.
     * Preserves physical line order and queries the backend Gemini 2.5 Flash OCR parser,
     * falling back to local heuristic layout-preserving parser if offline.
     */
    suspend fun structureKhataTextWithGemini(rawRows: List<String>): List<OcrParsedItem> = withContext(Dispatchers.IO) {
        val joinedText = rawRows.joinToString("\n").trim()
        if (joinedText.isBlank()) return@withContext emptyList()

        val backendUrls = listOf(
            "$BACKEND_URL_EMULATOR/api/v1/khata/parse-ocr",
            "$BACKEND_URL_LOCALHOST/api/v1/khata/parse-ocr",
            "$BACKEND_URL_HOST/api/v1/khata/parse-ocr"
        )

        for (url in backendUrls) {
            try {
                val reqJson = JSONObject().apply {
                    put("raw_text", joinedText)
                    put("business_type", "KIRANA")
                }
                val request = Request.Builder()
                    .url(url)
                    .post(reqJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val jsonArray = JSONArray(body)
                            val items = mutableListOf<OcrParsedItem>()
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val partyName = obj.optString("party_name", "Khata Customer")
                                val desc = obj.optString("description", "Item")
                                val amt = obj.optDouble("amount", 0.0)
                                val typeStr = obj.optString("type", "CREDIT")
                                val type = if (typeStr.equals("DEBIT", ignoreCase = true)) LedgerType.DEBIT else LedgerType.CREDIT
                                val catStr = obj.optString("category", "SALES")
                                val cat = try {
                                    LedgerCategory.valueOf(catStr)
                                } catch (e: Exception) {
                                    if (type == LedgerType.CREDIT) LedgerCategory.SALES else LedgerCategory.CUSTOMER_UDHAAR
                                }
                                val date = obj.optString("date", "")
                                val conf = obj.optDouble("confidence", 0.95).toFloat()
                                items.add(
                                    OcrParsedItem(
                                        partyName = partyName,
                                        description = desc,
                                        amount = amt,
                                        type = type,
                                        category = cat,
                                        date = date,
                                        rawText = "$partyName $desc $amt",
                                        confidence = conf
                                    )
                                )
                            }
                            if (items.isNotEmpty()) {
                                Log.i(TAG, "Backend OCR parsing succeeded with ${items.size} items")
                                return@withContext items
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed connecting to backend OCR at $url: ${e.message}")
            }
        }

        return@withContext OcrKhataParser().parseRowsPreservingLayout(rawRows)
    }
}
