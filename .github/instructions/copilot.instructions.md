---
applyTo: '**'
---

# Instructions

### ✅ General Guidelines
- **Only modify related code**: Never alter unrelated code. Keep changes scoped to the specific user request.
- **Code quality**: Write clean, modular, and production-grade Java code using industry best practices.
- **Object-Oriented Design**: Leverage OOP principles like encapsulation, abstraction, inheritance, and polymorphism where applicable.
- **Strong typing**: Use clear and appropriate Java types for all method arguments, return types, and variables.
- **Error handling**: Implement structured exception handling using `try-catch` blocks with meaningful logging or messages.
- **Cross-platform compatibility**: Ensure code runs consistently on different operating systems when applicable (e.g., file paths, line separators).

### 🧱 Code Structure & Best Practices
- **Component-based design**: Separate concerns into packages such as `core`, `utils`, `service`, `model`, `controller`, etc.
- **Organized project hierarchy**: Maintain a clean package and directory structure adhering to Maven/Gradle conventions.
- **Multifile organization**: Split large classes or files into smaller ones to follow the Single Responsibility Principle.
- **Proper indentation**: Use 4 spaces per indentation level. Match indentation style of existing code if contributing.
- **Avoid duplication**: Reuse existing code through methods, utility classes, or inheritance. Avoid copy-paste logic.
- **Imports**: Keep all import statements at the top of the file. Avoid wildcard imports (`import java.util.*`).

### 📃 Comments & Documentation
- **Javadoc**: Use Javadoc-style comments for all public classes and methods. Document parameters, return values, and exceptions.
- **Inline comments**: Use inline comments sparingly to clarify complex logic. Avoid obvious or redundant comments.

### 🛠️ Fixing / Updating Code
- **Change format**: Wrap all code changes using the following format:
  ```java
  // -------------- Fix Start for this method(methodName)-----------
  ...updated code...
  // -------------- Fix Ended for this method(methodName)-----------
  ```
  
### 🧠 Communication
- **Ask when unsure**: If requirements are unclear or ambiguous, ask for clarification before proceeding.
- **Concise responses**: Keep responses short and focused. Avoid unnecessary explanations or code unless requested.
- **Complete solutions**: If the fix/update is small, provide the full method or class code block for clarity.

### 🧑‍💻 Industry Best Practices
- **Follow style guides**: Follow Google Java Style Guide or Oracle conventions for naming and formatting.
- **SOLID principles**: Apply SOLID principles, design patterns, and dependency injection where applicable.
- **Exception handling**: Handle exceptions gracefully. Do not catch generic Exception unless necessary.
- **Input validation**: Always validate external inputs and sanitize file/stream access.
- **Logging**: Use logging frameworks like SLF4J or Log4j instead of System.out.println for production code.
- **Testing**: Ensure code is scalable, testable, and maintainable with proper unit tests (e.g., JUnit).
- **Build and install**: After every complete change, run `./gradlew compileDebugJavaWithJavac` to check if the build succeeds. If not, fix the errors and check again. Once successful, run `./gradlew installDebug` so the app is installed on your phone.

### 🎯 UI Icon Policy — Material Icons Font (Required)
- Always use the local Material Icons font for UI icons instead of vector/XML drawables.
- Font path: `app/src/main/res/font/materialicons.ttf` (referenced as `@font/materialicons`).
- Rendering approach:
  - Prefer a `TextView` configured with `android:fontFamily="@font/materialicons"` and set the icon using its ligature text (e.g., `"arrow_upward"`).
  - When setting programmatically, load once and cache the typeface:
    - `Typeface tf = ResourcesCompat.getFont(context, R.font.materialicons);`
    - Reuse the cached instance to avoid repeated loads and crashes.
- In pickers and side sheets, prefer ligature icons via a dedicated leading symbol `TextView`; keep legacy `ImageView` only as fallback.
- For new features, select contextual ligatures (e.g., `trending_up`/`trending_down` for size, `arrow_upward`/`arrow_downward` for recency).
- Do not add new SVG/vector drawables for standard icons; use ligatures unless a brand/logo asset is required.
 
## FadCam UI Styling — Settings Rows & Bottom Pickers

Use this spec when adding new settings or bottom-sheet pickers to keep visuals consistent.

- Shared background and ripple
  - Use `@drawable/settings_home_row_bg` for all tappable rows and picker items.
  - Ripple inset must be 4dp uniformly on all sides so the press fill stays off the card edges.

- Settings rows (inside group cards)
  - Container: apply `style="@style/SettingsGroupRow"` (center vertical, shared background, no per-row paddings).
  - Content gutters: paddingStart 14dp, paddingEnd 12dp (provided by the style).
  - Leading icon/symbol: 24dp square with marginEnd 16dp.
    - Prefer Material Icons ligatures via a TextView using `@font/materialicons` (per policy); use ImageView only as fallback.
  - Title: 15sp, bold, color `?attr/colorHeading`.
  - Subtitle/value text: 12sp, color `@android:color/darker_gray`.
  - Value-to-arrow gap: add `android:layout_marginEnd="12dp"` on the trailing value TextView before the arrow/switch.
  - Trailing arrow/icon: 14dp, tinted `@android:color/darker_gray`.
  - Dividers between rows: use `@style/SettingsDivider` with margins Start 14dp / End 12dp to align with gutters.

- Group cards
  - Wrap related rows in a card using `@drawable/settings_group_card_bg`.
  - Vertical padding on the card container: 4dp top and 4dp bottom.

- Bottom pickers (bottom sheets)
  - Item rows mirror Settings rows: same background, gutters (14dp start / 12dp end), and spacing rules.
  - Leading symbol/icon: 24dp with marginEnd 16dp; prefer ligatures via `@font/materialicons`.
  - Trailing switch/chevron: add `layout_marginEnd="12dp"` for breathing room.
  - Keep optional subtitle at 12sp, `@android:color/darker_gray`.

- Text and resources
  - Never hardcode UI strings in layouts; always use `@string/...` entries.
  - Keep spacing/margins centralized via shared styles; avoid per-row overrides unless absolutely necessary.
