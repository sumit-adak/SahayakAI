package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries ORDER BY date DESC, id DESC")
    fun getAllEntries(): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entries WHERE isConfirmed = 0 ORDER BY id DESC")
    fun getPendingEntries(): Flow<List<LedgerEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LedgerEntry>)

    @Update
    suspend fun updateEntry(entry: LedgerEntry)

    @Delete
    suspend fun deleteEntry(entry: LedgerEntry)

    @Query("DELETE FROM ledger_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SchemeDao {
    @Query("SELECT * FROM schemes")
    fun getAllSchemes(): Flow<List<Scheme>>

    @Query("SELECT * FROM schemes WHERE category = :category")
    fun getSchemesByCategory(category: SchemeCategory): Flow<List<Scheme>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchemes(schemes: List<Scheme>)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllReminders(): Flow<List<BusinessReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: BusinessReminder): Long

    @Update
    suspend fun updateReminder(reminder: BusinessReminder)

    @Query("UPDATE reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Delete
    suspend fun deleteReminder(reminder: BusinessReminder)
}

@Dao
interface CommunityDao {
    // --- Posts ---
    @Query("SELECT * FROM community_posts ORDER BY id DESC")
    fun getAllPosts(): Flow<List<CommunityPost>>

    @Query("SELECT * FROM community_posts WHERE type = :type ORDER BY id DESC")
    fun getPostsByType(type: CommunityPostType): Flow<List<CommunityPost>>

    @Query("SELECT * FROM community_posts WHERE userId = :userId ORDER BY id DESC")
    fun getPostsByUser(userId: String): Flow<List<CommunityPost>>

    @Query("SELECT * FROM community_posts WHERE id = :id")
    suspend fun getPostById(id: Long): CommunityPost?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: CommunityPost): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<CommunityPost>)

    @Query("DELETE FROM community_posts WHERE id = :id")
    suspend fun deletePost(id: Long)

    @Query("UPDATE community_posts SET isLikedByUser = :isLiked, likesCount = :newCount WHERE id = :id")
    suspend fun updateLike(id: Long, isLiked: Boolean, newCount: Int)

    @Query("UPDATE community_posts SET commentsCount = commentsCount + 1 WHERE id = :postId")
    suspend fun incrementCommentCount(postId: Long)

    @Query("UPDATE community_posts SET commentsCount = MAX(0, commentsCount - 1) WHERE id = :postId")
    suspend fun decrementCommentCount(postId: Long)

    // --- Comments ---
    @Query("SELECT * FROM community_comments WHERE postId = :postId ORDER BY id ASC")
    fun getCommentsForPost(postId: Long): Flow<List<PostComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: PostComment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<PostComment>)

    @Query("DELETE FROM community_comments WHERE id = :id")
    suspend fun deleteComment(id: Long)

    // --- Users ---
    @Query("SELECT * FROM community_users ORDER BY isCurrentUser DESC, followersCount DESC")
    fun getAllUsers(): Flow<List<CommunityUser>>

    @Query("SELECT * FROM community_users WHERE uid = :uid")
    fun getUserByUid(uid: String): Flow<CommunityUser?>

    @Query("SELECT * FROM community_users WHERE uid = :uid")
    suspend fun getUserSync(uid: String): CommunityUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: CommunityUser)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<CommunityUser>)

    @Update
    suspend fun updateUser(user: CommunityUser)

    @Query("UPDATE community_users SET isFollowing = :isFollowing, followersCount = :newFollowersCount WHERE uid = :uid")
    suspend fun updateFollowStatus(uid: String, isFollowing: Boolean, newFollowersCount: Int)

    // --- Follow Relations ---
    @Query("SELECT * FROM follow_relations WHERE followingUid = :followingUid")
    fun getFollowers(followingUid: String): Flow<List<FollowRelation>>

    @Query("SELECT * FROM follow_relations WHERE followerUid = :followerUid")
    fun getFollowing(followerUid: String): Flow<List<FollowRelation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowRelation(relation: FollowRelation)

    @Query("DELETE FROM follow_relations WHERE followerUid = :followerUid AND followingUid = :followingUid")
    suspend fun deleteFollowRelation(followerUid: String, followingUid: String)

    // --- Direct Conversations & Messages ---
    @Query("SELECT * FROM direct_conversations ORDER BY lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<DirectConversation>>

    @Query("SELECT * FROM direct_conversations WHERE conversationId = :conversationId")
    fun getConversationById(conversationId: String): Flow<DirectConversation?>

    @Query("SELECT * FROM direct_conversations WHERE conversationId = :conversationId")
    suspend fun getConversationSync(conversationId: String): DirectConversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: DirectConversation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<DirectConversation>)

    @Query("UPDATE direct_conversations SET lastMessage = :lastMsg, lastMessageTimestamp = :timestamp, lastMessageAtFormatted = :timeFormatted, unreadCount = :unreadCount WHERE conversationId = :conversationId")
    suspend fun updateConversationLastMessage(
        conversationId: String,
        lastMsg: String,
        timestamp: Long,
        timeFormatted: String,
        unreadCount: Int
    )

    @Query("UPDATE direct_conversations SET unreadCount = 0 WHERE conversationId = :conversationId")
    suspend fun resetUnreadCount(conversationId: String)

    @Query("SELECT * FROM direct_messages WHERE conversationId = :conversationId ORDER BY id ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<DirectMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DirectMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<DirectMessage>)

    @Query("UPDATE direct_messages SET isRead = 1 WHERE conversationId = :conversationId")
    suspend fun markMessagesAsRead(conversationId: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM digital_documents ORDER BY id DESC")
    fun getAllDocuments(): Flow<List<DigitalDocument>>

    @Query("SELECT * FROM digital_documents WHERE category = :category ORDER BY id DESC")
    fun getDocumentsByCategory(category: DocumentCategory): Flow<List<DigitalDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: DigitalDocument): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(docs: List<DigitalDocument>)

    @Delete
    suspend fun deleteDocument(doc: DigitalDocument)

    @Query("DELETE FROM digital_documents WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface KhataDao {
    @Query("SELECT * FROM scanned_khatas ORDER BY updatedAt DESC, id DESC")
    fun getAllKhatas(): Flow<List<ScannedKhata>>

    @Query("SELECT * FROM scanned_khatas WHERE id = :id")
    fun getKhataById(id: Long): Flow<ScannedKhata?>

    @Query("SELECT * FROM scanned_khata_entries WHERE khataId = :khataId ORDER BY id ASC")
    fun getEntriesForKhata(khataId: Long): Flow<List<ScannedKhataEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhata(khata: ScannedKhata): Long

    @Update
    suspend fun updateKhata(khata: ScannedKhata)

    @Delete
    suspend fun deleteKhata(khata: ScannedKhata)

    @Query("DELETE FROM scanned_khatas WHERE id = :id")
    suspend fun deleteKhataById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataEntries(entries: List<ScannedKhataEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataEntry(entry: ScannedKhataEntry): Long

    @Update
    suspend fun updateKhataEntry(entry: ScannedKhataEntry)

    @Query("DELETE FROM scanned_khata_entries WHERE id = :id")
    suspend fun deleteKhataEntryById(id: Long)

    @Query("DELETE FROM scanned_khata_entries WHERE khataId = :khataId")
    suspend fun deleteEntriesForKhata(khataId: Long)
}

@Dao
interface FeasibilityDao {
    @Query("SELECT * FROM feasibility_checks ORDER BY createdAt DESC, id DESC")
    fun getAllChecks(): Flow<List<FeasibilityCheckEntity>>

    @Query("SELECT * FROM feasibility_checks WHERE id = :id")
    suspend fun getCheckById(id: Long): FeasibilityCheckEntity?

    @Query("SELECT * FROM feasibility_checks WHERE checkId = :checkId LIMIT 1")
    suspend fun getCheckByCheckId(checkId: String): FeasibilityCheckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: FeasibilityCheckEntity): Long

    @Delete
    suspend fun deleteCheck(check: FeasibilityCheckEntity)

    @Query("DELETE FROM feasibility_checks WHERE id = :id")
    suspend fun deleteById(id: Long)
}

