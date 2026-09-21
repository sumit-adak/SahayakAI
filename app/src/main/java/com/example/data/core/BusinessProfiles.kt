package com.example.data.core

import android.util.Log
import com.example.data.model.BusinessType
import com.example.data.model.UserProfile
import java.util.Locale

/**
 * Enterprise Business Profile Configuration
 * Guarantees every micro-enterprise trade has an explicit advisory scope,
 * tracked Agmarknet commodities, qualitative input guidance, and negative guardrail constraints.
 *
 * Rule: Every BusinessType MUST have an entry. Silent fallbacks to Kirana are strictly forbidden.
 */
data class BusinessProfile(
    val businessType: BusinessType,
    val primaryTitle: String,
    val primaryTitleHi: String,
    val relevantCommodities: List<String>,
    val nonAgmarknetInputs: List<String>,
    val advisoryTopics: List<String>,
    val excludeTopics: List<String>,
    val applicableSchemes: List<String>,
    val offlineAdviceGenerator: (query: String, profile: UserProfile, isHindi: Boolean) -> String
)

object BusinessProfiles {

    private const val TAG = "BusinessProfiles"

    val PROFILES: Map<BusinessType, BusinessProfile> = mapOf(
        BusinessType.FOOD_STALL to BusinessProfile(
            businessType = BusinessType.FOOD_STALL,
            primaryTitle = "Tea Shop / Tea Stall",
            primaryTitleHi = "चाय दुकान व नाश्ता स्टॉल",
            relevantCommodities = listOf("Tea", "Sugar", "Ginger"),
            nonAgmarknetInputs = listOf(
                "Fresh Milk (Buffalo/Cow)",
                "Commercial LPG Cylinder (19kg)",
                "Disposable Paper Cups & Kulhad",
                "Cardamom (Elaichi) & Spices",
                "Biscuits, Rusk & Snacks"
            ),
            advisoryTopics = listOf(
                "daily morning and evening footfall rushes",
                "milk procurement stability with local dairy farmers",
                "commercial LPG cylinder fuel efficiency and burner maintenance",
                "bulk procurement of paper cups and tea leaves to save 15%",
                "repeat customer loyalty and hygiene presentation",
                "managing daily cash flow and small-ticket UPI payments"
            ),
            excludeTopics = listOf(
                "onion", "pyaz", "प्याज",
                "mustard oil", "sarson", "सरसों",
                "dal", "chana", "दाल", "चना",
                "wheat", "gehun", "गेहूं", "aata", "आटा",
                "grain sacks", "rice", "चावल",
                "fabric", "कपड़ा", "सिलाई",
                "fertilizer", "खाद", "यूरिया",
                "livestock feed", "चारा",
                "tractor", "बीज"
            ),
            applicableSchemes = listOf("PM SVANidhi (₹10,000 to ₹50,000)", "PM Mudra Shishu (Up to ₹50,000)"),
            offlineAdviceGenerator = { query, user, isHindi ->
                if (isHindi) {
                    """
                    ☕ **चाय दुकान व नाश्ता (Tea Stall) व्यावसायिक सलाह:**
                    
                    1. **दूध खरीद व लागत नियंत्रण:** खुले पाउच की जगह नजदीकी डेयरी किसान से दैनिक 30-40 लीटर का सीधा अनुबंध करें, जिससे प्रति लीटर ₹3-4 की सीधी बचत होगी।
                    2. **गैस (LPG) व ईंधन दक्षता:** कमर्शियल बर्नर पर रेग्युलेटर की नियमित सफाई करें और भारी पेंदे वाले तांबे/स्टील केटली का उपयोग करें ताकि 10-12% गैस की खपत घटे।
                    3. **थोक खरीद:** सीटीसी चाय पत्ती (CTC Tea) और डिस्पोजेबल कुल्हड़/कप थोक डिस्ट्रीब्यूटर से 15 दिन के बंडल में लें।
                    4. **कार्यशील पूंजी लोन:** कार्यशील पूंजी बढ़ाने के लिए 'पीएम स्वनिधि' (PM SVANidhi) के तहत ₹10,000 - ₹50,000 का बिना गारंटी 7% सब्सिडी वाला ऋण लें।
                    """.trimIndent()
                } else {
                    """
                    ☕ **Tea Shop & Eatery Business Advisory:**
                    
                    1. **Milk Sourcing & Cost Control:** Contract directly with a local dairy farmer for 30-40 daily liters instead of buying retail pouches to save ₹3-4 per liter.
                    2. **LPG Efficiency:** Maintain commercial burners regularly and use insulated, heavy-bottom tea urns to cut fuel consumption by 10-12%.
                    3. **Smart Inventory:** Procure CTC tea leaves in 5kg bulk packs and paper cups/kulhads in 1,000-unit cartons to capture 15% wholesale margin.
                    4. **Working Capital Funding:** Avail PM SVANidhi micro-credit (₹10,000 to ₹50,000 collateral-free with 7% interest subsidy) to upgrade stall counter & billing.
                    """.trimIndent()
                }
            }
        ),

        BusinessType.KIRANA to BusinessProfile(
            businessType = BusinessType.KIRANA,
            primaryTitle = "Kirana / Grocery Store",
            primaryTitleHi = "किराना व जनरल स्टोर",
            relevantCommodities = listOf("Onion", "Wheat", "Rice", "Sugar", "Chana Dal", "Mustard Oil", "Potato"),
            nonAgmarknetInputs = listOf(
                "Packaged FMCG Goods (Soaps, Detergents, Biscuits)",
                "Packaging Poly & Carry Bags",
                "Distributor Credit Accounts"
            ),
            advisoryTopics = listOf(
                "bulk Mandi sourcing of grains and edible oil",
                "shelf-stable inventory rotation (FIFO) to prevent expiry",
                "local wholesale mandi arrival timings",
                "seasonal staples demand shifts (Festivals & Harvest)",
                "udhaar recovery rules (keep under 20% of monthly sales)"
            ),
            excludeTopics = listOf(
                "tea stall cups",
                "milk sourcing contract for stall",
                "fabric cutting", "garment stitching",
                "cattle feed ration", "silage",
                "pottery wheel", "loom"
            ),
            applicableSchemes = listOf("PM Mudra Kishor (₹50,000 to ₹5 Lakh)", "PM Mudra Shishu (Up to ₹50,000)"),
            offlineAdviceGenerator = { query, user, isHindi ->
                if (isHindi) {
                    """
                    🛒 **किराना व जनरल स्टोर सलाह:**
                    
                    1. **थोक मंडी खरीद:** सरसों तेल, गेहूं और दालें सीधे थोक मंडी से 15-दिवसीय चक्र में उठाएं ताकि 8-12% बिचौलिया मार्जिन बचे।
                    2. **फास्ट-मूविंग माल:** दैनिक उपभोग की चीजें (तेल, आटा, मसाले, साबुन) आगे रखें और एक्सपायरी से 30 दिन पहले कॉम्बो ऑफर चलाएं।
                    3. **उधार सीमा:** कुल बिक्री के 20% से अधिक ग्राहक उधार न दें और WhatsApp/SMS तकादा संदेश नियमित भेजें।
                    4. **ऋण योजना:** दुकान के विस्तार हेतु बैंक से 'पीएम मुद्रा किशोर' (₹50,000 से ₹5 लाख) ऋण के लिए आवेदन करें।
                    """.trimIndent()
                } else {
                    """
                    🛒 **Kirana & Grocery Advisory:**
                    
                    1. **Wholesale Procurement:** Source cooking oil, flour, and pulses directly from the wholesale mandi on a 15-day cycle to save 8-12% distributor margins.
                    2. **Fast-Moving Turnover:** Maintain high turnover on essential staples; bundle slow-moving stock with festive grocery combos.
                    3. **Credit (Udhaar) Cap:** Cap outstanding customer credit at 20% of monthly sales and send weekly polite reminders.
                    4. **Credit Facility:** Apply for PM Mudra Kishor (₹50,000 to ₹5 Lakh) at your nearest rural/commercial bank branch.
                    """.trimIndent()
                }
            }
        ),

        BusinessType.STREET_VENDOR to BusinessProfile(
            businessType = BusinessType.STREET_VENDOR,
            primaryTitle = "Street Vendor / Cart",
            primaryTitleHi = "रेहड़ी-पटरी व फल-सब्जी विक्रेता",
            relevantCommodities = listOf("Onion", "Tomato", "Potato", "Green Chilli", "Ginger", "Lemon", "Garlic"),
            nonAgmarknetInputs = listOf(
                "Cart Maintenance & Lighting",
                "Daily Mandi Cartage / Auto Rickshaw Fare",
                "Digital Soundbox / QR Stand"
            ),
            advisoryTopics = listOf(
                "perishability and daily spoilage reduction",
                "early-morning 4:00 AM - 6:00 AM Mandi auction timing",
                "evening clearance discounts to eliminate day-end waste",
                "cart strategic positioning near transit points",
                "PM SVANidhi zero-collateral loan application"
            ),
            excludeTopics = listOf(
                "tea leaves wholesale", "fabric", "tailoring", "grain sacks mill",
                "fertilizer subsidy", "cow breeding"
            ),
            applicableSchemes = listOf("PM SVANidhi (₹10,000, ₹20,000, ₹50,000)", "PM Mudra Shishu"),
            offlineAdviceGenerator = { query, user, isHindi ->
                if (isHindi) {
                    """
                    🍢 **रेहड़ी-पटरी व विक्रेता सलाह:**
                    
                    1. **सुबह की मंडी नीलामी:** ताजी सब्जियां और फल सुबह 4:30 से 6:00 बजे के बीच मंडी में जाकर बोली लगाएं ताकि 15-20% भाव सस्ता मिले।
                    2. **नुकसान (Spoilage) से बचाव:** शाम 7 बजे के बाद पेरीशेबल सामान पर 10-15% का छूट देकर क्लीयरेंस करें ताकि बासी न हो।
                    3. **पीएम स्वनिधि ऋण:** रेहड़ी-पटरी के लिए ₹10,000 का पहला ऋण लें और समय पर चुकाकर ₹20,000 व ₹50,000 की लिमिट पाएं।
                    """.trimIndent()
                } else {
                    """
                    🍢 **Street Vendor Advisory:**
                    
                    1. **Early Mandi Procurement:** Procure fresh produce at the wholesale yard between 4:30 AM and 6:00 AM for 15-20% cheaper auction rates.
                    2. **Zero Wastage Policy:** Run discounted clearance bundles after 7:00 PM for perishables to avoid carry-over spoilage losses.
                    3. **PM SVANidhi Loan:** Register via the PM SVANidhi portal to access ₹10,000 collateral-free working capital with digital cashback.
                    """.trimIndent()
                }
            }
        ),

        BusinessType.TAILORING to BusinessProfile(
            businessType = BusinessType.TAILORING,
            primaryTitle = "Tailoring / Garments",
            primaryTitleHi = "सिलाई व परिधान उद्यम",
            relevantCommodities = emptyList(), // Not tracked on Agmarknet
            nonAgmarknetInputs = listOf(
                "Fabrics, Linings & Aster",
                "Threads, Zippers & Buttons",
                "Sewing Machine Lubricating Oil & Spare Needles",
                "Iron Press & Cutting Shears"
            ),
            advisoryTopics = listOf(
                "bulk fabric sourcing from textile wholesale hubs",
                "seasonal demand planning for wedding and school reopening seasons",
                "sewing machine motorized upgrade to boost daily output by 40%",
                "taking 50% upfront cash advances on custom stitching orders",
                "PM Vishwakarma toolkits and subsidy"
            ),
            excludeTopics = listOf(
                "onion", "pyaz", "प्याज",
                "potato", "आलू", "सब्जी",
                "mustard oil", "सरसों तेल",
                "wheat", "गेहूं", "दाल", "चावल",
                "milk sourcing", "dairy cattle", "खाद"
            ),
            applicableSchemes = listOf("PM Vishwakarma (Darzi / Tailor category)", "PMEGP", "Stand-Up India"),
            offlineAdviceGenerator = { query, user, isHindi ->
                if (isHindi) {
                    """
                    🧵 **सिलाई व परिधान (Tailoring) उद्यम सलाह:**
                    
                    1. **अग्रिम भुगतान (Cash Advance):** हर नए आर्डर पर 50% अग्रिम राशि लें ताकि कपड़ा और धागे की लागत तुरंत सुरक्षित हो जाए।
                    2. **उत्पादन क्षमता:** हाथ की मशीन को इलेक्ट्रिक मोटर में अपग्रेड करें, जिससे दैनिक सिलाई क्षमता 40% बढ़ जाएगी।
                    3. **पीएम विश्वकर्मा योजना:** दर्जी वर्ग के अंतर्गत ₹15,000 का आधुनिक टूलकिट अनुदान और 5% रियायती ब्याज पर ₹1-2 लाख का ऋण प्राप्त करें।
                    """.trimIndent()
                } else {
                    """
                    🧵 **Tailoring & Garment Enterprise Advisory:**
                    
                    1. **Upfront Order Advance:** Collect a mandatory 50% deposit on custom orders to cover fabric, lining, and thread procurement upfront.
                    2. **Equipment Upgrade:** Attach an electric motor to your sewing machine to raise daily output by 40% with reduced physical fatigue.
                    3. **PM Vishwakarma Scheme:** Apply under the 'Darzi' (Tailor) trade for the ₹15,000 modern toolkit grant and 5% subsidized credit up to ₹2 Lakh.
                    """.trimIndent()
                }
            }
        ),

        BusinessType.DAIRY_FARMING to BusinessProfile(
            businessType = BusinessType.DAIRY_FARMING,
            primaryTitle = "Dairy & Livestock",
            primaryTitleHi = "डेयरी व पशुपालन",
            relevantCommodities = listOf("Dairy Fresh Milk", "Fodder / Cattle Feed"),
            nonAgmarknetInputs = listOf(
                "Veterinary Health & Vaccination",
                "Green Fodder (Barseem/Chari) & Dry Straw",
                "Milking Cans & Hygiene Detergents",
                "Mineral Mixture Supplements"
            ),
            advisoryTopics = listOf(
                "milk fat and SNF yield optimization",
                "cattle deworming and seasonal vaccination calendar",
                "direct cooperative or sweet-shop pooling to bypass middleman deductions",
                "Kisan Credit Card (KCC) for Animal Husbandry",
                "silage storage for dry summer months"
            ),
            excludeTopics = listOf(
                "onion", "pyaz", "mustard oil retail", "vegetable cart",
                "garment stitching", "tea leaves bulk", "tailoring"
            ),
            applicableSchemes = listOf("KCC Animal Husbandry (Up to ₹2 Lakh at 4% net interest)", "National Dairy Plan"),
            offlineAdviceGenerator = { query, user, isHindi ->
                if (isHindi) {
                    """
                    🐄 **डेयरी व पशुपालन सलाह:**
                    
                    1. **फैट व उत्पादन वृद्धि:** संतुलित आहार में खल, चोकर और 50 ग्राम खनिज मिश्रण (Mineral Mixture) रोज दें, जिससे दूध का फैट 0.5% बढ़ेगा।
                    2. **सीधी बिक्री:** बिचौलियों के बजाय सीधे स्थानीय चाय दुकानों, हलवाइयों या दुग्ध समिति को आपूर्ति करें ताकि ₹4-6/लीटर अधिक भाव मिले।
                    3. **पशुपालन केसीसी (KCC):** प्रति दुधारू पशु ₹40,000-₹60,000 का केसीसी ऋण केवल 4% प्रभावी ब्याज दर पर उपलब्ध है।
                    """.trimIndent()
                } else {
                    """
                    🐄 **Dairy & Livestock Advisory:**
                    
                    1. **Fat & Yield Optimization:** Supplement daily feed with 50g mineral mixture and green silage to enhance milk fat and solid-not-fat (SNF) percentages.
                    2. **Direct Marketing:** Supply directly to local eateries, tea vendors, and sweet shops to bypass collection center discounts and gain ₹4-6 per liter.
                    3. **KCC Animal Husbandry:** Avail the Kisan Credit Card for livestock working capital (up to ₹2 Lakh at subsidized 4% net interest).
                    """.trimIndent()
                }
            }
        ),

        BusinessType.AGRICULTURE to BusinessProfile(
            businessType = BusinessType.AGRICULTURE,
            primaryTitle = "Small Farmer / Agriculture",
            primaryTitleHi = "लघु किसान व कृषि",
            relevantCommodities = listOf("Wheat", "Onion", "Potato", "Mustard Oil", "Chana Dal"),
            nonAgmarknetInputs = listOf(
                "Certified Quality Seeds",
                "Urea, DAP & NPK Fertilizers",
                "Tube-well Electricity & Diesel",
                "Pesticides & Bio-fertilizers"
            ),
            advisoryTopics = listOf(
                "crop-specific Mandi wholesale price cycles",
                "MSP procurement center dates and registration",
                "harvest timing to avoid post-monsoon glut price crash",
                "soil health testing and fertilizer cost reduction",
                "PM Kisan Samman Nidhi and PM Fasal Bima Yojana"
            ),
            excludeTopics = listOf(
                "garments", "stitching", "tea shop cups", "LPG cylinder for stalls",
                "pottery kiln"
            ),
            applicableSchemes = listOf("PM Kisan Samman Nidhi (₹6,000/yr)", "Kisan Credit Card (KCC)", "PM Fasal Bima"),
            offlineAdviceGenerator = { query, user, isHindi ->
                if (isHindi) {
                    """
                    🌾 **कृषि व किसान सलाह:**
                    
                    1. **मंडी भाव समय:** फसल कटाई के तुरंत बाद भारी आवक के समय न बेचें। ई-नाम (e-NAM) और स्थानीय वेयरहाउस रसीद पर लोन लेकर 3-4 सप्ताह रुककर बेचें।
                    2. **लागत नियंत्रण:** मिट्टी जांच (Soil Test) के अनुसार ही डीएपी/यूरिया डालें, जिससे प्रति एकड़ ₹1,200 की खाद लागत बचेगी।
                    3. **केसीसी व फसल बीमा:** प्राकृतिक आपदा से सुरक्षा हेतु बुवाई के 10 दिन के अंदर पीएम फसल बीमा योजना में फसल दर्ज करें।
                    """.trimIndent()
                } else {
                    """
                    🌾 **Small Farmer & Crop Advisory:**
                    
                    1. **Strategic Mandi Sale:** Avoid distress selling during harvest peaks. Utilize warehouse receipts or e-NAM to hold non-perishable grains for 3-4 weeks for higher realizations.
                    2. **Input Cost Optimization:** Apply chemical fertilizers strictly per soil health card guidelines to save ₹1,200 per acre on unnecessary DAP application.
                    3. **Crop Insurance:** Ensure crop coverage under PM Fasal Bima Yojana within 10 days of sowing to safeguard against drought/unseasonal rains.
                    """.trimIndent()
                }
            }
        ),

        BusinessType.HANDICRAFTS to BusinessProfile(
            businessType = BusinessType.HANDICRAFTS,
            primaryTitle = "Artisan / Handicrafts",
            primaryTitleHi = "हस्तशिल्प व कारीगर",
            relevantCommodities = emptyList(),
            nonAgmarknetInputs = listOf(
                "Raw Clay, Wood, Bamboo or Brass",
                "Natural Dyes, Polishes & Paints",
                "Protective Packaging & Bubble Wrap",
                "Display Stall Banners"
            ),
            advisoryTopics = listOf(
                "raw material collective buying through SHG federations",
                "listing on ONDC, Hunar Haat, and Saras Mela government exhibitions",
                "PM Vishwakarma Artisan toolkit and credit line",
                "festival season decorative gift demand",
                "pricing custom craft pieces based on artisan hours rather than only raw materials"
            ),
            excludeTopics = listOf(
                "onion", "pyaz", "potato", "mustard oil", "wheat", "vegetables", "milk dairy", "fertilizer"
            ),
            applicableSchemes = listOf("PM Vishwakarma (₹15,000 toolkit + 5% loan)", "ODOP (One District One Product)", "PMEGP"),
            offlineAdviceGenerator = { query, user, isHindi ->
                if (isHindi) {
                    """
                    🏺 **हस्तशिल्प व कारीगर (Artisan) सलाह:**
                    
                    1. **कीमत निर्धारण (Pricing):** केवल कच्चे माल पर नहीं, बल्कि अपने हुनर और समय के अनुसार मूल्य तय करें। प्रीमियम फिनिशिंग पर 40% तक मार्जिन मिलता है।
                    2. **सरकारी मेले व हाट:** सरस मेला, हुनर हाट और ओडीओपी (ODOP) स्टॉल में भाग लें, जहां परिवहन सब्सिडी और सीधे ग्राहक मिलते हैं।
                    3. **पीएम विश्वकर्मा योजना:** अपने पारंपरिक शिल्प के तहत पंजीकरण कर ₹15,000 की टूलकिट सहायता और ₹1-3 लाख का रियायती ऋण प्राप्त करें।
                    """.trimIndent()
                } else {
                    """
                    🏺 **Artisan & Handicrafts Advisory:**
                    
                    1. **Value-Based Pricing:** Price products based on artisan labor hours and exclusivity rather than raw material weight alone to secure 40%+ gross margins.
                    2. **Exhibitions & Fair Channels:** Participate in Saras Melas, District ODOP marts, and ONDC channels for direct-to-consumer sales without middleman cuts.
                    3. **PM Vishwakarma Support:** Register to claim the ₹15,000 modern toolkit grant and collateral-free working capital loan at 5% interest.
                    """.trimIndent()
                }
            }
        ),

        BusinessType.OTHER to BusinessProfile(
            businessType = BusinessType.OTHER,
            primaryTitle = "Micro Enterprise",
            primaryTitleHi = "सूक्ष्म उद्यम",
            relevantCommodities = emptyList(),
            nonAgmarknetInputs = listOf("Commercial Premises", "Electricity & Utilities", "Daily Labor / Tools"),
            advisoryTopics = listOf(
                "daily khata bookkeeping to establish bank creditworthiness",
                "cash-to-credit balance management",
                "PM Mudra Shishu loan application",
                "local supplier negotiation"
            ),
            excludeTopics = listOf(
                "onion bulk", "mustard oil mandi", "vegetable spoil", "crop harvest", "milk yield"
            ),
            applicableSchemes = listOf("PM Mudra Shishu", "PM SVANidhi", "PMEGP"),
            offlineAdviceGenerator = { query, user, isHindi ->
                if (isHindi) {
                    """
                    💼 **सूक्ष्म व्यवसाय सलाह:**
                    
                    1. **दैनिक खाता:** हर नकद और उधार लेनदेन को SahayakAI खाता में दर्ज करें ताकि आपका वित्तीय स्कोर बैंक-रेडी बने।
                    2. **उधार वसूली:** नकद बिक्री पर 2-3% की नकद छूट दें ताकि कार्यशील पूंजी अटके नहीं।
                    3. **सरकारी योजना:** काम शुरू करने या बढ़ाने के लिए बिना गारंटी 'पीएम मुद्रा शिशु' लोन के लिए नजदीकी बैंक शाखा में संपर्क करें।
                    """.trimIndent()
                } else {
                    """
                    💼 **Micro Enterprise Advisory:**
                    
                    1. **Daily Bookkeeping:** Consistently record all inflows and customer credit in your SahayakAI digital Khata to build a strong Bank-Ready Health Score.
                    2. **Cashflow Protection:** Restrict customer credit to verified regulars and incentivize immediate digital payments.
                    3. **Government Credit:** Apply for collateral-free PM Mudra Shishu (up to ₹50,000) at your local rural/commercial bank.
                    """.trimIndent()
                }
            }
        )
    )

    init {
        // Enforce the critical rule: every BusinessType MUST have an explicit, non-default profile
        for (type in BusinessType.values()) {
            if (!PROFILES.containsKey(type)) {
                val errorMsg = "CRITICAL BUG: BusinessType ${type.name} is missing from BusinessProfiles.PROFILES!"
                Log.e(TAG, errorMsg)
                throw IllegalStateException(errorMsg)
            }
        }
    }

    /**
     * Resolves profile strictly without silent fallbacks.
     */
    fun getProfile(businessType: BusinessType): BusinessProfile {
        return PROFILES[businessType]
            ?: throw IllegalStateException("CRITICAL BUG: No BusinessProfile configured for ${businessType.name}!")
    }

    /**
     * Flexible lookup that maps free-text or imported strings strictly to known profiles.
     * Logs loudly if an unknown key is provided.
     */
    fun findProfile(identifier: String): BusinessProfile {
        val clean = identifier.trim().lowercase(Locale.ROOT)
        val match = PROFILES.values.firstOrNull { profile ->
            profile.businessType.name.equals(clean, ignoreCase = true) ||
            profile.primaryTitle.lowercase(Locale.ROOT).contains(clean) ||
            profile.primaryTitleHi.lowercase(Locale.ROOT).contains(clean) ||
            clean.contains("tea") && profile.businessType == BusinessType.FOOD_STALL ||
            clean.contains("chai") && profile.businessType == BusinessType.FOOD_STALL ||
            clean.contains("kirana") && profile.businessType == BusinessType.KIRANA ||
            clean.contains("grocery") && profile.businessType == BusinessType.KIRANA ||
            clean.contains("tailor") && profile.businessType == BusinessType.TAILORING ||
            clean.contains("darzi") && profile.businessType == BusinessType.TAILORING ||
            clean.contains("dairy") && profile.businessType == BusinessType.DAIRY_FARMING ||
            clean.contains("farmer") && profile.businessType == BusinessType.AGRICULTURE ||
            clean.contains("artisan") && profile.businessType == BusinessType.HANDICRAFTS ||
            clean.contains("vendor") && profile.businessType == BusinessType.STREET_VENDOR
        }

        if (match == null) {
            Log.e(TAG, "LOUD ALERT: Unknown business identifier '$identifier'! Defaulting strictly to explicit OTHER.")
            return getProfile(BusinessType.OTHER)
        }
        return match
    }

    /**
     * Guardrail: checks if response contains forbidden keywords for this business.
     */
    fun validateResponseRelevance(responseText: String, profile: BusinessProfile): List<String> {
        val violations = mutableListOf<String>()
        val lower = responseText.lowercase(Locale.ROOT)

        for (topic in profile.excludeTopics) {
            val topicLower = topic.lowercase(Locale.ROOT)
            if (topicLower.length <= 3) {
                // Short words require word boundaries
                if (Regex("\\b${Regex.escape(topicLower)}\\b").containsMatchIn(lower)) {
                    violations.add(topic)
                }
            } else {
                if (lower.contains(topicLower)) {
                    violations.add(topic)
                }
            }
        }
        return violations
    }
}
