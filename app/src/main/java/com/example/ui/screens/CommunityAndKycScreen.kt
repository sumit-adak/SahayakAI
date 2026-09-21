package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.community.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel
import java.util.Locale

@Composable
fun CommunityAndKycScreen(
    viewModel: SahayakViewModel
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isHindi = userProfile.preferredLanguage == AppLanguage.HINDI
    val conversations by viewModel.allConversations.collectAsState()
    val allUsers by viewModel.allCommunityUsers.collectAsState()

    val totalUnreadDMs = remember(conversations) {
        conversations.sumOf { it.unreadCount }
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = if (isHindi) {
        listOf("समुदाय फ़ीड", "व्यापारी DM", "सदस्य सूची", "KYC सैंडबॉक्स")
    } else {
        listOf("Peer Feed", "Direct Messages", "Merchants", "KYC Sandbox")
    }

    var activeConversationId by remember { mutableStateOf<String?>(null) }
    var activePostForComments by remember { mutableStateOf<CommunityPost?>(null) }
    var selectedUserForProfile by remember { mutableStateOf<CommunityUser?>(null) }
    var showCreatePostDialog by remember { mutableStateOf(false) }

    // If an active conversation is open, render ActiveChatScreen fullscreen
    if (activeConversationId != null) {
        ActiveChatScreen(
            conversationId = activeConversationId!!,
            viewModel = viewModel,
            onBack = {
                activeConversationId = null
                viewModel.setActiveConversation(null)
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = PureWhite,
            contentColor = Emerald800,
            edgePadding = 12.dp
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("community_tab_$index"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            if (index == 1 && totalUnreadDMs > 0) {
                                Surface(
                                    color = Emerald700,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$totalUnreadDMs",
                                        color = PureWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> CommunityFeedTab(
                viewModel = viewModel,
                isHindi = isHindi,
                onOpenCreatePost = { showCreatePostDialog = true },
                onOpenComments = { post ->
                    viewModel.setActivePostForComments(post)
                    activePostForComments = post
                },
                onOpenProfile = { uid ->
                    val user = allUsers.find { it.uid == uid }
                    if (user != null) {
                        selectedUserForProfile = user
                    }
                },
                onDirectMessage = { user ->
                    viewModel.startConversationWithUser(user) { convId ->
                        activeConversationId = convId
                    }
                }
            )
            1 -> DirectMessagingInboxView(
                viewModel = viewModel,
                isHindi = isHindi,
                onOpenConversation = { convId ->
                    viewModel.setActiveConversation(convId)
                    activeConversationId = convId
                }
            )
            2 -> CommunityMembersDirectoryTab(
                viewModel = viewModel,
                isHindi = isHindi,
                onOpenProfile = { user -> selectedUserForProfile = user },
                onDirectMessage = { user ->
                    viewModel.startConversationWithUser(user) { convId ->
                        activeConversationId = convId
                    }
                }
            )
            3 -> KycSandboxTab(
                viewModel = viewModel,
                isHindi = isHindi
            )
        }
    }

    // Modal Dialogs
    if (showCreatePostDialog) {
        CreatePostDialog(
            viewModel = viewModel,
            onDismiss = { showCreatePostDialog = false }
        )
    }

    if (activePostForComments != null) {
        PostDetailAndCommentsDialog(
            post = activePostForComments!!,
            viewModel = viewModel,
            onDismiss = {
                activePostForComments = null
                viewModel.setActivePostForComments(null)
            },
            onOpenProfile = { uid ->
                val user = allUsers.find { it.uid == uid }
                if (user != null) {
                    selectedUserForProfile = user
                }
            }
        )
    }

    if (selectedUserForProfile != null) {
        CommunityUserProfileDialog(
            user = selectedUserForProfile!!,
            viewModel = viewModel,
            onDismiss = { selectedUserForProfile = null },
            onDirectMessage = {
                val target = selectedUserForProfile!!
                selectedUserForProfile = null
                viewModel.startConversationWithUser(target) { convId ->
                    activeConversationId = convId
                }
            },
            onOpenPostComments = { post ->
                viewModel.setActivePostForComments(post)
                activePostForComments = post
            }
        )
    }
}

@Composable
fun CommunityFeedTab(
    viewModel: SahayakViewModel,
    isHindi: Boolean,
    onOpenCreatePost: () -> Unit,
    onOpenComments: (CommunityPost) -> Unit,
    onOpenProfile: (String) -> Unit,
    onDirectMessage: (CommunityUser) -> Unit
) {
    val posts by viewModel.allPosts.collectAsState()
    val allUsers by viewModel.allCommunityUsers.collectAsState()
    val moderationNotice by viewModel.moderationNotice.collectAsState()

    var filterType by remember { mutableStateOf<CommunityPostType?>(null) }

    val filteredPosts = remember(posts, filterType) {
        if (filterType == null) posts else posts.filter { it.type == filterType }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenCreatePost,
                containerColor = Emerald800,
                contentColor = PureWhite,
                shape = CircleShape,
                modifier = Modifier.testTag("create_community_post_fab")
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Create Post")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Slate50)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
        ) {
            // Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Emerald50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = Emerald800, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = if (isHindi) "ग्रामीण उद्यमी समुदाय (Pure Social Layer)" else "Rural Entrepreneurs Network",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Emerald900
                            )
                            Text(
                                text = if (isHindi) "साथी दुकानदारों और स्वयं सहायता समूहों से सीधे सीखें, सवाल पूछें व DM करें" else "Share grassroots field photos, practical tips, questions & private peer DMs",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }
                }
            }

            // Moderation safety alert
            item {
                ModerationNoticeBanner(
                    notice = moderationNotice,
                    onDismiss = { viewModel.clearModerationNotice() }
                )
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterType == null,
                        onClick = { filterType = null },
                        label = { Text(if (isHindi) "सभी पोस्ट" else "All Posts", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald800,
                            selectedLabelColor = PureWhite
                        )
                    )
                    FilterChip(
                        selected = filterType == CommunityPostType.PHOTO,
                        onClick = { filterType = CommunityPostType.PHOTO },
                        label = { Text(if (isHindi) "फोटो अनुभव" else "Photo Posts", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald800,
                            selectedLabelColor = PureWhite
                        )
                    )
                    FilterChip(
                        selected = filterType == CommunityPostType.QUESTION,
                        onClick = { filterType = CommunityPostType.QUESTION },
                        label = { Text(if (isHindi) "सवाल / प्रश्न" else "Questions", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Amber800,
                            selectedLabelColor = PureWhite
                        )
                    )
                }
            }

            if (filteredPosts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.DynamicFeed, contentDescription = null, tint = Slate400, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isHindi) "कोई पोस्ट नहीं मिली" else "No posts matching filter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Slate800
                            )
                        }
                    }
                }
            }

            items(filteredPosts, key = { it.id }) { post ->
                val authorUser = allUsers.find { it.uid == post.userId }

                CommunityPostCardItem(
                    post = post,
                    isHindi = isHindi,
                    onLike = { viewModel.togglePostLike(post) },
                    onOpenComments = { onOpenComments(post) },
                    onOpenProfile = { onOpenProfile(post.userId) },
                    onDirectMessage = {
                        if (authorUser != null) {
                            onDirectMessage(authorUser)
                        }
                    },
                    onPlayVoice = {
                        viewModel.speakText(post.caption, if (isHindi) "hi" else "en")
                    },
                    onDeletePost = if (post.userId == "user_current") {
                        { viewModel.deleteCommunityPost(post.id) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun CommunityMembersDirectoryTab(
    viewModel: SahayakViewModel,
    isHindi: Boolean,
    onOpenProfile: (CommunityUser) -> Unit,
    onDirectMessage: (CommunityUser) -> Unit
) {
    val allUsers by viewModel.allCommunityUsers.collectAsState()
    var searchMerchantQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(allUsers, searchMerchantQuery) {
        if (searchMerchantQuery.isBlank()) allUsers
        else allUsers.filter {
            it.displayName.contains(searchMerchantQuery, ignoreCase = true) ||
            it.username.contains(searchMerchantQuery, ignoreCase = true) ||
            it.businessCategory.contains(searchMerchantQuery, ignoreCase = true) ||
            it.location.contains(searchMerchantQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchMerchantQuery,
            onValueChange = { searchMerchantQuery = it },
            placeholder = { Text(if (isHindi) "व्यापारी, स्थान या श्रेणी खोजें..." else "Search by merchant name, location, or trade...", fontSize = 12.sp, color = Slate500) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Slate500) },
            modifier = Modifier.fillMaxWidth(),
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filteredUsers, key = { it.uid }) { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProfile(user) }
                        .testTag("member_card_${user.uid}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CommunityUserAvatar(
                            name = user.displayName,
                            avatarIndex = user.avatarColorIndex,
                            sizeDp = 48
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = user.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Slate900
                                )
                                if (user.isCurrentUser) {
                                    Surface(
                                        color = Emerald100,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isHindi) "आप" else "You",
                                            color = Emerald900,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "@${user.username} • ${user.location}",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                            Text(
                                text = user.role,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Emerald800
                            )
                            Text(
                                text = "${user.followersCount} ${if (isHindi) "फॉलोअर्स" else "followers"} • ${user.businessCategory}",
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }

                        if (!user.isCurrentUser) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.toggleFollowUser(user.uid) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (user.isFollowing) Slate100 else Color.Transparent,
                                        contentColor = if (user.isFollowing) Slate700 else Emerald800
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (user.isFollowing) Slate300 else Emerald700
                                    ),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = if (user.isFollowing) {
                                            if (isHindi) "फॉलोइंग" else "Following"
                                        } else {
                                            if (isHindi) "+ फॉलो" else "+ Follow"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = { onDirectMessage(user) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Send,
                                        contentDescription = "DM",
                                        tint = Emerald800,
                                        modifier = Modifier.size(16.dp)
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

/**
 * Swappable Mock KYC Sandbox (Mandate: Swappable Interface + Clear Demo Labels)
 */
@Composable
fun KycSandboxTab(
    viewModel: SahayakViewModel,
    isHindi: Boolean
) {
    val panDetails by viewModel.panDetails.collectAsState()
    val aadhaarDetails by viewModel.aadhaarDetails.collectAsState()
    val bankAccount by viewModel.bankAccount.collectAsState()
    val cibilReport by viewModel.cibilReport.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Amber100),
                border = androidx.compose.foundation.BorderStroke(1.dp, Amber400)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Amber800)
                    Column {
                        Text(
                            text = "Swappable KycProvider Sandbox (MockKycProvider)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Amber900
                        )
                        Text(
                            text = "All verification endpoints return structured realistic data flagged as 'Demo Data' for SIH 2026 jury evaluation.",
                            fontSize = 10.sp,
                            color = Amber800
                        )
                    }
                }
            }
        }

        // PAN Verification Card
        panDetails?.let { pan ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1. PAN Verification (NDML / NSDL)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                            DemoBadge()
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("PAN: ${pan.panNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emerald800)
                        Text("Full Name: ${pan.fullName}", fontSize = 12.sp, color = Slate700)
                        Text("Status: ${pan.status} • DOB: ${pan.dateOfBirth}", fontSize = 11.sp, color = Slate500)
                    }
                }
            }
        }

        // Aadhaar Verification Card
        aadhaarDetails?.let { aadhaar ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("2. Aadhaar e-KYC (UIDAI Sandbox)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                            DemoBadge()
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Masked Aadhaar: ${aadhaar.maskedAadhaar}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emerald800)
                        Text("Name: ${aadhaar.name}", fontSize = 12.sp, color = Slate700)
                        Text("Address: ${aadhaar.address}", fontSize = 11.sp, color = Slate500)
                    }
                }
            }
        }

        // Bank Account Link Card
        bankAccount?.let { bank ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("3. Account Aggregator (AA) Bank Link", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                            DemoBadge()
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(bank.bankName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emerald800)
                        Text("Account: ${bank.accountNumberMasked} (IFSC: ${bank.ifscCode})", fontSize = 12.sp, color = Slate700)
                        Text("Avg Monthly Balance: ₹${String.format(Locale.ROOT, "%,.0f", bank.avgMonthlyBalance)}", fontSize = 11.sp, color = Slate500)
                    }
                }
            }
        }

        // CIBIL Credit Bureau Card
        cibilReport?.let { cibil ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("4. Credit Bureau (CIBIL / Experian)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                            DemoBadge()
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Score: ${cibil.score}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Emerald800)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(cibil.riskCategory, fontSize = 12.sp, color = Slate600, fontWeight = FontWeight.SemiBold)
                        }
                        Text("On-Time Repayment: ${cibil.onTimeRepaymentPercent}% • Active Loans: ${cibil.activeLoans}", fontSize = 11.sp, color = Slate500)
                    }
                }
            }
        }
    }
}

@Composable
fun DemoBadge() {
    Surface(
        color = Amber200,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "Demo Data",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Amber900,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
