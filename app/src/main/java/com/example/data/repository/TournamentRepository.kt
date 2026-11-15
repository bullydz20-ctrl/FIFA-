package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.local.AdminAccountEntity
import com.example.data.local.AppDatabase
import com.example.data.local.LockoutState
import com.example.data.local.MatchEntity
import com.example.data.local.MatchReminderEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.TournamentEntity
import com.example.data.local.TournamentGroupEntity
import com.example.data.model.DrawMode
import com.example.data.model.KnockoutTieRule
import com.example.data.model.MatchStage
import com.example.data.model.PlayerLevel
import com.example.data.model.PlayerStatus
import com.example.data.model.TournamentFormat
import com.example.data.model.TournamentStatus
import com.example.domain.GroupStandingRow
import com.example.domain.ScoreResolution
import com.example.domain.TournamentEngine
import com.example.notifications.MatchReminderScheduler
import com.example.security.AdminSecurityModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class RepoResult<out T> {
    data class Success<T>(val data: T, val messageAr: String = "") : RepoResult<T>()
    data class Error(val messageAr: String) : RepoResult<Nothing>()
}

@OptIn(ExperimentalCoroutinesApi::class)
class TournamentRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val sessionPrefs: SessionPreferences
) {
    private val tournamentDao = db.tournamentDao()
    private val playerDao = db.playerDao()
    private val groupDao = db.groupDao()
    private val matchDao = db.matchDao()
    private val adminDao = db.adminDao()
    private val matchReminderDao = db.matchReminderDao()

    val securityModule = AdminSecurityModule(adminDao, sessionPrefs)

    val latestTournament: Flow<TournamentEntity?> = tournamentDao.observeLatestTournament()

    val players: Flow<List<PlayerEntity>> = latestTournament.flatMapLatest { t ->
        if (t == null) flowOf(emptyList()) else playerDao.observePlayers(t.id)
    }

    val groups: Flow<List<TournamentGroupEntity>> = latestTournament.flatMapLatest { t ->
        if (t == null) flowOf(emptyList()) else groupDao.observeGroups(t.id)
    }

    val matches: Flow<List<MatchEntity>> = latestTournament.flatMapLatest { t ->
        if (t == null) flowOf(emptyList()) else matchDao.observeMatches(t.id)
    }

    val matchReminders: Flow<List<MatchReminderEntity>> = latestTournament.flatMapLatest { t ->
        if (t == null) flowOf(emptyList()) else matchReminderDao.observeReminders(t.id)
    }

    val admins: Flow<List<AdminAccountEntity>> = securityModule.admins

    val loggedInAdminId: Flow<Long?> = securityModule.loggedInAdminId

    val sessionState = securityModule.sessionState

    val lockoutState: Flow<LockoutState> = securityModule.lockoutState

    suspend fun clearEphemeralSessionOnLaunch() = securityModule.clearEphemeralSessionOnLaunch()

    suspend fun ensureDefaultHeadAdminIfEmpty() = securityModule.ensureDefaultHeadAdminIfEmpty()

    suspend fun syncPendingRemindersOnLaunch() {
        val tournament = tournamentDao.getLatestTournament() ?: return
        val reminders = matchReminderDao.getReminders(tournament.id)
        val now = System.currentTimeMillis()
        reminders.filter { it.isEnabled && !it.isDelivered && it.remindAtMillis > now }
            .forEach { reminder ->
                MatchReminderScheduler.scheduleReminder(context, reminder)
            }
    }

    suspend fun ensureDefaultTournamentShellIfEmpty() {
        if (tournamentDao.getLatestTournament() == null) {
            tournamentDao.insertTournament(
                TournamentEntity(
                    name = "بطولة FIFA 22 — COMMUNITY PS4 JAILBREAK ONLINE",
                    description = "البطولة الرسمية لمجتمع Discord لألعاب PS4 عبر شبكة ElysiumNet. التزم بقواعد اللعب النظيف ومواعيد المباريات.",
                    startDateTime = "يُحدد من لوحة الإدارة",
                    maxParticipants = 16,
                    format = TournamentFormat.GROUPS_KNOCKOUT,
                    groupCount = 4,
                    qualifiersPerGroup = 2,
                    drawMode = DrawMode.RANDOM,
                    knockoutTieRule = KnockoutTieRule.SINGLE_MATCH_PENALTIES,
                    matchDurationMinutes = 12,
                    targetGoalsRule = "المباراة الكاملة (6 دقائق لكل شوط)"
                )
            )
        }
    }

    suspend fun copyPickedImageToInternal(uri: Uri, prefix: String): String? = withContext(Dispatchers.IO) {
        try {
            val mediaDir = File(context.filesDir, "media")
            if (!mediaDir.exists()) mediaDir.mkdirs()
            val outFile = File(mediaDir, "${prefix}_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            Uri.fromFile(outFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    // region Admin Auth & Sub-Admins (delegated to AdminSecurityModule)

    suspend fun setupHeadAdmin(
        username: String,
        displayName: String,
        pin: String,
        keepSignedIn: Boolean = true
    ): RepoResult<AdminAccountEntity> =
        securityModule.setupHeadAdmin(username, displayName, pin, keepSignedIn)

    suspend fun loginAdmin(
        username: String,
        pin: String,
        keepSignedIn: Boolean = true
    ): RepoResult<AdminAccountEntity> =
        securityModule.loginAdmin(username, pin, keepSignedIn)

    suspend fun logoutAdmin() = securityModule.logoutAdmin()

    suspend fun changeOwnPin(adminId: Long, currentPin: String, newPin: String): RepoResult<Unit> =
        securityModule.changeOwnPin(adminId, currentPin, newPin)

    suspend fun createSubAdmin(
        actingAdminId: Long,
        username: String,
        displayName: String,
        pin: String,
        canManageTournament: Boolean,
        canManagePlayers: Boolean,
        canRunDraw: Boolean,
        canEditScores: Boolean,
        canCrownChampion: Boolean
    ): RepoResult<Unit> = securityModule.createSubAdmin(
        actingAdminId, username, displayName, pin,
        canManageTournament, canManagePlayers, canRunDraw, canEditScores, canCrownChampion
    )

    suspend fun updateSubAdminPermissions(
        actingAdminId: Long,
        target: AdminAccountEntity,
        displayName: String = target.displayName,
        newPin: String?,
        isActive: Boolean = target.isActive,
        canManageTournament: Boolean,
        canManagePlayers: Boolean,
        canRunDraw: Boolean,
        canEditScores: Boolean,
        canCrownChampion: Boolean
    ): RepoResult<Unit> = securityModule.updateSubAdmin(
        actingAdminId, target, displayName, newPin, isActive,
        canManageTournament, canManagePlayers, canRunDraw, canEditScores, canCrownChampion
    )

    suspend fun deleteSubAdmin(actingAdminId: Long, target: AdminAccountEntity): RepoResult<Unit> =
        securityModule.deleteSubAdmin(actingAdminId, target)

    // endregion

    // region Tournament CRUD

    suspend fun saveTournamentSettings(updated: TournamentEntity): RepoResult<Unit> {
        if (updated.name.isBlank()) return RepoResult.Error("يرجى إدخال اسم البطولة.")
        if (updated.maxParticipants < 2) return RepoResult.Error("الحد الأدنى للمشاركين هو لاعبان.")
        if (updated.format == TournamentFormat.GROUPS_KNOCKOUT && updated.groupCount < 1) {
            return RepoResult.Error("عدد المجموعات يجب أن يكون 1 على الأقل.")
        }
        val approvedCount = playerDao.getPlayers(updated.id).count { it.status == PlayerStatus.APPROVED }
        val finalEntity = if (approvedCount >= updated.maxParticipants && updated.status == TournamentStatus.REGISTRATION_OPEN) {
            updated.copy(status = TournamentStatus.REGISTRATION_CLOSED)
        } else if (approvedCount < updated.maxParticipants && !updated.isRegistrationClosedManual && updated.status == TournamentStatus.REGISTRATION_CLOSED) {
            updated.copy(status = TournamentStatus.REGISTRATION_OPEN)
        } else {
            updated
        }
        tournamentDao.updateTournament(finalEntity)
        return RepoResult.Success(Unit, "تم حفظ إعدادات البطولة بنجاح.")
    }

    suspend fun createFreshTournament(newTournament: TournamentEntity): RepoResult<Unit> {
        if (newTournament.name.isBlank()) return RepoResult.Error("يرجى إدخال اسم البطولة.")
        tournamentDao.insertTournament(newTournament)
        return RepoResult.Success(Unit, "تم إنشاء البطولة الجديدة بنجاح.")
    }

    suspend fun deleteCurrentTournament(tournamentId: Long): RepoResult<Unit> {
        val existingReminders = matchReminderDao.getReminders(tournamentId)
        existingReminders.forEach { MatchReminderScheduler.cancelReminder(context, it.matchId) }
        db.withTransaction {
            matchReminderDao.deleteRemindersForTournament(tournamentId)
            matchDao.deleteMatchesForTournament(tournamentId)
            groupDao.deleteGroupsForTournament(tournamentId)
            playerDao.deletePlayersForTournament(tournamentId)
            tournamentDao.deleteTournament(tournamentId)
        }
        ensureDefaultTournamentShellIfEmpty()
        return RepoResult.Success(Unit, "تم حذف البطولة وإعادة التهيئة.")
    }

    // endregion

    // region Player Registration & Management

    suspend fun registerPlayer(
        tournamentId: Long,
        name: String,
        elysiumNetId: String,
        avatarUri: String?,
        level: PlayerLevel,
        clubName: String
    ): RepoResult<Unit> {
        val tournament = tournamentDao.getLatestTournament()
            ?: return RepoResult.Error("لا توجد بطولة نشطة حالياً.")
        if (tournament.id != tournamentId) return RepoResult.Error("بيانات البطولة تغيرت، يرجى إعادة المحاولة.")

        val currentPlayers = playerDao.getPlayers(tournamentId)
        val approvedCount = currentPlayers.count { it.status == PlayerStatus.APPROVED }
        if (tournament.isRegistrationClosedManual ||
            tournament.status != TournamentStatus.REGISTRATION_OPEN ||
            approvedCount >= tournament.maxParticipants
        ) {
            return RepoResult.Error("تم إغلاق التسجيل لاكتمال العدد المحدد أو بدء البطولة.")
        }

        val cleanName = name.trim()
        val cleanElysium = elysiumNetId.trim()
        if (cleanName.isEmpty() || cleanElysium.isEmpty()) {
            return RepoResult.Error("يرجى إدخال اسم اللاعب ومعرف ElysiumNet.")
        }
        if (playerDao.countByElysiumId(tournamentId, cleanElysium) > 0) {
            return RepoResult.Error("معرف ElysiumNet مسجل مسبقاً في هذه البطولة.")
        }

        playerDao.insertPlayer(
            PlayerEntity(
                tournamentId = tournamentId,
                name = cleanName,
                elysiumNetId = cleanElysium,
                avatarUri = avatarUri,
                level = level,
                ovrRating = level.defaultOvr,
                clubName = clubName.trim(),
                status = PlayerStatus.PENDING
            )
        )
        return RepoResult.Success(Unit, "تم إرسال طلب التسجيل بنجاح! بانتظار موافقة المشرف.")
    }

    suspend fun updatePlayerStatus(player: PlayerEntity, newStatus: PlayerStatus): RepoResult<Unit> {
        val tournament = tournamentDao.getLatestTournament() ?: return RepoResult.Error("لا توجد بطولة نشطة.")
        if (newStatus == PlayerStatus.APPROVED && player.status != PlayerStatus.APPROVED) {
            val approvedCount = playerDao.getPlayers(tournament.id).count { it.status == PlayerStatus.APPROVED }
            if (approvedCount >= tournament.maxParticipants) {
                return RepoResult.Error("تم الوصول للحد الأقصى للمشاركين (${tournament.maxParticipants}).")
            }
        }
        db.withTransaction {
            playerDao.updatePlayer(player.copy(status = newStatus))
            val newApprovedCount = playerDao.getPlayers(tournament.id).count { it.status == PlayerStatus.APPROVED }
            if (newApprovedCount >= tournament.maxParticipants && tournament.status == TournamentStatus.REGISTRATION_OPEN) {
                tournamentDao.updateTournament(
                    tournament.copy(
                        isRegistrationClosedManual = true,
                        status = TournamentStatus.REGISTRATION_CLOSED
                    )
                )
            }
        }
        return RepoResult.Success(Unit, "تم تحديث حالة اللاعب إلى ${newStatus.labelAr}.")
    }

    suspend fun updatePlayerDetails(player: PlayerEntity): RepoResult<Unit> {
        if (player.name.isBlank() || player.elysiumNetId.isBlank()) {
            return RepoResult.Error("الاسم ومعرف ElysiumNet مطلوبان.")
        }
        playerDao.updatePlayer(player)
        return RepoResult.Success(Unit, "تم تحديث بيانات اللاعب.")
    }

    suspend fun deletePlayer(player: PlayerEntity): RepoResult<Unit> {
        val tournament = tournamentDao.getLatestTournament()
        db.withTransaction {
            playerDao.deletePlayer(player)
            if (tournament != null) {
                val approvedCount = playerDao.getPlayers(tournament.id).count { it.status == PlayerStatus.APPROVED }
                if (approvedCount < tournament.maxParticipants &&
                    tournament.status == TournamentStatus.REGISTRATION_CLOSED &&
                    !tournament.isRegistrationClosedManual
                ) {
                    tournamentDao.updateTournament(tournament.copy(status = TournamentStatus.REGISTRATION_OPEN))
                }
            }
        }
        return RepoResult.Success(Unit, "تم حذف اللاعب.")
    }

    // endregion

    // region Draw & Fixture Generation

    suspend fun runDrawAndGenerateSchedule(
        manualGroupAssignments: Map<Long, Int> = emptyMap(), // playerId -> groupIndex (0..groupCount-1)
        manualKnockoutOrder: List<Long> = emptyList()
    ): RepoResult<Unit> {
        val tournament = tournamentDao.getLatestTournament()
            ?: return RepoResult.Error("لا توجد بطولة نشطة.")
        val approved = playerDao.getPlayers(tournament.id).filter { it.status == PlayerStatus.APPROVED }
        if (approved.size < 2) {
            return RepoResult.Error("يجب اعتماد لاعبين اثنين على الأقل لإجراء القرعة.")
        }

        return db.withTransaction {
            matchDao.deleteMatchesForTournament(tournament.id)
            groupDao.deleteGroupsForTournament(tournament.id)

            when (tournament.format) {
                TournamentFormat.GROUPS_KNOCKOUT -> {
                    val gCount = tournament.groupCount.coerceIn(1, approved.size)
                    val insertedGroups = mutableListOf<TournamentGroupEntity>()
                    for (i in 0 until gCount) {
                        val g = TournamentGroupEntity(
                            tournamentId = tournament.id,
                            name = TournamentEngine.groupDisplayName(i),
                            displayOrder = i
                        )
                        val gId = groupDao.insertGroup(g)
                        insertedGroups.add(g.copy(id = gId))
                    }

                    val updatedPlayers = if (tournament.drawMode == DrawMode.MANUAL && manualGroupAssignments.isNotEmpty()) {
                        approved.mapIndexed { idx, p ->
                            val targetIdx = (manualGroupAssignments[p.id] ?: (idx % gCount)).coerceIn(0, gCount - 1)
                            p.copy(groupId = insertedGroups[targetIdx].id, seedOrder = idx + 1)
                        }
                    } else {
                        TournamentEngine.distributePlayersIntoGroups(
                            players = approved,
                            groups = insertedGroups,
                            shuffle = true
                        )
                    }
                    playerDao.updatePlayers(updatedPlayers)

                    val allGroupMatches = mutableListOf<MatchEntity>()
                    for (group in insertedGroups) {
                        val groupPlayerIds = updatedPlayers.filter { it.groupId == group.id }.map { it.id }
                        if (groupPlayerIds.size >= 2) {
                            allGroupMatches.addAll(
                                TournamentEngine.generateRoundRobinMatches(
                                    tournamentId = tournament.id,
                                    stage = MatchStage.GROUP,
                                    groupId = group.id,
                                    playerIds = groupPlayerIds,
                                    doubleRoundRobin = false
                                )
                            )
                        }
                    }
                    matchDao.insertMatches(allGroupMatches)
                    tournamentDao.updateTournament(
                        tournament.copy(
                            status = TournamentStatus.GROUP_STAGE,
                            isRegistrationClosedManual = true,
                            championPlayerId = null
                        )
                    )
                    RepoResult.Success(Unit, "تم إجراء قرعة المجموعات وتوليد جدول المباريات بنجاح!")
                }

                TournamentFormat.LEAGUE_KNOCKOUT -> {
                    val ordered = if (tournament.drawMode == DrawMode.MANUAL && manualKnockoutOrder.isNotEmpty()) {
                        manualKnockoutOrder.mapNotNull { id -> approved.find { it.id == id } } +
                            approved.filter { p -> p.id !in manualKnockoutOrder }
                    } else {
                        approved.shuffled()
                    }
                    playerDao.updatePlayers(ordered.mapIndexed { idx, p -> p.copy(groupId = null, seedOrder = idx + 1) })
                    val leagueMatches = TournamentEngine.generateRoundRobinMatches(
                        tournamentId = tournament.id,
                        stage = MatchStage.LEAGUE,
                        groupId = null,
                        playerIds = ordered.map { it.id },
                        doubleRoundRobin = false
                    )
                    matchDao.insertMatches(leagueMatches)
                    tournamentDao.updateTournament(
                        tournament.copy(
                            status = TournamentStatus.GROUP_STAGE,
                            isRegistrationClosedManual = true,
                            championPlayerId = null
                        )
                    )
                    RepoResult.Success(Unit, "تم توليد جدول الدوري بنجاح!")
                }

                TournamentFormat.DIRECT_KNOCKOUT -> {
                    val orderedIds = if (tournament.drawMode == DrawMode.MANUAL && manualKnockoutOrder.isNotEmpty()) {
                        (manualKnockoutOrder.filter { id -> approved.any { it.id == id } } +
                            approved.map { it.id }.filter { it !in manualKnockoutOrder }).take(16)
                    } else {
                        approved.shuffled().map { it.id }.take(16)
                    }
                    val bracketSize = TournamentEngine.nextPowerOfTwo(orderedIds.size.coerceAtLeast(2)).coerceAtMost(16)
                    val padded = List<Long?>(bracketSize) { i -> orderedIds.getOrNull(i) }
                    playerDao.updatePlayers(approved.map { it.copy(groupId = null) })
                    insertLinkedKnockoutBracket(tournament, padded)
                    tournamentDao.updateTournament(
                        tournament.copy(
                            status = TournamentStatus.KNOCKOUT_STAGE,
                            isRegistrationClosedManual = true,
                            championPlayerId = null
                        )
                    )
                    RepoResult.Success(Unit, "تم إنشاء مخطط الأدوار الإقصائية المباشرة!")
                }
            }
        }
    }

    suspend fun generateKnockoutFromCurrentStandings(
        groupStandings: List<List<GroupStandingRow>>,
        leagueStandings: List<GroupStandingRow>
    ): RepoResult<Unit> {
        val tournament = tournamentDao.getLatestTournament()
            ?: return RepoResult.Error("لا توجد بطولة نشطة.")

        val seededSlots: List<Long?> = when (tournament.format) {
            TournamentFormat.GROUPS_KNOCKOUT -> {
                if (groupStandings.isEmpty()) return RepoResult.Error("لا توجد مجموعات لتأهيل اللاعبين.")
                TournamentEngine.buildKnockoutSeedPairsFromGroups(groupStandings, tournament.qualifiersPerGroup)
            }
            TournamentFormat.LEAGUE_KNOCKOUT -> {
                val topCount = tournament.qualifiersPerGroup.coerceIn(2, 16)
                val topIds = leagueStandings.take(topCount).map { it.player.id }
                if (topIds.size < 2) return RepoResult.Error("يلزم تأهل لاعبين اثنين على الأقل.")
                val bracketSize = TournamentEngine.nextPowerOfTwo(topIds.size).coerceAtMost(16)
                // Standard seeding: 1 vs N, 2 vs N-1...
                val paired = mutableListOf<Long?>()
                var left = 0
                var right = bracketSize - 1
                while (left < right) {
                    paired.add(topIds.getOrNull(left))
                    paired.add(topIds.getOrNull(right))
                    left++
                    right--
                }
                paired
            }
            TournamentFormat.DIRECT_KNOCKOUT -> {
                return RepoResult.Error("البطولة بنظام الإقصائيات المباشرة بالفعل.")
            }
        }

        if (seededSlots.count { it != null } < 2) {
            return RepoResult.Error("عدد المتأهلين غير كافٍ لبدء الأدوار الإقصائية.")
        }

        db.withTransaction {
            matchDao.deleteKnockoutMatchesForTournament(tournament.id)
            insertLinkedKnockoutBracket(tournament, seededSlots)
            tournamentDao.updateTournament(
                tournament.copy(
                    status = TournamentStatus.KNOCKOUT_STAGE,
                    championPlayerId = null
                )
            )
        }
        return RepoResult.Success(Unit, "تم تأهيل المتصدرين وبناء مخطط الأدوار الإقصائية بنجاح!")
    }

    private suspend fun insertLinkedKnockoutBracket(
        tournament: TournamentEntity,
        seededSlots: List<Long?>
    ) {
        val drafts = TournamentEngine.buildKnockoutTreeDrafts(
            tournamentId = tournament.id,
            seededSlots = seededSlots,
            tieRule = tournament.knockoutTieRule
        )
        // Insert top-down (Final first at index 0) so parentTempIndex already has a real DB ID!
        val tempToRealId = mutableMapOf<Int, Long>()
        val insertedMatches = mutableListOf<MatchEntity>()
        for (draft in drafts) {
            val realParentId = draft.parentTempIndex?.let { tempToRealId[it] }
            val toInsert = draft.entity.copy(nextMatchId = realParentId)
            val realId = matchDao.insertMatch(toInsert)
            tempToRealId[draft.tempIndex] = realId
            insertedMatches.add(toInsert.copy(id = realId))
        }

        // Auto-advance any first-round BYEs (where player1Id != null && player2Id == null)
        val firstRoundStage = insertedMatches.minByOrNull { it.stage.order }?.stage ?: return
        for (match in insertedMatches.filter { it.stage == firstRoundStage }) {
            val p1 = match.player1Id
            val p2 = match.player2Id
            if ((p1 != null && p2 == null) || (p2 != null && p1 == null)) {
                val byeWinner = p1 ?: p2
                matchDao.updateMatch(match.copy(isCompleted = true, winnerId = byeWinner))
                val nextId = match.nextMatchId
                if (nextId != null && byeWinner != null) {
                    val parent = matchDao.getMatchById(nextId)
                    if (parent != null) {
                        val updatedParent = if (match.nextMatchSlot == 1) {
                            parent.copy(player1Id = byeWinner)
                        } else {
                            parent.copy(player2Id = byeWinner)
                        }
                        matchDao.updateMatch(updatedParent)
                    }
                }
            }
        }
    }

    // endregion

    // region Match Score Updates & Cascading Advancement

    suspend fun updateMatchScore(
        match: MatchEntity,
        leg1S1: Int?,
        leg1S2: Int?,
        leg2S1: Int?,
        leg2S2: Int?,
        pen1: Int?,
        pen2: Int?
    ): RepoResult<Unit> {
        val resolution = TournamentEngine.resolveMatchResult(
            match = match,
            leg1S1 = leg1S1,
            leg1S2 = leg1S2,
            leg2S1 = leg2S1,
            leg2S2 = leg2S2,
            pen1 = pen1,
            pen2 = pen2
        )

        if (resolution is ScoreResolution.Error) {
            return RepoResult.Error(resolution.messageAr)
        }
        val valid = resolution as ScoreResolution.Valid

        db.withTransaction {
            val oldWinnerId = match.winnerId
            val updatedMatch = match.copy(
                leg1Score1 = leg1S1,
                leg1Score2 = leg1S2,
                leg2Score1 = if (match.isTwoLegged) leg2S1 else null,
                leg2Score2 = if (match.isTwoLegged) leg2S2 else null,
                penaltyScore1 = pen1,
                penaltyScore2 = pen2,
                isCompleted = valid.isCompleted,
                winnerId = valid.winnerId
            )
            matchDao.updateMatch(updatedMatch)
            if (updatedMatch.isCompleted) {
                matchReminderDao.deleteReminderByMatchId(updatedMatch.id)
                MatchReminderScheduler.cancelReminder(context, updatedMatch.id)
            }

            // Propagate to next round if knockout match
            val nextId = updatedMatch.nextMatchId
            if (nextId != null) {
                propagateWinnerAndResetIfChanged(
                    nextMatchId = nextId,
                    slot = updatedMatch.nextMatchSlot,
                    oldWinnerId = oldWinnerId,
                    newWinnerId = valid.winnerId
                )
            }

            // If this is the Final and was edited/reopened, sync champion state
            if (updatedMatch.stage == MatchStage.FINAL) {
                val t = tournamentDao.getLatestTournament()
                if (t != null) {
                    if (!valid.isCompleted) {
                        tournamentDao.updateTournament(
                            t.copy(
                                championPlayerId = null,
                                status = TournamentStatus.KNOCKOUT_STAGE
                            )
                        )
                    } else if (valid.winnerId != null && t.championPlayerId != null && t.championPlayerId != valid.winnerId) {
                        tournamentDao.updateTournament(t.copy(championPlayerId = valid.winnerId))
                    }
                }
            }
        }
        return RepoResult.Success(Unit, "تم حفظ النتيجة وتحديث المخطط بنجاح.")
    }

    private suspend fun propagateWinnerAndResetIfChanged(
        nextMatchId: Long,
        slot: Int,
        oldWinnerId: Long?,
        newWinnerId: Long?
    ) {
        val nextMatch = matchDao.getMatchById(nextMatchId) ?: return
        val winnerChanged = (oldWinnerId != newWinnerId)
        val withAssignedSlot = if (slot == 1) {
            nextMatch.copy(player1Id = newWinnerId)
        } else {
            nextMatch.copy(player2Id = newWinnerId)
        }

        if (winnerChanged) {
            val previousNextWinner = nextMatch.winnerId
            val resetNext = withAssignedSlot.copy(
                leg1Score1 = null,
                leg1Score2 = null,
                leg2Score1 = null,
                leg2Score2 = null,
                penaltyScore1 = null,
                penaltyScore2 = null,
                isCompleted = false,
                winnerId = null
            )
            matchDao.updateMatch(resetNext)
            val furtherNextId = resetNext.nextMatchId
            if (furtherNextId != null) {
                propagateWinnerAndResetIfChanged(
                    nextMatchId = furtherNextId,
                    slot = resetNext.nextMatchSlot,
                    oldWinnerId = previousNextWinner,
                    newWinnerId = null
                )
            }
        } else {
            matchDao.updateMatch(withAssignedSlot)
        }
    }

    /**
     * Directly sets or clears the advancing winner of a knockout match from the interactive Bracket View,
     * cascading advancement or resets downstream and syncing the tournament champion if the Final is updated.
     */
    suspend fun updateKnockoutMatchWinner(
        match: MatchEntity,
        winnerPlayerId: Long?
    ): RepoResult<Unit> {
        if (match.stage == MatchStage.GROUP || match.stage == MatchStage.LEAGUE) {
            return RepoResult.Error("تحديث المتأهل المباشر متاح لمباريات الأدوار الإقصائية فقط.")
        }
        val p1 = match.player1Id
        val p2 = match.player2Id
        if (winnerPlayerId != null && (p1 == null || p2 == null)) {
            return RepoResult.Error("لا يمكن تحديد الفائز قبل اكتمال طرفي المواجهة.")
        }
        if (winnerPlayerId != null && winnerPlayerId != p1 && winnerPlayerId != p2) {
            return RepoResult.Error("اللاعب المختار ليس طرفاً في هذه المواجهة.")
        }

        db.withTransaction {
            val oldWinnerId = match.winnerId

            // Keep existing scores only if they strictly agree with the chosen winner; otherwise clear stale scores
            val keepExistingScores = if (winnerPlayerId == null) {
                false
            } else {
                val existingRes = if (match.leg1Score1 != null && match.leg1Score2 != null) {
                    TournamentEngine.resolveMatchResult(
                        match = match,
                        leg1S1 = match.leg1Score1,
                        leg1S2 = match.leg1Score2,
                        leg2S1 = match.leg2Score1,
                        leg2S2 = match.leg2Score2,
                        pen1 = match.penaltyScore1,
                        pen2 = match.penaltyScore2
                    )
                } else null
                existingRes is ScoreResolution.Valid && existingRes.isCompleted && existingRes.winnerId == winnerPlayerId
            }

            val updatedMatch = match.copy(
                leg1Score1 = if (keepExistingScores) match.leg1Score1 else null,
                leg1Score2 = if (keepExistingScores) match.leg1Score2 else null,
                leg2Score1 = if (keepExistingScores && match.isTwoLegged) match.leg2Score1 else null,
                leg2Score2 = if (keepExistingScores && match.isTwoLegged) match.leg2Score2 else null,
                penaltyScore1 = if (keepExistingScores) match.penaltyScore1 else null,
                penaltyScore2 = if (keepExistingScores) match.penaltyScore2 else null,
                isCompleted = winnerPlayerId != null,
                winnerId = winnerPlayerId
            )
            matchDao.updateMatch(updatedMatch)

            val nextId = updatedMatch.nextMatchId
            if (nextId != null) {
                propagateWinnerAndResetIfChanged(
                    nextMatchId = nextId,
                    slot = updatedMatch.nextMatchSlot,
                    oldWinnerId = oldWinnerId,
                    newWinnerId = winnerPlayerId
                )
            }

            if (updatedMatch.stage == MatchStage.FINAL) {
                val t = tournamentDao.getLatestTournament()
                if (t != null) {
                    if (winnerPlayerId == null) {
                        tournamentDao.updateTournament(
                            t.copy(
                                championPlayerId = null,
                                status = TournamentStatus.KNOCKOUT_STAGE
                            )
                        )
                    } else {
                        tournamentDao.updateTournament(
                            t.copy(
                                championPlayerId = winnerPlayerId,
                                status = TournamentStatus.COMPLETED
                            )
                        )
                    }
                }
            }
        }

        val msg = if (winnerPlayerId == null) {
            "تم إلغاء اعتماد الفائز وإعادة فتح المواجهة."
        } else if (match.stage == MatchStage.FINAL) {
            "تم اعتماد الفائز بالنهائي وتتويجه بطلاً للبطولة!"
        } else {
            "تم تحديث الفائز وتأهيله للدور التالي بنجاح."
        }
        return RepoResult.Success(Unit, msg)
    }

    suspend fun crownChampion(playerId: Long?): RepoResult<Unit> {
        val tournament = tournamentDao.getLatestTournament()
            ?: return RepoResult.Error("لا توجد بطولة نشطة.")
        tournamentDao.updateTournament(
            tournament.copy(
                championPlayerId = playerId,
                status = if (playerId != null) TournamentStatus.COMPLETED else TournamentStatus.KNOCKOUT_STAGE
            )
        )
        return if (playerId != null) {
            RepoResult.Success(Unit, "تم تتويج بطل البطولة وإعلانه للجميع!")
        } else {
            RepoResult.Success(Unit, "تم إلغاء اعتماد البطل لإعادة المراجعة.")
        }
    }

    // endregion

    // region Match Reminders & Notifications

    suspend fun scheduleMatchReminder(
        match: MatchEntity,
        matchTitle: String,
        scheduledAtMillis: Long,
        leadMinutes: Int
    ): RepoResult<MatchReminderEntity> {
        if (match.isCompleted) {
            return RepoResult.Error("لا يمكن جدولة تنبيه لمباراة منتهية بالفعل.")
        }
        val now = System.currentTimeMillis()
        if (scheduledAtMillis <= now) {
            return RepoResult.Error("يرجى تحديد وقت مستقبلي لموعد المباراة.")
        }

        val safeLead = leadMinutes.coerceIn(0, 180)
        val rawRemindAt = scheduledAtMillis - safeLead * 60_000L
        val remindAtMillis = maxOf(rawRemindAt, now + 2_000L)
        val stageLabel = match.stage.labelAr
        val formattedTime = MatchReminderScheduler.formatDateTime(scheduledAtMillis)
        val existing = matchReminderDao.getReminderByMatchId(match.id)

        val entity = MatchReminderEntity(
            id = existing?.id ?: 0L,
            matchId = match.id,
            tournamentId = match.tournamentId,
            matchTitle = matchTitle,
            matchStageLabel = stageLabel,
            scheduledAtMillis = scheduledAtMillis,
            remindAtMillis = remindAtMillis,
            leadMinutes = safeLead,
            isEnabled = true,
            isDelivered = false
        )

        val savedId = db.withTransaction {
            val updatedLabel = match.scheduledLabel.ifBlank { formattedTime }
            matchDao.updateMatch(
                match.copy(
                    scheduledAtMillis = scheduledAtMillis,
                    scheduledLabel = updatedLabel
                )
            )
            matchReminderDao.upsertReminder(entity)
        }

        val savedEntity = entity.copy(id = if (entity.id != 0L) entity.id else savedId)
        MatchReminderScheduler.scheduleReminder(context, savedEntity)

        return RepoResult.Success(
            savedEntity,
            "تم جدولة التنبيه للمباراة عند ${MatchReminderScheduler.formatDateTime(remindAtMillis)} بنجاح."
        )
    }

    suspend fun cancelMatchReminder(matchId: Long): RepoResult<Unit> {
        matchReminderDao.deleteReminderByMatchId(matchId)
        MatchReminderScheduler.cancelReminder(context, matchId)
        return RepoResult.Success(Unit, "تم إلغاء التنبيه المجدول للمباراة.")
    }

    fun sendTestMatchNotification(
        match: MatchEntity,
        matchTitle: String,
        scheduledAtMillis: Long,
        leadMinutes: Int
    ): RepoResult<Unit> {
        val preview = MatchReminderEntity(
            matchId = match.id,
            tournamentId = match.tournamentId,
            matchTitle = matchTitle,
            matchStageLabel = match.stage.labelAr,
            scheduledAtMillis = scheduledAtMillis,
            remindAtMillis = System.currentTimeMillis(),
            leadMinutes = leadMinutes
        )
        val shown = MatchReminderScheduler.showMatchNotification(
            context = context,
            matchId = match.id,
            title = "⏰ تذكير بمباراة قادمة: $matchTitle",
            body = MatchReminderScheduler.buildNotificationBody(preview),
            stageLabel = match.stage.labelAr
        )
        return if (shown) {
            RepoResult.Success(Unit, "تم إرسال إشعار التذكير التجريبي الآن.")
        } else {
            RepoResult.Error("يرجى تفعيل إذن الإشعارات أولاً لعرض التنبيهات.")
        }
    }

    // endregion
}
