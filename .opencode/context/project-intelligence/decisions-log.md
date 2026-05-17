<!-- Context: project-intelligence/decisions | Priority: high | Version: 2.0 | Updated: 2026-05-17 -->

# Decisions Log

> Major architectural and business decisions with full context.

## Quick Reference
- **Purpose**: Document decisions so future team members understand context
- **Format**: Each decision as a separate entry
- **Status**: Decided | Pending | Under Review | Deprecated

---

## Decision: Use Camera2 API for Recording Instead of CameraX

**Date**: 2024 (initial development)
**Status**: Decided
**Owner**: anonfaded

### Context
Needed background recording capability (screen off) for dashcam and discreet recording use cases.

### Decision
Use Camera2 API directly for the recording pipeline, while using CameraX for lifecycle management and device compatibility elsewhere.

### Rationale
CameraX abstracts away camera control and doesn't support recording with screen off. Camera2 provides the low-level access needed to keep the camera pipeline active when the screen is off.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| CameraX only | Simpler code, automatic device compatibility | Cannot record with screen off | Core feature requirement |
| MediaRecorder only | Simple API | No fine-grained control over camera parameters | Need manual control for quality settings |

### Impact
**Positive**: Enables background recording, the app's core differentiator
**Negative**: More complex code (RecordingService.java is 6995 lines), device-specific quirks to handle
**Risk**: Camera2 API complexity increases bug surface area

---

## Decision: Fragmented MP4 Format for Recording

**Date**: 2024
**Status**: Decided
**Owner**: anonfaded

### Context
Users reported corrupted video files when phone crashed or battery died during recording.

### Decision
Use fragmented MP4 (fMP4) format via Media3 Muxer instead of standard MP4.

### Rationale
Standard MP4 requires finalization (writing moov atom at end of file). If recording is interrupted, the entire file is corrupted. Fragmented MP4 writes playable data continuously in segments.

### Impact
**Positive**: Zero corruption risk — every segment is independently playable
**Negative**: Requires custom playback logic (FragmentedMp4SeekableMediaSource, PlayerHolder, Remuxer)
**Risk**: Compatibility with some video players that don't support fMP4

---

## Decision: Java 8 Instead of Kotlin

**Date**: 2024 (project inception)
**Status**: Decided
**Owner**: anonfaded

### Context
Starting a new Android project in 2024 when Kotlin is the recommended language.

### Decision
Use Java 8 for the entire codebase.

### Rationale
Developer preference and familiarity. The codebase grew organically in Java and migration cost would be significant.

### Alternatives Considered
| Alternative | Pros | Cons | Why Rejected? |
|-------------|------|------|---------------|
| Kotlin | Modern language, null safety, coroutines | Learning curve, migration cost | Existing codebase is Java; developer preference |

### Impact
**Positive**: Faster development for the team, no migration needed
**Negative**: More boilerplate, no coroutines, manual null handling
**Risk**: Harder to attract Kotlin-preferring contributors

---

## Decision: No Analytics, No Tracking, 100% Ad-Free

**Date**: 2024
**Status**: Decided
**Owner**: anonfaded

### Context
Privacy-focused positioning requires zero data collection.

### Decision
No analytics SDKs, no ad networks, no crash reporting services that send data externally. FLog auto-redacts sensitive data (URLs, IPs, tokens, file paths).

### Rationale
Privacy is the product's core value proposition. Any data collection would contradict the brand promise.

### Impact
**Positive**: Strong differentiation, user trust, featured on privacy-focused platforms
**Negative**: No usage analytics for product decisions, harder to debug user issues
**Risk**: Relying on user-reported bugs only

---

## Decision: Monetize via Patreon (Lifetime Access)

**Date**: 2024
**Status**: Decided
**Owner**: anonfaded

### Context
Need revenue to sustain development without compromising privacy-first ethos.

### Decision
Patreon shop with lifetime access to Pro features. Separate build variants (pro, proPlus) with different app IDs.

### Rationale
One-time purchase aligns with open-source values. No subscription model. Build variants allow standalone Pro installation.

### Impact
**Positive**: Revenue without ads or tracking, users own their purchase
**Negative**: Lower recurring revenue vs subscription model
**Risk**: Lifetime access means no recurring revenue from existing users

---

## Decision: Multi-Store Distribution Strategy

**Date**: 2024
**Status**: Decided
**Owner**: anonfaded

### Context
Maximize reach while respecting user choice of app stores.

### Decision
Distribute on F-Droid, IzzyOnDroid, Amazon Appstore, and GitHub Releases simultaneously.

### Rationale
Different user segments prefer different stores. F-Droid for FOSS users, Amazon for Fire tablet users, GitHub for power users.

### Impact
**Positive**: Maximum reach, 50k+ downloads across channels
**Negative**: More complex release process, need to maintain compatibility with each store's requirements
**Risk**: Store-specific review delays

---

## Decision: Target SDK 36 (Android 16)

**Date**: 2025
**Status**: Decided
**Owner**: anonfaded

### Context
Google requires apps to target recent SDK levels. Android 16 (API 36) brings edge-to-edge enforcement and adaptive app requirements.

### Decision
Set compileSdk and targetSdk to 36, with 16KB page alignment for Android 15+ compatibility.

### Impact
**Positive**: Future-proof, Play Store compliance, access to latest APIs
**Negative**: Must handle edge-to-edge, orientation/resizability changes on large screens
**Risk**: Breaking changes on older devices

---

## Deprecated Decisions
| Decision | Date | Replaced By | Why |
|----------|------|-------------|-----|
| ExoPlayer 2.x for playback | 2024 | Media3 (ExoPlayer) 1.8.0 | ExoPlayer 2.x deprecated, Media3 is the modern replacement |

## Onboarding Checklist
- [ ] Understand why Camera2 was chosen over CameraX for recording
- [ ] Know why fragmented MP4 is used (corruption prevention)
- [ ] Understand the privacy-first decisions and their technical implications

## Related Files
- `technical-domain.md` — Technical implementation affected by these decisions
- `business-tech-bridge.md` — How decisions connect business and technical
