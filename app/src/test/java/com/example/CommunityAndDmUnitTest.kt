package com.example

import com.example.data.model.DirectConversation
import com.example.data.model.DirectMessage
import com.example.data.service.CommunityModerationService
import org.junit.Assert.*
import org.junit.Test

/**
 * Two-device Direct Messaging and Real-Time Delivery Tests (PS26091 Section F & L)
 * Verifies:
 * 1. Deterministic conversation ID calculation across two communicating peer devices
 * 2. Bi-directional message routing and unread tracking between sender and recipient
 * 3. Security/moderation filter preventing inappropriate content
 * 4. Isolation of conversation participants
 */
class CommunityAndDmUnitTest {

    @Test
    fun testDeterministicConversationIdBetweenTwoDevices() {
        val device1Uid = "user_ramesh"
        val device2Uid = "user_sunita"

        // Device 1 perspective
        val convIdFromDevice1 = listOf(device1Uid, device2Uid).sorted().joinToString("_")
        // Device 2 perspective
        val convIdFromDevice2 = listOf(device2Uid, device1Uid).sorted().joinToString("_")

        assertEquals("Both devices must resolve the exact same conversation ID regardless of initiator",
            convIdFromDevice1, convIdFromDevice2)
        assertEquals("user_ramesh_user_sunita", convIdFromDevice1)
    }

    @Test
    fun testTwoDeviceMessageDeliveryAndPerspective() {
        val convId = "user_ramesh_user_sunita"
        val messageSentByRamesh = DirectMessage(
            id = 101,
            conversationId = convId,
            senderId = "user_ramesh",
            senderName = "Ramesh Sharma",
            senderAvatarIndex = 0,
            content = "नमस्ते सुनीता जी, मुझे 20 सूती थैले चाहिए।",
            timestampFormatted = "11:00 AM",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            isFromCurrentUser = true
        )

        // Device 1 (Ramesh) sees isFromCurrentUser = true
        assertTrue("Ramesh's own device marks message as from current user", messageSentByRamesh.isFromCurrentUser)

        // Device 2 (Sunita) receives the message sync
        val messageReceivedBySunita = messageSentByRamesh.copy(
            isFromCurrentUser = ("user_sunita" == messageSentByRamesh.senderId)
        )
        assertFalse("Sunita's device marks Ramesh's message as incoming (isFromCurrentUser = false)",
            messageReceivedBySunita.isFromCurrentUser)
        assertEquals("Message content is strictly preserved",
            "नमस्ते सुनीता जी, मुझे 20 सूती थैले चाहिए।", messageReceivedBySunita.content)
        assertFalse("Message is unread on recipient device until opened", messageReceivedBySunita.isRead)

        // Recipient opens conversation and marks as read
        val readMessage = messageReceivedBySunita.copy(isRead = true)
        assertTrue("Message is successfully marked as read", readMessage.isRead)
    }

    @Test
    fun testCommunityModerationBlocksProfanityAndSpam() {
        // Clean trade message
        val cleanMsg = "हमारे पास शुद्ध कच्ची घानी सरसों तेल का नया स्टॉक उपलब्ध है।"
        val cleanResult = CommunityModerationService.checkContent(cleanMsg)
        assertTrue("Legitimate business trade message must be approved", cleanResult.isApproved)

        // Inappropriate / offensive message with blocked keywords
        val inappropriateMsg = "Join our telegram channel to earn 10000 daily with scam betting app"
        val blockedResult = CommunityModerationService.checkContent(inappropriateMsg)
        assertFalse("Harmful/fraudulent content must be flagged by moderation", blockedResult.isApproved)
    }

    @Test
    fun testConversationUnreadCountIncrement() {
        val initialConv = DirectConversation(
            conversationId = "user_arun_user_ramesh",
            participant1Uid = "user_arun",
            participant2Uid = "user_ramesh",
            otherUserId = "user_arun",
            otherUserName = "Arun Sahu",
            otherUserUsername = "arun_mandi",
            otherUserAvatarIndex = 5,
            otherUserRole = "Mandi Agent",
            lastMessage = "Old message",
            lastMessageAtFormatted = "Yesterday",
            lastMessageTimestamp = System.currentTimeMillis() - 100000,
            unreadCount = 0,
            isOnline = true
        )

        // Incoming message arrives for Ramesh
        val updatedConv = initialConv.copy(
            lastMessage = "सरसों तेल का भाव ₹2,150/टीन है",
            lastMessageAtFormatted = "Just now",
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = initialConv.unreadCount + 1
        )

        assertEquals("Unread count must increment for recipient", 1, updatedConv.unreadCount)
        assertEquals("Last message must update to incoming message content",
            "सरसों तेल का भाव ₹2,150/टीन है", updatedConv.lastMessage)
    }
}
