package com.famy.tree.ui.screen.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.famy.tree.R
import com.famy.tree.domain.model.TreeLayoutType
import com.famy.tree.ui.component.EmptyState
import com.famy.tree.ui.component.FamyFab
import com.famy.tree.ui.component.LoadingScreen
import com.famy.tree.ui.screen.tree.component.TreeCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeScreen(
    treeId: Long,
    onMemberClick: (Long) -> Unit,
    onAddMember: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToGallery: () -> Unit,
    viewModel: TreeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLayoutMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.tree?.name ?: stringResource(R.string.nav_tree))
                },
                actions = {
                    IconButton(onClick = onNavigateToTimeline) {
                        Icon(Icons.Default.Timeline, contentDescription = stringResource(R.string.nav_timeline))
                    }
                    IconButton(onClick = onNavigateToStatistics) {
                        Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.nav_statistics))
                    }
                    IconButton(onClick = onNavigateToGallery) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = stringResource(R.string.nav_gallery))
                    }
                    Box {
                        IconButton(onClick = { showLayoutMenu = true }) {
                            Icon(Icons.Default.AccountTree, contentDescription = "Layout")
                        }
                        DropdownMenu(
                            expanded = showLayoutMenu,
                            onDismissRequest = { showLayoutMenu = false }
                        ) {
                            TreeLayoutType.entries.forEach { layoutType ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (layoutType) {
                                                TreeLayoutType.VERTICAL -> stringResource(R.string.tree_layout_vertical)
                                                TreeLayoutType.HORIZONTAL -> stringResource(R.string.tree_layout_horizontal)
                                                TreeLayoutType.FAN_CHART -> stringResource(R.string.tree_layout_fan)
                                                TreeLayoutType.PEDIGREE -> stringResource(R.string.tree_layout_pedigree)
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.setLayoutType(layoutType)
                                        showLayoutMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FamyFab(
                onClick = onAddMember,
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_add_member)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingScreen()
                }
                uiState.members.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.AccountTree,
                        title = stringResource(R.string.tree_empty),
                        subtitle = "Add your first family member to start building your tree",
                        action = {
                            TextButton(onClick = onAddMember) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.tree_add_root))
                            }
                        }
                    )
                }
                else -> {
                    TreeCanvas(
                        nodes = uiState.layoutNodes,
                        relationships = uiState.relationships,
                        bounds = uiState.bounds,
                        config = uiState.layoutConfig,
                        scale = uiState.scale,
                        offsetX = uiState.offsetX,
                        offsetY = uiState.offsetY,
                        selectedMemberId = uiState.selectedMemberId,
                        onMemberClick = onMemberClick,
                        onMemberLongClick = { memberId ->
                            viewModel.selectMember(memberId)
                        },
                        onScaleChange = viewModel::onScaleChange,
                        onPan = viewModel::onPan,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        FilledIconButton(
                            onClick = { viewModel.onScaleChange(1.2f) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = stringResource(R.string.tree_zoom_in))
                        }
                        FilledIconButton(
                            onClick = { viewModel.onScaleChange(0.8f) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = stringResource(R.string.tree_zoom_out))
                        }
                        FilledIconButton(
                            onClick = { viewModel.resetView() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = stringResource(R.string.tree_center))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                MaterialTheme.shapes.small
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(uiState.scale * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
