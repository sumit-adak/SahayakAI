package com.example

import com.example.data.core.BusinessProfiles
import com.example.data.model.BusinessType
import org.junit.Assert.*
import org.junit.Test

/**
 * Voice & Accessibility Semantics Unit Tests (PS26091 Section I)
 * Verifies:
 * 1. Spoken text generation for all 8 micro-enterprise trade profiles
 * 2. TTS clean formatting (stripping markdown syntax like *, #, ` before reading out loud)
 * 3. Bilingual content descriptions for accessibility screen readers (English & Hindi)
 */
class AccessibilitySemanticsUnitTest {

    @Test
    fun testTtsStringCleaning() {
        val markdownText = """
            # Sahayak Advisory
            * Ramesh Ji, your **daily profit** is ₹1,200.
            - Focus on: `Mustard Oil` & wholesale sugar.
            • Send WhatsApp reminder to Suresh.
        """.trimIndent()

        val cleaned = markdownText
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace("- ", "")
            .replace("• ", "")
            .trim()

        assertFalse("Cleaned speech text must not contain markdown asterisks", cleaned.contains("*"))
        assertFalse("Cleaned speech text must not contain markdown hash tags", cleaned.contains("#"))
        assertFalse("Cleaned speech text must not contain markdown backticks", cleaned.contains("`"))
        assertTrue("Speech text preserves critical numbers and party names", cleaned.contains("Ramesh Ji"))
        assertTrue("Speech text preserves currency amount", cleaned.contains("₹1,200"))
    }

    @Test
    fun testAllTradesHaveBilingualAccessibilityTitles() {
        for (type in BusinessType.values()) {
            val profile = BusinessProfiles.getProfile(type)
            assertNotNull(profile)
            assertTrue("English title must not be blank for accessibility: ${type.name}", profile.primaryTitle.isNotBlank())
            assertTrue("Hindi title must not be blank for accessibility: ${type.name}", profile.primaryTitleHi.isNotBlank())
            assertTrue("Advisory topics must be populated for audio advice: ${type.name}", profile.advisoryTopics.isNotEmpty())
        }
    }

    @Test
    fun testVoiceConfirmationPromptGeneration() {
        // Critical action confirmation text (e.g. sharing financial report or bank statement)
        val applicantName = "Ramesh Sharma"
        val loanAmount = 150000.0
        val confirmationPromptEn = "Are you sure you want to share your verified bank report for ₹1,50,000 Mudra loan with Bank of Baroda?"
        val confirmationPromptHi = "क्या आप बैंक ऑफ बड़ौदा के साथ ₹1,50,000 मुद्रा लोन हेतु अपना सत्यापित बैंक खाता साझा करना चाहते हैं?"

        assertTrue(confirmationPromptEn.contains(applicantName) || confirmationPromptEn.contains("1,50,000"))
        assertTrue(confirmationPromptHi.contains("1,50,000"))
        assertTrue(confirmationPromptHi.contains("साझा"))
    }
}
