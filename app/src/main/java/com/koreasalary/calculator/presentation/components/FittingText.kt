package com.koreasalary.calculator.presentation.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Keep the normal typography whenever it fits; never hide a pay amount behind ellipsis. */
@Composable
internal fun FittingText(
    text: String,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    softWrap: Boolean = maxLines > 1,
    scaleLineHeight: Boolean = false
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val resolved = LocalTextStyle.current.merge(style).merge(TextStyle(
        color = color, fontSize = fontSize, fontWeight = fontWeight,
        letterSpacing = letterSpacing, textAlign = textAlign ?: TextAlign.Unspecified
    ))
    BoxWithConstraints(modifier, propagateMinConstraints = true) {
        val available = constraints
        val fitted = remember(text, resolved, available, density.density, density.fontScale, maxLines, softWrap, scaleLineHeight) {
            val nominal = resolved.fontSize.value
            fun at(size: Float): TextStyle = resolved.copy(
                fontSize = size.sp,
                lineHeight = if (scaleLineHeight && resolved.lineHeight != TextUnit.Unspecified)
                    (resolved.lineHeight.value * size / nominal).sp else resolved.lineHeight
            )
            fun fits(style: TextStyle): Boolean {
                val layout = measurer.measure(text, style, softWrap = softWrap, maxLines = maxLines,
                    constraints = Constraints(maxWidth = available.maxWidth, maxHeight = available.maxHeight))
                return !layout.multiParagraph.didExceedMaxLines && !layout.didOverflowHeight &&
                    (0 until layout.lineCount).all { layout.getLineRight(it) <= available.maxWidth + 0.5f && layout.getLineLeft(it) >= -0.5f }
            }
            if (fits(resolved)) resolved else {
                var low = 0.5f
                var high = nominal
                repeat(12) {
                    val candidate = (low + high) / 2f
                    if (fits(at(candidate))) low = candidate else high = candidate
                }
                at(low)
            }
        }
        Text(text, modifier = textModifier, style = fitted,
            maxLines = maxLines, softWrap = softWrap, overflow = TextOverflow.Clip)
    }
}
