package com.transferrate.app.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Single-line chrome/label Text that shrinks its font to fit instead of
 * ellipsising at the boundary.  v0.31.1.
 *
 * Behaviour:
 *
 *   1. Start at [fontSize].  Lay out.
 *   2. If layout overflows the width:
 *      - reduce font by [stepSize]
 *      - re-lay-out
 *      - repeat until the text fits OR [minFontSize] is reached.
 *   3. If even [minFontSize] doesn't fit, draw with TextOverflow.Ellipsis
 *      as a last-resort safety net — never chop mid-glyph.
 *
 * Why this matters for Transfer Rate: the UI was originally laid out with
 * English glyph metrics.  Tamil "தங்கம்" / Malayalam "സ്വർണം" / Hindi "सोना"
 * occupy more horizontal space at the same sp than "GOLD" does, so labels
 * that fit in English clipped to "தங்..." in Tamil.  Static font reduction
 * would hurt English; AutoSizeText hits the right size per locale.
 *
 * Cost: one extra layout pass per shrink step (typically 1–3 steps for
 * non-Latin labels, zero for English).  The first frame draws blank to
 * avoid a visible "jump"; second frame is the settled size.  At 60 fps
 * that's ~16 ms — invisible to the user.
 *
 * Defaults aim at the most common label use-case: short chrome labels
 * (4–10 chars) at 10–14 sp.  Caller can override per site.
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    minFontSize: TextUnit = (fontSize.value * 0.65f).sp,
    stepSize: TextUnit = 0.5.sp,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    // Reset state when the text content changes (e.g. locale switch);
    // otherwise a previously-shrunk font would persist for a new word
    // that fits at the original size.
    var currentFontSize by remember(text, fontSize) { mutableStateOf(fontSize) }
    var readyToDraw by remember(text, fontSize) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        fontSize = currentFontSize,
        fontWeight = fontWeight,
        color = color,
        letterSpacing = letterSpacing,
        textAlign = textAlign,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result ->
            if (result.didOverflowWidth) {
                val next = (currentFontSize.value - stepSize.value).sp
                if (next.value >= minFontSize.value) {
                    currentFontSize = next
                } else {
                    // Reached the floor — draw with ellipsis fallback so
                    // the text never disappears entirely.
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        },
    )
}
