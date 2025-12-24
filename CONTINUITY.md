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
- Min SDK ~24, Target SDK 34

## Key Decisions
- Architecture: MVVM + Repository pattern + Clean Architecture layers
- DI: Hilt
- Database: Room with Flow for reactive updates
- Navigation: Compose Navigation with bottom nav + drawer
- Tree rendering: Custom Canvas composable with viewport culling
- State management: ViewModel + StateFlow
- Image loading: Coil

## State

### Done
- (Starting fresh)

### Now
- Creating project structure and Gradle configuration

### Next
- Implement data layer (entities, DAOs, database)
- Implement domain layer (models, repositories, use cases)
- Implement UI layer (screens, components, navigation)
- Add crash handler and debug activity
- Create onboarding flow
- Generate app icon assets
- Set up GitHub Actions workflow
- Create keystore

## Open Questions
- None currently

## Working Set
- Root: /workspace/repo-fed96474-be42-468c-8a67-3b080ff111b7
- Main package: app/src/main/java/com/famy/tree/
