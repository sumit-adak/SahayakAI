"""
Advisor Service (Server-Side Gemini Reasoning, Guardrails & Response Validation)
Responsible for:
- Constructing context-injected Gemini prompts with strict trade boundaries
- Injecting ML Mandi forecasting only for trade-relevant commodities
- Calling Gemini API (REST / SDK)
- Validating generated responses against forbidden-topics guardrails
- Automated corrective re-prompting and fallback execution
"""

import os
import json
import logging
import urllib.request
import urllib.error
from typing import Tuple, List, Optional

from app.core.business_profiles import get_profile, validate_response_relevance, BusinessProfile
from app.services.forecasting_engine import generate_mandi_prompt_context
from app.schemas.advisor_schema import AdvisorQueryRequest, AdvisorQueryResponse

logger = logging.getLogger("advisor_service")
logging.basicConfig(level=logging.INFO)

# Universal Offline Knowledge Base for standard scheme queries
OFFLINE_SCHEME_KNOWLEDGE = [
    {
        "keywords": ["mudra", "मुद्रा", "loan", "लोन", "कर्ज", "shishu", "kishor", "tarun"],
        "hi": (
            "🏦 **प्रधानमंत्री मुद्रा योजना (PMMY) जानकारी:**\n\n"
            "1. **शिशु लोन:** ₹50,000 तक (नया या छोटा काम शुरू करने हेतु, कोई गारंटी नहीं)।\n"
            "2. **किशोर लोन:** ₹50,000 से ₹5 लाख तक (दुकान या स्टॉक बढ़ाने हेतु)।\n"
            "3. **तरुण लोन:** ₹5 लाख से ₹10 लाख तक।\n\n"
            "💡 *आवेदन कैसे करें:* अपने नजदीकी सरकारी या ग्रामीण बैंक में आधार, पैन कार्ड, दुकान का प्रमाण और अपना SahayakAI 'बैंक-रेडी रिपोर्ट' लेकर जाएं।"
        ),
        "en": (
            "🏦 **PM Mudra Yojana (PMMY) Details:**\n\n"
            "1. **Shishu Loan:** Up to ₹50,000 (For small tools/working capital, zero collateral).\n"
            "2. **Kishor Loan:** ₹50,000 to ₹5,00,000 (For expanding shop/inventory).\n"
            "3. **Tarun Loan:** ₹5 Lakh to ₹10 Lakh.\n\n"
            "💡 *How to apply:* Visit your nearest rural/commercial bank with Aadhaar, PAN, shop proof, and your SahayakAI 'Bank-Ready Report'."
        ),
        "schemes": ["PM Mudra Yojana (PMMY)"]
    },
    {
        "keywords": ["svanidhi", "स्वनिधि", "street vendor", "ठेला", "पटरी", "रेहड़ी", "10000", "20000", "50000"],
        "hi": (
            "🛒 **पीएम स्वनिधि योजना (PM SVANidhi):**\n\n"
            "- रेहड़ी, ठेला, फल-सब्जी विक्रेता और चाय-नाश्ता दुकानों के लिए ₹10,000 का पहला ऋण (समय पर भरने पर ₹20,000 व ₹50,000 का अगला ऋण)।\n"
            "- 7% ब्याज सब्सिडी सीधे खाते में मिलती है और डिजिटल भुगतान पर ₹1,200 वार्षिक कैशबैक।\n"
            "- किसी संपत्ति को गिरवी रखने की आवश्यकता नहीं है।"
        ),
        "en": (
            "🛒 **PM SVANidhi Scheme:**\n\n"
            "- Micro working capital loan of ₹10,000 for street vendors & micro food stalls (graduates to ₹20,000 & ₹50,000 upon timely repayment).\n"
            "- 7% interest subsidy directly credited + ₹1,200 annual cashback on digital transactions."
        ),
        "schemes": ["PM SVANidhi"]
    },
    {
        "keywords": ["pmegp", "पीएमईजीपी", "subsidy", "सब्सिडी", "खादी", "kvic", "मैन्युफैक्चरिंग"],
        "hi": (
            "🏭 **PMEGP योजना (प्रधानमंत्री रोजगार सृजन कार्यक्रम):**\n\n"
            "- विनिर्माण (Manufacturing) हेतु ₹50 लाख तक और सेवा क्षेत्र (Services) हेतु ₹20 लाख तक का ऋण।\n"
            "- ग्रामीण क्षेत्र में सामान्य वर्ग को 25% और SC/ST/OBC/महिला/दिव्यांग को **35% तक सरकारी सब्सिडी** मिलती है।\n"
            "- 18 वर्ष से अधिक आयु और 8वीं पास योग्यता।"
        ),
        "en": (
            "🏭 **PMEGP Scheme:**\n\n"
            "- Up to ₹50 Lakh for manufacturing & ₹20 Lakh for service micro-units.\n"
            "- Government subsidy up to **35% in rural areas** for SC/ST/OBC/Women entrepreneurs (25% for general).\n"
            "- Apply via kviconline.gov.in portal."
        ),
        "schemes": ["PMEGP"]
    },
    {
        "keywords": ["udhaar", "उधार", "recovery", "वसूली", "khata", "खाता", "बकाया"],
        "hi": (
            "📋 **उधार वसूली व खाता प्रबंधन टिप्स:**\n\n"
            "1. कभी भी कुल मासिक बिक्री का 20% से अधिक उधार न बांटें।\n"
            "2. ग्राहक को प्यार से याद दिलाने हेतु SahayakAI खाता सेक्शन से **WhatsApp/SMS तकादा संदेश** भेजें।\n"
            "3. नए ग्राहकों को पहले छोटी रकम का उधार दें, समय पर लौटाने पर ही सीमा बढ़ाएं।"
        ),
        "en": (
            "📋 **Udhaar Recovery & Cashflow Tips:**\n\n"
            "1. Keep total outstanding customer credit below 20% of monthly sales.\n2. Use the SahayakAI WhatsApp reminder feature to send polite payment links/messages.\n3. Offer a small 2% cash discount on immediate upfront payment."
        ),
        "schemes": ["Sahayak Digital Khata"]
    }
]

def match_offline_knowledge(query: str, is_hindi: bool) -> Optional[Tuple[str, List[str]]]:
    q = query.lower()
    for item in OFFLINE_SCHEME_KNOWLEDGE:
        if any(kw in q for kw in item["keywords"]):
            text = item["hi"] if is_hindi else item["en"]
            return (text, item["schemes"])
    return None

def build_gemini_prompt(req: AdvisorQueryRequest, profile: BusinessProfile) -> str:
    user = req.user_profile
    ledger = req.ledger_summary
    is_hindi = user.language.lower().startswith("hi")
    lang_name = "Hindi (Devanagari script)" if is_hindi else "English"

    # ML Mandi Context (Strictly trade-filtered)
    mandi_context = generate_mandi_prompt_context(profile.relevant_commodities)

    # Operational inputs formatted
    non_mandi_inputs_str = ", ".join(profile.non_agmarknet_inputs)
    advisory_topics_str = "\n".join(f"- {topic}" for topic in profile.advisory_topics)
    exclude_topics_str = ", ".join(profile.exclude_topics) if profile.exclude_topics else "None"
    schemes_str = ", ".join(profile.applicable_schemes)

    prompt = f"""
You are SahayakAI (सहायक AI), an expert rural business mentor & financial advisor tailored exclusively for Indian rural & semi-urban micro-entrepreneurs.

CRITICAL IDENTITY & TRADE BOUNDARY DIRECTIVES:
1. The user's exact business is: "{profile.primary_title}" ({profile.primary_title_hi}) located in {user.location}.
2. You MUST provide guidance strictly relevant to this trade.
3. EXPLICITLY FORBIDDEN TOPICS FOR THIS TRADE: [{exclude_topics_str}]
   DO NOT mention, recommend, or refer to any of these forbidden items under any circumstances.
4. KEY OPERATIONAL INPUTS FOR THIS TRADE: {non_mandi_inputs_str}
5. CORE ADVISORY FOCUS FOR THIS TRADE:
{advisory_topics_str}

USER PROFILE & DIGITIZED FINANCIAL CONTEXT:
- Name: {user.name}
- Profession: {profile.primary_title} ({user.business_type})
- Location: {user.location}
- Self-Help Group / Vyapar Mandal: {user.shg_name or 'Independent Micro Enterprise'}
- Monthly Turnover: ₹{user.monthly_turnover:,.0f}
- Digitized Ledger Inflow: ₹{ledger.total_inflow:,.0f}
- Digitized Ledger Outflow: ₹{ledger.total_outflow:,.0f}
- Digitized Net Savings: ₹{ledger.net_savings:,.0f}
- Outstanding Customer Udhaar (Pending): ₹{ledger.pending_udhaar:,.0f} (Recorded across {ledger.transaction_count} ledger transactions)
- Applicable Government Schemes: {schemes_str}

{mandi_context}

RESPONSE REQUIREMENTS:
- Primary Language: Respond in natural, warm, practical {lang_name}. Use clear bullet points and bold headers.
- Never give generic filler. Ground every answer in the user's specific business ({profile.primary_title}), real inputs, and exact numbers.
- Keep total response focused and concise (3-5 actionable bullet points).
- If the user asks about loans or schemes, explain {schemes_str} with exact loan limits, subsidy percentages, and document steps.

USER'S QUERY:
"{req.query}"
""".strip()

    return prompt

def call_gemini_api(prompt: str, api_key: str) -> str:
    """Calls Gemini REST API using urllib."""
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={api_key}"

    payload = {
        "contents": [
            {
                "parts": [
                    {"text": prompt}
                ]
            }
        ],
        "generationConfig": {
            "temperature": 0.6,
            "topP": 0.95,
            "topK": 40
        }
    }

    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json; charset=utf-8"},
        method="POST"
    )

    with urllib.request.urlopen(req, timeout=30) as resp:
        body = resp.read().decode("utf-8")
        data = json.loads(body)

        candidates = data.get("candidates", [])
        if candidates:
            parts = candidates[0].get("content", {}).get("parts", [])
            if parts:
                return parts[0].get("text", "")

    return "No text response received from advisor."

def process_advisor_query(req: AdvisorQueryRequest) -> AdvisorQueryResponse:
    """
    Core pipeline:
    1. Lookup BusinessProfile (Single source of truth in Python)
    2. Check Offline Knowledge Base if offline requested or no API key
    3. Generate Gemini prompt with ML forecasting and strict negative constraints
    4. Call Gemini REST
    5. Post-Response Validation: Check for forbidden topics
    6. If violations found, re-prompt with correction.
    7. If second attempt fails, flag validation failure and return safe trade-tailored advice.
    """
    profile = get_profile(req.user_profile.business_type)
    is_hindi = req.user_profile.language.lower().startswith("hi")
    api_key = os.getenv("GEMINI_API_KEY", "").strip()

    logger.info(
        f"[ADVISOR DEBUG] user_id={req.user_id or 'anon'} "
        f"business_type='{req.user_profile.business_type}' ({profile.primary_title}) "
        f"location='{req.user_profile.location}' "
        f"tracked_commodities={profile.relevant_commodities}"
    )

    # 1. Offline Mode / No API Key Check
    if req.force_offline or not api_key or api_key == "MY_GEMINI_API_KEY":
        offline_match = match_offline_knowledge(req.query, is_hindi)
        if offline_match:
            advice_text, schemes = offline_match
        else:
            advice_text = profile.get_offline_advice(
                query=req.query,
                user_name=req.user_profile.name,
                location=req.user_profile.location,
                is_hindi=is_hindi
            )
            schemes = profile.applicable_schemes

        return AdvisorQueryResponse(
            advice_text=advice_text,
            is_offline_tier=True,
            source_tag="CACHED",
            validation_passed=True,
            validation_violations=[],
            retry_count=0,
            business_type=profile.business_type,
            primary_title=profile.primary_title,
            relevant_commodities=profile.relevant_commodities,
            recommended_schemes=schemes,
            audio_tts_text=advice_text.replace("*", "").replace("#", "")
        )

    # 2. Online Gemini Execution with Guardrail Loop
    prompt = build_gemini_prompt(req, profile)
    retry_count = 0
    validation_violations: List[str] = []
    final_advice = ""
    validation_passed = True

    try:
        raw_response = call_gemini_api(prompt, api_key)
        violations = validate_response_relevance(raw_response, profile)

        if not violations:
            logger.info(f"[ADVISOR GUARDRAIL] Initial response passed validation cleanly for {profile.primary_title}")
            final_advice = raw_response
            validation_passed = True
            validation_violations = []
        else:
            logger.warning(
                f"[ADVISOR GUARDRAIL] Violations detected on attempt 1 for {profile.primary_title}: {violations}. "
                f"Triggering corrective re-prompt..."
            )
            retry_count = 1

            correction_prompt = f"""
CRITICAL GUARDRAIL CORRECTION:
Your previous draft response violated trade relevance rules by mentioning forbidden items for a {profile.primary_title}: {violations}.

THIS BUSINESS IS STRICTLY A {profile.primary_title.upper()} ({profile.primary_title_hi}).
YOU ARE STRICTLY FORBIDDEN FROM MENTIONING: {violations} or any other agricultural items like onion, mustard oil, or grain sacks.
Focus exclusively on {profile.primary_title} operational inputs: {', '.join(profile.non_agmarknet_inputs)}.

Please regenerate the complete advisory response for the following query, with ZERO mention of the forbidden items:
"{req.query}"
""".strip()

            corrected_response = call_gemini_api(correction_prompt, api_key)
            violations_attempt_2 = validate_response_relevance(corrected_response, profile)

            if not violations_attempt_2:
                logger.info(f"[ADVISOR GUARDRAIL] Corrective re-prompt succeeded for {profile.primary_title}")
                final_advice = corrected_response
                validation_passed = True
                validation_violations = []
            else:
                logger.error(
                    f"[ADVISOR GUARDRAIL] Attempt 2 STILL contained violations for {profile.primary_title}: {violations_attempt_2}. "
                    f"Safely falling back to verified trade-specific advice generator."
                )
                final_advice = profile.get_offline_advice(
                    query=req.query,
                    user_name=req.user_profile.name,
                    location=req.user_profile.location,
                    is_hindi=is_hindi
                )
                validation_passed = False
                validation_violations = violations_attempt_2

    except Exception as e:
        logger.error(f"[ADVISOR ERROR] Gemini call failed: {e}. Falling back to offline trade advice.", exc_info=True)
        final_advice = profile.get_offline_advice(
            query=req.query,
            user_name=req.user_profile.name,
            location=req.user_profile.location,
            is_hindi=is_hindi
        )
        validation_passed = True
        validation_violations = []
        return AdvisorQueryResponse(
            advice_text=final_advice,
            is_offline_tier=True,
            source_tag="CACHED",
            validation_passed=True,
            validation_violations=[],
            retry_count=retry_count,
            business_type=profile.business_type,
            primary_title=profile.primary_title,
            relevant_commodities=profile.relevant_commodities,
            recommended_schemes=profile.applicable_schemes,
            audio_tts_text=final_advice.replace("*", "").replace("#", "")
        )

    return AdvisorQueryResponse(
        advice_text=final_advice,
        is_offline_tier=False,
        source_tag="LIVE",
        validation_passed=validation_passed,
        validation_violations=validation_violations,
        retry_count=retry_count,
        business_type=profile.business_type,
        primary_title=profile.primary_title,
        relevant_commodities=profile.relevant_commodities,
        recommended_schemes=profile.applicable_schemes,
        audio_tts_text=final_advice.replace("*", "").replace("#", "")
    )
