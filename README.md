# Word Guard

Free, do-it-yourself blocking of bad stuff and distractions for your computer and Android phone. It has three layers that cover each other's gaps:

| Layer | What it blocks | Where |
|---|---|---|
| **NextDNS** (free cloud service) | Porn sites, plus forcing SafeSearch | Every device, on any network |
| **Word Filter** browser extension | Searches and pages whose address or title contains a blocked word; hides matching YouTube videos | Chrome, Brave, Edge, Chromium |
| **Word Guard** Android app | Browser searches and addresses with blocked words, YouTube videos with blocked words, and attempts to uninstall it or change Private DNS | Android phones |

None of it is impossible to get around. It's meant to turn a moment of weakness into a deliberate, multi-step effort. It works best when an **accountability partner** holds the passwords and PIN.

---

## 1. NextDNS (do this first)

1. Make a free account at [nextdns.io](https://nextdns.io).
2. **Parental Control** tab: turn on **Porn** under Categories, plus **SafeSearch**. Turn on **YouTube Restricted Mode** too if you'd rather use that than the word filter.
3. **Privacy** tab: add a blocklist such as "NextDNS Ads & Trackers". Turn on **Block bypass methods** if it's offered; it blocks VPNs, proxies and Tor.
4. Note your **configuration ID** (6 characters) on the **Setup** tab.
5. Have your partner **change the NextDNS password**.

**Android phone:** Settings → Network & internet → **Private DNS** → "Private DNS provider hostname" → `PhoneName-YOURID.dns.nextdns.io`

**Windows / Mac:** follow the instructions on the NextDNS **Setup** tab.

**Linux:** `desktop/install.sh YOURID` sets this up for you (see below).

Check it's working by visiting [test.nextdns.io](https://test.nextdns.io).

---

## 2. Computer: Word Filter extension

The blocked word list is in [`desktop/words.txt`](desktop/words.txt): one word or phrase per line, matched as whole words regardless of capitalization ("sex" does not block "Essex"). Write `/regex/` for a pattern.

On any site, if the page's **address or title** contains a blocked word, it's replaced with a "Blocked" page. That covers searches, since the search words are in the address. On YouTube, it hides matching videos, comments and searches, and hides Shorts.

### Windows / Mac (simple install)
1. Download this repo (green **Code** button → **Download ZIP**) and unzip it.
2. Edit `desktop/ext/default-words.txt` if you want to change the list.
3. Open `chrome://extensions` (or `brave://extensions` / `edge://extensions`) and turn on **Developer mode**.
4. Click **Load unpacked** and pick the `desktop/ext` folder.

This version can be turned off from the extensions page. To lock it, your browser has to be "managed" by system policy. On Linux, the script below does that.

### Linux (locked install for Chromium + Brave)
```bash
cd desktop
./build.sh                       # packs and signs the extension (needs chromium installed)
sudo ./install.sh YOURNEXTDNSID  # installs it and locks it in
```
This:
- **Force-installs** the extension in Chromium and Brave, so it can't be removed or disabled
- **Routes all DNS** through NextDNS, encrypted
- **Locks** each browser's own encrypted DNS ("Secure DNS") to NextDNS
- **Forces** Google SafeSearch
- **Disables** incognito and guest windows, and Tor windows in Brave

After that, edit the list at `/etc/yt-word-filter/words.txt` and run `sudo yt-filter-apply`.

**Updating the extension later:** Brave won't update it on its own. Close Brave, delete the extension's entry from `extensions.settings` in `~/.config/BraveSoftware/Brave-Browser/Default/Preferences` and its folder under `Extensions/`, then reopen Brave and it reinstalls the new version.

---

## 3. Android: Word Guard app

An Accessibility service that reads what's on screen and presses **Back** when it sees a blocked word:
- **Browsers** (Chrome, Brave, Firefox, Samsung, Edge, DuckDuckGo, the Google app, …): checks the address and search bar
- **YouTube** (official, ReVanced, Morphe, NewPipe, LibreTube): checks every title on screen
- **Settings:** blocks the screens for uninstalling Word Guard, turning it off, or changing **Private DNS**, which also protects NextDNS
- **Word list:** editable inside the app, behind a **PIN** your partner sets

It works entirely on the phone, with no internet or DNS needed.

### Install
1. Download `WordGuard.apk` from the [Releases](../../releases) page onto the phone and open it. Allow "install unknown apps" when asked.
2. Open **Settings → Accessibility → Word Guard** and turn it on.
   - If it says **"Restricted setting"**: go to Settings → Apps → Word Guard → ⋮ (top right) → **Allow restricted settings**, then try again.
   - Or, from a computer with `adb`: `adb shell settings put secure enabled_accessibility_services org.wordguard/org.wordguard.GuardService`. This **replaces** any other Accessibility services you have on; add them back separated by `:`.
3. Open Word Guard and have your partner **set the PIN**.

**Escape hatch:** booting into **safe mode** turns off all downloaded apps, so you can uninstall Word Guard that way if something goes wrong.

### Build it yourself
Needs JDK 17 and the Android SDK.
```bash
cd android
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
./gradlew assembleDebug    # → app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. Android TV / Google TV (e.g. Sony Bravia)

The same app works on Android TV and Google TV. It covers the TV **YouTube** app, YouTube Kids and SmartTube, and protects the TV's Settings. A TV can't open an APK by itself, so install it from a computer on the **same network** as the TV.

### On the TV
1. **Settings → System → About**, then click **Android TV OS build** 7 times until it says you're a developer.
2. **Settings → System → Developer options** → turn on **USB debugging**.
3. **Settings → Network & Internet** → click the connection and note the **IP address**.

### On the computer (Windows PowerShell)
```powershell
# adb (Android platform-tools)
Invoke-WebRequest https://dl.google.com/android/repository/platform-tools-latest-windows.zip -OutFile pt.zip
Expand-Archive pt.zip -DestinationPath . -Force
$adb = ".\platform-tools\adb.exe"

# Word Guard
Invoke-WebRequest https://github.com/DuckySteals/word-guard/releases/latest/download/WordGuard.apk -OutFile WordGuard.apk

& $adb connect TV_IP:5555        # the TV asks "Allow USB debugging?": tick Always allow, OK
& $adb install -r WordGuard.apk
& $adb shell settings put secure enabled_accessibility_services org.wordguard/org.wordguard.GuardService
& $adb shell settings put secure accessibility_enabled 1
```
On Mac or Linux, use the same `adb` commands.

Then open **Word Guard** from the TV's apps row and have your partner **set the PIN**.

### Checking it works
Search YouTube on the TV for a blocked word. It should back straight out.

If it doesn't, turn on the debug log, open the screen in question, and read what Word Guard sees:
```powershell
& $adb shell setprop log.tag.WordGuard DEBUG
& $adb logcat -c
# ...use the TV for a moment...
& $adb logcat -d -s WordGuard
```
A `window:` line shows each app or screen that opens, and `dump` lines list the text Word Guard can read. If YouTube's titles aren't there, that app can't be filtered by words, so block the YouTube app instead. Turn the log off with `setprop log.tag.WordGuard ""`.

Afterwards, turn **USB debugging** off on the TV, so nobody else on the network can connect to it.

---

## Limits
- **Images:** Words can only catch text. A bad image under an innocent title gets through. NextDNS blocks the porn sites themselves. For screen-scanning AI on Android, [Bulldog Blocker](https://play.google.com/store/apps/details?id=com.everaccountable.bulldog) (free) pairs well with this.
- **Admin access:** Anyone with admin rights on the computer can undo the Linux setup. The NextDNS password is the real lock, so give it to your partner.
- **YouTube feed on Android:** The app backs you out when a matching title is anywhere on screen, including the feed.
- **App updates:** If a browser or YouTube changes its screen layout, the app may need updating.
