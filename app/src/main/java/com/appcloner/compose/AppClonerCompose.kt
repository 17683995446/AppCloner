package com.appcloner.compose

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * AppCloner 9.3 Jetpack Compose 全面迁移框架
 * 
 * 核心特性：
 * 1. Material 3 设计系统集成
 * 2. 动态主题与色彩方案
 * 3. 响应式布局支持
 * 4. 状态管理与数据流
 * 5. 动画与过渡效果
 * 6. 无障碍与可访问性
 */

/**
 * 主题系统
 */
@Composable
fun AppClonerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

/**
 * 色彩方案
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8D5FF),
    onPrimaryContainer = Color(0xFF1F003A),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF91F2E9),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF7B2CBF),
    onTertiary = Color.White,
    background = Color(0xFFFBFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFBFBFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4F4F5),
    inversePrimary = Color(0xFFC7B2FF),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC7B2FF),
    onPrimary = Color(0xFF3B008A),
    primaryContainer = Color(0xFF4F00B5),
    onPrimaryContainer = Color(0xFFE8D5FF),
    secondary = Color(0xFF4FD1C5),
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF00524A),
    onSecondaryContainer = Color(0xFF91F2E9),
    tertiary = Color(0xFFE0BBE4),
    onTertiary = Color(0xFF4A1054),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = Color(0xFF6200EE),
)

/**
 * 排版系统
 */
private val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * 形状系统
 */
private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * 状态管理
 */
@Composable
fun <T> rememberState(initialValue: T): MutableState<T> {
    return remember { mutableStateOf(initialValue) }
}

@Composable
fun <T> rememberMutableStateListOf(vararg elements: T): MutableList<T> {
    return remember { mutableStateListOf(*elements) }
}

@Composable
fun <T> Flow<T>.collectAsStateWithLifecycle(
    initialValue: T
): State<T> {
    return collectAsState(initialValue)
}

/**
 * 自定义组件库
 */

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colors = when (variant) {
        ButtonVariant.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        ButtonVariant.SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
        ButtonVariant.OUTLINED -> ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
        ButtonVariant.TEXT -> ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        colors = colors,
        shape = MaterialTheme.shapes.medium,
        elevation = if (variant == ButtonVariant.PRIMARY || variant == ButtonVariant.SECONDARY) {
            ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        } else null
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    TEXT
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { if (label != null) Text(label) },
        placeholder = { if (placeholder != null) Text(placeholder) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = { if (supportingText != null) Text(supportingText) },
        keyboardType = keyboardType,
        singleLine = singleLine,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
        )
    )
}

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    )
}

@Composable
fun AppProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

/**
 * 列表组件
 */

@Composable
fun <T> AppList(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    LazyColumn(modifier = modifier) {
        items(items) { item ->
            itemContent(item)
        }
    }
}

@Composable
fun <T> AppGrid(
    items: List<T>,
    columns: Int = 2,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        content = {
            items(items) { item ->
                itemContent(item)
            }
        }
    )
}

/**
 * 导航组件
 */

@Composable
fun AppNavigationBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    items: List<NavigationItem>
) {
    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = selectedItem == index,
                onClick = { onItemSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }
    }
}

data class NavigationItem(
    val icon: ImageVector,
    val label: String
)

/**
 * 动画系统
 */

@Composable
fun AnimateVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandVertically(),
    exit: ExitTransition = fadeOut() + shrinkVertically(),
    content: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        content = content
    )
}

@Composable
fun AnimateContentSize(
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<IntSize> = spring(),
    content: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedContentSize(
        modifier = modifier,
        animationSpec = animationSpec,
        content = content
    )
}

@Composable
fun <T> AnimateContent(
    targetState: T,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentScope<T>.() -> ContentTransform = {
        fadeIn(animationSpec = tween(220, delayMillis = 90)) with
                fadeOut(animationSpec = tween(90))
    },
    content: @Composable (T) -> Unit
) {
    androidx.compose.animation.AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = transitionSpec,
        content = content
    )
}

/**
 * 响应式布局
 */

@Composable
fun ResponsiveLayout(
    windowSizeClass: WindowSizeClass,
    compactContent: @Composable () -> Unit,
    mediumContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> compactContent()
        WindowWidthSizeClass.Medium -> mediumContent()
        WindowWidthSizeClass.Expanded -> expandedContent()
        else -> compactContent()
    }
}

/**
 * 状态流收集
 */

@Composable
fun <T> StateFlow<T>.observeAsState(initial: T): State<T> {
    val state = remember { mutableStateOf(initial) }
    DisposableEffect(Unit) {
        val job = CoroutineScope(Dispatchers.Main).launch {
            collect { state.value = it }
        }
        onDispose { job.cancel() }
    }
    return state
}

/**
 * 生命周期感知
 */

@Composable
fun OnLifecycleEvent(event: Lifecycle.Event, action: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == event) {
                action()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * 自定义主题属性
 */

val LocalAppTheme = compositionLocalOf { AppThemeConfig() }

data class AppThemeConfig(
    val isDark: Boolean = false,
    val accentColor: Color = Color(0xFF6200EE),
    val isDynamicColor: Boolean = true
)

@Composable
fun ProvideAppTheme(
    config: AppThemeConfig,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppTheme provides config) {
        content()
    }
}

/**
 * 自定义滚动条
 */

@Composable
fun CustomScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val thumbHeight = max(100.dp, (scrollState.maxValue - scrollState.value).toDp())
    val thumbOffset = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()) * 100

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(8.dp)
            .padding(horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(4.dp)
                .height(thumbHeight)
                .offset(y = thumbOffset.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

/**
 * 阴影效果
 */

fun Modifier.elevation(
    elevation: Dp,
    color: Color = Color.Black.copy(alpha = 0.1f)
): Modifier {
    val elevationPx = elevation.value * density
    return this.shadow(
        elevation = elevation,
        ambientColor = color,
        spotColor = color,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * 渐变背景
 */

@Composable
fun GradientBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(brush = Brush.verticalGradient(colors))
    ) {
        content()
    }
}

/**
 * 扩展函数
 */

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    return clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    ) { onClick() }
}

fun Modifier.placeholder(visible: Boolean, color: Color): Modifier {
    return if (visible) {
        this.background(color)
    } else {
        this
    }
}

/**
 * 图标按钮
 */

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector,
    contentDescription: String
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 对话框组件
 */

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    dismissButtonText: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmButtonText)
            }
        },
        dismissButton = dismissButtonText?.let {
            {
                TextButton(onClick = onDismiss ?: onDismissRequest) {
                    Text(it)
                }
            }
        }
    )
}

/**
 * Snackbar 管理
 */

class SnackbarManager(
    private val scaffoldState: ScaffoldState
) {
    suspend fun showMessage(message: String, actionLabel: String? = null) {
        scaffoldState.snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel
        )
    }
}

@Composable
fun rememberSnackbarManager(scaffoldState: ScaffoldState): SnackbarManager {
    return remember { SnackbarManager(scaffoldState) }
}

/**
 * 加载状态组件
 */

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    loading: Boolean,
    content: @Composable () -> Unit
) {
    if (loading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
    } else {
        content()
    }
}

/**
 * 错误状态组件
 */

@Composable
fun ErrorState(
    error: Throwable?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    error?.let {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error.message ?: "Unknown error",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppButton(text = "Retry", onClick = onRetry)
            }
        }
    }
}

/**
 * 空状态组件
 */

@Composable
fun EmptyState(
    message: String,
    icon: ImageVector = Icons.Default.Inbox,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Empty",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 主应用组件
 */

@Composable
fun AppClonerApp(
    windowSizeClass: WindowSizeClass
) {
    AppClonerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ResponsiveLayout(
                windowSizeClass = windowSizeClass,
                compactContent = { CompactLayout() },
                mediumContent = { MediumLayout() },
                expandedContent = { ExpandedLayout() }
            )
        }
    }
}

@Composable
private fun CompactLayout() {
    // 移动端布局
}

@Composable
private fun MediumLayout() {
    // 平板布局
}

@Composable
private fun ExpandedLayout() {
    // 桌面布局
}

/**
 * 预览组件
 */

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFFBFBFF
)
@Composable
fun LightPreview() {
    AppClonerTheme(darkTheme = false) {
        PreviewContent()
    }
}

@Preview(
    name = "Dark Mode",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F
)
@Composable
fun DarkPreview() {
    AppClonerTheme(darkTheme = true) {
        PreviewContent()
    }
}

@Composable
private fun PreviewContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Hello, Compose!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        AppButton(text = "Click Me", onClick = {})
        Spacer(modifier = Modifier.height(16.dp))
        AppSwitch(checked = true, onCheckedChange = {})
    }
}
