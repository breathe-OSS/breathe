#!/usr/bin/env bash
set -x

PKG=com.sidharthify.breathe
APK=app/build/outputs/apk/debug/app-debug.apk
mkdir -p shots

adb install -r "$APK"

adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile hide
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false

adb shell am start -W -n "$PKG/.MainActivity"
sleep 5
adb shell am force-stop "$PKG"

seed_prefs() {
  local dark=$1
  cat > prefs.xml <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <set name="pinned_ids">
        <string>srinagar</string>
        <string>kargil_town</string>
        <string>jammu_city</string>
    </set>
    <string name="pinned_order">srinagar,kargil_town,jammu_city</string>
    <boolean name="has_onboarded" value="true" />
    <boolean name="is_dark_theme" value="$dark" />
</map>
XML
  adb push prefs.xml /data/local/tmp/breathe_prefs.xml
  adb shell chmod 644 /data/local/tmp/breathe_prefs.xml
  adb shell run-as "$PKG" mkdir -p shared_prefs
  adb shell "run-as $PKG sh -c 'cat /data/local/tmp/breathe_prefs.xml > shared_prefs/breathe_prefs.xml'"
}

tap_label() {
  local label=$1
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null
  adb pull /sdcard/ui.xml ui.xml > /dev/null
  local bounds
  bounds=$(grep -o "text=\"$label\"[^>]*bounds=\"[^\"]*\"" ui.xml | head -n 1 | grep -o 'bounds="[^"]*"')
  if [ -z "$bounds" ]; then
    bounds=$(grep -o "content-desc=\"$label\"[^>]*bounds=\"[^\"]*\"" ui.xml | head -n 1 | grep -o 'bounds="[^"]*"')
  fi
  if [ -z "$bounds" ]; then
    echo "No element labelled $label"
    return 1
  fi
  read -r x1 y1 x2 y2 <<< "$(echo "$bounds" | grep -o '[0-9]\+' | tr '\n' ' ')"
  adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
}

shoot() {
  adb exec-out screencap -p > "shots/$1.png"
}

launch() {
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/.MainActivity"
  sleep 15
}

size=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -n 1)
width=${size%x*}
height=${size#*x}

for mode in light dark; do
  if [ "$mode" = dark ]; then
    seed_prefs true
  else
    seed_prefs false
  fi

  launch
  shoot "home-$mode"

  adb shell input swipe $(( width / 2 )) $(( height * 3 / 4 )) $(( width / 2 )) $(( height / 4 )) 400
  sleep 2
  shoot "home-scrolled-$mode"

  tap_label Map && sleep 10 && shoot "map-$mode"
  tap_label Explore && sleep 4 && shoot "explore-$mode"
  tap_label Settings && sleep 3 && shoot "settings-$mode"
done

ls -la shots
