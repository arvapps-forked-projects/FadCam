<!-- Context: project-intelligence/business | Priority: high | Version: 2.0 | Updated: 2026-05-17 -->

# Business Domain

> Business context, problems solved, and value created by FadCam.

## Quick Reference
- **Purpose**: Understand why FadCam exists and who it serves
- **Update When**: Business direction changes, new features shipped, pivot
- **Audience**: Developers needing context, stakeholders, product team

## Project Identity
```
Project Name: FadCam
Tagline: Privacy-focused Android multimedia suite
Problem Statement: Android users lack a trustworthy, ad-free camera app that can record discreetly, serve as a dashcam, record screens professionally, and stream remotely — without data collection or tracking
Solution: Open-source, 100% ad-free Android app with background recording, dashcam, screen recorder (FadRec), live streaming, and remote control
```

## Target Users
| User Segment | Who They Are | What They Need | Pain Points |
|--------------|--------------|----------------|-------------|
| Privacy-conscious users | People who value data privacy | Ad-free recording without tracking | Existing apps collect data, show ads, require unnecessary permissions |
| Drivers | Commuters, rideshare drivers | Reliable dashcam with auto-start | Dedicated dashcams are expensive, phone apps drain battery |
| Content creators | YouTubers, educators | Screen recording with annotations | Professional tools are paid, complex |
| Security-conscious | Home owners, travelers | Remote camera monitoring | IP cameras are expensive, cloud services have privacy risks |

## Value Proposition
**For Users**:
- 100% ad-free, no data collection or tracking
- Background recording with screen off
- Fragmented MP4 format prevents file corruption
- Multiple distribution channels (F-Droid, IzzyOnDroid, Amazon, GitHub)
- Open-source — fully auditable

**For Business**:
- Patreon shop for Pro features (lifetime access model)
- Trending #12830 on Trendshift — strong organic growth
- Featured on 50+ blogs, podcasts, and YouTube channels
- Community-driven via Discord (1200+ members)

## Success Metrics
| Metric | Definition | Target | Current |
|--------|------------|--------|---------|
| Downloads | Total APK downloads | Growing | 50k+ (GitHub + stores) |
| Trendshift ranking | GitHub trending position | Top 100 | #12830 |
| Community | Discord members | Growing | 1200+ |
| Distribution | Store presence | 4+ channels | F-Droid, IzzyOnDroid, Amazon, GitHub |

## Business Model
```
Revenue Model: Freemium — free open-source core + Pro features via Patreon (lifetime access)
Pricing Strategy: One-time lifetime access (no subscription)
Market Position: Privacy-first, open-source alternative to commercial camera/dashcam apps
Part of: FadSec Lab suite (fadsec-lab) — ad-free, privacy-first applications
```

## Key Stakeholders
| Role | Name | Responsibility |
|------|------|----------------|
| Creator/Lead Dev | anonfaded (Faded) | Architecture, development, community |
| Designer | T010 (ko-fi.com/t010nl) | App screenshots, banner design |
| Community | Discord server | Feedback, bug reports, feature requests |

## Roadmap Context
**Current Focus**: v3.0.0 — stable release with all core features
**Next Milestone**: Scheduled recording (auto start/stop at set times)
**Planned**: In-app video editor (Faditor Mini), enhanced remote features
**Long-term**: FadCam for iOS and Desktop (planned)

## Business Constraints
- **Open-source license** — all code must remain open, limiting proprietary features
- **Android platform dependency** — features constrained by Android API capabilities and permissions
- **Privacy-first mandate** — no analytics, no tracking, no cloud dependencies for core features
- **Single developer** — limited bandwidth for feature development and support

## Onboarding Checklist
- [ ] Understand the privacy-first mission and why it drives every decision
- [ ] Know the target users: privacy-conscious, drivers, creators, security-minded
- [ ] Understand the freemium model: open-source core + Patreon Pro
- [ ] Know the distribution channels: F-Droid, IzzyOnDroid, Amazon, GitHub
- [ ] Understand the FadSec Lab suite context

## Related Files
- `technical-domain.md` — How this business need is solved technically
- `business-tech-bridge.md` — Mapping between business and technical
- `decisions-log.md` — Business decisions with context
