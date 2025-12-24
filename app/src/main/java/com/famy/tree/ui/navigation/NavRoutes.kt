package com.famy.tree.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoutes(val route: String) {
    data object Onboarding : NavRoutes("onboarding")
    data object Home : NavRoutes("home")
    data object Tree : NavRoutes("tree/{treeId}") {
        fun createRoute(treeId: Long) = "tree/$treeId"
    }
    data object Members : NavRoutes("members/{treeId}") {
        fun createRoute(treeId: Long) = "members/$treeId"
    }
    data object Search : NavRoutes("search")
    data object Profile : NavRoutes("profile/{memberId}") {
        fun createRoute(memberId: Long) = "profile/$memberId"
    }
    data object EditMember : NavRoutes("edit_member/{treeId}?memberId={memberId}") {
        fun createRoute(treeId: Long, memberId: Long? = null): String {
            return if (memberId != null) {
                "edit_member/$treeId?memberId=$memberId"
            } else {
                "edit_member/$treeId"
            }
        }
    }
    data object AddRelationship : NavRoutes("add_relationship/{memberId}/{relationshipType}") {
        fun createRoute(memberId: Long, relationshipType: String) = "add_relationship/$memberId/$relationshipType"
    }
    data object Timeline : NavRoutes("timeline/{treeId}") {
        fun createRoute(treeId: Long) = "timeline/$treeId"
    }
    data object Statistics : NavRoutes("statistics/{treeId}") {
        fun createRoute(treeId: Long) = "statistics/$treeId"
    }
    data object Gallery : NavRoutes("gallery/{treeId}") {
        fun createRoute(treeId: Long) = "gallery/$treeId"
    }
    data object Settings : NavRoutes("settings")
    data object ImportExport : NavRoutes("import_export")
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

object BottomNavItems {
    fun getItems(treeId: Long?): List<BottomNavItem> {
        val safeTreeId = treeId ?: 0L
        return listOf(
            BottomNavItem(
                route = NavRoutes.Home.route,
                label = "Home",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home
            ),
            BottomNavItem(
                route = NavRoutes.Tree.createRoute(safeTreeId),
                label = "Tree",
                selectedIcon = Icons.Filled.AccountTree,
                unselectedIcon = Icons.Outlined.AccountTree
            ),
            BottomNavItem(
                route = NavRoutes.Members.createRoute(safeTreeId),
                label = "Members",
                selectedIcon = Icons.Filled.People,
                unselectedIcon = Icons.Outlined.People
            ),
            BottomNavItem(
                route = NavRoutes.Search.route,
                label = "Search",
                selectedIcon = Icons.Filled.Search,
                unselectedIcon = Icons.Outlined.Search
            )
        )
    }
}
