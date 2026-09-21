package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Supported business categories for PS26091 Feasibility Module.
 * Matches the backend fixed list exactly.
 */
val FEASIBILITY_BUSINESS_CATEGORIES = listOf(
    "Dairy",
    "Retail / Kirana",
    "Textiles",
    "Handicrafts",
    "Food Processing",
    "Agriculture",
    "Poultry",
    "Tailoring",
    "Street Vendor",
    "Other"
)

data class FeasibilityInput(
    val location: String = "Varanasi, UP",
    val availableMargin: Double = 100000.0,
    val businessCategory: String = "Dairy"
)

data class CompetitorMapping(
    val countNearby: Int,
    val radiusKm: Double = 7.5,
    val densityNote: String,
    val sampleNames: List<String> = emptyList()
)

data class MarketReach(
    val estimatedConsumerBase: String,
    val radiusKm: Double = 7.5,
    val distributionChannelHint: String,
    val district: String = ""
)

data class SwotAnalysis(
    val strengths: List<String>,
    val weaknesses: List<String>,
    val opportunities: List<String>,
    val threats: List<String>
)

data class ProductMarketValue(
    val source: String, // "ml_forecast" or "general_guidance"
    val badgeLabel: String,
    val summary: String,
    val guidanceOrText: String = "",
    val commodities: List<MandiCommodity> = emptyList()
)

data class FinancialStructure(
    val availableMargin: Double,
    val projectCost: Double,
    val loanAmount: Double,
    val scheme: String,
    val interestRate: Double,
    val tenureYears: Int,
    val moratoriumMonths: Int,
    val maxLoanCap: Double = 4500000.0
)

data class QuarterlyEmiItem(
    val quarter: Int,
    val month: Int,
    val status: String, // "moratorium" or "repayment"
    val emi: Double,
    val principal: Double,
    val interest: Double,
    val balance: Double
)

data class FeasibilityReport(
    val checkId: String,
    val location: String,
    val availableMargin: Double,
    val businessCategory: String,
    val competitorMapping: CompetitorMapping,
    val marketReach: MarketReach,
    val opportunityAnalysis: String,
    val swot: SwotAnalysis,
    val threats: String,
    val productMarketValue: ProductMarketValue,
    val financialStructure: FinancialStructure,
    val quarterlySchedule: List<QuarterlyEmiItem>,
    val createdAt: String
)

/**
 * Room Entity for persisting feasibility checks so a user can run this multiple times
 * for different business ideas and compare past checks.
 */
@Entity(tableName = "feasibility_checks")
data class FeasibilityCheckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val checkId: String,
    val location: String,
    val availableMargin: Double,
    val businessCategory: String,
    val projectCost: Double,
    val loanAmount: Double,
    val scheme: String,
    val interestRate: Double,
    val tenureYears: Int,
    val moratoriumMonths: Int,
    val competitorCount: Int,
    val competitorDensityNote: String,
    val sampleCompetitorsJson: String,
    val marketReachPopulation: String,
    val distributionChannelHint: String,
    val opportunityAnalysis: String,
    val swotStrengthsJson: String,
    val swotWeaknessesJson: String,
    val swotOpportunitiesJson: String,
    val swotThreatsJson: String,
    val threatsNarrative: String,
    val productMarketValueSource: String,
    val productMarketValueSummary: String,
    val productMarketValueDetail: String,
    val quarterlyScheduleJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
