package com.app.burdii

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView

/**
 * SplashActivity - Entry point for the Burdii disc golf scorekeeping app
 * Displays the app logo and transitions to the HomeActivity after a short delay
 */
class SplashActivity : AppCompatActivity() {
    private val SPLASH_DELAY = 2000L // 2 seconds delay
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Set up animation for logo and title if needed
        val logoImageView = findViewById<ImageView>(R.id.splashLogoImageView)
        val titleTextView = findViewById<TextView>(R.id.splashTitleTextView)
        
        // Simple animation: fade in or scale up could be added here
        
        // Delayed transition to HomeActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish() // Close SplashActivity so it's not kept in the back stack
        }, SPLASH_DELAY)
    }
}
