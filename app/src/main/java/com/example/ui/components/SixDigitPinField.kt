package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.then
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.PinSecurityManager
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.CyberSky
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ScoreDisplayStyle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SixDigitPinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {},
    supportingText: String? = null
) {
    var revealDigits by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val textFieldState = rememberTextFieldState(initialText = value.filter { it.isDigit() }.take(PinSecurityManager.PIN_LENGTH))
    var latestPropagatedValue by remember { mutableStateOf(textFieldState.text.toString()) }

    // Sync external value changes (e.g. parent clearing PIN) into TextFieldState without disrupting active typing
    LaunchedEffect(value) {
        val normalized = value.filter { it.isDigit() }.take(PinSecurityManager.PIN_LENGTH)
        if (normalized != latestPropagatedValue && textFieldState.text.toString() != normalized) {
            latestPropagatedValue = normalized
            textFieldState.setTextAndPlaceCursorAtEnd(normalized)
        }
    }

    // Propagate atomic TextFieldState updates back to parent state
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { current ->
                latestPropagatedValue = current
                if (current != value) {
                    onValueChange(current)
                }
            }
    }

    val digitsOnlyMaxLengthTransformation = remember {
        InputTransformation.maxLength(PinSecurityManager.PIN_LENGTH).then {
            val current = asCharSequence()
            if (current.any { !it.isDigit() }) {
                val filtered = current.filter { it.isDigit() }.take(PinSecurityManager.PIN_LENGTH)
                replace(0, length, filtered)
            }
        }
    }

    val currentText = textFieldState.text.toString()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = if (isError) CrimsonLoss else ChampionshipGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isError) CrimsonLoss else TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (currentText.length == PinSecurityManager.PIN_LENGTH) {
                        EmeraldWin.copy(alpha = 0.16f)
                    } else {
                        ElevatedNavy
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (currentText.length == PinSecurityManager.PIN_LENGTH) EmeraldWin.copy(alpha = 0.6f) else CardBorder
                    )
                ) {
                    Text(
                        text = "${currentText.length}/${PinSecurityManager.PIN_LENGTH}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (currentText.length == PinSecurityManager.PIN_LENGTH) EmeraldWin else CyberSky,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                IconButton(
                    onClick = { revealDigits = !revealDigits },
                    enabled = enabled,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("${testTag}_visibility_toggle")
                ) {
                    Icon(
                        imageVector = if (revealDigits) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (revealDigits) "إخفاء رمز PIN" else "إظهار رمز PIN",
                        tint = CyberSky
                    )
                }
            }
        }

        // Numeric PIN boxes are always ordered Left-to-Right regardless of RTL app locale
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BasicTextField(
                state = textFieldState,
                enabled = enabled,
                inputTransformation = digitsOnlyMaxLengthTransformation,
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle = TextStyle(color = Color.Transparent, fontSize = 18.sp),
                cursorBrush = SolidColor(Color.Transparent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = imeAction
                ),
                onKeyboardAction = { performDefaultAction ->
                    onDone()
                    performDefaultAction()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .testTag(testTag),
                decorator = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Keep full-width innerTextField behind boxes so IME & pointer bounds stay accurate without 1dp wrapping
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0f)
                        ) {
                            innerTextField()
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (index in 0 until PinSecurityManager.PIN_LENGTH) {
                                val digit = currentText.getOrNull(index)
                                val isCurrentSlot = isFocused && (
                                    index == currentText.length ||
                                        (currentText.length == PinSecurityManager.PIN_LENGTH && index == PinSecurityManager.PIN_LENGTH - 1)
                                    )
                                val borderColor by animateColorAsState(
                                    targetValue = when {
                                        isError -> CrimsonLoss
                                        isCurrentSlot -> ChampionshipGold
                                        digit != null -> ElectricCobalt
                                        else -> CardBorder
                                    },
                                    label = "pin_cell_border"
                                )
                                val containerColor by animateColorAsState(
                                    targetValue = when {
                                        isCurrentSlot -> ElectricCobalt.copy(alpha = 0.2f)
                                        digit != null -> ElevatedNavy
                                        else -> ObsidianVoid.copy(alpha = 0.72f)
                                    },
                                    label = "pin_cell_bg"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(containerColor)
                                        .border(
                                            width = if (isCurrentSlot) 2.dp else 1.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        digit != null && revealDigits -> {
                                            Text(
                                                text = digit.toString(),
                                                style = ScoreDisplayStyle.copy(
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = ChampionshipGold
                                            )
                                        }
                                        digit != null -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(ChampionshipGold)
                                            )
                                        }
                                        isCurrentSlot -> {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(22.dp)
                                                    .background(ChampionshipGold)
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextMuted.copy(alpha = 0.45f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isError) CrimsonLoss else TextSecondary
            )
        }
    }
}
