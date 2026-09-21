package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.core.content.ContextCompat
import com.example.data.model.AppLanguage
import com.example.data.model.ChatMessage
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel
import kotlinx.coroutines.launch

@Composable
fun AdvisorScreen(
    viewModel: SahayakViewModel
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val isSpeaking by viewModel.isTtsSpeaking.collectAsState()
    val isHindi = userProfile.preferredLanguage == AppLanguage.HINDI

    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Voice to Text Speech Recognizer Result Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
                viewModel.sendChatMessage(spokenText, autoSpeak = true)
                inputText = ""
            }
        }
    }

    // Microphone Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchVoiceRecognition(
                context = context,
                isHindi = isHindi,
                launcher = speechLauncher,
                onListening = { isListening = it },
                onFallback = { showVoiceDialog = true }
            )
        } else {
            Toast.makeText(
                context,
                if (isHindi) "वॉइस इनपुट के लिए माइक अनुमति आवश्यक है" else "Microphone permission required for voice input",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val promptSuggestions = remember(isHindi) {
        if (isHindi) {
            listOf(
                "👤 मेरा नाम व व्यापार विवरण बताएं",
                "📈 मैं अपना बिज़नेस कैसे बढ़ाऊं और मुनाफा लाऊं?",
                "🏦 पीएम मुद्रा शिशु लोन ₹50,000 कैसे मिलेगा?",
                "📋 ग्राहक से उधार वसूली तेजी से कैसे करें?",
                "🌾 मेरे व्यापार के लिए सरकारी सब्सिडी योजनाएं बताएं"
            )
        } else {
            listOf(
                "👤 Tell me my name & business details",
                "📈 How can I grow my business and boost profit?",
                "🏦 How to apply for PM Mudra loan (₹50,000)?",
                "📋 Best polite strategies to recover pending customer udhaar?",
                "🌾 Government subsidy schemes for my enterprise"
            )
        }
    }

    LaunchedEffect(chatMessages.size, isAiThinking) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        // 1. Context Injection Banner (Demonstrating personal AI pipeline & Language switcher)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Emerald50,
            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Emerald800, modifier = Modifier.size(16.dp))
                    Column {
                        Text(
                            text = "👤 ${userProfile.name.ifBlank { "Ramesh Kumar Sharma" }} • ${userProfile.businessName.ifBlank { "Sharma General Store" }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald900
                        )
                        Text(
                            text = "${if (isHindi) userProfile.businessType.titleHi else userProfile.businessType.title} • ${userProfile.location.substringBefore(",").ifBlank { "Varanasi" }} • ₹${userProfile.monthlyTurnover.toInt()}/माह",
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick Language Switcher Pill
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val newLang = if (isHindi) AppLanguage.ENGLISH else AppLanguage.HINDI
                                viewModel.setLanguage(newLang)
                            },
                        color = Amber100,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Amber300)
                    ) {
                        Text(
                            text = if (isHindi) "🌐 हिन्दी" else "🌐 English",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Amber900,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = Emerald100,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald300)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Emerald600)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gemini 2.5",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald900
                            )
                        }
                    }
                }
            }
        }

        // 2. Chat Messages Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Welcome Assistant Message if empty
            if (chatMessages.isEmpty()) {
                item {
                    AssistantWelcomeCard(isHindi = isHindi, userProfile = userProfile)
                }
            }

            items(chatMessages) { message ->
                ChatMessageBubble(
                    message = message,
                    isHindi = isHindi,
                    onSpeak = {
                        viewModel.speakText(message.text, if (isHindi) "hi" else "en")
                    }
                )
            }

            // AI Thinking / Typing Indicator
            if (isAiThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PureWhite,
                            tonalElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Emerald700
                                )
                                Text(
                                    text = if (isHindi) "सहायक AI आपके व्यापार व बहीखाते का विस्तृत विश्लेषण कर रहा है..." else "SahayakAI is carefully analyzing your business and khata...",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Prompt Chips Carousel
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(promptSuggestions) { prompt ->
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            inputText = prompt
                            viewModel.sendChatMessage(prompt, autoSpeak = true)
                            inputText = ""
                        }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = Slate800,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 4. Input Bar with TTS & Real Voice-to-Text
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PureWhite,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            if (isHindi) "व्यापार, मुनाफा या योजना का सवाल पूछें..." else "Ask business growth or scheme query...",
                            fontSize = 13.sp,
                            color = Slate500
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("advisor_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedPlaceholderColor = Slate500,
                        unfocusedPlaceholderColor = Slate500,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = Slate50,
                        focusedBorderColor = Emerald700,
                        unfocusedBorderColor = Slate400,
                        cursorColor = Emerald700
                    )
                )

                // Voice-to-Text Mic Button
                IconButton(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            launchVoiceRecognition(
                                context = context,
                                isHindi = isHindi,
                                launcher = speechLauncher,
                                onListening = { isListening = it },
                                onFallback = { showVoiceDialog = true }
                            )
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isListening) Amber300 else Amber100)
                        .testTag("advisor_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Filled.GraphicEq else Icons.Filled.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) Emerald900 else Amber800
                    )
                }

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val textToSend = inputText
                            inputText = ""
                            viewModel.sendChatMessage(textToSend, autoSpeak = true)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isAiThinking,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) Emerald700 else Slate300)
                        .testTag("advisor_send_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = PureWhite
                    )
                }
            }
        }
    }

    // Voice Dialog for devices without voice services or quick speech prompts
    if (showVoiceDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            icon = {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = Amber800, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    text = if (isHindi) "बोलकर पूछें (Voice Prompts)" else "Voice Queries (बोलकर पूछें)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHindi)
                            "सहायक AI से पूछने के लिए नीचे दिए गए प्रश्नों में से चुनें:"
                        else
                            "Select a spoken query to ask SahayakAI:",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                    val voiceSamples = if (isHindi) {
                        listOf(
                            "👤 मेरा नाम क्या है और मेरी दुकान कैसी चल रही है?",
                            "📈 मैं अपना बिज़नेश कैसे बढ़ाऊं और मुनाफा कैसे लाऊं?",
                            "🏦 पीएम मुद्रा लोन ₹50,000 के लिए कैसे अप्लाई करें?",
                            "📋 ग्राहक से उधार वसूली तेजी से कैसे करें?",
                            "🌾 मेरे व्यापार के लिए सरकारी सब्सिडी योजनाएं बताएं"
                        )
                    } else {
                        listOf(
                            "👤 What is my name and business details?",
                            "📈 How can I grow my business and increase profits?",
                            "🏦 How to apply for PM Mudra loan (₹50,000)?",
                            "📋 How to politely recover pending customer udhaar?",
                            "🌾 Best government subsidy schemes for my shop"
                        )
                    }
                    voiceSamples.forEach { sample ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showVoiceDialog = false
                                    viewModel.sendChatMessage(sample, autoSpeak = true)
                                },
                            color = Emerald50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200)
                        ) {
                            Text(
                                text = sample,
                                fontSize = 12.sp,
                                color = Emerald900,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text(if (isHindi) "बंद करें" else "Close", color = Emerald800)
                }
            }
        )
    }
}

private fun launchVoiceRecognition(
    context: Context,
    isHindi: Boolean,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onListening: (Boolean) -> Unit,
    onFallback: () -> Unit
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isHindi) "hi-IN" else "en-IN")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, if (isHindi) "hi-IN" else "en-IN")
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
        putExtra(RecognizerIntent.EXTRA_PROMPT, if (isHindi) "बोलिए, सहायक AI सुन रहा है..." else "Speak now, SahayakAI is listening...")
    }
    try {
        onListening(true)
        launcher.launch(intent)
    } catch (e: ActivityNotFoundException) {
        onListening(false)
        onFallback()
    }
}

@Composable
fun AssistantWelcomeCard(
    isHindi: Boolean,
    userProfile: com.example.data.model.UserProfile
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Emerald800),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Psychology, contentDescription = null, tint = Amber300, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi) "नमस्ते ${userProfile.name.ifBlank { "उद्यमी" }} जी! 🙏" else "Welcome ${userProfile.name.ifBlank { "Entrepreneur" }}! 🙏",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Emerald900
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isHindi)
                    "मैं आपका 'सहायक एआई' सलाहकार हूँ। मैं आपकी दुकान (${userProfile.businessName.ifBlank { "आपकी दुकान" }} - ${userProfile.businessType.titleHi}) के बहीखाते, व्यक्तिगत वित्तीय स्थिति और सरकारी योजनाओं का विश्लेषण करके सही व्यावसायिक विकास और मुनाफे की सलाह देता हूँ।\n\n🎙️ बोलकर पूछने के लिए माइक बटन दबाएं, या नीचे दिए गए सुझावों में से चुनें।"
                else
                    "I am your 'SahayakAI' business advisor. I analyze your enterprise (${userProfile.businessName.ifBlank { "Your Enterprise" }} - ${userProfile.businessType.title}), digitized Khata records, and government schemes to guide your business growth and profitability.\n\n🎙️ Tap the mic icon to ask via voice, or pick one of the quick suggestions below.",
                fontSize = 12.sp,
                color = Slate700,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isHindi: Boolean,
    onSpeak: () -> Unit
) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Emerald700),
                contentAlignment = Alignment.Center
            ) {
                Text("₹", color = Amber300, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = if (isUser) 300.dp else 345.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) Emerald800 else PureWhite,
            tonalElevation = 2.dp,
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, Slate200) else null
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = message.backendTradeTitle?.takeIf { it.isNotBlank() } ?: "SahayakAI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald900
                            )
                            val sourceBadge = when {
                                message.sourceTag.equals("QUEUED", ignoreCase = true) -> "QUEUED"
                                message.sourceTag.equals("CACHED", ignoreCase = true) || message.isOfflineTier -> "CACHED"
                                else -> "LIVE"
                            }
                            val badgeBg = when (sourceBadge) {
                                "LIVE" -> Emerald100
                                "CACHED" -> Amber100
                                else -> Color(0xFFE0E7FF)
                            }
                            val badgeTextColor = when (sourceBadge) {
                                "LIVE" -> Emerald900
                                "CACHED" -> Amber900
                                else -> Color(0xFF3730A3)
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = badgeBg,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, badgeTextColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    if (sourceBadge == "LIVE") {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = Emerald800,
                                            modifier = Modifier.size(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    } else if (sourceBadge == "CACHED") {
                                        Text("💾", fontSize = 7.sp)
                                        Spacer(modifier = Modifier.width(2.dp))
                                    } else {
                                        Text("⏳", fontSize = 7.sp)
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    Text(
                                        text = sourceBadge,
                                        fontSize = 8.sp,
                                        color = badgeTextColor,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Voice Speaker Icon for Low Literacy Users
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read aloud",
                                tint = Emerald700,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    color = if (isUser) PureWhite else Slate900,
                    lineHeight = 19.sp
                )
            }
        }
    }
}
