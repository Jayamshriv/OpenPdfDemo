package com.example.openpdfdemo

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.R
import android.app.Activity
import android.app.appsearch.SetSchemaRequest.READ_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.openpdfdemo.ui.theme.OpenPdfDemoTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings
import androidx.compose.runtime.setValue
import androidx.core.os.BuildCompat

val REQUEST_CODE = 100

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            var count by remember {
                mutableStateOf(0)
            }

            OpenPdfDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    checkStoragePermission(context, )
                    val cameraPermissionState = rememberMultiplePermissionsState(
                        permissions =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            listOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO
                            )
//                        }
//                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                            listOf(
//                                Manifest.permission.MANAGE_EXTERNAL_STORAGE
//                            )
                        } else {
                            listOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        }
                    )

                    Button(
                        onClick = {
                            if (cameraPermissionState.allPermissionsGranted) {
                                savePdfToDownloads(context, "invoice$count")
                                count++
                            } else {
                                cameraPermissionState.launchMultiplePermissionRequest()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    // Android 11 and above
                                    if (Environment.isExternalStorageManager()) {
                                        // Permission is granted, proceed
                                    } else {
                                        // Request permission
                                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                        intent.data = Uri.parse("package:${context.packageName}")
                                        startActivity(intent)
                                    }
                                } else {
                                    // Android 10 and below
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                        // Permission is granted, proceed
                                    } else {
                                        ActivityCompat.requestPermissions(
                                            this,
                                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                            REQUEST_CODE
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(vertical = 50.dp)
                    ) {
                        Text(text = "Generate PDF")
                    }

                    if (cameraPermissionState.allPermissionsGranted) {
                        Text("Permissions Granted")
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            var textToShow = if (cameraPermissionState.shouldShowRationale) {
                                "The app needs these permissions to function correctly. Please grant them."
                            } else {
                                "Permissions required. Please grant them."
                            }
                            RequestStoragePermission {
                                textToShow = " granted"
                            }
                            Text(textToShow)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestStoragePermission(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                Toast.makeText(context, "Permission Not Granted", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(key1 = Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            onPermissionGranted()
        }
    }
}


fun savePdfToDownloads(context: Context, baseFileName: String) {
    // Get the Downloads directory
    val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    // Define the "My Invoices" directory
    val invoicesDirectory = File(downloadsDirectory, "My Invoices")

    // Check if the directory exists, if not, create it
    if (!invoicesDirectory.exists()) {
        invoicesDirectory.mkdirs()
    }

    // Generate a unique file name
    var fileName = baseFileName
    var file = File(invoicesDirectory, "$fileName.pdf")
    var counter = 1

    while (file.exists()) {
        fileName = "$baseFileName($counter)"
        file = File(invoicesDirectory, "$fileName.pdf")
        counter++
    }

    // Define the file path for the PDF
    val pdfFile = file

    // Create a new PDF document
    val pdfDocument = PdfDocument()

    // Start a page
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
    val page = pdfDocument.startPage(pageInfo)

    // Get the Canvas from the page
    val canvas: Canvas = page.canvas
    val paint = Paint()

    // Add Header
    paint.textSize = 20f
    canvas.drawText("Company Name", 50f, 50f, paint)  // Company name or title
    paint.textSize = 12f
    canvas.drawText("Invoice", 50f, 70f, paint) // Invoice title

    // Add an Image (e.g., a logo)
    val logoBitmap: Bitmap? = BitmapFactory.decodeResource(context.resources, R.drawable.star_on)  // Replace with your logo resource
    logoBitmap?.let {
        canvas.drawBitmap(it, 450f, 30f, paint)  // Position the logo in the header
    }

    // Draw some lines (Header and Footer separation)
    canvas.drawLine(0f, 100f, 595f, 100f, paint) // Line under the header
    canvas.drawLine(0f, 780f, 595f, 780f, paint) // Line above the footer

    // Add Footer
    paint.textSize = 12f
    canvas.drawText("Powered by Attendify", 50f, 820f, paint)  // Contact information

    // Add Content (Items, Prices, etc.)
    paint.textSize = 16f
    canvas.drawText("Fees Receipt for Member Ship ", 50f, 150f, paint)
    canvas.drawText("Item 2: 200", 50f, 200f, paint)

    // Finish the page
    pdfDocument.finishPage(page)

    Log.d("@@@", "File generated: ${pdfFile.absolutePath}")

    // Write the document content to the file
    try {
        FileOutputStream(pdfFile).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
    } catch (e: IOException) {
        e.printStackTrace()
        // Handle the error, e.g., show a message to the user
    } finally {
        pdfDocument.close()
    }
}

//fun openPdf(context: Context, filePath: String) {
//    val file = File(filePath)
//    val uri: Uri = FileProvider.getUriForFile(
//        context,
//        "${}.provider",
//        file
//    )
//    val intent = Intent(Intent.ACTION_VIEW).apply {
//        setDataAndType(uri, "application/pdf")
//        flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
//    }
//    context.startActivity(intent)
//}



/*    private fun generatePDF() {
        // creating an object variable for our PDF document.
        val pdfDocument = PdfDocument()

        // two variables for paint "paint" is used for drawing shapes and we will use "title" for adding text in our PDF file.
        val paint = Paint()
        val title = Paint()

        // adding page info to our PDF file in which we will be passing our pageWidth, pageHeight and number of pages and after that we are calling it to create our PDF.
        val mypageInfo = PdfDocument.PageInfo.Builder(pagewidth, pageHeight, 1).create()

        // setting start page for our PDF file.
        val myPage = pdfDocument.startPage(mypageInfo)

        // creating a variable for canvas from our page of PDF.
        val canvas = myPage.canvas

        if (scaledbmp != null) {
            // drawing our image on our PDF file.
            canvas.drawBitmap(scaledbmp!!, 56f, 40f, paint)
        } else {
            Log.e("generatePDF", "Bitmap is null. Skipping bitmap drawing.")
        }

        // adding typeface for our text which we will be adding in our PDF file.
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))

        // setting text size which we will be displaying in our PDF file.
        title.textSize = 15F

        // setting color of our text inside our PDF file.
        title.setColor(ContextCompat.getColor(this, R.color.black))

        // drawing text in our PDF file.
        canvas.drawText("A portal for IT professionals.", 209f, 100f, title)
        canvas.drawText("Geeks for Geeks", 209f, 80f, title)

        // creating another text and in this we are aligning this text to center of our PDF file.
        title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        title.setColor(ContextCompat.getColor(this, R.color.black))
        title.textSize = 15f

        // setting our text to center of PDF.
        title.textAlign = Paint.Align.CENTER
        canvas.drawText("This is sample document which we have created.", 396f, 560f, title)

        // finishing our page.
        pdfDocument.finishPage(myPage)

        // setting the name of our PDF file and its path.
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "GFG.pdf"
        )

        try {
            // writing our PDF file to that location.
            pdfDocument.writeTo(FileOutputStream(file))

            // printing toast message on completion of PDF generation.
            Toast.makeText(
                this@MainActivity,
                "PDF file generated successfully.",
                Toast.LENGTH_SHORT
            )
                .show()
        } catch (e: IOException) {
            // handling error
            e.printStackTrace()
            Toast.makeText(this@MainActivity, "Failed to generate PDF file.", Toast.LENGTH_SHORT)
                .show()
        }

        // closing our PDF file.
        pdfDocument.close()
    }


    @Composable
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermission(): Boolean {
        val permission1 =
            ContextCompat.checkSelfPermission(applicationContext, WRITE_EXTERNAL_STORAGE)
        val permission2 =
            ContextCompat.checkSelfPermission(
                applicationContext,
                "android.Manifest.permission.READ_EXTERNAL_STORAGE"
            )

        return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPermission() {
        Log.e("@@@@", "Permission Requested")
        ActivityCompat.requestPermissions(
            this,
            arrayOf(WRITE_EXTERNAL_STORAGE, "android.Manifest.permission.READ_EXTERNAL_STORAGE"),
            PERMISSION_REQUEST_CODE
        )
    }
*/


