package com.reishandy.guestbook.data

import android.content.Context
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

// INFO: I Don't know how this code functions but I don't care, as long as it works
fun startQRCodeAnalyzer(
    context: Context,
    cameraController: LifecycleCameraController,
    previewView: PreviewView,
    onQrCodeScanned: (String) -> Unit
) {
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val barcodeScanner = BarcodeScanning.getClient(options)

    val imageAnalyzer = ImageAnalysis.Builder()
        .setTargetResolution(android.util.Size(1280, 720))
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(context), ImageAnalysis.Analyzer { imageProxy ->
        processImageProxy(barcodeScanner, imageProxy, onQrCodeScanned)
    })

    cameraController.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context), ImageAnalysis.Analyzer { imageProxy ->
        processImageProxy(barcodeScanner, imageProxy, onQrCodeScanned)
    })
    cameraController.bindToLifecycle(context as androidx.lifecycle.LifecycleOwner)
    previewView.controller = cameraController
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onQrCodeScanned: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { onQrCodeScanned(it) }
                }
            }
            .addOnFailureListener {
                // Handle failure
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}