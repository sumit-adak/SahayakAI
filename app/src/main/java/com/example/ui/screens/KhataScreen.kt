package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.*
import com.example.ui.components.AddLedgerEntryDialog
import com.example.ui.components.WhatsAppReminderDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.LedgerFilter
import com.example.ui.viewmodel.SahayakViewModel
import java.util.Locale

enum class KhataTab {
    SCANNED_KHATAS,
    LEDGER_ENTRIES,
    DOCUMENT_LOCKER
}

private enum class InternalKhataScreen {
    MAIN_LIST,
    CAPTURE,
    REVIEW,
    DETAIL
}

@Composable
fun KhataScreen(
    viewModel: SahayakViewModel,
    onOpenOcrScan: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val ledgerEntries by viewModel.allLedgerEntries.collectAsState()
    val digitalDocs by viewModel.allDigitalDocuments.collectAsState()
    val scannedKhatas by viewModel.allScannedKhatas.collectAsState()
    val selectedFilter by viewModel.selectedLedgerFilter.collectAsState()
    val isHindi = userProfile.preferredLanguage == AppLanguage.HINDI

    var activeScreen by remember { mutableStateOf(InternalKhataScreen.MAIN_LIST) }
    var activeTab by remember { mutableStateOf(KhataTab.SCANNED_KHATAS) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedWhatsAppReminderEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var selectedDocDetail by remember { mutableStateOf<DigitalDocument?>(null) }
    var selectedKhataIdForDetail by remember { mutableLongStateOf(1L) }

    // Review temporary state
    var reviewItems by remember { mutableStateOf<List<OcrParsedItem>>(emptyList()) }
    var reviewIsOffline by remember { mutableStateOf(false) }
    var reviewImageUri by remember { mutableStateOf<String?>(null) }
    var reviewPresetId by remember { mutableStateOf<String?>(null) }
    var reviewSource by remember { mutableStateOf(KhataSource.PRESET) }

    when (activeScreen) {
        InternalKhataScreen.CAPTURE -> {
            KhataCaptureScreen(
                viewModel = viewModel,
                onProceedToReview = { items, isOffline, imageUri, presetId, source ->
                    reviewItems = items
                    reviewIsOffline = isOffline
                    reviewImageUri = imageUri
                    reviewPresetId = presetId
                    reviewSource = source
                    activeScreen = InternalKhataScreen.REVIEW
                },
                onBack = { activeScreen = InternalKhataScreen.MAIN_LIST }
            )
        }

        InternalKhataScreen.REVIEW -> {
            KhataReviewScreen(
                viewModel = viewModel,
                initialItems = reviewItems,
                isOffline = reviewIsOffline,
                imageUri = reviewImageUri,
                presetId = reviewPresetId,
                source = reviewSource,
                onSaveSuccess = { savedId ->
                    selectedKhataIdForDetail = savedId
                    activeScreen = InternalKhataScreen.DETAIL
                },
                onBack = { activeScreen = InternalKhataScreen.CAPTURE }
            )
        }

        InternalKhataScreen.DETAIL -> {
            KhataDetailScreen(
                khataId = selectedKhataIdForDetail,
                viewModel = viewModel,
                onBack = { activeScreen = InternalKhataScreen.MAIN_LIST }
            )
        }

        InternalKhataScreen.MAIN_LIST -> {
            val filteredEntries = when (selectedFilter) {
                LedgerFilter.ALL -> ledgerEntries
                LedgerFilter.JAMA_CREDIT -> ledgerEntries.filter { it.type == LedgerType.CREDIT }
                LedgerFilter.UDHAAR_DEBIT -> ledgerEntries.filter { it.type == LedgerType.DEBIT }
                LedgerFilter.PENDING_OCR -> ledgerEntries.filter { !it.isConfirmed }
            }

            val totalCredit = ledgerEntries.filter { it.type == LedgerType.CREDIT }.sumOf { it.amount }
            val totalDebit = ledgerEntries.filter { it.type == LedgerType.DEBIT }.sumOf { it.amount }
            val netBalance = totalCredit - totalDebit

            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = Emerald800,
                        contentColor = PureWhite,
                        shape = CircleShape,
                        modifier = Modifier.testTag("fab_add_khata_entry")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Entry")
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Slate50)
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
                ) {
                    // 1. Digital Cash Balance Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, Slate200),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (isHindi) "कुल शुद्ध रोकड़ (नेट बैलेंस)" else "Net Cash In Hand Balance",
                                    fontSize = 12.sp,
                                    color = Slate700,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${String.format(Locale.ROOT, "%,.0f", netBalance)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (netBalance >= 0) Emerald900 else UdhaarRed
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = JamaGreenBg,
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = JamaGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(if (isHindi) "कुल जमा" else "Total Jama", fontSize = 11.sp, color = Slate900, fontWeight = FontWeight.SemiBold)
                                                Text("₹${String.format(Locale.ROOT, "%,.0f", totalCredit)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = JamaGreen)
                                            }
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = UdhaarRedBg,
                                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = UdhaarRed, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(if (isHindi) "कुल उधार/खर्च" else "Total Udhaar", fontSize = 11.sp, color = Slate900, fontWeight = FontWeight.SemiBold)
                                                Text("₹${String.format(Locale.ROOT, "%,.0f", totalDebit)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = UdhaarRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Scan Khata Hero Action Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Emerald50),
                            border = BorderStroke(1.dp, Emerald200)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(42.dp),
                                        shape = CircleShape,
                                        color = Emerald800
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Filled.DocumentScanner,
                                                contentDescription = null,
                                                tint = PureWhite,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isHindi) "कागजी बहीखाता स्कैन करें" else "Scan Handwritten Khata",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Emerald900
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Emerald200
                                            ) {
                                                Text(
                                                    "AI OCR",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Emerald900,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isHindi) "फोटो खींचकर डिजिटल लेजर में बदलें व खाता विवरण सुरक्षित रखें" else "Snap paper ledger → Layout-preserving OCR → Digital Ledger",
                                            fontSize = 11.sp,
                                            color = Slate800
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { activeScreen = InternalKhataScreen.CAPTURE },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("btn_open_khata_capture"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald800)
                                ) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isHindi) "स्कैन शुरू करें (Camera / Presets)" else "Scan Khata / Open Presets",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                }
                            }
                        }
                    }

                    // 3. Section Switcher Tabs: [Scanned Khatas] vs [Ledger Entries] vs [Digital Vault]
                    item {
                        TabRow(
                            selectedTabIndex = activeTab.ordinal,
                            containerColor = PureWhite,
                            contentColor = Emerald800,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        ) {
                            Tab(
                                selected = activeTab == KhataTab.SCANNED_KHATAS,
                                onClick = { activeTab = KhataTab.SCANNED_KHATAS },
                                text = {
                                    Text(
                                        text = if (isHindi) "📖 खाते (${scannedKhatas.size})" else "📖 Khatas (${scannedKhatas.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (activeTab == KhataTab.SCANNED_KHATAS) Emerald900 else Slate700
                                    )
                                }
                            )
                            Tab(
                                selected = activeTab == KhataTab.LEDGER_ENTRIES,
                                onClick = { activeTab = KhataTab.LEDGER_ENTRIES },
                                text = {
                                    Text(
                                        text = if (isHindi) "📋 लेन-देन (${ledgerEntries.size})" else "📋 Entries (${ledgerEntries.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (activeTab == KhataTab.LEDGER_ENTRIES) Emerald900 else Slate700
                                    )
                                }
                            )
                            Tab(
                                selected = activeTab == KhataTab.DOCUMENT_LOCKER,
                                onClick = { activeTab = KhataTab.DOCUMENT_LOCKER },
                                text = {
                                    Text(
                                        text = if (isHindi) "🗄️ लॉकर (${digitalDocs.size})" else "🗄️ Vault (${digitalDocs.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (activeTab == KhataTab.DOCUMENT_LOCKER) Emerald900 else Slate700
                                    )
                                }
                            )
                        }
                    }

                    when (activeTab) {
                        KhataTab.SCANNED_KHATAS -> {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Digitized Handwritten Ledgers",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        "${scannedKhatas.size} Saved Notebooks",
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                            }

                            if (scannedKhatas.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No scanned khatas yet. Tap 'Scan Khata' above to digitize your first notebook.", color = Slate600)
                                    }
                                }
                            } else {
                                items(scannedKhatas) { khata ->
                                    ScannedKhataCard(
                                        khata = khata,
                                        onClick = {
                                            selectedKhataIdForDetail = khata.id
                                            activeScreen = InternalKhataScreen.DETAIL
                                        }
                                    )
                                }
                            }
                        }

                        KhataTab.LEDGER_ENTRIES -> {
                            // Filter Chips
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(LedgerFilter.entries) { filter ->
                                        val isSelected = filter == selectedFilter
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { /* filter state */ },
                                            label = {
                                                Text(
                                                    text = when (filter) {
                                                        LedgerFilter.ALL -> if (isHindi) "सभी (${ledgerEntries.size})" else "All (${ledgerEntries.size})"
                                                        LedgerFilter.JAMA_CREDIT -> if (isHindi) "जमा / बिक्री" else "Jama (Credit)"
                                                        LedgerFilter.UDHAAR_DEBIT -> if (isHindi) "उधार / खर्च" else "Udhaar (Debit)"
                                                        LedgerFilter.PENDING_OCR -> if (isHindi) "समीक्षा बाकी" else "Pending Review"
                                                    },
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Emerald800,
                                                selectedLabelColor = PureWhite,
                                                containerColor = PureWhite,
                                                labelColor = Slate700
                                            )
                                        )
                                    }
                                }
                            }

                            if (filteredEntries.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No transactions found in this filter.", color = Slate600)
                                    }
                                }
                            } else {
                                items(filteredEntries) { entry ->
                                    LedgerEntryCard(
                                        entry = entry,
                                        isHindi = isHindi,
                                        onSendWhatsApp = { selectedWhatsAppReminderEntry = entry },
                                        onDelete = { viewModel.deleteLedgerEntry(entry.id) }
                                    )
                                }
                            }
                        }

                        KhataTab.DOCUMENT_LOCKER -> {
                            item {
                                Text(
                                    "Verified Digital Receipts & Chits",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Slate900
                                )
                            }

                            items(digitalDocs) { doc ->
                                Card(
                                    onClick = { selectedDocDetail = doc },
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
                                            Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                                            Text("₹${String.format(Locale.ROOT, "%.0f", doc.totalAmount)}", fontWeight = FontWeight.Bold, color = Emerald800)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(doc.dateAdded, fontSize = 11.sp, color = Slate600)
                                        if (doc.notes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(doc.notes, fontSize = 10.sp, color = Slate500)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Entry Dialog
    if (showAddDialog) {
        AddLedgerEntryDialog(
            isHindi = isHindi,
            onDismiss = { showAddDialog = false },
            onSave = { partyName, desc, amount, type, category, phone ->
                viewModel.addManualLedgerEntry(
                    partyName = partyName,
                    description = desc,
                    amount = amount,
                    type = type,
                    category = category,
                    phone = phone
                )
                showAddDialog = false
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

    // Digital Document Detail Modal
    selectedDocDetail?.let { doc ->
        AlertDialog(
            onDismissRequest = { selectedDocDetail = null },
            title = { Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Preserved in Digital Vault on ${doc.dateAdded}", fontSize = 12.sp, color = Slate600)
                    Text("Total Extracted Amount: ₹${String.format(Locale.ROOT, "%.0f", doc.totalAmount)}", fontWeight = FontWeight.Bold, color = Emerald800)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate100)
                            .padding(10.dp)
                    ) {
                        Text(doc.extractedText, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Slate900)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDocDetail = null }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = Emerald800)
                }
            }
        )
    }
}

@Composable
private fun ScannedKhataCard(
    khata: ScannedKhata,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scanned_khata_card_${khata.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFBF8F0),
                        border = BorderStroke(1.dp, Color(0xFFE2D6C0)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFF8B775C),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            khata.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate900
                        )
                        Text(
                            "${khata.entryCount} digital rows • ${khata.source.name}",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                }

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Khata",
                    tint = Slate400,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (khata.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    khata.description,
                    fontSize = 11.sp,
                    color = Slate700,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Jama / Udhaar stats bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate100, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Jama: ₹${String.format(Locale.ROOT, "%.0f", khata.totalCredit)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald800
                )
                Text(
                    "Udhaar: ₹${String.format(Locale.ROOT, "%.0f", khata.totalDebit)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Crimson800
                )
                Text(
                    "Net: ₹${String.format(Locale.ROOT, "%.0f", khata.totalCredit - khata.totalDebit)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            }
        }
    }
}

@Composable
private fun LedgerEntryCard(
    entry: LedgerEntry,
    isHindi: Boolean,
    onSendWhatsApp: () -> Unit,
    onDelete: () -> Unit
) {
    val isCredit = entry.type == LedgerType.CREDIT

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_entry_${entry.id}"),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isCredit) JamaGreenBg else UdhaarRedBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCredit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                            contentDescription = null,
                            tint = if (isCredit) JamaGreen else UdhaarRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = entry.partyName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Slate900
                        )
                        Text(
                            text = entry.description,
                            fontSize = 11.sp,
                            color = Slate700
                        )
                        Text(
                            text = entry.date,
                            fontSize = 10.sp,
                            color = Slate500
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isCredit) "+" else "-"} ₹${String.format(Locale.ROOT, "%,.0f", entry.amount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = if (isCredit) JamaGreen else UdhaarRed
                    )
                    Text(
                        text = if (isCredit) "Jama" else "Udhaar",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCredit) JamaGreen else UdhaarRed
                    )
                }
            }

            if (!isCredit) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onSendWhatsApp,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFDCFCE7), contentColor = Color(0xFF15803D)),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp Reminder", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
