package com.app.burdii.data.models.firebase

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class League(
    @get:PropertyName("leagueId") @set:PropertyName("leagueId") var leagueId: String = "", // Document ID
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("hostId") @set:PropertyName("hostId") var hostId: String = "", // UID of the host
    @get:PropertyName("hostName") @set:PropertyName("hostName") var hostName: String = "", // Display name of host for convenience
    @get:PropertyName("accessCode") @set:PropertyName("accessCode") var accessCode: String = "",
    @get:PropertyName("numberOfWeeks") @set:PropertyName("numberOfWeeks") var numberOfWeeks: Int = 0,
    @get:PropertyName("members") @set:PropertyName("members") var members: List<String> = listOf(), // List of member UIDs
    @get:PropertyName("memberNames") @set:PropertyName("memberNames") var memberNames: Map<String, String> = mapOf(), // Map of UID to DisplayName for convenience
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "active", // e.g., "active", "archived"
    @ServerTimestamp @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Date? = null
) {
    // No-argument constructor for Firestore deserialization
    constructor() : this("", "", "", "", "", 0, listOf(), mapOf(), "active", null)
}