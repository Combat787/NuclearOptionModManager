package com.combat.nomm

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
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
    val mod = RepoMods.mods.value.find { it.id == modId } ?: run { onBack(); return }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val backStack = rememberNavBackStack(ModNavigation.config, ModNavigation.Details)
        val currentKey = backStack.lastOrNull() ?: ModNavigation.Details
        ModTitleCard(mod, onBack)
        ModNavigationBar(currentKey, backStack)
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            transitionSpec = {
                EnterTransition.None togetherWith ExitTransition.None
            },
            popTransitionSpec = {
                EnterTransition.None togetherWith ExitTransition.None
            },
            predictivePopTransitionSpec = {
                EnterTransition.None togetherWith ExitTransition.None
            },
            entryProvider = entryProvider {
                entry<ModNavigation.Details> {
                    ModDetailsContent(mod)
                }
                entry<ModNavigation.Versions> {
                    ModVersionsContent(mod , onOpenMod) { version ->
                        backStack.clear()
                        backStack.add(ModNavigation.Dependencies(version))
                    }
                }
                entry<ModNavigation.Dependencies> {
                    ModVersionDependenciesContent(mod, it.version)
                }
            })
    }
}

@Composable
fun ModVersionDependenciesContent(mod: Extension, version: Version) {
    mod.artifacts.first {
        it.version == version
    }.extends

}


@Composable
private fun ModNavigationBar(
    currentKey: NavKey,
    backStack: NavBackStack<NavKey>,
) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavItem(
                selected = currentKey is ModNavigation.Details,
                label = "Details",
                icon = Res.drawable.info_24px
            ) {
                backStack.clear()
                backStack.add(ModNavigation.Details)
            }

            NavItem(
                selected = currentKey is ModNavigation.Versions,
                label = "Versions",
                icon = Res.drawable.list_24px
            ) {
                backStack.clear()
                backStack.add(ModNavigation.Versions)
            }
        }
    
}

@Composable
private fun NavItem(
    selected: Boolean,
    label: String,
    icon: DrawableResource,
    onClick: () -> Unit,
) {
    val backgroundColor =
        if (selected) MaterialTheme.colorScheme.secondaryContainer
        else Color.Transparent

    val contentColor =
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = contentColor
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@Composable
fun BaseTitleCard(
    onBack: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()

        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(Res.drawable.close_24px),
                contentDescription = "Close",
            )
        }
    }
}

@Composable
fun ModTitleCard(mod: Extension, onBack: () -> Unit) {
    BaseTitleCard(onBack = onBack) {
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Column {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = MaterialTheme.typography.headlineLarge.toSpanStyle()) {
                            append(mod.displayName)
                        }
                        appendLine()
                        withStyle(style = MaterialTheme.typography.labelMedium.toSpanStyle()) {
                            append("Authors: ")
                            append(mod.authors.joinToString(", "))
                        }
                    }
                )
            }
        }

        IconButton(onClick = {  }) {
            Icon(
                painter = painterResource(Res.drawable.download_24px),
                contentDescription = "Download",
            )
        }
    }
}

@Composable
fun <T> BaseListContent(
    items: List<T>,
    emptyMessage: String = "Nothing here. huh",
    itemContent: @Composable (T) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
    ) {
        if (items.isEmpty()) {
            item {
                SelectionContainer {
                    Text(emptyMessage, modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            itemsIndexed(items) { i, item ->
                itemContent(item)

                if (i < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ModVersionsContent(mod: Extension, onOpenMod: (String) -> Unit, onOpenDependencies: (Version) -> Unit) {
    val sortedArtifacts = remember(mod.artifacts) {
        mod.artifacts.sortedByDescending { it.version }
    }

    BaseListContent(
        items = sortedArtifacts,
    ) { artifact ->
        ArtifactCard(
            artifact, onOpenMod
        ) {
            onOpenDependencies(artifact.version)
        }
    }
}

@Composable
fun ModDetailsContent(mod: Extension) {
    Column(
        modifier = Modifier.fillMaxSize(1f).clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
        verticalArrangement = Arrangement.Top
    ) {
        mod.infoUrl.let { url ->
            SelectionContainer {
                Text(buildAnnotatedString {
                    append("Info: ")
                    withLink(LinkAnnotation.Url(url)) { append(url) }
                })
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        SelectionContainer {
            Text(mod.description)
        }
    }
}

@Composable
fun ArtifactCard(
    artifact: Artifact,
    onOpenMod: (String) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionContainer(
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "Version: ${artifact.version}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = buildAnnotatedString {
                        artifact.downloadUrl.let { url ->
                            append("Download: ")
                            withLink(LinkAnnotation.Url(url)) { append(url) }
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis
                )

                Text(
                    text = buildAnnotatedString {
                        artifact.extends?.let { extends ->
                            append("Extends: ")
                            val mod = RepoMods.mods.value.firstOrNull { it.id == extends.id }
                            if (mod != null) {
                                withLink(LinkAnnotation.Clickable("open${mod.id}") {
                                    onOpenMod(mod.id)
                                }) {
                                    append(mod.displayName)
                                }
                            } else {
                                append("Not Found id = ${extends.id}")
                            }
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Hash: ${artifact.hash}",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Button(
            onClick = onClick,
            modifier = Modifier.requiredSize(40.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.rule_24px),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}