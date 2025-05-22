// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // ... other classpath dependencies
        classpath("com.google.gms:google-services:4.4.1") // Check for the latest version
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // Removed the apply false for google.gms.google.services here as we are using classpath
}
