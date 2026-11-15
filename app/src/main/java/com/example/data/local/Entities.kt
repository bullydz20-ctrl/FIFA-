package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.data.model.AdminRole
import com.example.data.model.DrawMode
import com.example.data.model.KnockoutTieRule
import com.example.data.model.MatchStage
import com.example.data.model.PlayerLevel
import com.example.data.model.PlayerStatus
import com.example.data.model.TournamentFormat
import com.example.data.model.TournamentStatus

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val bannerUri: String? = null,
    val startDateTime: String,
    val maxParticipants: Int = 16,
    val format: TournamentFormat = TournamentFormat.GROUPS_KNOCKOUT,
    val groupCount: Int = 4,
    val qualifiersPerGroup: Int = 2,
    val drawMode: DrawMode = DrawMode.RANDOM,
    val knockoutTieRule: KnockoutTieRule = KnockoutTieRule.SINGLE_MATCH_PENALTIES,
    val matchDurationMinutes: Int = 12,
    val targetGoalsRule: String = "المباراة الكاملة (6 دقائق لكل شوط)",
    val isRegistrationClosedManual: Boolean = false,
    val status: TournamentStatus = TournamentStatus.REGISTRATION_OPEN,
    val championPlayerId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tournamentId"]),
        Index(value = ["tournamentId", "elysiumNetId"], unique = true),
        Index(value = ["groupId"])
    ]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tournamentId: Long,
    val name: String,
    val elysiumNetId: String,
    val avatarUri: String? = null,
    val level: PlayerLevel = PlayerLevel.INTERMEDIATE,
    val ovrRating: Int = 81,
    val clubName: String = "",
    val status: PlayerStatus = PlayerStatus.PENDING,
    val groupId: Long? = null,
    val seedOrder: Int = 0,
    val registeredAt: Long = System.currentTimeMillis()
)

typealias ParticipantEntity = PlayerEntity

@Entity(
    tableName = "tournament_groups",
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tournamentId"])
    ]
)
data class TournamentGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tournamentId: Long,
    val name: String,
    val displayOrder: Int
)

@Entity(
    tableName = "matches",
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tournamentId"]),
        Index(value = ["groupId"]),
        Index(value = ["player1Id"]),
        Index(value = ["player2Id"])
    ]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tournamentId: Long,
    val stage: MatchStage,
    val groupId: Long? = null,
    val roundNumber: Int = 1,
    val bracketIndex: Int = 0,
    val player1Id: Long? = null,
    val player2Id: Long? = null,
    val leg1Score1: Int? = null,
    val leg1Score2: Int? = null,
    val leg2Score1: Int? = null,
    val leg2Score2: Int? = null,
    val penaltyScore1: Int? = null,
    val penaltyScore2: Int? = null,
    val isTwoLegged: Boolean = false,
    val isCompleted: Boolean = false,
    val winnerId: Long? = null,
    val nextMatchId: Long? = null,
    val nextMatchSlot: Int = 1,
    val scheduledLabel: String = "",
    val scheduledAtMillis: Long? = null
)

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val username: String,
    val displayName: String,
    val role: AdminRole,
    val pinHash: String,
    val pinSalt: String,
    val canManageTournament: Boolean = false,
    val canManagePlayers: Boolean = false,
    val canRunDraw: Boolean = false,
    val canEditScores: Boolean = false,
    val canCrownChampion: Boolean = false,
    val permissionsHash: String = "",
    val pinUpdatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val lastLoginAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

typealias AdminAccountEntity = UserEntity

data class TournamentWithDetails(
    @Embedded val tournament: TournamentEntity,
    @Relation(parentColumn = "id", entityColumn = "tournamentId")
    val participants: List<PlayerEntity>,
    @Relation(parentColumn = "id", entityColumn = "tournamentId")
    val groups: List<TournamentGroupEntity>,
    @Relation(parentColumn = "id", entityColumn = "tournamentId")
    val matches: List<MatchEntity>
)

@Entity(
    tableName = "match_reminders",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["matchId"], unique = true),
        Index(value = ["tournamentId"])
    ]
)
data class MatchReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val matchId: Long,
    val tournamentId: Long,
    val matchTitle: String,
    val matchStageLabel: String,
    val scheduledAtMillis: Long,
    val remindAtMillis: Long,
    val leadMinutes: Int,
    val isEnabled: Boolean = true,
    val isDelivered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
