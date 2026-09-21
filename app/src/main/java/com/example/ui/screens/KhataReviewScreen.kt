package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataReviewScreen(
    viewModel: SahayakViewModel,
    initialItems: List<OcrParsedItem>,
    isOffline: Boolean,
    imageUri: String?,
    presetId: String?,
    source: KhataSource,
    onSaveSuccess: (khataId: Long) -> Unit,
    onBack: () -> Unit
) {
    val itemsState = remember { mutableStateListOf<OcrParsedItem>().apply { addAll(initialItems) } }
    val todayFormatted = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) }

    var khataLabel by remember {
        mutableStateOf(
            when (presetId) {
                "preset_kirana_march_2026" -> "March 2026 — Grocery Shop Khata"
                "preset_shg_handicrafts" -> "February 2026 — SHG Handicrafts Ledger"
                "preset_mandi_challan" -> "Mandi Sales & Transport Challan"
                "preset_dairy_milk" -> "March 2026 — Cooperative Milk Supply"
                else -> "Scanned Khata — $todayFormatted"
            }
        )
    }
    var khataDescription by remember {
        mutableStateOf(
            when (presetId) {
                "preset_kirana_march_2026" -> "Daily customer ration chits, wholesale mustard oil restock, and counter UPI bikri."
                "preset_shg_handicrafts" -> "Artisan group raw material procurement, weekly thrift savings, and exhibition sales."
                else -> "Digitized from handwritten paper ledger via SahayakAI OCR."
            }
        )
    }

    var showAddRowDialog by remember { mutableStateOf(false) }
    var selectedRowForRawView by remember { mutableStateOf<OcrParsedItem?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showOriginalImageExpanded by remember { mutableStateOf(true) }

    val totalCredit = itemsState.filter { it.type == LedgerType.CREDIT }.sumOf { it.amount }
    val totalDebit = itemsState.filter { it.type == LedgerType.DEBIT }.sumOf { it.amount }
    val netBalance = totalCredit - totalDebit
    val lowConfidenceCount = itemsState.count { it.confidence < 0.90f }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Review & Label Khata",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate900
                        )
                        Text(
                            "बहीखाता प्रविष्टि सत्यापन",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_review")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate800
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showOriginalImageExpanded = !showOriginalImageExpanded }) {
                        Icon(
                            if (showOriginalImageExpanded) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle Original Document View",
                            tint = Slate800
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
            )
        },
        bottomBar = {
            Surface(
                color = PureWhite,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showAddRowDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald800),
                        border = BorderStroke(1.dp, Emerald700),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Row", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            isSaving = true
                            viewModel.saveScannedKhataWithEntries(
                                label = khataLabel,
                                description = khataDescription,
                                imageUri = imageUri,
                                presetId = presetId,
                                source = source,
                                items = itemsState.toList(),
                                isOffline = isOffline
                            ) { savedId ->
                                isSaving = false
                                onSaveSuccess(savedId)
                            }
                        },
                        enabled = !isSaving && itemsState.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("btn_confirm_save_khata")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = PureWhite)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Confirm & Save",
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)
        ) {
            // Low Confidence Warning Banner
            if (lowConfidenceCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Amber100),
                        border = BorderStroke(1.dp, Amber300)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = Amber800,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "⚠️ $lowConfidenceCount Entries Need Review",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Amber900
                                )
                                Text(
                                    "Highlighted in amber below. Please verify party name or amount against original paper.",
                                    fontSize = 11.sp,
                                    color = Amber900
                                )
                            }
                        }
                    }
                }
            }

            // Top: Original Khata Image View (Collapsible / Toggleable)
            if (showOriginalImageExpanded) {
                item {
                    OriginalKhataDocCard(
                        imageUri = imageUri,
                        presetId = presetId,
                        isOffline = isOffline
                    )
                }
            }

            // Label & Metadata Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Khata Details & Label",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = khataLabel,
                            onValueChange = { khataLabel = it },
                            label = { Text("Khata Name / Label *", color = Slate800) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_khata_label"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedContainerColor = Slate50,
                                unfocusedContainerColor = Slate50,
                                focusedBorderColor = Emerald700,
                                unfocusedBorderColor = Slate400
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = khataDescription,
                            onValueChange = { khataDescription = it },
                            label = { Text("Description / Notes (Optional)", color = Slate800) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedContainerColor = Slate50,
                                unfocusedContainerColor = Slate50,
                                focusedBorderColor = Emerald700,
                                unfocusedBorderColor = Slate400
                            )
                        )
                    }
                }
            }

            // Live Totals Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "TOTAL JAMA (CREDIT)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald300
                            )
                            Text(
                                "₹${String.format(Locale.ROOT, "%.0f", totalCredit)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "TOTAL UDHAAR (DEBIT)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Crimson300
                            )
                            Text(
                                "₹${String.format(Locale.ROOT, "%.0f", totalDebit)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "NET BALANCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Amber300
                            )
                            Text(
                                "₹${String.format(Locale.ROOT, "%.0f", netBalance)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }
                }
            }

            // Editable Rows Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Extracted Ledger Entries (${itemsState.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Slate900
                    )
                    Text(
                        "Tap to edit any row",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }
            }

            // Editable Entry Cards
            itemsIndexed(itemsState) { index, item ->
                EditableEntryCard(
                    item = item,
                    onItemUpdated = { updated ->
                        itemsState[index] = updated.copy(editedByUser = true)
                    },
                    onDelete = {
                        itemsState.removeAt(index)
                    },
                    onViewRaw = {
                        selectedRowForRawView = item
                    }
                )
            }
        }
    }

    // Add Row Dialog
    if (showAddRowDialog) {
        AddRowDialog(
            onDismiss = { showAddRowDialog = false },
            onAdd = { newItem ->
                itemsState.add(newItem)
                showAddRowDialog = false
            }
        )
    }

    // View Raw OCR Text Dialog
    selectedRowForRawView?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedRowForRawView = null },
            title = {
                Text("Original OCR Line Text", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Exact text detected by camera OCR scanner:", fontSize = 12.sp, color = Slate600)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate100)
                            .padding(12.dp)
                    ) {
                        Text(
                            item.rawText.ifBlank { "No raw text available" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Slate900
                        )
                    }
                    Text("Confidence: ${(item.confidence * 100).toInt()}%", fontSize = 11.sp, color = Slate700)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRowForRawView = null }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = Emerald800)
                }
            }
        )
    }
}

@Composable
private fun OriginalKhataDocCard(
    imageUri: String?,
    presetId: String?,
    isOffline: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = null,
                        tint = Emerald800,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Original Khata Document",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate900
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isOffline) Amber100 else Emerald50
                ) {
                    Text(
                        if (isOffline) "Local OCR" else "Gemini Structured",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOffline) Amber900 else Emerald900,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFBF9F5))
                    .border(BorderStroke(1.dp, Color(0xFFE5DEC9)), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Original Khata",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF8B775C),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Handwritten Ledger Original Scan",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(0xFF5E4E39)
                        )
                        Text(
                            "Preserved permanently in your Sahayak account",
                            fontSize = 10.sp,
                            color = Color(0xFF8B775C)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableEntryCard(
    item: OcrParsedItem,
    onItemUpdated: (OcrParsedItem) -> Unit,
    onDelete: () -> Unit,
    onViewRaw: () -> Unit
) {
    val isLowConfidence = item.confidence < 0.90f
    var partyName by remember(item.partyName) { mutableStateOf(item.partyName) }
    var description by remember(item.description) { mutableStateOf(item.description) }
    var amountStr by remember(item.amount) { mutableStateOf(String.format(Locale.ROOT, "%.0f", item.amount)) }
    var itemType by remember(item.type) { mutableStateOf(item.type) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowConfidence && !item.editedByUser) Color(0xFFFFFBEB) else PureWhite
        ),
        border = BorderStroke(
            if (isLowConfidence && !item.editedByUser) 1.5.dp else 1.dp,
            if (isLowConfidence && !item.editedByUser) Amber400 else Slate200
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Type toggle & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Switcher Chip (Credit / Debit)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = itemType == LedgerType.CREDIT,
                        onClick = {
                            itemType = LedgerType.CREDIT
                            onItemUpdated(item.copy(type = LedgerType.CREDIT))
                        },
                        label = { Text("Jama (Credit)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald700,
                            selectedLabelColor = PureWhite,
                            containerColor = Slate100,
                            labelColor = Slate700
                        )
                    )

                    FilterChip(
                        selected = itemType == LedgerType.DEBIT,
                        onClick = {
                            itemType = LedgerType.DEBIT
                            onItemUpdated(item.copy(type = LedgerType.DEBIT))
                        },
                        label = { Text("Udhaar (Debit)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Crimson600,
                            selectedLabelColor = PureWhite,
                            containerColor = Slate100,
                            labelColor = Slate700
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onViewRaw, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = "View Raw OCR",
                            tint = Slate600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete Row",
                            tint = Crimson600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Editable Input Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = partyName,
                    onValueChange = {
                        partyName = it
                        onItemUpdated(item.copy(partyName = it))
                    },
                    label = { Text("Party Name", color = Slate800) },
                    modifier = Modifier.weight(1.4f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedBorderColor = Emerald700,
                        unfocusedBorderColor = Slate400,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    )
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        val parsed = it.toDoubleOrNull() ?: item.amount
                        onItemUpdated(item.copy(amount = parsed))
                    },
                    label = { Text("Amount (₹)", color = Slate800) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedBorderColor = Emerald700,
                        unfocusedBorderColor = Slate400,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    onItemUpdated(item.copy(description = it))
                },
                label = { Text("Item / Description", color = Slate800) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate900,
                    unfocusedTextColor = Slate900,
                    focusedBorderColor = Emerald700,
                    unfocusedBorderColor = Slate400,
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50
                )
            )

            if (isLowConfidence && !item.editedByUser) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "⚠️ OCR Confidence ${(item.confidence * 100).toInt()}% — original: \"${item.rawText.take(30)}...\"",
                        fontSize = 10.sp,
                        color = Amber800,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun AddRowDialog(
    onDismiss: () -> Unit,
    onAdd: (OcrParsedItem) -> Unit
) {
    var partyName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(LedgerType.DEBIT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Ledger Row", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text("Party Name / Customer", color = Slate800) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
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
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Items", color = Slate800) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
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
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)", color = Slate800) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedBorderColor = Emerald700,
                        unfocusedBorderColor = Slate400,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = Slate50
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = type == LedgerType.CREDIT,
                        onClick = { type = LedgerType.CREDIT },
                        label = { Text("Jama (Credit)") }
                    )
                    FilterChip(
                        selected = type == LedgerType.DEBIT,
                        onClick = { type = LedgerType.DEBIT },
                        label = { Text("Udhaar (Debit)") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (partyName.isNotBlank() && amount > 0) {
                        onAdd(
                            OcrParsedItem(
                                partyName = partyName,
                                description = description.ifBlank { "Khata entry" },
                                amount = amount,
                                type = type,
                                category = if (type == LedgerType.CREDIT) LedgerCategory.SALES else LedgerCategory.CUSTOMER_UDHAAR,
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()),
                                rawText = "$partyName - $description Rs $amount",
                                confidence = 1.0f,
                                editedByUser = true
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald800)
            ) {
                Text("Add Row", fontWeight = FontWeight.Bold, color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
