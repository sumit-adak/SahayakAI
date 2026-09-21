package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class CommunityPostType(val labelEn: String, val labelHi: String) {
    PHOTO("Photo Story", "फोटो अनुभव"),
    QUESTION("Ask Question", "प्रश्न / सलाह")
}

@Entity(tableName = "community_users")
data class CommunityUser(
    @PrimaryKey val uid: String,
    val username: String,
    val displayName: String,
    val displayNameHi: String = "",
    val bio: String,
    val avatarUrl: String = "",
    val avatarColorIndex: Int = 0,
    val role: String,
    val location: String = "Varanasi, UP",
    val followersCount: Int = 124,
    val followingCount: Int = 56,
    val postsCount: Int = 12,
    val isFollowing: Boolean = false,
    val isCurrentUser: Boolean = false,
    val verifiedBadge: Boolean = true,
    val businessCategory: String = "Retail & Trade"
)

@Entity(tableName = "community_posts")
data class CommunityPost(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: String = "post_${System.currentTimeMillis()}",
    val userId: String = "user_default",
    val authorName: String,
    val authorUsername: String = "entrepreneur",
    val authorAvatar: String = "",
    val authorAvatarIndex: Int = 0,
    val authorRole: String,
    val type: CommunityPostType = CommunityPostType.PHOTO,
    val caption: String,
    val mediaUrl: String? = null,
    val tag: String = "#General",
    val category: String = "Business Tips",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByUser: Boolean = false,
    val voiceNoteSeconds: Int? = null,
    val createdAtFormatted: String = "Just now",
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "community_comments")
data class PostComment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commentId: String = "cmt_${System.currentTimeMillis()}",
    val postId: Long,
    val userId: String,
    val userName: String,
    val userUsername: String = "",
    val userAvatar: String = "",
    val userAvatarIndex: Int = 0,
    val userRole: String = "",
    val content: String,
    val createdAtFormatted: String = "Just now",
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "direct_conversations")
data class DirectConversation(
    @PrimaryKey val conversationId: String, // deterministic: sorted(uid1, uid2).joinToString("_")
    val participant1Uid: String,
    val participant2Uid: String,
    val otherUserId: String,
    val otherUserName: String,
    val otherUserUsername: String,
    val otherUserAvatar: String = "",
    val otherUserAvatarIndex: Int = 0,
    val otherUserRole: String,
    val lastMessage: String,
    val lastMessageAtFormatted: String = "Just now",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isOnline: Boolean = true
)

@Entity(tableName = "direct_messages")
data class DirectMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String = "msg_${System.currentTimeMillis()}",
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String = "",
    val senderAvatarIndex: Int = 0,
    val content: String,
    val mediaUrl: String? = null,
    val timestampFormatted: String = "Just now",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = true,
    val isFromCurrentUser: Boolean = false
)

@Entity(tableName = "follow_relations")
data class FollowRelation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val followerUid: String,
    val followingUid: String,
    val followedAt: Long = System.currentTimeMillis()
)
