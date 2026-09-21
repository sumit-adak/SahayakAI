package com.example.ui.components.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

// Consistent avatar background colors for community members
val AvatarPalette = listOf(
    Color(0xFF0D5C3A), // Emerald
    Color(0xFF8B2500), // Terracotta
    Color(0xFF1E40AF), // Deep Blue
    Color(0xFFB45309), // Amber
    Color(0xFF6B21A8), // Purple
    Color(0xFF047857), // Forest
    Color(0xFF9F1239), // Rose
    Color(0xFF0E7490)  // Teal
)

@Composable
fun CommunityUserAvatar(
    name: String,
    avatarIndex: Int,
    sizeDp: Int = 40,
    showVerified: Boolean = true,
    isOnline: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val bgColor = AvatarPalette[avatarIndex.coerceIn(0, AvatarPalette.lastIndex)]
    val initial = name.trim().take(1).uppercase()

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(bgColor)
                .border(1.5.dp, PureWhite, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = (sizeDp * 0.42).sp
            )
        }

        if (showVerified) {
            Box(
                modifier = Modifier
                    .size((sizeDp * 0.38).dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB))
                    .border(1.dp, PureWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Verified Member",
                    tint = PureWhite,
                    modifier = Modifier.size((sizeDp * 0.24).dp)
                )
            }
        } else if (isOnline) {
            Box(
                modifier = Modifier
                    .size((sizeDp * 0.32).dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .border(1.5.dp, PureWhite, CircleShape)
            )
        }
    }
}

@Composable
fun ModerationNoticeBanner(
    notice: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = notice != null) {
        if (notice != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("moderation_notice_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Moderation Warning",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Community Safety & Civility Notice",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = notice,
                            fontSize = 11.sp,
                            color = Color(0xFF7F1D1D),
                            lineHeight = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFF991B1B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityPostCardItem(
    post: CommunityPost,
    isHindi: Boolean,
    onLike: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenProfile: () -> Unit,
    onDirectMessage: () -> Unit,
    onPlayVoice: () -> Unit,
    onDeletePost: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Avatar, Name, Handle, Role, Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenProfile() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CommunityUserAvatar(
                        name = post.authorName,
                        avatarIndex = post.authorAvatarIndex,
                        sizeDp = 42
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = post.authorName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Slate900
                            )
                            Text(
                                text = "@${post.authorUsername}",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                        Text(
                            text = post.authorRole,
                            fontSize = 10.sp,
                            color = Slate600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Post Type / Category pill
                Surface(
                    color = when (post.type) {
                        CommunityPostType.PHOTO -> Color(0xFFEFF6FF)
                        CommunityPostType.QUESTION -> Color(0xFFFEF3C7)
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (post.type) {
                            CommunityPostType.PHOTO -> Color(0xFFBFDBFE)
                            CommunityPostType.QUESTION -> Color(0xFFFDE68A)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (post.type) {
                                CommunityPostType.PHOTO -> Icons.Filled.PhotoCamera
                                CommunityPostType.QUESTION -> Icons.AutoMirrored.Filled.HelpOutline
                            },
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = when (post.type) {
                                CommunityPostType.PHOTO -> Color(0xFF1D4ED8)
                                CommunityPostType.QUESTION -> Color(0xFFB45309)
                            }
                        )
                        Text(
                            text = when (post.type) {
                                CommunityPostType.PHOTO -> if (isHindi) "फोटो पोस्ट" else "Photo Post"
                                CommunityPostType.QUESTION -> if (isHindi) "सवाल / प्रश्न" else "Ask Peers"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (post.type) {
                                CommunityPostType.PHOTO -> Color(0xFF1E40AF)
                                CommunityPostType.QUESTION -> Color(0xFF92400E)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Special Question Banner if post type is QUESTION
            if (post.type == CommunityPostType.QUESTION) {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.LiveHelp,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isHindi) "समुदाय से मार्गदर्शन की आवश्यकता है:" else "Peer Advice Requested:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Caption Content
            Text(
                text = post.caption,
                fontSize = 13.sp,
                color = Slate800,
                lineHeight = 19.sp,
                modifier = Modifier.clickable { onOpenComments() }
            )

            // Photo / Media Attachment Card if available
            if (post.type == CommunityPostType.PHOTO && post.mediaUrl != null) {
                Spacer(modifier = Modifier.height(10.dp))
                PhotoPostAttachmentBanner(
                    mediaUrl = post.mediaUrl,
                    category = post.category,
                    isHindi = isHindi,
                    onClick = onOpenComments
                )
            }

            // Voice Note Bar
            if (post.voiceNoteSeconds != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPlayVoice() },
                    color = Emerald50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.PlayCircleFilled, contentDescription = null, tint = Emerald800, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "ऑडियो अनुभव सुनें (${post.voiceNoteSeconds}s)" else "Listen Voice Note (${post.voiceNoteSeconds}s)",
                                fontSize = 11.sp,
                                color = Emerald900,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isHindi) "आवाज़ में सुनने के लिए टैप करें" else "Tap to play recorded advice",
                                fontSize = 10.sp,
                                color = Slate600
                            )
                        }
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = Emerald700, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Tags & Category row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = post.tag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (post.category.isNotBlank()) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = post.category,
                            fontSize = 10.sp,
                            color = Slate600,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = post.createdAtFormatted,
                    fontSize = 10.sp,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Slate100)
            Spacer(modifier = Modifier.height(4.dp))

            // Action Bar: Like, Comments, DM author, Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLike() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("post_like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isLikedByUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLikedByUser) UdhaarRed else Slate500,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.likesCount}",
                        fontSize = 12.sp,
                        fontWeight = if (post.isLikedByUser) FontWeight.Bold else FontWeight.Normal,
                        color = if (post.isLikedByUser) UdhaarRed else Slate600
                    )
                }

                // Comment Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenComments() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("post_comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.commentsCount}",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }

                // DM Author Button (if not own post)
                if (post.userId != "user_current") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDirectMessage() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("post_dm_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Direct Message Author",
                            tint = Emerald700,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "DM संदेश" else "Message",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald800
                        )
                    }
                } else if (onDeletePost != null) {
                    IconButton(
                        onClick = onDeletePost,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Post",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoPostAttachmentBanner(
    mediaUrl: String,
    category: String,
    isHindi: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = Slate800
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
        ) {
            // Decorative background pattern
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0x33FFFFFF),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = Amber400, modifier = Modifier.size(12.dp))
                            Text(
                                text = if (isHindi) "सत्यापित जमीनी फोटो" else "Verified Field Photo",
                                color = PureWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.ZoomIn,
                        contentDescription = "Tap to enlarge",
                        tint = PureWhite.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Center visual representation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Emerald800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                mediaUrl.contains("sewing") -> Icons.Filled.Engineering
                                mediaUrl.contains("dairy") -> Icons.Filled.Agriculture
                                mediaUrl.contains("craft") -> Icons.Filled.Brush
                                mediaUrl.contains("oil") -> Icons.Filled.Opacity
                                else -> Icons.Filled.PhotoLibrary
                            },
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = when {
                                mediaUrl.contains("sewing") -> if (isHindi) "4 ऑटोमेटिक सिलाई मशीनें (Mudra Loan)" else "4 Automated Sewing Machines (Mudra Loan)"
                                mediaUrl.contains("dairy") -> if (isHindi) "साइलेज साइलो पिट व दुग्ध संग्रह" else "Silage Fodder Silo Pit & Collection"
                                mediaUrl.contains("craft") -> if (isHindi) "टेराकोटा दीये व वाटर बॉटल्स (PM विश्वकर्मा)" else "Terracotta Pots & Water Bottles"
                                mediaUrl.contains("oil") -> if (isHindi) "मिर्जापुर कच्ची घानी सरसों तेल टीन" else "Cold Press Mustard Oil Tins"
                                else -> if (isHindi) "व्यापार प्रमाण फोटो संलग्न" else "Business Proof Photo Attached"
                            },
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isHindi) "पूर्ण आकार में देखने और विवरण के लिए टैप करें" else "Tap to view full resolution & comments",
                            color = Slate300,
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = "Sahayak Community Proof Verified • No AI Generation",
                    color = Slate400,
                    fontSize = 9.sp
                )
            }
        }
    }
}
