package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AdminAccountEntity
import com.example.data.local.AdminSessionState
import com.example.data.local.AppDatabase
import com.example.data.local.LockoutState
import com.example.data.local.MatchEntity
import com.example.data.local.MatchReminderEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.TournamentEntity
import com.example.data.local.TournamentGroupEntity
import com.example.data.model.AdminRole
import com.example.data.model.MatchStage
import com.example.data.model.PlayerLevel
import com.example.data.model.PlayerStatus
import com.example.data.model.TournamentFormat
import com.example.data.model.TournamentStatus
import com.example.data.repository.RepoResult
import com.example.data.repository.TournamentRepository
import com.example.domain.GroupStandingRow
import com.example.domain.PlayerStats
import com.example.domain.TournamentEngine
import com.example.domain.TournamentOverviewStats
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GroupWithStandings(
    val group: TournamentGroupEntity,
    val standings: List<GroupStandingRow>,
    val matches: List<MatchEntity>
)

data class TournamentUiState(
    val isLoaded: Boolean = false,
    val tournament: TournamentEntity? = null,
    val players: List<PlayerEntity> = emptyList(),
    val approvedPlayers: List<PlayerEntity> = emptyList(),
    val pendingPlayers: List<PlayerEntity> = emptyList(),
    val rejectedPlayers: List<PlayerEntity> = emptyList(),
    val groupsWithStandings: List<GroupWithStandings> = emptyList(),
    val leagueStandings: List<GroupStandingRow> = emptyList(),
    val leagueMatches: List<MatchEntity> = emptyList(),
    val knockoutMatches: List<MatchEntity> = emptyList(),
    val allMatches: List<MatchEntity> = emptyList(),
    val matchReminders: List<MatchReminderEntity> = emptyList(),
    val playerStats: List<PlayerStats> = emptyList(),
    val overviewStats: TournamentOverviewStats = TournamentOverviewStats(0, 0, 0, null, null, null),
    val championPlayer: PlayerEntity? = null,
    val admins: List<AdminAccountEntity> = emptyList(),
    val hasHeadAdmin: Boolean = false,
    val currentAdmin: AdminAccountEntity? = null,
    val sessionState: AdminSessionState = AdminSessionState(),
    val lockoutState: LockoutState = LockoutState(),
    val isRegistrationOpenNow: Boolean = false
)

class TournamentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TournamentRepository(
        context = application,
        db = AppDatabase.getInstance(application),
        sessionPrefs = SessionPreferences(application)
    )

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.clearEphemeralSessionOnLaunch()
            repository.ensureDefaultHeadAdminIfEmpty()
            repository.ensureDefaultTournamentShellIfEmpty()
            repository.syncPendingRemindersOnLaunch()
        }
    }

    private data class CoreFlows(
        val tournament: TournamentEntity?,
        val players: List<PlayerEntity>,
        val groups: List<TournamentGroupEntity>,
        val matches: List<MatchEntity>,
        val reminders: List<MatchReminderEntity>
    )

    val uiState: StateFlow<TournamentUiState> = combine(
        combine(
            repository.latestTournament,
            repository.players,
            repository.groups,
            repository.matches,
            repository.matchReminders
        ) { t, p, g, m, r -> CoreFlows(t, p, g, m, r) },
        repository.admins,
        repository.sessionState,
        repository.lockoutState
    ) { core, admins, session, lockout ->
        val loggedId = session.loggedInAdminId
        val t = core.tournament
        val players = core.players
        val approved = players.filter { it.status == PlayerStatus.APPROVED }
        val pending = players.filter { it.status == PlayerStatus.PENDING }
        val rejected = players.filter { it.status == PlayerStatus.REJECTED }
        val matches = core.matches

        val groupsWithStandings = if (t != null && t.format == TournamentFormat.GROUPS_KNOCKOUT) {
            core.groups.map { grp ->
                val gPlayers = approved.filter { it.groupId == grp.id }
                val gMatches = matches.filter { it.stage == MatchStage.GROUP && it.groupId == grp.id }
                val standings = TournamentEngine.computeStandings(
                    players = gPlayers,
                    matches = gMatches,
                    qualifiersCount = t.qualifiersPerGroup
                )
                GroupWithStandings(grp, standings, gMatches)
            }
        } else emptyList()

        val leagueMatches = matches.filter { it.stage == MatchStage.LEAGUE }
        val leagueStandings = if (t != null && t.format == TournamentFormat.LEAGUE_KNOCKOUT) {
            TournamentEngine.computeStandings(
                players = approved,
                matches = leagueMatches,
                qualifiersCount = t.qualifiersPerGroup
            )
        } else emptyList()

        val knockoutMatches = matches.filter {
            it.stage == MatchStage.ROUND_OF_16 ||
                it.stage == MatchStage.QUARTER_FINAL ||
                it.stage == MatchStage.SEMI_FINAL ||
                it.stage == MatchStage.FINAL
        }

        val (playerStatsList, overviewStats) = TournamentEngine.computeTournamentStats(approved, matches)
        val champion = t?.championPlayerId?.let { cId -> players.find { it.id == cId } }
        val currentAdmin = loggedId?.let { id -> admins.find { it.id == id && it.isActive } }
        val hasHeadAdmin = admins.any { it.role == AdminRole.HEAD_ADMIN }
        val regOpen = t != null &&
            !t.isRegistrationClosedManual &&
            t.status == TournamentStatus.REGISTRATION_OPEN &&
            approved.size < t.maxParticipants

        TournamentUiState(
            isLoaded = true,
            tournament = t,
            players = players,
            approvedPlayers = approved,
            pendingPlayers = pending,
            rejectedPlayers = rejected,
            groupsWithStandings = groupsWithStandings,
            leagueStandings = leagueStandings,
            leagueMatches = leagueMatches,
            knockoutMatches = knockoutMatches,
            allMatches = matches,
            matchReminders = core.reminders,
            playerStats = playerStatsList,
            overviewStats = overviewStats,
            championPlayer = champion,
            admins = admins,
            hasHeadAdmin = hasHeadAdmin,
            currentAdmin = currentAdmin,
            sessionState = session,
            lockoutState = lockout,
            isRegistrationOpenNow = regOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TournamentUiState()
    )

    private fun <T> handleResult(res: RepoResult<T>) {
        when (res) {
            is RepoResult.Success -> if (res.messageAr.isNotEmpty()) _messages.tryEmit(res.messageAr)
            is RepoResult.Error -> _messages.tryEmit(res.messageAr)
        }
    }

    fun importImageFromPicker(uri: Uri, prefix: String, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val saved = repository.copyPickedImageToInternal(uri, prefix)
            if (saved != null) {
                onSaved(saved)
            } else {
                _messages.tryEmit("تعذر قراءة الصورة المختارة.")
            }
        }
    }

    fun setupHeadAdmin(username: String, displayName: String, pin: String, keepSignedIn: Boolean = true) {
        viewModelScope.launch {
            handleResult(repository.setupHeadAdmin(username, displayName, pin, keepSignedIn))
        }
    }

    fun loginAdmin(username: String, pin: String, keepSignedIn: Boolean = true) {
        viewModelScope.launch {
            handleResult(repository.loginAdmin(username, pin, keepSignedIn))
        }
    }

    fun logoutAdmin() {
        viewModelScope.launch {
            repository.logoutAdmin()
            _messages.tryEmit("تم تسجيل الخروج من لوحة المشرف.")
        }
    }

    fun createSubAdmin(
        username: String,
        displayName: String,
        pin: String,
        canManageTournament: Boolean,
        canManagePlayers: Boolean,
        canRunDraw: Boolean,
        canEditScores: Boolean,
        canCrownChampion: Boolean
    ) {
        val actorId = uiState.value.currentAdmin?.id ?: return
        viewModelScope.launch {
            handleResult(
                repository.createSubAdmin(
                    actorId, username, displayName, pin,
                    canManageTournament, canManagePlayers, canRunDraw, canEditScores, canCrownChampion
                )
            )
        }
    }

    fun changeOwnPin(currentPin: String, newPin: String) {
        val actorId = uiState.value.currentAdmin?.id ?: return
        viewModelScope.launch {
            handleResult(repository.changeOwnPin(actorId, currentPin, newPin))
        }
    }

    fun updateSubAdmin(
        target: AdminAccountEntity,
        displayName: String = target.displayName,
        newPin: String?,
        isActive: Boolean,
        canManageTournament: Boolean,
        canManagePlayers: Boolean,
        canRunDraw: Boolean,
        canEditScores: Boolean,
        canCrownChampion: Boolean
    ) {
        val actorId = uiState.value.currentAdmin?.id ?: return
        viewModelScope.launch {
            handleResult(
                repository.updateSubAdminPermissions(
                    actorId, target, displayName, newPin, isActive,
                    canManageTournament, canManagePlayers, canRunDraw, canEditScores, canCrownChampion
                )
            )
        }
    }

    fun deleteSubAdmin(target: AdminAccountEntity) {
        val actorId = uiState.value.currentAdmin?.id ?: return
        viewModelScope.launch {
            handleResult(repository.deleteSubAdmin(actorId, target))
        }
    }

    fun registerPlayer(
        name: String,
        elysiumNetId: String,
        avatarUri: String?,
        level: PlayerLevel,
        clubName: String,
        onSuccess: () -> Unit = {}
    ) {
        val tId = uiState.value.tournament?.id ?: return
        viewModelScope.launch {
            val res = repository.registerPlayer(tId, name, elysiumNetId, avatarUri, level, clubName)
            handleResult(res)
            if (res is RepoResult.Success) onSuccess()
        }
    }

    fun updatePlayerStatus(player: PlayerEntity, status: PlayerStatus) {
        viewModelScope.launch {
            handleResult(repository.updatePlayerStatus(player, status))
        }
    }

    fun updatePlayerDetails(player: PlayerEntity) {
        viewModelScope.launch {
            handleResult(repository.updatePlayerDetails(player))
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            handleResult(repository.deletePlayer(player))
        }
    }

    fun saveTournamentSettings(updated: TournamentEntity) {
        viewModelScope.launch {
            handleResult(repository.saveTournamentSettings(updated))
        }
    }

    fun createFreshTournament(newTournament: TournamentEntity) {
        viewModelScope.launch {
            handleResult(repository.createFreshTournament(newTournament))
        }
    }

    fun deleteCurrentTournament() {
        val id = uiState.value.tournament?.id ?: return
        viewModelScope.launch {
            handleResult(repository.deleteCurrentTournament(id))
        }
    }

    fun runDraw(
        manualGroupAssignments: Map<Long, Int> = emptyMap(),
        manualKnockoutOrder: List<Long> = emptyList()
    ) {
        viewModelScope.launch {
            handleResult(repository.runDrawAndGenerateSchedule(manualGroupAssignments, manualKnockoutOrder))
        }
    }

    fun generateKnockoutFromStandings() {
        val state = uiState.value
        viewModelScope.launch {
            handleResult(
                repository.generateKnockoutFromCurrentStandings(
                    groupStandings = state.groupsWithStandings.map { it.standings },
                    leagueStandings = state.leagueStandings
                )
            )
        }
    }

    fun updateMatchScore(
        match: MatchEntity,
        leg1S1: Int?,
        leg1S2: Int?,
        leg2S1: Int?,
        leg2S2: Int?,
        pen1: Int?,
        pen2: Int?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.updateMatchScore(match, leg1S1, leg1S2, leg2S1, leg2S2, pen1, pen2)
            handleResult(res)
            if (res is RepoResult.Success) onSuccess()
        }
    }

    fun updateKnockoutMatchWinner(
        match: MatchEntity,
        winnerPlayerId: Long?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.updateKnockoutMatchWinner(match, winnerPlayerId)
            handleResult(res)
            if (res is RepoResult.Success) onSuccess()
        }
    }

    fun crownChampion(playerId: Long?) {
        viewModelScope.launch {
            handleResult(repository.crownChampion(playerId))
        }
    }

    fun scheduleMatchReminder(
        match: MatchEntity,
        matchTitle: String,
        scheduledAtMillis: Long,
        leadMinutes: Int,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.scheduleMatchReminder(match, matchTitle, scheduledAtMillis, leadMinutes)
            handleResult(res)
            if (res is RepoResult.Success) onSuccess()
        }
    }

    fun cancelMatchReminder(matchId: Long) {
        viewModelScope.launch {
            handleResult(repository.cancelMatchReminder(matchId))
        }
    }

    fun sendTestMatchNotification(
        match: MatchEntity,
        matchTitle: String,
        scheduledAtMillis: Long,
        leadMinutes: Int
    ) {
        handleResult(repository.sendTestMatchNotification(match, matchTitle, scheduledAtMillis, leadMinutes))
    }

    fun notifyNotificationPermissionDenied() {
        _messages.tryEmit("يرجى السماح بإذن الإشعارات لتفعيل تنبيهات المباريات القادمة.")
    }
}
