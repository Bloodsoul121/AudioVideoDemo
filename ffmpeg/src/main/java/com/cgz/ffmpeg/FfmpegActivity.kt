package com.cgz.ffmpeg

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FfmpegActivity : AppCompatActivity() {

    companion object {
        init {
            System.loadLibrary("ffmpeg-lib")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ffmpeg)
        findViewById<TextView>(R.id.tv).text = testFfmpeg()
    }

    private external fun testFfmpeg(): String

}