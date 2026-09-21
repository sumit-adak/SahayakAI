from pydantic import BaseModel, Field
from typing import List, Optional

class UserProfileContext(BaseModel):
    name: str = "Rural Entrepreneur"
    business_type: str = "OTHER"
    location: str = "Varanasi, Uttar Pradesh"
    monthly_turnover: float = 25000.0
    shg_name: Optional[str] = None
    language: str = "hi"

class LedgerSummaryContext(BaseModel):
    total_inflow: float = 0.0
    total_outflow: float = 0.0
    net_savings: float = 0.0
    pending_udhaar: float = 0.0
    transaction_count: int = 0

class AdvisorQueryRequest(BaseModel):
    query: str
    user_profile: UserProfileContext = Field(default_factory=UserProfileContext)
    ledger_summary: LedgerSummaryContext = Field(default_factory=LedgerSummaryContext)
    force_offline: bool = False
    user_id: Optional[str] = None

class AdvisorQueryResponse(BaseModel):
    advice_text: str
    is_offline_tier: bool = False
    source_tag: str = "LIVE"
    validation_passed: bool = True
    validation_violations: List[str] = []
    retry_count: int = 0
    business_type: str = "OTHER"
    primary_title: str = "Micro Enterprise"
    relevant_commodities: List[str] = []
    recommended_schemes: List[str] = []
    audio_tts_text: str = ""
