package com.example.feature.mylist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.designsystem.AniScrollLogo
import com.example.core.designsystem.AnimePosterCard
import com.example.core.designsystem.EmptyIllustrationType
import com.example.core.designsystem.EmptyStateView
import com.example.core.designsystem.SelectablePillChip
import com.example.domain.model.Anime
import com.example.domain.model.WatchStatus
import com.example.domain.model.WatchlistItem
import com.example.feature.details.WatchlistStatusBottomSheet
import com.example.ui.theme.AniCardBackground
import com.example.ui.theme.AniGreen
import com.example.ui.theme.AniTextSecondary

@Composable
fun MyListScreen(
    watchlist: List<WatchlistItem>,
    onAnimeClick: (Anime) -> Unit,
    onSetWatchStatus: (Anime, WatchStatus) -> Unit,
    onRemoveFromWatchlist: (Int) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<WatchStatus?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeStatusItem by remember { mutableStateOf<WatchlistItem?>(null) }

    val filteredList = remember(watchlist, selectedCategory, searchQuery) {
        watchlist.filter { item ->
            val matchesCat = when (selectedCategory) {
                null -> true
                WatchStatus.FAVORITES -> item.isFavorite || item.status == WatchStatus.FAVORITES
                else -> item.status == selectedCategory
            }
            val matchesSearch = searchQuery.isBlank() ||
                item.anime.displayTitle.contains(searchQuery, ignoreCase = true) ||
                item.anime.romajiTitle.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("my_list_screen")
    ) {
        // Top Header matching Screenshot 44 & 45
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AniScrollLogo(size = 30.dp)
                Text(
                    text = stringResource(R.string.nav_mylist),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(
                onClick = {
                    showSearchBar = !showSearchBar
                    if (!showSearchBar) searchQuery = ""
                },
                modifier = Modifier.testTag("mylist_search_toggle")
            ) {
                Icon(
                    imageVector = if (showSearchBar) Icons.Default.Close else Icons.Outlined.Search,
                    contentDescription = "Search My List",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (showSearchBar) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search saved anime...", color = AniTextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AniGreen,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = AniCardBackground,
                    unfocusedContainerColor = AniCardBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("mylist_search_input")
            )
        }

        // Category Filter Chips (All, Watching, Plan to Watch, Completed, On Hold, Dropped, Favorites)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SelectablePillChip(
                    text = "All (${watchlist.size})",
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    testTag = "mylist_cat_all"
                )
            }
            items(WatchStatus.entries, key = { it.name }) { status ->
                SelectablePillChip(
                    text = status.displayName,
                    selected = selectedCategory == status,
                    onClick = { selectedCategory = status },
                    testTag = "mylist_cat_${status.name}"
                )
            }
        }

        if (filteredList.isEmpty()) {
            EmptyStateView(
                title = stringResource(R.string.empty_list_title),
                subtitle = stringResource(R.string.empty_list_subtitle),
                illustrationType = EmptyIllustrationType.EMPTY_LIST,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("mylist_grid")
            ) {
                items(filteredList, key = { it.anilistId }) { item ->
                    Box {
                        AnimePosterCard(
                            anime = item.anime,
                            episodeLabel = item.status.displayName,
                            onClick = { onAnimeClick(item.anime) }
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable { activeStatusItem = item },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Change Status",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    activeStatusItem?.let { item ->
        WatchlistStatusBottomSheet(
            anime = item.anime,
            inWatchlist = true,
            onDismiss = { activeStatusItem = null },
            onSelectStatus = { status ->
                onSetWatchStatus(item.anime, status)
                activeStatusItem = null
            },
            onToggleQuick = {
                onRemoveFromWatchlist(item.anilistId)
                activeStatusItem = null
            }
        )
    }
}
