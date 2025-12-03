Task: Implement Smart Canvas UX Features
Context
This is a lightweight Canvas Note App built with Kotlin and Jetpack Compose. We are adding two specific UX improvements to handle card creation and off-screen editing.

Objectives
Smart Scrolling: Automatically scroll the canvas when a new card is created near screen edges or under the keyboard.

Ghost Input Bar: Replace the formatting toolbar with a "Ghost" text input field when the currently active card is scrolled off-screen.

Phase 1: Smart Scrolling (Creation Logic)
Target: The function or ViewModel handling the onLongPress or "Create Card" gesture.

Requirement: When a card is created at specific coordinates (touchX, touchY), calculate if it falls outside the "Safe Viewport". If it does, trigger a scroll event on the Canvas state.

Logic to Implement:

Define SafeViewport as:

Width: ScreenWidth

Height: ScreenHeight - KeyboardHeight

Calculate CardBottomRight = (touchX + CardWidth, touchY + CardHeight).

Calculate Delta:

DeltaX: Amount the card overflows the right edge.

DeltaY: Amount the card overflows the bottom "Safe Viewport" (taking keyboard into account).

Action: If Delta > 0, scroll the canvas by Delta + Padding.

Code Snippet (Reference):

Kotlin

// Use this logic inside the creation handler
val padding = 50f // Px
val screenBottomSafe = viewportSize.height - keyboardHeight

val overflowX = (touchX + cardWidth) - (viewportSize.width - padding)
val overflowY = (touchY + cardHeight) - (screenBottomSafe - padding)

var scrollDx = 0f
var scrollDy = 0f

if (overflowX > 0) scrollDx = overflowX
if (overflowY > 0) scrollDy = overflowY

if (scrollDx > 0 || scrollDy > 0) {
    // Trigger canvas scroll
    canvasState.scrollBy(scrollDx, scrollDy)
}
Phase 2: Context-Aware Toolbar (Ghost Input)
Target: The main Scaffold or Bottom Bar implementation.

Requirement: The bottom bar must react to the visibility of the activeCard.

State A (Visible): Show the existing FormattingToolbar.

State B (Off-Screen): Show the new GhostInputBar.

Step 2.1: Visibility Tracking
Add a check to determine if the active card is currently rendered within the viewport.

Logic:

Kotlin

fun isCardVisible(cardRect: Rect, viewportRect: Rect): Boolean {
    return cardRect.overlaps(viewportRect)
}
Step 2.2: The Ghost Input UI
Create a new Composable GhostInputBar.

Input: Must bind two-way to the same text state as the active card on the canvas.

UI: A simple row containing:

An Icon (Edit/Pen) indicating "Remote Mode".

A BasicTextField for typing.

A "Snap Back" button (LocationOn icon) that scrolls the canvas to center the active card.

Step 2.3: State Hoisting
Ensure the text state is hoisted. The CanvasCard and GhostInputBar must update the exact same source of truth in the ViewModel.

Composing the Toolbar:

Kotlin

// Pseudo-code structure for integration
@Composable
fun MainScreenBottomBar(
    isCardVisible: Boolean,
    activeCardText: String,
    onTextChange: (String) -> Unit,
    onSnapBack: () -> Unit
) {
    AnimatedContent(targetState = isCardVisible) { visible ->
        if (visible) {
            ExistingFormattingToolbar()
        } else {
            GhostInputBar(
                text = activeCardText,
                onValueChange = onTextChange,
                onSnapBack = onSnapBack
            )
        }
    }
}
Constraints & Style
Comments: All code comments must be in English.

Changes: Only modify files necessary for these two features. Do not refactor unrelated parts of the canvas logic.

Keyboard: Ensure GhostInputBar uses .imePadding() or WindowInsets to sit correctly on top of the keyboard.