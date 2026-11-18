# CloudBox - Step 2 (Expo / React Native)

## Run it (about 5 minutes)
1. Install Node.js (LTS) from nodejs.org and the **Expo Go** app on your phone.
2. In a terminal:
   npx create-expo-app@latest cloudbox --template blank
   cd cloudbox
   npx expo install @react-navigation/native @react-navigation/bottom-tabs react-native-screens react-native-safe-area-context
3. Copy **App.js** and the **src** folder from this download into the `cloudbox` folder (replace App.js).
4. npx expo start  -> scan the QR code with Expo Go (Android) or the Camera app (iPhone).

## Try these
- Tap "Use demo data", open Work > Projects (nested folders), tap the breadcrumb to jump back.
- Pull down to refresh. Tap Grid / List. Rotate the phone (columns change).
- Open src/data/mockRepository.js and set FLAGS.forceError or FLAGS.forceEmpty to true to see those states.

## Step 3 changes
- New: src/components/Sheet.js, Bits.js, Toast.js
- Updated: src/data/mockRepository.js, src/components/FileItem.js, src/screens/FilesScreen.js
- Try: tap +, New folder; tap the three dots on a file (star, rename, trash then Undo); Sort & filter.
