"""
BUSINESS_PROFILES Configuration (Single Source of Truth in Python)
Defines enterprise operational parameters, Agmarknet tracked commodities,
non-Mandi operational inputs, advisory scope, and forbidden-topics guardrails
for every micro-enterprise category.
"""

from typing import List, Dict, Any, Optional
import logging
import re

logger = logging.getLogger("business_profiles")

class BusinessProfile:
    def __init__(
        self,
        business_type: str,
        primary_title: str,
        primary_title_hi: str,
        relevant_commodities: List[str],
        non_agmarknet_inputs: List[str],
        advisory_topics: List[str],
        exclude_topics: List[str],
        applicable_schemes: List[str],
        operational_summary: str,
        operational_summary_hi: str
    ):
        self.business_type = business_type
        self.primary_title = primary_title
        self.primary_title_hi = primary_title_hi
        self.relevant_commodities = relevant_commodities
        self.non_agmarknet_inputs = non_agmarknet_inputs
        self.advisory_topics = advisory_topics
        self.exclude_topics = exclude_topics
        self.applicable_schemes = applicable_schemes
        self.operational_summary = operational_summary
        self.operational_summary_hi = operational_summary_hi

    def to_dict(self) -> Dict[str, Any]:
        return {
            "business_type": self.business_type,
            "primary_title": self.primary_title,
            "primary_title_hi": self.primary_title_hi,
            "relevant_commodities": self.relevant_commodities,
            "non_agmarknet_inputs": self.non_agmarknet_inputs,
            "advisory_topics": self.advisory_topics,
            "exclude_topics": self.exclude_topics,
            "applicable_schemes": self.applicable_schemes,
            "operational_summary": self.operational_summary,
            "operational_summary_hi": self.operational_summary_hi
        }

    def get_offline_advice(self, query: str, user_name: str, location: str, is_hindi: bool = True) -> str:
        q = query.lower()
        if is_hindi:
            if "loan" in q or "लोन" in q or "मुद्रा" in q or "पैसा" in q or "स्कीम" in q or "योजना" in q:
                schemes_str = " व ".join(self.applicable_schemes)
                return (
                    f"🏦 **{self.primary_title_hi} के लिए सरकारी सहायता:**\n\n"
                    f"नमस्ते {user_name} जी, आपके व्यवसाय के लिए **{schemes_str}** सबसे उपयुक्त हैं।\n"
                    f"- **PM SVANidhi:** ₹10,000 का प्रारंभिक कार्यशील पूंजी ऋण (7% ब्याज सब्सिडी, बिना गारंटी)।\n"
                    f"- **PMMY मुद्रा शिशु:** ₹50,000 तक दुकान के नए उपकरण, गैस चूल्हा व सामग्री के लिए।\n\n"
                    f"💡 *सलाह:* बैंक में अपने SahayakAI डिजिटल खाते का 'बैंक-रेडी सारांश' प्रस्तुत करें।"
                )
            return (
                f"☕ **{self.primary_title_hi} व्यापार सलाह ({location}):**\n\n"
                f"1. **कच्चा माल व लागत नियंत्रण:**\n"
                f"   - दूध की दैनिक आपूर्ति के लिए स्थानीय डेयरी किसानों से सीधा मासिक अनुबंध करें (रिटेल पाउच की तुलना में ₹4-6/लीटर की बचत)।\n"
                f"   - कमर्शियल 19kg एलपीजी सिलेंडर बर्नर की साप्ताहिक सफाई करें ताकि गैस की 8-12% खपत कम हो।\n"
                f"   - डिस्पोजेबल पेपर कप और कुल्हड़ हमेशा 1,000-5,000 के थोक कार्टन में खरीदें।\n\n"
                f"2. **मुनाफा व नकद प्रवाह:**\n"
                f"   - चाय के साथ हाई-मार्जिन नाश्ता (समोसा, बन-मक्खन, बिस्कुट) रखें जिससे प्रति ग्राहक औसत बिल ₹10 से बढ़कर ₹25+ हो।\n"
                f"   - नियमित ग्राहकों को केवल साप्ताहिक सीमा (अधिकतम ₹200) तक ही उधार दें।"
            ) if self.business_type == "FOOD_STALL" else (
                f"🏪 **{self.primary_title_hi} व्यवसाय सलाह ({location}):**\n\n"
                f"1. **मुख्य इनपुट:** {', '.join(self.non_agmarknet_inputs[:3])}\n"
                f"2. **कार्यशील पूंजी:** उधार की सीमा कुल मासिक बिक्री के 15-20% से कम रखें।\n"
                f"3. **वित्तीय सहायता:** {', '.join(self.applicable_schemes)} के तहत रियायती ऋण का लाभ उठाएं।"
            )
        else:
            if "loan" in q or "scheme" in q or "mudra" in q or "credit" in q:
                schemes_str = " and ".join(self.applicable_schemes)
                return (
                    f"🏦 **Government Credit for {self.primary_title}:**\n\n"
                    f"Hello {user_name}, recommended schemes: **{schemes_str}**.\n"
                    f"- **PM SVANidhi:** ₹10,000 collateral-free working capital loan at 7% interest subsidy.\n"
                    f"- **PM Mudra Shishu:** Up to ₹50,000 for equipment, stoves, and commercial inventory.\n\n"
                    f"💡 *Tip:* Carry your SahayakAI digitized ledger report to your local bank branch."
                )
            return (
                f"☕ **{self.primary_title} Advisory ({location}):**\n\n"
                f"1. **Input Cost Optimization:**\n"
                f"   - Procure fresh milk directly from local dairy farmers on monthly settlement to save ₹4-6/L vs retail pouches.\n"
                f"   - Maintain commercial 19kg LPG burners weekly to reduce fuel wastage by 8-12%.\n"
                f"   - Purchase paper cups, kulhad, and tea leaves in wholesale bulk bundles (1,000-5,000 units).\n\n"
                f"2. **Margin Maximization:**\n"
                f"   - Pair tea with high-margin snacks (bun maska, samosas, packaged biscuits) to raise average customer spend from ₹10 to ₹25+.\n"
                f"   - Cap customer udhaar credit at ₹200 per regular patron with strict weekly settlement."
            ) if self.business_type == "FOOD_STALL" else (
                f"🏪 **{self.primary_title} Business Advisory ({location}):**\n\n"
                f"1. **Key Operational Inputs:** {', '.join(self.non_agmarknet_inputs[:3])}\n"
                f"2. **Working Capital:** Cap customer credit at 15-20% of monthly sales.\n"
                f"3. **Financial Support:** Explore {', '.join(self.applicable_schemes)} for collateral-free credit."
            )

BUSINESS_PROFILES: Dict[str, BusinessProfile] = {
    "FOOD_STALL": BusinessProfile(
        business_type="FOOD_STALL",
        primary_title="Tea Shop / Tea Stall",
        primary_title_hi="चाय दुकान व नाश्ता",
        relevant_commodities=["Tea", "Sugar", "Ginger"],
        non_agmarknet_inputs=[
            "Fresh Milk (Buffalo/Cow daily delivery)",
            "Commercial LPG Cylinder (19kg red/blue)",
            "Disposable Paper Cups & Earthen Kulhad",
            "Cardamom (Elaichi), Clove & Chai Masala",
            "Biscuits, Rusk, Samosa & Snack inventory"
        ],
        advisory_topics=[
            "Direct local dairy farmer tie-up for daily unadulterated milk",
            "Commercial LPG burner maintenance and heat regulation to reduce fuel waste",
            "Bulk wholesale procurement of CTC tea leaves (Assam/Dooars) in 5kg-20kg sacks",
            "Bulk purchase of 100ml/150ml disposable paper cups and kulhads by the carton",
            "Margin expansion via snack bundling (bun maska, samosa, biscuits)",
            "Cash flow management and restricting loose customer credit to strict weekly settlement"
        ],
        exclude_topics=[
            "onion", "pyaz", "प्याज",
            "mustard oil", "sarson", "सरसों",
            "dal", "chana", "दाल", "चना",
            "wheat", "gehun", "गेहूं", "aata", "आटा",
            "grain sacks", "rice sacks", "चावल",
            "fertilizer", "खाद", "यूरिया",
            "pesticide", "कीटनाशक",
            "tractor", "बीज", "seed varieties"
        ],
        applicable_schemes=["PM SVANidhi", "PM Mudra Shishu", "FSSAI Street Food Registration"],
        operational_summary="Fast-turnover micro beverage & snack business relying on daily fresh dairy, commercial LPG fuel, wholesale CTC tea leaves, sugar, and disposable cups.",
        operational_summary_hi="दैनिक ताजे दूध, कमर्शियल एलपीजी गैस, थोक सीटीसी चायपत्ती, चीनी व डिस्पोजेबल कुल्हड़ पर आधारित त्वरित दैनिक नकद कारोबार।"
    ),
    "KIRANA": BusinessProfile(
        business_type="KIRANA",
        primary_title="Kirana / Grocery Retail",
        primary_title_hi="किराना दुकान",
        relevant_commodities=["Wheat", "Onion", "Potato", "Tomato", "Mustard Oil", "Sugar", "Chana Dal"],
        non_agmarknet_inputs=[
            "Packaged FMCG Goods (Soap, Detergent, Toothpaste)",
            "Branded Spices & Packaged Edible Oils",
            "Counter Weighing Scale & Display Shelving"
        ],
        advisory_topics=[
            "Wholesale APMC Mandi bulk purchasing for staple grains and pulses",
            "FMCG distributor credit terms and volume discount margins",
            "Customer credit (udhaar) limits and WhatsApp digital reminders",
            "Stock turnover velocity and preventing expiry losses on packaged goods"
        ],
        exclude_topics=[
            "tailoring thread", "sewing machine needle", "bobbin",
            "silage", "cattle feed", "tractor implement", "pottery clay"
        ],
        applicable_schemes=["PM Mudra Kishor/Tarun", "CGTMSE", "PM Vishwakarma (Retail tools)"],
        operational_summary="Multi-SKU dry grocery and FMCG store with staple commodities, packaged consumer goods, and customer credit ledger.",
        operational_summary_hi="अनाज, दाल, तेल, चीनी और एफएमसीजी पैकेटबंद सामानों का बहु-उत्पाद खुदरा स्टोर।"
    ),
    "STREET_VENDOR": BusinessProfile(
        business_type="STREET_VENDOR",
        primary_title="Street Vendor / Thela Cart",
        primary_title_hi="फेरीवाला / ठेला व पटरी",
        relevant_commodities=["Potato", "Tomato", "Onion", "Green Chilli", "Ginger"],
        non_agmarknet_inputs=[
            "Handcart / Mobile Thela maintenance",
            "Weighing balance & weights calibration",
            "Plastic / Jute carrying bags",
            "Tarpaulin shade / lighting battery"
        ],
        advisory_topics=[
            "Early morning 4 AM mandi wholesale lot procurement",
            "Perishable goods daily liquidation to prevent distress rot",
            "Municipal vending certificates & Town Vending Committee (TVC) rights",
            "PM SVANidhi working capital access with UPI cashback"
        ],
        exclude_topics=[
            "fertilizer subsidy", "pesticides", "tractor loan",
            "commercial lpg 19kg", "wholesale cloth bolt"
        ],
        applicable_schemes=["PM SVANidhi", "PM Mudra Shishu"],
        operational_summary="Daily cashflow mobile vending requiring zero-wastage inventory turnover and municipal zone security.",
        operational_summary_hi="प्रतिदिन सुबह मंडी से खरीद कर शाम तक ताजी बिक्री करने वाला दैनिक नकद ठेला कारोबार।"
    ),
    "TAILORING": BusinessProfile(
        business_type="TAILORING",
        primary_title="Tailoring & Garments",
        primary_title_hi="सिलाई व परिधान कार्य",
        relevant_commodities=[],  # No Agmarknet agricultural commodities
        non_agmarknet_inputs=[
            "Sewing Machine Motors & Spare needles",
            "Spools of Thread, Zippers, Buttons & Interlining",
            "Fabric wholesale bolts from textile cloth markets",
            "Steam Iron & Tailoring Scissors"
        ],
        advisory_topics=[
            "Festival season (Eid, Diwali, Wedding) order booking and advance collection",
            "Upgrading from manual pedal to motorized industrial sewing machines",
            "School uniform bulk stitching contracts with local institutions",
            "PM Vishwakarma toolkit incentive of ₹15,000 for master tailors"
        ],
        exclude_topics=[
            "onion", "pyaz", "mustard oil", "sarson", "dal", "chana", "wheat",
            "mandi arrival", "apmc", "fertilizer", "pesticide", "milk procurement"
        ],
        applicable_schemes=["PM Vishwakarma (Tailor/Darzi)", "PM Mudra Shishu", "Stand Up India"],
        operational_summary="Service and customized apparel micro-enterprise dependent on haberdashery wholesale and machine efficiency.",
        operational_summary_hi="सिलाई मशीन, धागे, बटन व कपड़े के थोक थानों पर आधारित सेवा व परिधान सूक्ष्म उद्यम।"
    ),
    "DAIRY_FARMING": BusinessProfile(
        business_type="DAIRY_FARMING",
        primary_title="Dairy & Livestock Farming",
        primary_title_hi="डेयरी व पशुपालन",
        relevant_commodities=["Chana Dal"],  # Protein feed component
        non_agmarknet_inputs=[
            "Green Fodder (Barseem, Napier Grass) & Dry Straw (Bhusa)",
            "Compound Cattle Feed & Mineral Mixture",
            "Veterinary medicines, AI (Artificial Insemination) services",
            "Stainless Steel Milk Cans & Milking hygiene equipment"
        ],
        advisory_topics=[
            "Cooperative dairy vs private vendor milk fat-testing rate parity",
            "Balanced cattle ration formulation to increase SNF and milk yield",
            "NABARD Dairy Entrepreneurship Development Scheme subsidies",
            "Kisan Credit Card (KCC) for animal husbandry working capital"
        ],
        exclude_topics=[
            "fabric wholesale", "sewing machine", "paper cups",
            "restaurant packaging", "kirana fmcg"
        ],
        applicable_schemes=["NABARD DEDS Subsidy", "Kisan Credit Card (Animal Husbandry)", "Pashu Kisan Credit"],
        operational_summary="Livestock husbandry producing daily milk yields dependent on cattle nutrition, veterinary care, and chilling collection.",
        operational_summary_hi="दूध उत्पादन, हरा चारा-भूसा, पशु आहार व दुग्ध समिति आपूर्ति पर आधारित पशुपालन व्यवसाय।"
    ),
    "AGRICULTURE": BusinessProfile(
        business_type="AGRICULTURE",
        primary_title="Small Farmer / Crop Agriculture",
        primary_title_hi="कृषि व लघु कृषक",
        relevant_commodities=["Wheat", "Onion", "Potato", "Tomato", "Mustard Oil", "Chana Dal"],
        non_agmarknet_inputs=[
            "Certified Hybrid Seeds & Seed treatment chemicals",
            "NPK Fertilizers, DAP & Organic Compost",
            "Drip Irrigation / Borewell electricity & Solar pump",
            "Tractor tillage rental and harvest labor"
        ],
        advisory_topics=[
            "e-NAM electronic trading for transparent APMC price realization",
            "Crop diversification away from mono-cropping toward high-value horticulture",
            "PM Fasal Bima Yojana crop insurance claim protocols",
            "Kisan Credit Card crop loan interest subvention"
        ],
        exclude_topics=[
            "tea stall burner", "disposable paper cups", "tailoring needle",
            "grocery fmcg distribution"
        ],
        applicable_schemes=["PM-KISAN", "Kisan Credit Card", "PM Fasal Bima Yojana", "Sub-Mission on Agricultural Mechanization"],
        operational_summary="Seasonal crop production reliant on input procurement, weather resilience, and APMC market access.",
        operational_summary_hi="फसल उत्पादन, बीज, खाद, सिंचाई व कृषि उपज मंडी समिति आधारित मौसमी खेती।"
    ),
    "HANDICRAFTS": BusinessProfile(
        business_type="HANDICRAFTS",
        primary_title="Artisan & Cottage Handicrafts",
        primary_title_hi="हस्तशिल्प व कुटीर उद्योग",
        relevant_commodities=[],  # No Agmarknet agricultural commodities
        non_agmarknet_inputs=[
            "Raw Clay, Brass / Metal sheets, Wood blocks, Bamboo strips",
            "Natural Dyes, Paints, Varnishes & Polishing compounds",
            "Hand Carving Chisels, Potter's wheel, Handloom looms",
            "Export / Retail protective bubble packaging"
        ],
        advisory_topics=[
            "PM Vishwakarma ₹15,000 tool kit incentive and 5% subsidized credit",
            "Direct onboarding onto GeM (Government e-Marketplace) and ONDC",
            "Exhibition participation in SARAS Melas and Dastkar haats",
            "GI (Geographical Indication) tagging for authentic craft pricing"
        ],
        exclude_topics=[
            "onion", "pyaz", "mustard oil", "wheat mandi", "fertilizer",
            "fresh milk procurement", "tea leaf auction"
        ],
        applicable_schemes=["PM Vishwakarma Scheme", "Ambedkar Hastshilp Vikas Yojana", "PMEGP"],
        operational_summary="Handmade craft enterprise relying on indigenous raw materials, artisan toolkits, and heritage exhibition markets.",
        operational_summary_hi="माटी, धातु, काष्ठ या वस्त्र आधारित पारंपरिक हस्तशिल्प व कारीगरी सूक्ष्म उद्योग।"
    ),
    "OTHER": BusinessProfile(
        business_type="OTHER",
        primary_title="Rural Micro Enterprise",
        primary_title_hi="अन्य सूक्ष्म ग्रामीण व्यवसाय",
        relevant_commodities=[],
        non_agmarknet_inputs=[
            "Commercial trade licenses",
            "Wholesale trade inventory",
            "Digital payment soundbox & QR setup"
        ],
        advisory_topics=[
            "Cash flow tracking and separate business vs personal bank accounts",
            "PM Mudra Shishu loan application process",
            "Udyam Micro Registration for collateral-free bank loans",
            "Digital ledger discipline and polite udhaar collection"
        ],
        exclude_topics=[],
        applicable_schemes=["PM Mudra Yojana", "Udyam Registration", "PM SVANidhi"],
        operational_summary="General rural micro-business requiring working capital management, digital payments, and formal credit linkage.",
        operational_summary_hi="दैनिक नकद प्रबंधन व औपचारिक बैंक ऋण लिंकेज पर आधारित सामान्य ग्रामीण सूक्ष्म उद्यम।"
    )
}

def get_profile(business_type: Optional[str]) -> BusinessProfile:
    """
    Flexible single source lookup for BusinessProfile.
    Maps string identifiers (e.g. FOOD_STALL, TEA_STALL, chai, Kirana) to valid profile.
    Strictly forbids falling back to Kirana for Tea Shop or any other distinct trade.
    """
    if not business_type:
        logger.warning("[PROFILE LOOKUP] Missing business_type, using OTHER micro enterprise profile")
        return BUSINESS_PROFILES["OTHER"]

    bt = business_type.upper().strip()

    # Direct match
    if bt in BUSINESS_PROFILES:
        return BUSINESS_PROFILES[bt]

    # Alias matching
    if "TEA" in bt or "CHAI" in bt or "FOOD" in bt or "STALL" in bt or "DHABA" in bt:
        return BUSINESS_PROFILES["FOOD_STALL"]
    elif "KIRANA" in bt or "GROCERY" in bt or "STORE" in bt or "DUKAN" in bt or "PROVISION" in bt:
        return BUSINESS_PROFILES["KIRANA"]
    elif "STREET" in bt or "VENDOR" in bt or "THELA" in bt or "HAWKER" in bt or "CART" in bt:
        return BUSINESS_PROFILES["STREET_VENDOR"]
    elif "TAILOR" in bt or "SILAI" in bt or "GARMENT" in bt or "CLOTH" in bt:
        return BUSINESS_PROFILES["TAILORING"]
    elif "DAIRY" in bt or "MILK" in bt or "COW" in bt or "BUFFALO" in bt or "PASHU" in bt:
        return BUSINESS_PROFILES["DAIRY_FARMING"]
    elif "AGRI" in bt or "FARM" in bt or "KISAN" in bt or "KHETI" in bt or "CROP" in bt:
        return BUSINESS_PROFILES["AGRICULTURE"]
    elif "CRAFT" in bt or "ARTISAN" in bt or "VISHWAKARMA" in bt or "HANDICRAFT" in bt or "POTTERY" in bt:
        return BUSINESS_PROFILES["HANDICRAFTS"]
    else:
        logger.warning(f"[PROFILE LOOKUP] Unknown business_type '{business_type}', defaulting to OTHER")
        return BUSINESS_PROFILES["OTHER"]

def validate_response_relevance(response_text: str, profile: BusinessProfile) -> List[str]:
    """
    Checks if generated response violates forbidden topics for this trade.
    Returns list of violated keywords found.
    """
    if not profile.exclude_topics or not response_text:
        return []

    lower = response_text.lower()
    violations = []

    for topic in profile.exclude_topics:
        # Check boundary or word presence
        pattern = r"\b" + re.escape(topic.lower()) + r"\b"
        if re.search(pattern, lower) or (len(topic) > 4 and topic.lower() in lower):
            violations.append(topic)

    return list(set(violations))
