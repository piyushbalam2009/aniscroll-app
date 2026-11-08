package com.example.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.designsystem.AnimePosterCard
import com.example.core.designsystem.EmptyIllustrationType
import com.example.core.designsystem.EmptyStateView
import com.example.core.designsystem.GreenPrimaryButton
import com.example.core.designsystem.SelectablePillChip
import com.example.core.designsystem.SpinningDotsLoader
import com.example.domain.model.Anime
import com.example.domain.model.AnimeFilterState
import com.example.feature.common.SearchUiState
import com.example.ui.theme.AniCardBackground
import com.example.ui.theme.AniGreen
import com.example.ui.theme.AniGreenSurface
import com.example.ui.theme.AniSurfaceElevated
import com.example.ui.theme.AniTextPrimary
import com.example.ui.theme.AniTextSecondary

@Composable
fun SearchScreen(
    searchState: SearchUiState,
    recentSearches: List<String>,
    topSearches: List<Anime>,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onOpenSortFilter: () -> Unit,
    onDeleteRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onAnimeClick: (Anime) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("search_screen")
    ) {
        // Top Search Bar + Filter Button matching Screenshots 25, 26, 27, 29
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("search_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background( if (isFocused) AniGreenSurface else AniCardBackground )
                    .border(
                        width = 1.5.dp,
                        color = if (isFocused) AniGreen else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = if (isFocused) AniGreen else AniTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchState.query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AniTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchState.query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(AniGreen),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    onSubmitSearch(searchState.query)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isFocused = it.isFocused }
                                .testTag("search_input")
                        )
                    }
                    if (searchState.query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = AniTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Filter button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AniGreenSurface)
                    .clickable(onClick = onOpenSortFilter)
                    .testTag("open_sort_filter_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Sort and Filter",
                    tint = AniGreen
                )
            }
        }

        // Active filter chips bar (matching Screenshot 29_Dark_sort & filter results)
        val activeBadges = buildActiveFilterLabels(searchState.filter)
        if (activeBadges.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeBadges) { label ->
                    Surface(
                        color = AniGreen,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        when {
            searchState.isSearching -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SpinningDotsLoader()
                }
            }

            searchState.hasSearched && searchState.results.isEmpty() -> {
                EmptyStateView(
                    title = stringResource(R.string.not_found_title),
                    subtitle = stringResource(R.string.not_found_subtitle),
                    illustrationType = EmptyIllustrationType.NOT_FOUND_404,
                    modifier = Modifier.fillMaxSize()
                )
            }

            searchState.hasSearched && searchState.results.isNotEmpty() -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("search_results_grid")
                ) {
                    items(searchState.results, key = { it.anilistId }) { anime ->
                        AnimePosterCard(
                            anime = anime,
                            onClick = { onAnimeClick(anime) }
                        )
                    }
                }
            }

            else -> {
                // Default Search Discovery: Recent Searches + Top Searches (matches Screenshot 25)
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Clear all",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AniGreen,
                                    modifier = Modifier.clickable(onClick = onClearRecentSearches)
                                )
                            }
                        }

                        items(recentSearches, key = { "recent_$it" }) { queryItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSubmitSearch(queryItem) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = AniTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = queryItem,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteRecentSearch(queryItem) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = AniTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Top Searches",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        )
                    }

                    items(topSearches, key = { "top_search_${it.anilistId}" }) { anime ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onAnimeClick(anime) }
                                .testTag("top_search_item_${anime.anilistId}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(138.dp)
                                    .aspectRatio(1.45f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AniCardBackground)
                            ) {
                                AsyncImage(
                                    model = anime.coverUrl?.takeIf { it.isNotBlank() } ?: anime.posterUrl,
                                    contentDescription = anime.displayTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color.White.copy(alpha = 0.88f),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.Center)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = anime.displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildActiveFilterLabels(filter: AnimeFilterState): List<String> {
    val list = mutableListOf<String>()
    when (filter.sort) {
        "POPULARITY_DESC" -> list.add("Popularity")
        "SCORE_DESC" -> list.add("Top Score")
        "START_DATE_DESC" -> list.add("Latest Release")
        "UPDATED_AT_DESC" -> list.add("Recently Updated")
    }
    filter.format?.let { list.add(if (it == "TV") "Episode" else it.lowercase().replaceFirstChar { c -> c.uppercase() }) }
    filter.countryOfOrigin?.let {
        list.add(
            when (it) {
                "JP" -> "Japan"
                "CN" -> "Chinese"
                "KR" -> "Korea"
                else -> it
            }
        )
    }
    list.addAll(filter.genres)
    filter.year?.let { list.add(it.toString()) }
    filter.season?.let { list.add(it.lowercase().replaceFirstChar { c -> c.uppercase() }) }
    filter.status?.let { list.add(it.lowercase().replace('_', ' ').replaceFirstChar { c -> c.uppercase() }) }
    return list
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SortAndFilterScreen(
    initialFilter: AnimeFilterState,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onApply: (AnimeFilterState) -> Unit
) {
    var sort by remember { mutableStateOf(initialFilter.sort) }
    var format by remember { mutableStateOf(initialFilter.format) }
    var region by remember { mutableStateOf(initialFilter.countryOfOrigin) }
    var genres by remember { mutableStateOf(initialFilter.genres) }
    var year by remember { mutableStateOf(initialFilter.year) }
    var season by remember { mutableStateOf(initialFilter.season) }
    var status by remember { mutableStateOf(initialFilter.status) }

    val sortOptions = listOf(
        "Popularity" to "POPULARITY_DESC",
        "Score" to "SCORE_DESC",
        "Latest Release" to "START_DATE_DESC",
        "Recently Updated" to "UPDATED_AT_DESC"
    )
    val formatOptions = listOf(
        "Episode (TV)" to "TV",
        "Movie" to "MOVIE",
        "OVA" to "OVA",
        "ONA" to "ONA",
        "Special" to "SPECIAL"
    )
    val regionOptions = listOf(
        "All" to null,
        "Japan" to "JP",
        "Chinese" to "CN",
        "Korea" to "KR"
    )
    val genreOptions = listOf(
        "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror",
        "Mystery", "Romance", "Sci-Fi", "Slice of Life", "Sports",
        "Supernatural", "Thriller"
    )
    val yearOptions = listOf(null, 2026, 2025, 2024, 2023, 2022, 2021, 2020)
    val seasonOptions = listOf(
        "All" to null,
        "Winter" to "WINTER",
        "Spring" to "SPRING",
        "Summer" to "SUMMER",
        "Fall" to "FALL"
    )
    val statusOptions = listOf(
        "All" to null,
        "Releasing" to "RELEASING",
        "Finished" to "FINISHED",
        "Upcoming" to "NOT_YET_RELEASED"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("sort_filter_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("filter_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(R.string.sort_and_filter),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FilterSectionTitle("Sort")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                sortOptions.forEach { (label, value) ->
                    SelectablePillChip(
                        text = label,
                        selected = sort == value,
                        onClick = { sort = value }
                    )
                }
            }

            FilterSectionTitle("Categories")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SelectablePillChip(
                    text = "All",
                    selected = format == null,
                    onClick = { format = null }
                )
                formatOptions.forEach { (label, value) ->
                    SelectablePillChip(
                        text = label,
                        selected = format == value,
                        onClick = { format = if (format == value) null else value }
                    )
                }
            }

            FilterSectionTitle("Region")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                regionOptions.forEach { (label, value) ->
                    SelectablePillChip(
                        text = label,
                        selected = region == value,
                        onClick = { region = value }
                    )
                }
            }

            FilterSectionTitle("Genre")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SelectablePillChip(
                    text = "All",
                    selected = genres.isEmpty(),
                    onClick = { genres = emptySet() }
                )
                genreOptions.forEach { genre ->
                    val isSelected = genre in genres
                    SelectablePillChip(
                        text = genre,
                        selected = isSelected,
                        onClick = {
                            genres = if (isSelected) genres - genre else genres + genre
                        }
                    )
                }
            }

            FilterSectionTitle("Release Year")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                yearOptions.forEach { yr ->
                    SelectablePillChip(
                        text = yr?.toString() ?: "All",
                        selected = year == yr,
                        onClick = { year = yr }
                    )
                }
            }

            FilterSectionTitle("Season")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                seasonOptions.forEach { (label, value) ->
                    SelectablePillChip(
                        text = label,
                        selected = season == value,
                        onClick = { season = value }
                    )
                }
            }

            FilterSectionTitle("Status")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                statusOptions.forEach { (label, value) ->
                    SelectablePillChip(
                        text = label,
                        selected = status == value,
                        onClick = { status = value }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Bottom Reset & Apply buttons matching Screenshot 28
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = {
                    sort = "POPULARITY_DESC"
                    format = null
                    region = null
                    genres = emptySet()
                    year = null
                    season = null
                    status = null
                    onReset()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AniSurfaceElevated,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("filter_reset_button")
            ) {
                Text(
                    text = stringResource(R.string.reset),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            GreenPrimaryButton(
                text = stringResource(R.string.apply),
                onClick = {
                    onApply(
                        initialFilter.copy(
                            sort = sort,
                            format = format,
                            countryOfOrigin = region,
                            genres = genres,
                            year = year,
                            season = season,
                            status = status
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                testTag = "filter_apply_button"
            )
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}
