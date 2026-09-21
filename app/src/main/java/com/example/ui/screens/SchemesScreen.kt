package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel
import java.util.Locale

@Composable
fun SchemesScreen(
    viewModel: SahayakViewModel,
    onNavigateToAdvisorWithPrompt: (String) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val schemes by viewModel.allSchemes.collectAsState()
    val reminders by viewModel.allReminders.collectAsState()
    val selectedCategory by viewModel.selectedSchemeCategory.collectAsState()
    val isHindi = userProfile.preferredLanguage == AppLanguage.HINDI

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = if (isHindi) listOf("सरकारी योजनाएं व सब्सिडी", "रिमाइंडर व देय तिथियां") else listOf("Govt Schemes & Subsidies", "Reminders & Deadlines")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PureWhite,
            contentColor = Emerald800
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
        }

        if (selectedTab == 0) {
            SchemesDirectoryTab(
                schemes = schemes,
                userProfile = userProfile,
                selectedCategory = selectedCategory,
                isHindi = isHindi,
                onCategorySelect = { viewModel.setSchemeCategory(it) },
                onAskAiAboutScheme = { schemeName ->
                    val prompt = if (isHindi) "मुझे $schemeName के लिए आवेदन प्रक्रिया, सब्सिडी और जरूरी दस्तावेजों के बारे में विस्तार से बताएं।"
                    else "Explain the application process, subsidy percentage, and required documents for $schemeName."
                    onNavigateToAdvisorWithPrompt(prompt)
                }
            )
        } else {
            RemindersSchedulerTab(
                viewModel = viewModel,
                reminders = reminders,
                isHindi = isHindi
            )
        }
    }
}

@Composable
fun SchemesDirectoryTab(
    schemes: List<Scheme>,
    userProfile: com.example.data.model.UserProfile,
    selectedCategory: SchemeCategory?,
    isHindi: Boolean,
    onCategorySelect: (SchemeCategory?) -> Unit,
    onAskAiAboutScheme: (String) -> Unit
) {
    var showOnlyMyProfession by remember { mutableStateOf(false) }

    val filteredSchemes = schemes.filter { scheme ->
        val matchesCategory = selectedCategory == null || scheme.category == selectedCategory
        val matchesProfession = !showOnlyMyProfession || scheme.isEligibleFor(userProfile.businessType)
        matchesCategory && matchesProfession
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // Trade Personalization Recommendation Banner & Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Profession toggle chip
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOnlyMyProfession = !showOnlyMyProfession },
                    shape = RoundedCornerShape(10.dp),
                    color = if (showOnlyMyProfession) PrimaryBlue50 else SurfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (showOnlyMyProfession) PrimaryDeepBlue else BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(userProfile.businessType.icon, fontSize = 18.sp)
                            Column {
                                Text(
                                    text = if (isHindi)
                                        "${userProfile.businessType.titleHi} हेतु विशेष योजनाएं"
                                    else
                                        "Recommended for ${userProfile.businessType.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (showOnlyMyProfession) PrimaryDeepBlue else TextPrimary
                                )
                                Text(
                                    text = if (showOnlyMyProfession)
                                        (if (isHindi) "केवल आपके व्यापार की योजनाएं दिख रही हैं" else "Showing schemes matching your exact trade")
                                    else
                                        (if (isHindi) "अपने व्यापार की योजनाएं फिल्टर करने के लिए दबाएं" else "Tap to filter schemes tailored for your enterprise"),
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = showOnlyMyProfession,
                            onCheckedChange = { showOnlyMyProfession = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = PrimaryDeepBlue
                            )
                        )
                    }
                }

                // Category Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onCategorySelect(null) },
                            label = { Text(if (isHindi) "सभी श्रेणियां" else "All Categories", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryDeepBlue, selectedLabelColor = PureWhite)
                        )
                    }
                    items(SchemeCategory.values()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onCategorySelect(cat) },
                            label = { Text(if (isHindi) cat.labelHi else cat.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryDeepBlue, selectedLabelColor = PureWhite)
                        )
                    }
                }
            }
        }

        if (filteredSchemes.isEmpty()) {
            item {
                com.example.ui.components.AppEmptyState(
                    title = if (isHindi) "कोई योजना नहीं मिली" else "No matching schemes found",
                    description = if (isHindi) "फिल्टर बदलकर अन्य योजनाएं देखें।" else "Try clearing filters to explore all available government schemes.",
                    actionButtonText = if (isHindi) "सभी योजनाएं देखें" else "Show All Schemes",
                    onActionClick = {
                        showOnlyMyProfession = false
                        onCategorySelect(null)
                    }
                )
            }
        }

        items(filteredSchemes, key = { it.id }) { scheme ->
            SchemeDetailCard(
                scheme = scheme,
                userProfile = userProfile,
                isHindi = isHindi,
                onAskAi = { onAskAiAboutScheme(scheme.name) }
            )
        }
    }
}

@Composable
fun SchemeDetailCard(
    scheme: Scheme,
    userProfile: com.example.data.model.UserProfile? = null,
    isHindi: Boolean,
    onAskAi: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isTradeEligible = userProfile != null && scheme.isEligibleFor(userProfile.businessType)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isTradeEligible) PrimaryBlue100 else Slate200)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isTradeEligible) {
                Surface(
                    color = PrimaryBlue50,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = if (isHindi) "🎯 आपके व्यवसाय (${userProfile?.businessType?.titleHi ?: ""}) हेतु अनुशंसित" else "🎯 Recommended for your trade (${userProfile?.businessType?.title ?: ""})",
                        color = PrimaryDeepBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHindi) scheme.nameHi else scheme.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Slate900
                    )
                    Text(
                        text = scheme.ministry,
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }

                if (scheme.subsidyPercent > 0) {
                    Surface(
                        color = Amber100,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${scheme.subsidyPercent}% Subsidy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Amber900,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isHindi) scheme.descriptionHi else scheme.description,
                fontSize = 12.sp,
                color = Slate700,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Max Loan: ₹${String.format(Locale.ROOT, "%,.0f", scheme.maxLoanAmount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald800
                )

                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isExpanded) (if (isHindi) "कम देखें ▲" else "Less ▲") else (if (isHindi) "पात्रता व दस्तावेज ▼" else "Eligibility & Docs ▼"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = Slate100)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(if (isHindi) "📋 पात्रता मापदंड:" else "📋 Eligibility Criteria:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate800)
                    Text(if (isHindi) scheme.eligibilityCriteriaHi else scheme.eligibilityCriteria, fontSize = 11.sp, color = Slate600)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(if (isHindi) "📄 आवश्यक दस्तावेज:" else "📄 Documents Required:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate800)
                    Text(scheme.documentsRequired, fontSize = 11.sp, color = Slate600)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAskAi,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                        ) {
                            Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHindi) "AI से आवेदन पूछें" else "Ask AI Advisor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val browserIntent = Intent(Intent.ACTION_VIEW, scheme.applicationUrl.toUri())
                                context.startActivity(browserIntent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHindi) "पोर्टल खोलें" else "Govt Portal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RemindersSchedulerTab(
    viewModel: SahayakViewModel,
    reminders: List<BusinessReminder>,
    isHindi: Boolean
) {
    var showAddReminderDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddReminderDialog = true },
                containerColor = Amber600,
                contentColor = PureWhite,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Reminder")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            item {
                Text(
                    text = if (isHindi) "ऋण EMI, समूह बैठक व योजना रिमाइंडर" else "Loan EMIs, SHG Meetings & Subsidy Deadlines",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate900
                )
            }

            items(reminders, key = { it.id }) { reminder ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (reminder.isCompleted) Slate100 else PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = reminder.isCompleted,
                            onCheckedChange = { viewModel.toggleReminder(reminder) },
                            colors = CheckboxDefaults.colors(checkedColor = Emerald700)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reminder.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (reminder.isCompleted) Slate400 else Slate900
                            )
                            Text(
                                text = "Due: ${reminder.dueDate}${reminder.amount?.let { " • ₹${String.format(Locale.ROOT, "%,.0f", it)}" } ?: ""}",
                                fontSize = 11.sp,
                                color = if (reminder.isCompleted) Slate400 else Amber800,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (reminder.note.isNotBlank()) {
                                Text(
                                    text = reminder.note,
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.deleteReminder(reminder) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = Slate400, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddReminderDialog) {
        var title by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("2026-09-15") }
        var amountText by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddReminderDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = PureWhite
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if (isHindi) "नया रिमाइंडर जोड़ें" else "Add New Reminder", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title", color = Slate800) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate400,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Slate50
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date (YYYY-MM-DD)", color = Slate800) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate400,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Slate50
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹ Optional)", color = Slate800) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate400,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Slate50
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Notes", color = Slate800) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate400,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Slate50
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAddReminderDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    viewModel.addReminder(title, dueDate, ReminderType.SCHEME_DEADLINE, amountText.toDoubleOrNull(), note)
                                    showAddReminderDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
