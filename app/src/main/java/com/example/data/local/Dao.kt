package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestTournament(): Flow<TournamentEntity?>

    @Transaction
    @Query("SELECT * FROM tournaments WHERE id = :tournamentId LIMIT 1")
    fun observeTournamentWithDetails(tournamentId: Long): Flow<TournamentWithDetails?>

    @Query("SELECT * FROM tournaments ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestTournament(): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity): Long

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    @Query("DELETE FROM tournaments WHERE id = :id")
    suspend fun deleteTournament(id: Long)
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId ORDER BY registeredAt ASC")
    fun observePlayers(tournamentId: Long): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId ORDER BY registeredAt ASC")
    suspend fun getPlayers(tournamentId: Long): List<PlayerEntity>

    @Query("SELECT COUNT(*) FROM players WHERE tournamentId = :tournamentId AND LOWER(elysiumNetId) = LOWER(:elysiumId)")
    suspend fun countByElysiumId(tournamentId: Long, elysiumId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Update
    suspend fun updatePlayers(players: List<PlayerEntity>)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)

    @Query("DELETE FROM players WHERE tournamentId = :tournamentId")
    suspend fun deletePlayersForTournament(tournamentId: Long)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM tournament_groups WHERE tournamentId = :tournamentId ORDER BY displayOrder ASC")
    fun observeGroups(tournamentId: Long): Flow<List<TournamentGroupEntity>>

    @Query("SELECT * FROM tournament_groups WHERE tournamentId = :tournamentId ORDER BY displayOrder ASC")
    suspend fun getGroups(tournamentId: Long): List<TournamentGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: TournamentGroupEntity): Long

    @Query("DELETE FROM tournament_groups WHERE tournamentId = :tournamentId")
    suspend fun deleteGroupsForTournament(tournamentId: Long)
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY roundNumber ASC, bracketIndex ASC, id ASC")
    fun observeMatches(tournamentId: Long): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY roundNumber ASC, bracketIndex ASC, id ASC")
    suspend fun getMatches(tournamentId: Long): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: Long): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE tournamentId = :tournamentId")
    suspend fun deleteMatchesForTournament(tournamentId: Long)

    @Query("DELETE FROM matches WHERE tournamentId = :tournamentId AND stage IN ('ROUND_OF_16', 'QUARTER_FINAL', 'SEMI_FINAL', 'FINAL')")
    suspend fun deleteKnockoutMatchesForTournament(tournamentId: Long)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY role ASC, createdAt ASC")
    fun observeAdmins(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY role ASC, createdAt ASC")
    suspend fun getAdmins(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getAdminById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getAdminByUsername(username: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users WHERE role = 'SUB_ADMIN'")
    suspend fun countSubAdmins(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmin(admin: UserEntity): Long

    @Update
    suspend fun updateAdmin(admin: UserEntity)

    @Delete
    suspend fun deleteAdmin(admin: UserEntity)
}

typealias AdminDao = UserDao

@Dao
interface MatchReminderDao {
    @Query("SELECT * FROM match_reminders WHERE tournamentId = :tournamentId ORDER BY remindAtMillis ASC")
    fun observeReminders(tournamentId: Long): Flow<List<MatchReminderEntity>>

    @Query("SELECT * FROM match_reminders WHERE tournamentId = :tournamentId ORDER BY remindAtMillis ASC")
    suspend fun getReminders(tournamentId: Long): List<MatchReminderEntity>

    @Query("SELECT * FROM match_reminders WHERE matchId = :matchId LIMIT 1")
    suspend fun getReminderByMatchId(matchId: Long): MatchReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: MatchReminderEntity): Long

    @Query("UPDATE match_reminders SET isDelivered = 1 WHERE matchId = :matchId")
    suspend fun markDelivered(matchId: Long)

    @Query("DELETE FROM match_reminders WHERE matchId = :matchId")
    suspend fun deleteReminderByMatchId(matchId: Long)

    @Query("DELETE FROM match_reminders WHERE tournamentId = :tournamentId")
    suspend fun deleteRemindersForTournament(tournamentId: Long)
}
