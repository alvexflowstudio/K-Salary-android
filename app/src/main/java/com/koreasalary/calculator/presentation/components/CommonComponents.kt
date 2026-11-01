package com.koreasalary.calculator.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.koreasalary.calculator.presentation.theme.LocalAppPalette
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object CurrencyFormatUtil {
    private val wonFormatter = DecimalFormat("#,###")

    fun formatWon(amount: Long): String {
        return wonFormatter.format(amount)
    }

    fun formatWon(amount: Double): String {
        return if (amount.isFinite() && amount > 0.0) {
            wonFormatter.format(
                BigDecimal.valueOf(amount)
                    .setScale(0, RoundingMode.HALF_UP)
                    .toLong()
            )
        } else {
            wonFormatter.format(0L)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AppInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    helperText: String? = null,
    suffixText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onFocusChange: (Boolean) -> Unit = {},
    inputModifier: Modifier = Modifier,
    compactLabel: Boolean = false
) {
    val palette = LocalAppPalette.current
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var wasFocused by remember { mutableStateOf(false) }

    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(wasFocused, imeBottom) {
        if (wasFocused) {
            // Insets change during keyboard animation; wait for the resized viewport.
            withFrameNanos { }
            bringIntoViewRequester.bringIntoView()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    label,
                    style = if (compactLabel) MaterialTheme.typography.bodySmall else LocalTextStyle.current,
                    maxLines = if (compactLabel) 1 else Int.MAX_VALUE,
                    softWrap = !compactLabel
                )
            },
            suffix = suffixText?.let { { Text(it, fontWeight = FontWeight.SemiBold) } },
            trailingIcon = trailingContent,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            enabled = enabled,
            readOnly = readOnly,
            isError = isError,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
                onNext = { focusManager.moveFocus(FocusDirection.Next) }
            ),
            modifier = inputModifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    if (keyboardType != KeyboardType.Text) {
                        if (focusState.isFocused && !wasFocused && value.toDoubleOrNull() == 0.0) {
                            onValueChange("")
                        } else if (!focusState.isFocused && wasFocused && value.isBlank()) {
                            onValueChange("0")
                        }
                    }
                    wasFocused = focusState.isFocused
                    onFocusChange(focusState.isFocused)
                }
        )
        if (!helperText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error else palette.tertiaryText,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun QuickActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val palette = LocalAppPalette.current

    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                palette.accent
            } else {
                palette.cardBorder.copy(alpha = 0.42f)
            },
            contentColor = if (isSelected) palette.onAccent else palette.primaryText
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = modifier.heightIn(min = 34.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
