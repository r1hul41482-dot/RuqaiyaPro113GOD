package com.ruqaiyapro.debug.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.ruqaiyapro.debug.core.Core
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.Executors

private fun facesDir(ctx: android.content.Context): File = File(ctx.filesDir, "faces").apply { mkdirs() }

private fun listFaces(ctx: android.content.Context): List<File> =
    (facesDir(ctx).listFiles()?.filter { it.extension == "jpg" } ?: emptyList()).sortedByDescending { it.lastModified() }

private fun loadThumb(path: String): Bitmap? = try {
    val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
    val bmp = BitmapFactory.decodeFile(path, opts)
    val deg = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (bmp != null && deg != 0f) Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true) else bmp
} catch (e: Throwable) { null }

@Composable
fun FaceScreen(core: Core, activity: ComponentActivity) {
    val ctx = LocalContext.current
    var hasCam by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCam = it }

    var cameraOn by remember { mutableStateOf(false) }
    var front by remember { mutableStateOf(true) }
    var autoCapture by remember { mutableStateOf(false) }
    var faceCount by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("Camera off") }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var gallery by remember { mutableStateOf(listFaces(ctx)) }
    var ownerInput by remember { mutableStateOf(core.ownerName) }

    val previewView = remember { PreviewView(ctx) }

    // Bind / unbind CameraX
    DisposableEffect(cameraOn, front, hasCam) {
        var provider: ProcessCameraProvider? = null
        val executor = Executors.newSingleThreadExecutor()
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build()
        )
        if (cameraOn && hasCam) {
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                try {
                    val p = future.get()
                    provider = p
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    var lastTs = 0L
                    analysis.setAnalyzer(executor) { proxy ->
                        val now = System.currentTimeMillis()
                        val media = proxy.image
                        if (media == null || now - lastTs < 400) {
                            proxy.close()
                        } else {
                            lastTs = now
                            val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                            detector.process(img)
                                .addOnSuccessListener { faces -> faceCount = faces.size }
                                .addOnCompleteListener { proxy.close() }
                        }
                    }
                    val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    p.unbindAll()
                    try {
                        p.bindToLifecycle(activity, selector, preview, capture, analysis)
                        status = "Camera ON - face detect active"
                    } catch (e: Exception) {
                        // Some low-end cameras cannot run 3 streams: fall back to preview + capture only
                        p.unbindAll()
                        p.bindToLifecycle(activity, selector, preview, capture)
                        status = "Camera ON (face detect unsupported on this camera mode)"
                    }
                    imageCapture = capture
                } catch (e: Exception) {
                    status = "Camera error: ${e.message}"
                }
            }, ContextCompat.getMainExecutor(ctx))
        } else {
            status = if (hasCam) "Camera off" else "Camera permission lagbe"
        }
        onDispose {
            try { provider?.unbindAll() } catch (e: Exception) { }
            imageCapture = null
            faceCount = 0
            try { detector.close() } catch (e: Exception) { }
            executor.shutdown()
        }
    }

    fun capture(name: String, onDone: () -> Unit = {}) {
        val cap = imageCapture
        if (cap == null) { toast(ctx, "Age camera ON koro"); return }
        val file = File(facesDir(ctx), name)
        cap.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(ctx),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    gallery = listFaces(ctx)
                    onDone()
                }
                override fun onError(exception: ImageCaptureException) {
                    toast(ctx, "Capture fail: ${exception.message}")
                }
            }
        )
    }

    // Auto capture every 15 s, only if a face is visible (max 20 auto photos kept)
    LaunchedEffect(autoCapture, cameraOn) {
        while (autoCapture && cameraOn) {
            delay(15_000)
            if (faceCount > 0) {
                capture("auto_${System.currentTimeMillis()}.jpg")
                listFaces(ctx).filter { it.name.startsWith("auto_") }.drop(20).forEach { it.delete() }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RCard("Camera & Face Detection") {
                Box(
                    modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0A0A0A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraOn && hasCam) {
                        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("Camera preview ekhane dekhabe", color = RC.TextSec, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(status, color = RC.TextSec, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    if (cameraOn) StatusPill(if (faceCount > 0) "FACE x$faceCount" else "NO FACE", if (faceCount > 0) RC.Green else RC.Amber)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RButton(if (cameraOn) "Camera OFF" else "Camera ON", onClick = {
                        if (!hasCam) permLauncher.launch(Manifest.permission.CAMERA) else cameraOn = !cameraOn
                    }, modifier = Modifier.weight(1f))
                    RButton("Switch", onClick = { front = !front }, outlined = true, enabled = cameraOn, modifier = Modifier.weight(1f))
                    RButton("Capture", onClick = { capture("face_${System.currentTimeMillis()}.jpg") }, outlined = true, enabled = cameraOn, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto capture every 15s (face thakle)", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Switch(checked = autoCapture, onCheckedChange = { autoCapture = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = RC.Green, checkedThumbColor = Color.Black))
                }
            }
        }

        item {
            RCard("Owner (Boss)") {
                RField(ownerInput, { ownerInput = it }, "Owner name")
                Spacer(Modifier.height(8.dp))
                RButton("Amake Mone Rakho ${ownerInput.ifBlank { "Boss" }}", onClick = {
                    val name = ownerInput.ifBlank { "Rubel" }
                    core.setOwnerName(name)
                    capture("${name.lowercase().replace(" ", "_")}.jpg") {
                        toast(ctx, "$name er chobi save holo")
                        core.logHud("Owner photo saved: $name")
                        core.logActivity("FACE", "Owner photo saved", name, "SUCCESS")
                    }
                }, enabled = cameraOn, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text("Note: ekhon sudhu face detect + chobi save hoy. Boss/Unknown pechano (identity match) ekhono nei - eta alada face model lage.",
                    color = RC.TextSec, fontSize = 11.sp)
            }
        }

        item {
            RCard("Face Gallery (${gallery.size})") {
                if (gallery.isEmpty()) {
                    Text("Kono chobi nei", color = RC.TextSec, fontSize = 12.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(gallery.take(30), key = { it.path }) { f ->
                            val bmp = remember(f.path, f.lastModified()) { loadThumb(f.path) }
                            if (bmp != null) {
                                Image(bitmap = bmp.asImageBitmap(), contentDescription = f.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(84.dp).clip(RoundedCornerShape(10.dp)))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    RButton("Clear gallery", onClick = {
                        listFaces(ctx).forEach { it.delete() }
                        gallery = listFaces(ctx)
                    }, outlined = true)
                }
            }
        }
    }
}
