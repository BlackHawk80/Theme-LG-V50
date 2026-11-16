from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path("LiquidGlassWidget")
layout = root / "app/src/main/res/layout/widget_liquid_glass.xml"
xml = layout.read_text(encoding="utf-8")

# Only use classes allowed in RemoteViews: a plain <View> divider may fail inflation.
xml = xml.replace("<View\n", "<TextView\n")

# Change weekday highlight without invoking the forbidden setBackgroundResource method.
# Static image resources are shown/hidden via the supported setViewVisibility API.
pattern = re.compile(r'<TextView (?P<attrs>android:id="@\\+id/tv_week_(?P<index>[0-6])"[^>]+)\\s*/>')

def replace_day(match):
    index = int(match.group("index"))
    attrs = match.group("attrs")
    attrs = attrs.replace('android:layout_width="20dp"', 'android:layout_width="24dp"')
    attrs = attrs.replace('android:layout_height="20dp"', 'android:layout_height="24dp"')
    return f'''<FrameLayout android:layout_width="24dp" android:layout_height="24dp">
                <ImageView
                    android:id="@+id/bg_week_{index}"
                    android:layout_width="24dp"
                    android:layout_height="24dp"
                    android:src="@drawable/bg_glass_circle"
                    android:contentDescription="@null"
                    android:visibility="gone" />
                <TextView {attrs} />
            </FrameLayout>'''

xml, hits = pattern.subn(replace_day, xml)
assert hits == 7, f"Expected 7 weekday cells, found {hits}"
assert "<View\n" not in xml
ET.fromstring(xml)
layout.write_text(xml, encoding="utf-8")

provider = root / "app/src/main/java/com/example/liquidglasswidget/LiquidGlassWidgetProvider.kt"
code = provider.read_text(encoding="utf-8")
code = code.replace("import android.widget.RemoteViews", "import android.widget.RemoteViews\nimport android.view.View")
code = code.replace("        val labelIds = intArrayOf(\n", """        val highlightIds = intArrayOf(
            R.id.bg_week_0, R.id.bg_week_1, R.id.bg_week_2,
            R.id.bg_week_3, R.id.bg_week_4, R.id.bg_week_5, R.id.bg_week_6
        )
        val labelIds = intArrayOf(
""")
code = code.replace('views.setInt(numberIds[i], "setBackgroundResource", R.drawable.bg_glass_circle)',
                    'views.setViewVisibility(highlightIds[i], View.VISIBLE)')
code = code.replace('views.setInt(numberIds[i], "setBackgroundResource", android.R.color.transparent)',
                    'views.setViewVisibility(highlightIds[i], View.GONE)')
assert '"setBackgroundResource"' not in code
code = code.replace("""val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextAlarm = alarmManager.nextAlarmClock""",
"""val nextAlarm = try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.nextAlarmClock
        } catch (ignored: Exception) {
            null
        }""")
provider.write_text(code, encoding="utf-8")

info = root / "app/src/main/res/xml/widget_info.xml"
config = info.read_text(encoding="utf-8")
config = config.replace('android:minHeight="180dp"', 'android:minHeight="300dp"')
config = config.replace('android:targetCellHeight="3"', 'android:targetCellHeight="5"')
ET.fromstring(config)
info.write_text(config, encoding="utf-8")

build = root / "app/build.gradle"
gradle = build.read_text(encoding="utf-8")
gradle = gradle.replace("versionCode 1", "versionCode 2").replace('versionName "1.0"', 'versionName "1.1"')
assert "versionCode 2" in gradle
build.write_text(gradle, encoding="utf-8")
print("LG V50 RemoteViews compatibility patch v1.1: validated successfully")
