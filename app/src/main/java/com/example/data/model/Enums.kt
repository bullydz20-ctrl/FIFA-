package com.example.data.model

enum class TournamentFormat(val labelAr: String, val descriptionAr: String) {
    GROUPS_KNOCKOUT("مجموعات + أدوار إقصائية", "توزيع على مجموعات ثم تأهل المتصدرين للأدوار الإقصائية"),
    LEAGUE_KNOCKOUT("دوري + أدوار إقصائية", "مواجهات بنظام الدوري ثم تأهل أفضل اللاعبين للإقصائيات"),
    DIRECT_KNOCKOUT("خروج مباشر (إقصائيات فقط)", "مواجهات إقصائية مباشرة من الدور الأول حتى النهائي")
}

enum class DrawMode(val labelAr: String) {
    RANDOM("قرعة عشوائية تلقائية"),
    MANUAL("قرعة يدوية مخصصة")
}

enum class KnockoutTieRule(val labelAr: String, val shortAr: String) {
    SINGLE_MATCH_PENALTIES(
        "مباراة واحدة (مع أشواط إضافية وركلات ترجيح عند التعادل)",
        "مباراة واحدة"
    ),
    TWO_LEGS_FINAL_SINGLE(
        "ذهاب وإياب حتى نصف النهائي + نهائي من مباراة واحدة",
        "ذهاب وإياب"
    )
}

enum class PlayerStatus(val labelAr: String) {
    PENDING("بانتظار الموافقة"),
    APPROVED("معتمد"),
    REJECTED("مرفوض")
}

enum class PlayerLevel(val labelAr: String, val defaultOvr: Int) {
    BEGINNER("مبتدئ", 74),
    INTERMEDIATE("متوسط", 81),
    PRO("محترف", 88),
    LEGEND("أسطوري", 94)
}

enum class MatchStage(val labelAr: String, val order: Int) {
    GROUP("دور المجموعات", 1),
    LEAGUE("مرحلة الدوري", 1),
    ROUND_OF_16("دور الـ 16", 2),
    QUARTER_FINAL("ربع النهائي", 3),
    SEMI_FINAL("نصف النهائي", 4),
    FINAL("المباراة النهائية", 5)
}

enum class AdminRole(val labelAr: String) {
    HEAD_ADMIN("المشرف الرئيسي"),
    SUB_ADMIN("مشرف مساعد")
}

enum class TournamentStatus(val labelAr: String) {
    REGISTRATION_OPEN("التسجيل مفتوح"),
    REGISTRATION_CLOSED("التسجيل مغلق"),
    GROUP_STAGE("مرحلة المجموعات / الدوري"),
    KNOCKOUT_STAGE("الأدوار الإقصائية"),
    COMPLETED("انتهت البطولة")
}
