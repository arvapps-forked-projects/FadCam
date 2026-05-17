<!-- Context: project-intelligence/technical | Priority: critical | Version: 2.1 | Updated: 2026-05-17 -->

# Technical Domain

**Purpose**: Tech stack, architecture, and development patterns for FadCam — a privacy-focused Android multimedia suite (background recording, dashcam, screen recorder, live streaming, remote control).
**Last Updated**: 2026-05-17

## Quick Reference
**Update Triggers**: Tech stack changes | New features | Architecture decisions
**Audience**: Developers, AI agents

## Primary Stack
| Layer | Technology | Version | Rationale |
|-------|-----------|---------|-----------|
| Language | Java | 8 | Android native, broad compatibility |
| Build | Gradle + AGP | 8.13.1 | Kotlin DSL build scripts |
| Min SDK | Android 7.0 (API 24) | — | Broad device coverage |
| Target SDK | Android 16 (API 36 "Baklava") | — | Latest platform features |
| Camera API | Camera2 + CameraX | 1.6.0 | Camera2 for raw recording, CameraX for lifecycle |
| Playback | Media3 (ExoPlayer) | 1.8.0 | Modern media playback + Transformer for editing |
| Database | Room | 2.8.4 | Local SQLite (VideoIndexDB, ForensicsDB) |
| Storage | SharedPreferences | — | App-wide settings (700+ pref keys) |
| HTTP Server | NanoHTTPD | 2.3.1 | Local network streaming |
| Video Processing | FFmpeg-kit | 6.0-2 LTS | Transcoding, muxing |

## Key Libraries
CameraX 1.6.0 | Media3 1.8.0 | FFmpeg-kit 6.0-2 LTS | NanoHTTPD 2.3.1 | MP4Parser 1.1.22 | TensorFlow Lite 2.17.0 | OpenCV 4.13.0 | Lottie 6.7.1 | Glide 5.0.5 | OkHttp 5.3.2 | Gson 2.13.2 | Osmdroid 6.1.20 | AppIntro 6.3.1 | ZXing 4.3.0 | Material 1.13.0 | Navigation 2.9.7 | Lifecycle 2.10.0

## Architecture Pattern
```
Type: Monolithic Android App
Package: com.fadcam
Structure: Feature-based packages under com.fadcam.*
```

### Project Structure
```
app/src/main/java/com/fadcam/
├── MainActivity.java              # Shell activity w/ BottomNavigationView (2149 lines)
├── FadCamApplication.java         # Application-level init
├── Constants.java                 # All SharedPreferences keys + constants (704 lines)
├── SharedPreferencesManager.java  # Centralized singleton prefs access
├── RecordingState.java            # Enum: STARTING, IN_PROGRESS, PAUSED, NONE, WAITING_FOR_CAMERA
├── CameraType.java                # Enum: FRONT(1), BACK(0), DUAL_PIP(2)
├── VideoCodec.java                # Enum: H264, H265
├── FLog.java                      # Privacy-aware logging w/ redaction (URLs, IPs, tokens, paths)
├── Log.java                       # In-app HTML log viewer
├── services/                      # Foreground services (RecordingService 6995 lines, TorchService, etc.)
├── ui/                            # UI fragments (HomeFragment, RecordsFragment, SettingsHomeFragment, etc.)
├── utils/                         # Utility classes (StorageCache, DeviceHelper, VideoStatsCache, etc.)
├── data/                          # Data layer (Room DBs: VideoIndexDatabase, VideoIndexDao, FastFileScanner)
├── playback/                      # Fragmented MP4 playback (SeekableMediaSource, PlayerHolder, Remuxer)
├── streaming/                     # Remote streaming (RemoteStreamService, CloudStatusManager, RemoteAuthManager)
├── dualcam/                       # Dual PiP camera (DualCameraConfig, DualCameraState, DualCameraCapability)
├── fadrec/                        # FadRec screen recorder (MediaProjection, annotation tools)
├── opengl/                        # OpenGL ES (GLWatermarkRenderer, GLRecordingPipeline, grafika utils)
├── watermark/                     # WatermarkManager + dynamic overlays
├── security/                      # StreamKeyManager, SegmentEncryptor
├── model/                         # Data models (TrashItem, etc.)
├── widgets/                       # Home screen widgets (ClockWidgetProvider, ArabicDateUtils)
├── shortcuts/                     # App shortcuts (ShortcutsManager, ShortcutsPreferences)
├── forensics/                     # Digital forensics module (ForensicsDatabase, snapshots)
└── sensors/                       # SensorDataProvider (noise, motion, etc.)
```

## Code Patterns

### Service Pattern (foreground service w/ notification)
```java
public class RecordingService extends Service {
    private static final String TAG = "RecordingService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle start command, setup camera/recording
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cleanup resources, stop recording
    }
}
```

### SharedPreferences Pattern (centralized singleton)
```java
// Access via singleton
SharedPreferencesManager spm = SharedPreferencesManager.getInstance(context);

// Read
String theme = spm.sharedPreferences.getString(Constants.PREF_APP_THEME, Constants.DEFAULT_APP_THEME);

// Write
spm.sharedPreferences.edit()
    .putString(Constants.PREF_KEY, value)
    .apply();
```

### Logging Pattern (FLog w/ auto-redaction)
```java
private static final String TAG = "ClassName";
FLog.d(TAG, "Debug message");
FLog.e(TAG, "Error message", exception);
// FLog auto-redacts: URLs, IPs, tokens/apikeys, file paths
```

### Room Database Pattern (thread-safe singleton)
```java
@Database(entities = {VideoIndexEntity.class}, version = 1, exportSchema = false)
public abstract class VideoIndexDatabase extends RoomDatabase {
    private static volatile VideoIndexDatabase instance;

    public static VideoIndexDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (VideoIndexDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                        VideoIndexDatabase.class, "video_index.db")
                        .fallbackToDestructiveMigration().build();
                }
            }
        }
        return instance;
    }
}
```

## Naming Conventions
| Type | Convention | Example |
|------|-----------|---------|
| Java files | PascalCase | `MainActivity.java`, `RecordingService.java` |
| Packages | lowercase | `com.fadcam.utils`, `com.fadcam.services` |
| Classes | PascalCase | `VideoStatsCache`, `FragmentedMp4Muxer` |
| Methods | camelCase | `startRecording()`, `getCachedStats()` |
| Constants (public static final) | UPPER_SNAKE_CASE | `PREFS_NAME`, `PREF_VIDEO_FRAME_RATE` |
| Constants (private static final) | lowerCamelCase | `cacheLock`, `mainHandler` |
| SharedPreferences keys | lower_snake_case w/ prefix | `video_frame_rate`, `pref_torch_state` |
| Resources (layouts/drawables) | snake_case | `activity_main.xml`, `ic_record.xml` |
| Database entities | PascalCase + Entity suffix | `VideoIndexEntity` |
| DAOs | PascalCase + Dao suffix | `VideoIndexDao` |

## Code Standards
- Java 8 source/target compatibility
- AndroidX libraries exclusively (no legacy support library)
- BuildConfig enabled for build-time constants
- Dependencies info excluded from APK (`dependenciesInfo.includeInApk = false`)
- ProGuard/R8 rules in `proguard-rules.pro`
- Product flavors: notesPro, calcPro, weatherPro, default
- Build types: debug (.beta suffix), release (minified), pro (.pro), proPlus (.proplus)
- ABI splits: armeabi-v7a + arm64-v8a (universal APK for main, arm64-only for pro)
- 16KB page alignment enabled for Android 15+ compatibility
- Vector drawables use support library
- Lint: MissingTranslation disabled, release checks disabled
- Test framework: JUnit 4.13.2, Mockito 5.2.0, Espresso 3.7.0

## Security Requirements
- Minify + shrink resources enabled for all release builds
- No dependency metadata in APKs
- FLog auto-redacts sensitive data (URLs, IPs, tokens, file paths)
- Stream key management for remote access authentication
- Segment encryption support (SegmentEncryptor)
- Hide from recent apps option for privacy
- No data collection or tracking, 100% ad-free
- Ethical use guidelines and disclaimer enforced

## 📂 Codebase References
**Build Config**: `app/build.gradle.kts` — flavors, splits, signing, 16KB alignment
**Version Catalog**: `gradle/libs.versions.toml` — all dependency versions
**Constants**: `app/src/main/java/com/fadcam/Constants.java` — 704 lines of SharedPreferences keys
**Main Activity**: `app/src/main/java/com/fadcam/MainActivity.java` — BottomNavigationView shell (2149 lines)
**Recording Service**: `app/src/main/java/com/fadcam/services/RecordingService.java` — Camera2 + MediaRecorder (6995 lines)
**FLog**: `app/src/main/java/com/fadcam/FLog.java` — privacy-aware logging w/ redaction
**SharedPreferencesManager**: `app/src/main/java/com/fadcam/SharedPreferencesManager.java` — centralized prefs singleton
**VideoIndexDatabase**: `app/src/main/java/com/fadcam/data/VideoIndexDatabase.java` — Room DB for media index
**ForensicsDatabase**: `app/src/main/java/com/fadcam/forensics/data/local/ForensicsDatabase.java` — Room DB for forensics
**Playback**: `app/src/main/java/com/fadcam/playback/` — FragmentedMp4SeekableMediaSource, PlayerHolder, Remuxer
**Streaming**: `app/src/main/java/com/fadcam/streaming/` — RemoteStreamService, RemoteAuthManager, CloudStatusManager
**Dual Camera**: `app/src/main/java/com/fadcam/dualcam/` — DualCameraConfig, DualCameraState, DualCameraCapability
**Screen Recorder**: `app/src/main/java/com/fadcam/fadrec/` — MediaProjection-based screen recording
**OpenGL**: `app/src/main/java/com/fadcam/opengl/` — GLWatermarkRenderer, GLRecordingPipeline, grafika
**Security**: `app/src/main/java/com/fadcam/security/` — StreamKeyManager, SegmentEncryptor

## Related Files
- `business-domain.md` — Why this technical foundation exists
- `business-tech-bridge.md` — How business needs map to technical solutions
- `decisions-log.md` — Full decision history with context
- `living-notes.md` — Active issues, debt, open questions
