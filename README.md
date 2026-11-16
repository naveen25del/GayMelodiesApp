# Gay Melodies App 🎵

gaymelodies.best ke liye Spotify-style Android app (WebView based).

## Features
- Full-screen app jaisa experience (browser ke address bar ke bina)
- Pull-to-refresh
- Back button navigation (app ke andar)
- Spotify-green theme + music note launcher icon
- Loading progress bar
- Cookies/login session save hote hain

## APK kaise banaye — 2 tarike

### Tarika 1: GitHub Actions se (FREE, phone se hi ho sakta hai)
1. GitHub pe account banao (agar nahi hai)
2. New repository banao (public rakho, free hai)
3. Is poore `GayMelodiesApp` folder ko upload kar do (ya `git push` karo)
4. Repo me **Actions** tab kholo → "Build APK" workflow chalega
5. Complete hone ke baad run kholo → **Artifacts** me `GayMelodies-APK` download karo
6. Phone me install karo (unknown sources allow karke)

### Tarika 2: Android Studio se
1. Android Studio kholo → `Open` → ye folder select karo
2. Gradle sync hone do (SDK apne aap download ho jayega)
3. Menu: `Build` → `Build Bundle(s)/APK(s)` → `Build APK(s)`
4. APK `app/build/outputs/apk/debug/app-debug.apk` me milega

## Project structure
```
GayMelodiesApp/
├── .github/workflows/build.yml   ← CI se APK auto-build
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/gaymelodies/app/MainActivity.java
│       └── res/ (icons, layout, theme)
├── build.gradle
└── settings.gradle
```

## Note
- Ye ek WebView wrapper app hai — site ka poora player iske andar chalega
- App ko minimize karne par audio kuch devices me band ho sakta hai
- Version/app name badalne ke liye: `app/build.gradle` aur `strings.xml`
