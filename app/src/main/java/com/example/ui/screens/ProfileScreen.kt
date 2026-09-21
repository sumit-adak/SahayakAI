package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val AVATAR_EMOJIS = listOf("👨🏽‍💼", "👩🏽‍🌾", "👨🏽‍🍳", "👩🏽‍🏫", "👨🏽‍🏭", "👩🏽‍🎨")
private val AVATAR_COLORS = listOf(Emerald700, Saffron600, SkyBlue, PurpleAccent, Amber600, Emerald900)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: SahayakViewModel,
    onNavigateBack: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isHindi = userProfile.preferredLanguage == AppLanguage.HINDI
    val scope = rememberCoroutineScope()

    // Form fields state initialized from current userProfile
    var name by remember(userProfile.name) { mutableStateOf(userProfile.name) }
    var businessName by remember(userProfile.businessName) { mutableStateOf(userProfile.businessName) }
    var phone by remember(userProfile.phone) { mutableStateOf(userProfile.phone) }
    var email by remember(userProfile.email) { mutableStateOf(userProfile.email) }
    var whatsappPhone by remember(userProfile.whatsappPhone) { mutableStateOf(userProfile.whatsappPhone) }
    var location by remember(userProfile.location) { mutableStateOf(userProfile.location) }
    var state by remember(userProfile.state) { mutableStateOf(userProfile.state) }
    var pincode by remember(userProfile.pincode) { mutableStateOf(userProfile.pincode) }
    var selectedBusinessType by remember(userProfile.businessType) { mutableStateOf(userProfile.businessType) }
    var turnoverStr by remember(userProfile.monthlyTurnover) { mutableStateOf(userProfile.monthlyTurnover.toInt().toString()) }
    var dailyCustomersStr by remember(userProfile.dailyCustomers) { mutableStateOf(userProfile.dailyCustomers.toString()) }
    var shgName by remember(userProfile.shgName) { mutableStateOf(userProfile.shgName) }
    var selectedAvatarIndex by remember(userProfile.avatarIndex) { mutableIntStateOf(userProfile.avatarIndex) }

    var isEditMode by remember { mutableStateOf(false) }
    var showSaveSnackbar by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    val businessTypes: List<Pair<BusinessType, String>> = listOf(
        BusinessType.KIRANA to if (isHindi) "किराना / जनरल स्टोर" else "Kirana / General Store",
        BusinessType.AGRICULTURE to if (isHindi) "कृषि व उपज" else "Agriculture & Crops",
        BusinessType.DAIRY_FARMING to if (isHindi) "डेयरी व पशुपालन" else "Dairy & Livestock",
        BusinessType.STREET_VENDOR to if (isHindi) "फेरीवाला / ठेला" else "Street Vendor / Thela",
        BusinessType.TAILORING to if (isHindi) "सिलाई व परिधान" else "Tailoring & Garments",
        BusinessType.HANDICRAFTS to if (isHindi) "हस्तशिल्प व कुटीर उद्योग" else "Handicrafts & Cottage",
        BusinessType.FOOD_STALL to if (isHindi) "चाय दुकान व नाश्ता" else "Tea Shop / Tea Stall",
        BusinessType.OTHER to if (isHindi) "अन्य सूक्ष्म व्यवसाय" else "Other Micro Enterprise"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "मेरी प्रोफाइल व व्यवसाय" else "My Profile & Enterprise",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate900
                        )
                        Text(
                            text = if (isHindi) "उद्यमी खाता व सेटिंग्स" else "Merchant Account & Settings",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("profile_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate900
                        )
                    }
                },
                actions = {
                    if (!isEditMode) {
                        FilledTonalButton(
                            onClick = { isEditMode = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Emerald100,
                                contentColor = Emerald800
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("profile_edit_toggle_btn")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "संपादित करें" else "Edit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.updateFullProfile(
                                    name = name,
                                    businessName = businessName,
                                    phone = phone,
                                    email = email,
                                    whatsappPhone = whatsappPhone,
                                    businessType = selectedBusinessType,
                                    location = location,
                                    state = state,
                                    pincode = pincode,
                                    monthlyTurnover = turnoverStr.toDoubleOrNull() ?: userProfile.monthlyTurnover,
                                    dailyCustomers = dailyCustomersStr.toIntOrNull() ?: userProfile.dailyCustomers,
                                    shgName = shgName,
                                    avatarIndex = selectedAvatarIndex
                                )
                                isEditMode = false
                                showSaveSnackbar = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald800,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("profile_save_btn")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "सुरक्षित करें" else "Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
            )
        },
        containerColor = Slate50
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success Feedback banner
            AnimatedVisibility(
                visible = showSaveSnackbar,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = Emerald100,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Emerald800)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "आपकी प्रोफ़ाइल सफलतापूर्वक अपडेट हो गई है!" else "Your profile has been successfully updated!",
                            color = Emerald800,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showSaveSnackbar = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Emerald800, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                LaunchedEffect(showSaveSnackbar) {
                    delay(3500)
                    showSaveSnackbar = false
                }
            }

            // --- HERO CARD: Profile Header with Avatar & Verification Badge ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar display with color accent
                    val avatarBg = AVATAR_COLORS.getOrElse(selectedAvatarIndex) { Emerald700 }
                    val avatarEmoji = AVATAR_EMOJIS.getOrElse(selectedAvatarIndex) { "👨🏽‍💼" }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(avatarBg)
                            .border(3.dp, PureWhite, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = avatarEmoji, fontSize = 40.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = name.ifEmpty { "उद्यमी (Merchant)" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Slate900
                    )

                    Text(
                        text = businessName.ifEmpty { "ग्रामीण सूक्ष्म उद्यम" },
                        fontSize = 14.sp,
                        color = Slate700,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Verification Badges Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Emerald50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald700)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = Emerald700, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHindi) "सत्यापित व्यवसायी • Sahayak Verified" else "Sahayak Verified Merchant",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald800
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Saffron100
                        ) {
                            Text(
                                text = "UDYAM Registered",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Saffron700,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Avatar Picker when editing
                    if (isEditMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isHindi) "अवतार चुनें (Choose Avatar):" else "Choose Avatar Profile:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(AVATAR_EMOJIS) { idx, emoji ->
                                val isSelected = selectedAvatarIndex == idx
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(AVATAR_COLORS[idx])
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Emerald800 else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedAvatarIndex = idx },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 1: PERSONAL & CONTACT DETAILS ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_personal_details_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Emerald800, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "व्यक्तिगत व संपर्क विवरण" else "Personal & Contact Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate900
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Full Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isHindi) "पूरा नाम (Full Name) *" else "Full Name *", color = Slate800) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                        enabled = isEditMode,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            disabledTextColor = Slate900,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate400,
                            disabledBorderColor = Slate200,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Slate50,
                            disabledContainerColor = Slate50
                        ),
                        leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Slate600) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mobile Number Field
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isHindi) "प्राथमिक मोबाइल नंबर *" else "Primary Mobile Number *", color = Slate800) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_phone_input"),
                        enabled = isEditMode,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            disabledTextColor = Slate900,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate400,
                            disabledBorderColor = Slate200,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Slate50,
                            disabledContainerColor = Slate50
                        ),
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = Slate600) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(if (isHindi) "ईमेल पता (Email Address)" else "Email Address", color = Slate800) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_email_input"),
                        enabled = isEditMode,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            disabledTextColor = Slate900,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate400,
                            disabledBorderColor = Slate200,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Slate50,
                            disabledContainerColor = Slate50
                        ),
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = Slate600) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // WhatsApp Phone Field
                    OutlinedTextField(
                        value = whatsappPhone,
                        onValueChange = { whatsappPhone = it },
                        label = { Text(if (isHindi) "व्हाट्सएप नंबर (तकादा संदेश हेतु)" else "WhatsApp Number (for automated reminders)", color = Slate800) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_whatsapp_input"),
                        enabled = isEditMode,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            disabledTextColor = Slate900,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate400,
                            disabledBorderColor = Slate200,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Slate50,
                            disabledContainerColor = Slate50
                        ),
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Emerald700) }
                    )
                }
            }

            // --- SECTION 2: BUSINESS & ENTERPRISE DETAILS ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_business_details_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Store, contentDescription = null, tint = Saffron700, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "दुकान व उद्यम विवरण" else "Business & Enterprise Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate900
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Shop / Business Name
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text(if (isHindi) "दुकान / उद्यम का नाम *" else "Shop / Enterprise Name *", color = Slate800) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_business_name_input"),
                        enabled = isEditMode,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            disabledTextColor = Slate900,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate400,
                            disabledBorderColor = Slate200,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Slate50,
                            disabledContainerColor = Slate50
                        ),
                        leadingIcon = { Icon(Icons.Filled.Storefront, contentDescription = null, tint = Slate600) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Business Category Chooser
                    Text(
                        text = if (isHindi) "व्यापार श्रेणी (Business Category):" else "Business Category:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (isEditMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            businessTypes.chunked(2).forEach { rowPairs ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPairs.forEach { (type, label) ->
                                        val isSelected = selectedBusinessType == type
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedBusinessType = type },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Emerald50 else Slate50,
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Emerald700 else Slate300
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { selectedBusinessType = type },
                                                    colors = RadioButtonDefaults.colors(selectedColor = Emerald800)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Emerald900 else Slate800
                                                )
                                            }
                                        }
                                    }
                                    if (rowPairs.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Emerald50,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Category, contentDescription = null, tint = Emerald800)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = businessTypes.firstOrNull { it.first == selectedBusinessType }?.second ?: selectedBusinessType.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Emerald900
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Location / Village
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text(if (isHindi) "गांव / कस्बा / शहर *" else "Village / Town / City *", color = Slate800) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_location_input"),
                        enabled = isEditMode,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            disabledTextColor = Slate900,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate400,
                            disabledBorderColor = Slate200,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Slate50,
                            disabledContainerColor = Slate50
                        ),
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Slate600) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // State & Pincode Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text(if (isHindi) "राज्य (State)" else "State", color = Slate800) },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("profile_state_input"),
                            enabled = isEditMode,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                disabledTextColor = Slate900,
                                focusedBorderColor = Emerald700,
                                unfocusedBorderColor = Slate400,
                                disabledBorderColor = Slate200,
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = Slate50,
                                disabledContainerColor = Slate50
                            )
                        )

                        OutlinedTextField(
                            value = pincode,
                            onValueChange = { pincode = it },
                            label = { Text(if (isHindi) "पिनकोड" else "Pincode", color = Slate800) },
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("profile_pincode_input"),
                            enabled = isEditMode,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                disabledTextColor = Slate900,
                                focusedBorderColor = Emerald700,
                                unfocusedBorderColor = Slate400,
                                disabledBorderColor = Slate200,
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = Slate50,
                                disabledContainerColor = Slate50
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SHG / Gram Samiti Name
                    OutlinedTextField(
                        value = shgName,
                        onValueChange = { shgName = it },
                        label = { Text(if (isHindi) "स्वयं सहायता समूह / ग्राम समिति (SHG Name)" else "Self Help Group (SHG) / Samiti", color = Slate800) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_shg_input"),
                        enabled = isEditMode,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            disabledTextColor = Slate900,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate400,
                            disabledBorderColor = Slate200,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Slate50,
                            disabledContainerColor = Slate50
                        ),
                        leadingIcon = { Icon(Icons.Filled.Groups, contentDescription = null, tint = Slate600) }
                    )
                }
            }

            // --- SECTION 3: FINANCIAL CAPACITY & METRICS ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_financial_metrics_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = Emerald800, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "वित्तीय आँकड़े व ऋण क्षमता" else "Financial Scale & Credit Readiness",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate900
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = turnoverStr,
                            onValueChange = { turnoverStr = it },
                            label = { Text(if (isHindi) "मासिक टर्नओवर (₹)" else "Monthly Turnover (₹)", color = Slate800) },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("profile_turnover_input"),
                            enabled = isEditMode,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                disabledTextColor = Slate900,
                                focusedBorderColor = Emerald700,
                                unfocusedBorderColor = Slate400,
                                disabledBorderColor = Slate200,
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = Slate50,
                                disabledContainerColor = Slate50
                            ),
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = Slate700) }
                        )

                        OutlinedTextField(
                            value = dailyCustomersStr,
                            onValueChange = { dailyCustomersStr = it },
                            label = { Text(if (isHindi) "दैनिक ग्राहक" else "Daily Footfall", color = Slate800) },
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("profile_customers_input"),
                            enabled = isEditMode,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                disabledTextColor = Slate900,
                                focusedBorderColor = Emerald700,
                                unfocusedBorderColor = Slate400,
                                disabledBorderColor = Slate200,
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = Slate50,
                                disabledContainerColor = Slate50
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mudra & Credit Ready Highlights
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Speed, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "अनुमानित साख स्कोर (Estimated Credit Health): 780 / 900" else "Estimated Financial Health: 780 / 900 (Excellent)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Slate900
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHindi) "आप प्रधानमंत्री मुद्रा योजना (किशोर लोन - ₹5 लाख तक) के लिए पात्र हैं।" else "Eligible for PM Mudra Kishor Category (Up to ₹5 Lakh Zero Collateral).",
                                fontSize = 11.sp,
                                color = Slate700
                            )
                        }
                    }
                }
            }

            // --- SECTION 4: APP LANGUAGE & PREFERENCES ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_preferences_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Translate, contentDescription = null, tint = Emerald800, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "ऐप भाषा व प्राथमिकताएं" else "Language & App Preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate900
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languages = listOf(
                            AppLanguage.HINDI to "हिन्दी (Hindi)",
                            AppLanguage.ENGLISH to "English",
                            AppLanguage.HINGLISH to "Hinglish"
                        )

                        languages.forEach { (lang, label) ->
                            val isSelected = userProfile.preferredLanguage == lang
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setLanguage(lang) },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Emerald900 else Slate800
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald100,
                                    selectedLabelColor = Emerald900
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // --- SECTION 5: ACCOUNT ACTIONS & LOGOUT ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_actions_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (isEditMode) {
                        Button(
                            onClick = {
                                viewModel.updateFullProfile(
                                    name = name,
                                    businessName = businessName,
                                    phone = phone,
                                    email = email,
                                    whatsappPhone = whatsappPhone,
                                    businessType = selectedBusinessType,
                                    location = location,
                                    state = state,
                                    pincode = pincode,
                                    monthlyTurnover = turnoverStr.toDoubleOrNull() ?: userProfile.monthlyTurnover,
                                    dailyCustomers = dailyCustomersStr.toIntOrNull() ?: userProfile.dailyCustomers,
                                    shgName = shgName,
                                    avatarIndex = selectedAvatarIndex
                                )
                                isEditMode = false
                                showSaveSnackbar = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_all_profile_changes_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald800, contentColor = PureWhite)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHindi) "सभी विवरण सुरक्षित करें • Save Profile" else "Save All Profile Changes", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedButton(
                        onClick = { showLogoutConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_logout_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = UdhaarRed),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, UdhaarRed)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = UdhaarRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "खाता से लॉगआउट करें (Logout Account)" else "Sign Out / Logout Account",
                            fontWeight = FontWeight.Bold,
                            color = UdhaarRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text(
                    text = if (isHindi) "लॉगआउट की पुष्टि करें" else "Confirm Sign Out",
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            },
            text = {
                Text(
                    text = if (isHindi) "क्या आप निश्चित रूप से SahayakAI खाते से लॉगआउट करना चाहते हैं? आपका स्थानीय डेटा सुरक्षित रहेगा।" else "Are you sure you want to sign out from SahayakAI? Your local ledger data will remain safely stored.",
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UdhaarRed, contentColor = PureWhite)
                ) {
                    Text(if (isHindi) "हाँ, लॉगआउट करें" else "Yes, Sign Out")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel", color = Slate800)
                }
            }
        )
    }
}
