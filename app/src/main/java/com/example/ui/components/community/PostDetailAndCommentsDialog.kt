package com.example.ui.components.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.data.model.CommunityPost
import com.example.data.model.CommunityPostType
import com.example.data.model.PostComment
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailAndCommentsDialog(
    post: CommunityPost,
    viewModel: SahayakViewModel,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val comments by viewModel.activePostComments.collectAsState()
    val moderationNotice by viewModel.moderationNotice.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isHindi = userProfile.preferredLanguage == AppLanguage.HINDI

    var commentText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isHindi) "पोस्ट व टिप्पणियां (${comments.size})" else "Post & Comments (${comments.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .navigationBarsPadding()
                                .imePadding(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CommunityUserAvatar(
                                name = userProfile.name,
                                avatarIndex = 0,
                                sizeDp = 36,
                                showVerified = false
                            )

                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = {
                                    Text(
                                        if (isHindi) "व्यावसायिक राय या सुझाव लिखें..." else "Write a constructive comment or reply...",
                                        fontSize = 12.sp,
                                        color = Slate500
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("comment_input_field"),
                                shape = RoundedCornerShape(20.dp),
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
                                    if (commentText.isNotBlank()) {
                                        val success = viewModel.addPostComment(post.id, commentText)
                                        if (success) {
                                            commentText = ""
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Emerald800, CircleShape)
                                    .testTag("submit_comment_button"),
                                colors = IconButtonDefaults.iconButtonColors(contentColor = PureWhite)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Comment",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
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
                // Header Post Detail
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CommunityUserAvatar(
                                    name = post.authorName,
                                    avatarIndex = post.authorAvatarIndex,
                                    sizeDp = 44,
                                    onClick = { onOpenProfile(post.userId) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = post.authorName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "@${post.authorUsername} • ${post.authorRole}",
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                                Text(
                                    text = post.createdAtFormatted,
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = post.caption,
                                fontSize = 14.sp,
                                color = Slate800,
                                lineHeight = 20.sp
                            )

                            if (post.type == CommunityPostType.PHOTO && post.mediaUrl != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                PhotoPostAttachmentBanner(
                                    mediaUrl = post.mediaUrl,
                                    category = post.category,
                                    isHindi = isHindi,
                                    onClick = {}
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { viewModel.togglePostLike(post) }
                                ) {
                                    Icon(
                                        imageVector = if (post.isLikedByUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (post.isLikedByUser) UdhaarRed else Slate500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${post.likesCount} ${if (isHindi) "पसंद" else "Likes"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (post.isLikedByUser) UdhaarRed else Slate700
                                    )
                                }

                                Surface(
                                    color = Amber100,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = post.tag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Amber900,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Comments Section Header
                item {
                    Text(
                        text = if (isHindi) "समुदाय की राय व सुझाव (${comments.size})" else "Community Insights & Replies (${comments.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate700,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (comments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Forum,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isHindi) "अभी तक कोई टिप्पणी नहीं है" else "No comments yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Slate800
                                )
                                Text(
                                    text = if (isHindi) "पहला उपयोगी सुझाव या अनुभव साझा करें!" else "Be the first to share business advice!",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }
                }

                items(comments, key = { it.id }) { comment ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comment_item_${comment.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.clickable { onOpenProfile(comment.userId) }
                                ) {
                                    CommunityUserAvatar(
                                        name = comment.userName,
                                        avatarIndex = comment.userAvatarIndex,
                                        sizeDp = 32
                                    )
                                    Column {
                                        Text(
                                            text = comment.userName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "@${comment.userUsername}",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = comment.createdAtFormatted,
                                        fontSize = 10.sp,
                                        color = Slate400
                                    )
                                    if (comment.userId == "user_current") {
                                        IconButton(
                                            onClick = { viewModel.deletePostComment(comment.id, post.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Delete Comment",
                                                tint = Slate400,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = comment.content,
                                fontSize = 12.sp,
                                color = Slate800,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
