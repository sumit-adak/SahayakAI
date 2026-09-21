import os
from contextlib import asynccontextmanager
from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# Explicitly load .env from root or current directory
root_env = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../.env"))
if os.path.exists(root_env):
    load_dotenv(root_env)
else:
    load_dotenv()

from app.routers import advisor, khata, schemes, kyc, feasibility
from app.services.scheduler_service import start_scheduler, shutdown_scheduler, get_scheduler_status

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Start APScheduler
    start_scheduler()
    yield
    # Shutdown: Stop APScheduler
    shutdown_scheduler()

app = FastAPI(
    title="SahayakAI Backend API",
    description="Rural Business & Financial Advisory Backend (SIH 2026 Problem Statement SIH26091)",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Chatbot endpoint router mounted directly at /chatbot for POST /chatbot/query
app.include_router(advisor.router, prefix="/chatbot", tags=["Chatbot"])

# Direct endpoints matching PS26091 specification
app.include_router(feasibility.router, prefix="/feasibility", tags=["Feasibility"])
app.include_router(feasibility.router, prefix="/finance", tags=["Finance"])

# API v1 standard namespaces
app.include_router(advisor.router, prefix="/api/v1/advisor", tags=["AI Advisor"])
app.include_router(khata.router, prefix="/api/v1/khata", tags=["Khata & OCR"])
app.include_router(schemes.router, prefix="/api/v1/schemes", tags=["Government Schemes"])
app.include_router(kyc.router, prefix="/api/v1/kyc", tags=["KYC & Bank Sandbox"])
app.include_router(feasibility.router, prefix="/api/v1/feasibility", tags=["Feasibility PS26091"])

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": "SahayakAI Backend",
        "sih_team": "SIH26091 - Smart India Hackathon 2026",
        "single_source": "BUSINESS_PROFILES in Python",
        "gemini_pipeline": "Active" if os.getenv("GEMINI_API_KEY") else "Offline",
        "scheduler": get_scheduler_status(),
        "endpoints": [
            "POST /chatbot/query",
            "GET /chatbot/profiles",
            "GET /chatbot/forecasts"
        ]
    }
