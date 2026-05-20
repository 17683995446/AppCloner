# AppCloner 9.2 - Material Design 3 完整设计系统文档

## 1. 设计原则

### 1.1 核心原则
- **Material You**: 利用Android 12+的动态色彩功能
- **可访问性**: WCAG 2.1 AA级对比度
- **响应式**: 适配手机、平板、折叠屏
- **一致性**: 统一的视觉语言和交互模式

### 1.2 色彩系统

#### 主色调调色板
```kotlin
Primary: #006C51 (主品牌色)
On Primary: #FFFFFF
Primary Container: #89F8C7
On Primary Container: #002114

Secondary: #4B635A
On Secondary: #FFFFFF
Secondary Container: #CDE9DD
On Secondary Container: #072018

Tertiary: #3A656E
On Tertiary: #FFFFFF
Tertiary Container: #BEEAF5
On Tertiary Container: #001F25

Error: #BA1A1A
Error Container: #FFDAD6
On Error: #FFFFFF
On Error Container: #410002
```

#### 语义色彩
```kotlin
Success: #2E7D32
Warning: #F57C00
Info: #1976D2
Surface: #FBFDF9
Surface Variant: #DBE5DE
Outline: #707973
```

### 1.3 排版系统

#### 字体层级
```kotlin
Display Large: 57sp / 64sp line height
Display Medium: 45sp / 52sp line height
Display Small: 36sp / 44sp line height

Headline Large: 32sp / 40sp line height
Headline Medium: 28sp / 36sp line height
Headline Small: 24sp / 32sp line height

Title Large: 22sp / 28sp line height
Title Medium: 16sp / 24sp line height / Medium 500
Title Small: 14sp / 20sp line height / Medium 500

Body Large: 16sp / 24sp line height
Body Medium: 14sp / 20sp line height
Body Small: 12sp / 16sp line height

Label Large: 14sp / 20sp line height / Medium 500
Label Medium: 12sp / 16sp line height / Medium 500
Label Small: 11sp / 16sp line height / Medium 500
```

### 1.4 间距系统

#### 基础间距
```kotlin
xs: 4dp
sm: 8dp
md: 16dp
lg: 24dp
xl: 32dp
xxl: 48dp
```

#### 组件间距
```kotlin
Card Padding: 16dp
List Item Height: 56dp (三行) / 72dp (四行)
Button Height: 40dp
FAB Height: 56dp
Top App Bar Height: 64dp
Bottom Navigation Height: 80dp
Navigation Rail Width: 80dp
```

### 1.5 圆角系统
```kotlin
Extra Small: 4dp
Small: 8dp
Medium: 12dp
Large: 16dp
Extra Large: 28dp
Full: 50% (圆形)
```

## 2. 组件库

### 2.1 导航组件

#### Top App Bar
```kotlin
AppClonerTopAppBar(
    title = "AppCloner",
    navigationIcon = Icons.Default.Menu,
    onNavigationClick = { },
    actions = {
        IconButton(Icons.Default.Search) { }
        IconButton(Icons.Default.MoreVert) { }
    },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorLevel(1)
    )
)
```

#### Bottom Navigation
```kotlin
AppClonerBottomNavigation(
    items = listOf(
        BottomNavItem("home", "首页", Icons.Default.Home, Icons.Filled.Home),
        BottomNavItem("apps", "应用", Icons.Default.Apps, Icons.Filled.Apps),
        BottomNavItem("templates", "模板", Icons.Default.Dashboard, Icons.Filled.Dashboard),
        BottomNavItem("settings", "设置", Icons.Default.Settings, Icons.Filled.Settings)
    ),
    selectedItem = "home",
    onItemSelected = { }
)
```

#### Navigation Rail
```kotlin
NavigationRail(
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.width(80.dp)
) {
    items.forEach { item ->
        NavigationRailItem(
            selected = selectedItem == item.id,
            onClick = { onItemSelected(item.id) },
            icon = { Icon(item.icon, contentDescription = item.label) },
            label = { Text(item.label) }
        )
    }
}
```

### 2.2 卡片组件

#### 克隆应用卡片
```kotlin
AppCloneCard(
    onClick = { /* 打开克隆配置 */ },
    modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = app.icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${app.cloneCount} 个分身",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "查看",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

#### AI推荐卡片
```kotlin
AIRecommendationCard(
    recommendation = recommendation,
    onAccept = { },
    onDismiss = { },
    modifier = Modifier.fillMaxWidth()
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI 推荐",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "建议克隆 ${recommendation.appName}",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = recommendation.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("忽略")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onAccept) {
                Text("克隆")
            }
        }
    }
}
```

### 2.3 表单组件

#### 搜索栏
```kotlin
SearchBar(
    inputField = {
        SearchBarDefaults.InputField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = onSearch,
            expanded = isSearchActive,
            onExpandedChange = { setSearchActive(it) },
            placeholder = { Text("搜索应用...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "搜索")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除")
                    }
                }
            }
        )
    },
    expanded = isSearchActive,
    onExpandedChange = { setSearchActive(it) },
    modifier = Modifier.fillMaxWidth()
) {
    // 搜索建议列表
}
```

#### 设置开关
```kotlin
SettingSwitch(
    title = "自动克隆建议",
    subtitle = "根据使用习惯自动推荐要克隆的应用",
    checked = autoCloneEnabled,
    onCheckedChange = { setAutoCloneEnabled(it) },
    leadingIcon = Icons.Default.AutoAwesome
)
```

#### 滑块输入
```kotlin
SettingSlider(
    title = "克隆数量限制",
    value = cloneLimit,
    onValueChange = { setCloneLimit(it) },
    valueRange = 1f..100f,
    steps = 9,
    valueLabel = "${cloneLimit.toInt()} 个"
)
```

### 2.4 反馈组件

#### 加载指示器
```kotlin
// 线性进度
LinearProgressIndicator(
    progress = { progress },
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.primary,
    trackColor = MaterialTheme.colorScheme.surfaceVariant
)

// 圆形进度
CircularProgressIndicator(
    progress = { progress },
    modifier = Modifier.size(48.dp),
    color = MaterialTheme.colorScheme.primary
)

// 骨架屏
ShimmerEffect(
    modifier = Modifier
        .fillMaxWidth()
        .height(80.dp)
)
```

#### Snackbar
```kotlin
Snackbar(
    modifier = Modifier.padding(16.dp),
    action = {
        TextButton(onClick = { /* 重试 */ }) {
            Text("重试")
        }
    },
    dismissAction = {
        IconButton(onClick = { }) {
            Icon(Icons.Default.Close, contentDescription = "关闭")
        }
    }
) {
    Text("同步失败，请检查网络连接")
}
```

### 2.5 高级组件

#### 同步状态指示器
```kotlin
SyncStatusIndicator(
    state = syncState,
    progress = syncProgress,
    onRetry = { },
    modifier = Modifier.fillMaxWidth()
) {
    when (syncState) {
        is SyncState.Syncing -> {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = { syncProgress?.percentComplete ?: 0f }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "正在同步... ${(syncProgress?.percentComplete ?: 0f * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = syncProgress?.currentFile ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        is SyncState.Success -> {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "同步完成",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        is SyncState.Error -> {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "同步失败: ${syncState.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onRetry) {
                    Text("重试")
                }
            }
        }
    }
}
```

#### 插件卡片
```kotlin
PluginCard(
    plugin = plugin,
    isInstalled = isInstalled,
    onInstall = { },
    onUninstall = { },
    onConfigure = { },
    modifier = Modifier.fillMaxWidth()
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = plugin.iconUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "v${plugin.version} • ${plugin.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            RatingBar(
                rating = plugin.rating,
                onRatingChange = { },
                modifier = Modifier.size(80.dp, 16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = plugin.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 标签
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            plugin.tags.forEach { tag ->
                AssistChip(
                    onClick = { },
                    label = { Text(tag) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (isInstalled) {
                OutlinedButton(onClick = onUninstall) {
                    Text("卸载")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onConfigure) {
                    Text("配置")
                }
            } else {
                Button(onClick = onInstall) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("安装")
                }
            }
        }
    }
}
```

## 3. 布局模式

### 3.1 响应式布局

#### 自适应布局
```kotlin
@Composable
fun AdaptiveLayout() {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    
    when (windowSize.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            // 手机布局
            CompactLayout()
        }
        WindowWidthSizeClass.MEDIUM -> {
            // 平板竖屏布局
            MediumLayout()
        }
        WindowWidthSizeClass.EXPANDED -> {
            // 平板横屏/桌面布局
            ExpandedLayout()
        }
    }
}

@Composable
private fun ExpandedLayout() {
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            // 导航项
        }
        
        Column(modifier = Modifier.weight(1f)) {
            AppClonerTopAppBar(title = "AppCloner")
            
            // 主内容区
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // 列表
                LazyColumn(
                    modifier = Modifier.weight(0.4f)
                ) {
                    items(apps) { app ->
                        AppCloneCard(app = app)
                    }
                }
                
                VerticalDivider()
                
                // 详情
                Box(
                    modifier = Modifier.weight(0.6f)
                ) {
                    AppDetailPanel(app = selectedApp)
                }
            }
        }
    }
}
```

### 3.2 列表布局

#### 分组列表
```kotlin
@Composable
fun GroupedAppList(
    apps: List<AppGroup>,
    onAppClick: (App) -> Unit,
    onAppLongClick: (App) -> Unit
) {
    apps.forEach { group ->
        // 分组标题
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // 分组内容
        LazyColumn {
            items(group.apps) { app ->
                AppListItem(
                    app = app,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }
        }
        
        // 分组间隔
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AppListItem(
    app: App,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(app.name) },
        supportingContent = { Text("${app.cloneCount} 个分身") },
        leadingContent = {
            Icon(
                imageVector = app.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

### 3.3 详情页布局

```kotlin
@Composable
fun AppDetailPage(
    app: App,
    clones: List<Clone>,
    onCreateClone: () -> Unit,
    onEditClone: (Clone) -> Unit,
    onDeleteClone: (Clone) -> Unit
) {
    Scaffold(
        topBar = {
            AppClonerTopAppBar(
                title = app.name,
                navigationIcon = Icons.Default.ArrowBack,
                onNavigationClick = { },
                actions = {
                    IconButton(Icons.Default.MoreVert) { }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClone,
                icon = { Icon(Icons.Default.Add) },
                text = { Text("创建分身") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 应用信息
            AppInfoSection(app = app)
            
            HorizontalDivider()
            
            // 分身列表
            Text(
                text = "所有分身 (${clones.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            LazyColumn {
                items(clones) { clone ->
                    CloneListItem(
                        clone = clone,
                        onEdit = { onEditClone(clone) },
                        onDelete = { onDeleteClone(clone) }
                    )
                }
            }
        }
    }
}
```

## 4. 主题系统

### 4.1 主题配置

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006C51),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF89F8C7),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4B635A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE9DD),
    onSecondaryContainer = Color(0xFF072018),
    tertiary = Color(0xFF3A656E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBEEAF5),
    onTertiaryContainer = Color(0xFF001F25),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFDF9),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFBFDF9),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDBE5DE),
    onSurfaceVariant = Color(0xFF404944),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFBFC9C2),
    inverseSurface = Color(0xFF2E312F),
    inverseOnSurface = Color(0xFFF0F1EC),
    inversePrimary = Color(0xFF7DD9BB),
    surfaceTint = Color(0xFF006C51)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7DD9BB),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF00513C),
    onPrimaryContainer = Color(0xFF89F8C7),
    secondary = Color(0xFFB1CCC1),
    onSecondary = Color(0xFF1D352D),
    secondaryContainer = Color(0xFF344C43),
    onSecondaryContainer = Color(0xFFCDE9DD),
    tertiary = Color(0xFFA2CED9),
    onTertiary = Color(0xFF023640),
    tertiaryContainer = Color(0xFF214D57),
    onTertiaryContainer = Color(0xFFBEEAF5),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191C1A),
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF191C1A),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF404944),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF89938D),
    outlineVariant = Color(0xFF404944),
    inverseSurface = Color(0xFFE1E3DE),
    inverseOnSurface = Color(0xFF2E312F),
    inversePrimary = Color(0xFF006C51),
    surfaceTint = Color(0xFF7DD9BB)
)
```

### 4.2 动态主题

```kotlin
@Composable
fun DynamicTheme(
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

## 5. 动画系统

### 5.1 过渡动画

```kotlin
// 页面过渡
@Composable
fun AppClonerNavHost() {
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { 300 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -300 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -300 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 300 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable("home") { HomeScreen() }
        composable("app/{appId}") { AppDetailScreen() }
    }
}
```

### 5.2 状态动画

```kotlin
@Composable
fun AnimatedSyncButton(
    isSyncing: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    IconButton(
        onClick = onClick,
        enabled = !isSyncing
    ) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "同步",
            modifier = Modifier.rotate(if (isSyncing) rotation else 0f)
        )
    }
}
```

## 6. 无障碍支持

### 6.1 内容描述

```kotlin
Icon(
    imageVector = Icons.Default.Settings,
    contentDescription = "设置", // 必要的无障碍描述
    modifier = Modifier.semantics {
        contentDescription = "打开设置菜单"
    }
)

Button(
    onClick = { },
    modifier = Modifier.semantics {
        customActions = listOf(
            CustomAction(
                name = "帮助",
                action = { /* 显示帮助 */ true }
            )
        )
    }
) {
    Text("设置")
}
```

### 6.2 颜色对比度

```kotlin
Text(
    text = "主要文本",
    color = MaterialTheme.colorScheme.onSurface, // 最小对比度 4.5:1
    modifier = Modifier.background(
        MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp)
    )
)

Text(
    text = "次要文本",
    color = MaterialTheme.colorScheme.onSurfaceVariant, // 最小对比度 3:1
    modifier = Modifier.background(
        MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp)
    )
)
```

## 7. 最佳实践

### 7.1 性能优化

```kotlin
@Composable
fun OptimizedAppList(apps: List<App>) {
    LazyColumn {
        items(
            items = apps,
            key = { it.id } // 使用稳定的key
        ) { app ->
            AppListItem(
                app = app,
                modifier = Modifier.animateItem() // 动画优化
            )
        }
    }
}

@Composable
private fun AppListItem(
    app: App,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    
    ListItem(
        modifier = modifier
            .clickable { expanded = true }
            .padding(horizontal = 16.dp),
        // ...
    )
}
```

### 7.2 错误处理

```kotlin
@Composable
fun ErrorState(
    error: Error,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "出错了",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("重试")
        }
    }
}
```

## 8. 实现清单

### UI/UX MD3升级完成清单

- [x] 完整设计系统文档
- [x] 色彩系统定义
- [x] 排版系统定义
- [x] 间距系统定义
- [x] 导航组件（TopAppBar, BottomNav, NavRail）
- [x] 卡片组件（AppCard, AIRecommendationCard）
- [x] 表单组件（SearchBar, Switch, Slider）
- [x] 反馈组件（Progress, Snackbar, Skeleton）
- [x] 高级组件（SyncStatus, PluginCard）
- [x] 响应式布局支持
- [x] 主题系统（Light/Dark/Dynamic）
- [x] 动画系统
- [x] 无障碍支持
- [x] 最佳实践指南

**状态**: ✅ 完成
