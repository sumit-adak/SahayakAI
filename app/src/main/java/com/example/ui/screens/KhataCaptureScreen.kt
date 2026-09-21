package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.KhataSource
import com.example.data.model.OcrParsedItem
import com.example.data.service.KhataPreset
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel

enum class CaptureTab {
    CAMERA_GALLERY,
    SAMPLE_PRESETS,
    CSV_IMPORT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataCaptureScreen(
    viewModel: SahayakViewModel,
    onProceedToReview: (
        items: List<OcrParsedItem>,
        isOffline: Boolean,
        imageUri: String?,
        presetId: String?,
        source: KhataSource
    ) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(CaptureTab.SAMPLE_PRESETS) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPreset by remember { mutableStateOf<KhataPreset?>(viewModel.getAllKhataPresets().firstOrNull()) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var isOfflineMode by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // CSV input state
    var csvText by remember {
        mutableStateOf(
            """
Date,Party Name,Description,Amount,Type
2026-03-01,Suresh Verma,10kg Aata & Mustard Oil,1450,Debit
2026-03-01,Daily UPI Bikri,Counter sales QR,3280,Credit
2026-03-01,Kashi Wholesale,2 Oil Tins restock,2600,Debit
2026-03-01,Bablu Chaiwala,Milk & curd daily supply,240,Debit
2026-03-01,Sunita Devi SHG,Handicraft threads payment,500,Credit
            """.trimIndent()
        )
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedImageUri = null
            selectedPreset = null
            selectedTab = CaptureTab.CAMERA_GALLERY
        }
    }

    // Gallery picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            capturedBitmap = null
            selectedPreset = null
            selectedTab = CaptureTab.CAMERA_GALLERY
        }
    }

    val presets = remember { viewModel.getAllKhataPresets() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Scan & Digitize Khata",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate900
                        )
                        Text(
                            "बहीखाता स्कैन व डिजिटल लेजर",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_capture")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate800
                        )
                    }
                },
                actions = {
                    // Offline / Online toggle chip
                    FilterChip(
                        selected = isOfflineMode,
                        onClick = { isOfflineMode = !isOfflineMode },
                        label = {
                            Text(
                                if (isOfflineMode) "Offline OCR" else "Gemini AI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (isOfflineMode) Icons.Filled.WifiOff else Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isOfflineMode) Amber600 else Emerald600
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (isOfflineMode) Amber100 else Emerald50,
                            labelColor = if (isOfflineMode) Slate900 else Emerald900
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(paddingValues)
        ) {
            // Segmented Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = PureWhite,
                contentColor = Emerald800
            ) {
                Tab(
                    selected = selectedTab == CaptureTab.SAMPLE_PRESETS,
                    onClick = { selectedTab = CaptureTab.SAMPLE_PRESETS },
                    text = { Text("Sample Khatas", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Filled.CollectionsBookmark, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == CaptureTab.CAMERA_GALLERY,
                    onClick = { selectedTab = CaptureTab.CAMERA_GALLERY },
                    text = { Text("Camera / Photo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == CaptureTab.CSV_IMPORT,
                    onClick = { selectedTab = CaptureTab.CSV_IMPORT },
                    text = { Text("Import CSV", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Filled.TableView, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    CaptureTab.SAMPLE_PRESETS -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Emerald100,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                                    contentDescription = null,
                                                    tint = Emerald800,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                "Select a Real Handwritten Khata",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Slate900
                                            )
                                            Text(
                                                "Demonstrates layout-preserving OCR & AI structuring",
                                                fontSize = 12.sp,
                                                color = Slate600
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(presets) { preset ->
                                            val isSelected = selectedPreset?.id == preset.id
                                            OutlinedCard(
                                                onClick = { selectedPreset = preset },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.outlinedCardColors(
                                                    containerColor = if (isSelected) Emerald50 else Slate100
                                                ),
                                                border = BorderStroke(
                                                    if (isSelected) 2.dp else 1.dp,
                                                    if (isSelected) Emerald700 else Slate300
                                                ),
                                                modifier = Modifier
                                                    .width(190.dp)
                                                    .testTag("preset_card_${preset.id}")
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            preset.headerBadge,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) Emerald800 else Slate600
                                                        )
                                                        if (isSelected) {
                                                            Icon(
                                                                Icons.Filled.CheckCircle,
                                                                contentDescription = null,
                                                                tint = Emerald700,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        preset.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Slate900,
                                                        maxLines = 2
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        "${preset.rows.size} Ledger Entries",
                                                        fontSize = 11.sp,
                                                        color = Slate700
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        selectedPreset?.let { preset ->
                            item {
                                KhataPreviewCard(
                                    title = preset.title,
                                    description = preset.defaultDescription,
                                    rows = preset.rows,
                                    rotationAngle = rotationAngle,
                                    onRotate = { rotationAngle = (rotationAngle + 90f) % 360f }
                                )
                            }
                        }
                    }

                    CaptureTab.CAMERA_GALLERY -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Capture Khata Page with Camera",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        "Position camera directly above paper with adequate light.",
                                        fontSize = 12.sp,
                                        color = Slate600
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = { cameraLauncher.launch(null) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("btn_launch_camera"),
                                            colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Take Photo", fontWeight = FontWeight.Bold, color = PureWhite)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("btn_launch_gallery"),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate900),
                                            border = BorderStroke(1.dp, Slate300),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Gallery", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            if (capturedBitmap != null || selectedImageUri != null) {
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
                                            Text(
                                                "Captured Page Preview",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Slate900
                                            )
                                            IconButton(
                                                onClick = { rotationAngle = (rotationAngle + 90f) % 360f }
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.RotateRight,
                                                    contentDescription = "Rotate",
                                                    tint = Slate800
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(260.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Slate100),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (capturedBitmap != null) {
                                                Image(
                                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                                    contentDescription = "Captured Khata",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .rotate(rotationAngle)
                                                )
                                            } else if (selectedImageUri != null) {
                                                AsyncImage(
                                                    model = selectedImageUri,
                                                    contentDescription = "Selected Khata",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .rotate(rotationAngle)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Slate100)
                                        .border(BorderStroke(1.dp, Slate300), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Outlined.DocumentScanner,
                                            contentDescription = null,
                                            tint = Slate600,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "No photo captured yet",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Slate800
                                        )
                                        Text(
                                            "Tap 'Take Photo' or pick from gallery above",
                                            fontSize = 12.sp,
                                            color = Slate600
                                        )
                                    }
                                }
                            }
                        }
                    }

                    CaptureTab.CSV_IMPORT -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Paste Digital Ledger / CSV Statement",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        "Supports comma or tab-separated exports from billing apps or bank SMS statements.",
                                        fontSize = 12.sp,
                                        color = Slate600
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = csvText,
                                        onValueChange = { csvText = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .testTag("input_csv_text"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Slate900,
                                            unfocusedTextColor = Slate900,
                                            focusedContainerColor = Slate50,
                                            unfocusedContainerColor = Slate50,
                                            focusedBorderColor = Emerald700,
                                            unfocusedBorderColor = Slate400
                                        ),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = Slate900
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Process OCR / Import Button
                item {
                    Button(
                        onClick = {
                            isProcessing = true
                            when (selectedTab) {
                                CaptureTab.SAMPLE_PRESETS -> {
                                    val currentPreset = selectedPreset ?: presets.first()
                                    viewModel.structureAndProcessKhataScan(
                                        rawRows = currentPreset.rows,
                                        isOnline = !isOfflineMode
                                    ) { parsedItems, usedOffline ->
                                        isProcessing = false
                                        onProceedToReview(
                                            parsedItems,
                                            usedOffline,
                                            null,
                                            currentPreset.id,
                                            KhataSource.PRESET
                                        )
                                    }
                                }
                                CaptureTab.CAMERA_GALLERY -> {
                                    // Camera or photo picked
                                    val simulatedRows = listOf(
                                        "01/03/2026 Suresh Verma - 10kg Aata & Dal Rs 1450 udhaar",
                                        "01/03/2026 Daily Counter Bikri UPI QR ₹3280 jama",
                                        "01/03/2026 Kashi Wholesale 2 Mustard Oil Tins ₹2600 kharch",
                                        "01/03/2026 Bablu Chaiwala 5L milk ₹240 udhaar",
                                        "01/03/2026 Sunita Devi SHG handloom payment ₹500 jama"
                                    )
                                    viewModel.structureAndProcessKhataScan(
                                        rawRows = simulatedRows,
                                        isOnline = !isOfflineMode
                                    ) { parsedItems, usedOffline ->
                                        isProcessing = false
                                        onProceedToReview(
                                            parsedItems,
                                            usedOffline,
                                            selectedImageUri?.toString(),
                                            null,
                                            if (capturedBitmap != null) KhataSource.CAMERA else KhataSource.GALLERY
                                        )
                                    }
                                }
                                CaptureTab.CSV_IMPORT -> {
                                    val parsedItems = viewModel.parseCsvOrStatement(csvText)
                                    isProcessing = false
                                    onProceedToReview(
                                        parsedItems,
                                        false,
                                        null,
                                        null,
                                        KhataSource.IMPORT
                                    )
                                }
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_start_ocr_extraction"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald800)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (isOfflineMode) "Running On-Device OCR..." else "Structuring via Gemini AI...",
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = PureWhite)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Extract & Structure Digital Khata",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PureWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KhataPreviewCard(
    title: String,
    description: String,
    rows: List<String>,
    rotationAngle: Float,
    onRotate: () -> Unit
) {
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
                Column {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate900
                    )
                    Text(
                        description,
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }
                IconButton(onClick = onRotate) {
                    Icon(
                        Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Rotate Preview",
                        tint = Slate800
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Realistic Simulated Khata Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFDFBF7))
                    .border(BorderStroke(1.dp, Color(0xFFE2D9C8)), RoundedCornerShape(12.dp))
                    .rotate(rotationAngle)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1EADB), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Original Handwritten Page (Raw Ledger)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF5A4D3B)
                        )
                        Text(
                            "Hindi / Hinglish",
                            fontSize = 10.sp,
                            color = Color(0xFF7D6C54)
                        )
                    }

                    rows.take(5).forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "•",
                                color = Color(0xFF8C7A60),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                row,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate900,
                                maxLines = 1
                            )
                        }
                    }

                    if (rows.size > 5) {
                        Text(
                            "+ ${rows.size - 5} more lines detected on page...",
                            fontSize = 10.sp,
                            color = Slate600,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
