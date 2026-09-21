package com.example.data.service

import java.util.Locale

data class ModerationCheckResult(
    val isApproved: Boolean,
    val reasonMessage: String = "",
    val reasonMessageHi: String = ""
)

object CommunityModerationService {

    // Simple keyword-based safety & anti-spam blocklist (English, Hindi, Hinglish)
    // No AI or Chatbot dependencies - pure local deterministic filter
    private val blockedSpamKeywords = listOf(
        "crypto", "bitcoin", "telegram channel", "earn 10000 daily", "lottery winner",
        "free money", "hack account", "rummy king", "teen patti hack", "betting app",
        "get rich quick", "pyramid scheme", "investment scheme guaranteed 50%", "click this link now",
        "scam", "cheat", "frauds list"
    )

    private val blockedAbusiveKeywords = listOf(
        "abuse", "harass", "threat", "violence", "kill", "hate",
        "गाली", "धोखेबाज", "फर्जी", "मारपीट", "नुकसान", "चोर"
    )

    /**
     * Evaluates user-submitted content (post caption, comment, question) against keyword blocklist.
     */
    fun checkContent(text: String): ModerationCheckResult {
        val trimmed = text.trim()
        if (trimmed.length < 3) {
            return ModerationCheckResult(
                isApproved = false,
                reasonMessage = "Content is too short. Please provide a clear message or question.",
                reasonMessageHi = "सामग्री बहुत छोटी है। कृपया स्पष्ट विवरण या प्रश्न लिखें।"
            )
        }

        val lower = trimmed.lowercase(Locale.ROOT)

        for (spamWord in blockedSpamKeywords) {
            if (lower.contains(spamWord)) {
                return ModerationCheckResult(
                    isApproved = false,
                    reasonMessage = "Post flagged by community moderation: promotional spam/suspicious links not allowed.",
                    reasonMessageHi = "समुदाय सुरक्षा नीति: प्रचार, स्पैम या सट्टा सामग्री की अनुमति नहीं है।"
                )
            }
        }

        for (abusiveWord in blockedAbusiveKeywords) {
            if (lower.contains(abusiveWord)) {
                return ModerationCheckResult(
                    isApproved = false,
                    reasonMessage = "Post flagged: contains prohibited abusive or harmful language.",
                    reasonMessageHi = "समुदाय सुरक्षा नीति: अभद्र या आपत्तिजनक भाषा की अनुमति नहीं है।"
                )
            }
        }

        return ModerationCheckResult(isApproved = true)
    }
}
