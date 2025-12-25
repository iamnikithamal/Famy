package com.famy.tree.ui.screen.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.famy.tree.R
import com.famy.tree.domain.model.CareerStatus
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.model.GeocodedLocation
import com.famy.tree.domain.model.RelationshipStatus
import com.famy.tree.ui.component.BackButton
import com.famy.tree.ui.component.LoadingOverlay
import com.famy.tree.ui.component.LoadingScreen
import com.famy.tree.ui.component.SectionHeader
import com.famy.tree.ui.component.UnsavedChangesDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditMemberScreen(
    treeId: Long,
    memberId: Long?,
    onNavigateBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: EditMemberViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showBirthDatePicker by remember { mutableStateOf(false) }
    var showDeathDatePicker by remember { mutableStateOf(false) }
    var showAddCustomFieldDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.updatePhoto(context, it) }
    }

    BackHandler(enabled = uiState.hasChanges) {
        showUnsavedChangesDialog = true
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    if (showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onDiscard = onNavigateBack,
            onDismiss = { showUnsavedChangesDialog = false }
        )
    }

    if (showBirthDatePicker) {
        DatePickerDialog(
            initialDate = uiState.form.birthDate,
            onDateSelected = { viewModel.updateBirthDate(it) },
            onDismiss = { showBirthDatePicker = false }
        )
    }

    if (showDeathDatePicker) {
        DatePickerDialog(
            initialDate = uiState.form.deathDate,
            onDateSelected = { viewModel.updateDeathDate(it) },
            onDismiss = { showDeathDatePicker = false }
        )
    }

    if (showAddCustomFieldDialog) {
        AddCustomFieldDialog(
            onAdd = { key, value ->
                viewModel.addCustomField(key, value)
                showAddCustomFieldDialog = false
            },
            onDismiss = { showAddCustomFieldDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditing) stringResource(R.string.action_edit)
                        else stringResource(R.string.cd_add_member)
                    )
                },
                navigationIcon = {
                    BackButton(onClick = {
                        if (uiState.hasChanges) {
                            showUnsavedChangesDialog = true
                        } else {
                            onNavigateBack()
                        }
                    })
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save(onSaved) },
                        enabled = !uiState.isSaving && uiState.validationErrors.isEmpty()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.action_save))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        PhotoSection(
                            photoPath = uiState.form.photoPath,
                            onPhotoClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }

                    item {
                        SectionHeader(title = "Basic Information")
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.form.firstName,
                                onValueChange = viewModel::updateFirstName,
                                label = { Text(stringResource(R.string.profile_first_name) + " *") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                isError = uiState.validationErrors.containsKey("firstName"),
                                supportingText = uiState.validationErrors["firstName"]?.let { { Text(it) } },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                )
                            )
                            OutlinedTextField(
                                value = uiState.form.middleName,
                                onValueChange = viewModel::updateMiddleName,
                                label = { Text("Middle Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                )
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = uiState.form.lastName,
                            onValueChange = viewModel::updateLastName,
                            label = { Text(stringResource(R.string.profile_last_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.form.maidenName,
                                onValueChange = viewModel::updateMaidenName,
                                label = { Text(stringResource(R.string.profile_maiden_name)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                )
                            )
                            OutlinedTextField(
                                value = uiState.form.nickname,
                                onValueChange = viewModel::updateNickname,
                                label = { Text(stringResource(R.string.profile_nickname)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                )
                            )
                        }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.profile_gender),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Gender.entries.forEach { gender ->
                                FilterChip(
                                    selected = uiState.form.gender == gender,
                                    onClick = { viewModel.updateGender(gender) },
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
                    }

                    item {
                        SectionHeader(title = "Dates & Places")
                    }

                    item {
                        DatePickerField(
                            label = stringResource(R.string.profile_birth_date),
                            date = uiState.form.birthDate,
                            error = uiState.validationErrors["birthDate"],
                            onClick = { showBirthDatePicker = true },
                            onClear = { viewModel.updateBirthDate(null) }
                        )
                    }

                    item {
                        LocationSearchField(
                            value = uiState.form.birthPlace,
                            onValueChange = { value ->
                                viewModel.updateBirthPlace(value)
                                viewModel.searchBirthPlace(value)
                            },
                            label = stringResource(R.string.profile_birth_place),
                            searchResults = uiState.birthPlaceSearchResults,
                            isSearching = uiState.isSearchingBirthPlace,
                            onLocationSelected = viewModel::selectBirthPlace,
                            onDismissResults = viewModel::clearBirthPlaceSearch,
                            hasCoordinates = uiState.form.birthPlaceLatitude != null
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.profile_is_living),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = uiState.form.isLiving,
                                onCheckedChange = viewModel::updateIsLiving
                            )
                        }
                    }

                    if (!uiState.form.isLiving) {
                        item {
                            DatePickerField(
                                label = stringResource(R.string.profile_death_date),
                                date = uiState.form.deathDate,
                                error = uiState.validationErrors["deathDate"],
                                onClick = { showDeathDatePicker = true },
                                onClear = { viewModel.updateDeathDate(null) }
                            )
                        }

                        item {
                            LocationSearchField(
                                value = uiState.form.deathPlace,
                                onValueChange = { value ->
                                    viewModel.updateDeathPlace(value)
                                    viewModel.searchDeathPlace(value)
                                },
                                label = stringResource(R.string.profile_death_place),
                                searchResults = uiState.deathPlaceSearchResults,
                                isSearching = uiState.isSearchingDeathPlace,
                                onLocationSelected = viewModel::selectDeathPlace,
                                onDismissResults = viewModel::clearDeathPlaceSearch,
                                hasCoordinates = uiState.form.deathPlaceLatitude != null
                            )
                        }
                    }

                    item {
                        SectionHeader(title = "Additional Information")
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.form.occupation,
                                onValueChange = viewModel::updateOccupation,
                                label = { Text(stringResource(R.string.profile_occupation)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                )
                            )
                            OutlinedTextField(
                                value = uiState.form.education,
                                onValueChange = viewModel::updateEducation,
                                label = { Text("Education") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                )
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusDropdown(
                                label = "Career Status",
                                selectedValue = uiState.form.careerStatus.displayName,
                                options = CareerStatus.entries.map { it.displayName },
                                onOptionSelected = { displayName ->
                                    val status = CareerStatus.entries.find { it.displayName == displayName }
                                        ?: CareerStatus.UNKNOWN
                                    viewModel.updateCareerStatus(status)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StatusDropdown(
                                label = "Relationship",
                                selectedValue = uiState.form.relationshipStatus.displayName,
                                options = RelationshipStatus.entries.map { it.displayName },
                                onOptionSelected = { displayName ->
                                    val status = RelationshipStatus.entries.find { it.displayName == displayName }
                                        ?: RelationshipStatus.UNKNOWN
                                    viewModel.updateRelationshipStatus(status)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        InterestsField(
                            interests = uiState.form.interests,
                            onAddInterest = viewModel::addInterest,
                            onRemoveInterest = viewModel::removeInterest
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = uiState.form.biography,
                            onValueChange = viewModel::updateBiography,
                            label = { Text(stringResource(R.string.profile_biography)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = uiState.form.notes,
                            onValueChange = viewModel::updateNotes,
                            label = { Text(stringResource(R.string.profile_notes)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences
                            )
                        )
                    }

                    if (uiState.form.customFields.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.profile_custom_fields),
                                action = {
                                    IconButton(onClick = { showAddCustomFieldDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                    }
                                }
                            )
                        }

                        uiState.form.customFields.forEach { (key, value) ->
                            item {
                                CustomFieldItem(
                                    key = key,
                                    value = value,
                                    onRemove = { viewModel.removeCustomField(key) }
                                )
                            }
                        }
                    } else {
                        item {
                            TextButton(onClick = { showAddCustomFieldDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Custom Field")
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }

            LoadingOverlay(isLoading = uiState.isSaving)
        }
    }
}

@Composable
private fun PhotoSection(
    photoPath: String?,
    onPhotoClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onPhotoClick),
            contentAlignment = Alignment.Center
        ) {
            if (photoPath != null && File(photoPath).exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(photoPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = stringResource(R.string.profile_add_photo),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun DatePickerField(
    label: String,
    date: Long?,
    error: String?,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    OutlinedTextField(
        value = date?.let { dateFormat.format(Date(it)) } ?: "",
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        readOnly = true,
        enabled = false,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        trailingIcon = {
            Row {
                if (date != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.filter_clear))
                    }
                }
                IconButton(onClick = onClick) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initialDate: Long?,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun CustomFieldItem(
    key: String,
    value: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}

@Composable
private fun AddCustomFieldDialog(
    onAdd: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Field") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Field Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(key.trim(), value.trim()) },
                enabled = key.isNotBlank() && value.isNotBlank()
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    searchResults: List<GeocodedLocation>,
    isSearching: Boolean,
    onLocationSelected: (GeocodedLocation) -> Unit,
    onDismissResults: () -> Unit,
    hasCoordinates: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded && searchResults.isNotEmpty(),
            onExpandedChange = { }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    onValueChange(newValue)
                    expanded = true
                },
                label = { Text(label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (hasCoordinates) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                supportingText = if (hasCoordinates) {
                    { Text("Location coordinates saved", color = MaterialTheme.colorScheme.primary) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                )
            )

            ExposedDropdownMenu(
                expanded = expanded && searchResults.isNotEmpty(),
                onDismissRequest = {
                    expanded = false
                    onDismissResults()
                }
            ) {
                searchResults.forEach { location ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = location.displayName,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            onLocationSelected(location)
                            expanded = false
                            focusManager.clearFocus()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterestsField(
    interests: List<String>,
    onAddInterest: (String) -> Unit,
    onRemoveInterest: (String) -> Unit
) {
    var newInterest by remember { mutableStateOf("") }
    var showPresets by remember { mutableStateOf(false) }

    val presetInterests = listOf(
        "Reading", "Music", "Sports", "Cooking", "Travel",
        "Photography", "Gardening", "Art", "Movies", "Gaming",
        "Hiking", "Fishing", "Dancing", "Writing", "Crafts",
        "Yoga", "Chess", "History", "Science", "Nature"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Interests & Hobbies",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (interests.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                interests.forEach { interest ->
                    InputChip(
                        selected = false,
                        onClick = { onRemoveInterest(interest) },
                        label = { Text(interest) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newInterest,
                onValueChange = { newInterest = it },
                label = { Text("Add interest") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                )
            )
            IconButton(
                onClick = {
                    if (newInterest.isNotBlank()) {
                        onAddInterest(newInterest)
                        newInterest = ""
                    }
                },
                enabled = newInterest.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { showPresets = !showPresets }) {
            Text(if (showPresets) "Hide presets" else "Show preset interests")
        }

        if (showPresets) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presetInterests
                    .filter { it !in interests }
                    .forEach { preset ->
                        AssistChip(
                            onClick = { onAddInterest(preset) },
                            label = { Text(preset) }
                        )
                    }
            }
        }
    }
}
