package com.veggievision.lokatani.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.GsonBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.veggievision.lokatani.databinding.FragmentCameraBinding
import com.veggievision.lokatani.detection.BoundingBox
import com.veggievision.lokatani.detection.Model.LABELS_PATH
import com.veggievision.lokatani.detection.Model.MODEL_PATH
import com.veggievision.lokatani.detection.ObjectDetectorHelper
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: ObjectDetectorHelper
    private lateinit var textRecognizer: TextRecognizer

    private var capturedBitmap: Bitmap? = null
    private var detectedBoundingBoxes: List<BoundingBox> = emptyList()
    private var recognizedText: String? = null
    private var rawJsonResult: String? = null

    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.CAMERA] == true) {
                startCamera()
            } else {
                Log.e(TAG, "ko permissionnya ditolak coek")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        detector = ObjectDetectorHelper(requireContext(), MODEL_PATH, LABELS_PATH, this)
        detector.setup()

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.captureButton.setOnClickListener {
            captureImage()
        }
        binding.overlay.visibility = View.GONE
        binding.resetButton.visibility = View.GONE

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            capturedBitmap = null
            detectedBoundingBoxes = emptyList()
            recognizedText = null
            rawJsonResult = null
            resetUI()
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun resetUI() {
        binding.captureButton.isEnabled = true
        showLoading(false)
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        try {
            cameraProvider.unbindAll()

            val rotation = binding.viewFinder.display.rotation

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            preview = Preview.Builder()
                .setTargetRotation(rotation)
                .build()

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(rotation)
                .build()

            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            binding.captureButton.isEnabled = true

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            binding.captureButton.isEnabled = false
        }
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return

        capturedBitmap = null
        detectedBoundingBoxes = emptyList()
        recognizedText = null
        rawJsonResult = null
        showLoading(true)
        binding.captureButton.isEnabled = false

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    capturedBitmap = bitmap
                    detector.detect(bitmap)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "gagal poto ${exception.message}", exception)
                    showLoading(false)
                    binding.captureButton.isEnabled = true
                }
            }
        )
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f)
            }
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun processTextRecognition(bitmap: Bitmap, boundingBox: BoundingBox) {
        val imgWidth = bitmap.width
        val imgHeight = bitmap.height

        val x1 = (boundingBox.x1 * imgWidth).toInt().coerceIn(0, imgWidth)
        val y1 = (boundingBox.y1 * imgHeight).toInt().coerceIn(0, imgHeight)
        val x2 = (boundingBox.x2 * imgWidth).toInt().coerceIn(0, imgWidth)
        val y2 = (boundingBox.y2 * imgHeight).toInt().coerceIn(0, imgHeight)

        if (x2 <= x1 || y2 <= y1) {
            Log.e(TAG, "Invalid bounding box dimensions")
            navigateToResultActivity()
            return
        }

        val width = x2 - x1
        val height = y2 - y1

        try {
            val croppedBitmap = Bitmap.createBitmap(bitmap, x1, y1, width, height)

            val inputImage = InputImage.fromBitmap(croppedBitmap, 0)

            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    recognizedText = visionText.text
                    Log.d(TAG, "isi teks : $recognizedText")
                    navigateToResultActivity()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "gagal baca teks ${e.message}")
                    navigateToResultActivity()
                }
        } catch (e: Exception) {
            Log.e(TAG, "eror baca teks ${e.message}")
            navigateToResultActivity()
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): Uri {
        val filename = "captured_image_${System.currentTimeMillis()}.png"
        val file = File(requireContext().cacheDir, filename)

        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
        }

        return Uri.fromFile(file)
    }

    private fun createJsonResult(): String {
        try {
            data class Detection(
                val `class`: String,
                val confidence: Float,
                val x1: Float,
                val y1: Float,
                val x2: Float,
                val y2: Float
            )

            data class ResultData(
                val detections: List<Detection>,
                val recognizedText: String?,
                val timestamp: Long
            )

            val detectionsList = detectedBoundingBoxes.map { box ->
                Detection(
                    `class` = box.clsName,
                    confidence = box.cnf,
                    x1 = box.x1,
                    y1 = box.y1,
                    x2 = box.x2,
                    y2 = box.y2
                )
            }

            val resultData = ResultData(
                detections = detectionsList,
                recognizedText = recognizedText,
                timestamp = System.currentTimeMillis()
            )

            val gson = GsonBuilder().setPrettyPrinting().create()
            return gson.toJson(resultData)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating JSON with Gson: ${e.message}")
            return "{\"error\":\"Failed to create JSON\"}"
        }
    }

    private fun navigateToResultActivity() {
        capturedBitmap?.let { bitmap ->
            val fileUri = saveBitmapToFile(bitmap)

            val detectedClasses = ArrayList<String>()
            val confidenceValues = FloatArray(detectedBoundingBoxes.size)

            for (i in detectedBoundingBoxes.indices) {
                val box = detectedBoundingBoxes[i]
                detectedClasses.add(box.clsName)
                confidenceValues[i] = box.cnf
            }

            rawJsonResult = createJsonResult()

            val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                putExtra("captured_image_uri", fileUri.toString())
                putStringArrayListExtra("detected_classes", detectedClasses)
                putExtra("confidence_values", confidenceValues)
                putExtra("raw_json_result", rawJsonResult)
                recognizedText?.let { text ->
                    putExtra("recognized_text", text)
                }
            }

            startActivity(intent)

            showLoading(false)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onEmptyDetect() {
        activity?.runOnUiThread {
            navigateToResultActivity()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        detectedBoundingBoxes = boundingBoxes

        activity?.runOnUiThread {
            Log.d(TAG, "Detection completed in ${inferenceTime}ms")

            val beratBox = boundingBoxes.find { it.clsName.equals("berat", ignoreCase = true) }

            if (beratBox != null && capturedBitmap != null) {
                processTextRecognition(capturedBitmap!!, beratBox)
            } else {
                navigateToResultActivity()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        detector.clear()
        _binding = null
        Log.d(TAG, "kameraactivity meninggal")
    }

    override fun onResume() {
        super.onResume()

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        } else {
            startCamera()
        }
    }

    override fun onStop() {
        super.onStop()
        cameraProvider?.unbindAll()
    }

    companion object {
        private const val TAG = "CameraFragment"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
