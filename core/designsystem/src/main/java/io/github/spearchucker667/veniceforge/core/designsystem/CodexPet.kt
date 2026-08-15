package io.github.spearchucker667.veniceforge.core.designsystem

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Semantic states corresponding to the Codex Pet v2 animation atlas rows.
 */
enum class CodexPetState(
    val row: Int,
    val frameCount: Int,
    val defaultFps: Int = 8,
) {
    Idle(row = 0, frameCount = 6, defaultFps = 4),
    RunningRight(row = 1, frameCount = 8, defaultFps = 8),
    RunningLeft(row = 2, frameCount = 8, defaultFps = 8),
    Waving(row = 3, frameCount = 4, defaultFps = 6),
    Jumping(row = 4, frameCount = 5, defaultFps = 8),
    Failed(row = 5, frameCount = 8, defaultFps = 8),
    Waiting(row = 6, frameCount = 6, defaultFps = 6),
    ActiveTask(row = 7, frameCount = 6, defaultFps = 8),
    Review(row = 8, frameCount = 6, defaultFps = 6),
}

/**
 * Metadata constants for the Codex Pet v2 fixed-cell spritesheet.
 */
object CodexPetAtlasContract {
    const val COLUMNS = 8
    const val ROWS = 11
    const val FRAME_WIDTH = 192
    const val FRAME_HEIGHT = 208
    const val ASPECT_RATIO = FRAME_WIDTH.toFloat() / FRAME_HEIGHT.toFloat()
    @DrawableRes
    val DEFAULT_SPRITESHEET_RES = R.drawable.ayanami_rei_spritesheet
}

/**
 * Fixed-cell Codex Pet v2 Compose Renderer.
 *
 * Renders state animations from an 8x11 atlas without slicing or allocating bitmaps per frame.
 * Respects [reducedMotion] by freezing on frame 0 of the selected state.
 */
@Composable
fun CodexPetRenderer(
    state: CodexPetState,
    modifier: Modifier = Modifier,
    @DrawableRes spritesheetRes: Int = CodexPetAtlasContract.DEFAULT_SPRITESHEET_RES,
    fps: Int = state.defaultFps,
    reducedMotion: Boolean = false,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val imageBitmap: ImageBitmap? = remember(spritesheetRes) {
        val options = BitmapFactory.Options().apply {
            inScaled = false // Do not scale in memory to preserve exact 192x208 pixel grid
        }
        val androidBitmap = BitmapFactory.decodeResource(context.resources, spritesheetRes, options)
        androidBitmap?.asImageBitmap()
    }

    if (imageBitmap == null) return

    var currentFrameIndex by remember(state, reducedMotion) { mutableIntStateOf(0) }

    if (!reducedMotion && state.frameCount > 1) {
        val frameDurationMs = (1000L / fps.coerceAtLeast(1))
        LaunchedEffect(state, fps) {
            while (isActive) {
                delay(frameDurationMs)
                currentFrameIndex = (currentFrameIndex + 1) % state.frameCount
            }
        }
    }

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .aspectRatio(CodexPetAtlasContract.ASPECT_RATIO)
            .then(semanticsModifier),
    ) {
        val srcX = currentFrameIndex * CodexPetAtlasContract.FRAME_WIDTH
        val srcY = state.row * CodexPetAtlasContract.FRAME_HEIGHT

        drawImage(
            image = imageBitmap,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(CodexPetAtlasContract.FRAME_WIDTH, CodexPetAtlasContract.FRAME_HEIGHT),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        )
    }
}
