package com.famy.tree.ui.screen.timeline

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.famy.tree.R
import com.famy.tree.ui.component.BackButton
import com.famy.tree.ui.component.EmptyState
import com.famy.tree.ui.component.LoadingScreen
import com.famy.tree.ui.component.MemberAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimelineScreen(
    treeId: Long,
    onNavigateBack: () -> Unit,
    onMemberClick: (Long) -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_timeline)) },
                navigationIcon = { BackButton(onClick = onNavigateBack) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimelineFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    TimelineFilter.ALL -> stringResource(R.string.timeline_all_events)
                                    TimelineFilter.BIRTHS -> stringResource(R.string.timeline_births)
                                    TimelineFilter.DEATHS -> stringResource(R.string.timeline_deaths)
                                    TimelineFilter.MARRIAGES -> stringResource(R.string.timeline_marriages)
                                }
                            )
                        }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    LoadingScreen()
                }
                uiState.events.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Timeline,
                        title = "No events yet",
                        subtitle = "Add dates to your family members to see their timeline"
                    )
                }
                else -> {
                    TimelineContent(
                        groupedEvents = uiState.groupedEvents,
                        onMemberClick = onMemberClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineContent(
    groupedEvents: Map<Int, List<TimelineEvent>>,
    onMemberClick: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 88.dp
        )
    ) {
        groupedEvents.forEach { (year, events) ->
            item(key = "year_$year") {
                YearHeader(year = year)
            }

            items(
                items = events,
                key = { it.id }
            ) { event ->
                TimelineEventItem(
                    event = event,
                    onClick = { onMemberClick(event.memberId) },
                    isLast = event == events.last()
                )
            }
        }
    }
}

@Composable
private fun YearHeader(year: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = year.toString().takeLast(2),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimelineEventItem(
    event: TimelineEvent,
    onClick: () -> Unit,
    isLast: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    val (iconColor, iconBackgroundColor) = when (event.type) {
        TimelineEventType.BIRTH -> MaterialTheme.colorScheme.primary to
            MaterialTheme.colorScheme.primaryContainer
        TimelineEventType.DEATH -> MaterialTheme.colorScheme.onSurfaceVariant to
            MaterialTheme.colorScheme.surfaceVariant
        TimelineEventType.MARRIAGE -> Color(0xFFE91E63) to Color(0xFFFCE4EC)
        TimelineEventType.CUSTOM -> MaterialTheme.colorScheme.tertiary to
            MaterialTheme.colorScheme.tertiaryContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(iconColor)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = 16.dp)
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (event.type) {
                            TimelineEventType.BIRTH -> Icons.Default.Cake
                            TimelineEventType.DEATH -> Icons.Default.Event
                            TimelineEventType.MARRIAGE -> Icons.Default.Favorite
                            TimelineEventType.CUSTOM -> Icons.Default.Event
                        },
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateFormat.format(Date(event.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    event.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    event.place?.let { place ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = place,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                MemberAvatar(
                    member = null,
                    size = 36.dp,
                    showBorder = false
                )
            }
        }
    }
}
