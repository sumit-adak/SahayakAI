"""
Feasibility Router for SahayakAI PS26091 Implementation
Provides:
- POST /feasibility/full-report (Module 7: runs all modules together)
- POST /feasibility/analysis (Module 4: qualitative SWOT & competitor grounded analysis)
- POST /feasibility/calculator (Module 6: financial project cost & quarterly EMI schedule)
- GET /feasibility/categories (Fixed business category list)
- GET /feasibility/history (List past feasibility checks for user comparison)
"""

import time
import uuid
import logging
from typing import Dict, Any, List, Optional
from fastapi import APIRouter, HTTPException, Query

from app.schemas.feasibility import (
    FeasibilityRequest,
    BUSINESS_CATEGORIES,
    CompetitorMappingResult,
    MarketReachResult,
    SwotResult,
    ProductMarketValueResult,
    FinancialStructureResult,
    QuarterlyEmiScheduleItem,
    FeasibilityReportResponse
)
from app.services.competitor_service import get_competitor_density, geocode_location_sync
from app.services.market_reach_service import estimate_market_reach
from app.services.market_value_service import get_product_market_value
from app.services.opportunity_service import generate_opportunity_and_swot
from app.services.financial_calculator import (
    calculate_project_structure,
    generate_repayment_schedule
)

logger = logging.getLogger("feasibility_router")
router = APIRouter()

# In-Memory store for persistence across feasibility checks (synced with client-side Room/Firestore)
FEASIBILITY_STORE: Dict[str, List[Dict[str, Any]]] = {}

@router.get("/categories")
def get_business_categories() -> List[str]:
    """Returns the fixed list of supported business categories."""
    return BUSINESS_CATEGORIES

@router.post("/analysis")
def qualitative_analysis_endpoint(payload: FeasibilityRequest) -> Dict[str, Any]:
    """
    Module 4 Standalone: Grounded qualitative SWOT and opportunity analysis
    driven by real competitor mapping and market reach.
    """
    lat, lon, district = geocode_location_sync(payload.location)
    if payload.lat is not None and payload.lon is not None:
        lat, lon = payload.lat, payload.lon

    competitor_data = get_competitor_density(lat, lon, payload.business_category, payload.location)
    market_data = estimate_market_reach(lat, lon, district)

    qualitative = generate_opportunity_and_swot(
        business_category=payload.business_category,
        location=payload.location,
        available_margin=payload.available_margin,
        competitor_data=competitor_data,
        market_data=market_data
    )

    return {
        "competitor_mapping": competitor_data,
        "market_reach": market_data,
        "opportunity_analysis": qualitative.get("opportunity_analysis", ""),
        "swot": qualitative.get("swot", {}),
        "threats": qualitative.get("threats", "")
    }

@router.post("/calculator")
def financial_calculator_endpoint(payload: FeasibilityRequest) -> Dict[str, Any]:
    """
    Module 6 Standalone: Smart Financial Calculator & Scheme Router
    """
    structure = calculate_project_structure(payload.available_margin)
    if "error" in structure:
        raise HTTPException(status_code=400, detail=structure["error"])

    schedule = generate_repayment_schedule(
        loan_amount=structure["loan_amount"],
        interest_rate=structure["interest_rate"],
        tenure_years=structure["tenure_years"],
        moratorium_months=structure["moratorium_months"]
    )

    return {
        **structure,
        "quarterly_schedule": schedule
    }

@router.post("/full-report", response_model=FeasibilityReportResponse)
def full_feasibility_report(payload: FeasibilityRequest):
    """
    Module 7: Orchestration Endpoint
    Runs all PS26091 modules together in dependency order:
    1. Geocoding & Competitor Mapping (Module 2)
    2. Market Reach Estimation (Module 3)
    3. Qualitative Opportunity, SWOT & Threats (Module 4)
    4. Product Market Value with Data Source Badging (Module 5)
    5. Financial Calculator & Quarterly Repayment Schedule (Module 6)
    6. Persists check record for user comparison.
    """
    # 1. Location & Demographics
    lat, lon, district = geocode_location_sync(payload.location)
    if payload.lat is not None and payload.lon is not None:
        lat, lon = payload.lat, payload.lon

    # 2. Competitor Mapping (Module 2)
    competitor_data = get_competitor_density(lat, lon, payload.business_category, payload.location)

    # 3. Market Reach (Module 3)
    market_data = estimate_market_reach(lat, lon, district)

    # 4. Opportunity, SWOT, Threats (Module 4)
    qualitative = generate_opportunity_and_swot(
        business_category=payload.business_category,
        location=payload.location,
        available_margin=payload.available_margin,
        competitor_data=competitor_data,
        market_data=market_data
    )

    # 5. Product Market Value (Module 5)
    market_value = get_product_market_value(payload.business_category, payload.location, district)

    # 6. Financial Structure & Repayment (Module 6)
    financial = calculate_project_structure(payload.available_margin)
    if "error" in financial:
        raise HTTPException(status_code=400, detail=financial["error"])

    quarterly_schedule = generate_repayment_schedule(
        loan_amount=financial["loan_amount"],
        interest_rate=financial["interest_rate"],
        tenure_years=financial["tenure_years"],
        moratorium_months=financial["moratorium_months"]
    )

    check_id = f"fc_{uuid.uuid4().hex[:8]}"
    created_at = time.strftime("%Y-%m-%d %H:%M:%S")

    report_dict = {
        "check_id": check_id,
        "location": payload.location,
        "available_margin": payload.available_margin,
        "business_category": payload.business_category,
        "competitor_mapping": competitor_data,
        "market_reach": market_data,
        "opportunity_analysis": qualitative.get("opportunity_analysis", ""),
        "swot": qualitative.get("swot", {"strengths": [], "weaknesses": [], "opportunities": [], "threats": []}),
        "threats": qualitative.get("threats", ""),
        "product_market_value": market_value,
        "financial_structure": financial,
        "quarterly_schedule": quarterly_schedule,
        "created_at": created_at
    }

    # Save to user's history store
    uid = payload.user_id or "anon"
    if uid not in FEASIBILITY_STORE:
        FEASIBILITY_STORE[uid] = []
    FEASIBILITY_STORE[uid].append(report_dict)

    return report_dict

@router.get("/history")
def get_user_feasibility_history(user_id: str = Query("anon")) -> List[Dict[str, Any]]:
    """Returns past feasibility checks for this user to compare business ideas."""
    return FEASIBILITY_STORE.get(user_id, [])
