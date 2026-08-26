# Compilare Neko APK con GitHub

Non serve Android Studio.

## 1. Crea un account GitHub
Vai su github.com e crea/accedi al tuo account.

## 2. Crea un repository
- Premi **New repository**
- Nome consigliato: `Neko`
- Può essere **Private**
- Premi **Create repository**

## 3. Carica il progetto
Estrai lo ZIP `Neko_Android_GitHub.zip`.

Nel repository GitHub:
- scegli **Add file → Upload files**
- trascina TUTTO il contenuto della cartella estratta
- è importante che siano presenti direttamente nella radice:
  - `app/`
  - `.github/`
  - `build.gradle`
  - `settings.gradle`
  - `gradle.properties`
- premi **Commit changes**

## 4. Compila l'APK
- In **Settings → Secrets and variables → Actions**, configura i repository secrets:
  - `NEKO_KEYSTORE_BASE64`
  - `NEKO_STORE_PASSWORD`
  - `NEKO_KEY_ALIAS`
  - `NEKO_KEY_PASSWORD`
- Apri la scheda **Actions**
- scegli **Build Neko APK**
- premi **Run workflow**
- conferma con **Run workflow**

Attendi che il job diventi verde.

## 5. Scarica Neko.apk
Apri il job completato.
In fondo alla pagina trovi **Artifacts → Neko-APK**.

Scarica l'archivio dell'artefatto: dentro troverai:

`Neko.apk`

## 6. Installa sul telefono
- trasferisci `Neko.apk` sul telefono
- aprilo
- Android potrebbe chiederti di consentire l'installazione da quella fonte
- abilita il permesso e installa Neko

## Privacy
Il repository può essere impostato su **Private**.
Il progetto contiene l'app, ma non contiene i movimenti bancari salvati sul tuo telefono:
i dati locali vengono creati solo durante l'uso dell'app.
Il keystore e `keystore.properties` non sono inclusi nel repository: la workflow li
ricrea temporaneamente dai repository secrets durante la build.
