package com.prisar.seawoods

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import java.io.File
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.prisar.seawoods.ui.theme.SeawoodsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeawoodsTheme {
                val navController = rememberNavController()
                var imageUri by remember { mutableStateOf<Uri?>(null) }
                var pdfUri by remember { mutableStateOf<Uri?>(null) }
                var pageCount by remember { mutableStateOf(0) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("main") {
                            MainScreen(
                                navController = navController,
                                imageUri = imageUri,
                                pdfUri = pdfUri,
                                pageCount = pageCount
                            )
                        }
                        composable("scanner") {
                            ScannerScreen(
                                activity = this@MainActivity,
                                onResult = { image, pdf, count ->
                                    imageUri = image
                                    pdfUri = pdf
                                    pageCount = count
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun shareDocument(context: Context, uri: Uri, mimeType: String = "image/jpeg") {
    try {
        // Convert file URI to content URI if needed
        val contentUri = if (uri.scheme == "file") {
            val file = File(uri.path!!)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            uri
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Scanned Document")
            putExtra(Intent.EXTRA_TEXT, "Please find the scanned document attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Create chooser to let user pick Gmail or other apps
        val chooserIntent = Intent.createChooser(shareIntent, "Share Document")
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
        // Handle error gracefully
    }
}

@Composable
fun MainScreen(
    navController: NavController,
    imageUri: Uri?,
    pdfUri: Uri?,
    pageCount: Int
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (imageUri != null || pdfUri != null) {
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Scanned Document Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pageCount > 0) {
                Text(text = "$pageCount page(s) scanned")
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Share image button
            if (imageUri != null) {
                Button(
                    onClick = { shareDocument(context, imageUri, "image/jpeg") },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(text = "Share Image (First Page)")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Share PDF button
            if (pdfUri != null) {
                Button(
                    onClick = { shareDocument(context, pdfUri, "application/pdf") },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(text = "Share PDF (All Pages)")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            Text(text = "No document scanned yet.")
        }

        Button(
            onClick = { navController.navigate("scanner") },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Scan Document")
        }
    }
}

@Composable
fun ScannerScreen(
    activity: ComponentActivity,
    onResult: (imageUri: Uri?, pdfUri: Uri?, pageCount: Int) -> Unit
) {
    val options = GmsDocumentScannerOptions.Builder()
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .setGalleryImportAllowed(true)
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
            GmsDocumentScannerOptions.RESULT_FORMAT_PDF
        )
        .setPageLimit(50) // Allow up to 50 pages
        .build()

    val scanner = GmsDocumentScanning.getClient(options)
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                scanningResult?.let {
                    val imageUri = it.pages?.firstOrNull()?.imageUri
                    val pdfUri = it.pdf?.uri
                    val pageCount = it.pages?.size ?: 0
                    onResult(imageUri, pdfUri, pageCount)
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                // Handle failure
            }
    }
}
