package com.famy.tree.ui.screen.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.famy.tree.R
import com.famy.tree.ui.component.BackButton
import com.famy.tree.ui.component.EmptyState
import com.famy.tree.ui.component.LoadingScreen
import com.famy.tree.ui.component.SectionHeader
import java.text.DateFormatSymbols
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    treeId: Long,
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_statistics)) },
                navigationIcon = { BackButton(onClick = onNavigateBack) }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            }
            uiState.totalMembers == 0 -> {
                EmptyState(
                    icon = Icons.Default.Analytics,
                    title = stringResource(R.string.no_data),
                    subtitle = "Add family members to see statistics",
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                StatisticsContent(
                    uiState = uiState,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    uiState: StatisticsUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(title = stringResource(R.string.stats_overview))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.stats_total_members),
                    value = uiState.totalMembers.toString(),
                    icon = Icons.Default.Groups,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.stats_generations),
                    value = uiState.generations.toString(),
                    icon = Icons.Default.Timeline,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.stats_living),
                    value = uiState.livingMembers.toString(),
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    title = stringResource(R.string.stats_deceased),
                    value = uiState.deceasedMembers.toString(),
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            GenderBreakdownCard(
                maleCount = uiState.maleCount,
                femaleCount = uiState.femaleCount,
                otherCount = uiState.otherCount + uiState.unknownGenderCount
            )
        }

        uiState.averageLifespan?.let { lifespan ->
            item {
                StatCard(
                    title = stringResource(R.string.stats_avg_lifespan),
                    value = "${lifespan.roundToInt()} years",
                    icon = Icons.Default.Cake,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        uiState.averageChildrenPerPerson?.let { avg ->
            item {
                StatCard(
                    title = stringResource(R.string.stats_avg_children),
                    value = String.format("%.1f", avg),
                    icon = Icons.Default.Groups,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (uiState.generationBreakdown.isNotEmpty()) {
            item {
                SectionHeader(title = "Generation Breakdown")
            }

            item {
                GenerationChart(
                    breakdown = uiState.generationBreakdown,
                    totalMembers = uiState.totalMembers
                )
            }
        }

        if (uiState.birthsByMonth.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.stats_birth_months))
            }

            item {
                BirthsByMonthChart(birthsByMonth = uiState.birthsByMonth)
            }
        }

        if (uiState.mostCommonFirstNames.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.stats_common_names))
            }

            item {
                NamesList(names = uiState.mostCommonFirstNames)
            }
        }

        if (uiState.mostCommonLastNames.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.stats_common_surnames))
            }

            item {
                NamesList(names = uiState.mostCommonLastNames)
            }
        }

        if (uiState.longestLived != null || uiState.oldestLiving != null) {
            item {
                SectionHeader(title = stringResource(R.string.stats_longevity))
            }

            uiState.oldestLiving?.let { member ->
                item {
                    LongevityCard(
                        title = "Oldest Living",
                        member = member,
                        value = member.age?.let { "$it years old" } ?: "Unknown"
                    )
                }
            }

            uiState.longestLived?.let { member ->
                item {
                    LongevityCard(
                        title = "Longest Lived",
                        member = member,
                        value = member.lifespan?.let { "Lived $it years" } ?: "Unknown"
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GenderBreakdownCard(
    maleCount: Int,
    femaleCount: Int,
    otherCount: Int
) {
    val total = maleCount + femaleCount + otherCount
    if (total == 0) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Gender Distribution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))

            val maleColor = Color(0xFF2196F3)
            val femaleColor = Color(0xFFE91E63)
            val otherColor = Color(0xFF9E9E9E)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (maleCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(maleCount.toFloat())
                            .fillMaxSize()
                            .background(maleColor)
                    )
                }
                if (femaleCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(femaleCount.toFloat())
                            .fillMaxSize()
                            .background(femaleColor)
                    )
                }
                if (otherCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(otherCount.toFloat())
                            .fillMaxSize()
                            .background(otherColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GenderLegendItem(
                    icon = Icons.Default.Male,
                    label = stringResource(R.string.stats_male),
                    count = maleCount,
                    color = maleColor
                )
                GenderLegendItem(
                    icon = Icons.Default.Female,
                    label = stringResource(R.string.stats_female),
                    count = femaleCount,
                    color = femaleColor
                )
                if (otherCount > 0) {
                    GenderLegendItem(
                        icon = Icons.Default.Person,
                        label = "Other",
                        count = otherCount,
                        color = otherColor
                    )
                }
            }
        }
    }
}

@Composable
private fun GenderLegendItem(
    icon: ImageVector,
    label: String,
    count: Int,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun GenerationChart(
    breakdown: Map<Int, Int>,
    totalMembers: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            breakdown.forEach { (generation, count) ->
                val percentage = (count.toFloat() / totalMembers) * 100
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gen ${generation}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(48.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percentage / 100f)
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BirthsByMonthChart(
    birthsByMonth: Map<Int, Int>
) {
    val maxCount = birthsByMonth.values.maxOrNull() ?: 1
    val months = DateFormatSymbols().shortMonths.take(12)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(100.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            months.forEachIndexed { index, month ->
                val count = birthsByMonth[index] ?: 0
                val heightFraction = if (maxCount > 0) count.toFloat() / maxCount else 0f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height((heightFraction * 60).dp.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = month.take(1),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun NamesList(names: List<NameCount>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(names) { nameCount ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = nameCount.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${nameCount.count}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun LongevityCard(
    title: String,
    member: com.famy.tree.domain.model.FamilyMember,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = member.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
