package com.famy.tree.ui.screen.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.famy.tree.R
import com.famy.tree.domain.model.Gender
import com.famy.tree.ui.component.EmptyState
import com.famy.tree.ui.component.LoadingScreen
import com.famy.tree.ui.component.MemberAvatar
import com.famy.tree.ui.component.SearchBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onMemberClick: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_search)) },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.search_filter),
                            tint = if (viewModel.hasActiveFilters) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onClear = { viewModel.clearSearch() }
            )

            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FilterSection(
                    selectedGender = uiState.selectedGender,
                    showLivingOnly = uiState.showLivingOnly,
                    selectedTreeId = uiState.selectedTreeId,
                    trees = uiState.trees,
                    hasActiveFilters = viewModel.hasActiveFilters,
                    onGenderSelected = viewModel::setGenderFilter,
                    onLivingOnlyChanged = viewModel::setLivingOnlyFilter,
                    onTreeSelected = viewModel::setTreeFilter,
                    onClearFilters = viewModel::clearFilters
                )
            }

            when {
                uiState.isLoading -> {
                    LoadingScreen()
                }
                uiState.searchQuery.isBlank() && uiState.recentSearches.isNotEmpty() -> {
                    RecentSearchesSection(
                        recentSearches = uiState.recentSearches,
                        onSearchSelected = { viewModel.setSearchQuery(it) }
                    )
                }
                uiState.searchQuery.isBlank() -> {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = "Search Family Members",
                        subtitle = "Search by name, place, occupation, or any other information"
                    )
                }
                uiState.hasSearched && uiState.results.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.search_no_results),
                        subtitle = "Try a different search term or adjust your filters",
                        action = {
                            if (viewModel.hasActiveFilters) {
                                TextButton(onClick = { viewModel.clearFilters() }) {
                                    Text(stringResource(R.string.filter_clear))
                                }
                            }
                        }
                    )
                }
                else -> {
                    SearchResultsList(
                        results = uiState.results,
                        onMemberClick = { memberId ->
                            viewModel.addToRecentSearches(uiState.searchQuery)
                            onMemberClick(memberId)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    selectedGender: Gender?,
    showLivingOnly: Boolean,
    selectedTreeId: Long?,
    trees: List<com.famy.tree.domain.model.FamilyTree>,
    hasActiveFilters: Boolean,
    onGenderSelected: (Gender?) -> Unit,
    onLivingOnlyChanged: (Boolean) -> Unit,
    onTreeSelected: (Long?) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Gender",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedGender == null,
                onClick = { onGenderSelected(null) },
                label = { Text("All") }
            )
            Gender.entries.forEach { gender ->
                FilterChip(
                    selected = selectedGender == gender,
                    onClick = { onGenderSelected(gender) },
                    label = {
                        Text(
                            when (gender) {
                                Gender.MALE -> stringResource(R.string.gender_male)
                                Gender.FEMALE -> stringResource(R.string.gender_female)
                                Gender.OTHER -> stringResource(R.string.gender_other)
                                Gender.UNKNOWN -> stringResource(R.string.gender_unknown)
                            }
                        )
                    }
                )
            }
        }

        if (trees.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Family Tree",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedTreeId == null,
                    onClick = { onTreeSelected(null) },
                    label = { Text("All Trees") }
                )
                trees.forEach { tree ->
                    FilterChip(
                        selected = selectedTreeId == tree.id,
                        onClick = { onTreeSelected(tree.id) },
                        label = { Text(tree.name) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = showLivingOnly,
                onClick = { onLivingOnlyChanged(!showLivingOnly) },
                label = { Text(stringResource(R.string.filter_living)) }
            )

            if (hasActiveFilters) {
                TextButton(onClick = onClearFilters) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.filter_clear))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun RecentSearchesSection(
    recentSearches: List<String>,
    onSearchSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Recent Searches",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        recentSearches.forEach { query ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSearchSelected(query) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<SearchResult>,
    onMemberClick: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = results,
            key = { it.member.id }
        ) { result ->
            SearchResultItem(
                result = result,
                onClick = { onMemberClick(result.member.id) }
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(member = result.member, size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.member.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = result.treeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Matched: ${result.matchedField}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                result.member.age?.let { age ->
                    Text(
                        text = if (result.member.isLiving) {
                            stringResource(R.string.years_old, age)
                        } else {
                            stringResource(R.string.lived_years, age)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
