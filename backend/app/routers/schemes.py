from fastapi import APIRouter, Query, HTTPException
from typing import List, Dict, Any, Optional
from pydantic import BaseModel
from app.services.scheduler_service import get_all_reminders, add_reminder, get_scheduler_status, check_reminders_job
import uuid
from datetime import datetime

router = APIRouter()

SCHEMES_DB = [
    {
        "id": "pm_mudra",
        "name": "PM Mudra Yojana (PMMY)",
        "ministry": "Ministry of Finance",
        "subsidy_percent": 0,
        "max_loan_amount": 1000000.0,
        "category": "CREDIT_LOAN",
        "eligible_professions": ["KIRANA", "TAILORING", "FOOD_STALL", "HANDICRAFTS", "DAIRY_FARMING", "AGRI_ALLIED", "OTHER"],
        "description": "Collateral-free micro loans up to ₹10 Lakh (Shishu ₹50k, Kishor ₹5L, Tarun ₹10L) for small business setup, shop expansion, and equipment."
    },
    {
        "id": "pmegp_scheme",
        "name": "PMEGP (Prime Minister Employment Generation)",
        "ministry": "Ministry of MSME & KVIC",
        "subsidy_percent": 35,
        "max_loan_amount": 5000000.0,
        "category": "MINORITY_SC_ST",
        "eligible_professions": ["TAILORING", "HANDICRAFTS", "FOOD_PROCESSING", "DAIRY_FARMING", "AGRICULTURE", "OTHER"],
        "description": "Credit-linked subsidy programme offering up to 35% government subsidy for rural manufacturing & service micro enterprises."
    },
    {
        "id": "pm_svanidhi",
        "name": "PM SVANidhi (Street Vendor AtmaNirbhar)",
        "ministry": "Ministry of Housing & Urban Affairs",
        "subsidy_percent": 7,
        "max_loan_amount": 50000.0,
        "category": "STREET_VENDORS",
        "eligible_professions": ["FOOD_STALL", "STREET_VENDOR", "KIRANA", "OTHER"],
        "description": "Micro working capital collateral-free credit starting at ₹10,000 graduating to ₹20,000 and ₹50,000 on timely digital repayment."
    },
    {
        "id": "pm_vishwakarma",
        "name": "PM Vishwakarma Scheme",
        "ministry": "Ministry of MSME",
        "subsidy_percent": 15,
        "max_loan_amount": 300000.0,
        "category": "ARTISANS_CRAFTS",
        "eligible_professions": ["TAILORING", "HANDICRAFTS", "POTTERY", "CARPENTRY", "BLACKSMITH", "OTHER"],
        "description": "Toolkit incentive grant of ₹15,000 plus 5% collateral-free loan up to ₹3 Lakh for traditional artisans and craftspeople (including Darzi/Tailor)."
    },
    {
        "id": "kcc_animal_husbandry",
        "name": "Kisan Credit Card (Animal Husbandry & Dairy)",
        "ministry": "Ministry of Fisheries, Animal Husbandry and Dairying",
        "subsidy_percent": 3,
        "max_loan_amount": 200000.0,
        "category": "AGRICULTURE_ALLIED",
        "eligible_professions": ["DAIRY_FARMING", "POULTRY", "FISHERIES", "AGRI_ALLIED", "OTHER"],
        "description": "Working capital loan at 4% effective interest rate (with 3% prompt repayment incentive) for cattle feed, veterinary care, and silage."
    }
]

class CreateReminderRequest(BaseModel):
    title: str
    title_hi: Optional[str] = None
    amount: float = 0.0
    due_date: str  # YYYY-MM-DD
    category: str = "LOAN_EMI"  # LOAN_EMI, SCHEME_DEADLINE, TAX_FILING
    notes: Optional[str] = ""

@router.get("/", response_model=List[Dict[str, Any]])
def list_schemes(business_type: Optional[str] = Query(None, description="Filter schemes matching profession enum e.g. TAILORING, FOOD_STALL")):
    if not business_type:
        return SCHEMES_DB
    clean_type = business_type.strip().upper()
    return [
        scheme for scheme in SCHEMES_DB
        if clean_type in scheme.get("eligible_professions", []) or "OTHER" in scheme.get("eligible_professions", [])
    ]

@router.get("/reminders", response_model=List[Dict[str, Any]])
def list_reminders():
    return get_all_reminders()

@router.post("/reminders", response_model=Dict[str, Any])
def create_reminder(req: CreateReminderRequest):
    new_rem = {
        "id": f"rem_{uuid.uuid4().hex[:8]}",
        "title": req.title,
        "title_hi": req.title_hi or req.title,
        "amount": req.amount,
        "due_date": req.due_date,
        "category": req.category,
        "is_completed": False,
        "notified": False,
        "notes": req.notes,
        "created_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }
    return add_reminder(new_rem)

@router.get("/scheduler-status")
def scheduler_status():
    return get_scheduler_status()

@router.post("/scheduler-trigger-check")
def trigger_scheduler_check():
    check_reminders_job()
    return {"status": "triggered", "scheduler": get_scheduler_status()}
