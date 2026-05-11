# Lessons Learned

## QRScanner Crash: YUV Format Mismatch (2026-05-07)

**Pattern**: When fixing camera image format crashes, verify ALL code paths that consume the image data.

**Fix applied**: Kept `YUV_420_888`, added `yuv420888ToNv21()` with plane-specific strides + buffer rewind (decodeImage exhausts buffer positions).

## QRScanner: BufferUnderflowException (2026-05-07)

**Root cause**: `decodeImage()` called `buf.get(data)` on Y plane buffer, advancing position to end. `yuv420888ToNv21` then tried `yBuffer.get(...)` from exhausted buffer.

**Rule**: When multiple methods read from same `ImageProxy` planes, always `.rewind()` buffers before reading. `getBuffer()` returns shared ByteBuffer views.
