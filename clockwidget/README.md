# AURA Glass Clock — standalone Android home widget

**V1.0 | package** `com.auraglass.clockwidget` | **Minimum** Android 8.0 | Designed for LG V50 / Android 12.

The APK is a separate native Android App Widget. It does **not** replace the home launcher.

## Visual design
- HH:mm only — no date, frame, or background.
- Transparent alpha background with iced glass, volumetric bevel, glow and holographic rainbow highlights.
- Color styles: Pearl, Ice Blue, Holographic.
- Adjustable glass brightness.
- Widget can be resized on the Home screen.
- Touching the widget opens its configuration app.

## Installation
1. Download and install the signed **debug** APK in the workflow artifact.
2. Open **AURA Glass Clock** and choose a glass style.
3. Tap **Thêm widget vào màn hình Home**, or long-press home screen and choose **Widget → AURA Glass Clock**.
4. Optionally enable the Android "Alarms & reminders" permission for more accurate minute updates.
5. If this setting is unavailable, the widget still works, but Android may delay updates during Doze mode.

## Notes
- Color refraction is simulated inside digits. Standard Android widgets cannot read or optically distort the wallpaper behind them.
- Wallpaper remains fully visible through the empty areas around digits.
- No root, Internet permission, tracking, or Android launcher replacement required.
- Source builds in GitHub Actions with Android SDK 35, Gradle and Java 17.
- Compiled APK uses Android debug signing; later releases should use a stable release signing key for seamless updates.
