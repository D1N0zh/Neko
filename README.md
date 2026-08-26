# Neko Android 1.1.1

Prima conversione Android della versione web Neko V10.8.28.

## Cosa contiene
- UI completa Neko V10.8.28 in `app/src/main/assets/`
- WebView Android con JavaScript e localStorage/DOM storage
- selettore file Android per l'importazione PDF
- icona Neko per tutte le densità Android
- package: `com.neko.finance`
- minSdk 26, targetSdk 35
- Home Edit con pressione prolungata, riordino drag-and-drop e ordine salvato in locale

## Build
Aprire la cartella in Android Studio e lasciare sincronizzare Gradle.
Poi:
- Build > Build APK(s)
oppure
- `./gradlew assembleDebug`

APK previsto:
`app/build/outputs/apk/debug/app-debug.apk`

## Nota PDF
La versione web attuale usa PDF.js da cdnjs. L'app Android ha il permesso INTERNET,
quindi il parsing PDF funziona con connessione. Un passaggio successivo consigliato
è includere PDF.js localmente per rendere l'import completamente offline.

## Prossimi step nativi
1. Test reale import PDF e persistenza.
2. Export CSV/backup con Storage Access Framework nativo.
3. Eventuale blocco biometrico/PIN.


## GitHub Actions
È incluso `.github/workflows/build-apk.yml`. Vedi `GITHUB_BUILD_GUIDE.md` per compilare automaticamente `Neko.apk` su GitHub.
