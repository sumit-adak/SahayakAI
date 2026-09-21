import os
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.services.competitor_service import get_competitor_density

client = TestClient(app)

def test_health_and_scheduler_status():
    """Verify backend health endpoint reports operational state and running APScheduler."""
    with TestClient(app) as tc:
        response = tc.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert "scheduler" in data
        assert isinstance(data["scheduler"], dict)
        assert data["scheduler"]["running"] is True

def test_live_openstreetmap_competitor_mapping():
    """Verify live geographic competitor mapping (no LLM guessing)."""
    # Coordinates for Varanasi center
    analysis = get_competitor_density(lat=25.3176, lon=82.9739, business_category="Retail / Kirana", radius_km=5.0)
    assert analysis["count_nearby"] > 0
    assert analysis["source"] in ["openstreetmap_live", "google_places_api", "regional_census_grounded"]
    assert len(analysis["sample_names"]) > 0
    assert analysis["radius_km"] == 5.0
    print(f"Competitor analysis returned {analysis['count_nearby']} real businesses from source: {analysis['source']}")

def test_gemini_ocr_khata_parser_endpoint():
    """Verify Gemini 2.5 Flash OCR structured transaction parsing."""
    raw_khata = """
    01/03 Suresh Verma (Teacher) - 10kg Aata, Dal, Mustard Oil Rs 1450 udhaar
    01/03 Daily Cash Counter & UPI QR Bikri ₹3280 jama
    01/03 Kashi Wholesale Mandi - 2 Mustard Oil Tins & Sugar ₹2600 kharch
    """
    response = client.post(
        "/api/v1/khata/parse-ocr",
        json={"raw_text": raw_khata, "business_type": "KIRANA"}
    )
    assert response.status_code == 200
    items = response.json()
    assert len(items) >= 3
    # Verify first transaction
    suresh = next((i for i in items if "Suresh" in i["party_name"] or "suresh" in i["description"].lower()), None)
    assert suresh is not None
    assert suresh["amount"] == 1450.0
    assert suresh["type"] == "DEBIT"
    assert suresh["category"] == "CUSTOMER_UDHAAR"

    # Verify credit transaction
    counter = next((i for i in items if "Counter" in i["party_name"] or "Bikri" in i["party_name"] or "Cash" in i["description"]), None)
    assert counter is not None
    assert counter["amount"] == 3280.0
    assert counter["type"] == "CREDIT"
    assert counter["category"] == "SALES"

def test_advisor_query_endpoint_live_tag():
    """Verify chatbot query receives LIVE source tag and trade guardrail validation."""
    payload = {
        "query": "दुकान में बिक्री और ग्राहक कैसे बढ़ाएं?",
        "business_type": "KIRANA",
        "location": "Varanasi, UP",
        "language": "hi",
        "force_offline": False
    }
    response = client.post("/chatbot/query", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "advice_text" in data
    assert len(data["advice_text"]) > 20
    assert data["source_tag"] == "LIVE"
    assert data["is_offline_tier"] is False
    assert data["validation_passed"] is True

def test_schemes_profession_filtering_and_reminders():
    """Verify schemes endpoint filters by profession and manages reminders."""
    # Kirana schemes
    resp_kirana = client.get("/api/v1/schemes?business_type=KIRANA")
    assert resp_kirana.status_code == 200
    schemes = resp_kirana.json()
    assert len(schemes) > 0

    # Add reminder
    rem_payload = {
        "title": "PM Mudra Verification",
        "due_date": "2026-09-30",
        "amount": 2500.0,
        "category": "EMI_REPAYMENT",
        "notes": "Verify bank report"
    }
    resp_rem = client.post("/api/v1/schemes/reminders", json=rem_payload)
    assert resp_rem.status_code == 200
    created = resp_rem.json()
    assert "id" in created
    assert created["title"] == "PM Mudra Verification"
    assert created["amount"] == 2500.0

    # Get reminders
    get_rem = client.get("/api/v1/schemes/reminders")
    assert get_rem.status_code == 200
    assert len(get_rem.json()) >= 1
