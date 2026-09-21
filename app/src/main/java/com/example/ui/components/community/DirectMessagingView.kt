package com.example.ui.components.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.DirectConversation
import com.example.data.model.DirectMessage
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel
import kotlinx.coroutines.launch

@Composable
fun DirectMessagingInboxView(
    viewModel: SahayakViewModel,
    isHindi: Boolean,
    onOpenConversation: (String) -> Unit
) {
    val conversations by viewModel.allConversations.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) conversations
        else conversations.filter {
            it.otherUserName.contains(searchQuery, ignoreCase = true) ||
            it.otherUserUsername.contains(searchQuery, ignoreCase = true) ||
            it.lastMessage.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (isHindi) "संदेश या व्यापारी खोजें..." else "Search messages or merchants...", fontSize = 12.sp, color = Slate500) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Slate500) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Slate500)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dm_search_field"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Slate900,
                unfocusedTextColor = Slate900,
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                focusedBorderColor = Emerald700,
                unfocusedBorderColor = Slate400
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Security & Privacy notice as required by system prompt
        Surface(
            color = Color(0xFFF0FDF4),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Emerald800, modifier = Modifier.size(16.dp))
                Text(
                    text = if (isHindi) "सुरक्षित व्यापारी DM: संदेश केवल आपके और संबंधित व्यापारी के बीच सीमित हैं।" else "Private Merchant DM: Conversations are strictly locked between participants.",
                    fontSize = 11.sp,
                    color = Emerald900,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredConversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.MarkChatUnread,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "कोई सीधी बातचीत नहीं मिली" else "No Direct Messages Yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate800
                    )
                    Text(
                        text = if (isHindi) "फ़ीड में किसी पोस्ट पर 'DM संदेश' दबाकर बात शुरू करें" else "Tap 'Message' on any feed post to start a private chat",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredConversations, key = { it.conversationId }) { conv ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenConversation(conv.conversationId) }
                            .testTag("conversation_item_${conv.conversationId}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CommunityUserAvatar(
                                name = conv.otherUserName,
                                avatarIndex = conv.otherUserAvatarIndex,
                                sizeDp = 46,
                                isOnline = conv.isOnline
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = conv.otherUserName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = conv.lastMessageAtFormatted,
                                        fontSize = 10.sp,
                                        color = if (conv.unreadCount > 0) Emerald800 else Slate400,
                                        fontWeight = if (conv.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                Text(
                                    text = "@${conv.otherUserUsername} • ${conv.otherUserRole}",
                                    fontSize = 10.sp,
                                    color = Slate500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = conv.lastMessage,
                                        fontSize = 12.sp,
                                        color = if (conv.unreadCount > 0) Slate900 else Slate600,
                                        fontWeight = if (conv.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (conv.unreadCount > 0) {
                                        Surface(
                                            color = Emerald700,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${conv.unreadCount}",
                                                color = PureWhite,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveChatScreen(
    conversationId: String,
    viewModel: SahayakViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.activeConversationMessages.collectAsState()
    val conversations by viewModel.allConversations.collectAsState()
    val moderationNotice by viewModel.moderationNotice.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isHindi = userProfile.preferredLanguage == AppLanguage.HINDI

    val conversation = conversations.find { it.conversationId == conversationId }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var messageInput by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickResponses = if (isHindi) listOf(
        "नमस्ते! क्या थोक स्टॉक उपलब्ध है?",
        "दर प्रति किलो / टीन क्या रहेगी?",
        "कृपया बिल की पर्ची साझा करें",
        "कल सुबह तक माल भेज दीजिए"
    ) else listOf(
        "Hello! Is wholesale stock available?",
        "What is the final wholesale rate?",
        "Please send the scanned khata receipt",
        "Confirmed, please dispatch tomorrow"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (conversation != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CommunityUserAvatar(
                                name = conversation.otherUserName,
                                avatarIndex = conversation.otherUserAvatarIndex,
                                sizeDp = 36,
                                isOnline = conversation.isOnline
                            )
                            Column {
                                Text(
                                    text = conversation.otherUserName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "@${conversation.otherUserUsername} • ${conversation.otherUserRole}",
                                    fontSize = 10.sp,
                                    color = Slate600,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Text(if (isHindi) "सीधा संदेश" else "Direct Message", fontSize = 16.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureWhite,
                    titleContentColor = Slate900
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PureWhite,
                shadowElevation = 8.dp
            ) {
                Column {
                    ModerationNoticeBanner(
                        notice = moderationNotice,
                        onDismiss = { viewModel.clearModerationNotice() }
                    )

                    // Quick response chips for fast merchant trade communication
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 42.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                quickResponses.forEach { chipText ->
                                    Surface(
                                        color = Emerald50,
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200),
                                        modifier = Modifier.clickable {
                                            viewModel.sendDirectMessage(conversationId, chipText)
                                        }
                                    ) {
                                        Text(
                                            text = chipText,
                                            fontSize = 11.sp,
                                            color = Emerald900,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            placeholder = { Text(if (isHindi) "संदेश लिखें..." else "Type direct message...", fontSize = 13.sp, color = Slate500) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dm_message_input"),
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedBorderColor = Emerald700,
                                unfocusedBorderColor = Slate400,
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = Slate50
                            ),
                            maxLines = 3
                        )

                        IconButton(
                            onClick = {
                                if (messageInput.isNotBlank()) {
                                    val success = viewModel.sendDirectMessage(conversationId, messageInput)
                                    if (success) {
                                        messageInput = ""
                                        scope.launch {
                                            if (messages.isNotEmpty()) {
                                                listState.animateScrollToItem(messages.size - 1)
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Emerald800, CircleShape)
                                .testTag("dm_send_button"),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = PureWhite)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Slate50)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // E2E security header in thread
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        color = Slate200,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isHindi) "🔒 सीधी सुरक्षित वार्ता • कोई बिचौलिया नहीं" else "🔒 Direct Peer Channel • Private & Moderated",
                            fontSize = 10.sp,
                            color = Slate700,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val isMe = msg.isFromCurrentUser || msg.senderId == "user_current"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    if (!isMe) {
                        CommunityUserAvatar(
                            name = msg.senderName,
                            avatarIndex = msg.senderAvatarIndex,
                            sizeDp = 28,
                            showVerified = false
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Card(
                        shape = RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = if (isMe) 14.dp else 2.dp,
                            bottomEnd = if (isMe) 2.dp else 14.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) Emerald800 else PureWhite
                        ),
                        border = if (isMe) null else androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = msg.content,
                                fontSize = 13.sp,
                                color = if (isMe) PureWhite else Slate900,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = msg.timestampFormatted,
                                    fontSize = 9.sp,
                                    color = if (isMe) PureWhite.copy(alpha = 0.7f) else Slate400
                                )
                                if (isMe) {
                                    Icon(
                                        imageVector = if (msg.isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                                        contentDescription = null,
                                        tint = if (msg.isRead) Amber300 else PureWhite.copy(alpha = 0.7f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
