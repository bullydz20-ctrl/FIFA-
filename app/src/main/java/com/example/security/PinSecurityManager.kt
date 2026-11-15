package com.example.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class PinStrength(val labelAr: String) {
    INVALID("أدخل 6 أرقام"),
    WEAK("ضعيف (يُنصح بتجنب التكرار أو التسلسل)"),
    GOOD("جيد ومشفر"),
    STRONG("قوي ومشفر")
}

object PinSecurityManager {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 65_536
    private const val KEY_LENGTH_BITS = 256
    private const val PREFIX_PBKDF2 = "pbkdf2_v1:"
    private val random = SecureRandom()

    const val PIN_LENGTH = 6
    const val MAX_SUB_ADMINS = 5
    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 30_000L

    fun isValidPinFormat(pin: String): Boolean {
        return pin.length == PIN_LENGTH && pin.all { it.isDigit() }
    }

    fun evaluateStrength(pin: String): PinStrength {
        if (!isValidPinFormat(pin)) return PinStrength.INVALID
        val allSame = pin.all { it == pin[0] }
        val sequentialAsc = "0123456789".contains(pin)
        val sequentialDesc = "9876543210".contains(pin)
        if (allSame || sequentialAsc || sequentialDesc) return PinStrength.WEAK
        return if (pin.toSet().size >= 4) PinStrength.STRONG else PinStrength.GOOD
    }

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hashPin(pin: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val derived = factory.generateSecret(spec).encoded
            PREFIX_PBKDF2 + Base64.encodeToString(derived, Base64.NO_WRAP)
        } finally {
            spec.clearPassword()
        }
    }

    fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
        if (pin.length !in 4..PIN_LENGTH || !pin.all { it.isDigit() }) return false
        return if (expectedHash.startsWith(PREFIX_PBKDF2)) {
            val computed = hashPin(pin, salt)
            MessageDigest.isEqual(
                computed.toByteArray(Charsets.UTF_8),
                expectedHash.toByteArray(Charsets.UTF_8)
            )
        } else {
            // Backward compatibility for legacy SHA-256 salted hashes
            val digest = MessageDigest.getInstance("SHA-256")
            val input = "$salt:$pin".toByteArray(Charsets.UTF_8)
            val legacy = Base64.encodeToString(digest.digest(input), Base64.NO_WRAP)
            MessageDigest.isEqual(
                legacy.toByteArray(Charsets.UTF_8),
                expectedHash.toByteArray(Charsets.UTF_8)
            )
        }
    }

    fun computePermissionsHash(
        username: String,
        role: String,
        canManageTournament: Boolean,
        canManagePlayers: Boolean,
        canRunDraw: Boolean,
        canEditScores: Boolean,
        canCrownChampion: Boolean,
        pinSalt: String,
        pinHash: String
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val payload = listOf(
            username.trim().lowercase(),
            role,
            canManageTournament.toString(),
            canManagePlayers.toString(),
            canRunDraw.toString(),
            canEditScores.toString(),
            canCrownChampion.toString(),
            pinSalt,
            pinHash
        ).joinToString("|")
        return "perm_v1:" + Base64.encodeToString(
            digest.digest(payload.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
    }

    fun verifyPermissionsHash(
        username: String,
        role: String,
        canManageTournament: Boolean,
        canManagePlayers: Boolean,
        canRunDraw: Boolean,
        canEditScores: Boolean,
        canCrownChampion: Boolean,
        pinSalt: String,
        pinHash: String,
        expectedPermissionsHash: String
    ): Boolean {
        if (expectedPermissionsHash.isBlank()) return true
        val computed = computePermissionsHash(
            username = username,
            role = role,
            canManageTournament = canManageTournament,
            canManagePlayers = canManagePlayers,
            canRunDraw = canRunDraw,
            canEditScores = canEditScores,
            canCrownChampion = canCrownChampion,
            pinSalt = pinSalt,
            pinHash = pinHash
        )
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            expectedPermissionsHash.toByteArray(Charsets.UTF_8)
        )
    }
}
