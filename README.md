# Creatif — Offline Graphics Studio (Android source)

Creatif is an Android-native, offline-first graphics design starter project. It is designed to build into an installable debug APK using Android Studio or the included GitHub Actions workflow.

## Current features
- 1080 × 1350 poster canvas
- Add photos from the device and add text/shapes
- Drag layers around the canvas
- Select layers through the Layers list
- Edit existing text, change text colour, resize selected layers, and move layers forward/back
- Undo/redo for design changes
- Canvas background presets
- Export the design as a PNG to `Pictures/Creatif`
- **AI person background remover:** select a photo layer and tap **AI Cutout**. It uses the on-device ML Kit Selfie Segmentation model and is designed for people/selfies, not arbitrary objects. After processing, use **Save Cutout PNG** to save the selected photo as a standalone PNG with transparent background to `Pictures/Creatif/Cutouts`. Use **Export PNG** to export the full design. Clear, well-lit photos give the best results. The bundled-model dependency is intended to allow offline processing after the app is installed; this still needs verification on a physical device.

## Build the APK using a phone
1. Create a GitHub repository and upload the contents of this project, preserving the folder structure.
2. Open the repository's **Actions** tab.
3. Choose **Build Creatif APK** and tap **Run workflow**.
4. When the workflow finishes, open the run and download the `Creatif-debug-apk` artifact.
5. Extract the downloaded artifact ZIP and install the APK on your Android phone. Android may ask you to allow installation from that source.

You can also open the project in Android Studio and build `app` > `assembleDebug`.

## Technical notes
- Application ID: `com.creatif.studio`
- Minimum Android version: Android 8.0 (API 26)
- Compile SDK: 35
- The app's core design and export tools work offline. The AI person-cutout feature uses an on-device bundled model; no cloud upload is used by the feature.
- This is an actively extendable starter build, not a claim of feature parity with commercial desktop graphics suites. Text styling, project-file saving, advanced typography, precise transform handles, and general-object background removal are future improvements.
