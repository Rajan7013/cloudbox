# CloudBox – File Manager App PRD

Version 1.0 · Mobile app (Android and iOS) · Storage backed by Google Drive

## 1. Vision

CloudBox is a simple, beautiful mobile app to store, view, organize and share every kind of file (documents, images, audio, video, PDFs, archives) using the user's Google Drive as the storage layer.

## 2. Goals

- Upload, view, download and share any file type from a phone.
- Organize with unlimited nested folders.
- Find files fast with search, sort and filters.
- Control who can see or edit each file or folder.

## 3. Target users

Students, freelancers and small teams who want a lightweight Drive experience that is faster and simpler to use.

## 4. Core features (MVP)

1. **Sign in with Google** and connect Drive (OAuth, scope: drive.file).
2. **Browse**: grid or list view, breadcrumb path, folders inside folders.
3. **Create**: new folder, upload file, take photo, scan document.
4. **Upload**: multi-select, progress, pause, retry, background upload.
5. **View / read in-app**: PDF, Office docs, images (zoom), video player, audio player, text files.
6. **Download** and offline availability ("Make available offline").
7. **Share**: link sharing and invite by email.
8. **Permissions**: Viewer, Commenter, Editor; link access Restricted or Anyone with link; optional expiry.
9. **Search**: by name and by content type, with recent searches.
10. **Sort**: name, date modified, size, type; ascending or descending.
11. **Filters**: type (docs, images, video, audio, PDF, folders), owner, date range, shared or not.
12. **File actions**: rename, move, copy, star, details, delete to trash, restore.
13. **Trash** with restore and permanent delete (auto-empty after 30 days).
14. **Storage meter** with usage by category.
15. **Settings**: theme (light/dark), default view, wifi-only uploads, account.

## 5. Later features (V2)

Comments, version history, document scanner with OCR, offline folders sync, app lock (PIN/biometric), activity feed, tags and colors for folders, multi-account, widgets.

## 6. Screen list

1. Splash and onboarding (3 slides)
2. Sign in with Google
3. Home (recent, quick access, storage)
4. My Files (folders and files, grid/list)
5. Folder view (nested, breadcrumb)
6. Create / Upload bottom sheet
7. Upload progress
8. File viewer (image, PDF, video, audio)
9. File actions sheet
10. Share and permissions
11. Search with filters
12. Sort and filter sheet
13. Shared with me
14. Starred
15. Offline
16. Trash
17. File details
18. Storage
19. Settings and profile

## 7. Navigation

Bottom tabs: **Home, Files, Shared, Starred**. Floating **+** button for create/upload. Top bar holds search and the profile avatar.

## 8. UX and visual design

- Style: clean, rounded (16px cards), generous spacing, soft shadows.
- Colors: indigo primary (#4F46E5), with type colors: blue for docs, green for sheets, orange for PDF, pink for images, purple for video, teal for audio.
- Typography: Inter or Poppins; 16px body, 22px titles.
- Motion: bottom sheets slide up, smooth list-grid transition, upload progress ring.
- Dark mode supported; minimum 44px touch targets; accessible contrast.

## 9. Technical plan

- **Beginner path**: Step 1 clickable web prototype (done), Step 2 real app with Flutter or React Native (Expo), Step 3 connect Google Drive API v3.
- Auth: Google Sign-In (OAuth 2.0). Storage and sharing/permissions: Drive API (files, permissions, about).
- Local cache: SQLite for offline metadata.

## 10. Success metrics

Upload success rate above 98%, time to first upload under 60 seconds, 7-day retention above 35%, search result in under 1 second.

## 11. Risks

Drive API quotas, large video uploads on weak networks (use resumable uploads), OAuth verification for sensitive scopes.

## 12. Roadmap

- Week 1: prototype and screens
- Weeks 2–3: sign-in, browse, folders
- Weeks 4–5: upload, viewer, download
- Week 6: share, permissions, search, sort, filters
- Week 7: trash, offline, settings
- Week 8: testing and release
