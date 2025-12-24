# Continuity Ledger - Famy Family Tree App

## Goal (incl. success criteria)
Build a production-grade Android family tree app "Famy" from scratch with:
- Material 3 Jetpack Compose UI with Poppins fonts
- Offline-first local storage (Room DB)
- High-performance Canvas-based tree visualization (viewport culling, lazy loading)
- Complete feature set: Home Dashboard, Interactive Tree, Member Profiles, Relationships, Search, Timeline, Statistics, Media Gallery, Import/Export (GEDCOM/JSON), Settings
- Global crash handler with debug activity
- Onboarding flow
- Custom app icon with tree/F motif
- GitHub Actions CI/CD with signed APK
- Package: com.famy.tree

Success criteria: Fully functional, production-grade app that builds, runs, and handles large family trees smoothly.

## Constraints/Assumptions
- Kotlin + Jetpack Compose only
- Material 3 + Material Icons
- Poppins font family
- Offline-first (Room database)
- Performance critical: Canvas-based tree rendering with culling
- Modular code: 500-1000 lines per file max
- No TODOs or placeholder implementations
- Public keystore for open-source signing
- Min SDK 24, Target SDK 35

## Key Decisions
- Architecture: MVVM + Repository pattern + Clean Architecture layers
- DI: Hilt
- Database: Room with Flow for reactive updates
- Navigation: Compose Navigation with bottom nav + drawer
- Tree rendering: Custom Canvas composable with viewport culling
- State management: ViewModel + StateFlow
- Image loading: Coil

## State

### Done (Previous Session - ~30-35% complete)
- Project structure and Gradle configuration
- Data layer: 5 entities, 5 DAOs, database, converters
- Repository implementations (5 complete)
- Domain models (7), use cases (tree, member, statistics)
- UI: HomeScreen/ViewModel, TreeScreen/ViewModel (partial)
- Navigation routes and NavGraph structure
- CrashHandler + CrashActivity
- FamyApplication with Hilt + Coil
- AndroidManifest configuration
- Theme setup (Color, Type, Theme)
- Common components and dialogs (partial)
- Splash icon drawable

### Now
- Creating MainActivity
- Implementing TreeCanvas (high-performance Canvas-based visualization)
- Completing all missing screens

### Next (Priority Order)
1. MainActivity - app entry point
2. TreeCanvas - Canvas-based tree rendering with viewport culling
3. OnboardingScreen - first-time user flow
4. MembersScreen + MembersViewModel - member list
5. ProfileScreen + ProfileViewModel - member details
6. EditMemberScreen + EditMemberViewModel - create/edit members
7. AddRelationshipScreen - relationship editor
8. SearchScreen + SearchViewModel - search & filter
9. TimelineScreen + TimelineViewModel - chronological events
10. StatisticsScreen + StatisticsViewModel - family analytics
11. GalleryScreen + GalleryViewModel - media gallery
12. SettingsScreen + SettingsViewModel - app settings
13. Import/Export functionality
14. App icon generation (mipmap assets)
15. Keystore creation
16. GitHub Actions workflow

## Open Questions
- None currently

## Working Set
- Root: /workspace/repo-7a8c9db4-b146-4581-b426-cecb94a648e8
- Main package: app/src/main/java/com/famy/tree/
- Key files to create:
  - ui/activity/MainActivity.kt
  - ui/component/TreeCanvas.kt
  - ui/screen/onboarding/OnboardingScreen.kt
  - ui/screen/members/MembersScreen.kt + MembersViewModel.kt
  - ui/screen/profile/ProfileScreen.kt + ProfileViewModel.kt
  - ui/screen/editor/EditMemberScreen.kt + EditMemberViewModel.kt
  - ui/screen/relationship/AddRelationshipScreen.kt + AddRelationshipViewModel.kt
  - ui/screen/search/SearchScreen.kt + SearchViewModel.kt
  - ui/screen/timeline/TimelineScreen.kt + TimelineViewModel.kt
  - ui/screen/statistics/StatisticsScreen.kt + StatisticsViewModel.kt
  - ui/screen/gallery/GalleryScreen.kt + GalleryViewModel.kt
  - ui/screen/settings/SettingsScreen.kt + SettingsViewModel.kt
  - .github/workflows/android.yml
  - keystore/famy-release.jks
