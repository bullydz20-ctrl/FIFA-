package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.AdminAccountEntity
import com.example.data.local.LockoutState
import com.example.data.model.AdminRole
import com.example.security.PinSecurityManager
import com.example.security.PinStrength
import com.example.ui.components.SixDigitPinInputField
import com.example.ui.theme.AmberDraw
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.CyberSky
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminAuthGate(
    hasHeadAdmin: Boolean,
    admins: List<AdminAccountEntity>,
    lockoutState: LockoutState,
    onSetupHeadAdmin: (username: String, displayName: String, pin: String, keepSignedIn: Boolean) -> Unit,
    onLogin: (username: String, pin: String, keepSignedIn: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isLocked = lockoutState.lockedUntilMillis > nowMillis
    val remainingSeconds = ((lockoutState.lockedUntilMillis - nowMillis) / 1000L).coerceAtLeast(0L)

    LaunchedEffect(lockoutState.lockedUntilMillis) {
        while (lockoutState.lockedUntilMillis > System.currentTimeMillis()) {
            nowMillis = System.currentTimeMillis()
            delay(500L)
        }
        nowMillis = System.currentTimeMillis()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (hasHeadAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Security,
                    contentDescription = null,
                    tint = ChampionshipGold,
                    modifier = Modifier.size(48.dp)
                )

                // Encryption & DataStore Assurance Pill
                Surface(
                    color = ElevatedNavy,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, ElectricCobalt.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = EmeraldWin,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تشفير PBKDF2-SHA256 • حفظ الجلسة عبر DataStore",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberSky
                        )
                    }
                }

                if (!hasHeadAdmin) {
                    var username by remember { mutableStateOf("admin") }
                    var displayName by remember { mutableStateOf("المشرف الرئيسي") }
                    var pin by remember { mutableStateOf("") }
                    var confirmPin by remember { mutableStateOf("") }
                    var keepSignedIn by remember { mutableStateOf(true) }
                    val strength = PinSecurityManager.evaluateStrength(pin)

                    Text(
                        text = "إعداد حساب المشرف الرئيسي",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "أنشئ رمز PIN مكون من 6 أرقام لتأمين لوحة التحكم محلياً بتشفير قوي، وبعدها يمكنك إضافة حتى 5 حسابات مساعدة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم للدخول") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyberSky) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_admin_username")
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("الاسم الظاهر") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_admin_display_name")
                    )

                    SixDigitPinInputField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = "رمز PIN المكون من 6 أرقام *",
                        testTag = "setup_admin_pin",
                        imeAction = ImeAction.Next,
                        supportingText = if (pin.isNotEmpty()) "قوة الرمز: ${strength.labelAr}" else "أدخل 6 أرقام سرية"
                    )

                    if (pin.isNotEmpty()) {
                        val strengthColor = when (strength) {
                            PinStrength.STRONG -> EmeraldWin
                            PinStrength.GOOD -> CyberSky
                            PinStrength.WEAK -> AmberDraw
                            PinStrength.INVALID -> CrimsonLoss
                        }
                        Surface(
                            color = strengthColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, strengthColor.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "حالة أمان الرمز: ${strength.labelAr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = strengthColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    val confirmMismatch = confirmPin.length == PinSecurityManager.PIN_LENGTH && confirmPin != pin
                    SixDigitPinInputField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it },
                        label = "تأكيد رمز PIN (6 أرقام) *",
                        testTag = "setup_admin_confirm_pin",
                        isError = confirmMismatch,
                        supportingText = if (confirmMismatch) "رمز التأكيد غير مطابق" else null,
                        onDone = {
                            if (username.trim().length >= 3 && PinSecurityManager.isValidPinFormat(pin) && pin == confirmPin) {
                                onSetupHeadAdmin(username, displayName, pin, keepSignedIn)
                            }
                        }
                    )

                    KeepSignedInRow(
                        checked = keepSignedIn,
                        onCheckedChange = { keepSignedIn = it },
                        testTag = "setup_keep_session_switch"
                    )

                    Button(
                        onClick = { onSetupHeadAdmin(username, displayName, pin, keepSignedIn) },
                        enabled = username.trim().length >= 3 && PinSecurityManager.isValidPinFormat(pin) && pin == confirmPin,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("setup_admin_submit")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("إنشاء حساب المشرف والدخول")
                    }
                } else {
                    var username by remember(admins) {
                        mutableStateOf(admins.firstOrNull { it.isActive }?.username ?: admins.firstOrNull()?.username ?: "")
                    }
                    var pin by remember { mutableStateOf("") }
                    var keepSignedIn by remember { mutableStateOf(true) }

                    Text(
                        text = "تسجيل دخول المشرف والمساعدين",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "اختر حسابك وأدخل رمز PIN المكون من 6 أرقام لفتح لوحة الإدارة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (isLocked) {
                        Surface(
                            color = CrimsonLoss.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CrimsonLoss),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_lockout_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LockClock, contentDescription = null, tint = CrimsonLoss)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تم قفل الدخول مؤقتاً للحماية من التخمين. المتبقي: $remainingSeconds ثانية",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CrimsonLoss
                                )
                            }
                        }
                    } else if (lockoutState.failedAttempts > 0) {
                        val left = (PinSecurityManager.MAX_FAILED_ATTEMPTS - lockoutState.failedAttempts).coerceAtLeast(1)
                        Text(
                            text = "تنبيه أمان: متبقي $left محاولات قبل القفل المؤقت",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberDraw
                        )
                    }

                    if (admins.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "اختر الحساب (${admins.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberSky
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                admins.forEach { acc ->
                                    val isHead = acc.role == AdminRole.HEAD_ADMIN
                                    val selected = username.equals(acc.username, ignoreCase = true)
                                    val suspendedLabel = if (!acc.isActive) " [موقوف]" else ""
                                    FilterChip(
                                        selected = selected,
                                        onClick = { username = acc.username },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isHead) Icons.Default.Shield else Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = { Text("${acc.displayName} (${acc.role.labelAr})$suspendedLabel") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (isHead) ChampionshipGold.copy(alpha = 0.24f) else ElectricCobalt,
                                            selectedLabelColor = if (isHead) ChampionshipGold else TextPrimary,
                                            selectedLeadingIconColor = if (isHead) ChampionshipGold else CyberSky
                                        ),
                                        modifier = Modifier.testTag("login_account_chip_${acc.username}")
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyberSky) },
                        singleLine = true,
                        enabled = !isLocked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_admin_username")
                    )

                    SixDigitPinInputField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = "رمز PIN المكون من 6 أرقام",
                        testTag = "login_admin_pin",
                        enabled = !isLocked,
                        supportingText = "التحقق يتم محلياً عبر PBKDF2-SHA256 دون إرسال الرمز لأي خادم",
                        onDone = {
                            if (!isLocked && username.isNotBlank() && PinSecurityManager.isValidPinFormat(pin)) {
                                onLogin(username, pin, keepSignedIn)
                            }
                        }
                    )

                    KeepSignedInRow(
                        checked = keepSignedIn,
                        onCheckedChange = { keepSignedIn = it },
                        enabled = !isLocked,
                        testTag = "login_keep_session_switch"
                    )

                    Button(
                        onClick = { onLogin(username, pin, keepSignedIn) },
                        enabled = !isLocked && username.isNotBlank() && PinSecurityManager.isValidPinFormat(pin),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("login_admin_submit")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("دخول إلى لوحة التحكم")
                    }
                }
            }
        }
    }
}

@Composable
private fun KeepSignedInRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    enabled: Boolean = true
) {
    Surface(
        color = ElevatedNavy.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "تذكر وحفظ الجلسة (DataStore)",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Text(
                    text = if (checked) "تبقى الجلسة مفتوحة بعد إعادة تشغيل التطبيق" else "جلسة مؤقتة تُغلق عند إعادة تشغيل التطبيق",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ChampionshipGold,
                    checkedTrackColor = ElectricCobalt
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
