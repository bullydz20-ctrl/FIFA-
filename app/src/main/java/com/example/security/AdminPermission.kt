package com.example.security

import com.example.data.local.AdminAccountEntity
import com.example.data.model.AdminRole

enum class AdminPermission(
    val titleAr: String,
    val descriptionAr: String
) {
    MANAGE_TOURNAMENT(
        titleAr = "إدارة البطولة",
        descriptionAr = "تعديل اسم البطولة والقواعد والعدد والموعد"
    ),
    MANAGE_PLAYERS(
        titleAr = "إدارة المشاركين",
        descriptionAr = "قبول أو رفض أو تعديل أو حذف اللاعبين"
    ),
    RUN_DRAW(
        titleAr = "إجراء القرعة",
        descriptionAr = "توزيع المجموعات وتوليد جداول المباريات والإقصائيات"
    ),
    EDIT_SCORES(
        titleAr = "تسجيل النتائج",
        descriptionAr = "إدخال وتعديل نتائج المباريات وركلات الترجيح"
    ),
    CROWN_CHAMPION(
        titleAr = "تتويج البطل",
        descriptionAr = "اعتماد بطل البطولة وإعلان النتائج النهائية"
    )
}

enum class SubAdminPreset(
    val labelAr: String,
    val permissions: Set<AdminPermission>
) {
    SCOREKEEPER(
        labelAr = "مراقب نتائج",
        permissions = setOf(AdminPermission.EDIT_SCORES)
    ),
    REGISTRAR(
        labelAr = "مسؤول تسجيل",
        permissions = setOf(AdminPermission.MANAGE_PLAYERS)
    ),
    ORGANIZER(
        labelAr = "منسق مباريات",
        permissions = setOf(AdminPermission.MANAGE_PLAYERS, AdminPermission.RUN_DRAW, AdminPermission.EDIT_SCORES)
    ),
    CO_DIRECTOR(
        labelAr = "نائب مشرف",
        permissions = AdminPermission.entries.toSet()
    )
}

fun AdminAccountEntity.hasVerifiedPermissionsIntegrity(): Boolean {
    return PinSecurityManager.verifyPermissionsHash(
        username = username,
        role = role.name,
        canManageTournament = canManageTournament,
        canManagePlayers = canManagePlayers,
        canRunDraw = canRunDraw,
        canEditScores = canEditScores,
        canCrownChampion = canCrownChampion,
        pinSalt = pinSalt,
        pinHash = pinHash,
        expectedPermissionsHash = permissionsHash
    )
}

fun AdminAccountEntity.hasPermission(permission: AdminPermission): Boolean {
    if (!isActive || !hasVerifiedPermissionsIntegrity()) return false
    if (role == AdminRole.HEAD_ADMIN) return true
    return when (permission) {
        AdminPermission.MANAGE_TOURNAMENT -> canManageTournament
        AdminPermission.MANAGE_PLAYERS -> canManagePlayers
        AdminPermission.RUN_DRAW -> canRunDraw
        AdminPermission.EDIT_SCORES -> canEditScores
        AdminPermission.CROWN_CHAMPION -> canCrownChampion
    }
}

fun AdminAccountEntity.grantedPermissions(): Set<AdminPermission> {
    if (!isActive || !hasVerifiedPermissionsIntegrity()) return emptySet()
    if (role == AdminRole.HEAD_ADMIN) return AdminPermission.entries.toSet()
    return AdminPermission.entries.filter { hasPermission(it) }.toSet()
}
