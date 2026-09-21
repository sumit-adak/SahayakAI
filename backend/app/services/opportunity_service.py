"""
Module 4: Opportunity Analysis, SWOT & Threats Service
Gemini-powered qualitative analysis strictly grounded in real competitor density
and market reach data from Modules 2 & 3.
"""

import os
import json
import logging
import urllib.request
from typing import Dict, Any, List

logger = logging.getLogger("opportunity_service")

QUALITATIVE_ANALYSIS_PROMPT = """
You are a rural enterprise feasibility expert in India.
Business category: {business_category}
Location: {location}
Available margin capital: ₹{available_margin:,.0f}

Real local data gathered:
- Competitor density: {competitor_count} similar businesses within {radius}km ({density_note})
- Market reach: {market_reach_note}

Based ONLY on this real data plus general knowledge of {business_category} businesses in
rural/semi-urban India, return a valid JSON object (NO markdown, NO code fences, ONLY raw JSON) with:

1. "opportunity_analysis": string describing specific underserved niches within {business_category} in this local economy, reasoning explicitly from the competitor density above.
2. "swot": an object with four string arrays:
   "strengths": [2-3 concise points tailored specifically to starting with ₹{available_margin:,.0f} margin capital],
   "weaknesses": [2-3 concise realistic operational constraints for this capital size],
   "opportunities": [2-3 local underserved gaps in {location}],
   "threats": [2-3 specific environmental or market threats]
3. "threats": string describing specific local operational risks — supply chain bottlenecks, seasonal demand shifts, single-buyer dependency — relevant to {business_category}.

Do not invent specific statistics beyond what is given above. Where you do not have hard data, reason qualitatively and say so explicitly.
""".strip()

def _generate_fallback_analysis(
    business_category: str,
    location: str,
    available_margin: float,
    competitor_count: int,
    radius: float,
    density_note: str,
    market_reach_note: str
) -> Dict[str, Any]:
    """Generates grounded qualitative SWOT and opportunity analysis if Gemini API is offline."""
    
    if competitor_count <= 2:
        opp_text = (
            f"With only {competitor_count} established {business_category} enterprises within a {radius}km radius, "
            f"this locality has clear underserved demand. Capturing village morning trade and establishing consistent "
            f"product availability provides immediate first-mover advantage without price wars."
        )
    else:
        opp_text = (
            f"With {competitor_count} existing {business_category} businesses within {radius}km ({density_note}), "
            f"direct price competition will erode margins. Focus on a differentiated sub-niche—such as doorstep "
            f"service, digital UPI payments, or higher-quality graded inventory—to secure loyal clientele."
        )

    strengths = [
        f"Promoter margin of ₹{available_margin:,.0f} enables quick launch with zero debt burden during initial setup.",
        f"Deep local familiarity with {location} consumer habits and seasonal cash-flow cycles.",
        "Low operational overhead compared to town-center competitors."
    ]

    weaknesses = [
        f"Initial working capital of ₹{available_margin:,.0f} restricts bulk procurement discounts.",
        "Susceptibility to customer requests for long-duration credit (Udhaar).",
        "Limited cold-storage or moisture-resistant inventory infrastructure."
    ]

    opportunities = [
        f"Underserved consumer base in adjacent rural clusters within {radius}km.",
        "Tie-up with local Self-Help Groups (SHGs) and weekly Haats for aggregated distribution.",
        "Eligibility for subsidized government enterprise schemes (Mudra / Term Loan)."
    ]

    threats_list = [
        "Seasonal demand slumps during agricultural sowing and harvest transition periods.",
        "Local price fluctuations for wholesale raw inputs without long-term price locks.",
        "Risk of informal competitor copying successful product lines."
    ]

    threats_narrative = (
        f"Primary operational risks for {business_category} in {location} include seasonal cash liquidity swings "
        f"when farm laborers await crop payments, single-wholesaler supply dependency, and unexpected weather disruptions."
    )

    return {
        "opportunity_analysis": opp_text,
        "swot": {
            "strengths": strengths,
            "weaknesses": weaknesses,
            "opportunities": opportunities,
            "threats": threats_list
        },
        "threats": threats_narrative
    }

def generate_opportunity_and_swot(
    business_category: str,
    location: str,
    available_margin: float,
    competitor_data: Dict[str, Any],
    market_data: Dict[str, Any]
) -> Dict[str, Any]:
    """
    Calls Gemini API with the real data prompt, or falls back to grounded benchmark.
    """
    api_key = os.getenv("GEMINI_API_KEY", "").strip()

    competitor_count = competitor_data.get("count_nearby", 2)
    radius = competitor_data.get("radius_km", 7.5)
    density_note = competitor_data.get("density_note", "Moderate competition")
    market_reach_note = market_data.get("distribution_channel_hint", "Local retail")

    if api_key and api_key != "MY_GEMINI_API_KEY":
        try:
            prompt = QUALITATIVE_ANALYSIS_PROMPT.format(
                business_category=business_category,
                location=location,
                available_margin=available_margin,
                competitor_count=competitor_count,
                radius=radius,
                density_note=density_note,
                market_reach_note=market_reach_note
            )
            url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={api_key}"
            payload = {
                "contents": [{"parts": [{"text": prompt}]}],
                "generationConfig": {
                    "temperature": 0.4,
                    "responseMimeType": "application/json"
                }
            }
            req = urllib.request.Request(
                url,
                data=json.dumps(payload).encode("utf-8"),
                headers={"Content-Type": "application/json"},
                method="POST"
            )
            with urllib.request.urlopen(req, timeout=15) as resp:
                resp_data = json.loads(resp.read().decode("utf-8"))
                candidates = resp_data.get("candidates", [])
                if candidates:
                    raw_text = candidates[0].get("content", {}).get("parts", [{}])[0].get("text", "")
                    clean_text = raw_text.strip()
                    if clean_text.startswith("```"):
                        clean_text = clean_text.strip("`").replace("json\n", "", 1)
                    parsed = json.loads(clean_text)
                    if "opportunity_analysis" in parsed and "swot" in parsed:
                        return parsed
        except Exception as e:
            logger.warning(f"[GEMINI OPPORTUNITY] Error or timeout: {e}. Using grounded fallback.")

    return _generate_fallback_analysis(
        business_category=business_category,
        location=location,
        available_margin=available_margin,
        competitor_count=competitor_count,
        radius=radius,
        density_note=density_note,
        market_reach_note=market_reach_note
    )
