package com.famy.tree.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.famy.tree.ui.screen.editor.EditMemberScreen
import com.famy.tree.ui.screen.gallery.GalleryScreen
import com.famy.tree.ui.screen.home.HomeScreen
import com.famy.tree.ui.screen.members.MembersScreen
import com.famy.tree.ui.screen.onboarding.OnboardingScreen
import com.famy.tree.ui.screen.profile.ProfileScreen
import com.famy.tree.ui.screen.relationship.AddRelationshipScreen
import com.famy.tree.ui.screen.search.SearchScreen
import com.famy.tree.ui.screen.settings.SettingsScreen
import com.famy.tree.ui.screen.statistics.StatisticsScreen
import com.famy.tree.ui.screen.timeline.TimelineScreen
import com.famy.tree.ui.screen.tree.TreeScreen

@Composable
fun FamyNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    onTreeSelected: (Long?) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavRoutes.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Home.route) {
            HomeScreen(
                onTreeSelected = { treeId ->
                    onTreeSelected(treeId)
                    navController.navigate(NavRoutes.Tree.createRoute(treeId))
                },
                onCreateTree = { },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Settings.route)
                }
            )
        }

        composable(
            route = NavRoutes.Tree.route,
            arguments = listOf(
                navArgument("treeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val treeId = backStackEntry.arguments?.getLong("treeId") ?: 0L
            TreeScreen(
                treeId = treeId,
                onMemberClick = { memberId ->
                    navController.navigate(NavRoutes.Profile.createRoute(memberId))
                },
                onAddMember = {
                    navController.navigate(NavRoutes.EditMember.createRoute(treeId))
                },
                onNavigateToTimeline = {
                    navController.navigate(NavRoutes.Timeline.createRoute(treeId))
                },
                onNavigateToStatistics = {
                    navController.navigate(NavRoutes.Statistics.createRoute(treeId))
                },
                onNavigateToGallery = {
                    navController.navigate(NavRoutes.Gallery.createRoute(treeId))
                }
            )
        }

        composable(
            route = NavRoutes.Members.route,
            arguments = listOf(
                navArgument("treeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val treeId = backStackEntry.arguments?.getLong("treeId") ?: 0L
            MembersScreen(
                treeId = treeId,
                onMemberClick = { memberId ->
                    navController.navigate(NavRoutes.Profile.createRoute(memberId))
                },
                onAddMember = {
                    navController.navigate(NavRoutes.EditMember.createRoute(treeId))
                }
            )
        }

        composable(NavRoutes.Search.route) {
            SearchScreen(
                onMemberClick = { memberId ->
                    navController.navigate(NavRoutes.Profile.createRoute(memberId))
                }
            )
        }

        composable(
            route = NavRoutes.Profile.route,
            arguments = listOf(
                navArgument("memberId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getLong("memberId") ?: 0L
            ProfileScreen(
                memberId = memberId,
                onNavigateBack = { navController.popBackStack() },
                onEditMember = { treeId ->
                    navController.navigate(NavRoutes.EditMember.createRoute(treeId, memberId))
                },
                onRelatedMemberClick = { relatedMemberId ->
                    navController.navigate(NavRoutes.Profile.createRoute(relatedMemberId))
                },
                onAddRelationship = { type ->
                    navController.navigate(NavRoutes.AddRelationship.createRoute(memberId, type.name))
                }
            )
        }

        composable(
            route = NavRoutes.EditMember.route,
            arguments = listOf(
                navArgument("treeId") { type = NavType.LongType },
                navArgument("memberId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val treeId = backStackEntry.arguments?.getLong("treeId") ?: 0L
            val memberId = backStackEntry.arguments?.getLong("memberId")?.takeIf { it != -1L }
            EditMemberScreen(
                treeId = treeId,
                memberId = memberId,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { savedMemberId ->
                    navController.popBackStack()
                    if (memberId == null) {
                        navController.navigate(NavRoutes.Profile.createRoute(savedMemberId))
                    }
                }
            )
        }

        composable(
            route = NavRoutes.Timeline.route,
            arguments = listOf(
                navArgument("treeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val treeId = backStackEntry.arguments?.getLong("treeId") ?: 0L
            TimelineScreen(
                treeId = treeId,
                onNavigateBack = { navController.popBackStack() },
                onMemberClick = { memberId ->
                    navController.navigate(NavRoutes.Profile.createRoute(memberId))
                }
            )
        }

        composable(
            route = NavRoutes.Statistics.route,
            arguments = listOf(
                navArgument("treeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val treeId = backStackEntry.arguments?.getLong("treeId") ?: 0L
            StatisticsScreen(
                treeId = treeId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.Gallery.route,
            arguments = listOf(
                navArgument("treeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val treeId = backStackEntry.arguments?.getLong("treeId") ?: 0L
            GalleryScreen(
                treeId = treeId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.AddRelationship.route,
            arguments = listOf(
                navArgument("memberId") { type = NavType.LongType },
                navArgument("relationshipType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getLong("memberId") ?: 0L
            val relationshipType = backStackEntry.arguments?.getString("relationshipType") ?: "PARENT"
            AddRelationshipScreen(
                memberId = memberId,
                relationshipType = relationshipType,
                onNavigateBack = { navController.popBackStack() },
                onCreateNewMember = {
                    navController.popBackStack()
                    val treeId = navController.currentBackStackEntry?.arguments?.getLong("treeId") ?: 0L
                    navController.navigate(NavRoutes.EditMember.createRoute(treeId))
                }
            )
        }
    }
}
