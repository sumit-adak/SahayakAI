"""
Feasibility Schemas for SahayakAI PS26091 Implementation
"""

from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field

BUSINESS_CATEGORIES = [
    "Dairy",
    "Retail / Kirana",
    "Textiles",
    "Handicrafts",
    "Food Processing",
    "Agriculture",
    "Poultry",
    "Tailoring",
    "Street Vendor",
    "Other"
]

class FeasibilityRequest(BaseModel):
    location: str = Field(..., description="Village/Block/District or City")
    available_margin: float = Field(..., gt=0, description="Available margin capital in INR e.g. 100000")
    business_category: str = Field(..., description="Business category from BUSINESS_CATEGORIES list")
    lat: Optional[float] = Field(None, description="Optional latitude")
    lon: Optional[float] = Field(None, description="Optional longitude")
    user_id: Optional[str] = Field("anon", description="User ID for saving feasibility check")

class CompetitorMappingResult(BaseModel):
    count_nearby: int
    radius_km: float
    density_note: str
    sample_names: List[str]

class MarketReachResult(BaseModel):
    estimated_consumer_base: str
    radius_km: float
    distribution_channel_hint: str
    district: str

class SwotResult(BaseModel):
    strengths: List[str]
    weaknesses: List[str]
    opportunities: List[str]
    threats: List[str]

class ProductMarketValueResult(BaseModel):
    source: str  # "ml_forecast" or "general_guidance"
    data: Any    # String advice or commodity forecast dict/list
    badge_label: str

class FinancialStructureResult(BaseModel):
    available_margin: float
    project_cost: float
    loan_amount: float
    scheme: str
    interest_rate: float
    tenure_years: int
    moratorium_months: int
    max_loan_cap: float

class QuarterlyEmiScheduleItem(BaseModel):
    quarter: int
    month: int
    status: str
    emi: float
    principal: float
    interest: float
    balance: float

class FeasibilityReportResponse(BaseModel):
    check_id: str
    location: str
    available_margin: float
    business_category: str
    competitor_mapping: CompetitorMappingResult
    market_reach: MarketReachResult
    opportunity_analysis: str
    swot: SwotResult
    threats: str
    product_market_value: ProductMarketValueResult
    financial_structure: FinancialStructureResult
    quarterly_schedule: List[QuarterlyEmiScheduleItem]
    created_at: str
