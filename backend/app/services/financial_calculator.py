"""
Module 6: Smart Financial Calculator & Scheme Router
PS26091 Financial Structuring and Repayment Schedule Generator
"""

from typing import Dict, Any, List

MICRO_FINANCE_THRESHOLD = 140_000.0

def calculate_project_structure(available_margin: float) -> Dict[str, Any]:
    """
    Calculates project cost based on 10% promoter contribution / available margin.
    Applies scheme boundaries:
    - If project cost <= 140,000: Micro Finance Scheme (6.5% interest, 3 yrs, 3 mo moratorium, max 125,000)
    - If project cost <= 5,000,000: Term Loan Scheme (8.0% interest, 7 yrs, 6 mo moratorium, max 4,500,000)
    """
    if available_margin <= 0:
        return {"error": "Available margin must be greater than zero."}

    project_cost = available_margin / 0.10
    loan_amount = project_cost * 0.90

    if project_cost <= MICRO_FINANCE_THRESHOLD:
        scheme = "Micro Finance Scheme"
        interest_rate = 6.5
        tenure_years = 3
        moratorium_months = 3
        max_loan_cap = 125_000.0
    elif project_cost <= 5_000_000.0:
        scheme = "Term Loan Scheme"
        interest_rate = 8.0
        tenure_years = 7
        moratorium_months = 6
        max_loan_cap = 4_500_000.0
    else:
        return {"error": "Project cost exceeds scheme limits (Max project cost ₹50,00,000)."}

    loan_amount = min(loan_amount, max_loan_cap)

    return {
        "available_margin": float(available_margin),
        "project_cost": round(float(project_cost), 2),
        "loan_amount": round(float(loan_amount), 2),
        "scheme": scheme,
        "interest_rate": float(interest_rate),
        "tenure_years": int(tenure_years),
        "moratorium_months": int(moratorium_months),
        "max_loan_cap": float(max_loan_cap),
    }

def generate_repayment_schedule(
    loan_amount: float,
    interest_rate: float,
    tenure_years: int,
    moratorium_months: int
) -> List[Dict[str, Any]]:
    """
    Generates repayment schedule taking moratorium into account.
    Returns quarterly snapshots: months 3, 6, 9, 12, ...
    """
    if loan_amount <= 0 or tenure_years <= 0:
        return []

    monthly_rate = interest_rate / 12.0 / 100.0
    total_months = tenure_years * 12
    repayment_months = total_months - moratorium_months

    if repayment_months <= 0 or monthly_rate <= 0:
        return []

    # Standard EMI formula on remaining repayment tenure
    emi = (loan_amount * monthly_rate * ((1.0 + monthly_rate) ** repayment_months)) / \
          (((1.0 + monthly_rate) ** repayment_months) - 1.0)

    schedule = []
    balance = float(loan_amount)

    for month in range(1, total_months + 1):
        if month <= moratorium_months:
            # During moratorium, no principal repayment
            schedule.append({
                "month": month,
                "status": "moratorium",
                "emi": 0.0,
                "principal": 0.0,
                "interest": round(balance * monthly_rate, 2),
                "balance": round(balance, 2)
            })
        else:
            interest_component = balance * monthly_rate
            principal_component = emi - interest_component
            balance -= principal_component
            schedule.append({
                "month": month,
                "status": "repayment",
                "emi": round(emi, 2),
                "principal": round(principal_component, 2),
                "interest": round(interest_component, 2),
                "balance": round(max(balance, 0.0), 2)
            })

    # Quarterly snapshots (months 3, 6, 9, 12...)
    quarterly = []
    for i in range(2, len(schedule), 3):
        item = schedule[i].copy()
        item["quarter"] = (item["month"] // 3)
        quarterly.append(item)

    return quarterly
