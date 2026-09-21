package com.example.ui.components.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.data.model.CommunityPost
import com.example.data.model.CommunityUser
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityUserProfileDialog(
    user: CommunityUser,
    viewModel: SahayakViewModel,
    onDismiss: () -> Unit,
    onDirectMessage: () -> Unit,
    onOpenPostComments: (CommunityPost) -> Unit
) {
    val allPosts by viewModel.allPosts.collectAsState()
    val allUsers by viewModel.allCommunityUsers.collectAsState()
    val currentUserProfile by viewModel.userProfile.collectAsState()
    val isHindi = currentUserProfile.preferredLanguage == AppLanguage.HINDI

    // Live reactive instance of this user from StateFlow
    val reactiveUser = allUsers.find { it.uid == user.uid } ?: user
    val userPosts = remember(allPosts, reactiveUser.uid) {
        allPosts.filter { it.userId == reactiveUser.uid }
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isHindi) "उद्यमी प्रोफ़ाइल" else "Merchant Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        if (reactiveUser.isCurrentUser) {
                            IconButton(onClick = { showEditProfileDialog = true }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit Profile", tint = Emerald800)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PureWhite,
                        titleContentColor = Slate900
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Slate50)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CommunityUserAvatar(
                                    name = reactiveUser.displayName,
                                    avatarIndex = reactiveUser.avatarColorIndex,
                                    sizeDp = 64
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = reactiveUser.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Slate900
                                        )
                                        if (reactiveUser.verifiedBadge) {
                                            Icon(
                                                imageVector = Icons.Filled.Verified,
                                                contentDescription = "Verified Merchant",
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "@${reactiveUser.username}",
                                        fontSize = 12.sp,
                                        color = Slate500
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = reactiveUser.role,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Emerald800
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            tint = Slate400,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = reactiveUser.location,
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bio
                            if (reactiveUser.bio.isNotBlank()) {
                                Text(
                                    text = reactiveUser.bio,
                                    fontSize = 12.sp,
                                    color = Slate700,
                                    lineHeight = 17.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Stats row (Posts, Followers, Following)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate100, RoundedCornerShape(12.dp))
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${userPosts.size.coerceAtLeast(reactiveUser.postsCount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = if (isHindi) "पोस्ट" else "Posts",
                                        fontSize = 10.sp,
                                        color = Slate500
                                    )
                                }
                                VerticalDivider(modifier = Modifier.height(24.dp), color = Slate300)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${reactiveUser.followersCount}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = if (isHindi) "फॉलोअर्स" else "Followers",
                                        fontSize = 10.sp,
                                        color = Slate500
                                    )
                                }
                                VerticalDivider(modifier = Modifier.height(24.dp), color = Slate300)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${reactiveUser.followingCount}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = if (isHindi) "फॉलोइंग" else "Following",
                                        fontSize = 10.sp,
                                        color = Slate500
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons: Follow & DM
                            if (!reactiveUser.isCurrentUser) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.toggleFollowUser(reactiveUser.uid) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("follow_user_button_${reactiveUser.uid}"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (reactiveUser.isFollowing) Slate200 else Emerald800,
                                            contentColor = if (reactiveUser.isFollowing) Slate800 else PureWhite
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (reactiveUser.isFollowing) Icons.Filled.Check else Icons.Filled.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (reactiveUser.isFollowing) {
                                                if (isHindi) "फॉलो कर रहे हैं" else "Following"
                                            } else {
                                                if (isHindi) "फॉलो करें" else "Follow"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onDismiss()
                                            onDirectMessage()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("dm_profile_button_${reactiveUser.uid}"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Emerald800
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald700)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MailOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "सीधा संदेश" else "Direct Message",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { showEditProfileDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isHindi) "अपनी प्रोफ़ाइल बदलें" else "Edit My Profile", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Member's Posts Header
                item {
                    Text(
                        text = if (isHindi) "${reactiveUser.displayName} के अनुभव व पोस्ट" else "Posts by ${reactiveUser.displayName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate800,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                if (userPosts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isHindi) "इस सदस्य ने अभी कोई पोस्ट नहीं की है" else "No posts published yet by this member",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }
                }

                items(userPosts, key = { it.id }) { post ->
                    CommunityPostCardItem(
                        post = post,
                        isHindi = isHindi,
                        onLike = { viewModel.togglePostLike(post) },
                        onOpenComments = { onOpenPostComments(post) },
                        onOpenProfile = {},
                        onDirectMessage = onDirectMessage,
                        onPlayVoice = {
                            viewModel.speakText(post.caption, if (isHindi) "hi" else "en")
                        },
                        onDeletePost = if (reactiveUser.isCurrentUser) {
                            { viewModel.deleteCommunityPost(post.id) }
                        } else null
                    )
                }
            }
        }
    }

    if (showEditProfileDialog) {
        var usernameInput by remember { mutableStateOf(reactiveUser.username) }
        var bioInput by remember { mutableStateOf(reactiveUser.bio) }
        var roleInput by remember { mutableStateOf(reactiveUser.role) }
        var locationInput by remember { mutableStateOf(reactiveUser.location) }

        Dialog(onDismissRequest = { showEditProfileDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = PureWhite
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "व्यापारी प्रोफ़ाइल संपादित करें" else "Edit Merchant Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username Handle (@)", color = Slate800) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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
                        value = roleInput,
                        onValueChange = { roleInput = it },
                        label = { Text("Business Role & Specialty", color = Slate800) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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
                        value = locationInput,
                        onValueChange = { locationInput = it },
                        label = { Text("City, State", color = Slate800) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("About Your Business (Bio)", color = Slate800) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate400,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Slate50
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showEditProfileDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.updateOwnCommunityProfile(
                                    username = usernameInput,
                                    bio = bioInput,
                                    role = roleInput,
                                    location = locationInput
                                )
                                showEditProfileDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald800)
                        ) {
                            Text(if (isHindi) "सहेजें" else "Save")
                        }
                    }
                }
            }
        }
    }
}
