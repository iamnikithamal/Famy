package com.famy.tree.ui.screen.tree.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.model.Relationship
import com.famy.tree.domain.model.TreeBounds
import com.famy.tree.domain.model.TreeLayoutConfig
import com.famy.tree.domain.model.TreeNode
import com.famy.tree.ui.theme.FemaleCardColor
import com.famy.tree.ui.theme.FemaleCardColorDark
import com.famy.tree.ui.theme.MaleCardColor
import com.famy.tree.ui.theme.MaleCardColorDark
import com.famy.tree.ui.theme.OtherCardColor
import com.famy.tree.ui.theme.OtherCardColorDark
import com.famy.tree.ui.theme.PaternalLineColor
import com.famy.tree.ui.theme.SpouseLineColor
import com.famy.tree.ui.theme.UnknownCardColor
import com.famy.tree.ui.theme.UnknownCardColorDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val PHOTO_CACHE_SIZE = 50
private const val AVATAR_SIZE = 48

@Composable
fun TreeCanvas(
    nodes: List<TreeNode>,
    relationships: List<Relationship>,
    bounds: TreeBounds,
    config: TreeLayoutConfig,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    selectedMemberId: Long?,
    onMemberClick: (Long) -> Unit,
    onMemberLongClick: (Long) -> Unit,
    onScaleChange: (Float) -> Unit,
    onPan: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var localScale by remember { mutableFloatStateOf(scale) }
    var localOffsetX by remember { mutableFloatStateOf(offsetX) }
    var localOffsetY by remember { mutableFloatStateOf(offsetY) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val photoCache = remember { mutableStateMapOf<String, Bitmap?>() }

    LaunchedEffect(nodes) {
        nodes.forEach { node ->
            node.member.photoPath?.let { path ->
                if (!photoCache.containsKey(path)) {
                    withContext(Dispatchers.IO) {
                        val bitmap = loadAndScaleBitmap(path, AVATAR_SIZE)
                        photoCache[path] = bitmap
                    }
                }
            }
        }
        if (photoCache.size > PHOTO_CACHE_SIZE) {
            val keysToRemove = photoCache.keys.take(photoCache.size - PHOTO_CACHE_SIZE)
            keysToRemove.forEach { key ->
                photoCache[key]?.recycle()
                photoCache.remove(key)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            photoCache.values.filterNotNull().forEach { it.recycle() }
            photoCache.clear()
        }
    }

    val nodeWidth = config.nodeWidth
    val nodeHeight = config.nodeHeight
    val cornerRadius = with(density) { 12.dp.toPx() }

    val textPaint = remember(textColor) {
        Paint().apply {
            color = textColor.toArgb()
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    val subtitlePaint = remember(textColor) {
        Paint().apply {
            color = textColor.copy(alpha = 0.7f).toArgb()
            textSize = 24f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (localScale * zoom).coerceIn(0.2f, 3f)
                    localScale = newScale
                    localOffsetX += pan.x
                    localOffsetY += pan.y
                    onScaleChange(zoom)
                    onPan(pan.x, pan.y)
                }
            }
            .pointerInput(nodes) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val transformedTap = transformTapToCanvas(
                            tapOffset,
                            canvasSize,
                            localOffsetX,
                            localOffsetY,
                            localScale
                        )
                        nodes.find { node ->
                            val nodeRect = Rect(
                                offset = Offset(node.x, node.y),
                                size = Size(nodeWidth, nodeHeight)
                            )
                            nodeRect.contains(transformedTap)
                        }?.let { node ->
                            onMemberClick(node.member.id)
                        }
                    },
                    onLongPress = { tapOffset ->
                        val transformedTap = transformTapToCanvas(
                            tapOffset,
                            canvasSize,
                            localOffsetX,
                            localOffsetY,
                            localScale
                        )
                        nodes.find { node ->
                            val nodeRect = Rect(
                                offset = Offset(node.x, node.y),
                                size = Size(nodeWidth, nodeHeight)
                            )
                            nodeRect.contains(transformedTap)
                        }?.let { node ->
                            onMemberLongClick(node.member.id)
                        }
                    }
                )
            }
    ) {
        canvasSize = size

        val viewportRect = calculateViewport(
            canvasSize = size,
            offsetX = localOffsetX,
            offsetY = localOffsetY,
            scale = localScale,
            padding = maxOf(nodeWidth, nodeHeight) * 2
        )

        clipRect {
            drawContext.canvas.nativeCanvas.save()

            drawContext.canvas.nativeCanvas.translate(
                size.width / 2 + localOffsetX,
                size.height / 2 + localOffsetY
            )
            drawContext.canvas.nativeCanvas.scale(localScale, localScale)

            val centerOffsetX = -bounds.centerX
            val centerOffsetY = -bounds.centerY

            val visibleNodes = nodes.filter { node ->
                isNodeVisible(
                    node = node,
                    nodeWidth = nodeWidth,
                    nodeHeight = nodeHeight,
                    viewportRect = viewportRect,
                    centerOffsetX = centerOffsetX,
                    centerOffsetY = centerOffsetY
                )
            }

            drawConnections(
                nodes = nodes,
                config = config,
                lineColor = outlineColor,
                spouseLineColor = SpouseLineColor,
                parentChildColor = PaternalLineColor,
                centerOffsetX = centerOffsetX,
                centerOffsetY = centerOffsetY
            )

            visibleNodes.forEach { node ->
                drawMemberNode(
                    node = node,
                    nodeWidth = nodeWidth,
                    nodeHeight = nodeHeight,
                    cornerRadius = cornerRadius,
                    isSelected = node.member.id == selectedMemberId,
                    isDarkTheme = isDarkTheme,
                    textPaint = textPaint,
                    subtitlePaint = subtitlePaint,
                    outlineColor = outlineColor,
                    selectedColor = primaryColor,
                    centerOffsetX = centerOffsetX,
                    centerOffsetY = centerOffsetY,
                    photoBitmap = node.member.photoPath?.let { photoCache[it] }
                )
            }

            drawContext.canvas.nativeCanvas.restore()
        }
    }
}

private fun transformTapToCanvas(
    tapOffset: Offset,
    canvasSize: Size,
    offsetX: Float,
    offsetY: Float,
    scale: Float
): Offset {
    val x = (tapOffset.x - canvasSize.width / 2 - offsetX) / scale
    val y = (tapOffset.y - canvasSize.height / 2 - offsetY) / scale
    return Offset(x, y)
}

private fun calculateViewport(
    canvasSize: Size,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    padding: Float
): Rect {
    val halfWidth = (canvasSize.width / 2 / scale) + padding
    val halfHeight = (canvasSize.height / 2 / scale) + padding
    val centerX = -offsetX / scale
    val centerY = -offsetY / scale

    return Rect(
        left = centerX - halfWidth,
        top = centerY - halfHeight,
        right = centerX + halfWidth,
        bottom = centerY + halfHeight
    )
}

private fun isNodeVisible(
    node: TreeNode,
    nodeWidth: Float,
    nodeHeight: Float,
    viewportRect: Rect,
    centerOffsetX: Float,
    centerOffsetY: Float
): Boolean {
    val nodeLeft = node.x + centerOffsetX
    val nodeTop = node.y + centerOffsetY
    val nodeRight = nodeLeft + nodeWidth
    val nodeBottom = nodeTop + nodeHeight

    return !(nodeRight < viewportRect.left ||
            nodeLeft > viewportRect.right ||
            nodeBottom < viewportRect.top ||
            nodeTop > viewportRect.bottom)
}

private fun DrawScope.drawConnections(
    nodes: List<TreeNode>,
    config: TreeLayoutConfig,
    lineColor: Color,
    spouseLineColor: Color,
    parentChildColor: Color,
    centerOffsetX: Float,
    centerOffsetY: Float
) {
    val strokeWidth = 2f
    val nodeWidth = config.nodeWidth
    val nodeHeight = config.nodeHeight

    nodes.forEach { parentNode ->
        val parentCenterX = parentNode.x + centerOffsetX + nodeWidth / 2
        val parentBottomY = parentNode.y + centerOffsetY + nodeHeight

        parentNode.children.forEach { childNode ->
            val childCenterX = childNode.x + centerOffsetX + nodeWidth / 2
            val childTopY = childNode.y + centerOffsetY

            val path = Path().apply {
                moveTo(parentCenterX, parentBottomY)
                val midY = (parentBottomY + childTopY) / 2
                cubicTo(
                    parentCenterX, midY,
                    childCenterX, midY,
                    childCenterX, childTopY
                )
            }

            drawPath(
                path = path,
                color = parentChildColor.copy(alpha = 0.6f),
                style = Stroke(width = strokeWidth)
            )
        }

        parentNode.spouses.forEachIndexed { index, spouse ->
            val spouseNode = nodes.find { it.member.id == spouse.id }
            if (spouseNode != null) {
                val spouseCenterX = spouseNode.x + centerOffsetX + nodeWidth / 2
                val spouseCenterY = spouseNode.y + centerOffsetY + nodeHeight / 2
                val nodeCenterX = parentNode.x + centerOffsetX + nodeWidth / 2
                val nodeCenterY = parentNode.y + centerOffsetY + nodeHeight / 2

                drawLine(
                    color = spouseLineColor.copy(alpha = 0.6f),
                    start = Offset(nodeCenterX + nodeWidth / 2, nodeCenterY),
                    end = Offset(spouseCenterX - nodeWidth / 2, spouseCenterY),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

private fun DrawScope.drawMemberNode(
    node: TreeNode,
    nodeWidth: Float,
    nodeHeight: Float,
    cornerRadius: Float,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    textPaint: Paint,
    subtitlePaint: Paint,
    outlineColor: Color,
    selectedColor: Color,
    centerOffsetX: Float,
    centerOffsetY: Float,
    photoBitmap: Bitmap? = null
) {
    val member = node.member
    val x = node.x + centerOffsetX
    val y = node.y + centerOffsetY

    val backgroundColor = getGenderColor(member.gender, isDarkTheme)

    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(
                    offset = Offset(x, y),
                    size = Size(nodeWidth, nodeHeight)
                ),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        )
    }

    drawPath(path = path, color = backgroundColor)

    val strokeColor = if (isSelected) selectedColor else outlineColor
    val strokeWidth = if (isSelected) 4f else 1f

    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(width = strokeWidth)
    )

    val avatarRadius = 24f
    val avatarCenterX = x + nodeWidth / 2
    val avatarCenterY = y + 35f

    if (photoBitmap != null && !photoBitmap.isRecycled) {
        val photoPaint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(photoBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                val scale = (avatarRadius * 2) / photoBitmap.width.toFloat()
                val matrix = Matrix()
                matrix.setScale(scale, scale)
                matrix.postTranslate(avatarCenterX - avatarRadius, avatarCenterY - avatarRadius)
                setLocalMatrix(matrix)
            }
        }
        drawContext.canvas.nativeCanvas.drawCircle(
            avatarCenterX,
            avatarCenterY,
            avatarRadius,
            photoPaint
        )
        drawCircle(
            color = outlineColor.copy(alpha = 0.5f),
            radius = avatarRadius,
            center = Offset(avatarCenterX, avatarCenterY),
            style = Stroke(width = 2f)
        )
    } else {
        drawCircle(
            color = outlineColor.copy(alpha = 0.3f),
            radius = avatarRadius,
            center = Offset(avatarCenterX, avatarCenterY)
        )
        val initialPaint = Paint().apply {
            color = outlineColor.copy(alpha = 0.7f).toArgb()
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText(
            member.firstName.take(1).uppercase(),
            avatarCenterX,
            avatarCenterY + 10f,
            initialPaint
        )
    }

    val genderIndicatorColor = when (member.gender) {
        Gender.MALE -> Color(0xFF2196F3)
        Gender.FEMALE -> Color(0xFFE91E63)
        Gender.OTHER -> Color(0xFF9C27B0)
        Gender.UNKNOWN -> Color(0xFF9E9E9E)
    }

    drawCircle(
        color = genderIndicatorColor,
        radius = 5f,
        center = Offset(x + nodeWidth - 12f, y + 12f)
    )

    if (!member.isLiving) {
        drawLine(
            color = outlineColor.copy(alpha = 0.5f),
            start = Offset(x + 8f, y + 8f),
            end = Offset(x + 20f, y + 20f),
            strokeWidth = 2f
        )
        drawLine(
            color = outlineColor.copy(alpha = 0.5f),
            start = Offset(x + 20f, y + 8f),
            end = Offset(x + 8f, y + 20f),
            strokeWidth = 2f
        )
    }

    drawContext.canvas.nativeCanvas.drawText(
        member.firstName.take(12),
        x + nodeWidth / 2,
        y + 80f,
        textPaint
    )

    member.lastName?.let { lastName ->
        drawContext.canvas.nativeCanvas.drawText(
            lastName.take(12),
            x + nodeWidth / 2,
            y + 105f,
            subtitlePaint
        )
    }

    member.age?.let { age ->
        val ageText = if (member.isLiving) "$age" else "($age)"
        drawContext.canvas.nativeCanvas.drawText(
            ageText,
            x + nodeWidth / 2,
            y + nodeHeight - 10f,
            subtitlePaint
        )
    }
}

private fun getGenderColor(gender: Gender, isDarkTheme: Boolean): Color {
    return when (gender) {
        Gender.MALE -> if (isDarkTheme) MaleCardColorDark else MaleCardColor
        Gender.FEMALE -> if (isDarkTheme) FemaleCardColorDark else FemaleCardColor
        Gender.OTHER -> if (isDarkTheme) OtherCardColorDark else OtherCardColor
        Gender.UNKNOWN -> if (isDarkTheme) UnknownCardColorDark else UnknownCardColor
    }
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}

private fun loadAndScaleBitmap(path: String, targetSize: Int): Bitmap? {
    return try {
        val file = File(path)
        if (!file.exists()) return null

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        val sampleSize = calculateInSampleSize(options.outWidth, options.outHeight, targetSize)

        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize

        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }
        scaledBitmap
    } catch (e: Exception) {
        null
    }
}

private fun calculateInSampleSize(width: Int, height: Int, targetSize: Int): Int {
    var inSampleSize = 1
    if (width > targetSize || height > targetSize) {
        val halfWidth = width / 2
        val halfHeight = height / 2
        while ((halfWidth / inSampleSize) >= targetSize && (halfHeight / inSampleSize) >= targetSize) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
