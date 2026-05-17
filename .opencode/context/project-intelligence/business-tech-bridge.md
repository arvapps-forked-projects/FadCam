<!-- Context: project-intelligence/bridge | Priority: high | Version: 2.0 | Updated: 2026-05-17 -->

# Business ↔ Tech Bridge

> How FadCam's business needs translate to technical solutions.

## Quick Reference
- **Purpose**: Connect business goals to technical implementations
- **Update When**: New features, refactoring, business pivot

## Core Mapping
| Business Need | Technical Solution | Why This Mapping | Business Value |
|---------------|-------------------|------------------|----------------|
| Record discreetly without screen on | Background service + Camera2 API + screen-off capability | Camera2 gives raw access needed for background recording | Core differentiator vs commercial apps |
| Prevent file corruption on crash | Fragmented MP4 muxing (Media3 Muxer) | Each segment is independently playable | Users never lose critical footage |
| No data collection / privacy-first | FLog w/ auto-redaction, no analytics SDKs, no cloud dependencies | Privacy is the product's core value proposition | Trust and differentiation in market |
| Professional screen recording | FadRec module w/ MediaProjection API + OpenGL overlay | MediaProjection is the only Android API for screen capture | Expands user base to content creators |
| Remote monitoring from anywhere | NanoHTTPD local server + web interface | Lightweight, no cloud dependency needed | Security use case without subscription costs |
| Dual camera recording | Concurrent camera sessions via Camera2 + PiP compositing | Camera2 supports concurrent camera IDs | Unique feature for comprehensive recording |
| Wide device compatibility | Min SDK 24 (Android 7.0), CameraX lifecycle | CameraX handles device-specific camera quirks | Largest possible user base |
| Monetize without ads | Patreon integration, Pro build variants (pro, proPlus) | Separate build variants w/ different app IDs | Revenue without compromising privacy |
| Distribute widely | ABI splits (arm64 + armeabi-v7a), universal APK | Smaller downloads for users, broad compatibility | More downloads across all channels |
| Android 15+ compatibility | 16KB page alignment, targetSdk 36 | Google requires 16KB alignment for Android 15 | Future-proof, Play Store compliance |

## Feature: Background Recording
**Business Context**: Users need to record video while phone screen is off (dashcam, security, discreet recording)
**Technical Implementation**: Foreground service (`RecordingService.java`, 6995 lines) using Camera2 API directly, not CameraX, for full control over recording pipeline
**Connection**: Camera2 provides the low-level access needed to keep recording when screen is off. CameraX alone can't do this.

## Feature: Fragmented MP4
**Business Context**: Users can't afford to lose recordings if phone crashes or battery dies mid-recording
**Technical Implementation**: Media3 Muxer writes fragmented MP4 segments; each segment is independently playable. MP4Parser (isoparser) handles box structure parsing for playback
**Connection**: Standard MP4 requires finalization (moov atom at end). Fragmented MP4 writes playable data continuously.

## Feature: FadRec Screen Recorder
**Business Context**: Content creators need professional screen recording with annotation tools
**Technical Implementation**: MediaProjection API for screen capture, OpenGL pipeline for annotation overlays (pen, eraser, text, shapes), Media3 Transformer for post-processing
**Connection**: MediaProjection is Android's only screen capture API. OpenGL enables real-time annotation overlays.

## Feature: Remote Streaming
**Business Context**: Users want to monitor their phone camera remotely (security, baby monitor)
**Technical Implementation**: NanoHTTPD serves MJPEG stream on local network, web interface for control, RemoteAuthManager for stream key authentication
**Connection**: Local-only streaming means no cloud dependency, no subscription, no data leaving the device.

## Trade-off Decisions
| Situation | Business Priority | Technical Priority | Decision Made | Rationale |
|-----------|-------------------|-------------------|---------------|-----------|
| Camera API choice | Works on all devices | Use modern CameraX | Camera2 for recording + CameraX for lifecycle | Camera2 needed for background recording; CameraX for device compatibility elsewhere |
| Language choice | Fast development | Kotlin for modern features | Java 8 | Existing codebase is Java; migration cost outweighs benefits |
| Monetization | Revenue | Keep it simple | Patreon lifetime access + build variants | No subscription model aligns with privacy-first ethos |

## Stakeholder Communication
**For Business Stakeholders**: Technical investments (Camera2, fragmented MP4, FLog) directly serve the privacy-first, reliable recording value proposition
**For Technical Stakeholders**: Business constraints (no cloud, no analytics, open-source) drive architecture decisions like local-only streaming and FLog redaction

## Onboarding Checklist
- [ ] Understand how each major feature maps to a business need
- [ ] Know the key trade-offs (Camera2 vs CameraX, Java vs Kotlin)
- [ ] Be able to explain why technical choices serve business goals

## Related Files
- `business-domain.md` — Business needs in detail
- `technical-domain.md` — Technical implementation in detail
- `decisions-log.md` — Decisions made with full context
