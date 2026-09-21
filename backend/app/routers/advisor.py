"""
Advisor Router for SahayakAI Backend
Provides:
- POST /query (mounted under /chatbot and /api/v1/advisor)
- GET /profiles (view business profiles single source config)
- GET /forecasts (view server-side ML commodity forecasts)
- GET /health
"""

from fastapi import APIRouter, Query
from typing import Dict, Any, List, Optional

from app.schemas.advisor_schema import AdvisorQueryRequest, AdvisorQueryResponse
from app.services.advisor_service import process_advisor_query
from app.core.business_profiles import BUSINESS_PROFILES, get_profile
from app.services.forecasting_engine import COMMODITY_FORECAST_DATABASE, get_filtered_forecasts

router = APIRouter()

@router.post("/query", response_model=AdvisorQueryResponse)
def advisor_query_endpoint(req: AdvisorQueryRequest):
    """
    Main Chatbot Query Endpoint.
    Collects user query + trade context, applies server-side trade filtering,
    calls Gemini reasoning with strict guardrails, validates response against forbidden topics,
    and returns verified trade-specific advice.
    """
    return process_advisor_query(req)

@router.get("/profiles")
def list_business_profiles() -> Dict[str, Any]:
    """Returns the single-source BUSINESS_PROFILES configuration."""
    return {
        key: profile.to_dict()
        for key, profile in BUSINESS_PROFILES.items()
    }

@router.get("/forecasts")
def list_forecasts(business_type: Optional[str] = Query(None, description="Optional business type filter e.g. FOOD_STALL")) -> List[Dict[str, Any]]:
    """Returns server-side ML Mandi commodity forecasts, optionally filtered by business type."""
    if business_type:
        profile = get_profile(business_type)
        forecasts = get_filtered_forecasts(profile.relevant_commodities)
    else:
        forecasts = list(COMMODITY_FORECAST_DATABASE.values())

    return [fc.to_dict() for fc in forecasts]

@router.get("/health")
def advisor_health():
    return {
        "status": "online",
        "service": "Advisor Router",
        "single_source": "Python BUSINESS_PROFILES",
        "supported_trades": list(BUSINESS_PROFILES.keys())
    }
