package com.example.app

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView

class SplashActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var launchRunnable: Runnable? = null
    private var rotationAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val wheelImage = findViewById<ImageView>(R.id.splashRouletteWheel)
        rotationAnimator = ObjectAnimator.ofFloat(wheelImage, View.ROTATION, 0f, 360f).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        launchRunnable = Runnable {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }.also {
            handler.postDelayed(it, 1500L)
        }
    }

    override fun onDestroy() {
        launchRunnable?.let(handler::removeCallbacks)
        launchRunnable = null
        rotationAnimator?.cancel()
        rotationAnimator = null
        super.onDestroy()
    }
}
