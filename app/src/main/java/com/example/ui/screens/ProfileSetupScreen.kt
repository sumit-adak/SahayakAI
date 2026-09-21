package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.BusinessType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel

/**
 * Mandatory Enterprise Profile Setup Screen (Part D1)
 * Enforces authentic user data capture before entering the main application.
 */
@Composable
fun ProfileSetupScreen(
    viewModel: SahayakViewModel,
    onComplete: () -> Unit
) {
    val currentProfile by viewModel.userProfile.collectAsState()
    var preferredLanguage by remember { mutableStateOf(currentProfile.preferredLanguage) }
    val isHindi = preferredLanguage == AppLanguage.HINDI

    var name by remember { mutableStateOf(currentProfile.name) }
    var selectedBusinessType by remember { mutableStateOf(currentProfile.businessType) }
    var businessName by remember { mutableStateOf(currentProfile.businessName) }
    var location by remember { mutableStateOf(currentProfile.location) }
    var state by remember { mutableStateOf(if (currentProfile.state.isNotBlank()) currentProfile.state else "Uttar Pradesh") }
    var pincode by remember { mutableStateOf(currentProfile.pincode) }
    var monthlyTurnoverText by remember { mutableStateOf(if (currentProfile.monthlyTurnover > 0) currentProfile.monthlyTurnover.toInt().toString() else "35000") }
    var shgName by remember { mutableStateOf(currentProfile.shgName) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val turnoverPresets = listOf(
        "15000" to "₹15,000",
        "35000" to "₹35,000",
        "60000" to "₹60,000",
        "120000" to "₹1,20,000+"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner
        AppCard(
            backgroundColor = PrimaryDeepBlue,
            elevation = 2.dp,
            border = null
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isHindi) "व्यवसाय प्रोफ़ाइल सेटअप" else "Enterprise Profile Setup",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (isHindi) "सही जानकारी से AI सलाह और योजनाएं व्यक्तिगत होंगी" else "Personalizes AI advice, schemes & market intelligence",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PrimaryBlue100
                        )
                    )
                }
            }
        }

        // Language Selector
        AppCard {
            Text(
                text = if (isHindi) "पसंदीदा भाषा (Preferred Language)" else "Preferred Language",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.values().forEach { lang ->
                    val isSelected = preferredLanguage == lang
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { preferredLanguage = lang }
                            .testTag("lang_chip_${lang.code}"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) PrimaryDeepBlue else SurfaceColor,
                        border = BorderStroke(1.dp, if (isSelected) PrimaryDeepBlue else BorderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = lang.nativeName,
                                color = if (isSelected) PureWhite else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = lang.displayName,
                                color = if (isSelected) PrimaryBlue100 else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Error message if validation fails
        if (errorMessage != null) {
            AppErrorBanner(
                message = errorMessage!!,
                onDismiss = { errorMessage = null }
            )
        }

        // 1. Entrepreneur & Business Details
        AppCard {
            Text(
                text = if (isHindi) "1. व्यक्तिगत व व्यापार विवरण" else "1. Entrepreneur & Business Info",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(14.dp))

            AppTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = if (isHindi) "आपका पूरा नाम *" else "Full Name *",
                placeholder = if (isHindi) "उदा. रमेश कुमार शर्मा" else "e.g. Ramesh Kumar Sharma",
                leadingIcon = Icons.Filled.Person,
                testTag = "setup_input_name"
            )

            Spacer(modifier = Modifier.height(12.dp))

            AppTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = if (isHindi) "दुकान / उद्यम का नाम *" else "Enterprise / Business Name *",
                placeholder = if (isHindi) "उदा. शर्मा किराना एवं जनरल स्टोर" else "e.g. Sharma Kirana Store",
                leadingIcon = Icons.Filled.Store,
                testTag = "setup_input_business_name"
            )
        }

        // 2. Profession / Trade Selection (Part D1)
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "2. व्यवसाय का प्रकार चुनें *" else "2. Select Profession / Trade *",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    color = PrimaryBlue50,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isHindi) "AI ट्यूनिंग" else "AI Tuning",
                        color = PrimaryDeepBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = if (isHindi) "योजनाएं और मंडी भाव आपके व्यापार अनुसार दिखाए जाएंगे" else "Schemes, subsidies, and advisory will adapt to this trade",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BusinessType.values().toList().chunked(2).forEach { rowTypes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTypes.forEach { type ->
                            val isSelected = selectedBusinessType == type
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedBusinessType = type
                                        if (businessName.isBlank() && name.isNotBlank()) {
                                            businessName = "$name ${type.title.substringBefore("/")}"
                                        }
                                    }
                                    .testTag("business_type_${type.name}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) PrimaryBlue50 else SurfaceColor,
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) PrimaryDeepBlue else BorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(type.icon, fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = if (isHindi) type.titleHi else type.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) PrimaryDeepBlue else TextPrimary,
                                                fontSize = 12.sp
                                            ),
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Location & Scale
        AppCard {
            Text(
                text = if (isHindi) "3. स्थान एवं मासिक कारोबार" else "3. Location & Scale",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(14.dp))

            AppTextField(
                value = location,
                onValueChange = { location = it; errorMessage = null },
                label = if (isHindi) "गाँव / कस्बा / जिला *" else "Village / Town / District *",
                placeholder = if (isHindi) "उदा. वाराणसी ग्रामीण" else "e.g. Varanasi Rural",
                leadingIcon = Icons.Filled.LocationOn,
                testTag = "setup_input_location"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = if (isHindi) "राज्य" else "State",
                    modifier = Modifier.weight(1.2f),
                    testTag = "setup_input_state"
                )

                AppTextField(
                    value = pincode,
                    onValueChange = { pincode = it },
                    label = if (isHindi) "पिन कोड" else "PIN Code",
                    placeholder = "221001",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f),
                    testTag = "setup_input_pincode"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isHindi) "अनुमानित मासिक बिक्री / टर्नओवर (₹)" else "Estimated Monthly Sales / Turnover (₹)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = TextPrimary)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                turnoverPresets.forEach { (amountVal, labelStr) ->
                    val isSelected = monthlyTurnoverText == amountVal
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { monthlyTurnoverText = amountVal },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PrimaryDeepBlue else PrimaryBlue50,
                        border = BorderStroke(1.dp, if (isSelected) PrimaryDeepBlue else PrimaryBlue100)
                    ) {
                        Text(
                            text = labelStr,
                            color = if (isSelected) PureWhite else PrimaryDeepBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AppTextField(
                value = shgName,
                onValueChange = { shgName = it },
                label = if (isHindi) "स्वयं सहायता समूह (SHG) / ग्राम समिति (वैकल्पिक)" else "SHG / Group Name (Optional)",
                placeholder = if (isHindi) "उदा. श्री राधा महिला SHG" else "e.g. Radhe Mahila SHG",
                leadingIcon = Icons.Filled.Group,
                testTag = "setup_input_shg"
            )
        }

        // Action CTA
        AppButton(
            text = if (isHindi) "प्रोफ़ाइल सहेजें और शुरू करें" else "Save Profile & Enter SahayakAI",
            onClick = {
                if (name.isBlank()) {
                    errorMessage = if (isHindi) "कृपया अपना नाम दर्ज करें" else "Please enter your name"
                    return@AppButton
                }
                if (location.isBlank()) {
                    errorMessage = if (isHindi) "कृपया अपना गाँव या शहर दर्ज करें" else "Please enter your location"
                    return@AppButton
                }

                isSubmitting = true
                val turnover = monthlyTurnoverText.toDoubleOrNull() ?: 35000.0
                val bName = if (businessName.isNotBlank()) businessName else "$name ${selectedBusinessType.title}"

                viewModel.completeProfileSetup(
                    name = name,
                    businessType = selectedBusinessType,
                    businessName = bName,
                    location = location,
                    state = state,
                    pincode = pincode,
                    monthlyTurnover = turnover,
                    preferredLanguage = preferredLanguage,
                    shgName = shgName
                )
                viewModel.setLanguage(preferredLanguage)
                onComplete()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_profile_button"),
            isLoading = isSubmitting,
            icon = Icons.Filled.CheckCircle,
            variant = AppButtonVariant.PRIMARY
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
