package com.famy.tree.ui.screen.gallery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.famy.tree.R
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Media
import com.famy.tree.domain.model.MediaKind
import com.famy.tree.domain.model.MediaWithMember
import com.famy.tree.ui.component.BackButton
import com.famy.tree.ui.component.EmptyState
import com.famy.tree.ui.component.LoadingScreen
import com.famy.tree.ui.component.SearchBar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    treeId: Long,
    onNavigateBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    var showMemberPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddMediaSheet by remember { mutableStateOf(false) }
    var selectedMemberForAdd by remember { mutableStateOf<FamilyMember?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedMemberForAdd?.let { member ->
                viewModel.addMedia(member.id, uri)
            }
        }
        showAddMediaSheet = false
        selectedMemberForAdd = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.nav_gallery))
                        Text(
                            text = "${uiState.totalCount} items • ${uiState.totalSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = { BackButton(onClick = onNavigateBack) },
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
                    IconButton(onClick = {
                        viewModel.setViewMode(
                            if (uiState.viewMode == GalleryViewMode.GRID) GalleryViewMode.LIST
                            else GalleryViewMode.GRID
                        )
                    }) {
                        Icon(
                            imageVector = if (uiState.viewMode == GalleryViewMode.GRID)
                                Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle view mode"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.members.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showAddMediaSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add media")
                }
            }
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
                        text = "Media Type",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = uiState.selectedFilter == null,
                            onClick = { viewModel.setFilter(null) },
                            label = { Text("All") }
                        )
                        MediaKind.entries.forEach { type ->
                            FilterChip(
                                selected = uiState.selectedFilter == type,
                                onClick = { viewModel.setFilter(type) },
                                label = { Text(type.displayName) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getMediaTypeIcon(type),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = uiState.selectedMemberId != null,
                            onClick = { showMemberPicker = true },
                            label = {
                                val selectedMember = uiState.members.find { it.id == uiState.selectedMemberId }
                                Text(selectedMember?.fullName ?: "Filter by member")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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

            when {
                uiState.isLoading -> {
                    LoadingScreen()
                }
                uiState.mediaItems.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.PhotoLibrary,
                        title = "No media yet",
                        subtitle = "Add photos and documents to family members",
                        action = if (uiState.members.isNotEmpty()) {
                            {
                                TextButton(onClick = { showAddMediaSheet = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Media")
                                }
                            }
                        } else null
                    )
                }
                uiState.filteredMedia.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.PhotoLibrary,
                        title = stringResource(R.string.search_no_results),
                        subtitle = "Try adjusting your filters",
                        action = {
                            TextButton(onClick = { viewModel.clearFilters() }) {
                                Text(stringResource(R.string.filter_clear))
                            }
                        }
                    )
                }
                else -> {
                    if (uiState.viewMode == GalleryViewMode.GRID) {
                        MediaGrid(
                            mediaItems = uiState.filteredMedia,
                            onMediaClick = { viewModel.selectMedia(it) }
                        )
                    } else {
                        MediaList(
                            mediaItems = uiState.filteredMedia,
                            onMediaClick = { viewModel.selectMedia(it) }
                        )
                    }
                }
            }
        }
    }

    if (showMemberPicker) {
        MemberPickerDialog(
            members = uiState.members,
            selectedMemberId = uiState.selectedMemberId,
            onMemberSelected = {
                viewModel.setMemberFilter(it)
                showMemberPicker = false
            },
            onDismiss = { showMemberPicker = false }
        )
    }

    if (uiState.showMediaViewer && uiState.selectedMedia != null) {
        MediaViewerDialog(
            media = uiState.selectedMedia!!,
            memberName = uiState.mediaItems.find { it.media.id == uiState.selectedMedia?.id }?.member?.fullName,
            onDismiss = { viewModel.dismissMediaViewer() },
            onDelete = { showDeleteDialog = true }
        )
    }

    if (showDeleteDialog && uiState.selectedMedia != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Media") },
            text = { Text("Are you sure you want to delete this item? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMedia(uiState.selectedMedia!!.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddMediaSheet) {
        AddMediaBottomSheet(
            members = uiState.members,
            onMemberSelected = { member ->
                selectedMemberForAdd = member
                imagePickerLauncher.launch("*/*")
            },
            onDismiss = { showAddMediaSheet = false }
        )
    }
}

@Composable
private fun MediaGrid(
    mediaItems: List<MediaWithMember>,
    onMediaClick: (Media) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(mediaItems, key = { it.media.id }) { item ->
            MediaGridItem(
                mediaWithMember = item,
                onClick = { onMediaClick(item.media) }
            )
        }
    }
}

@Composable
private fun MediaGridItem(
    mediaWithMember: MediaWithMember,
    onClick: () -> Unit
) {
    val media = mediaWithMember.media

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        when (media.type) {
            MediaKind.PHOTO -> {
                AsyncImage(
                    model = File(media.thumbnailPath ?: media.filePath),
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = getMediaTypeIcon(media.type),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = media.title ?: media.filePath.substringAfterLast("/"),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(2.dp)
        ) {
            Icon(
                imageVector = getMediaTypeIcon(media.type),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MediaList(
    mediaItems: List<MediaWithMember>,
    onMediaClick: (Media) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(mediaItems, key = { it.media.id }) { item ->
            MediaListItem(
                mediaWithMember = item,
                onClick = { onMediaClick(item.media) }
            )
        }
    }
}

@Composable
private fun MediaListItem(
    mediaWithMember: MediaWithMember,
    onClick: () -> Unit
) {
    val media = mediaWithMember.media
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                when (media.type) {
                    MediaKind.PHOTO -> {
                        AsyncImage(
                            model = File(media.thumbnailPath ?: media.filePath),
                            contentDescription = media.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = getMediaTypeIcon(media.type),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title ?: media.filePath.substringAfterLast("/"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mediaWithMember.member.fullName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = media.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ${media.formattedSize}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    media.dateTaken?.let { date ->
                        Text(
                            text = " • ${dateFormat.format(Date(date))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberPickerDialog(
    members: List<FamilyMember>,
    selectedMemberId: Long?,
    onMemberSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Member") },
        text = {
            LazyColumn {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMemberSelected(null) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("All Members")
                    }
                }
                items(members) { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMemberSelected(member.id) }
                            .padding(vertical = 12.dp)
                            .background(
                                if (selectedMemberId == member.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(member.fullName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaViewerDialog(
    media: Media,
    memberName: String?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                when (media.type) {
                    MediaKind.PHOTO -> {
                        AsyncImage(
                            model = File(media.filePath),
                            contentDescription = media.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = getMediaTypeIcon(media.type),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = media.type.displayName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = media.title ?: media.filePath.substringAfterLast("/"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    memberName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    media.description?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Size: ${media.formattedSize}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMediaBottomSheet(
    members: List<FamilyMember>,
    onMemberSelected: (FamilyMember) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Add Media to Member",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Select a family member to add media",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(members) { member ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onMemberSelected(member) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = member.fullName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getMediaTypeIcon(type: MediaKind): ImageVector {
    return when (type) {
        MediaKind.PHOTO -> Icons.Default.Image
        MediaKind.VIDEO -> Icons.Default.VideoFile
        MediaKind.AUDIO -> Icons.Default.AudioFile
        MediaKind.DOCUMENT -> Icons.Default.Description
        MediaKind.OTHER -> Icons.Default.InsertDriveFile
    }
}
