package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AdminRole
import com.example.data.model.DrawMode
import com.example.data.model.KnockoutTieRule
import com.example.data.model.MatchStage
import com.example.data.model.PlayerLevel
import com.example.data.model.PlayerStatus
import com.example.data.model.TournamentFormat
import com.example.data.model.TournamentStatus

class Converters {
    @TypeConverter fun fromTournamentFormat(v: TournamentFormat): String = v.name
    @TypeConverter fun toTournamentFormat(v: String): TournamentFormat = TournamentFormat.valueOf(v)

    @TypeConverter fun fromDrawMode(v: DrawMode): String = v.name
    @TypeConverter fun toDrawMode(v: String): DrawMode = DrawMode.valueOf(v)

    @TypeConverter fun fromKnockoutTieRule(v: KnockoutTieRule): String = v.name
    @TypeConverter fun toKnockoutTieRule(v: String): KnockoutTieRule = KnockoutTieRule.valueOf(v)

    @TypeConverter fun fromPlayerStatus(v: PlayerStatus): String = v.name
    @TypeConverter fun toPlayerStatus(v: String): PlayerStatus = PlayerStatus.valueOf(v)

    @TypeConverter fun fromPlayerLevel(v: PlayerLevel): String = v.name
    @TypeConverter fun toPlayerLevel(v: String): PlayerLevel = PlayerLevel.valueOf(v)

    @TypeConverter fun fromMatchStage(v: MatchStage): String = v.name
    @TypeConverter fun toMatchStage(v: String): MatchStage = MatchStage.valueOf(v)

    @TypeConverter fun fromAdminRole(v: AdminRole): String = v.name
    @TypeConverter fun toAdminRole(v: String): AdminRole = AdminRole.valueOf(v)

    @TypeConverter fun fromTournamentStatus(v: TournamentStatus): String = v.name
    @TypeConverter fun toTournamentStatus(v: String): TournamentStatus = TournamentStatus.valueOf(v)
}
