package com.combat.nomm

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import nuclearoptionmodmanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun ModDetailScreen(
    modId: String,
    onOpenMod: (String) -> Unit,
    onBack: () -> Unit,
) {
    val mod = RepoMods.mods.collectAsState().value.find { it.id == modId }
        ?: SettingsManager.config.value.cachedManifest.find { it.id == modId } ?: run { onBack(); return }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val backStack = rememberNavBackStack(ModNavigation.config, ModNavigation.Details)
        val currentKey = backStack.lastOrNull() ?: ModNavigation.Details

        ModTitleCard(
            mod = mod, onBack = onBack
        )

        ModNavigationBar(currentKey, backStack)

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                entryProvider = entryProvider {
                    entry<ModNavigation.Details> {
                        ModDetailsContent(mod)
                    }
                    entry<ModNavigation.Versions> {
                        ModVersionsContent(mod) { version ->
                            backStack.clear()
                            backStack.add(ModNavigation.Dependencies(version))
                        }
                    }
                    entry<ModNavigation.Dependencies> { args ->
                        ModVersionDependenciesContent(mod, args.version, onOpenMod)
                    }
                })
        }
    }
}

@Composable
fun ModTitleCard(
    mod: Extension,
    onBack: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = mod.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                if (mod.authors.isNotEmpty()) {
                                    append("by ")
                                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                        append(mod.authors.joinToString(", "))
                                    }
                                }
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        VerticalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(Res.drawable.download_24px), null, Modifier.size(24.dp))
                            Text(mod.downloadCount.toString(), style = MaterialTheme.typography.bodyMedium)
                        }

                        VerticalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        mod.tags.forEach { tag ->
                            DisableSelection {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onSurfaceVariant) {
                                    Text(
                                        text = tag,
                                        modifier = Modifier.padding(horizontal = 8.dp, 2.dp).clip(CircleShape),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val installStatuses by Installer.installStatuses.collectAsState()
            val installedMods by LocalMods.mods.collectAsState()
            val taskState = installStatuses[mod.id]
            val modMeta = installedMods[mod.id]
            ModActions(taskState, modMeta, mod)

            IconButton(
                onClick = onBack, colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    painter = painterResource(Res.drawable.close_24px),
                    contentDescription = "Close",
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ModActions(
    taskState: TaskState?,
    modMeta: ModMeta?,
    mod: Extension,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val controlSize = 40.dp
        val iconSize = 24.dp
        when {
            taskState != null -> {
                val animatedProgress by animateFloatAsState(
                    targetValue = taskState.progress ?: 1f,
                    label = "downloadProgress"
                )

                Box(
                    modifier = Modifier.size(controlSize),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(controlSize*1.2f),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    IconButton(
                        onClick = {
                            if (taskState.phase == TaskState.Phase.DOWNLOADING) taskState.cancel()
                        },
                        modifier = Modifier.size(controlSize)
                    ) {
                        Icon(
                            painter = if (taskState.phase == TaskState.Phase.DOWNLOADING)
                                painterResource(Res.drawable.close_24px)
                            else
                                painterResource(Res.drawable.unarchive_24px),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize/2*3),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            modMeta != null -> {
                if (modMeta.problems.isNotEmpty()) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        state = rememberTooltipState(),
                        tooltip = {
                            PlainTooltip(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                maxWidth = Dp.Unspecified,
                            ) {
                                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                                    modMeta.problems.forEachIndexed { i, problem ->
                                        Text(
                                            text = problem,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Visible,
                                        )
                                        if (i != modMeta.problems.size - 1) {
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 2.dp).fillMaxWidth(),
                                                color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = { modMeta.resolveProblems() },
                            modifier = Modifier.size(controlSize)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (modMeta.problems.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text(modMeta.problems.size.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painterResource(Res.drawable.warning_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(iconSize),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                if (modMeta.hasUpdate) {
                    IconButton(
                        onClick = { modMeta.update() },
                        modifier = Modifier.size(controlSize)
                    ) {
                        Icon(
                            painterResource(Res.drawable.refresh_24px),
                            contentDescription = "Update",
                            modifier = Modifier.size(iconSize),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = { modMeta.uninstall() },
                    modifier = Modifier.size(controlSize)
                ) {
                    Icon(
                        painterResource(Res.drawable.delete_24px),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Switch(
                    checked = modMeta.enabled ?: false,
                    onCheckedChange = { isEnabled ->
                        if (isEnabled) modMeta.enable() else modMeta.disable()
                    },
                    modifier = Modifier
                        .height(32.dp)
                )

            }

            else -> {
                IconButton(
                    onClick = {
                        mod.artifacts.maxByOrNull { it.version }?.let { latest ->
                            if (mod.real) RepoMods.installMod(mod.id, latest.version)
                        }
                    },
                    modifier = Modifier.size(controlSize)
                ) {
                    Icon(
                        painterResource(Res.drawable.download_24px),
                        contentDescription = "Install",
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ModNavigationBar(
    currentKey: NavKey,
    backStack: NavBackStack<NavKey>,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = CircleShape,
        modifier = Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        ) {
            NavItem(
                selected = currentKey is ModNavigation.Details,
                label = "Details",
                icon = Res.drawable.info_24px,
            ) {
                backStack.clear()
                backStack.add(ModNavigation.Details)
            }

            NavItem(
                selected = currentKey is ModNavigation.Versions,
                label = "Versions",
                icon = Res.drawable.list_24px,
            ) {
                backStack.clear()
                backStack.add(ModNavigation.Versions)
            }
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    label: String,
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
    else Color.Transparent

    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier.fillMaxHeight().clip(CircleShape).background(backgroundColor).clickable(onClick = onClick)
            .padding(8.dp), contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
fun ModDetailsContent(mod: Extension) {
    SelectionContainer {
        val state = rememberScrollState()

        val isScrollable by remember {
            derivedStateOf { state.maxValue > 0 }
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {


            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(state),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                Spacer(Modifier.height(0.dp))
                if (mod.infoUrl.isNotEmpty()) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Info: ") }
                            withLink(LinkAnnotation.Url(mod.infoUrl)) {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append(mod.infoUrl)
                                }
                            }
                        }, style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                }
                Text(
                    text = mod.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(0.dp))
            }
            if (isScrollable) {
                VerticalScrollbar(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(8.dp)
                        .padding(vertical = 16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    adapter = rememberScrollbarAdapter(state),
                    style = defaultScrollbarStyle().copy(
                        unhoverColor = MaterialTheme.colorScheme.outline,
                        hoverColor = MaterialTheme.colorScheme.primary,
                        thickness = 8.dp,
                        shape = CircleShape
                    )
                )
            }
        }
    }
}

@Composable
fun ModVersionsContent(
    mod: Extension,
    onOpenDependencies: (Version) -> Unit,
) {
    val sortedArtifacts = remember(mod.artifacts) {
        mod.artifacts.sortedByDescending { it.version }
    }

    val mods by LocalMods.mods.collectAsState()
    val modMeta = mods[mod.id]

    val state = rememberLazyListState()

    val isScrollable by remember {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo.size < state.layoutInfo.totalItemsCount ||
                state.firstVisibleItemScrollOffset > 0
        }
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = state,
        ) {
            items(sortedArtifacts) { artifact ->
                val isInstalled = modMeta?.artifact?.version == artifact.version
                ArtifactCard(
                    artifact = artifact,
                    isInstalled = isInstalled,
                    onInstall = { if (mod.real) RepoMods.installMod(mod.id, artifact.version) },
                    onViewDependencies = { onOpenDependencies(artifact.version) })
            }
        }
        if (isScrollable) {
            VerticalScrollbar(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .padding(vertical = 8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                adapter = rememberScrollbarAdapter(state),
                style = defaultScrollbarStyle().copy(
                    unhoverColor = MaterialTheme.colorScheme.outline,
                    hoverColor = MaterialTheme.colorScheme.primary,
                    thickness = 8.dp,
                    shape = CircleShape
                )
            )
        }
    }
}

@Composable
fun ArtifactCard(
    artifact: Artifact,
    isInstalled: Boolean,
    onInstall: () -> Unit,
    onViewDependencies: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        SelectionContainer {
            Row(
                modifier = Modifier.padding(8.dp).height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${artifact.version}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Hash: ${artifact.hash ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildAnnotatedString {
                            withLink(LinkAnnotation.Url(artifact.downloadUrl)) {
                                append(artifact.downloadUrl)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                VerticalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = onInstall, enabled = !isInstalled , modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = if (isInstalled) painterResource(Res.drawable.check_24px) else painterResource(Res.drawable.download_24px),
                            contentDescription = "Install",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = onViewDependencies, modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.rule_24px),
                            contentDescription = "Dependencies",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ModVersionDependenciesContent(
    mod: Extension,
    version: Version,
    onOpenMod: (String) -> Unit,
) {
    val artifact = mod.artifacts.find { it.version == version }
    val state = rememberLazyListState()
    val allMods by RepoMods.mods.collectAsState()

    val dependents = remember(allMods, mod.id) {
        allMods.mapNotNull { otherMod ->
            val versions = otherMod.artifacts
                .filter { art ->
                    art.extends?.id == mod.id || art.dependencies.any { it.id == mod.id }
                }
                .map { it.version }

            if (versions.isNotEmpty()) otherMod.id to versions else null
        }
    }

    val isScrollable by remember {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo.size < state.layoutInfo.totalItemsCount ||
                state.firstVisibleItemScrollOffset > 0
        }
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = state,
        ) {
            if (artifact == null) {
                item { Text("Artifact version not found.", color = MaterialTheme.colorScheme.error) }
                return@LazyColumn
            }

            item { DependencyHeader("Extends", MaterialTheme.colorScheme.primary) }
            if (artifact.extends != null) {
                item {
                    val extendsVersion = artifact.extends.version
                    DependencyItemCard(
                        id = artifact.extends.id,
                        versions = if (extendsVersion != null) listOf(extendsVersion) else emptyList(),
                        onOpenMod = onOpenMod,
                        isIncompatible = false
                    )
                }
            } else {
                item { EmptySectionText("None") }
            }

            item {
                Spacer(Modifier.height(8.dp))
                DependencyHeader("Dependencies", MaterialTheme.colorScheme.primary)
            }
            if (artifact.dependencies.isNotEmpty()) {
                val groupedDeps = artifact.dependencies.groupBy { it.id }
                items(groupedDeps.keys.toList()) { id ->
                    val versions = groupedDeps[id]?.mapNotNull { it.version } ?: emptyList()
                    DependencyItemCard(id, versions, onOpenMod, false)
                }
            } else {
                item { EmptySectionText("None") }
            }

            item {
                Spacer(Modifier.height(8.dp))
                DependencyHeader("Incompatibilities", MaterialTheme.colorScheme.error)
            }
            if (artifact.incompatibilities.isNotEmpty()) {
                val groupedIncompats = artifact.incompatibilities.groupBy { it.id }
                items(groupedIncompats.keys.toList()) { id ->
                    val versions = groupedIncompats[id]?.mapNotNull { it.version } ?: emptyList()
                    DependencyItemCard(id, versions, onOpenMod, true)
                }
            } else {
                item { EmptySectionText("None") }
            }

            item {
                Spacer(Modifier.height(8.dp))
                DependencyHeader("Dependents", MaterialTheme.colorScheme.secondary)
            }
            if (dependents.isNotEmpty()) {
                items(dependents) { (id, versions) ->
                    DependencyItemCard(id, versions, onOpenMod, false)
                }
            } else {
                item { EmptySectionText("None") }
            }
        }

        if (isScrollable) {
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight().width(8.dp).padding(vertical = 8.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                adapter = rememberScrollbarAdapter(state),
                style = defaultScrollbarStyle().copy(
                    unhoverColor = MaterialTheme.colorScheme.outline,
                    hoverColor = MaterialTheme.colorScheme.primary,
                    thickness = 8.dp,
                    shape = CircleShape
                )
            )
        }
    }
}


@Composable
private fun DependencyHeader(text: String, color: Color) {
    Column {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = color,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun DependencyItemCard(
    id: String,
    versions: List<Version>,
    onOpenMod: (String) -> Unit,
    isIncompatible: Boolean,
) {
    val mod = RepoMods.mods.collectAsState().value.find { it.id == id }

    Surface(
        onClick = { if (mod != null) onOpenMod(id) },
        enabled = mod != null,
        color = if (isIncompatible) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mod?.displayName ?: id,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncompatible) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (versions.isNotEmpty()) {
                    Text(
                        text = versions.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (mod != null) {
                Icon(
                    painter = painterResource(Res.drawable.arrow_right_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = "MISSING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptySectionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    )
}