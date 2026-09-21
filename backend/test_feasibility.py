import sys
import os

# Ensure backend directory is in path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "app"))

from services.financial_calculator import calculate_project_structure, generate_repayment_schedule
from services.market_reach_service import estimate_market_reach
from services.market_value_service import get_product_market_value
from services.competitor_service import get_competitor_density, geocode_location_sync

def test_financial_calculator():
    # Test Term Loan (Margin: 100,000 -> Project Cost: 1,000,000)
    res = calculate_project_structure(100000.0)
    assert res["project_cost"] == 1000000.0
    assert res["loan_amount"] == 900000.0
    assert res["scheme"] == "Term Loan Scheme"
    assert res["interest_rate"] == 8.0
    assert res["tenure_years"] == 7
    assert res["moratorium_months"] == 6

    # Test Micro Finance (Margin: 12,000 -> Project Cost: 120,000)
    res_micro = calculate_project_structure(12000.0)
    assert res_micro["project_cost"] == 120000.0
    assert res_micro["loan_amount"] == 108000.0
    assert res_micro["scheme"] == "Micro Finance Scheme"
    assert res_micro["interest_rate"] == 6.5
    assert res_micro["tenure_years"] == 3
    assert res_micro["moratorium_months"] == 3

    # Test Schedule
    sched = generate_repayment_schedule(900000.0, 8.0, 7, 6)
    assert len(sched) == 28
    assert sched[0]["status"] == "moratorium"
    assert sched[1]["status"] == "moratorium"
    assert sched[2]["status"] == "repayment"
    assert sched[-1]["balance"] == 0.0
    print("✓ Financial Calculator tests passed.")

def test_market_reach():
    reach = estimate_market_reach(25.3176, 82.9739, "Varanasi")
    assert "3,676,841" in reach["estimated_consumer_base"]
    assert "Census 2011" in reach["estimated_consumer_base"]
    print("✓ Market Reach tests passed.")

def test_market_value():
    dairy = get_product_market_value("Dairy", "Varanasi", "Varanasi")
    assert dairy["source"] == "ml_forecast"
    assert len(dairy["data"]["commodities"]) > 0

    retail = get_product_market_value("Retail / Kirana", "Varanasi", "Varanasi")
    assert retail["source"] == "general_guidance"
    assert len(retail["data"]["guidance"]) > 0
    print("✓ Market Value tests passed.")

def test_competitor():
    lat, lon, dist = geocode_location_sync("Varanasi")
    comp = get_competitor_density(lat, lon, "Dairy", "Varanasi")
    assert comp["count_nearby"] >= 1
    assert comp["radius_km"] == 7.5
    assert len(comp["density_note"]) > 0
    print("✓ Competitor Mapping tests passed.")

if __name__ == "__main__":
    test_financial_calculator()
    test_market_reach()
    test_market_value()
    test_competitor()
    print("\nALL BACKEND MODULE TESTS PASSED GREEN!")
