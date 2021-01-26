package com.blood.x264_rtmp

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.Builder
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.blood.x264_rtmp.R.layout
import com.blood.x264_rtmp.databinding.ActivityX264RtmpBinding
import com.blood.x264_rtmp.push.LivePusher
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class X264RtmpActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "X264RtmpActivity"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    private lateinit var binding: ActivityX264RtmpBinding
    private lateinit var cameraExecutor: ExecutorService

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private val url = ""
    private lateinit var livePusher: LivePusher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, layout.activity_x264_rtmp)
        init()

        // 宽高随便设，以相机尺寸为准
        livePusher = LivePusher(0, 0, 800_000, 10)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        livePusher.stopLive()
    }

    private fun init() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        binding.btnSwitch.isEnabled = false
        binding.btnSwitch.setOnClickListener {
            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            bindCameraUseCases()
        }
        binding.btnStartLive.setOnClickListener { livePusher.startLive(url) }
        binding.btnStopLive.setOnClickListener { livePusher.stopLive() }
        startCamera()
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    private fun updateCameraSwitchButton() {
        try {
            binding.btnSwitch.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            binding.btnSwitch.isEnabled = false
        }
    }

    private fun startCamera() {
        // ProcessCameraProvider 用于将摄像机的生命周期绑定到生命周期所有者。
        // 由于CameraX具有生命周期感知功能，因此省去了打开和关闭相机的任务。
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        // 将侦听器添加到中cameraProviderFuture
        cameraProviderFuture.addListener({

            cameraProvider = cameraProviderFuture.get()

            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            updateCameraSwitchButton()

            bindCameraUseCases()

        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        // 前后摄像头
        val cameraSelector = Builder().requireLensFacing(lensFacing).build()

        val metrics = DisplayMetrics().also { binding.previewView.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
        livePusher.videoChannel?.onSizeChanged(metrics.widthPixels, metrics.heightPixels)

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = binding.previewView.display.rotation

        // 从取景器中获取表面提供程序，然后在预览中进行设置。
        val preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

        imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { y: ByteArray, u: ByteArray, v: ByteArray, width: Int, height: Int, rowStride: Int ->
                        livePusher.videoChannel?.analyzeFrameData(y, u, v, width, height, rowStride)
                    })
                }

        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private class LuminosityAnalyzer(private val listener: (y: ByteArray, u: ByteArray, v: ByteArray, width: Int, height: Int, rowStride: Int) -> Unit) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {
            val y = image.planes[0].buffer.toByteArray()
            val u = image.planes[1].buffer.toByteArray()
            val v = image.planes[2].buffer.toByteArray()
            listener(y, u, v, image.width, image.height, image.planes[0].rowStride)
            image.close()
        }
    }

}