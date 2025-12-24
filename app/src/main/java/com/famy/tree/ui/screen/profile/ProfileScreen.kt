package com.famy.tree.ui.screen.profile

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.famy.tree.R
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.model.RelationshipKind
import com.famy.tree.domain.model.RelationshipWithMember
import com.famy.tree.ui.component.BackButton
import com.famy.tree.ui.component.DeleteMemberDialog
import com.famy.tree.ui.component.GenderIndicator
import com.famy.tree.ui.component.LoadingScreen
import com.famy.tree.ui.component.MemberAvatar
import com.famy.tree.ui.component.SectionHeader
import com.famy.tree.ui.theme.FamyThemeExtensions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    memberId: Long,
    onNavigateBack: () -> Unit,
    onEditMember: (Long) -> Unit,
    onRelatedMemberClick: (Long) -> Unit,
    onAddRelationship: (RelationshipKind) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.updatePhoto(context, it) }
    }

    if (showDeleteDialog) {
        DeleteMemberDialog(
            memberName = uiState.member?.fullName ?: "",
            onConfirm = {
                viewModel.deleteMember { onNavigateBack() }
            },
            onDismiss = { viewModel.hideDeleteDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = { BackButton(onClick = onNavigateBack) },
                actions = {
                    IconButton(onClick = { onEditMember(viewModel.treeId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.showDeleteDialog()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingScreen(modifier = Modifier.padding(paddingValues))
        } else {
            uiState.member?.let { member ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        ProfileHeader(
                            member = member,
                            onPhotoClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }

                    item {
                        ProfileInfoSection(member = member)
                    }

                    if (uiState.parents.isNotEmpty()) {
                        item {
                            RelationshipSection(
                                title = "Parents",
                                relationships = uiState.parents,
                                onMemberClick = onRelatedMemberClick,
                                onRemove = { viewModel.removeRelationship(it) }
                            )
                        }
                    }

                    if (uiState.spouses.isNotEmpty()) {
                        item {
                            RelationshipSection(
                                title = "Spouses",
                                relationships = uiState.spouses,
                                onMemberClick = onRelatedMemberClick,
                                onRemove = { viewModel.removeRelationship(it) }
                            )
                        }
                    }

                    if (uiState.children.isNotEmpty()) {
                        item {
                            RelationshipSection(
                                title = "Children",
                                relationships = uiState.children,
                                onMemberClick = onRelatedMemberClick,
                                onRemove = { viewModel.removeRelationship(it) }
                            )
                        }
                    }

                    if (uiState.siblings.isNotEmpty()) {
                        item {
                            RelationshipSection(
                                title = "Siblings",
                                relationships = uiState.siblings,
                                onMemberClick = onRelatedMemberClick,
                                onRemove = { viewModel.removeRelationship(it) }
                            )
                        }
                    }

                    item {
                        AddRelationshipSection(
                            hasParents = uiState.parents.size < 2,
                            onAddRelationship = onAddRelationship
                        )
                    }

                    if (member.biography != null) {
                        item {
                            BiographySection(biography = member.biography)
                        }
                    }

                    if (uiState.lifeEvents.isNotEmpty()) {
                        item {
                            LifeEventsSection(events = uiState.lifeEvents)
                        }
                    }

                    if (member.customFields.isNotEmpty()) {
                        item {
                            CustomFieldsSection(customFields = member.customFields)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    member: FamilyMember,
    onPhotoClick: () -> Unit
) {
    val backgroundColor = FamyThemeExtensions.memberCardColor(member.gender)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onPhotoClick),
            contentAlignment = Alignment.Center
        ) {
            if (member.photoPath != null && File(member.photoPath).exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(member.photoPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.cd_member_photo),
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = member.fullName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        member.maidenName?.let { maiden ->
            Text(
                text = "née $maiden",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            GenderIndicator(gender = member.gender, size = 10.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (member.gender) {
                    Gender.MALE -> stringResource(R.string.gender_male)
                    Gender.FEMALE -> stringResource(R.string.gender_female)
                    Gender.OTHER -> stringResource(R.string.gender_other)
                    Gender.UNKNOWN -> stringResource(R.string.gender_unknown)
                },
                style = MaterialTheme.typography.bodyMedium
            )

            member.age?.let { age ->
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (member.isLiving) {
                        stringResource(R.string.years_old, age)
                    } else {
                        stringResource(R.string.lived_years, age)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!member.isLiving) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Deceased",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileInfoSection(member: FamilyMember) {
    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        member.birthDate?.let { birthDate ->
            InfoRow(
                icon = Icons.Default.Cake,
                label = stringResource(R.string.profile_birth_date),
                value = dateFormat.format(Date(birthDate))
            )
            member.birthPlace?.let { place ->
                InfoRow(
                    icon = Icons.Default.LocationOn,
                    label = stringResource(R.string.profile_birth_place),
                    value = place
                )
            }
        }

        if (!member.isLiving && member.deathDate != null) {
            InfoRow(
                icon = Icons.Default.CalendarMonth,
                label = stringResource(R.string.profile_death_date),
                value = dateFormat.format(Date(member.deathDate))
            )
            member.deathPlace?.let { place ->
                InfoRow(
                    icon = Icons.Default.LocationOn,
                    label = stringResource(R.string.profile_death_place),
                    value = place
                )
            }
        }

        member.occupation?.let { occupation ->
            InfoRow(
                icon = Icons.Default.Work,
                label = stringResource(R.string.profile_occupation),
                value = occupation
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RelationshipSection(
    title: String,
    relationships: List<RelationshipWithMember>,
    onMemberClick: (Long) -> Unit,
    onRemove: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(relationships) { relWithMember ->
                RelationshipCard(
                    member = relWithMember.relatedMember,
                    relationshipType = relWithMember.relationship.type,
                    onClick = { onMemberClick(relWithMember.relatedMember.id) }
                )
            }
        }
    }
}

@Composable
private fun RelationshipCard(
    member: FamilyMember,
    relationshipType: RelationshipKind,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MemberAvatar(member = member, size = 56.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = member.firstName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                text = relationshipType.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddRelationshipSection(
    hasParents: Boolean,
    onAddRelationship: (RelationshipKind) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Add Relationship",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasParents) {
                AssistChip(
                    onClick = { onAddRelationship(RelationshipKind.PARENT) },
                    label = { Text(stringResource(R.string.relationship_add_parent)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            AssistChip(
                onClick = { onAddRelationship(RelationshipKind.SPOUSE) },
                label = { Text(stringResource(R.string.relationship_add_spouse)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            AssistChip(
                onClick = { onAddRelationship(RelationshipKind.CHILD) },
                label = { Text(stringResource(R.string.relationship_add_child)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            AssistChip(
                onClick = { onAddRelationship(RelationshipKind.SIBLING) },
                label = { Text(stringResource(R.string.relationship_add_sibling)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun BiographySection(biography: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        SectionHeader(title = stringResource(R.string.profile_biography))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = biography,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LifeEventsSection(events: List<com.famy.tree.domain.model.LifeEvent>) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        SectionHeader(title = stringResource(R.string.profile_timeline))
        Spacer(modifier = Modifier.height(8.dp))
        events.forEach { event ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = event.eventDate?.let { dateFormat.format(Date(it)) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(80.dp)
                )
                Column {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    event.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomFieldsSection(customFields: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        SectionHeader(title = stringResource(R.string.profile_custom_fields))
        Spacer(modifier = Modifier.height(8.dp))
        customFields.forEach { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}
