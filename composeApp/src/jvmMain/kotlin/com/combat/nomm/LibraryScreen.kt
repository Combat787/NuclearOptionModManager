package com.combat.nomm

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import nuclearoptionmodmanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onOpenMod: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val localMods by LocalMods.mods.collectAsState()

    var menuExpanded by remember { mutableStateOf(false) }

    val installedExtensions = remember(localMods) {
        localMods.values.map { modMeta ->
            RepoMods.mods.value.find { it.id == modMeta.id }
                ?: SettingsManager.config.value.cachedManifest.find { it.id == modMeta.id } ?: Extension(
                    modMeta.id,
                    modMeta.id,
                    "",
                    emptyList(),
                    "",
                    emptyList(),
                    emptyList()
                )
        }
    }

    var filteredMods by remember { mutableStateOf(installedExtensions) }

    LaunchedEffect(searchQuery, installedExtensions) {
        delay(250)
        val results = withContext(Dispatchers.Default) {
            if (searchQuery.isBlank()) {
                installedExtensions
            } else {
                installedExtensions.sortFilterByQuery(searchQuery, minSimilarity = 0.3) { ext, query ->
                    val nameScore = fuzzyPowerScore(query, ext.displayName)
                    val idScore = fuzzyPowerScore(query, ext.id)
                    val authorScore = ext.authors.maxOfOrNull { fuzzyPowerScore(query, it) } ?: 0.0
                    val weightedScore = (nameScore * 5.0) + (idScore * 2.0) + (authorScore * 1.5)
                    val lengthPenalty = if (ext.displayName.length > query.length * 3) 0.9 else 1.0
                    ext to (weightedScore * lengthPenalty)
                }
            }
        }
        filteredMods = results
    }
    val state = rememberLazyListState()
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            state = state,
        ) {
            stickyHeader {
                Row(
                    modifier = Modifier.padding(top = 16.dp).height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.weight(1f).border(
                            width = Dp.Hairline,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small
                        )
                    )

                    Button(
                        onClick = { LocalMods.refresh() },
                        modifier = Modifier.fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(
                            painterResource(Res.drawable.refresh_24px),
                            null,
                        )
                    }

                    Box {
                        Button(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.fillMaxHeight(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Icon(
                                painterResource(Res.drawable.more_vert_24px),
                                contentDescription = "Options"
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Modpack") },
                                onClick = {
                                    menuExpanded = false
                                    LocalMods.exportMods()
                                },
                                leadingIcon = { Icon(painterResource(Res.drawable.file_export_24px), null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Modpack") },
                                onClick = {
                                    menuExpanded = false
                                    LocalMods.importMods()
                                },
                                leadingIcon = { Icon(painterResource(Res.drawable.file_open_24px), null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Add from file") },
                                onClick = {
                                    menuExpanded = false
                                    LocalMods.addFromFile()
                                },
                                leadingIcon = { Icon(painterResource(Res.drawable.folder_open_24px), null) }
                            )
                        }
                    }
                }
            }

            if (filteredMods.isEmpty()) {
                item {
                    SelectionContainer {
                        Text(
                            "Nothing here. huh",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                items(filteredMods, key = { it.id }) { ext ->
                    ModItem(mod = ext, onClick = { onOpenMod(ext.id) })
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier.fillMaxHeight().width(8.dp).padding(vertical = 16.dp),
            adapter = rememberScrollbarAdapter(state),
            style = defaultScrollbarStyle().copy(
                unhoverColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                hoverColor = MaterialTheme.colorScheme.primary,
                thickness = 4.dp
            )
        )
    }
}