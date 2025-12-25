package com.famy.tree.ui.screen.relationship

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.famy.tree.R
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Gender
import com.famy.tree.ui.component.BackButton
import com.famy.tree.ui.component.EmptyState
import com.famy.tree.ui.component.LoadingScreen
import com.famy.tree.ui.component.SearchBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRelationshipScreen(
    memberId: Long,
    relationshipType: String,
    onNavigateBack: () -> Unit,
    onCreateNewMember: () -> Unit,
    viewModel: AddRelationshipViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Add ${viewModel.getRelationshipTypeDisplayName()}")
                        uiState.currentMember?.let { member ->
                            Text(
                                text = "for ${member.fullName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = { BackButton(onClick = onNavigateBack) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            }
            uiState.error != null && uiState.currentMember == null -> {
                EmptyState(
                    icon = Icons.Default.Person,
                    title = "Error",
                    subtitle = uiState.error ?: "Something went wrong",
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
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

                    if (viewModel.shouldShowDateField() || viewModel.shouldShowPlaceField()) {
                        RelationshipDetailsSection(
                            dateLabel = viewModel.getDateFieldLabel(),
                            placeLabel = viewModel.getPlaceFieldLabel(),
                            startDate = uiState.startDate,
                            startPlace = uiState.startPlace,
                            notes = uiState.notes,
                            onDateClick = { showDatePicker = true },
                            onPlaceChange = viewModel::updateStartPlace,
                            onNotesChange = viewModel::updateNotes,
                            showDate = viewModel.shouldShowDateField(),
                            showPlace = viewModel.shouldShowPlaceField()
                        )
                    }

                    Text(
                        text = "Select ${viewModel.getRelationshipTypeDisplayName()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.showCreateNewOption) {
                            item {
                                CreateNewMemberCard(
                                    relationshipType = viewModel.getRelationshipTypeDisplayName(),
                                    onClick = onCreateNewMember
                                )
                            }
                        }

                        if (uiState.filteredMembers.isEmpty() && uiState.searchQuery.isNotBlank()) {
                            item {
                                EmptyState(
                                    icon = Icons.Default.Person,
                                    title = stringResource(R.string.search_no_results),
                                    subtitle = "No family members match your search"
                                )
                            }
                        } else if (uiState.filteredMembers.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Default.Person,
                                    title = "No available members",
                                    subtitle = "All family members already have this relationship"
                                )
                            }
                        } else {
                            items(
                                items = uiState.filteredMembers,
                                key = { it.id }
                            ) { member ->
                                MemberSelectionCard(
                                    member = member,
                                    isSelected = uiState.selectedMemberId == member.id,
                                    onClick = { viewModel.selectMember(member.id) }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }

                    SaveButton(
                        enabled = uiState.selectedMemberId != null && !uiState.isSaving,
                        isLoading = uiState.isSaving,
                        onClick = { viewModel.saveRelationship(onNavigateBack) },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.startDate ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateStartDate(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun RelationshipDetailsSection(
    dateLabel: String,
    placeLabel: String,
    startDate: Long?,
    startPlace: String,
    notes: String,
    onDateClick: () -> Unit,
    onPlaceChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    showDate: Boolean,
    showPlace: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Relationship Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            if (showDate) {
                OutlinedTextField(
                    value = startDate?.let { dateFormat.format(Date(it)) } ?: "",
                    onValueChange = { },
                    label = { Text(dateLabel) },
                    readOnly = true,
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = onDateClick) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDateClick)
                )
            }

            if (showPlace) {
                OutlinedTextField(
                    value = startPlace,
                    onValueChange = onPlaceChange,
                    label = { Text(placeLabel) },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null)
                },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CreateNewMemberCard(
    relationshipType: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Create New $relationshipType",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Add a new family member and link as $relationshipType",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MemberSelectionCard(
    member: FamilyMember,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (member.gender) {
                            Gender.MALE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            Gender.FEMALE -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (member.gender) {
                        Gender.MALE -> Icons.Default.Male
                        Gender.FEMALE -> Icons.Default.Female
                        else -> Icons.Default.Person
                    },
                    contentDescription = null,
                    tint = when (member.gender) {
                        Gender.MALE -> MaterialTheme.colorScheme.primary
                        Gender.FEMALE -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    member.age?.let { age ->
                        Text(
                            text = if (member.isLiving) "$age years old" else "Lived $age years",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (member.generation > 0) {
                        Text(
                            text = "Gen ${member.generation}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(if (isLoading) "Saving..." else "Save Relationship")
    }
}
