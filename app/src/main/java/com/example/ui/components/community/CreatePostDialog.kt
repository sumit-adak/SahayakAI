package com.example.ui.components.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppLanguage
import com.example.data.model.CommunityPostType
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel

@Composable
fun CreatePostDialog(
    viewModel: SahayakViewModel,
    onDismiss: () -> Unit
) {
    val moderationNotice by viewModel.moderationNotice.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isHindi = userProfile.preferredLanguage == AppLanguage.HINDI

    var selectedType by remember { mutableStateOf(CommunityPostType.PHOTO) }
    var captionText by remember { mutableStateOf("") }
    var topicTag by remember { mutableStateOf("#KiranaProfit") }
    var selectedCategory by remember { mutableStateOf("Khata Management") }
    var selectedPhotoPreset by remember { mutableStateOf("photo_shop_counter") }

    val categories = listOf(
        "Khata Management",
        "Government Schemes",
        "Cost Reduction",
        "Agri Schemes",
        "Artisan Success",
        "Wholesale Deals"
    )

    val photoPresets = listOf(
        Triple("photo_shop_counter", if (isHindi) "दुकान काउंटर व डिजिटल UPI" else "Shop Counter & UPI QR", Icons.Filled.Storefront),
        Triple("photo_sewing_machines", if (isHindi) "सिलाई मशीन व वर्कशॉप" else "Sewing Machines & SHG", Icons.Filled.Engineering),
        Triple("photo_dairy_silage", if (isHindi) "डेयरी फार्म व चारा साइलो" else "Dairy & Fodder Silo", Icons.Filled.Agriculture),
        Triple("photo_terracotta_craft", if (isHindi) "हस्तशिल्प व मिट्टी बर्तन" else "Handicrafts & Terracotta", Icons.Filled.Brush),
        Triple("photo_mustard_oil", if (isHindi) "सरसों तेल मिल व स्टॉक" else "Oil Mill & Wholesale Stock", Icons.Filled.Opacity)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(4.dp),
            shape = RoundedCornerShape(20.dp),
            color = PureWhite
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "नया अनुभव या सवाल साझा करें" else "Create Community Post",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Slate900
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Post Type Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate100, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedType = CommunityPostType.PHOTO
                                if (topicTag == "#AskPeers") topicTag = "#KiranaProfit"
                            },
                        color = if (selectedType == CommunityPostType.PHOTO) PureWhite else Color.Transparent,
                        shadowElevation = if (selectedType == CommunityPostType.PHOTO) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = if (selectedType == CommunityPostType.PHOTO) Emerald800 else Slate500,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "फोटो व अनुभव" else "Photo Post",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedType == CommunityPostType.PHOTO) Emerald900 else Slate600
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedType = CommunityPostType.QUESTION
                                topicTag = "#AskPeers"
                            },
                        color = if (selectedType == CommunityPostType.QUESTION) PureWhite else Color.Transparent,
                        shadowElevation = if (selectedType == CommunityPostType.QUESTION) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = if (selectedType == CommunityPostType.QUESTION) Amber800 else Slate500,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "सवाल पूछें" else "Ask Peers",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedType == CommunityPostType.QUESTION) Amber900 else Slate600
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Moderation warning if triggered
                ModerationNoticeBanner(
                    notice = moderationNotice,
                    onDismiss = { viewModel.clearModerationNotice() }
                )

                // Photo Selector if Photo Post
                if (selectedType == CommunityPostType.PHOTO) {
                    Text(
                        text = if (isHindi) "फ़ोटो संलग्न करें (प्रमाण)" else "Attach Business Photo (Proof)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        photoPresets.forEach { (presetId, label, icon) ->
                            val isSelected = selectedPhotoPreset == presetId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedPhotoPreset = presetId },
                                color = if (isSelected) Emerald50 else Slate50,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Emerald600 else Slate200
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Emerald800 else Slate500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Emerald900 else Slate700,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Emerald700,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Caption / Question Text
                Text(
                    text = if (selectedType == CommunityPostType.QUESTION) {
                        if (isHindi) "आपका सवाल या मार्गदर्शन का विषय:" else "Your Question for Peers:"
                    } else {
                        if (isHindi) "अपना अनुभव व सीख लिखें:" else "Post Description & Lessons:"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    placeholder = {
                        Text(
                            text = if (selectedType == CommunityPostType.QUESTION) {
                                if (isHindi) "उदाहरण: क्या ब्लॉक ऑफिस में मुद्रा लोन के लिए गारंटी मांगी जाती है? आपका क्या अनुभव रहा..." else "E.g., What is the practical turnaround for KUSUM solar pump subsidy approval in your district?"
                            } else {
                                if (isHindi) "उदाहरण: हमने थोक मंडी से सीधे खरीदारी करके 12% बचत की, और SahayakAI से खाता प्रबंधित किया..." else "Share practical metrics, loan approvals, or cost savings that helped your rural business..."
                            },
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("create_post_caption_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedBorderColor = Emerald700,
                        unfocusedBorderColor = Slate400,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = Slate50
                    ),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Topic Tag & Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = topicTag,
                        onValueChange = { topicTag = it },
                        label = { Text("Topic Tag", color = Slate800) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_post_tag_input"),
                        shape = RoundedCornerShape(10.dp),
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

                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = { selectedCategory = it },
                        label = { Text("Category", color = Slate800) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_post_category_input"),
                        shape = RoundedCornerShape(10.dp),
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Publish Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (captionText.isNotBlank()) {
                                val success = viewModel.createCommunityPost(
                                    type = selectedType,
                                    caption = captionText,
                                    tag = topicTag,
                                    category = selectedCategory,
                                    mediaUrl = if (selectedType == CommunityPostType.PHOTO) selectedPhotoPreset else null
                                )
                                if (success) {
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("publish_post_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "प्रकाशित करें" else "Publish",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
