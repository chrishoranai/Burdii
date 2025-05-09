package com.app.burdii

data class Round(
    val name: String,
    val date: String,
    val scoreChange: String,
    val holesPlayed: String,
    val isComplete: Boolean,
    val currentHole: Int = 1 // Default to first hole if not specified
)
