"""
Module 5: Product Market Value Service
Routes between real Agmarknet-backed ML commodity price forecasting
and clearly labeled qualitative pricing guidance.
"""

import os
import json
import logging
import urllib.request
from typing import Dict, Any, List

from app.services.forecasting_engine import COMMODITY_FORECAST_DATABASE

logger = logging.getLogger("market_value_service")

AGMARKNET_LINKED_CATEGORIES = [
    "Dairy",
    "Agriculture",
    "Food Processing",
    "Poultry"
]

CATEGORY_COMMODITY_MAP: Dict[str, List[str]] = {
    "Dairy": ["Milk", "Ghee", "Paneer"],
    "Agriculture": ["Wheat", "Potato", "Onion", "Mustard Seed"],
    "Food Processing": ["Sugar", "Mustard Oil", "Spices", "Wheat"],
    "Poultry": ["Poultry Feed", "Maize"],
}

# Qualitative guidance benchmarks when Gemini API is offline or key missing
QUALITATIVE_PRICING_GUIDANCE: Dict[str, str] = {
    "Retail / Kirana": (
        "Standard FMCG goods operate on 8-12% gross retail margin, while loose grains and pulses yield 14-18%. "
        "In rural and semi-urban clusters, offer small sachet packs (₹5-₹20) to align with daily cash-flow cycles of daily wage earners."
    ),
    "Textiles": (
        "Target a 25-35% gross markup on everyday cotton sarees and suit fabrics, and 45-60% on festive/wedding Banarasi work. "
        "Maintain competitive baseline prices on school uniforms and workwear to build repeat customer footfall."
    ),
    "Handicrafts": (
        "Price items based on raw material cost + ₹350-₹500/day artisan wage allowance + 30% overhead margin. "
        "Create tiered pricing: affordable souvenir items (₹100-₹300) for high volume, alongside bespoke collector pieces (₹2,500+) for direct tourists or exhibition sales."
    ),
    "Tailoring": (
        "Set standard blouse/kurti basic stitching at ₹150-₹250 with 1-day express delivery charges of +₹50. "
        "Introduce combination stitching packages (suit + dupatta matching) to raise average transaction value."
    ),
    "Street Vendor": (
        "Maintain food/beverage unit pricing at ₹10-₹30 for fast impulse turnover. "
        "Ensure food cost percentage stays strictly below 38% of retail price to withstand unpredictable weather and evening footfall fluctuations."
    ),
    "Other": (
        "Adopt a cost-plus pricing strategy: compute total cost of acquisition + 20-30% gross margin. "
        "Compare against the closest block town market rates to prevent price resistance."
    )
}

def get_product_market_value(
    business_category: str,
    location: str,
    district: str
) -> Dict[str, Any]:
    """
    Returns data-backed Agmarknet forecast if category is agri/commodity linked;
    otherwise provides qualitative pricing guidance clearly badged.
    """
    if business_category in AGMARKNET_LINKED_CATEGORIES:
        # Real ML Mandi forecast
        relevant_keys = CATEGORY_COMMODITY_MAP.get(business_category, [])
        matched_forecasts = []
        for key, forecast in COMMODITY_FORECAST_DATABASE.items():
            if any(rk.lower() in key.lower() or key.lower() in rk.lower() for rk in relevant_keys):
                matched_forecasts.append(forecast.to_dict())

        # If direct map didn't catch, grab top items from DB
        if not matched_forecasts:
            matched_forecasts = [fc.to_dict() for fc in list(COMMODITY_FORECAST_DATABASE.values())[:3]]

        return {
            "source": "ml_forecast",
            "badge_label": "Agmarknet ML Forecast",
            "data": {
                "category": business_category,
                "commodities": matched_forecasts,
                "summary": f"Real-time Mandi price forecasts from regional wholesale hubs for {business_category} key inputs."
            }
        }
    else:
        # Qualitative pricing guidance via Gemini or verified benchmark
        api_key = os.getenv("GEMINI_API_KEY", "").strip()
        guidance_text = QUALITATIVE_PRICING_GUIDANCE.get(
            business_category,
            QUALITATIVE_PRICING_GUIDANCE["Other"]
        )

        if api_key and api_key != "MY_GEMINI_API_KEY":
            try:
                prompt = (
                    f"Suggest a concise 2-sentence practical pricing strategy for a small {business_category} business "
                    f"in {location} ({district}), India, based on typical regional purchasing power patterns. "
                    f"State clearly that this is general qualitative guidance, not a commodity forecast."
                )
                url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={api_key}"
                payload = {
                    "contents": [{"parts": [{"text": prompt}]}],
                    "generationConfig": {"temperature": 0.5, "maxOutputTokens": 200}
                }
                req = urllib.request.Request(
                    url,
                    data=json.dumps(payload).encode("utf-8"),
                    headers={"Content-Type": "application/json"},
                    method="POST"
                )
                with urllib.request.urlopen(req, timeout=10) as resp:
                    resp_data = json.loads(resp.read().decode("utf-8"))
                    candidates = resp_data.get("candidates", [])
                    if candidates:
                        text = candidates[0].get("content", {}).get("parts", [{}])[0].get("text", "")
                        if text:
                            guidance_text = text.strip()
            except Exception as e:
                logger.warning(f"[GEMINI PRICING] API call failed: {e}. Using benchmark guidance.")

        return {
            "source": "general_guidance",
            "badge_label": "General Pricing Guidance",
            "data": {
                "category": business_category,
                "guidance": guidance_text,
                "summary": f"Qualitative retail pricing strategy tailored for {business_category} in {district}."
            }
        }
