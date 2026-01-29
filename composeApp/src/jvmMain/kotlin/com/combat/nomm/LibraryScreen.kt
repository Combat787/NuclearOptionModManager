package com.combat.nomm

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onNavigateToMod: (String) -> Unit,
) {
    val localMods by LocalMods.mods.collectAsState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {if (localMods.isEmpty()) {
        item {
            SelectionContainer {
                Text("Nothing here. huh", modifier = Modifier.padding(horizontal =  16.dp), style = MaterialTheme.typography.labelLarge)

            }
        }
    } else {
        items(localMods.toList()) { mod ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ModItem(mod.second.cachedExtension?: Extension(mod.second.id,mod.second.id,"",emptyList(),"",emptyList(),emptyList()), onClick = { onNavigateToMod(mod.first) })
            }
        }
    }
    }
}