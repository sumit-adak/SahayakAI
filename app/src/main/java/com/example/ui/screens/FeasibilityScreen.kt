package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.viewmodel.SahayakViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeasibilityScreen(
    viewModel: SahayakViewModel,
    onBackClick: () -> Unit,
    onAskAdvisor: (String) -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val pastChecks by viewModel.pastFeasibilityChecks.collectAsState()
    val report by viewModel.feasibilityReport.collectAsState()
    val isLoading by viewModel.feasibilityLoading.collectAsState()
    val errorMessage by viewModel.feasibilityError.collectAsState()

    var locationInput by remember { mutableStateOf(userProfile.location.ifBlank { "Varanasi, UP" }) }
    var marginInput by remember { mutableStateOf("100000") }
    var selectedCategory by remember { mutableStateOf("Dairy") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "व्यावसायिक व्यवहार्यता (Feasibility)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "PS26091 • Grounded Local Economics & Scheme Router",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("feasibility_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (report != null) {
                        IconButton(
                            onClick = { viewModel.clearFeasibilityReport() },
                            modifier = Modifier.testTag("feasibility_new_check_button")
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "New Check")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("feasibility_screen_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // PAST CHECKS COMPARISON BAR
            // ==========================================
            if (pastChecks.isNotEmpty()) {
                item {
                    PastChecksComparisonBar(
                        pastChecks = pastChecks,
                        activeReportCheckId = report?.checkId,
                        onSelectCheck = { entity -> viewModel.loadPastCheck(entity) },
                        onDeleteCheck = { entity -> viewModel.deletePastCheck(entity) }
                    )
                }
            }

            // ==========================================
            // MODULE 1: THREE-INPUT ENTRY FORM
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feasibility_input_form_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("1", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "व्यावसायिक विवरण (3-Input Entry)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Enter location, margin capital and category to generate feasibility.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 1. Location Input
                        OutlinedTextField(
                            value = locationInput,
                            onValueChange = { locationInput = it },
                            label = { Text("स्थान / जिला (Location / District)") },
                            placeholder = { Text("e.g. Varanasi, Mirzapur, Lucknow") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("feasibility_location_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // 2. Margin Capital Input & Presets
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = marginInput,
                                onValueChange = { marginInput = it },
                                label = { Text("उपलब्ध पूंजी / मार्जिन मनी (Available Margin)") },
                                placeholder = { Text("₹ 1,00,000") },
                                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("feasibility_margin_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Quick preset chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val presets = listOf(
                                    15000 to "₹15,000 (Micro)",
                                    50000 to "₹50,000",
                                    100000 to "₹1,00,000 (Term Loan)",
                                    250000 to "₹2,50,000",
                                    500000 to "₹5,00,000"
                                )
                                presets.forEach { (amount, label) ->
                                    FilterChip(
                                        selected = marginInput == amount.toString(),
                                        onClick = { marginInput = amount.toString() },
                                        label = { Text(label, fontSize = 12.sp) },
                                        modifier = Modifier.testTag("preset_margin_${amount}")
                                    )
                                }
                            }
                        }

                        // 3. Business Category Dropdown (Fixed PS26091 List)
                        ExposedDropdownMenuBox(
                            expanded = categoryDropdownExpanded,
                            onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("व्यवसाय श्रेणी (Business Category)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("feasibility_category_dropdown"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                FEASIBILITY_BUSINESS_CATEGORIES.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            selectedCategory = cat
                                            categoryDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("category_item_$cat")
                                    )
                                }
                            }
                        }

                        // Error message banner if any
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Run Button
                        Button(
                            onClick = {
                                val marginVal = marginInput.toDoubleOrNull() ?: 100000.0
                                viewModel.runFeasibilityCheck(
                                    location = locationInput,
                                    margin = marginVal,
                                    category = selectedCategory
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_feasibility_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("विश्लेषण हो रहा है (Analyzing Local Data)...")
                            } else {
                                Icon(Icons.Default.Analytics, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("व्यवहार्यता रिपोर्ट तैयार करें (Run Feasibility Check)", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // REPORT OUTPUT SECTIONS
            // ==========================================
            if (report != null) {
                val currentReport = report!!

                // SECTION 2: BUSINESS FEASIBILITY REPORT (Modules 2, 3, 4, 5)
                item {
                    ReportHeaderBanner(report = currentReport)
                }

                // Module 3: Market Reach
                item {
                    MarketReachCard(marketReach = currentReport.marketReach)
                }

                // Module 4: Opportunity Analysis
                item {
                    OpportunityAnalysisCard(analysis = currentReport.opportunityAnalysis)
                }

                // Module 4: SWOT Matrix (4 Quadrants)
                item {
                    SwotMatrixCard(swot = currentReport.swot)
                }

                // Module 4: Threats & Risks
                item {
                    ThreatsCard(threats = currentReport.threats)
                }

                // Module 2: Competitor Mapping
                item {
                    CompetitorMappingCard(competitor = currentReport.competitorMapping)
                }

                // Module 5: Product Market Value
                item {
                    ProductMarketValueCard(value = currentReport.productMarketValue)
                }

                // SECTION 3: FINANCIAL STRUCTURING (Module 6)
                item {
                    FinancialStructuringCard(structure = currentReport.financialStructure)
                }

                // SECTION 4: REPAYMENT SCHEDULE (Module 6)
                item {
                    QuarterlyScheduleCard(schedule = currentReport.quarterlySchedule)
                }

                // SECTION 5: ACTION FOOTER
                item {
                    ReportActionFooter(
                        report = currentReport,
                        onShare = {
                            val shareText = buildString {
                                appendLine("📊 *SahayakAI Business Feasibility Report (PS26091)*")
                                appendLine("Category: ${currentReport.businessCategory} | Location: ${currentReport.location}")
                                appendLine("Margin: ₹${String.format(Locale.ROOT, "%,.0f", currentReport.availableMargin)}")
                                appendLine("Total Project Cost: ₹${String.format(Locale.ROOT, "%,.0f", currentReport.financialStructure.projectCost)}")
                                appendLine("Loan Amount: ₹${String.format(Locale.ROOT, "%,.0f", currentReport.financialStructure.loanAmount)} (${currentReport.financialStructure.scheme})")
                                appendLine("Interest: ${currentReport.financialStructure.interestRate}% | Moratorium: ${currentReport.financialStructure.moratoriumMonths} Months")
                                appendLine("\nCompetitor Density: ${currentReport.competitorMapping.densityNote} (${currentReport.competitorMapping.countNearby} nearby)")
                                appendLine("Market Reach: ${currentReport.marketReach.estimatedConsumerBase}")
                                appendLine("\nGenerated via SahayakAI Rural Financial Platform")
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Feasibility Report"))
                        },
                        onAskAdvisor = {
                            val prompt = "Please review my ${currentReport.businessCategory} feasibility report in ${currentReport.location} with project cost ₹${String.format(Locale.ROOT, "%,.0f", currentReport.financialStructure.projectCost)} and loan ₹${String.format(Locale.ROOT, "%,.0f", currentReport.financialStructure.loanAmount)} under ${currentReport.financialStructure.scheme}."
                            onAskAdvisor(prompt)
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS FOR FEASIBILITY SCREEN
// ==========================================

@Composable
fun PastChecksComparisonBar(
    pastChecks: List<FeasibilityCheckEntity>,
    activeReportCheckId: String?,
    onSelectCheck: (FeasibilityCheckEntity) -> Unit,
    onDeleteCheck: (FeasibilityCheckEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "तुलना करें (Compare Past Ideas - ${pastChecks.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Tap to switch • Long press to delete",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pastChecks.forEach { check ->
                val isSelected = check.checkId == activeReportCheckId
                InputChip(
                    selected = isSelected,
                    onClick = { onSelectCheck(check) },
                    label = {
                        Text(
                            "${check.businessCategory} • ₹${String.format(Locale.ROOT, "%,.0f", check.availableMargin / 1000)}k (${check.scheme.take(10)})",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { onDeleteCheck(check) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(12.dp))
                        }
                    },
                    modifier = Modifier.testTag("past_check_chip_${check.checkId}")
                )
            }
        }
    }
}

@Composable
fun ReportHeaderBanner(report: FeasibilityReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text(
                        "REPORT ID: ${report.checkId.uppercase()}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = report.createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Text(
                text = "${report.businessCategory} व्यवहार्यता मूल्यांकन",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "स्थान: ${report.location} • उपलब्ध मार्जिन: ₹${String.format(Locale.ROOT, "%,.0f", report.availableMargin)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun MarketReachCard(marketReach: MarketReach) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(icon = Icons.Default.People, title = "बाजार पहुंच (Market Reach)", badge = "Census 2011 Data")

            Text(
                text = "अनुमानित उपभोक्ता आधार (Estimated Consumer Base):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = marketReach.estimatedConsumerBase,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "वितरण चैनल रणनीति (Distribution Channel Strategy):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = marketReach.distributionChannelHint,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun OpportunityAnalysisCard(analysis: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(icon = Icons.Default.Lightbulb, title = "अवसर विश्लेषण (Opportunity Analysis)", badge = "Local Density Grounded")
            Text(
                text = analysis,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun SwotMatrixCard(swot: SwotAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(icon = Icons.Default.Grid4x4, title = "SWOT विश्लेषण (4-Quadrant Matrix)", badge = "Capital-Tailored")

            // 2x2 Quadrant Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Strengths & Weaknesses
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SwotQuadrantBox(
                        title = "ताकत (Strengths)",
                        points = swot.strengths,
                        containerColor = Color(0xFFE8F5E9),
                        contentColor = Color(0xFF1B5E20),
                        modifier = Modifier.weight(1f)
                    )
                    SwotQuadrantBox(
                        title = "कमजोरी (Weaknesses)",
                        points = swot.weaknesses,
                        containerColor = Color(0xFFFFF3E0),
                        contentColor = Color(0xFFE65100),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Opportunities & Threats
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SwotQuadrantBox(
                        title = "अवसर (Opportunities)",
                        points = swot.opportunities,
                        containerColor = Color(0xFFE3F2FD),
                        contentColor = Color(0xFF0D47A1),
                        modifier = Modifier.weight(1f)
                    )
                    SwotQuadrantBox(
                        title = "खतरे (Threats)",
                        points = swot.threats,
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = Color(0xFFB71C1C),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SwotQuadrantBox(
    title: String,
    points: List<String>,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            points.forEach { point ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("• ", color = contentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ThreatsCard(threats: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(icon = Icons.Default.Warning, title = "स्थानीय जोखिम व चुनौतियाँ (Threats & Risks)", badge = "Operational")
            Text(
                text = threats,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CompetitorMappingCard(competitor: CompetitorMapping) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(icon = Icons.Default.Storefront, title = "प्रतिस्पर्धी मैपिंग (Competitor Mapping)", badge = "Google Places / Demographic")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${competitor.countNearby} निकटवर्ती व्यवसाय",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${competitor.radiusKm} किमी के दायरे में",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Density Badge
                val (badgeBg, badgeText) = when {
                    competitor.countNearby <= 2 -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                    competitor.countNearby <= 6 -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                    else -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = competitor.densityNote,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeText
                    )
                }
            }

            if (competitor.sampleNames.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    text = "सत्यापित स्थानीय प्रतिष्ठान (Verified Local Benchmarks):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                competitor.sampleNames.forEach { name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(name, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductMarketValueCard(value: ProductMarketValue) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "उत्पाद बाजार मूल्य (Product Value)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Distinct Data Source Badge (Agmarknet ML vs General Guidance)
                val isMl = value.source == "ml_forecast"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMl) Color(0xFFE8F5E9) else Color(0xFFEDE7F6))
                        .border(1.dp, if (isMl) Color(0xFF4CAF50) else Color(0xFF7E57C2), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = value.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isMl) Color(0xFF2E7D32) else Color(0xFF4527A0)
                    )
                }
            }

            Text(
                text = value.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (value.commodities.isNotEmpty()) {
                // Table of wholesale commodities
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    value.commodities.forEach { comm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(comm.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(comm.marketLocation, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "₹${comm.pricePerUnit} ${comm.unit}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else if (value.guidanceOrText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = value.guidanceOrText,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialStructuringCard(structure: FinancialStructure) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "वित्तीय संरचना (Financial Structuring)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                    Text("10% Margin Rule", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }

            // Headline figures grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinancialMetricBox(
                    title = "कुल परियोजना लागत (Project Cost)",
                    value = "₹${String.format(Locale.ROOT, "%,.0f", structure.projectCost)}",
                    subtitle = "10x of your margin",
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricBox(
                    title = "ऋण राशि (Loan Amount)",
                    value = "₹${String.format(Locale.ROOT, "%,.0f", structure.loanAmount)}",
                    subtitle = "90% Debt Financing",
                    isHighlight = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinancialMetricBox(
                    title = "लागू योजना (Eligible Scheme)",
                    value = structure.scheme,
                    subtitle = "Govt. Supported",
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricBox(
                    title = "ब्याज दर (Interest Rate)",
                    value = "${structure.interestRate}% p.a.",
                    subtitle = "Concessional",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinancialMetricBox(
                    title = "ऋण अवधि (Tenure)",
                    value = "${structure.tenureYears} वर्ष (${structure.tenureYears * 12} माह)",
                    subtitle = "Total Period",
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricBox(
                    title = "मोराटोरियम (Moratorium)",
                    value = "${structure.moratoriumMonths} महीने",
                    subtitle = "Zero principal during setup",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FinancialMetricBox(
    title: String,
    value: String,
    subtitle: String,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isHighlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun QuarterlyScheduleCard(schedule: List<QuarterlyEmiItem>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "त्रैमासिक ईएमआई अनुसूची (Repayment)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "कम देखें" else "सभी ${schedule.size} देखें")
                }
            }

            Text(
                text = "मोराटोरियम अवधि में मूलधन शून्य रहता है। केवल त्रैमासिक स्थिति प्रदर्शित है।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("तिमाही (Qtr)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("माह (Mo)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                Text("स्थिति", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                Text("किस्त (EMI)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                Text("बकाया (Bal)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f), textAlign = TextAlign.End)
            }

            val displayItems = if (expanded) schedule else schedule.take(4)

            displayItems.forEach { item ->
                val isMoratorium = item.status == "moratorium"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Q${item.quarter}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("M${item.month}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.8f))
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isMoratorium) Color(0xFFFFF3E0) else Color(0xFFE8F5E9))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isMoratorium) "छूट (Morat)" else "किस्त",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMoratorium) Color(0xFFE65100) else Color(0xFF2E7D32)
                        )
                    }
                    Text(
                        text = if (isMoratorium) "₹0" else "₹${String.format(Locale.ROOT, "%,.0f", item.emi)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.3f),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "₹${String.format(Locale.ROOT, "%,.0f", item.balance)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1.4f),
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun ReportActionFooter(
    report: FeasibilityReport,
    onShare: () -> Unit,
    onAskAdvisor: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("share_feasibility_report_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("रिपोर्ट साझा करें (Share via WhatsApp)", color = Color.White, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onAskAdvisor,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ask_advisor_feasibility_button")
        ) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("इस रिपोर्ट पर AI सलाहकार से बात करें (Ask Advisor)", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SectionTitle(icon: ImageVector, title: String, badge: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            Text(badge, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
    }
}
