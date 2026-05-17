<!-- Context: project-intelligence/notes | Priority: high | Version: 2.0 | Updated: 2026-05-17 -->

# Living Notes

> Active issues, technical debt, open questions, and insights.

## Quick Reference
- **Purpose**: Capture current state, problems, and open questions
- **Update**: When status changes or new issues discovered

## Technical Debt
| Item | Impact | Priority | Mitigation |
|------|--------|----------|------------|
| RecordingService.java (6995 lines) | Hard to maintain, test, and review | High | Gradual refactoring into smaller service components |
| MainActivity.java (2149 lines) | God activity anti-pattern | Medium | Extract logic into dedicated controllers |
| Mixed Camera2 + CameraX usage | Complexity in camera lifecycle management | Medium | Clear separation of concerns between the two APIs |
| No unit test coverage for core recording | Regression risk on changes | High | Add tests for RecordingService, FragmentedMp4Muxer |
| Constants.java (704 lines of prefs keys) | Hard to manage, easy to introduce typos | Low | Consider generating from a schema or using typed prefs |

### Technical Debt Details
**RecordingService.java (6995 lines)**
*Priority*: High
*Impact*: Single file handles camera setup, recording, notifications, geotagging, watermarks, dual camera, auto-split — makes bug fixes risky
*Root Cause*: Organic growth of features into one service
*Proposed Solution*: Extract into modular components: CameraController, RecordingPipeline, NotificationManager, StorageManager
*Status*: Acknowledged

**MainActivity.java (2149 lines)**
*Priority*: Medium
*Impact*: Handles navigation, swipe gestures, cloak overlay, theme management, fragment lifecycle, back press logic
*Root Cause*: Shell activity accumulated too many responsibilities
*Proposed Solution*: Extract gesture handling, theme management, and cloak logic into separate classes
*Status*: Acknowledged

## Open Questions
| Question | Stakeholders | Status | Next Action |
|----------|--------------|--------|-------------|
| iOS/Desktop version architecture | anonfaded | Open | Define cross-platform strategy |
| Scheduled recording implementation | anonfaded | Open | Design WorkManager-based scheduler |
| Faditor Mini video editor scope | anonfaded | Open | Define MVP feature set |

## Known Issues
| Issue | Severity | Workaround | Status |
|-------|----------|------------|--------|
| Device-specific camera quirks on some OEMs | Medium | Device-specific helpers (SamsungFrameRateHelper) | Known — handled case by case |
| Edge-to-edge enforcement on Android 16 | Medium | Update UI for edge-to-edge | In Progress — targetSdk 36 |
| 16KB page alignment for native libs | Low | useLegacyPackaging = false | Fixed |

## Insights & Lessons Learned
### What Works Well
- **FLog redaction** — auto-scrubs URLs, IPs, tokens, file paths from logs. Prevents accidental data leaks in user-facing log viewer
- **Fragmented MP4** — zero corruption reports since implementation. Each segment is independently playable
- **Build variants for monetization** — pro, proPlus, notesPro, calcPro, weatherPro allow standalone installations without conflicts
- **ABI splits** — smaller APK sizes for users, universal APK for compatibility

### What Could Be Better
- **RecordingService size** — 6995 lines in one file makes it hard to onboard contributors
- **No CI/CD pipeline visible** — manual build process via `build.sh` script
- **Test coverage** — JUnit + Mockito configured but limited test files exist

### Lessons Learned
- **Camera2 is complex but necessary** — background recording requires low-level camera control that CameraX can't provide
- **Privacy-first is a differentiator** — being featured on 50+ blogs and privacy forums proves market demand
- **Open-source builds trust** — F-Droid presence and full source availability drives adoption

## Patterns & Conventions
### Code Patterns Worth Preserving
- **Thread-safe singleton** — `synchronized` double-checked locking for DB instances (VideoIndexDatabase, ForensicsDatabase)
- **SharedPreferencesManager singleton** — centralized prefs access across the entire app
- **FLog redaction** — privacy-aware logging that auto-scrubs sensitive data
- **Enum-based state management** — RecordingState, CameraType, VideoCodec as enums w/ Serializable

### Gotchas for Maintainers
- **Camera2 requires careful lifecycle management** — camera must be released before re-opening, especially during orientation changes
- **Fragmented MP4 playback needs custom seek logic** — standard ExoPlayer can't seek in fMP4 without index building
- **ProGuard rules are critical** — minification is enabled for all release builds; missing rules cause runtime crashes
- **ABI splits affect testing** — test on both arm64-v8a and armeabi-v7a devices

## Active Projects
| Project | Goal | Owner | Timeline |
|---------|------|-------|----------|
| v3.0.0 release | Stable release with all core features | anonfaded | Current |
| Scheduled recording | Auto start/stop at set times | anonfaded | Upcoming |
| Faditor Mini | In-app video editor | anonfaded | Planned |
| iOS/Desktop version | Cross-platform expansion | anonfaded | Planned |

## Archive (Resolved Items)
### Resolved: Migrate from ExoPlayer 2.x to Media3
- **Resolved**: 2025
- **Resolution**: Replaced deprecated exoplayer2 with Media3 (exoplayer, transformer, effect, muxer)
- **Learnings**: Media3 provides unified API for playback + editing + muxing

### Resolved: 16KB Page Alignment for Android 15
- **Resolved**: 2025
- **Resolution**: Set `useLegacyPackaging = false` in packaging.jniLibs, added `debugSymbolLevel = "FULL"`
- **Learnings**: Required for Android 15 compatibility on devices with 16KB memory pages

## Onboarding Checklist
- [ ] Review known technical debt and understand impact
- [ ] Know what open questions exist and who's involved
- [ ] Understand current issues and workarounds
- [ ] Be aware of patterns and gotchas
- [ ] Know active projects and timelines

## Related Files
- `decisions-log.md` — Past decisions that inform current state
- `business-domain.md` — Business context for current priorities
- `technical-domain.md` — Technical context for current state
