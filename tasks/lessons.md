# Lessons Learned

## Build Command Best Practices (2026-05-12)

**Rule**: Always use `./gradlew assembleDefaultDebug installDefaultDebug` instead of `./gradlew clean build`.

**Why**: 
- `clean` is unnecessary and wastes time
- `build` rebuilds everything, not needed for incremental changes
- `assembleDefaultDebug installDefaultDebug` is the correct workflow for fast iteration
- This is the project standard per AGENTS.md

**Pattern**: When building for testing/verification, always use the optimized assembly + install pattern.

## QRScanner Crash: YUV Format Mismatch (2026-05-07)

**Pattern**: When fixing camera image format crashes, verify ALL code paths that consume the image data.

**Fix applied**: Kept `YUV_420_888`, added `yuv420888ToNv21()` with plane-specific strides + buffer rewind (decodeImage exhausts buffer positions).

## QRScanner: BufferUnderflowException (2026-05-07)

**Root cause**: `decodeImage()` called `buf.get(data)` on Y plane buffer, advancing position to end. `yuv420888ToNv21` then tried `yBuffer.get(...)` from exhausted buffer.

**Rule**: When multiple methods read from same `ImageProxy` planes, always `.rewind()` buffers before reading. `getBuffer()` returns shared ByteBuffer views.
