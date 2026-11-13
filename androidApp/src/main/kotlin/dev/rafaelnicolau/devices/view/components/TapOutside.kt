package dev.rafaelnicolau.devices.view.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Clears focus, and with it closes the keyboard, when the user taps anywhere outside
 * [keepFocusIn] (in root coordinates). Same behavior as the iOS dashboard.
 *
 * The taps are only observed, never consumed, so buttons, chips and rows under the finger
 * still receive them. A drag (scrolling) is not a tap and keeps the keyboard open.
 */
fun Modifier.clearFocusOnTapOutside(keepFocusIn: () -> Rect?): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val holder = remember { CoordinatesHolder() }
    onGloballyPositioned { holder.coordinates = it }
        .pointerInput(focusManager) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial) ?: return@awaitEachGesture
                val isTap = (up.position - down.position).getDistance() < viewConfiguration.touchSlop
                if (!isTap) return@awaitEachGesture
                val inRoot = holder.coordinates?.takeIf { it.isAttached }?.localToRoot(up.position) ?: up.position
                if (keepFocusIn()?.contains(inRoot) != true) focusManager.clearFocus()
            }
        }
}

/** Where the modified layout is, so a tap position can be converted to root coordinates. */
private class CoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
}
