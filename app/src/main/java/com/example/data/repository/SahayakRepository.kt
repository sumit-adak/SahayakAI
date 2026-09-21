package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.data.service.KycProvider
import com.example.data.service.MockKycProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SahayakRepository(
    private val database: AppDatabase,
    val kycProvider: KycProvider = MockKycProvider()
) {
    private val ledgerDao = database.ledgerDao()
    private val schemeDao = database.schemeDao()
    private val reminderDao = database.reminderDao()
    private val communityDao = database.communityDao()
    private val chatDao = database.chatMessageDao()
    private val documentDao = database.documentDao()
    private val khataDao = database.khataDao()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile

    private val _mandiPrices = MutableStateFlow(getInitialMandiPrices())
    val mandiPrices: StateFlow<List<MandiCommodity>> = _mandiPrices

    val allLedgerEntries: Flow<List<LedgerEntry>> = ledgerDao.getAllEntries()
    val allSchemes: Flow<List<Scheme>> = schemeDao.getAllSchemes()
    val allReminders: Flow<List<BusinessReminder>> = reminderDao.getAllReminders()
    val allPosts: Flow<List<CommunityPost>> = communityDao.getAllPosts()
    val allCommunityUsers: Flow<List<CommunityUser>> = communityDao.getAllUsers()
    val allConversations: Flow<List<DirectConversation>> = communityDao.getAllConversations()
    val chatHistory: Flow<List<ChatMessage>> = chatDao.getAllMessages()
    val allDigitalDocuments: Flow<List<DigitalDocument>> = documentDao.getAllDocuments()
    val allScannedKhatas: Flow<List<ScannedKhata>> = khataDao.getAllKhatas()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfEmpty()
        }
    }

    fun updateUserProfile(profile: UserProfile) {
        _userProfile.value = profile
    }

    suspend fun addLedgerEntry(entry: LedgerEntry): Long {
        return ledgerDao.insertEntry(entry)
    }

    suspend fun addLedgerEntries(entries: List<LedgerEntry>) {
        ledgerDao.insertEntries(entries)
    }

    suspend fun updateLedgerEntry(entry: LedgerEntry) {
        ledgerDao.updateEntry(entry)
    }

    suspend fun deleteLedgerEntry(id: Long) {
        ledgerDao.deleteById(id)
    }

    // Scanned Khata & Entries Operations
    fun getKhataById(id: Long): Flow<ScannedKhata?> {
        return khataDao.getKhataById(id)
    }

    fun getEntriesForKhata(khataId: Long): Flow<List<ScannedKhataEntry>> {
        return khataDao.getEntriesForKhata(khataId)
    }

    suspend fun saveConfirmedKhata(
        khata: ScannedKhata,
        entries: List<ScannedKhataEntry>,
        syncToAggregateLedger: Boolean = true
    ): Long {
        val totalCredit = entries.filter { it.type == LedgerType.CREDIT }.sumOf { it.amount }
        val totalDebit = entries.filter { it.type == LedgerType.DEBIT }.sumOf { it.amount }
        val updatedKhata = khata.copy(
            entryCount = entries.size,
            totalCredit = totalCredit,
            totalDebit = totalDebit,
            updatedAt = System.currentTimeMillis()
        )
        val khataId = khataDao.insertKhata(updatedKhata)

        // Save entries linked with this khataId
        val mappedEntries = entries.map { it.copy(khataId = khataId) }
        khataDao.insertKhataEntries(mappedEntries)

        if (syncToAggregateLedger) {
            val ledgerItems = mappedEntries.map { entry ->
                LedgerEntry(
                    date = entry.date,
                    partyName = entry.partyName,
                    description = "${khata.label}: ${entry.description}",
                    amount = entry.amount,
                    type = entry.type,
                    category = entry.category,
                    source = LedgerSource.OCR,
                    isConfirmed = true,
                    linkedKhataId = khataId
                )
            }
            ledgerDao.insertEntries(ledgerItems)
        }

        return khataId
    }

    suspend fun updateScannedKhata(khata: ScannedKhata, entries: List<ScannedKhataEntry>) {
        val totalCredit = entries.filter { it.type == LedgerType.CREDIT }.sumOf { it.amount }
        val totalDebit = entries.filter { it.type == LedgerType.DEBIT }.sumOf { it.amount }
        val updatedKhata = khata.copy(
            entryCount = entries.size,
            totalCredit = totalCredit,
            totalDebit = totalDebit,
            updatedAt = System.currentTimeMillis()
        )
        khataDao.updateKhata(updatedKhata)
        khataDao.deleteEntriesForKhata(khata.id)
        val mapped = entries.map { it.copy(khataId = khata.id) }
        khataDao.insertKhataEntries(mapped)
    }

    suspend fun deleteScannedKhata(khataId: Long) {
        khataDao.deleteKhataById(khataId)
        khataDao.deleteEntriesForKhata(khataId)
    }

    suspend fun addReminder(reminder: BusinessReminder): Long {
        return reminderDao.insertReminder(reminder)
    }

    suspend fun toggleReminderComplete(id: Long, isCompleted: Boolean) {
        reminderDao.setCompleted(id, isCompleted)
    }

    suspend fun deleteReminder(reminder: BusinessReminder) {
        reminderDao.deleteReminder(reminder)
    }

    suspend fun addCommunityPost(post: CommunityPost): Long {
        return communityDao.insertPost(post)
    }

    suspend fun deleteCommunityPost(id: Long) {
        communityDao.deletePost(id)
    }

    suspend fun togglePostLike(id: Long, currentlyLiked: Boolean, currentCount: Int) {
        val newLiked = !currentlyLiked
        val newCount = if (newLiked) currentCount + 1 else (currentCount - 1).coerceAtLeast(0)
        communityDao.updateLike(id, newLiked, newCount)
    }

    fun getCommentsForPost(postId: Long): Flow<List<PostComment>> {
        return communityDao.getCommentsForPost(postId)
    }

    suspend fun addPostComment(comment: PostComment): Long {
        val id = communityDao.insertComment(comment)
        communityDao.incrementCommentCount(comment.postId)
        return id
    }

    suspend fun deletePostComment(commentId: Long, postId: Long) {
        communityDao.deleteComment(commentId)
        communityDao.decrementCommentCount(postId)
    }

    fun getCommunityUser(uid: String): Flow<CommunityUser?> {
        return communityDao.getUserByUid(uid)
    }

    suspend fun updateCommunityUser(user: CommunityUser) {
        communityDao.updateUser(user)
    }

    suspend fun toggleFollowUser(targetUid: String) {
        val targetUser = communityDao.getUserSync(targetUid) ?: return
        val newFollowing = !targetUser.isFollowing
        val newCount = if (newFollowing) targetUser.followersCount + 1 else (targetUser.followersCount - 1).coerceAtLeast(0)
        communityDao.updateFollowStatus(targetUid, newFollowing, newCount)
        if (newFollowing) {
            communityDao.insertFollowRelation(
                FollowRelation(followerUid = "user_current", followingUid = targetUid)
            )
        } else {
            communityDao.deleteFollowRelation(followerUid = "user_current", followingUid = targetUid)
        }
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<DirectMessage>> {
        return communityDao.getMessagesForConversation(conversationId)
    }

    fun getConversation(conversationId: String): Flow<DirectConversation?> {
        return communityDao.getConversationById(conversationId)
    }

    suspend fun getOrCreateConversation(
        targetUser: CommunityUser,
        currentUser: UserProfile
    ): String {
        val currentUid = "user_current"
        val targetUid = targetUser.uid
        // Deterministic ID as per prompt mandate: sorted([uid1, uid2]).joinToString("_")
        val conversationId = listOf(currentUid, targetUid).sorted().joinToString("_")

        val existing = communityDao.getConversationSync(conversationId)
        if (existing == null) {
            val newConversation = DirectConversation(
                conversationId = conversationId,
                participant1Uid = currentUid,
                participant2Uid = targetUid,
                otherUserId = targetUser.uid,
                otherUserName = targetUser.displayName,
                otherUserUsername = targetUser.username,
                otherUserAvatar = targetUser.avatarUrl,
                otherUserAvatarIndex = targetUser.avatarColorIndex,
                otherUserRole = targetUser.role,
                lastMessage = "Start your conversation with ${targetUser.displayName}",
                lastMessageAtFormatted = "New",
                lastMessageTimestamp = System.currentTimeMillis(),
                unreadCount = 0
            )
            communityDao.insertConversation(newConversation)
        }
        return conversationId
    }

    suspend fun sendDirectMessage(
        conversationId: String,
        content: String,
        mediaUrl: String? = null,
        senderName: String
    ): Long {
        val sdf = SimpleDateFormat("h:mm a", Locale.ROOT)
        val timeStr = sdf.format(Date())
        val msg = DirectMessage(
            conversationId = conversationId,
            senderId = "user_current",
            senderName = senderName,
            content = content,
            mediaUrl = mediaUrl,
            timestampFormatted = timeStr,
            timestamp = System.currentTimeMillis(),
            isRead = true,
            isFromCurrentUser = true
        )
        val id = communityDao.insertMessage(msg)
        communityDao.updateConversationLastMessage(
            conversationId = conversationId,
            lastMsg = content,
            timestamp = System.currentTimeMillis(),
            timeFormatted = timeStr,
            unreadCount = 0
        )
        return id
    }

    suspend fun markConversationRead(conversationId: String) {
        communityDao.resetUnreadCount(conversationId)
        communityDao.markMessagesAsRead(conversationId)
    }

    suspend fun saveChatMessage(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun clearChat() {
        chatDao.clearHistory()
    }

    suspend fun addDigitalDocument(doc: DigitalDocument): Long {
        return documentDao.insertDocument(doc)
    }

    suspend fun deleteDigitalDocument(id: Long) {
        documentDao.deleteById(id)
    }

    private suspend fun seedInitialDataIfEmpty() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val today = sdf.format(Date())

        // Initial Government Schemes with Profession Eligibility Mapping (Part D2)
        val initialSchemes = listOf(
            Scheme(
                id = "pm_mudra",
                name = "PM Mudra Yojana (PMMY)",
                nameHi = "प्रधानमंत्री मुद्रा योजना",
                ministry = "Ministry of Finance",
                description = "Collateral-free micro loans up to ₹10 Lakh for small business setup, shop expansion, and equipment.",
                descriptionHi = "दुकान, सिलाई, डेयरी व लघु उद्यम हेतु ₹10 लाख तक का बिना गारंटी बैंक ऋण (शिशु, किशोर व तरुण श्रेणियां)।",
                subsidyPercent = 0, // Interest subvention
                maxLoanAmount = 1000000.0,
                eligibilityCriteria = "All Indian citizens running or starting micro business (Kirana, food stall, artisan, services). No collateral required.",
                eligibilityCriteriaHi = "कोई भी भारतीय नागरिक जो लघु व्यवसाय चला रहा हो या शुरू करना चाहता हो। संपत्ति गारंटी की आवश्यकता नहीं।",
                documentsRequired = "Aadhaar Card, PAN Card, Business Address Proof, SahayakAI Khata Bank Report.",
                category = SchemeCategory.CREDIT_LOAN,
                deadline = "Ongoing Year-Round",
                eligibleProfessions = "ALL"
            ),
            Scheme(
                id = "pmegp_scheme",
                name = "PMEGP (Prime Minister Employment Generation)",
                nameHi = "प्रधानमंत्री रोजगार सृजन कार्यक्रम (PMEGP)",
                ministry = "Ministry of MSME & KVIC",
                description = "Credit-linked subsidy programme offering up to 35% government subsidy for micro enterprises in rural areas.",
                descriptionHi = "ग्रामीण क्षेत्रों में नया उद्यम लगाने पर सरकार से 35% तक की नकद सब्सिडी (मैन्युफैक्चरिंग ₹50 लाख, सर्विस ₹20 लाख)।",
                subsidyPercent = 35,
                maxLoanAmount = 5000000.0,
                eligibilityCriteria = "Age 18+, 8th pass for projects above ₹10L in manufacturing / ₹5L in service. Higher subsidy for SC/ST/Women/OBC.",
                eligibilityCriteriaHi = "आयु 18+ वर्ष। महिलाओं, SC/ST, OBC व ग्रामीण उद्यमियों को विशेष 35% सब्सिडी छूट।",
                documentsRequired = "Project Profile Report, Caste/Category Certificate, Aadhaar, Bank Details, Education proof.",
                category = SchemeCategory.MINORITY_SC_ST,
                deadline = "Ongoing FY 2026-27",
                eligibleProfessions = "KIRANA,TAILORING,HANDICRAFTS,FOOD_STALL,OTHER"
            ),
            Scheme(
                id = "pm_svanidhi",
                name = "PM SVANidhi (Street Vendor AtmaNirbhar)",
                nameHi = "पीएम स्वनिधि योजना",
                ministry = "Ministry of Housing and Urban Affairs",
                description = "Micro working capital collateral-free credit starting at ₹10,000 graduating to ₹20,000 and ₹50,000 on timely digital repayment.",
                descriptionHi = "रेहड़ी-पटरी, ठेला, फल-सब्जी व फुटपाथ विक्रेताओं के लिए ₹10,000 से ₹50,000 तक आसान ब्याज सब्सिडी लोन।",
                subsidyPercent = 7, // 7% interest subsidy directly deposited
                maxLoanAmount = 50000.0,
                eligibilityCriteria = "Street vendors, cart operators, weekly haat sellers with vending certificate or recommendation letter.",
                eligibilityCriteriaHi = "शहरी व अर्ध-शहरी रेहड़ी-पटरी व हाट बाजार विक्रेता।",
                documentsRequired = "Aadhaar Card, Mobile linked Bank Account, Vending ID / LOR.",
                category = SchemeCategory.STREET_VENDORS,
                deadline = "Active in all districts",
                eligibleProfessions = "STREET_VENDOR,FOOD_STALL,OTHER"
            ),
            Scheme(
                id = "stand_up_india",
                name = "Stand-Up India Scheme",
                nameHi = "स्टैंड-अप इंडिया योजना",
                ministry = "Ministry of Social Justice & Finance",
                description = "Bank loans between ₹10 Lakh and ₹1 Crore to at least one SC/ST and one Woman borrower per bank branch for greenfield enterprises.",
                descriptionHi = "अनुसूचित जाति (SC), जनजाति (ST) और महिला उद्यमियों को नया उद्यम लगाने हेतु ₹10 लाख से ₹1 करोड़ तक बैंक लोन।",
                subsidyPercent = 25,
                maxLoanAmount = 10000000.0,
                eligibilityCriteria = "SC/ST and/or Woman entrepreneurs above 18 years for manufacturing, services, agri-allied or trading sectors.",
                eligibilityCriteriaHi = "SC/ST वर्ग या महिला उद्यमी, न्यूनतम 51% हिस्सेदारी।",
                documentsRequired = "Identity proof, SC/ST Certificate, Project report, Balance sheet / Khata history.",
                category = SchemeCategory.MINORITY_SC_ST,
                deadline = "Active till 2027",
                eligibleProfessions = "TAILORING,HANDICRAFTS,FOOD_STALL,KIRANA,DAIRY_FARMING,AGRICULTURE,OTHER"
            ),
            Scheme(
                id = "nrlm_shg",
                name = "Deendayal Antyodaya - NRLM / Aajeevika",
                nameHi = "दीनदयाल अंत्योदय योजना - राष्ट्रीय ग्रामीण आजीविका मिशन",
                ministry = "Ministry of Rural Development",
                description = "Low-interest revolving fund and Community Investment Fund for Women Self-Help Groups (SHGs) to scale micro enterprises.",
                descriptionHi = "महिला स्वयं सहायता समूहों को कम ब्याज पर रिवाल्विंग फंड और माइक्रो-एंटरप्राइज आजीविका सहायता।",
                subsidyPercent = 30,
                maxLoanAmount = 600000.0,
                eligibilityCriteria = "Registered Rural SHG with regular Panchasutra (weekly meetings, savings, internal lending, timely repayment, book-keeping).",
                eligibilityCriteriaHi = "पंचसूत्र का पालन करने वाले ग्रामीण महिला स्वयं सहायता समूह।",
                documentsRequired = "SHG Resolution, Bank Passbook, Member List, SahayakAI Group Register.",
                category = SchemeCategory.WOMEN_EMPOWERMENT,
                deadline = "Continuous Community Program",
                eligibleProfessions = "TAILORING,HANDICRAFTS,DAIRY_FARMING,KIRANA,OTHER"
            ),
            Scheme(
                id = "pm_kisan_cc",
                name = "Kisan Credit Card (KCC) & Animal Husbandry",
                nameHi = "किसान क्रेडिट कार्ड (KCC) व पशुपालन",
                ministry = "Ministry of Agriculture & Farmers Welfare",
                description = "Subsidized interest credit up to ₹3 Lakh for crop cultivation, dairy farming, livestock feed, and dairy equipment.",
                descriptionHi = "फसल बुवाई, डेयरी पशु खरीद व चारे हेतु रियायती 4% ब्याज दर पर ₹3 लाख तक का किसान क्रेडिट कार्ड लोन।",
                subsidyPercent = 3, // 3% prompt repayment incentive
                maxLoanAmount = 300000.0,
                eligibilityCriteria = "Farmers, dairy owners, poultry/fisheries farmers possessing land records or dairy animals.",
                eligibilityCriteriaHi = "किसान व पशुपालक (डेयरी, बकरी, मत्स्य पालन)।",
                documentsRequired = "Land Records (Khatauni), Aadhaar, Bank Details, Cattle Ownership Proof.",
                category = SchemeCategory.AGRICULTURE,
                deadline = "Ongoing Year-Round",
                eligibleProfessions = "AGRICULTURE,DAIRY_FARMING"
            ),
            Scheme(
                id = "pm_vishwakarma",
                name = "PM Vishwakarma Toolkit & Loan",
                nameHi = "प्रधानमंत्री विश्वकर्मा योजना",
                ministry = "Ministry of MSME",
                description = "Collateral-free enterprise credit of ₹1 Lakh (Tier 1) and ₹2 Lakh (Tier 2) @ 5% interest + ₹15,000 modern toolkit incentive.",
                descriptionHi = "दर्जी, बुनकर, कुम्हार, लोहार व पारंपरिक कारीगरों हेतु ₹15,000 टूलकिट अनुदान व ₹3 लाख तक सस्ता लोन।",
                subsidyPercent = 8, // Interest subvention down to 5%
                maxLoanAmount = 300000.0,
                eligibilityCriteria = "Traditional artisans and craftspeople working with hands and tools (Tailors, Cobblers, Potters, Weavers).",
                eligibilityCriteriaHi = "18 पारंपरिक व्यवसायों से जुड़े कारीगर (सिलाई, हस्तशिल्प, बढ़ई आदि)।",
                documentsRequired = "Aadhaar, Skill Certificate / Trade Verification, Bank Account.",
                category = SchemeCategory.SKILL_EQUIPMENT,
                deadline = "Active in all States",
                eligibleProfessions = "TAILORING,HANDICRAFTS,OTHER"
            )
        )
        schemeDao.insertSchemes(initialSchemes)

        // Initial Reminders
        val initialReminders = listOf(
            BusinessReminder(
                title = "PM Mudra Kishor EMI Repayment",
                dueDate = "2026-09-05",
                type = ReminderType.EMI_REPAYMENT,
                amount = 2850.0,
                isCompleted = false,
                note = "Auto-debit from Bank of Baroda account. Keep balance ready."
            ),
            BusinessReminder(
                title = "Suresh Verma Udhaar Payment Follow-up",
                dueDate = "2026-09-02",
                type = ReminderType.UDHAAR_COLLECTION,
                amount = 1450.0,
                isCompleted = false,
                note = "Send friendly WhatsApp reminder from Sahayak Khata tab."
            ),
            BusinessReminder(
                title = "Mahila SHG Monthly Meeting & Savings",
                dueDate = "2026-09-08",
                type = ReminderType.SHG_MEETING,
                amount = 500.0,
                isCompleted = false,
                note = "Submit digitized Khata report to Panchayat Gram Sevak."
            )
        )
        for (r in initialReminders) {
            reminderDao.insertReminder(r)
        }

        // Initial Community Users
        val initialUsers = listOf(
            CommunityUser(
                uid = "user_current",
                username = "ramesh_grocer",
                displayName = "Ramesh Sharma",
                displayNameHi = "रमेश शर्मा",
                bio = "Kirana merchant & ration retailer in Varanasi. Modernizing shop with digital khata & UPI.",
                avatarColorIndex = 0,
                role = "Kirana Store Owner, Varanasi",
                location = "Varanasi, UP",
                followersCount = 142,
                followingCount = 48,
                postsCount = 9,
                isFollowing = false,
                isCurrentUser = true,
                verifiedBadge = true,
                businessCategory = "Retail & Grocery"
            ),
            CommunityUser(
                uid = "user_sunita",
                username = "sunita_shg",
                displayName = "Sunita Devi",
                displayNameHi = "सुनीता देवी",
                bio = "Head of 12-member Mahila Handloom SHG. Producing pure Chanderi & Banarasi silk stoles.",
                avatarColorIndex = 1,
                role = "SHG Head & Weaver, Mirzapur",
                location = "Mirzapur, UP",
                followersCount = 284,
                followingCount = 82,
                postsCount = 24,
                isFollowing = true,
                isCurrentUser = false,
                verifiedBadge = true,
                businessCategory = "Textiles & Handloom"
            ),
            CommunityUser(
                uid = "user_mohan",
                username = "mohan_kirana",
                displayName = "Mohan Lal Gupta",
                displayNameHi = "मोहन लाल गुप्ता",
                bio = "Wholesale pulses, edible oils & spices trader. Haat market supplier for 18 years.",
                avatarColorIndex = 2,
                role = "Kirana Wholesaler, Jaunpur",
                location = "Jaunpur, UP",
                followersCount = 195,
                followingCount = 64,
                postsCount = 18,
                isFollowing = true,
                isCurrentUser = false,
                verifiedBadge = true,
                businessCategory = "Wholesale & FMCG"
            ),
            CommunityUser(
                uid = "user_rajesh",
                username = "rajesh_dairy",
                displayName = "Rajesh Patel",
                displayNameHi = "राजेश पटेल",
                bio = "Dairy cooperative farmer (40 cows) & solar silage producer. Organic vermicompost supplier.",
                avatarColorIndex = 3,
                role = "Dairy Farmer & Bio-Fuel, Varanasi",
                location = "Varanasi, UP",
                followersCount = 310,
                followingCount = 110,
                postsCount = 31,
                isFollowing = false,
                isCurrentUser = false,
                verifiedBadge = true,
                businessCategory = "Dairy & Agriculture"
            ),
            CommunityUser(
                uid = "user_kavita",
                username = "kavita_pottery",
                displayName = "Kavita Sharma",
                displayNameHi = "कविता शर्मा",
                bio = "Master terracotta pottery artisan & bamboo home decor maker. PM Vishwakarma certified.",
                avatarColorIndex = 4,
                role = "Terracotta Artisan, Gorakhpur",
                location = "Gorakhpur, UP",
                followersCount = 168,
                followingCount = 52,
                postsCount = 15,
                isFollowing = false,
                isCurrentUser = false,
                verifiedBadge = true,
                businessCategory = "Handicrafts & Pottery"
            ),
            CommunityUser(
                uid = "user_arun",
                username = "arun_mandi",
                displayName = "Arun Kumar Sahu",
                displayNameHi = "अरुण कुमार साहू",
                bio = "Registered Mandi commission agent & mustard cold press expeller operator.",
                avatarColorIndex = 5,
                role = "Mandi Agent & Oil Mill, Mirzapur",
                location = "Mirzapur, UP",
                followersCount = 420,
                followingCount = 95,
                postsCount = 42,
                isFollowing = true,
                isCurrentUser = false,
                verifiedBadge = true,
                businessCategory = "Agri-Trade & Milling"
            ),
            CommunityUser(
                uid = "user_pooja",
                username = "pooja_agri",
                displayName = "Pooja Maurya",
                displayNameHi = "पूजा मौर्य",
                bio = "Mushroom spawn grower & terrace hydroponics trainer. SHG cluster coordinator.",
                avatarColorIndex = 6,
                role = "Mushroom Cultivator, Prayagraj",
                location = "Prayagraj, UP",
                followersCount = 175,
                followingCount = 49,
                postsCount = 19,
                isFollowing = false,
                isCurrentUser = false,
                verifiedBadge = true,
                businessCategory = "Organic Agri"
            )
        )
        communityDao.insertUsers(initialUsers)

        // Initial Follow Relations
        communityDao.insertFollowRelation(FollowRelation(followerUid = "user_current", followingUid = "user_sunita"))
        communityDao.insertFollowRelation(FollowRelation(followerUid = "user_current", followingUid = "user_mohan"))
        communityDao.insertFollowRelation(FollowRelation(followerUid = "user_current", followingUid = "user_arun"))

        // Initial Community Posts (Photo Posts and Question Posts)
        val initialPosts = listOf(
            CommunityPost(
                id = 1,
                postId = "post_sunita_1",
                userId = "user_sunita",
                authorName = "Sunita Devi",
                authorUsername = "sunita_shg",
                authorAvatarIndex = 1,
                authorRole = "Sewing Center & SHG Head, Mirzapur",
                type = CommunityPostType.PHOTO,
                caption = "हमारे महिला समूह ने SahayakAI के खाता रिकॉर्ड से बैंक मैनेजर को ₹80,000 का टर्नओवर दिखाया और हमें बिना किसी बिचौलिए के 4 नई सिलाई मशीनों के लिए मुद्रा लोन (₹1.8 लाख) मिल गया! 🙏🇮🇳 सभी बहनें अब दोगुनी कमाई कर रही हैं।",
                mediaUrl = "photo_sewing_machines",
                tag = "#MudraSuccess",
                category = "Government Schemes",
                likesCount = 38,
                commentsCount = 3,
                isLikedByUser = true,
                voiceNoteSeconds = 42,
                createdAtFormatted = "3 hours ago"
            ),
            CommunityPost(
                id = 2,
                postId = "post_mohan_1",
                userId = "user_mohan",
                authorName = "Mohan Lal Gupta",
                authorUsername = "mohan_kirana",
                authorAvatarIndex = 2,
                authorRole = "Kirana Wholesaler, Jaunpur",
                type = CommunityPostType.QUESTION,
                caption = "साथी दुकानदारों से सवाल: गांव में पुराने ग्राहकों का ₹2000-₹5000 का उधार अटका हुआ है। क्या SahayakAI का विनम्र व्हाट्सएप तकादा संदेश भेजने से रिश्ते खराब हुए बिना पैसा वापस मिलता है? आपका क्या अनुभव है?",
                mediaUrl = null,
                tag = "#UdhaarRecovery",
                category = "Khata Management",
                likesCount = 24,
                commentsCount = 2,
                isLikedByUser = false,
                voiceNoteSeconds = null,
                createdAtFormatted = "Yesterday"
            ),
            CommunityPost(
                id = 3,
                postId = "post_rajesh_1",
                userId = "user_rajesh",
                authorName = "Rajesh Patel",
                authorUsername = "rajesh_dairy",
                authorAvatarIndex = 3,
                authorRole = "Dairy Farm & Bio-Fuel, Varanasi",
                type = CommunityPostType.PHOTO,
                caption = "मंडी में दूध के दाम स्थिर हैं लेकिन चारे की कीमत बढ़ रही थी। मैंने साइलेज चारा बनाना शुरू किया जिससे 15% लागत कम हुई और दूध का फैट भी 4.2 से बढ़कर 4.8 हो गया। सहकारी समिति से ₹2/लीटर बोनस मिला!",
                mediaUrl = "photo_dairy_silage",
                tag = "#DairyTips",
                category = "Cost Reduction",
                likesCount = 45,
                commentsCount = 2,
                isLikedByUser = true,
                voiceNoteSeconds = 28,
                createdAtFormatted = "2 days ago"
            ),
            CommunityPost(
                id = 4,
                postId = "post_pooja_1",
                userId = "user_pooja",
                authorName = "Pooja Maurya",
                authorUsername = "pooja_agri",
                authorAvatarIndex = 6,
                authorRole = "Mushroom Cultivator, Prayagraj",
                type = CommunityPostType.QUESTION,
                caption = "क्या किसी साथी ने PM-KUSUM घटक-A के तहत 5HP सोलर पंप लगवाया है? ब्लॉक स्तर पर क्या खतौनी और बिजली एनओसी के अलावा कोई अन्य दस्तावेज मांगा जाता है? सब्सिडी आने में कितने दिन लगे?",
                mediaUrl = null,
                tag = "#SolarPump",
                category = "Agri Schemes",
                likesCount = 31,
                commentsCount = 2,
                isLikedByUser = false,
                voiceNoteSeconds = null,
                createdAtFormatted = "3 days ago"
            ),
            CommunityPost(
                id = 5,
                postId = "post_kavita_1",
                userId = "user_kavita",
                authorName = "Kavita Sharma",
                authorUsername = "kavita_pottery",
                authorAvatarIndex = 4,
                authorRole = "Terracotta Artisan, Gorakhpur",
                type = CommunityPostType.PHOTO,
                caption = "दीपावली और शादी के सीजन के लिए 500 मिट्टी के सजावटी दीये और वाटर बॉटल्स तैयार हैं! PM विश्वकर्मा योजना के ₹15,000 टूलकिट अनुदान से हमने इलेक्ट्रिक पॉटरी व्हील लिया है। गति 3 गुना बढ़ गई।",
                mediaUrl = "photo_terracotta_craft",
                tag = "#PMVishwakarma",
                category = "Artisan Success",
                likesCount = 52,
                commentsCount = 1,
                isLikedByUser = false,
                voiceNoteSeconds = null,
                createdAtFormatted = "4 days ago"
            ),
            CommunityPost(
                id = 6,
                postId = "post_arun_1",
                userId = "user_arun",
                authorName = "Arun Kumar Sahu",
                authorUsername = "arun_mandi",
                authorAvatarIndex = 5,
                authorRole = "Mandi Agent & Oil Mill, Mirzapur",
                type = CommunityPostType.PHOTO,
                caption = "मिर्जापुर मंडी में नई काली सरसों की आवक शुरू! तेल रिकवरी 38.5% आ रही है। जो दुकानदार थोक में कच्ची घानी तेल का टीन लेना चाहते हैं, सीधे DM में संपर्क कर सकते हैं।",
                mediaUrl = "photo_mustard_oil",
                tag = "#MandiTrade",
                category = "Wholesale Deals",
                likesCount = 29,
                commentsCount = 1,
                isLikedByUser = false,
                voiceNoteSeconds = null,
                createdAtFormatted = "5 days ago"
            )
        )
        communityDao.insertPosts(initialPosts)

        // Initial Comments
        val initialComments = listOf(
            PostComment(
                postId = 1,
                userId = "user_mohan",
                userName = "Mohan Lal Gupta",
                userUsername = "mohan_kirana",
                userAvatarIndex = 2,
                userRole = "Kirana Wholesaler, Jaunpur",
                content = "बहुत बहुत बधाई सुनीता जी! बैंक वाले अब ऐप का डिजिटल खाता स्टेटमेंट देखकर तुरंत विश्वास कर लेते हैं।",
                createdAtFormatted = "2 hours ago"
            ),
            PostComment(
                postId = 1,
                userId = "user_kavita",
                userName = "Kavita Sharma",
                userUsername = "kavita_pottery",
                userAvatarIndex = 4,
                userRole = "Terracotta Artisan, Gorakhpur",
                content = "शानदार! क्या इसके लिए प्रोजेक्ट रिपोर्ट भी जमा करनी पड़ी थी?",
                createdAtFormatted = "1 hour ago"
            ),
            PostComment(
                postId = 1,
                userId = "user_sunita",
                userName = "Sunita Devi",
                userUsername = "sunita_shg",
                userAvatarIndex = 1,
                userRole = "Sewing Center & SHG Head, Mirzapur",
                content = "@kavita_pottery जी हाँ, हमने SahayakAI से उत्पन्न फाइनेंशियल रिपोर्ट ही बैंक को दी थी, 3 दिन में पास हो गया।",
                createdAtFormatted = "45 mins ago"
            ),
            PostComment(
                postId = 2,
                userId = "user_current",
                userName = "Ramesh Sharma",
                userUsername = "ramesh_grocer",
                userAvatarIndex = 0,
                userRole = "Kirana Store, Varanasi",
                content = "मोहन भाई, मैंने पिछले हफ्ते 8 ग्राहकों को व्हाट्सएप रिमाइंडर भेजा। 6 लोगों ने उसी दिन UPI से पूरा भुगतान कर दिया। संदेश बहुत आदरपूर्वक जाता है तो कोई बुरा नहीं मानता।",
                createdAtFormatted = "18 hours ago"
            ),
            PostComment(
                postId = 2,
                userId = "user_arun",
                userName = "Arun Kumar Sahu",
                userUsername = "arun_mandi",
                userAvatarIndex = 5,
                userRole = "Mandi Agent, Mirzapur",
                content = "बिलकुल सही! जब तारीख और पर्चे की फोटो साथ जाती है तो कोई बहाना भी नहीं बनता।",
                createdAtFormatted = "12 hours ago"
            ),
            PostComment(
                postId = 3,
                userId = "user_pooja",
                userName = "Pooja Maurya",
                userUsername = "pooja_agri",
                userAvatarIndex = 6,
                userRole = "Mushroom Cultivator, Prayagraj",
                content = "राजेश जी, क्या आप साइलेज बनाने की विधि पर एक छोटा ऑडियो या पोस्ट साझा करेंगे?",
                createdAtFormatted = "1 day ago"
            ),
            PostComment(
                postId = 3,
                userId = "user_rajesh",
                userName = "Rajesh Patel",
                userUsername = "rajesh_dairy",
                userAvatarIndex = 3,
                userRole = "Dairy Farmer, Varanasi",
                content = "हाँ पूजा जी, मक्के की कटाई के बाद गड्ढे में गुड़ और नमक मिलाकर 45 दिन ढककर रखते हैं। कल विस्तार से लिखूंगा।",
                createdAtFormatted = "20 hours ago"
            ),
            PostComment(
                postId = 4,
                userId = "user_rajesh",
                userName = "Rajesh Patel",
                userUsername = "rajesh_dairy",
                userAvatarIndex = 3,
                userRole = "Dairy Farmer, Varanasi",
                content = "पूजा जी, जमीन की खतौनी और बैंक पासबुक मुख्य हैं। कृषि विभाग के पोर्टल पर ऑनलाइन आवेदन के बाद 35-40 दिन में सोलर पंप लग जाता है। 60% सब्सिडी सीधे कटकर बिल बनता है।",
                createdAtFormatted = "2 days ago"
            ),
            PostComment(
                postId = 4,
                userId = "user_pooja",
                userName = "Pooja Maurya",
                userUsername = "pooja_agri",
                userAvatarIndex = 6,
                userRole = "Mushroom Cultivator, Prayagraj",
                content = "बहुत बहुत धन्यवाद राजेश जी, मैं कल ही ऑनलाइन फॉर्म भरती हूँ।",
                createdAtFormatted = "1 day ago"
            ),
            PostComment(
                postId = 5,
                userId = "user_sunita",
                userName = "Sunita Devi",
                userUsername = "sunita_shg",
                userAvatarIndex = 1,
                userRole = "Sewing Center & SHG Head, Mirzapur",
                content = "बहुत सुंदर कलाकृति कविता जी! हमारे स्वयं सहायता समूह को भी 50 दीये चाहिए।",
                createdAtFormatted = "3 days ago"
            ),
            PostComment(
                postId = 6,
                userId = "user_current",
                userName = "Ramesh Sharma",
                userUsername = "ramesh_grocer",
                userAvatarIndex = 0,
                userRole = "Kirana Store, Varanasi",
                content = "अरुण जी, मैंने आपको DM किया है, 4 टीन का रेट बता दीजिए।",
                createdAtFormatted = "4 days ago"
            )
        )
        communityDao.insertComments(initialComments)

        // Initial Direct Conversations & Messages (Deterministic conversationId format: sorted(uid1, uid2).joinToString("_"))
        val convIdArun = listOf("user_current", "user_arun").sorted().joinToString("_")
        val convIdSunita = listOf("user_current", "user_sunita").sorted().joinToString("_")
        val convIdMohan = listOf("user_current", "user_mohan").sorted().joinToString("_")

        val initialConversations = listOf(
            DirectConversation(
                conversationId = convIdArun,
                participant1Uid = "user_arun",
                participant2Uid = "user_current",
                otherUserId = "user_arun",
                otherUserName = "Arun Kumar Sahu",
                otherUserUsername = "arun_mandi",
                otherUserAvatarIndex = 5,
                otherUserRole = "Mandi Agent & Oil Mill, Mirzapur",
                lastMessage = "नमस्ते रमेश भाई! 15 लीटर सरसों तेल टीन ₹2,150 में कल सुबह की गाड़ी से भेज दूंगा।",
                lastMessageAtFormatted = "10:30 AM",
                lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                unreadCount = 1,
                isOnline = true
            ),
            DirectConversation(
                conversationId = convIdSunita,
                participant1Uid = "user_current",
                participant2Uid = "user_sunita",
                otherUserId = "user_sunita",
                otherUserName = "Sunita Devi",
                otherUserUsername = "sunita_shg",
                otherUserAvatarIndex = 1,
                otherUserRole = "SHG Head & Weaver, Mirzapur",
                lastMessage = "रमेश जी, क्या आप अपनी दुकान पर हमारे समूह के सूती गमछे और थैले रख सकते हैं?",
                lastMessageAtFormatted = "Yesterday",
                lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 26,
                unreadCount = 0,
                isOnline = false
            ),
            DirectConversation(
                conversationId = convIdMohan,
                participant1Uid = "user_current",
                participant2Uid = "user_mohan",
                otherUserId = "user_mohan",
                otherUserName = "Mohan Lal Gupta",
                otherUserUsername = "mohan_kirana",
                otherUserAvatarIndex = 2,
                otherUserRole = "Kirana Wholesaler, Jaunpur",
                lastMessage = "चना दाल का भाव ₹78/kg हो गया है, 50kg कट्टा चाहिए तो बताएं।",
                lastMessageAtFormatted = "2 days ago",
                lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 50,
                unreadCount = 0,
                isOnline = true
            )
        )
        communityDao.insertConversations(initialConversations)

        val initialMessages = listOf(
            DirectMessage(
                conversationId = convIdArun,
                senderId = "user_current",
                senderName = "Ramesh Sharma",
                senderAvatarIndex = 0,
                content = "नमस्ते अरुण जी, मुझे दुकान के लिए 4 टीन शुद्ध सरसों तेल चाहिए। आज क्या थोक भाव है?",
                timestampFormatted = "10:15 AM",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 75,
                isRead = true,
                isFromCurrentUser = true
            ),
            DirectMessage(
                conversationId = convIdArun,
                senderId = "user_arun",
                senderName = "Arun Kumar Sahu",
                senderAvatarIndex = 5,
                content = "नमस्ते रमेश भाई! 15 लीटर सरसों तेल टीन ₹2,150 में कल सुबह की गाड़ी से भेज दूंगा। गुणवत्ता 100% शुद्ध घानी की गारंटी है।",
                timestampFormatted = "10:30 AM",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                isRead = false,
                isFromCurrentUser = false
            ),
            DirectMessage(
                conversationId = convIdSunita,
                senderId = "user_sunita",
                senderName = "Sunita Devi",
                senderAvatarIndex = 1,
                content = "रमेश जी, क्या आप अपनी दुकान पर हमारे समूह के सूती गमछे और थैले रख सकते हैं? बिक्री पर 20% कमीशन रहेगा।",
                timestampFormatted = "Yesterday 4:20 PM",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 26,
                isRead = true,
                isFromCurrentUser = false
            ),
            DirectMessage(
                conversationId = convIdSunita,
                senderId = "user_current",
                senderName = "Ramesh Sharma",
                senderAvatarIndex = 0,
                content = "जरूर सुनीता बहन, 20 पीस भिजवा दीजिए। प्लास्टिक बैन के बाद कपड़े के थैलों की अच्छी मांग है।",
                timestampFormatted = "Yesterday 5:00 PM",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 25,
                isRead = true,
                isFromCurrentUser = true
            )
        )
        communityDao.insertMessages(initialMessages)

        // Initial Scanned Khatas & Entries (Seed realistic records for demo)
        val initialKhatas = listOf(
            ScannedKhata(
                id = 1,
                label = "March 2026 — Grocery Shop Khata",
                description = "Daily customer ration chits, wholesale mustard oil restock, and counter UPI bikri.",
                imagePresetId = "preset_kirana_march_2026",
                source = KhataSource.PRESET,
                status = KhataStatus.CONFIRMED,
                entryCount = 6,
                totalCredit = 3780.0,
                totalDebit = 4470.0,
                isParsedOffline = false
            ),
            ScannedKhata(
                id = 2,
                label = "February 2026 — SHG Handicrafts Ledger",
                description = "Artisan group raw material procurement, weekly thrift savings, and exhibition sales.",
                imagePresetId = "preset_shg_handicrafts",
                source = KhataSource.PRESET,
                status = KhataStatus.CONFIRMED,
                entryCount = 5,
                totalCredit = 6600.0,
                totalDebit = 2330.0,
                isParsedOffline = false
            )
        )
        for (khata in initialKhatas) {
            khataDao.insertKhata(khata)
        }

        val initialKhataEntries = listOf(
            ScannedKhataEntry(
                khataId = 1,
                date = "2026-03-01",
                partyName = "Suresh Verma",
                description = "10kg Aata, Dal, Mustard Oil",
                amount = 1450.0,
                type = LedgerType.DEBIT,
                category = LedgerCategory.CUSTOMER_UDHAAR,
                rawText = "01/03 Suresh Verma (Teacher) - 10kg Aata, Dal, Mustard Oil Rs 1450 udhaar",
                confidence = 0.96f
            ),
            ScannedKhataEntry(
                khataId = 1,
                date = "2026-03-01",
                partyName = "Daily Cash Counter",
                description = "Counter UPI QR & cash sales",
                amount = 3280.0,
                type = LedgerType.CREDIT,
                category = LedgerCategory.SALES,
                rawText = "01/03 Daily Cash Counter & UPI QR Bikri ₹3280 jama",
                confidence = 0.98f
            ),
            ScannedKhataEntry(
                khataId = 1,
                date = "2026-03-01",
                partyName = "Kashi Wholesale Mandi",
                description = "2 Mustard Oil Tins & Sugar",
                amount = 2600.0,
                type = LedgerType.DEBIT,
                category = LedgerCategory.INVENTORY_BUY,
                rawText = "01/03 Kashi Wholesale Mandi - 2 Mustard Oil Tins & Sugar ₹2600 kharch",
                confidence = 0.92f
            ),
            ScannedKhataEntry(
                khataId = 1,
                date = "2026-03-01",
                partyName = "Bablu Chaiwala",
                description = "Morning Milk & Curd 5L",
                amount = 240.0,
                type = LedgerType.DEBIT,
                category = LedgerCategory.CUSTOMER_UDHAAR,
                rawText = "01/03 Bablu Chaiwala - Morning Milk & Curd 5L Rs 240 udhaar",
                confidence = 0.89f
            ),
            ScannedKhataEntry(
                khataId = 1,
                date = "2026-03-01",
                partyName = "Sunita Devi (SHG)",
                description = "Handloom threads payment",
                amount = 500.0,
                type = LedgerType.CREDIT,
                category = LedgerCategory.SALES,
                rawText = "01/03 Sunita Devi (SHG) - Handloom threads payment ₹500 jama",
                confidence = 0.97f
            ),
            ScannedKhataEntry(
                khataId = 1,
                date = "2026-03-01",
                partyName = "Electricity Board",
                description = "Shop meter payment",
                amount = 420.0,
                type = LedgerType.DEBIT,
                category = LedgerCategory.RENT_UTILITIES,
                rawText = "01/03 Shop Electricity Bill Meter Payment ₹420 kharch",
                confidence = 0.94f
            ),
            // Khata 2 entries
            ScannedKhataEntry(
                khataId = 2,
                date = "2026-02-28",
                partyName = "SHG Mahila Samiti",
                description = "Weekly thrift savings collection",
                amount = 1200.0,
                type = LedgerType.CREDIT,
                category = LedgerCategory.SHG_SAVINGS,
                rawText = "28/02 Weekly SHG Mahila Thrift Savings Collection ₹1200 jama",
                confidence = 0.98f
            ),
            ScannedKhataEntry(
                khataId = 2,
                date = "2026-02-28",
                partyName = "Varanasi Zari Vendor",
                description = "Raw threads & dyes",
                amount = 1850.0,
                type = LedgerType.DEBIT,
                category = LedgerCategory.INVENTORY_BUY,
                rawText = "28/02 Varanasi Zari Raw Threads & Chemical Dyes ₹1850 kharch",
                confidence = 0.93f
            ),
            ScannedKhataEntry(
                khataId = 2,
                date = "2026-02-28",
                partyName = "Delhi Buyer",
                description = "Handicraft sarees export delivery",
                amount = 5400.0,
                type = LedgerType.CREDIT,
                category = LedgerCategory.SALES,
                rawText = "28/02 Handicraft Embroidered Sarees Sold to Delhi Buyer ₹5400 jama",
                confidence = 0.97f
            ),
            ScannedKhataEntry(
                khataId = 2,
                date = "2026-02-28",
                partyName = "Community Hall",
                description = "Exhibition space fee",
                amount = 300.0,
                type = LedgerType.DEBIT,
                category = LedgerCategory.RENT_UTILITIES,
                rawText = "28/02 Community Hall Space Contribution ₹300 kharch",
                confidence = 0.90f
            ),
            ScannedKhataEntry(
                khataId = 2,
                date = "2026-02-28",
                partyName = "Meena Devi",
                description = "Sewing needle set",
                amount = 180.0,
                type = LedgerType.DEBIT,
                category = LedgerCategory.INVENTORY_BUY,
                rawText = "28/02 Meena Devi - Sewing machine needle set ₹180 kharch",
                confidence = 0.92f
            )
        )
        khataDao.insertKhataEntries(initialKhataEntries)
    }

    private fun getInitialMandiPrices(): List<MandiCommodity> {
        return listOf(
            MandiCommodity(
                id = "cmd_wheat",
                name = "Wheat (गेहूं)",
                nameHi = "गेहूं (Sharbati/Dara)",
                marketLocation = "Varanasi Mandi, UP",
                pricePerUnit = 2480.0,
                unit = "Quintal",
                priceChangePercent = +3.2,
                trend = "UP",
                advisoryNote = "Demand rising ahead of festive season. Recommended holding stock for 1-2 weeks.",
                advisoryNoteHi = "त्योहारी मांग से दाम बढ़ रहे हैं। 1-2 हफ्ते स्टॉक रखने पर बेहतर भाव मिल सकता है।"
            ),
            MandiCommodity(
                id = "cmd_mustard",
                name = "Mustard (सरसों)",
                nameHi = "सरसों / राई",
                marketLocation = "Agra Mandi, UP",
                pricePerUnit = 5650.0,
                unit = "Quintal",
                priceChangePercent = +1.5,
                trend = "UP",
                advisoryNote = "High oil mill crushing demand.",
                advisoryNoteHi = "तेल मिलों की मजबूत खरीद जारी है।"
            ),
            MandiCommodity(
                id = "cmd_tomato",
                name = "Tomato (टमाटर)",
                nameHi = "देशी टमाटर",
                marketLocation = "Mirzapur Mandi, UP",
                pricePerUnit = 24.0,
                unit = "Kg",
                priceChangePercent = -4.5,
                trend = "DOWN",
                advisoryNote = "Local fresh harvest arrival increasing. Sell within 24 hours to avoid spoilage.",
                advisoryNoteHi = "नई आवक बढ़ने से भाव में नरमी। खराब होने से बचाने के लिए जल्द बिक्री करें।"
            ),
            MandiCommodity(
                id = "cmd_potato",
                name = "Potato (आलू)",
                nameHi = "चिप्सोना व लाल आलू",
                marketLocation = "Kanpur Mandi, UP",
                pricePerUnit = 18.5,
                unit = "Kg",
                priceChangePercent = 0.0,
                trend = "STABLE",
                advisoryNote = "Cold storage release steady. Prices expected to remain range-bound.",
                advisoryNoteHi = "कोल्ड स्टोरेज से आपूर्ति स्थिर है, भाव सामान्य रहेंगे।"
            ),
            MandiCommodity(
                id = "cmd_milk",
                name = "Cow Milk (गाय का दूध)",
                nameHi = "ताज़ा गाय का दूध",
                marketLocation = "Local Cooperative",
                pricePerUnit = 48.0,
                unit = "Litre",
                priceChangePercent = +2.1,
                trend = "UP",
                advisoryNote = "Cooperative bonus active for SNF >= 8.5.",
                advisoryNoteHi = "फैट व SNF गुणवत्ता पर ₹2/लीटर अतिरिक्त प्रोत्साहन।"
            )
        )
    }
}
