"""
Scheduler Service for SahayakAI Backend
Uses APScheduler to run background scheduled jobs that check upcoming and overdue business reminders
(such as PM Mudra quarterly EMI dates, PMEGP subsidy deadline, GST/Tax filings).
"""

import logging
import time
from datetime import datetime, timedelta
from typing import List, Dict, Any, Optional
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.interval import IntervalTrigger

logger = logging.getLogger("scheduler_service")

# In-memory store for active business reminders
REMINDERS_DB: List[Dict[str, Any]] = [
    {
        "id": "rem_mudra_q1",
        "title": "PM Mudra Quarterly EMI Payment",
        "title_hi": "पीएम मुद्रा त्रैमासिक ईएमआई भुगतान",
        "amount": 4650.0,
        "due_date": (datetime.now() + timedelta(days=5)).strftime("%Y-%m-%d"),
        "category": "LOAN_EMI",
        "is_completed": False,
        "notified": False,
        "notes": "Bank of Baroda account deduction"
    },
    {
        "id": "rem_pmegp_doc",
        "title": "PMEGP Rural Subsidy Document Verification",
        "title_hi": "PMEGP ग्रामीण सब्सिडी दस्तावेज सत्यापन",
        "amount": 0.0,
        "due_date": (datetime.now() + timedelta(days=12)).strftime("%Y-%m-%d"),
        "category": "SCHEME_DEADLINE",
        "is_completed": False,
        "notified": False,
        "notes": "Submit electricity bill and shop proof at DIC office"
    }
]

# Audit log of trigger checks executed by the scheduler
SCHEDULER_AUDIT_LOG: List[Dict[str, Any]] = []

scheduler: Optional[BackgroundScheduler] = None

def check_reminders_job():
    """
    Periodic job triggered by APScheduler.
    Inspects all pending business reminders, checks if any are due today or in near future,
    and logs an alert notification.
    """
    now = datetime.now()
    today_str = now.strftime("%Y-%m-%d")
    triggered_alerts = []

    for rem in REMINDERS_DB:
        if not rem["is_completed"]:
            due_date = rem["due_date"]
            if due_date <= today_str:
                triggered_alerts.append({
                    "id": rem["id"],
                    "title": rem["title"],
                    "status": "DUE_NOW",
                    "due_date": due_date
                })
                rem["notified"] = True
            elif due_date <= (now + timedelta(days=7)).strftime("%Y-%m-%d"):
                triggered_alerts.append({
                    "id": rem["id"],
                    "title": rem["title"],
                    "status": "UPCOMING_SOON",
                    "due_date": due_date
                })

    record = {
        "timestamp": now.strftime("%Y-%m-%d %H:%M:%S"),
        "checked_reminders_count": len(REMINDERS_DB),
        "alerts_triggered": len(triggered_alerts),
        "alerts": triggered_alerts
    }
    SCHEDULER_AUDIT_LOG.append(record)
    if len(SCHEDULER_AUDIT_LOG) > 50:
        SCHEDULER_AUDIT_LOG.pop(0)

    logger.info(f"[APSCHEDULER RUN] Checked {len(REMINDERS_DB)} reminders. Triggered {len(triggered_alerts)} alerts.")

def start_scheduler():
    global scheduler
    if scheduler is None or not scheduler.running:
        scheduler = BackgroundScheduler()
        # Run check every 60 seconds
        scheduler.add_job(
            check_reminders_job,
            trigger=IntervalTrigger(seconds=60),
            id="reminder_checker_job",
            name="SahayakAI Reminder & Scheme Deadline Checker",
            replace_existing=True
        )
        scheduler.start()
        logger.info("[APSCHEDULER STARTED] Reminder background checker running every 60 seconds.")
        # Execute immediate initial run
        check_reminders_job()

def shutdown_scheduler():
    global scheduler
    if scheduler and scheduler.running:
        scheduler.shutdown(wait=False)
        logger.info("[APSCHEDULER STOPPED]")

def add_reminder(reminder_dict: Dict[str, Any]) -> Dict[str, Any]:
    REMINDERS_DB.append(reminder_dict)
    # Trigger an immediate check
    check_reminders_job()
    return reminder_dict

def get_all_reminders() -> List[Dict[str, Any]]:
    return REMINDERS_DB

def get_scheduler_status() -> Dict[str, Any]:
    return {
        "running": scheduler.running if scheduler else False,
        "jobs": [j.id for j in scheduler.get_jobs()] if scheduler else [],
        "reminders_count": len(REMINDERS_DB),
        "last_runs": SCHEDULER_AUDIT_LOG[-5:]
    }
