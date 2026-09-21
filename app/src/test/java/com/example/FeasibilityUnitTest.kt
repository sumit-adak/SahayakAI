package com.example

import com.example.data.model.*
import com.example.data.service.FeasibilityService
import org.junit.Assert.*
import org.junit.Test
class FeasibilityUnitTest {

    private val service = FeasibilityService()

    @Test
    fun testFinancialStructureTermLoan() {
        val structure = service.calculateProjectStructure(100000.0)
        assertEquals(100000.0, structure.availableMargin, 0.01)
        assertEquals(1000000.0, structure.projectCost, 0.01)
        assertEquals(900000.0, structure.loanAmount, 0.01)
        assertEquals("Term Loan Scheme", structure.scheme)
        assertEquals(8.0, structure.interestRate, 0.01)
        assertEquals(7, structure.tenureYears)
        assertEquals(6, structure.moratoriumMonths)
    }

    @Test
    fun testFinancialStructureMicroFinance() {
        val structure = service.calculateProjectStructure(12000.0)
        assertEquals(12000.0, structure.availableMargin, 0.01)
        assertEquals(120000.0, structure.projectCost, 0.01)
        assertEquals(108000.0, structure.loanAmount, 0.01)
        assertEquals("Micro Finance Scheme", structure.scheme)
        assertEquals(6.5, structure.interestRate, 0.01)
        assertEquals(3, structure.tenureYears)
        assertEquals(3, structure.moratoriumMonths)
    }

    @Test
    fun testQuarterlyScheduleMoratoriumAndRepayment() {
        val schedule = service.generateRepaymentSchedule(
            loanAmount = 900000.0,
            interestRate = 8.0,
            tenureYears = 7,
            moratoriumMonths = 6
        )

        // 7 years * 4 quarters = 28 quarters
        assertEquals(28, schedule.size)

        // Q1 & Q2: In moratorium period
        assertEquals("moratorium", schedule[0].status)
        assertEquals(0.0, schedule[0].emi, 0.01)
        assertEquals(0.0, schedule[0].principal, 0.01)
        assertEquals(900000.0, schedule[0].balance, 0.01)

        assertEquals("moratorium", schedule[1].status)
        assertEquals(0.0, schedule[1].emi, 0.01)
        assertEquals(0.0, schedule[1].principal, 0.01)
        assertEquals(900000.0, schedule[1].balance, 0.01)

        // Q3: Repayment starts
        assertEquals("repayment", schedule[2].status)
        assertTrue("EMI must be positive", schedule[2].emi > 0)
        assertTrue("Balance must decrease below 900,000", schedule[2].balance < 900000.0)

        // Final quarter: Balance amortized to 0
        assertEquals(0.0, schedule.last().balance, 0.01)
    }

    @Test
    fun testProductMarketValueAgmarknetLinking() {
        val dairyValue = service.getProductMarketValue("Dairy", "Varanasi")
        assertEquals("ml_forecast", dairyValue.source)
        assertEquals("Agmarknet ML Forecast", dairyValue.badgeLabel)
        assertTrue("Commodities list must not be empty", dairyValue.commodities.isNotEmpty())

        val retailValue = service.getProductMarketValue("Retail / Kirana", "Varanasi")
        assertEquals("general_guidance", retailValue.source)
        assertEquals("General Pricing Guidance", retailValue.badgeLabel)
        assertTrue("Guidance text must be provided", retailValue.guidanceOrText.isNotBlank())
    }

    @Test
    fun testCensusMarketReachLookup() {
        val varanasiReach = service.getMarketReach("Varanasi", 7.5)
        assertTrue("Must reference Varanasi Census 2011 population", varanasiReach.estimatedConsumerBase.contains("3,676,841"))

        val fallbackReach = service.getMarketReach("SomeRemoteDistrict", 7.5)
        assertTrue("Fallback message must be returned", fallbackReach.estimatedConsumerBase.contains("No population data available"))
    }

    @Test
    fun testEntityModelRoundTrip() {
        val report = FeasibilityReport(
            checkId = "chk_test123",
            location = "Lucknow, UP",
            availableMargin = 100000.0,
            businessCategory = "Dairy",
            competitorMapping = CompetitorMapping(3, 7.5, "Moderate", listOf("Shop A")),
            marketReach = MarketReach(estimatedConsumerBase = "Consumer Base 4M", distributionChannelHint = "Direct Delivery"),
            opportunityAnalysis = "Strong local opportunity",
            swot = SwotAnalysis(listOf("S1"), listOf("W1"), listOf("O1"), listOf("T1")),
            threats = "Fodder inflation",
            productMarketValue = ProductMarketValue(
                source = "ml_forecast",
                badgeLabel = "Agmarknet ML Forecast",
                summary = "Summary",
                guidanceOrText = "",
                commodities = emptyList()
            ),
            financialStructure = FinancialStructure(
                availableMargin = 100000.0,
                projectCost = 1000000.0,
                loanAmount = 900000.0,
                scheme = "Term Loan Scheme",
                interestRate = 8.0,
                tenureYears = 7,
                moratoriumMonths = 6
            ),
            quarterlySchedule = listOf(QuarterlyEmiItem(1, 3, "moratorium", 0.0, 0.0, 0.0, 900000.0)),
            createdAt = "2026-09-19"
        )

        val entity = service.reportToEntity(report)
        assertEquals(report.checkId, entity.checkId)
        assertEquals(report.availableMargin, entity.availableMargin, 0.01)

        val reconstructed = service.entityToReport(entity)
        assertEquals(report.checkId, reconstructed.checkId)
        assertEquals(report.location, reconstructed.location)
        assertEquals(report.businessCategory, reconstructed.businessCategory)
        assertEquals(report.financialStructure.scheme, reconstructed.financialStructure.scheme)
        assertEquals(report.financialStructure.loanAmount, reconstructed.financialStructure.loanAmount, 0.01)
        assertEquals(report.quarterlySchedule.size, reconstructed.quarterlySchedule.size)
    }
}
