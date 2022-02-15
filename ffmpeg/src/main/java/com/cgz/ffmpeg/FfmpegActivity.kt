package com.cgz.ffmpeg

import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import com.cgz.ffmpeg.databinding.ActivityFfmpegBinding
import com.cgz.ffmpeg.util.AssetsUtil
import java.io.File

class FfmpegActivity : AppCompatActivity(), SurfaceHolder.Callback {

    companion object {
        init {
            System.loadLibrary("ffmpeg-lib")
        }
        private const val TAG = "FfmpegActivity"
    }

    private lateinit var binding: ActivityFfmpegBinding
    private lateinit var url: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFfmpegBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val saveDir = File(filesDir, "ffmpeg").absolutePath
//        AssetsUtil.copyFileFromAssets(this, "ffmpeg/hot.mp4", saveDir, "ffmpeg.mp4")
        AssetsUtil.copyFileFromAssets(this, "input2.mp4", saveDir, "ffmpeg.mp4")
        url = File(saveDir, "ffmpeg.mp4").absolutePath

        binding.tv.text = testFfmpeg()
        binding.surface.holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceCreated: url >> $url")
        play(url, holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    private external fun testFfmpeg(): String

    private external fun play(url: String, surface: Surface): Int

}