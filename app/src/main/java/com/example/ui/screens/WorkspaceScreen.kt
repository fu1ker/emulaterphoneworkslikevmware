package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppEntity
import com.example.data.ComponentEntity
import com.example.ui.AppViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    app: AppEntity,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val components by viewModel.getComponentsForApp(app.id).collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Design Mode, 1 = Run Mode

    // Dialog state
    var showAddComponentDialog by remember { mutableStateOf(false) }
    var showEditAppDialog by remember { mutableStateOf(false) }
    var componentToEdit by remember { mutableStateOf<ComponentEntity?>(null) }
    var showLinkNestedAppDialog by remember { mutableStateOf<ComponentEntity?>(null) }

    val appColor = Color(app.primaryColor)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(appColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = app.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            text = "Project ID: ${app.id.take(8)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("workspace_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditAppDialog = true }, modifier = Modifier.testTag("edit_app_properties_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "App Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Design Mode", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("design_mode_tab")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run Mode", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("run_mode_tab")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> (width * 0.1f).toInt() } + fadeIn(animationSpec = tween(220))).togetherWith(
                            slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> -(width * 0.1f).toInt() } + fadeOut(animationSpec = tween(180))
                        )
                    } else {
                        (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> -(width * 0.1f).toInt() } + fadeIn(animationSpec = tween(220))).togetherWith(
                            slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> (width * 0.1f).toInt() } + fadeOut(animationSpec = tween(180))
                        )
                    }
                },
                label = "WorkspaceModeTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> DesignModeLayout(
                        components = components,
                        appColor = appColor,
                        onAddComponentClick = { showAddComponentDialog = true },
                        onEditComponentClick = { componentToEdit = it },
                        onDeleteComponentClick = { viewModel.deleteComponent(app.id, it.id) },
                        onMoveUp = { viewModel.moveComponentUp(app.id, it.id) },
                        onMoveDown = { viewModel.moveComponentDown(app.id, it.id) },
                        onLinkSubApp = { showLinkNestedAppDialog = it },
                        onDesignSubApp = { subAppId ->
                            coroutineScope.launch {
                                val subApp = viewModel.getSubAppFlow(subAppId).firstOrNull()
                                if (subApp != null) {
                                    viewModel.pushApp(subApp)
                                } else {
                                    snackbarHostState.showSnackbar("Unable to open sub-app")
                                }
                            }
                        }
                    )
                    1 -> RunModeSimulator(
                        app = app,
                        components = components,
                        viewModel = viewModel,
                        onShowNotification = { message ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                }
            }
        }
    }

    // Add Component Dialog
    if (showAddComponentDialog) {
        AddComponentDialog(
            onDismiss = { showAddComponentDialog = false },
            onConfirm = { type, label, value ->
                viewModel.addComponent(app.id, type, label, value)
                showAddComponentDialog = false
            }
        )
    }

    // Edit Component Dialog
    if (componentToEdit != null) {
        EditComponentDialog(
            component = componentToEdit!!,
            onDismiss = { componentToEdit = null },
            onConfirm = { updatedComp ->
                viewModel.updateComponent(updatedComp)
                componentToEdit = null
            }
        )
    }

    // Edit App Properties Dialog
    if (showEditAppDialog) {
        EditAppDialog(
            app = app,
            colorPalette = viewModel.colorPalette,
            onDismiss = { showEditAppDialog = false },
            onConfirm = { newName, newColor ->
                viewModel.updateAppDetails(app.id, newName, newColor)
                showEditAppDialog = false
            }
        )
    }

    // Create and Link Nested App Dialog
    if (showLinkNestedAppDialog != null) {
        CreateNestedAppDialog(
            parentApp = app,
            colorPalette = viewModel.colorPalette,
            onDismiss = { showLinkNestedAppDialog = null },
            onCreate = { name, color ->
                viewModel.createAndLinkNestedApp(
                    parentAppId = app.id,
                    componentId = showLinkNestedAppDialog!!.id,
                    subAppName = name,
                    subAppColor = color
                )
                showLinkNestedAppDialog = null
            }
        )
    }
}

@Composable
fun DesignModeLayout(
    components: List<ComponentEntity>,
    appColor: Color,
    onAddComponentClick: () -> Unit,
    onEditComponentClick: (ComponentEntity) -> Unit,
    onDeleteComponentClick: (ComponentEntity) -> Unit,
    onMoveUp: (ComponentEntity) -> Unit,
    onMoveDown: (ComponentEntity) -> Unit,
    onLinkSubApp: (ComponentEntity) -> Unit,
    onDesignSubApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Components Hierarchy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = onAddComponentClick,
                colors = ButtonDefaults.buttonColors(containerColor = appColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_component_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Component", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (components.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = appColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your App Canvas is Empty",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Click 'Add Component' to append visual controls, text displays, dividers, or recursive nested app cells.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(components, key = { it.id }) { component ->
                    DesignComponentCard(
                        component = component,
                        appColor = appColor,
                        onEdit = { onEditComponentClick(component) },
                        onDelete = { onDeleteComponentClick(component) },
                        onMoveUp = { onMoveUp(component) },
                        onMoveDown = { onMoveDown(component) },
                        onLinkSubApp = { onLinkSubApp(component) },
                        onDesignSubApp = onDesignSubApp,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
fun DesignComponentCard(
    component: ComponentEntity,
    appColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onLinkSubApp: () -> Unit,
    onDesignSubApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Get visual characteristics based on component type
    val info = when (component.type) {
        "TEXT" -> Triple(Icons.Default.TextFields, "Text Display", "Shows customized label (font: ${component.value}sp)")
        "BUTTON" -> Triple(Icons.Default.SmartButton, "Action Button", "Runs interactive popup with alert: \"${component.value}\"")
        "COUNTER" -> Triple(Icons.Default.AddCircleOutline, "Interactive Counter", "Displays a live incrementer widget")
        "INPUT" -> Triple(Icons.Default.Input, "Text Input", "Simulates field with placeholder: \"${component.value}\"")
        "DIVIDER" -> Triple(Icons.Default.LinearScale, "Horizontal Divider", "Draws a styling separator line")
        "SPACER" -> Triple(Icons.Default.SpaceBar, "Vertical Spacer", "Adds whitespace of height ${component.value}dp")
        "NESTED_APP" -> Triple(Icons.Default.Layers, "Nested App Cell", "The Inception gate: houses a sub-app builder inside")
        else -> Triple(Icons.Default.Extension, "Component", "")
    }
    val icon = info.first
    val typeLabel = info.second
    val desc = info.third

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("design_component_${component.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = appColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = appColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (component.label.isNotEmpty()) {
                        Text(
                            text = component.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Control actions: Up, Down, Delete, Edit
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp).testTag("edit_component_btn_${component.id}")) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp).testTag("delete_component_btn_${component.id}")) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            // Special Inception UI for NESTED_APP type
            if (component.type == "NESTED_APP") {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        .border(
                            BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ), RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    if (component.value.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No Nesting App is Linked to This Cell Yet",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onLinkSubApp,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("create_nested_app_btn_${component.id}")
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Build Sub-App Here", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        val subAppId = component.value
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sub-App Linked: ID ${subAppId.take(6)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Button(
                                onClick = { onDesignSubApp(subAppId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("design_sub_app_btn_${component.id}")
                            ) {
                                Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Sub-App", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simulated dynamic Run Mode preview
@Composable
fun RunModeSimulator(
    app: AppEntity,
    components: List<ComponentEntity>,
    viewModel: AppViewModel,
    onShowNotification: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local interactive states for simulating live usage
    val counterStates = remember { mutableStateMapOf<String, Int>() }
    val textInputStates = remember { mutableStateMapOf<String, String>() }
    val primaryColor = remember(app.primaryColor) { Color(app.primaryColor) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Active App Simulator",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Visual Simulator Phone Frame
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(4.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Phone Status Bar Simulation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("9:41 AM", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }

                // App's Styled Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(app.primaryColor))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = app.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // App's Simulated Canvas
                if (components.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No content added. Add components in Design Mode to display them in this live simulator.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(components, key = { it.id }) { component ->
                            Box(modifier = Modifier.animateItem()) {
                                SimulatedComponentRow(
                                    component = component,
                                    primaryColor = primaryColor,
                                    counterStates = counterStates,
                                    textInputStates = textInputStates,
                                    viewModel = viewModel,
                                    onShowNotification = onShowNotification
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCounterText(
    count: Int,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { height -> height } + fadeIn()).togetherWith(
                    slideOutVertically { height -> -height } + fadeOut()
                )
            } else {
                (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                    slideOutVertically { height -> height } + fadeOut()
                )
            }
        },
        label = "CounterAnimation",
        modifier = modifier
    ) { targetCount ->
        Text(
            text = targetCount.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.widthIn(min = 24.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SimulatedComponentRow(
    component: ComponentEntity,
    primaryColor: Color,
    counterStates: MutableMap<String, Int>,
    textInputStates: MutableMap<String, String>,
    viewModel: AppViewModel,
    onShowNotification: (String) -> Unit
) {
    when (component.type) {
        "TEXT" -> {
            val fontSize = component.value.toIntOrNull() ?: 16
            Text(
                text = component.label,
                fontSize = fontSize.sp,
                fontWeight = if (fontSize > 18) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().testTag("sim_text_${component.id}")
            )
        }
        "BUTTON" -> {
            Button(
                onClick = { onShowNotification(component.value) },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("sim_button_${component.id}")
            ) {
                Text(component.label, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        "COUNTER" -> {
            val count = counterStates[component.id] ?: 0
            Card(
                modifier = Modifier.fillMaxWidth().testTag("sim_counter_${component.id}"),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        component.label,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { counterStates[component.id] = count - 1 },
                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrement", modifier = Modifier.size(16.dp))
                        }
                        AnimatedCounterText(count = count)
                        IconButton(
                            onClick = { counterStates[component.id] = count + 1 },
                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increment", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        "INPUT" -> {
            val text = textInputStates[component.id] ?: ""
            OutlinedTextField(
                value = text,
                onValueChange = { textInputStates[component.id] = it },
                placeholder = { Text(component.value) },
                label = { Text(component.label) },
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("sim_input_${component.id}")
            )
        }
        "DIVIDER" -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        }
        "SPACER" -> {
            val height = component.value.toIntOrNull() ?: 8
            Spacer(modifier = Modifier.height(height.dp))
        }
        "NESTED_APP" -> {
            // RENDER NESTED APP INLINE RECURSIVELY!
            val subAppId = component.value
            if (subAppId.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.errorContainer), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Inception Simulator: No sub-app is configured. Switch to Design Mode to create your nested app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                NestedAppInlineSimulator(
                    appId = subAppId,
                    viewModel = viewModel,
                    onShowNotification = onShowNotification
                )
            }
        }
    }
}

// Recursive Nested App Simulator
@Composable
fun NestedAppInlineSimulator(
    appId: String,
    viewModel: AppViewModel,
    onShowNotification: (String) -> Unit
) {
    val subApp by viewModel.getSubAppFlow(appId).collectAsStateWithLifecycle(initialValue = null)

    if (subApp == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
        return
    }

    val app = subApp!!
    val subComponents by viewModel.getComponentsForApp(app.id).collectAsStateWithLifecycle()
    val subCounterStates = remember { mutableStateMapOf<String, Int>() }
    val subInputStates = remember { mutableStateMapOf<String, String>() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .testTag("inline_simulator_${app.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, Color(app.primaryColor))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Nested App Top Header (Inline Phone Simulation Frame)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(app.primaryColor))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NESTED: ${app.name}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Text("Sub-App Frame", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
            }

            // Canvas of nested sub-app
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (subComponents.isEmpty()) {
                    Text(
                        "Nest is empty. Tap 'Edit Sub-App' in Design Mode to add widgets to this nested layer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                } else {
                    subComponents.forEach { component ->
                        SimulatedComponentRow(
                            component = component,
                            primaryColor = Color(app.primaryColor),
                            counterStates = subCounterStates,
                            textInputStates = subInputStates,
                            viewModel = viewModel,
                            onShowNotification = onShowNotification
                        )
                    }
                }
            }
        }
    }
}

// Dialogs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddComponentDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, label: String, value: String) -> Unit
) {
    var selectedType by remember { mutableStateOf("TEXT") }
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    val componentTypes = listOf(
        "TEXT" to "Text Label",
        "BUTTON" to "Action Button",
        "COUNTER" to "Interactive Counter",
        "INPUT" to "Text Input Field",
        "DIVIDER" to "Horizontal Divider",
        "SPACER" to "Vertical Spacer",
        "NESTED_APP" to "Nesting App Container"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add UI Component", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Component Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = componentTypes.find { it.first == selectedType }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("component_type_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        componentTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.second) },
                                onClick = {
                                    selectedType = type.first
                                    expanded = false
                                    // Set reasonable default label and values based on type
                                    when (type.first) {
                                        "TEXT" -> {
                                            label = "Hello World!"
                                            value = "16"
                                        }
                                        "BUTTON" -> {
                                            label = "Click Me"
                                            value = "Button Pressed!"
                                        }
                                        "COUNTER" -> {
                                            label = "Visitor Count"
                                            value = ""
                                        }
                                        "INPUT" -> {
                                            label = "Your Name"
                                            value = "Enter name..."
                                        }
                                        "DIVIDER" -> {
                                            label = ""
                                            value = ""
                                        }
                                        "SPACER" -> {
                                            label = ""
                                            value = "16"
                                        }
                                        "NESTED_APP" -> {
                                            label = "My Nested App"
                                            value = ""
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("dropdown_item_${type.first}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedType != "DIVIDER" && selectedType != "SPACER") {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(if (selectedType == "NESTED_APP") "Container Label" else "Label / Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("component_label_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (selectedType == "TEXT") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Font Size (sp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("component_font_size_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (selectedType == "BUTTON") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Snackbar Message Alert") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("component_toast_msg_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (selectedType == "INPUT") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Input Placeholder") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("component_placeholder_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (selectedType == "SPACER") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Spacer Height (dp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("component_spacer_height_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedType, label, value) },
                modifier = Modifier.testTag("confirm_add_component")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun EditComponentDialog(
    component: ComponentEntity,
    onDismiss: () -> Unit,
    onConfirm: (ComponentEntity) -> Unit
) {
    var label by remember { mutableStateOf(component.label) }
    var value by remember { mutableStateOf(component.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Component", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (component.type != "DIVIDER" && component.type != "SPACER") {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label / Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_component_label"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (component.type == "TEXT") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Font Size (sp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("edit_component_font_size"),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (component.type == "BUTTON") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Snackbar Message Alert") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_component_toast_msg"),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (component.type == "INPUT") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Input Placeholder") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_component_placeholder"),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (component.type == "SPACER") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Spacer Height (dp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("edit_component_spacer_height"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(component.copy(label = label, value = value)) },
                modifier = Modifier.testTag("confirm_edit_component")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun EditAppDialog(
    app: AppEntity,
    colorPalette: List<Long>,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newColor: Long) -> Unit
) {
    var name by remember { mutableStateOf(app.name) }
    var selectedColor by remember { mutableStateOf(app.primaryColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Canvas Properties", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_app_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Accent Color Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorPalette.forEach { colorHex ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), selectedColor) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_edit_app_btn")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun CreateNestedAppDialog(
    parentApp: AppEntity,
    colorPalette: List<Long>,
    onDismiss: () -> Unit,
    onCreate: (name: String, color: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(colorPalette.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Build Nested Sub-App", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "You are nesting a new app inside: ${parentApp.name}. This child canvas will have its own components, design mode, and inline simulations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Sub-App Name") },
                    placeholder = { Text("e.g., Simple Calculator") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("nested_app_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Sub-App Color Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorPalette.forEach { colorHex ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, selectedColor) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_nested_app_btn")
            ) {
                Text("Link & Build")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
