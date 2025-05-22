package com.app.burdii.data.models.firebase

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class LeagueScore(
    @get:PropertyName("scoreId") @set:PropertyName("scoreId") var scoreId: String = "", // Document ID
    @get:PropertyName("leagueId") @set:PropertyName("leagueId") var leagueId: String = "",
    @get:PropertyName("playerId") @set:PropertyName("playerId") var playerId: String = "", // UID of the player
    @get:PropertyName("playerName") @set:PropertyName("playerName") var playerName: String = "", // Display name of player for convenience
    @get:PropertyName("playerNameForManualEntry") @set:PropertyName("playerNameForManualEntry") var playerNameForManualEntry: String? = null, // If manually added
    @get:PropertyName("weekNumber") @set:PropertyName("weekNumber") var weekNumber: Int = 0,
    @get:PropertyName("scoreValue") @set:PropertyName("scoreValue") var scoreValue: Int = 0,
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "pending", // "pending", "approved", "denied"
    @ServerTimestamp @get:PropertyName("submittedAt") @set:PropertyName("submittedAt") var submittedAt: Date? = null,
    @get:PropertyName("reviewedBy") @set:PropertyName("reviewedBy") var reviewedBy: String? = null, // hostId
    @ServerTimestamp @get:PropertyName("reviewedAt") @set:PropertyName("reviewedAt") var reviewedAt: Date? = null
) {
    // No-argument constructor for Firestore deserialization
    constructor() : this("", "", "", "", null, 0, 0, "pending", null, null, null)
}