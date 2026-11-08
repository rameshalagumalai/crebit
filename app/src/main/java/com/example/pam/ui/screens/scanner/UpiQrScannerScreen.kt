package com.example.pam.ui.screens.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pam.data.model.Transaction
import com.example.pam.data.repository.PAMRepository
import com.example.pam.ui.components.ReceiptScanResultBottomSheet
import com.example.pam.ui.theme.IncomeGreen
import com.example.pam.util.UpiParser
import com.example.pam.util.ai.ParsedReceipt
import com.example.pam.util.ai.ReceiptParser
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class ScannerMode(val label: String) {
    QR_CODE("UPI QR"),
    RECEIPT("Receipt (AI)")
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun UpiQrScannerScreen(
    initialModeStr: String? = null,
    onNavigateBack: () -> Unit,
    onQrCodeDetected: (scannedContent: String) -> Unit,
    onNavigateToAddTransactionWithReceipt: (amount: Double, merchant: String, categoryId: Long?, accountId: Long, dateMillis: Long, note: String) -> Unit = { _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val repository = remember { PAMRepository.getInstance(context) }

    val accounts by repository.getAccountsFlow().collectAsState(initial = emptyList())
    val categories by repository.getCategoriesFlow().collectAsState(initial = emptyList())

    var currentMode by remember {
        mutableStateOf(
            if (initialModeStr.equals("RECEIPT", ignoreCase = true)) ScannerMode.RECEIPT else ScannerMode.QR_CODE
        )
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to scan live")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isScanProcessed by remember { mutableStateOf(false) }
    var isAnalyzingReceipt by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var scannedReceiptResult by remember { mutableStateOf<ParsedReceipt?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Pick from Gallery Launcher (handles both QR code and Receipt photo)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                if (currentMode == ScannerMode.QR_CODE) {
                    val scanner = BarcodeScanning.getClient()
                    scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            val qrCode = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                            val rawValue = qrCode?.rawValue
                            if (!rawValue.isNullOrBlank()) {
                                triggerVibration(context)
                                onQrCodeDetected(rawValue)
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No QR code found in selected image")
                                }
                            }
                        }
                        .addOnFailureListener {
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to process QR image")
                            }
                        }
                } else {
                    // Receipt mode OCR from gallery
                    isAnalyzingReceipt = true
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            isAnalyzingReceipt = false
                            val parsed = ReceiptParser.parse(visionText, categories)
                            if (parsed.amount != null || parsed.merchantName != null) {
                                triggerVibration(context)
                                scannedReceiptResult = parsed
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Could not detect receipt details. Try a clearer image.")
                                }
                            }
                        }
                        .addOnFailureListener {
                            isAnalyzingReceipt = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to read receipt image")
                            }
                        }
                }
            } catch (e: Exception) {
                isAnalyzingReceipt = false
                scope.launch {
                    snackbarHostState.showSnackbar("Error reading image: ${e.localizedMessage}")
                }
            }
        }
    }

    // Function to snap photo and run on-device OCR
    fun snapAndAnalyzeReceipt() {
        if (isAnalyzingReceipt) return
        isAnalyzingReceipt = true

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        recognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                isAnalyzingReceipt = false
                                val parsed = ReceiptParser.parse(visionText, categories)
                                if (parsed.amount != null || parsed.merchantName != null) {
                                    triggerVibration(context)
                                    scannedReceiptResult = parsed
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("No bill amount detected. Try holding closer with good lighting.")
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                isAnalyzingReceipt = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Scan error: ${e.localizedMessage}")
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        isAnalyzingReceipt = false
                        imageProxy.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    isAnalyzingReceipt = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Capture failed: ${exception.localizedMessage}")
                    }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasCameraPermission) {
                // Camera Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val barcodeScanner = BarcodeScanning.getClient()
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null && !isScanProcessed && currentMode == ScannerMode.QR_CODE) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            barcodeScanner.process(image)
                                                .addOnSuccessListener { barcodes ->
                                                    for (barcode in barcodes) {
                                                        val rawVal = barcode.rawValue
                                                        if (!rawVal.isNullOrBlank() && !isScanProcessed) {
                                                            isScanProcessed = true
                                                            triggerVibration(context)
                                                            scope.launch {
                                                                onQrCodeDetected(rawVal)
                                                            }
                                                            break
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                DisposableEffect(Unit) {
                    onDispose {
                        cameraExecutor.shutdown()
                    }
                }

                // Dual-Mode Reticle Overlay
                SmartScannerOverlay(mode = currentMode)
            } else {
                // Permission Denied Card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Camera Access Required",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "PAM uses your camera to scan UPI QR codes and locally parse paper receipts with on-device AI.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Permission")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Choose from Gallery Instead")
                            }
                        }
                    }
                }
            }

            // Top Header: Back button, Segmented Mode Switch, Torch toggle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    // Mode Toggle (QR vs Receipt)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ScannerModeTab(
                                title = "UPI QR",
                                icon = Icons.Default.QrCodeScanner,
                                isSelected = currentMode == ScannerMode.QR_CODE,
                                activeColor = IncomeGreen,
                                onClick = { currentMode = ScannerMode.QR_CODE }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            ScannerModeTab(
                                title = "Receipt (AI)",
                                icon = Icons.Default.AutoAwesome,
                                isSelected = currentMode == ScannerMode.RECEIPT,
                                activeColor = Color(0xFF8B5CF6),
                                onClick = { currentMode = ScannerMode.RECEIPT }
                            )
                        }
                    }

                    if (hasCameraPermission && camera?.cameraInfo?.hasFlashUnit() == true) {
                        IconButton(
                            onClick = {
                                isTorchOn = !isTorchOn
                                camera?.cameraControl?.enableTorch(isTorchOn)
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Torch",
                                tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }
            }

            // Bottom Actions & Shutter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hint Tag
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = if (currentMode == ScannerMode.QR_CODE) {
                            "Point camera at any UPI QR code"
                        } else {
                            "Align receipt inside frame & tap Snap • 100% Offline"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (currentMode == ScannerMode.QR_CODE) {
                    // QR Mode Controls: Gallery & Manual
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = "Gallery", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery QR", color = Color.White)
                        }

                        Button(
                            onClick = { showManualInputDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Keyboard, contentDescription = "Manual", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste / Type", color = Color.White)
                        }
                    }
                } else {
                    // Receipt Mode Controls: Gallery Pick & Center Shutter Snap Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pick photo from gallery
                        IconButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Pick Receipt",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Shutter Button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                                .padding(5.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF8B5CF6),
                                            Color(0xFF3B82F6)
                                        )
                                    )
                                )
                                .clickable { snapAndAnalyzeReceipt() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAnalyzingReceipt) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = "Snap Receipt",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        // Placeholder for symmetry
                        Spacer(modifier = Modifier.size(50.dp))
                    }
                }
            }

            // Loading Overlay when analyzing
            AnimatedVisibility(
                visible = isAnalyzingReceipt,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF8B5CF6),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Extracting receipt on-device...",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Receipt Scanned Result Bottom Sheet
    scannedReceiptResult?.let { receipt ->
        ReceiptScanResultBottomSheet(
            parsedReceipt = receipt,
            accounts = accounts,
            categories = categories,
            onSaveTransaction = { txn ->
                scope.launch {
                    repository.addTransaction(txn)
                    snackbarHostState.showSnackbar("Logged: ${txn.title} (₹${txn.amount})")
                    onNavigateBack()
                }
            },
            onReviewInFullEditor = { amount, merchant, categoryId, accountId, dateMillis, note ->
                onNavigateToAddTransactionWithReceipt(amount, merchant, categoryId, accountId, dateMillis, note)
            },
            onClose = {
                scannedReceiptResult = null
            }
        )
    }

    // Manual Input Dialog
    if (showManualInputDialog) {
        var manualText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = { Text("Enter UPI ID or Paste QR Text") },
            text = {
                Column {
                    Text(
                        text = "Enter a valid UPI ID (e.g. store@okbank) or paste full upi://pay URI:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        placeholder = { Text("upi://pay?pa=... or name@upi") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = UpiParser.parse(manualText.trim())
                        if (parsed != null) {
                            showManualInputDialog = false
                            onQrCodeDetected(manualText.trim())
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Invalid UPI URI or UPI ID format")
                            }
                        }
                    },
                    enabled = manualText.isNotBlank()
                ) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScannerModeTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) activeColor else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun SmartScannerOverlay(mode: ScannerMode) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScannerAnim")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserPosition"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val isQr = mode == ScannerMode.QR_CODE

        val reticleWidth = if (isQr) size.minDimension * 0.7f else size.minDimension * 0.78f
        val reticleHeight = if (isQr) reticleWidth else reticleWidth * 1.35f

        val left = (size.width - reticleWidth) / 2
        val top = (size.height - reticleHeight) / 2 - 30.dp.toPx()
        val cornerRadius = 24.dp.toPx()

        // Dim background outside reticle
        val path = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
            addRoundRect(
                RoundRect(
                    left = left,
                    top = top,
                    right = left + reticleWidth,
                    bottom = top + reticleHeight,
                    radiusX = cornerRadius,
                    radiusY = cornerRadius
                )
            )
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
        }

        drawPath(path = path, color = Color.Black.copy(alpha = 0.55f))

        // Corner reticle accents
        val strokeWidth = 5.dp.toPx()
        val cornerLength = 32.dp.toPx()
        val reticleColor = if (isQr) IncomeGreen else Color(0xFF8B5CF6)

        val right = left + reticleWidth
        val bottom = top + reticleHeight

        // Top Left
        drawLine(reticleColor, Offset(left, top + cornerLength), Offset(left, top + cornerRadius), strokeWidth)
        drawLine(reticleColor, Offset(left + cornerRadius, top), Offset(left + cornerLength, top), strokeWidth)

        // Top Right
        drawLine(reticleColor, Offset(right, top + cornerLength), Offset(right, top + cornerRadius), strokeWidth)
        drawLine(reticleColor, Offset(right - cornerRadius, top), Offset(right - cornerLength, top), strokeWidth)

        // Bottom Left
        drawLine(reticleColor, Offset(left, bottom - cornerLength), Offset(left, bottom - cornerRadius), strokeWidth)
        drawLine(reticleColor, Offset(left + cornerRadius, bottom), Offset(left + cornerLength, bottom), strokeWidth)

        // Bottom Right
        drawLine(reticleColor, Offset(right, bottom - cornerLength), Offset(right, bottom - cornerRadius), strokeWidth)
        drawLine(reticleColor, Offset(right - cornerRadius, bottom), Offset(right - cornerLength, bottom), strokeWidth)

        // Animated scan line
        val scanY = top + (reticleHeight * laserPosition)
        drawLine(
            color = reticleColor,
            start = Offset(left + 16.dp.toPx(), scanY),
            end = Offset(right - 16.dp.toPx(), scanY),
            strokeWidth = 3.dp.toPx()
        )
    }
}

private fun triggerVibration(context: Context) {
    try {
        val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Exception) {
        // Ignore if vibrator fails
    }
}
