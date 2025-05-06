package com.app.burdii

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * SplashActivity - Entry point for the Burdii disc golf scorekeeping app
 * Displays the app logo and transitions to the HomeActivity after a short delay
 */
class SplashActivity : AppCompatActivity() {
    private val SPLASH_DELAY = 4000L // 4 seconds delay
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Delayed transition to HomeActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish() // Close SplashActivity so it's not kept in the back stack
            // Apply fade in/out animation transition
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }, SPLASH_DELAY)
    }
}
