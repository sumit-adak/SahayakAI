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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.BusinessType
import com.example.data.model.UserProfile
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel
import kotlinx.coroutines.delay

enum class AuthTab {
    LOGIN,
    REGISTER
}

enum class LoginMethod {
    MOBILE_OTP,
    PASSWORD
}

@Composable
fun AuthScreen(
    viewModel: SahayakViewModel,
    onLoginSuccess: () -> Unit
) {
    var activeTab by remember { mutableStateOf(AuthTab.LOGIN) }
    var loginMethod by remember { mutableStateOf(LoginMethod.MOBILE_OTP) }

    // LOGIN STATE
    var loginPhone by remember { mutableStateOf("9876543210") }
    var loginPassword by remember { mutableStateOf("sahayak123") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isOtpStep by remember { mutableStateOf(false) }
    var otpDigits by remember { mutableStateOf(listOf("7", "4", "9", "2", "0", "1")) }
    var resendCountdown by remember { mutableIntStateOf(30) }
    var isVerifying by remember { mutableStateOf(false) }

    // SIGN UP / REGISTRATION STATE
    var regName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regLocation by remember { mutableStateOf("Varanasi, UP") }
    var regBusinessType by remember { mutableStateOf(BusinessType.KIRANA) }
    var isRegAgreed by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Resend timer countdown effect
    LaunchedEffect(isOtpStep) {
        if (isOtpStep) {
            resendCountdown = 30
            while (resendCountdown > 0) {
                delay(1000)
                resendCountdown--
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // App Brand Emblem
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PrimaryDeepBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🇮🇳",
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "सहायक एआई • SahayakAI",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Ministry of Social Justice & Empowerment • SIH 2026",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryDeepBlue
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = "ग्रामीण व सूक्ष्म उद्यमियों का डिजिटल खाता और वित्तीय साथी",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error / Success Feedback
            errorMessage?.let { error ->
                AppErrorBanner(
                    message = error,
                    onDismiss = { errorMessage = null },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            successMessage?.let { msg ->
                Surface(
                    color = SuccessGreenBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, color = SuccessGreen, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { successMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Tabs: Login vs Register
            Surface(
                color = Slate200,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Surface(
                        color = if (activeTab == AuthTab.LOGIN) PrimaryDeepBlue else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                activeTab = AuthTab.LOGIN
                                errorMessage = null
                            }
                            .testTag("auth_tab_login")
                    ) {
                        Text(
                            text = "लॉगिन / Sign In",
                            color = if (activeTab == AuthTab.LOGIN) PureWhite else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    Surface(
                        color = if (activeTab == AuthTab.REGISTER) PrimaryDeepBlue else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                activeTab = AuthTab.REGISTER
                                errorMessage = null
                            }
                            .testTag("auth_tab_register")
                    ) {
                        Text(
                            text = "नया खाता / Register",
                            color = if (activeTab == AuthTab.REGISTER) PureWhite else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TAB CONTENT: LOGIN
            if (activeTab == AuthTab.LOGIN) {
                AppCard {
                    // Switch between OTP vs Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (loginMethod == LoginMethod.MOBILE_OTP) "मोबाइल OTP लॉगिन" else "पासवर्ड लॉगिन",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        TextButton(
                            onClick = {
                                loginMethod = if (loginMethod == LoginMethod.MOBILE_OTP) LoginMethod.PASSWORD else LoginMethod.MOBILE_OTP
                                errorMessage = null
                                isOtpStep = false
                            },
                            modifier = Modifier.testTag("switch_login_method")
                        ) {
                            Text(
                                text = if (loginMethod == LoginMethod.MOBILE_OTP) "Use Password" else "Use OTP",
                                color = PrimaryDeepBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (loginMethod == LoginMethod.MOBILE_OTP) {
                        if (!isOtpStep) {
                            AppTextField(
                                value = loginPhone,
                                onValueChange = { if (it.length <= 10) loginPhone = it.filter { ch -> ch.isDigit() } },
                                label = "मोबाइल नंबर (10 Digit Mobile Number)",
                                placeholder = "9876543210",
                                leadingIcon = Icons.Filled.Phone,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                testTag = "auth_login_phone"
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            AppButton(
                                text = "OTP प्राप्त करें / Send OTP",
                                onClick = {
                                    if (loginPhone.length < 10) {
                                        errorMessage = "कृपया 10 अंकों का वैध मोबाइल नंबर दर्ज करें"
                                    } else {
                                        errorMessage = null
                                        isOtpStep = true
                                        successMessage = "OTP भेज दिया गया है: 749201"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_send_otp_button"),
                                icon = Icons.AutoMirrored.Filled.Send,
                                variant = AppButtonVariant.PRIMARY
                            )
                        } else {
                            // OTP Verification view
                            Text(
                                text = "+91 $loginPhone पर भेजा गया 6-अंकीय OTP दर्ज करें:",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // 6-digit OTP Inputs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                otpDigits.forEachIndexed { index, digit ->
                                    Surface(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .border(
                                                width = 1.5.dp,
                                                color = if (digit.isNotEmpty()) PrimaryDeepBlue else BorderColor,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        color = SurfaceColor,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = digit,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { isOtpStep = false },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Change Number", fontSize = 12.sp, color = PrimaryDeepBlue)
                                }

                                Text(
                                    text = if (resendCountdown > 0) "Resend OTP in ${resendCountdown}s" else "Resend OTP",
                                    fontSize = 12.sp,
                                    color = if (resendCountdown > 0) TextSecondary else PrimaryDeepBlue,
                                    fontWeight = if (resendCountdown > 0) FontWeight.Normal else FontWeight.Bold,
                                    modifier = Modifier.clickable(enabled = resendCountdown == 0) {
                                        resendCountdown = 30
                                        successMessage = "New OTP Sent: 749201"
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            AppButton(
                                text = "OTP सत्यापित करें / Verify & Enter",
                                onClick = {
                                    val enteredOtp = otpDigits.joinToString("")
                                    if (enteredOtp == "749201" || enteredOtp.length == 6) {
                                        isVerifying = true
                                        // Login with verified phone
                                        viewModel.loginWithPhone(loginPhone, isRegistered = true)
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = "अमान्य OTP! कृपया '749201' दर्ज करें"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_verify_otp_button"),
                                isLoading = isVerifying,
                                icon = Icons.Filled.VerifiedUser,
                                variant = AppButtonVariant.PRIMARY
                            )
                        }
                    } else {
                        // Password Login Form
                        AppTextField(
                            value = loginPhone,
                            onValueChange = { loginPhone = it },
                            label = "मोबाइल नंबर या ईमेल (Mobile or Email)",
                            placeholder = "9876543210",
                            leadingIcon = Icons.Filled.Person,
                            testTag = "auth_login_user"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AppTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = "पासवर्ड (Password)",
                            leadingIcon = Icons.Filled.Lock,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            testTag = "auth_login_pass"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        AppButton(
                            text = "लॉगिन करें / Sign In",
                            onClick = {
                                if (loginPhone.isBlank() || loginPassword.isBlank()) {
                                    errorMessage = "कृपया मोबाइल और पासवर्ड दर्ज करें"
                                } else {
                                    viewModel.loginWithPhone(loginPhone, isRegistered = true)
                                    onLoginSuccess()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_login_submit"),
                            icon = Icons.AutoMirrored.Filled.Login,
                            variant = AppButtonVariant.PRIMARY
                        )
                    }
                }
            } else {
                // TAB CONTENT: REGISTER
                AppCard {
                    Text(
                        text = "नया उद्यमी पंजीकरण (New Registration)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    AppTextField(
                        value = regName,
                        onValueChange = { regName = it; errorMessage = null },
                        label = "पूरा नाम (Full Name) *",
                        placeholder = "उदा. रमेश शर्मा",
                        leadingIcon = Icons.Filled.Person,
                        testTag = "auth_reg_name"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AppTextField(
                        value = regPhone,
                        onValueChange = { if (it.length <= 10) regPhone = it.filter { ch -> ch.isDigit() } },
                        label = "मोबाइल नंबर (Mobile Number) *",
                        placeholder = "9876543210",
                        leadingIcon = Icons.Filled.Phone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        testTag = "auth_reg_phone"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AppTextField(
                        value = regLocation,
                        onValueChange = { regLocation = it },
                        label = "गाँव / कस्बा / जिला (Location) *",
                        placeholder = "उदा. वाराणसी, उत्तर प्रदेश",
                        leadingIcon = Icons.Filled.LocationOn,
                        testTag = "auth_reg_location"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRegAgreed = !isRegAgreed }
                    ) {
                        Checkbox(
                            checked = isRegAgreed,
                            onCheckedChange = { isRegAgreed = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryDeepBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "मैं डिजिटल भारत और आजीविका डेटा सुरक्षा शर्तों से सहमत हूँ।",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AppButton(
                        text = "खाता बनाएं / Create Account",
                        onClick = {
                            if (regName.isBlank()) {
                                errorMessage = "कृपया अपना नाम दर्ज करें"
                            } else if (regPhone.length < 10) {
                                errorMessage = "कृपया 10 अंकों का मोबाइल नंबर दर्ज करें"
                            } else if (!isRegAgreed) {
                                errorMessage = "कृपया नियम व शर्तों को स्वीकार करें"
                            } else {
                                // Start new user profile setup
                                viewModel.loginWithPhone(regPhone, isRegistered = false)
                                onLoginSuccess()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_register_submit"),
                        icon = Icons.Filled.PersonAdd,
                        variant = AppButtonVariant.PRIMARY
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Demo Accounts Section with Explicit Demo Badge (Part C1 & C2)
            AppCard(
                backgroundColor = SurfaceColor,
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ त्वरित टेस्ट प्रोफाइल्स (Quick Evaluation)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    DemoDataBadge("Demo Accounts")
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "मूल्यांकन हेतु किसी भी प्री-कॉन्फिगर्ड प्रोफाइल से तुरंत लॉगिन करें:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Demo Profile 1: Ramesh (Kirana)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val rameshProfile = UserProfile(
                                id = "usr_demo_01",
                                name = "Ramesh Kumar Sharma",
                                phone = "+91 98765 43210",
                                email = "ramesh.sharma@sahayak.in",
                                businessName = "Sharma General & Kirana Store",
                                businessType = BusinessType.KIRANA,
                                location = "Varanasi Rural, Uttar Pradesh",
                                state = "Uttar Pradesh",
                                pincode = "221001",
                                monthlyTurnover = 45000.0,
                                dailyCustomers = 85,
                                preferredLanguage = AppLanguage.HINDI,
                                isKycMockLinked = true,
                                shgName = "Shri Radha Mahila SHG / Gram Samiti",
                                isLoggedIn = true,
                                profileComplete = true,
                                onboardingCompletedAt = System.currentTimeMillis()
                            )
                            viewModel.loginWithPhone(rameshProfile.phone, isRegistered = true, profile = rameshProfile)
                            onLoginSuccess()
                        }
                        .testTag("demo_login_ramesh"),
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryBlue50,
                    border = BorderStroke(1.dp, PrimaryBlue100)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛒", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("रमेश शर्मा • किराना एवं जनरल स्टोर", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryDeepBlue)
                            Text("Varanasi, UP • ₹45k Turnover • Profile Complete", fontSize = 11.sp, color = TextSecondary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryDeepBlue, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Demo Profile 2: Sunita Devi (Tailoring & SHG)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val sunitaProfile = UserProfile(
                                id = "usr_demo_02",
                                name = "Sunita Devi",
                                phone = "+91 94150 78901",
                                email = "sunita.devi@sahayak.in",
                                businessName = "Sunita Mahila Silai Kendra",
                                businessType = BusinessType.TAILORING,
                                location = "Mirzapur Rural, Uttar Pradesh",
                                state = "Uttar Pradesh",
                                pincode = "231001",
                                monthlyTurnover = 28000.0,
                                dailyCustomers = 20,
                                preferredLanguage = AppLanguage.HINDI,
                                isKycMockLinked = true,
                                shgName = "Maa Durga Mahila SHG",
                                isLoggedIn = true,
                                profileComplete = true,
                                onboardingCompletedAt = System.currentTimeMillis()
                            )
                            viewModel.loginWithPhone(sunitaProfile.phone, isRegistered = true, profile = sunitaProfile)
                            onLoginSuccess()
                        }
                        .testTag("demo_login_sunita"),
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryBlue50,
                    border = BorderStroke(1.dp, PrimaryBlue100)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🧵", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("सुनीता देवी • सिलाई व स्वयं सहायता समूह", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryDeepBlue)
                            Text("Mirzapur, UP • ₹28k Turnover • SHG Member", fontSize = 11.sp, color = TextSecondary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryDeepBlue, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Demo Profile 3: Manoj Kumar (Tea Shop / Tea Stall)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val manojProfile = UserProfile(
                                id = "usr_demo_03",
                                name = "Manoj Kumar",
                                phone = "+91 97920 12345",
                                email = "manoj.chai@sahayak.in",
                                businessName = "Manoj Chai & Snacks Corner",
                                businessType = BusinessType.FOOD_STALL,
                                location = "Varanasi Cantt, Uttar Pradesh",
                                state = "Uttar Pradesh",
                                pincode = "221002",
                                monthlyTurnover = 32000.0,
                                dailyCustomers = 140,
                                preferredLanguage = AppLanguage.HINDI,
                                isKycMockLinked = true,
                                shgName = "Kashi Nagari Vyapar Mandal",
                                isLoggedIn = true,
                                profileComplete = true,
                                onboardingCompletedAt = System.currentTimeMillis()
                            )
                            viewModel.loginWithPhone(manojProfile.phone, isRegistered = true, profile = manojProfile)
                            onLoginSuccess()
                        }
                        .testTag("demo_login_manoj"),
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryBlue50,
                    border = BorderStroke(1.dp, PrimaryBlue100)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("☕", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("मनोज कुमार • चाय दुकान व नाश्ता", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryDeepBlue)
                            Text("Varanasi, UP • ₹32k Turnover • Tea Stall", fontSize = 11.sp, color = TextSecondary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryDeepBlue, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Demo Profile 4: Fresh Blank Account (To test genuine cold start)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val blankProfile = UserProfile(
                                id = "usr_new_${System.currentTimeMillis() % 10000}",
                                name = "",
                                phone = "+91 99999 88888",
                                isLoggedIn = true,
                                profileComplete = false
                            )
                            viewModel.loginWithPhone(blankProfile.phone, isRegistered = false, profile = blankProfile)
                            onLoginSuccess()
                        }
                        .testTag("demo_login_blank"),
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceColor,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("नया ब्लैंक खाता (Fresh Blank Onboarding)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text("Tests mandatory profile setup & empty ledger state", fontSize = 11.sp, color = TextSecondary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
