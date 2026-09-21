package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.WhatsAppReminderDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataDetailScreen(
    khataId: Long,
    viewModel: SahayakViewModel,
    onBack: () -> Unit
) {
    val khataFlow = remember(khataId) { viewModel.getKhataById(khataId) }
    val entriesFlow = remember(khataId) { viewModel.getEntriesForKhata(khataId) }

    val khata by khataFlow.collectAsState(initial = null)
    val entries by entriesFlow.collectAsState(initial = emptyList())
    val userProfile by viewModel.userProfile.collectAsState()

    var selectedWhatsAppReminderEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            khata?.label ?: "Digital Khata",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Slate900,
                            maxLines = 1
                        )
                        Text(
                            "${entries.size} Digital Ledger Entries",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_khata_detail")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate800
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete Khata",
                            tint = Crimson600
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
            )
        }
    ) { paddingValues ->
        if (khata == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Emerald800)
            }
        } else {
            val currentKhata = khata!!
            val totalCredit = entries.filter { it.type == LedgerType.CREDIT }.sumOf { it.amount }
            val totalDebit = entries.filter { it.type == LedgerType.DEBIT }.sumOf { it.amount }
            val net = totalCredit - totalDebit

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate50)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
            ) {
                // Khata Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (currentKhata.status) {
                                        KhataStatus.CONFIRMED -> Emerald100
                                        KhataStatus.NEEDS_REVIEW -> Amber100
                                        KhataStatus.PROCESSING -> Blue100
                                    }
                                ) {
                                    Text(
                                        currentKhata.status.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (currentKhata.status) {
                                            KhataStatus.CONFIRMED -> Emerald900
                                            KhataStatus.NEEDS_REVIEW -> Amber900
                                            KhataStatus.PROCESSING -> Blue900
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    "Source: ${currentKhata.source.name}",
                                    fontSize = 11.sp,
                                    color = Slate600,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                currentKhata.label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Slate900
                            )

                            if (currentKhata.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    currentKhata.description,
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Totals Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate100, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Jama (Credit)", fontSize = 10.sp, color = Emerald800, fontWeight = FontWeight.Bold)
                                    Text("₹${String.format(Locale.ROOT, "%.0f", totalCredit)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Udhaar (Debit)", fontSize = 10.sp, color = Crimson800, fontWeight = FontWeight.Bold)
                                    Text("₹${String.format(Locale.ROOT, "%.0f", totalDebit)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Balance", fontSize = 10.sp, color = Amber800, fontWeight = FontWeight.Bold)
                                    Text("₹${String.format(Locale.ROOT, "%.0f", net)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                }
                            }
                        }
                    }
                }

                // Original Scanned Document Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Emerald800, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Original Scanned Document (Preserved)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFBF8F2))
                                    .border(BorderStroke(1.dp, Color(0xFFE2D7C2)), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentKhata.imageUri != null) {
                                    AsyncImage(
                                        model = currentKhata.imageUri,
                                        contentDescription = "Original Khata Photo",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.Description, contentDescription = null, tint = Color(0xFF7A6B56), modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Handwritten Physical Ledger Page",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF5A4D3B)
                                        )
                                        Text(
                                            "Digitized via layout-preserving OCR & Gemini structuring",
                                            fontSize = 10.sp,
                                            color = Color(0xFF8C7A60)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Contextual ML Market Intelligence Banner (Matched items)
                item {
                    val firstMatchedItem = entries.firstNotNullOfOrNull { entry ->
                        viewModel.matchCommodityForecast(entry.description)
                    }
                    if (firstMatchedItem != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Emerald50),
                            border = BorderStroke(1.dp, Emerald200)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Emerald800, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Mandi ML Intelligence for Khata",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Emerald900
                                    )
                                    Text(
                                        "${firstMatchedItem.nameEn}: ${firstMatchedItem.procurementAction}",
                                        fontSize = 11.sp,
                                        color = Emerald800
                                    )
                                }
                            }
                        }
                    }
                }

                // Sub-Entries List
                item {
                    Text(
                        "Verified Digital Entries (${entries.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate900
                    )
                }

                items(entries) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (entry.type == LedgerType.CREDIT) Emerald100 else Crimson100,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                if (entry.type == LedgerType.CREDIT) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (entry.type == LedgerType.CREDIT) Emerald800 else Crimson800,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(entry.partyName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                                        Text(entry.date, fontSize = 10.sp, color = Slate600)
                                    }
                                }

                                Text(
                                    "${if (entry.type == LedgerType.CREDIT) "+₹" else "-₹"}${String.format(Locale.ROOT, "%.0f", entry.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (entry.type == LedgerType.CREDIT) Emerald800 else Crimson800
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                entry.description,
                                fontSize = 12.sp,
                                color = Slate700
                            )

                            if (entry.rawText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "OCR: ${entry.rawText}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Slate600
                                )
                            }

                            if (entry.type == LedgerType.DEBIT) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            selectedWhatsAppReminderEntry = LedgerEntry(
                                                date = entry.date,
                                                partyName = entry.partyName,
                                                description = entry.description,
                                                amount = entry.amount,
                                                type = entry.type,
                                                category = entry.category,
                                                source = LedgerSource.OCR,
                                                isConfirmed = true
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Emerald700, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WhatsApp Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald800)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Scanned Khata?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this digitized khata and its associated entries from your records?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteScannedKhata(khataId) {
                            showDeleteConfirmDialog = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Crimson600)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // WhatsApp Reminder Dialog
    selectedWhatsAppReminderEntry?.let { entry ->
        val reminderText = viewModel.generateWhatsAppReminderText(entry, userProfile.preferredLanguage)
        WhatsAppReminderDialog(
            reminderText = reminderText,
            customerName = entry.partyName,
            amount = entry.amount,
            isHindi = userProfile.preferredLanguage == AppLanguage.HINDI,
            onDismiss = { selectedWhatsAppReminderEntry = null }
        )
    }
}
