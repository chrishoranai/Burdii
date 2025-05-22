package com.app.burdii.data.models.firebase

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    @get:PropertyName("uid") @set:PropertyName("uid") var uid: String = "",
    @get:PropertyName("displayName") @set:PropertyName("displayName") var displayName: String? = null,
    @get:PropertyName("email") @set:PropertyName("email") var email: String? = null,
    @get:PropertyName("hostedLeagues") @set:PropertyName("hostedLeagues") var hostedLeagues: List<String> = listOf(), // List of league IDs
    @get:PropertyName("joinedLeagues") @set:PropertyName("joinedLeagues") var joinedLeagues: List<String> = listOf(), // List of league IDs
    @ServerTimestamp @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Date? = null
) {
    // No-argument constructor for Firestore deserialization
    constructor() : this("", null, null, listOf(), listOf(), null)
}