package com.app.burdii

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * SplashActivity - Entry point for the Burdii disc golf scorekeeping app
 * Displays the app logo and transitions to the HomeActivity after a short delay
 */
class SplashActivity : AppCompatActivity() {
    companion object {
        private const val SPLASH_DELAY = 3000L // 3 seconds delay
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Check authentication status and navigate appropriately
        lifecycleScope.launch {
            checkAuthAndNavigate()
        }
    }
    
    private suspend fun checkAuthAndNavigate() {
        // Show splash for minimum time
        Handler(Looper.getMainLooper()).postDelayed({
            if (!AuthManager.isUserSignedIn()) {
                // Sign in anonymously for basic functionality
                lifecycleScope.launch {
                    val result = AuthManager.signInAnonymously()
                    navigateToHome(result.isSuccess)
                }
            } else {
                navigateToHome(true)
            }
        }, SPLASH_DELAY)
    }
    
    private fun navigateToHome(isSignedIn: Boolean = false) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("IS_SIGNED_IN", isSignedIn)
        startActivity(intent)
        finish() // Close SplashActivity so it's not kept in the back stack
        
        // Apply fade in/out animation transition
        applyTransitionAnimation()
    }
    
    private fun applyTransitionAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN, 
                R.anim.fade_in, 
                R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }
}

