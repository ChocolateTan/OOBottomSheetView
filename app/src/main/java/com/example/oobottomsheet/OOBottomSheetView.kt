package com.example.oobottomsheet

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@Composable
fun Float.pxToDp(): Dp {
    val value = this
    return with(LocalDensity.current) {
        value.toDp()
    }
}

@Composable
fun OOBottomSheetView(
    updateOffsetY: SharedFlow<Float> = MutableSharedFlow(),
    lazyListState: LazyListState = rememberLazyListState(),
    contentPartOffsetY: Float? = null,
    contentMaxOffsetY: Float = 0f,
    contentStartOffsetY: Float = contentPartOffsetY ?: contentMaxOffsetY,
    contentView: @Composable () -> Unit,
) {
    val startOffsetY = contentMaxOffsetY
    val partOffsetY = contentPartOffsetY
    val endOffsetY = 0f

    val scope = rememberCoroutineScope()
    var offsetY by remember { mutableFloatStateOf(contentStartOffsetY) }
    val offsetYTarget by animateIntOffsetAsState(
        targetValue = IntOffset(0, offsetY.toInt()),
        label = "offset"
    )
    LaunchedEffect(Unit) {
        scope.launch {
            updateOffsetY.collect {
                offsetY = it
            }
        }
    }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val isDragUp = available.y < 0
                if (source == NestedScrollSource.UserInput) {
                    val nextOffsetY = offsetY + available.y
                    if (nextOffsetY <= endOffsetY) {
                        scope.launch {
                            offsetY = endOffsetY
                        }
                        return Offset.Zero
                    }
                    if (nextOffsetY >= startOffsetY) {
                        scope.launch {
                            offsetY = startOffsetY
                        }
                        return Offset.Zero
                    }
                    if (!isDragUp && lazyListState.canScrollBackward) {
                        return Offset.Zero
                    }
                    scope.launch {
                        offsetY = nextOffsetY
                    }
                    return Offset(0f, available.y)
                }
                if (isDragUp && !lazyListState.canScrollBackward) {
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val isDragUp = available.y < 0
                if (isDragUp) {
                    if (offsetY < startOffsetY) {
                        if (null != partOffsetY && offsetY >= partOffsetY && partOffsetY > 0) {
                            offsetY = partOffsetY
                        } else {
                            offsetY = endOffsetY
                        }
                    }
                } else {
                    if (offsetY > endOffsetY) {
                        if (null != partOffsetY && offsetY <= partOffsetY && partOffsetY > 0) {
                            offsetY = partOffsetY
                        } else {
                            offsetY = startOffsetY
                        }
                    }
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                return Velocity.Zero
            }
        }
    }
    var isDrag: Boolean by remember { mutableStateOf(false) }
    var isDragUp: Boolean by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .offset {
                offsetYTarget
            }
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragCancel = {
                    },
                    onDragEnd = {
                        if (isDrag) {
                            if (isDragUp) {
                                if (offsetY < startOffsetY) {
                                    if (null != partOffsetY && offsetY >= partOffsetY && partOffsetY > 0) {
                                        scope.launch {
                                            offsetY = partOffsetY
                                        }
                                    } else {
                                        scope.launch {
                                            offsetY = endOffsetY
                                        }
                                    }
                                }
                            } else {
                                if (offsetY > endOffsetY) {
                                    if (null != partOffsetY  && offsetY <= partOffsetY && partOffsetY > 0) {
                                        scope.launch {
                                            offsetY = partOffsetY
                                        }
                                    } else {
                                        scope.launch {
                                            offsetY = startOffsetY
                                        }
                                    }
                                }
                            }
                        }
                        isDrag = false
                    },
                    onDrag = { change, dragAmount ->
                        isDrag = true
                        isDragUp = dragAmount.y < 0
                        val nextOffsetY = offsetY + dragAmount.y
                        if (nextOffsetY <= endOffsetY) {
                            scope.launch {
                                offsetY = endOffsetY
                            }
                            return@detectDragGestures
                        }
                        if (nextOffsetY >= startOffsetY) {
                            scope.launch {
                                offsetY = startOffsetY
                            }
                            return@detectDragGestures
                        }
                        scope.launch {
                            offsetY = nextOffsetY
                        }
                    }
                )
            }
            .drawBehind {
                val shadowColor = Color.Black.copy(alpha = 0.25f)
                val shadowRadius = 20.dp.toPx()
                val shadowOffsetY = 0f

                drawIntoCanvas { canvas ->
                    val paint = Paint()
                    paint.color = shadowColor
                    paint
                        .asFrameworkPaint()
                        .setShadowLayer(shadowRadius, 0f, shadowOffsetY, shadowColor.toArgb())
                    withTransform({
                        translate(left = 0f, top = shadowOffsetY)
                    }) {
                        canvas.drawRoundRect(
                            0f,
                            0f,
                            size.width,
                            size.height,
                            20.dp
                                .roundToPx()
                                .toFloat(),
                            20.dp
                                .roundToPx()
                                .toFloat(),
                            paint
                        )
                    }
                }
            })
        {
            contentView.invoke()
        }
    }
}

@Composable
fun BottomSheetContent(
    closeBottomSheet: () -> Unit = {},
    lazyListState: LazyListState = rememberLazyListState(),
    maxHeightPx: Float = 0f
) {
    val shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomEnd = 0.dp,
        bottomStart = 0.dp
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxHeightPx.pxToDp())
            .background(Color.Cyan, shape = shape)
            .clip(shape = shape)
            .border(width = 1f.pxToDp(), color = Color.Yellow, shape = shape)
//            .padding(bottom = 38.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = shape),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    println("aaaaaaa")
                    closeBottomSheet.invoke()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta)
            ) {
                Text(text = "Close")
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        LazyColumn(state = lazyListState, modifier = Modifier.background(Color.Black)) {
            items(50) { index ->
                Text(
                    "I'm item $index", modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color.Gray)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OOBottomSheetViewPreview() {
    val navigationBarHeight = 49.dp
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightPx = with(density) { (screenHeightDp).toPx() }
    val parentHeightPx = with(density) { (screenHeightDp - navigationBarHeight).toPx() }
    val bottomMaxHeight = parentHeightPx * 0.9f
    val lazyListState = rememberLazyListState()

    val _updateY: MutableSharedFlow<Float> = MutableSharedFlow()
    val updateY: SharedFlow<Float> = _updateY

    // case 1
//    val closeOffset = bottomMaxHeight * 0.8f
//    val contentPartOffsetY = null//bottomMaxHeight * 0.4f
//    val contentMaxOffsetY = bottomMaxHeight * 0.8f
//    val contentStartOffsetY = bottomMaxHeight * 0.8f//bottomMaxHeight * 0.4f

    // case 2
    val closeOffset = bottomMaxHeight * 0.8f
    val contentPartOffsetY = bottomMaxHeight * 0.4f
    val contentMaxOffsetY = bottomMaxHeight * 0.8f
    val contentStartOffsetY = bottomMaxHeight * 0.4f

    val scope = rememberCoroutineScope()

    Column {
        Box(
            modifier = Modifier
                .height(navigationBarHeight)
                .fillMaxWidth()
                .background(Color.Gray)
        )
        Box(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = {
                    scope.launch {
                        _updateY.emit(closeOffset)
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta)
            ) {
                Text(text = "Click Me Close")
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    OOBottomSheetView(
                        updateOffsetY = updateY,
                        lazyListState = lazyListState,
                        contentPartOffsetY = contentPartOffsetY,//bottomMaxHeight * 0.4f,// null
                        contentMaxOffsetY = contentMaxOffsetY,//bottomMaxHeight * 0.8f,
                        contentStartOffsetY = contentStartOffsetY,//bottomMaxHeight * 0.8f,//bottomMaxHeight * 0.4f,
                    ) {
                        BottomSheetContent(
                            closeBottomSheet = {
                                scope.launch {
                                    _updateY.emit(closeOffset)
                                }
                            },
                            lazyListState = lazyListState,
                            maxHeightPx = bottomMaxHeight
                        )
                    }
                }
            }
        }
    }
}