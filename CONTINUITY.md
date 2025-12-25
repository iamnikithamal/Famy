# Continuity Ledger - Famy Family Tree App Enhancement

## Goal (incl. success criteria)
Major enhancement of the Famy Android app focusing on:
1. **Performance Optimization** - Buttery smooth tree rendering with 100s of members via virtualization/lazy loading
2. **Enhanced UI/UX** - Modern, minimal, professional, responsive design throughout
3. **Enhanced Member Addition** - More fields (middle name, education, interests, career status, relationship status, etc.) with presets
4. **Map Integration** - Free API integration (Nominatim/Photon) for location selection with coordinate storage
5. **Tree Canvas Improvements** - Profile pictures, better node UI, functional features
6. **Lint Fixes** - Fix all deprecation warnings and API level issues
7. **GitHub Workflow Cleanup** - Remove debug APK build, keep only release APK + lint
8. **Create idea.md** - 15+ innovative feature ideas for future development

Success criteria: Production-grade app with zero lint errors, smooth performance on low-end devices, professional UI/UX.

## Constraints/Assumptions
- Kotlin + Jetpack Compose only (Material 3)
- Offline-first (Room database, no cloud/auth/external DB)
- No paid APIs - use free alternatives (Nominatim, Photon for geocoding)
- Modular code: 500-1000 lines per file max
- No TODOs or placeholder implementations
- Min SDK 24, Target SDK 35
- All changes must be production-grade, fully functional

## Key Decisions
- **Map Integration**: Use Nominatim (free OSM geocoding) + Photon (fast free geocoding) as dual providers with fallback
- **Tree Rendering**: Canvas-based with viewport culling (already implemented), enhance with profile photo support
- **Model Enhancement**: Add middleName, education, interests, careerStatus, relationshipStatus, locations with coordinates
- **Lint Fixes**: Use AutoMirrored icons, BasicAlertDialog, enable edge-to-edge, fix API 28 check for longVersionCode

## State

### Done (Previous Session)
- Complete app structure with MVVM + Clean Architecture
- All screens implemented: Home, Tree, Members, Profile, Editor, Relationships, Gallery, Timeline, Statistics, Settings, Search, Onboarding
- Room database with 5 entities
- Tree visualization with Canvas (needs enhancement)
- GitHub Actions workflow (needs cleanup)

### Now
- Starting comprehensive enhancement task

### Next (Priority Order)
1. Fix all lint errors (deprecations + API level issues)
2. Cleanup GitHub workflow file
3. Enhance FamilyMember model + Entity with new fields
4. Create location picker with map integration (Nominatim + Photon APIs)
5. Enhance EditMemberScreen with new fields (education, interests, career/relationship status)
6. Improve TreeCanvas with profile pictures, better node design
7. Performance optimization throughout
8. Create idea.md with 15+ innovative ideas

## Open Questions
- None currently

## Working Set
- Root: /workspace/repo-9aa8ef24-e558-4bfe-a0ef-123cab716824
- Main package: app/src/main/java/com/famy/tree/
- Files to fix (lint errors):
  - CommonComponents.kt:388 - Icons.Filled.ArrowBack → AutoMirrored
  - GalleryScreen.kt:157 - Icons.Filled.ViewList → AutoMirrored
  - GalleryScreen.kt:629 - AlertDialog → BasicAlertDialog
  - GalleryScreen.kt:805 - Icons.Filled.InsertDriveFile → AutoMirrored
  - MembersScreen.kt:104 - Icons.Filled.Sort → AutoMirrored
  - MembersViewModel.kt:79 - Unchecked cast fix
  - ProfileViewModel.kt:70-72 - Unchecked cast fixes (3 instances)
  - AddRelationshipScreen.kt:321 - Icons.Filled.Notes → AutoMirrored
  - Theme.kt:102-103 - statusBarColor/navigationBarColor deprecation
  - CrashHandler.kt:77 - longVersionCode API level check
- .github/workflows/android.yml - Remove debug build, keystore upload
