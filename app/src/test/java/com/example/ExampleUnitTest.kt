package com.example

import com.example.data.core.BusinessProfiles
import com.example.data.ml.MlForecastingEngine
import com.example.data.model.AppLanguage
import com.example.data.model.BusinessType
import com.example.data.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAllBusinessTypesHaveExplicitProfiles() {
        for (type in BusinessType.values()) {
            val profile = BusinessProfiles.getProfile(type)
            assertNotNull("Profile must exist for ${type.name}", profile)
            assertTrue("Advisory topics must not be empty for ${type.name}", profile.advisoryTopics.isNotEmpty())
            assertTrue("Exclude topics must not be empty for ${type.name}", profile.excludeTopics.isNotEmpty())
        }
    }

    @Test
    fun testTeaShopProfileConfiguration() {
        val teaShop = BusinessProfiles.getProfile(BusinessType.FOOD_STALL)
        assertEquals("Tea Shop / Tea Stall", teaShop.primaryTitle)
        assertTrue("Must track Tea", teaShop.relevantCommodities.contains("Tea"))
        assertTrue("Must track Sugar", teaShop.relevantCommodities.contains("Sugar"))
        assertFalse("Must NOT track Onion on Agmarknet", teaShop.relevantCommodities.contains("Onion"))

        // Exclude topics must contain Kirana staples
        assertTrue("Must exclude onion", teaShop.excludeTopics.contains("onion"))
        assertTrue("Must exclude mustard oil", teaShop.excludeTopics.contains("mustard oil"))
        assertTrue("Must exclude dal", teaShop.excludeTopics.contains("dal"))
    }

    @Test
    fun testGuardrailCatchesKiranaDriftInTeaShopResponse() {
        val teaShop = BusinessProfiles.getProfile(BusinessType.FOOD_STALL)

        val badResponse = "As a tea stall, you should buy wholesale onion and mustard oil from the Mandi to stock up on dal sacks."
        val violations = BusinessProfiles.validateResponseRelevance(badResponse, teaShop)

        assertTrue("Guardrail must flag onion", violations.contains("onion"))
        assertTrue("Guardrail must flag mustard oil", violations.contains("mustard oil"))
        assertTrue("Guardrail must flag dal", violations.contains("dal"))

        val cleanResponse = "For your tea stall, negotiate directly with local dairy farmers for daily fresh milk and service your commercial LPG burners to save fuel."
        val cleanViolations = BusinessProfiles.validateResponseRelevance(cleanResponse, teaShop)
        assertTrue("Clean response must have zero violations", cleanViolations.isEmpty())
    }

    @Test
    fun testMlContextFilteringForTeaShop() {
        val teaShop = BusinessProfiles.getProfile(BusinessType.FOOD_STALL)
        val promptContext = MlForecastingEngine.generateGeminiPromptContext(teaShop.relevantCommodities)

        assertTrue("Must contain Tea forecast", promptContext.contains("Tea", ignoreCase = true))
        assertTrue("Must contain Sugar forecast", promptContext.contains("Sugar", ignoreCase = true))
        assertFalse("Must NOT contain Onion", promptContext.contains("Onion", ignoreCase = true))
        assertFalse("Must NOT contain Mustard Oil", promptContext.contains("Mustard Oil", ignoreCase = true))
    }

    @Test
    fun testOfflineAdviceTailoredToTeaShop() {
        val teaShop = BusinessProfiles.getProfile(BusinessType.FOOD_STALL)
        val user = UserProfile(
            id = "usr_test",
            name = "Manoj Kumar",
            businessType = BusinessType.FOOD_STALL,
            location = "Varanasi, UP",
            preferredLanguage = AppLanguage.HINDI
        )

        val hindiAdvice = teaShop.offlineAdviceGenerator("दुकान का मुनाफा कैसे बढ़ाएं?", user, true)
        assertTrue("Hindi advice must mention milk / दूध", hindiAdvice.contains("दूध"))
        assertTrue("Hindi advice must mention LPG / गैस", hindiAdvice.contains("गैस") || hindiAdvice.contains("LPG"))
        assertTrue("Hindi advice must mention चाय / Tea", hindiAdvice.contains("चाय"))
        assertFalse("Must not mention प्याज / onion", hindiAdvice.contains("प्याज"))
        assertFalse("Must not mention सरसों तेल / mustard oil", hindiAdvice.contains("सरसों"))
    }
}
