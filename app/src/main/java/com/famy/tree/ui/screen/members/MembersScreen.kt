package com.famy.tree.ui.screen.members

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.famy.tree.R
import com.famy.tree.domain.model.Gender
import com.famy.tree.ui.component.EmptyState
import com.famy.tree.ui.component.FamyFab
import com.famy.tree.ui.component.LoadingScreen
import com.famy.tree.ui.component.MemberListItem
import com.famy.tree.ui.component.SearchBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MembersScreen(
    treeId: Long,
    onMemberClick: (Long) -> Unit,
    onAddMember: () -> Unit,
    viewModel: MembersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val generations by viewModel.generations.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var showFilters by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.nav_members))
                        if (uiState.totalCount > 0) {
                            Text(
                                text = "${uiState.filteredCount} of ${uiState.totalCount} members",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
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
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort"
                        )
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            MemberSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (option) {
                                                MemberSortOption.NAME_ASC -> "Name (A-Z)"
                                                MemberSortOption.NAME_DESC -> "Name (Z-A)"
                                                MemberSortOption.AGE_ASC -> "Age (Youngest)"
                                                MemberSortOption.AGE_DESC -> "Age (Oldest)"
                                                MemberSortOption.GENERATION -> "Generation"
                                                MemberSortOption.RECENT -> "Recently Added"
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FamyFab(
                onClick = onAddMember,
                contentDescription = stringResource(R.string.cd_add_member)
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically(),
                exit = shrinkVertically()
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
                            selected = uiState.selectedGender == null,
                            onClick = { viewModel.setGenderFilter(null) },
                            label = { Text("All") }
                        )
                        Gender.entries.forEach { gender ->
                            FilterChip(
                                selected = uiState.selectedGender == gender,
                                onClick = { viewModel.setGenderFilter(gender) },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    if (generations.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.filter_generation),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = uiState.selectedGeneration == null,
                                onClick = { viewModel.setGenerationFilter(null) },
                                label = { Text("All") }
                            )
                            generations.forEach { gen ->
                                FilterChip(
                                    selected = uiState.selectedGeneration == gen,
                                    onClick = { viewModel.setGenerationFilter(gen) },
                                    label = { Text("Gen $gen") }
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
                            selected = uiState.showLivingOnly,
                            onClick = { viewModel.setLivingOnlyFilter(!uiState.showLivingOnly) },
                            label = { Text(stringResource(R.string.filter_living)) }
                        )

                        if (viewModel.hasActiveFilters) {
                            TextButton(onClick = { viewModel.clearFilters() }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(stringResource(R.string.filter_clear))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (uiState.isLoading) {
                LoadingScreen()
            } else if (uiState.members.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.People,
                    title = "No family members yet",
                    subtitle = "Add your first family member to get started",
                    action = {
                        TextButton(onClick = onAddMember) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.cd_add_member))
                        }
                    }
                )
            } else if (uiState.filteredMembers.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.People,
                    title = stringResource(R.string.search_no_results),
                    subtitle = "Try adjusting your search or filters",
                    action = {
                        TextButton(onClick = { viewModel.clearFilters() }) {
                            Text(stringResource(R.string.filter_clear))
                        }
                    }
                )
            } else {
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
                        items = uiState.filteredMembers,
                        key = { it.id }
                    ) { member ->
                        MemberListItem(
                            member = member,
                            onClick = { onMemberClick(member.id) }
                        )
                    }
                }
            }
        }
    }
}
