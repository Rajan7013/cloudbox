# CloudBox – Screen-by-Screen UI/UX Spec

Version 1.1 · Companion to the CloudBox PRD · Covers every screen, the design system, all data states (loading, skeleton, empty, error, offline, live) and responsive behaviour.

## 1. How to read this document

- **ASCII layouts** are drawn for a 360 px wide phone (compact). Tablet and large-screen changes are listed under *Responsive* in each screen.
- `[ ]` means a button, `( )` a chip or round control, `▓` filled progress, `░` empty progress, `~~~` a skeleton shimmer block.
- Every screen has: **Purpose, Layout, Components, States, Motion, Responsive**.
- All screens read data from one repository interface, so the same UI works with **mock data** now and **live Google Drive data** later (see section 4).

## 2. Design system

### 2.1 Colour tokens

| Token | Light | Dark | Use |
| --- | --- | --- | --- |
| primary | #4F46E5 | #7C83FF | Primary buttons, active tab, links, progress |
| primaryContainer | #EEF0FF | #24284A | Secondary buttons, selected chips, icon tiles |
| background | #F5F6FB | #0E1020 | App background |
| surface | #FFFFFF | #1A1D33 | Cards, rows, sheets, nav bar |
| textPrimary | #161B2E | #EEF0FF | Titles, file names |
| textSecondary | #6B7190 | #9296B5 | Metadata, hints |
| border | #E7E9F4 | #2A2E4F | Dividers, input outline |
| success | #10B981 | #34D399 | Uploaded, saved offline |
| warning | #F59E0B | #FBBF24 | Storage almost full |
| error | #EF4444 | #F87171 | Delete, failed upload |
| scrim | rgba(10,12,30,.5) | rgba(0,0,0,.6) | Behind sheets and dialogs |

**Hero gradient:** 135°, #4F46E5 to #8B5CF6 (storage card, onboarding logo tile).

**File-type colours** (icon tile uses the colour at 13% opacity, glyph at 100%):

| Type | Colour | Type | Colour |
| --- | --- | --- | --- |
| Folder | #F59E0B | Image | #EC4899 |
| Document | #3B82F6 | Video | #8B5CF6 |
| Sheet | #10B981 | Audio | #14B8A6 |
| PDF | #EF4444 | Archive | #64748B |

### 2.2 Typography

Font: **Inter** (fallback system-ui, Roboto, SF Pro). Numbers use tabular figures.

| Style | Size / line | Weight | Use |
| --- | --- | --- | --- |
| Display | 32 / 40 | 700 | Onboarding, empty-state titles |
| H1 | 24 / 32 | 700 | Screen titles |
| H2 | 20 / 28 | 600 | Section titles in sheets |
| Title | 17 / 24 | 600 | File name in viewer, list headers |
| Body | 15 / 22 | 400 | Paragraphs, inputs |
| Label | 14 / 20 | 600 | Buttons, tabs, chips |
| Caption | 12 / 16 | 500 | Metadata (size, date, owner) |
| Overline | 11 / 16 | 700, +0.5 tracking, uppercase | Section labels in Settings |

Text scales with the system font size up to 200%. File names truncate with an ellipsis in the middle for long names (keeps the extension visible).

### 2.3 Spacing, shape, elevation

- 4 pt grid: 4, 8, 12, 16, 24, 32, 48. Screen side padding 16 (compact), 24 (medium), 32 (expanded).
- Radius: chip 20 · input 14 · button 14 · card 18 · hero card 22 · bottom sheet top 26 · FAB 20 · avatar 50%.
- Elevation: card `0 6 20 rgba(60,60,120,.09)` · sheet `0 -8 30 rgba(0,0,0,.18)` · FAB `0 10 24 rgba(79,70,229,.5)`. In dark mode shadows are replaced by a 1 px border and a lighter surface.
- Icons: 24 px outline icons, 2 px stroke, rounded caps. Icon tiles are 44 px (list) or 74 px tall (grid thumbnail).

### 2.4 Buttons

| Button | Look | Height | Use |
| --- | --- | --- | --- |
| **Primary** | Filled primary, white label 15/700 | 48 | One main action per screen (Continue, Create, Invite) |
| **Secondary** | primaryContainer fill, primary label | 48 | Alternative action (Copy link, Cancel) |
| **Tertiary** | No fill, primary label | 40 | Inline actions (See all, Retry) |
| **Destructive** | error label on 10% error tint; filled error only on the final confirm | 48 | Delete forever, Empty trash |
| **Icon button** | 40 × 40, radius 12, surface fill, card shadow | 40 | Search, back, more (⋮) |
| **FAB** | 58 × 58, radius 20, primary, white plus | 58 | Create or upload |
| **Chip** | 36 high, radius 20; selected = primary fill + white label | 36 | Filters, sort, quick access |

**Button states:** default · pressed (scale .97, 8% darker, 100 ms) · focused (2 px primary ring for keyboard and switch access) · disabled (40% opacity, no ripple) · loading (label replaced by a 20 px spinner, width stays fixed so the layout does not jump). Minimum touch target 48 × 48 even if the visual is smaller.

### 2.5 Inputs and other components

- **Text field:** 48 high, radius 14, 1.5 px border (border token), background = background token. Focus: border primary + 3 px primaryContainer halo. Error: border error + 12/16 helper text in error.
- **Search bar:** 48 high, radius 16, surface fill, card shadow, leading search icon, trailing clear (x) when text exists.
- **List row:** 64 high, radius 16, icon tile 44, two text lines (Body 600 + Caption), trailing ⋮ or role badge.
- **Grid card:** radius 18, 12 padding, thumbnail 74 high, name, metadata, ⋮ top-right.
- **Role badge:** Caption 700 on primaryContainer, radius 8 (Viewer, Commenter, Editor, Owner).
- **Bottom sheet:** surface fill, top radius 26, 42 × 5 grabber, max height 85%, drag down to dismiss.
- **Toast / snackbar:** dark #1C2040 fill, white text, radius 16, floats 96 px above the bottom, 2.2 s; optional action (Undo).
- **Toggle:** 46 × 26 track, 20 knob, primary when on.
- **Segmented control:** surface track with 4 px padding, selected segment filled primary.
- **Skeleton block:** surface tone 8% darker with a 1.2 s left-to-right shimmer; same radius as the real element.

### 2.5 Motion

| Interaction | Duration | Curve | Description |
| --- | --- | --- | --- |
| Tap feedback | 100 ms | ease-out | Scale to .97 and ripple |
| Tab switch | 200 ms | ease-in-out | Cross-fade content, icon bounces 4 px |
| Open folder / file | 300 ms | cubic-bezier(.2,.8,.2,1) | Shared-element: thumbnail grows into the destination header |
| Back | 250 ms | ease-in | Reverse of open |
| Bottom sheet | 220 ms | cubic-bezier(.2,.8,.2,1) | Slides up from 60% with fade, scrim fades in |
| Grid ↔ list | 250 ms | ease-in-out | Cards morph to rows |
| List first load | 30 ms stagger, max 8 items | ease-out | Rows fade and rise 8 px |
| Skeleton shimmer | 1200 ms loop | linear | Gradient sweep |
| Upload progress | continuous | linear | Ring fills; on success it turns green with a check pop (250 ms) |
| New live item | 300 ms + 1.2 s | ease-out | Row slides in and its background flashes primaryContainer then fades |
| Delete | 250 ms | ease-in | Row collapses height to 0 and the toast offers Undo |
| Toast | 250 ms in, 2.2 s hold, 200 ms out | ease | Slides up |

**Reduced motion:** when the system asks for it, shimmer becomes a static tone, stagger and shared-element transitions become a 150 ms fade.

## 3. Responsive behaviour

| Class | Width | Navigation | Content columns | Sheets | Viewer / details |
| --- | --- | --- | --- | --- | --- |
| **Compact** (phones) | 0–599 | Bottom bar, 4 tabs, FAB | Grid: 2 columns, list: 1 | Bottom sheet | Full screen |
| **Medium** (large phones landscape, small tablets) | 600–839 | Navigation rail, 80 px, left | Grid: 3–4 columns | Centered dialog, max 560 wide | Full screen, tools on the side |
| **Expanded** (tablets, foldables, desktop web) | 840+ | Permanent drawer, 280 px | Grid: 4–6 columns, optional list + detail pane | Dialog or side panel | Right detail pane, 360 px |

Rules:

1. Grid column count = `floor((width − 2 × padding) / 160)`, minimum 2, so cards stay between 140 and 200 px wide.
2. Content never grows wider than 1200 px; extra space becomes margin.
3. Respect safe areas (notch, home indicator, gesture bar). The bottom bar and FAB add the bottom inset.
4. Portrait to landscape keeps scroll position and selection. The viewer goes edge to edge with controls that hide after 3 s.
5. Touch targets are at least 48 px; on desktop web hover shows a lighter surface and a visible focus ring.
6. Long text wraps in titles and truncates in lists; no horizontal scrolling except chips, tables and spreadsheets.
7. Foldables: when folded half-open, list on one half and the viewer on the other.

## 4. Data and state system

Every screen has the same five visual states, driven by one state object: `{ status: loading | success | empty | error, items, isRefreshing, isOffline, lastSynced }`.

| State | What the user sees | Rules |
| --- | --- | --- |
| **Loading (first load)** | Skeleton screen that matches the final layout (same number of cards or rows as would fit) with shimmer | Show skeleton only if loading takes more than 300 ms, then keep it for at least 400 ms so it does not flash |
| **Refreshing** | Existing content stays; a 24 px spinner appears at the top after pull-to-refresh | Never blank the screen while refreshing |
| **Loading more** | A small spinner row at the bottom of the list | Load next page 5 items before the end |
| **Empty** | Illustration (160 px), Display title, one line of help, one primary button | Different copy per screen (below); empty search shows suggestions instead |
| **Error** | Inline card: icon, plain-language message, **Retry** tertiary button | Content that was already loaded stays visible with a small error banner on top |
| **Offline** | Yellow-tinted banner “You are offline. Showing saved files.” Online-only actions are greyed out; offline files show a green pin | Actions taken offline are queued and show a clock badge until synced |
| **Live (real data)** | Real names, thumbnails, sizes and dates replace mock content with no layout change | Thumbnails fade in 200 ms over the file-type tile; changes from other devices appear with the live-item animation |

**Mock data to live data.** The UI only talks to a `FileRepository` interface with two implementations: `MockRepository` (the sample data in the last section, with an artificial 600–1200 ms delay so skeletons can be seen) and `DriveRepository` (Google Drive API v3 plus a local SQLite cache). A single setting `USE_MOCK_DATA` switches between them, so no screen needs to change when the backend is connected.

When live data arrives: (1) cached items show immediately, (2) a thin 2 px progress line runs under the top bar while syncing, (3) the list updates in place by file id, so nothing jumps, (4) “Updated just now” appears in the pull-to-refresh area, (5) star, rename and move apply optimistically and roll back with an error toast if the server refuses.

## 5. Screens

### Screen 1 · Splash

**Purpose:** brand moment while the app starts and the session is restored.

```
┌──────────────────────────────┐
│                              │
│                              │
│                              │
│          ┌────────┐          │
│          │ (cloud)│          │
│          └────────┘          │
│          CloudBox            │
│   Your files, everywhere     │
│                              │
│                              │
│            ( o )             │
│                              │
└──────────────────────────────┘
```

**Components:** 92 px logo tile with the hero gradient and glow shadow; wordmark Display 32 white on gradient (light: gradient background, dark: #0E1020); tagline Body at 80% opacity; 24 px spinner. **States:** if a valid session exists go straight to Home; otherwise Onboarding or Sign in. If it takes over 3 s show “Still loading…” and a Retry button. **Motion:** logo scales from .8 to 1 with a soft bounce (400 ms), text fades in after 150 ms. **Responsive:** content is centered; on tablets the logo stays 92 px.

### Screen 2 · Onboarding (3 slides)

**Purpose:** explain the three value points once, on first launch.

```
┌──────────────────────────────┐
│                      Skip    │
│    ┌──────────────────┐      │
│    │                  │      │
│    │  (illustration)  │      │
│    │                  │      │
│    └──────────────────┘      │
│                              │
│      Store everything        │
│  Documents, photos, video    │
│  and audio in your Drive.    │
│                              │
│          ━━  ○  ○            │
│                              │
│ ┌──────────────────────────┐ │
│ │          Next            │ │
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

**Slides:** 1 Store everything · 2 Share with control (Viewer, Commenter, Editor) · 3 Find it fast (search, sort, filters). **Components:** Skip (tertiary), illustration card 220 px high, Display title, Body text in textSecondary, page dots (active dot is 24 px wide pill), primary **Next** that becomes **Get started** on slide 3. **Motion:** swipe or tap Next slides content horizontally (300 ms); the active dot stretches. **Responsive:** on wide screens the illustration sits left and text right (two columns, max width 880).

### Screen 3 · Sign in

**Purpose:** connect a Google account (OAuth) so files are stored in the user’s Drive.

```
┌──────────────────────────────┐
│                              │
│          ┌────────┐          │
│          │ (cloud)│          │
│          └────────┘          │
│      Welcome to CloudBox     │
│  Sign in to open your Drive  │
│                              │
│ ┌──────────────────────────┐ │
│ │ G  Continue with Google  │ │
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │
│ │ Use demo data (no login) │ │
│ └──────────────────────────┘ │
│                              │
│  By continuing you accept    │
│  Terms and Privacy Policy    │
└──────────────────────────────┘
```

**Components:** primary button with the Google “G”; secondary **Use demo data** (turns on `MockRepository`); legal text Caption with link-coloured Terms and Privacy. **States:** *loading* the Google button shows a spinner and both buttons are disabled; *error* “Sign-in was cancelled” inline above the buttons with a Retry; *offline* banner and the Google button disabled. **Motion:** buttons fade up with 60 ms stagger. **Responsive:** form is a 400 px centered card on tablets, with the logo above it.

### Screen 4 · Home

**Purpose:** quick overview: storage, quick filters and recent files.

```
┌──────────────────────────────┐
│ 9:41                ▂▄▆ 100% │
│                              │
│ Hello, Alex              [A] │
│ ┌──────────────────────────┐ │
│ │ (o) Search in CloudBox   │ │
│ └──────────────────────────┘ │
│ ╔══════════════════════════╗ │
│ ║ Google Drive storage     ║ │
│ ║ 842 MB  of 15 GB         ║ │
│ ║ ▓▓░░░░░░░░░░░░░░░░░░░░░░ ║ │
│ ║ Docs · Images · Video  > ║ │
│ ╚══════════════════════════╝ │
│ Quick access                 │
│ (Images)(Video)(Audio)(PDF)> │
│ Recent               See all │
│ ┌────────────┐ ┌───────────┐ │
│ │  [thumb]   │ │  [thumb]  │ │
│ │ Resume.pdf │ │ Notes.docx│ │
│ │ Yesterday  │ │ Today     │ │
│ └────────────┘ └───────────┘ │
│                          (+) │
├──────────────────────────────┤
│ Home  Files  Shared  Starred │
└──────────────────────────────┘
```

**Components:** greeting H1 and avatar (opens Settings); search bar (opens Search); hero storage card with gradient, white text and a 8 px progress bar; Quick access chips (type shortcuts); Recent grid (up to 6 files); FAB; bottom bar. **States:**

- *Loading:* greeting is real text, the hero card, chips and 4 recent cards are `~~~` skeletons.
- *Empty (new account):* illustration of an open box, title “Nothing here yet”, text “Upload your first file to see it here”, primary **Upload a file**. Storage card still shows 0 MB of 15 GB.
- *Error:* the storage card shows “Couldn’t load storage” with Retry; Recent shows the error card.
- *Offline:* banner and the recent list from cache.
- *Storage > 90%:* hero card turns warning-amber with “Almost full. Manage storage”.
- *Live:* real total from Drive `about` data; new files uploaded elsewhere slide in at the start of Recent. **Motion:** progress bar animates from 0 to value (600 ms), cards stagger in. **Responsive:** Medium: hero and chips in the top row, Recent becomes 3 columns. Expanded: two-column layout, hero card left (360 px) and Recent grid right with 4 to 6 columns.

### Screen 5 · My Files (grid and list)

**Purpose:** browse everything at the top level of the user’s Drive.

```
┌──────────────────────────────┐
│ My Files             (o) [A] │
│ My Files > Work              │
│ [ Sort & filter ]   [ Grid ] │
│ FOLDERS                      │
│ ┌───────────┐  ┌───────────┐ │
│ │ [folder]  │  │ [folder]  │ │
│ │ Work      │  │ Photos    │ │
│ │ 3 items   │  │ 2 items   │ │
│ └───────────┘  └───────────┘ │
│ FILES                        │
│ ┌───────────┐  ┌───────────┐ │
│ │ [pdf]     │  │ [image]   │ │
│ │ Resume.pdf│  │ Beach.jpg │ │
│ │ 420 KB    │  │ 3.4 MB    │ │
│ └───────────┘  └───────────┘ │
│                          (+) │
├──────────────────────────────┤
│ Home  Files  Shared  Starred │
└──────────────────────────────┘
```

**List mode** (toggle with the Grid / List chip):

```
│ [folder] Work            (:) │
│          3 items · Today     │
│ [pdf]    Resume.pdf      (:) │
│          420 KB · Yesterday  │
│ [sheet]  Budget 2026.xlsx(:) │
│          310 KB · 2 days · * │
```

**Components:** title with search and avatar; breadcrumb; Sort & filter chip (shows a dot when a filter is active); Grid / List chip; section overlines FOLDERS and FILES (folders always first); cards or rows; star mark ★ after a name when starred; people icon when shared; green pin when available offline; FAB. **States:**

- *Loading:* breadcrumb is real, 4 skeleton cards (grid) or 6 skeleton rows (list), shimmer 1.2 s.
- *Empty:* box illustration, “This folder is empty”, text “Tap + to add files or create a folder”, primary **Upload a file**, secondary **New folder**.
- *No results after a filter:* “No files match your filter” with a tertiary **Clear filter**.
- *Error:* error card with Retry; existing items stay visible if already loaded.
- *Selection mode:* long press a card, the top bar becomes “2 selected” with Share, Move, Star, Trash and Select all; selected cards get a primary tick and a primaryContainer fill.
- *Live:* pull down to refresh; items uploaded from other devices appear with the live-item animation. **Motion:** grid and list morph in 250 ms; tapping a folder grows its thumbnail into the next screen’s header. **Responsive:** Compact 2 columns; Medium 3 to 4; Expanded 4 to 6 columns with the drawer on the left and an optional detail pane (360 px) that shows file details when a file is selected.

### Screen 6 · Folder (nested folders)

**Purpose:** any folder inside any folder, with a path the user can tap to jump back up.

```
┌──────────────────────────────┐
│ [<] Projects         (o) [A] │
│ My Files > Work > Projects   │
│ [ Sort & filter ]   [ List ] │
│ [folder] Archive         (:) │
│          0 items · 3 days    │
│ [pdf]    App design.pdf  (:) │
│          1.8 MB · Yesterday  │
│ [zip]    Backup.zip      (:) │
│          52 MB · 8 days      │
│                          (+) │
└──────────────────────────────┘
```

**Components:** back button; breadcrumb as tappable segments (each one jumps to that level; long paths collapse the middle to “…”); all the same rows and actions as My Files. New items are created inside the current folder. **States:** *Empty folder* uses the same empty state as Screen 5; *no permission* (a shared folder where the user lost access) shows a lock icon, “You don’t have access”, and **Request access**; *deleted while open* shows “This folder was moved to Trash” with **Go back**. **Motion:** shared-element open and close; breadcrumb scrolls so the current segment stays visible. **Responsive:** on Expanded the folder tree sits in the drawer under My Files, so the path is also a sidebar.

### Screen 7 · Create / Upload sheet and New folder

**Purpose:** the single entry point to add anything.

```
┌──────────────────────────────┐
│ ░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │
│ ░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │
│ ╭──────────────────────────╮ │
│ │          ━━━━            │ │
│ │ Create new               │ │
│ │ [+] New folder           │ │
│ │ [^] Upload file          │ │
│ │ [o] Take photo           │ │
│ │ [#] Scan document        │ │
│ ╰──────────────────────────╯ │
└──────────────────────────────┘
```

**New folder dialog:** field “Folder name” with the cursor already in it, secondary **Cancel**, primary **Create** (disabled while empty). Errors: “A folder with this name already exists” under the field. **Components:** four action rows (48 px, icon tile in primaryContainer). **Upload file** opens the system picker (any type, multi-select). **States:** *offline* disables New folder and Upload with the subtitle “Needs internet”, and queues if the user insists; *storage full* shows an amber banner “Not enough space” and disables Upload. **Motion:** sheet slides up in 220 ms, FAB rotates 45° to an x while it is open. **Responsive:** Medium and Expanded show this as a 360 px popover anchored to the FAB.

### Screen 8 · Upload progress

**Purpose:** show every transfer without blocking the user.

```
┌──────────────────────────────┐
│ ╭──────────────────────────╮ │
│ │ Uploading 3 files   Hide │ │
│ │ Clip.mp4  ▓▓▓▓▓░░░  62%  │ │
│ │ Song.mp3  ▓▓▓▓▓▓▓▓ 100%  │ │
│ │ Scan.jpg  ░░░░░░░░ queued│ │
│ ╰──────────────────────────╯ │
└──────────────────────────────┘
```

**Components:** a floating card above the bottom bar (collapses to a small ring with a count); each row has name, thin 4 px progress bar, percentage, and a cancel (x). Finished rows get a green check; failed rows turn red with **Retry**. **States:** *waiting for Wi-Fi* (when “Wi-Fi only” is on) shows “Waiting for Wi-Fi…” with a clock icon; *paused* after a lost connection, resumes automatically (resumable uploads); *failed* shows the reason (“Too large”, “No space”, “No connection”). **Motion:** progress animates linearly; success turns the bar green and pops a check (250 ms); the card collapses itself 2 s after everything finishes with a toast “3 files uploaded”. **Responsive:** Compact: full-width card; Expanded: 360 px card pinned to the bottom-right.

### Screen 9 · Search

**Purpose:** find any file or folder by name or by type.

```
┌──────────────────────────────┐
│ [<] ┌────────────────────┐   │
│ [<] │ invoice        (x) │   │
│     └────────────────────┘   │
│ (All)(Docs)(PDF)(Img)(Vid) > │
│ 2 results                    │
│ [pdf]  Invoice.pdf           │
│        in Work · 2 days ago  │
│ [doc]  Invoice template.docx │
│        in Study · Today      │
└──────────────────────────────┘
```

**Components:** auto-focused field with a clear (x); type chips (All, Docs, PDF, Images, Video, Audio, Folders); result rows show the file name with the matched text in bold primary, plus the folder path; **Recent searches** with a clock icon (swipe to remove) when the field is empty. **States:**

- *Idle:* Recent searches and Suggested filters (“Shared with me”, “Added today”).
- *Searching:* 5 skeleton rows after 300 ms; results update while typing (250 ms debounce).
- *No results:* magnifier illustration, “No results for ‘invoice’”, “Check the spelling or try a different filter”, tertiary **Clear filters**.
- *Offline:* “Searching saved files only” banner.
- *Live:* server search with the Drive query API (name contains, mimeType filters). **Motion:** keyboard opens with the field already focused; results fade in. **Responsive:** on Expanded the search is a 560 px wide field in the top bar and results fill the content area.

### Screen 10 · Sort and filter sheet

**Purpose:** change the order and narrow the list.

```
┌──────────────────────────────┐
│ ╭──────────────────────────╮ │
│ │          ━━━━            │ │
│ │ Sort by                  │ │
│ │ (Modified)(Name)(Size)   │ │
│ │ (Type) (v Descending)    │ │
│ │ Filter by type           │ │
│ │ (All)(Folders)(Docs)     │ │
│ │ (Images)(Video)(Audio)   │ │
│ │ Owner      [ Anyone  v ] │ │
│ │ Date       [ Any time v] │ │
│ │ [ Reset ]     [ Apply ]  │ │
│ ╰──────────────────────────╯ │
└──────────────────────────────┘
```

**Components:** single-select sort chips, direction toggle, multi-select type chips, two dropdowns (Owner: Anyone, Me, Shared with me; Date: Any time, Today, 7 days, 30 days, Custom range), secondary **Reset** and primary **Apply** (shows the result count: “Show 12 files”). **States:** *loading counts* the Apply label shows a small spinner; *no matches* Apply is disabled with “No files match”. **Motion:** chips animate their fill (150 ms); the count rolls like an odometer. **Responsive:** a right-side panel (320 px) on Expanded; a centered dialog on Medium.

### Screen 11 · File actions sheet (⋮)

**Purpose:** everything you can do to one file or folder.

```
┌──────────────────────────────┐
│ ╭──────────────────────────╮ │
│ │          ━━━━            │ │
│ │ [pdf] Resume.pdf         │ │
│ │ [ ] Share                │ │
│ │ [ ] Download             │ │
│ │ [ ] Add star             │ │
│ │ [ ] Make available offline│ │
│ │ [ ] Rename               │ │
│ │ [ ] Move to…             │ │
│ │ [ ] Details              │ │
│ │ [!] Move to trash        │ │
│ ╰──────────────────────────╯ │
└──────────────────────────────┘
```

**Components:** header with the file’s tile and name; action rows 48 px; Move to trash in error colour with a separator above it. For files shared with me the list changes by role: **Viewer** gets Open, Download, Star, Offline, Add a copy, Details, Remove; **Editor** also gets Rename and Move. **States:** actions that need internet are greyed with “Needs internet” when offline; *Move to trash* shows a toast “Moved to trash · Undo” (5 s). **Motion:** sheet slide-up; tapping Star toggles the icon with a 200 ms scale pop. **Responsive:** a context menu (240 px) at the pointer on Expanded when right-clicking or on the ⋮ button.

### Screen 12 · Shared with me

**Purpose:** files other people shared with the user, grouped by when they arrived.

```
┌──────────────────────────────┐
│ Shared               (o) [A] │
│ ┌──────────────────────────┐ │
│ │ Shared with me│ By me    │ │
│ └──────────────────────────┘ │
│ (Everyone)(Riya)(Sam)(Maya)  │
│ [ Sort & filter ]            │
│ TODAY                        │
│ ┌──────────────────────────┐ │
│ │ [doc] Team plan.docx  •  │ │
│ │ Riya Sen · 80 KB [Editor]│ │
│ └──────────────────────────┘ │
│ THIS WEEK                    │
│ ┌──────────────────────────┐ │
│ │ [img] Brand kit.png      │ │
│ │ Sam Lee · 2.6 MB[Viewer] │ │
│ └──────────────────────────┘ │
├──────────────────────────────┤
│ Home  Files  Shared  Starred │
└──────────────────────────────┘
```

**Components:** segmented control (Shared with me / Shared by me); owner chips built from the real list of people; sort and filter chip; date group headers (Today, This week, Earlier); rows with owner avatar or file tile, owner name, size, date, a role badge, and a red dot for items not yet opened. **Shared by me tab:** same rows, but the right side shows the number of people and the link status (Restricted or Anyone with the link). **States:**

- *Loading:* segmented control and chips are real; 5 skeleton rows with a short badge block.
- *Empty (with me):* handshake illustration, “Nothing shared with you yet”, “When someone shares a file, it will appear here”.
- *Empty (by me):* “You haven’t shared anything”, primary **Share a file** (opens the file picker for sharing).
- *Revoked:* an open file whose access was removed shows “You no longer have access” and **Remove from list**.
- *Live:* a new share slides in at the top of Today and the tab bar shows a small red dot until opened. **Motion:** segmented control thumb slides 200 ms; groups fade in with stagger. **Responsive:** Medium: two-column list; Expanded: list on the left (420 px) and a preview pane on the right with the viewer.

### Screen 13 · Starred

**Purpose:** quick list of files the user marked with a star, from any folder.

```
┌──────────────────────────────┐
│ Starred              (o) [A] │
│ [ Sort & filter ]   [ List ] │
│ [pdf]   Resume.pdf       (:) │
│         in My Files · Yday   │
│ [doc]   Notes.docx       (:) │
│         in My Files · Today  │
├──────────────────────────────┤
│ Home  Files  Shared  Starred │
└──────────────────────────────┘
```

**States:** *Loading* 4 skeleton rows. *Empty:* star outline illustration, “No starred files”, “Star important files to find them fast”, secondary **Browse files**. *Live:* starring elsewhere updates the list; un-starring removes the row with a collapse animation and an **Undo** toast. **Motion:** the star icon pops (scale 1 → 1.3 → 1, 200 ms) when toggled. **Responsive:** same grid rules as My Files.

### Screen 14 · Share and permissions

**Purpose:** invite people, set what each can do, and control link access.

```
┌──────────────────────────────┐
│ ╭──────────────────────────╮ │
│ │          ━━━━            │ │
│ │ Share “Budget 2026.xlsx” │ │
│ │ ┌──────────────────────┐ │ │
│ │ │ Add people by email  │ │ │
│ │ └──────────────────────┘ │ │
│ │ [ Invite ]               │ │
│ │ PEOPLE WITH ACCESS       │ │
│ │ (A) Alex (you)     Owner │ │
│ │ (R) Riya Sen  [Editor v] │ │
│ │ (S) Sam Lee  [Viewer v]  │ │
│ │ GENERAL ACCESS           │ │
│ │ Restricted       [Change]│ │
│ │ [ Copy link ]            │ │
│ ╰──────────────────────────╯ │
└──────────────────────────────┘
```

**Components:**

- Email field with chips for each address typed, and contact suggestions below it.
- Primary **Invite** (becomes **Invite 2 people** with a count).
- Person rows: avatar, name, email, role dropdown with **Viewer**, **Commenter**, **Editor**, **Remove access**. The owner row is fixed.
- General access: **Restricted** (only added people) or **Anyone with the link** (with its own role). Optional expiry date and a toggle “Allow downloads”.
- Secondary **Copy link** shows a toast “Link copied”. **States:** *Loading people:* 3 skeleton rows. *Inviting:* Invite button shows a spinner; the new person appears in the list with a slide-in. *Error:* “Couldn’t share. Check the email address” under the field. *No permission to share:* field and dropdowns are disabled, with the text “Only the owner or an editor can change who has access”. *Offline:* Invite and role changes are disabled. **Motion:** sheet slide-up; new person row slides in; the role dropdown opens as a small menu with a 150 ms fade. **Responsive:** a 480 px centered dialog on Medium and Expanded.

### Screen 15 · File viewer (all file types)

**Purpose:** read, play and inspect any file without leaving the app. One shell, six content types.

**Shell (all types):**

```
┌──────────────────────────────┐
│ [<] Resume.pdf          (:)  │
│     PDF · 420 KB             │
│ ┌──────────────────────────┐ │
│ │                          │ │
│ │     (content area)       │ │
│ │                          │ │
│ └──────────────────────────┘ │
│ TOOLS                        │
│ [Find][+][-][Theme][Mark]... │
│ ACTIONS                      │
│ [Down][Share][Star][Off][Inf]│
└──────────────────────────────┘
```

Top bar: back, file name (Title, truncated), type and size (Caption), ⋮ (opens the actions sheet). Tool row and action row scroll horizontally; each tool is a 66 px tall tile with an icon and a 11 px label; an active tool is filled with primary. In landscape and fullscreen the bars hide after 3 s and return on tap.

**15a Image**

```
│ ┌──────────────────────────┐ │
│ │                   [140%] │ │
│ │        ┌──────┐          │ │
│ │        │ img  │          │ │
│ │        └──────┘          │ │
│ └──────────────────────────┘ │
│ [Zoom +][Zoom -][Rotate][Fit]│
```

Pinch and double-tap zoom (up to 300%), drag to pan, swipe left or right for the next image in the folder, zoom badge top-right. Loading: blurred low-resolution thumbnail first, then the full image fades in (200 ms). Error: broken-image icon and **Retry**.

**15b PDF and Word documents**

```
│ ┌──────────────────────────┐ │
│ │ Resume.pdf               │ │
│ │ Page 2 of 6              │ │
│ │ Cloud storage keeps your │ │
│ │ files safe and available │ │
│ └──────────────────────────┘ │
│ [‹] ───●────────────── [›]   │
│ [Find][+][-][Theme][Mark][Print]│
```

Tools: **Find** (search bar with match count and highlight), **Zoom** 80% to 180%, **Theme** (light, sepia, dark paper), **Bookmark** page, **Print**, page slider and arrows. Loading: page-shaped skeleton with 8 shimmer lines. Password-protected: lock icon, password field, **Unlock**. Error: “Can’t open this file” with **Download instead**.

**15c Spreadsheet**

```
│ fx  =B2*C2                   │
│ ┌───┬──────┬───┬─────┬─────┐ │
│ │   │  A   │ B │  C  │  D  │ │
│ │ 1 │ Item │Qty│Price│Total│ │
│ │ 2 │Laptop│ 2 │ 640 │1280 │ │
│ │ 3 │Phone │ 5 │ 310 │1550 │ │
│ └───┴──────┴───┴─────┴─────┘ │
│ [Sheet1][Summary][Export CSV]│
```

Formula bar shows the selected cell; header row and row numbers are sticky; the grid scrolls both ways inside its own container; the selected cell has a 2 px primary outline. Summary tab shows four number cards (Total, Items, Largest, Due).

**15d Video**

```
│ ┌──────────────────────────┐ │
│ │                          │ │
│ │          ( ▶ )           │ │
│ │   Welcome to CloudBox    │ │
│ └──────────────────────────┘ │
│ ───────●──────────────────   │
│ 0:42 / 3:04            1.0x  │
│ [-10s][Play][+10s][Speed]    │
│ [Loop][Mute][Captions][Full] │
```

Controls: play and pause, seek bar, ±10 s, speed 0.5x, 1x, 1.5x, 2x, loop, mute, captions, fullscreen. Buffering shows a ring over the video; poster frame shows before playing.

**15e Audio**

```
│ ┌──────────────────────────┐ │
│ │  ▏▎▍▌▋▊▉▊▋▌▍▎▏▎▍▌▋▍▎▏  │ │
│ └──────────────────────────┘ │
│ ───────●──────────────────   │
│ 1:10 / 4:02                  │
│ [-10s][Pause][+10s][Speed]   │
```

Waveform bars fill with the progress colour as the audio plays; playback continues in the background with a lock-screen control.

**15f Archive (zip)**

```
│ ┌──────────────────────────┐ │
│ │ Backup.zip  5 items      │ │
│ │ [folder] docs/           │ │
│ │ [folder] photos/         │ │
│ │ [file] readme.txt   2 KB │ │
│ └──────────────────────────┘ │
│ [ Extract all ]              │
```

Unsupported file types (for example .dwg) show the file tile, name, size and **Download** and **Open with another app**.

**Viewer states:** loading (type-specific skeleton), error with Retry, offline (only offline files open; others show “Needs internet”), no access. **Responsive:** Expanded: the viewer fills the right pane next to the list; tools move to a vertical rail on the right edge.

### Screen 16 · File details

```
┌──────────────────────────────┐
│ ╭──────────────────────────╮ │
│ │ Details                  │ │
│ │ Name       Resume.pdf    │ │
│ │ Type       PDF           │ │
│ │ Size       420 KB        │ │
│ │ Modified   Yesterday     │ │
│ │ Owner      You           │ │
│ │ Location   My Files      │ │
│ │ Sharing    Private       │ │
│ ╰──────────────────────────╯ │
└──────────────────────────────┘
```

Label Caption in textSecondary (100 px column), value Body. For files shared with me: Shared by, Your access, and Added on. **Loading:** 7 skeleton rows. **Live:** Drive metadata (id, created, last modified by, md5).

### Screen 17 · Offline files

```
┌──────────────────────────────┐
│ [<] Offline files            │
│ 3 files · 12 MB saved        │
│ [pdf]  Resume.pdf   ✓ saved  │
│ [doc]  Notes.docx   ✓ saved  │
│ [mp3]  Podcast ep1  ↻ syncing│
└──────────────────────────────┘
```

Status text: *saved* (green), *syncing* (spinner), *update available* (amber), *failed* (red + Retry). **Empty:** cloud-off illustration, “No offline files”, “Use ⋮ then Make available offline”. Swipe left to remove from offline.

### Screen 18 · Trash

```
┌──────────────────────────────┐
│ [<] Trash                    │
│ Deleted after 30 days        │
│ [pdf] Old invoice.pdf        │
│       12 days left [Restore] │
│ [ Empty trash ]              │
└──────────────────────────────┘
```

**Components:** rows with days left, secondary **Restore** and a trash icon for delete forever; destructive **Empty trash** at the bottom. A confirmation dialog is required before anything is deleted permanently: title “Delete forever?”, text “This can’t be undone”, secondary **Cancel**, filled-error **Delete**. **Empty:** “Trash is empty”. **Live:** restored items return to their original folder (toast shows “Restored to Work”).

### Screen 19 · Storage

```
┌──────────────────────────────┐
│ [<] Storage                  │
│ ╔══════════════════════════╗ │
│ ║ 842 MB used of 15 GB     ║ │
│ ║ ▓▓░░░░░░░░░░░░░░░░░░░░░░ ║ │
│ ╚══════════════════════════╝ │
│ BY TYPE                      │
│ [vid]  Videos   ▓▓▓▓▓░ 84 MB │
│ [zip]  Archives ▓▓▓░░░ 52 MB │
│ [img]  Images   ▓░░░░░  6 MB │
│ [ Clear trash to free 12 MB ]│
└──────────────────────────────┘
```

Bars use each type’s colour. Tapping a row opens Files filtered by that type, sorted by size (largest first). **Loading:** hero and rows are skeletons. **Warning:** above 90% the card turns amber with a **Free up space** secondary button.

### Screen 20 · Settings and profile

```
┌──────────────────────────────┐
│ [<] Settings                 │
│ ╔══════════════════════════╗ │
│ ║ (A) Alex Kumar           ║ │
│ ║ alex@gmail.com           ║ │
│ ║ ▓▓░░░░░░  842 MB / 15 GB ║ │
│ ╚══════════════════════════╝ │
│ ACCOUNT                      │
│ Google Drive      ✓ Linked   │
│ Switch account            >  │
│ APPEARANCE                   │
│ Theme            [System v]  │
│ Default view     [Grid v]    │
│ UPLOADS & DOWNLOADS          │
│ Upload on Wi-Fi only    (●)  │
│ FILE VIEWER                  │
│ Autoplay media          (●)  │
│ NOTIFICATIONS                │
│ SECURITY & PRIVACY           │
│ OFFLINE & CACHE · ABOUT      │
│ [ Sign out ]                 │
└──────────────────────────────┘
```

Sections (Overline headers): Account, Appearance (theme, default view, default sort, language), Uploads and downloads (Wi-Fi only, auto-backup photos, upload quality), File viewer (autoplay, keep screen on), Notifications (new share, upload finished, storage almost full), Security and privacy (app lock, block screenshots, manage sharing links), Offline and cache (offline files, clear cache, trash), About (help, privacy, version). **Sign out** is a destructive-tinted secondary button with a confirmation dialog. Changes apply instantly and show a quick checkmark; theme changes cross-fade the whole app in 250 ms. **Loading:** profile card skeleton; toggles stay interactive because they are stored on the device. **Responsive:** on Expanded settings become a two-column layout (section list left, options right).

### Screen 21 · Global error, offline and permission screens

```
┌──────────────────────────────┐
│ ▒ You are offline. Showing   │
│ ▒ saved files.               │
│                              │
│         (cloud-off)          │
│    No connection             │
│  Connect to the internet     │
│  to see your latest files    │
│        [ Try again ]         │
└──────────────────────────────┘
```

- **Offline banner:** amber tint across the top, 40 px, auto-hides 2 s after the connection returns and syncing starts.
- **Server error:** “Something went wrong on our side” with **Try again** and a small “Details” link that copies the error code.
- **Session expired:** dialog “Please sign in again” with a primary **Sign in** button.
- **Drive permission revoked:** full screen explanation and **Reconnect Google Drive**.
- **Storage full:** amber banner on uploads with **Manage storage**.

## 6. Large-screen layout (Expanded, 840 px and up)

```
┌─────────┬──────────────────────┬──────────────┐
│ CloudBox│ My Files > Work      │ Details      │
│         │ [Search........]     │              │
│ Home    │ ┌─────┐ ┌─────┐ ┌───┐│ [preview]    │
│ Files   │ │folder│ │ pdf │ │img││ Resume.pdf   │
│ Shared  │ └─────┘ └─────┘ └───┘│ 420 KB       │
│ Starred │ ┌─────┐ ┌─────┐ ┌───┐│ Owner: You   │
│ Offline │ │ doc │ │sheet│ │mp4││ [Share][Down]│
│ Trash   │ └─────┘ └─────┘ └───┘│              │
│ Storage │                      │              │
└─────────┴──────────────────────┴──────────────┘
   Drawer 280     Content (flexible)   Pane 360
```

The drawer replaces the bottom bar; the FAB becomes a **+ New** button at the top of the drawer; the right pane shows details, share settings or the viewer for the selected file; below 840 px the pane becomes a full screen.

## 7. How the screens look with real, live data

| Area | Mock data (now) | Live data (backend connected) |
| --- | --- | --- |
| File list | Fixed sample array, 600–1200 ms fake delay | Drive `files.list` with page size 30, cached in SQLite; list shows cache first, then merges |
| Thumbnails | Emoji or colour tile by type | Drive `thumbnailLink` or generated preview; fades in over the type tile |
| Names and dates | Sample strings | Real names (ellipsis in the middle), dates as Today, Yesterday, then 12 Mar |
| Sizes | Hard-coded | Formatted bytes (KB, MB, GB, one decimal) |
| Storage | 842 MB of 15 GB | Drive `about.storageQuota` |
| Sharing | Sample people | Drive `permissions` (role, email, photo, link access) |
| Search | Local filter | Drive query (name contains, mimeType) with debounce |
| Uploads | Timer-driven progress | Resumable upload with real progress, retries, background service |
| Changes by others | None | Drive `changes` feed polled every 30 s in the foreground or push notification; changed rows animate in |
| Errors | Simulated by a debug switch | Real HTTP codes mapped to friendly messages (401 sign in, 403 no access, 404 gone, 429 slow down, 5xx server) |

**Layout stability rules for live data:** (1) reserve the thumbnail and text space with the skeleton so nothing jumps; (2) update rows by file id, never rebuild the whole list; (3) keep the scroll position when new items arrive and show a “New items ↑” chip instead of scrolling the user; (4) format long names with a middle ellipsis; (5) handle very long lists with virtualised scrolling; (6) show relative dates in the user’s locale and time zone.

## 8. Mock data set

| id | Name | Type | Parent | Size | Modified | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Work | folder | root | – | 5 d | contains Projects |
| 2 | Photos | folder | root | – | 3 d | shared |
| 3 | Study | folder | root | – | 9 d |  |
| 4 | Resume.pdf | pdf | root | 420 KB | 1 d | starred |
| 5 | Budget 2026.xlsx | sheet | root | 310 KB | 2 d | shared |
| 6 | Trip video.mp4 | video | root | 84 MB | 4 d |  |
| 7 | Podcast ep1.mp3 | audio | root | 9.2 MB | 6 d |  |
| 8 | Notes.docx | doc | root | 55 KB | today | starred |
| 9 | Projects | folder | Work | – | 7 d | nested folder |
| 12 | App design.pdf | pdf | Projects | 1.8 MB | 1 d |  |
| 13 | Backup.zip | zip | Projects | 52 MB | 8 d |  |
| 14 | Beach.jpg | image | Photos | 3.4 MB | 3 d |  |
| 30 | Team plan.docx | doc | shared with me | 80 KB | today | Riya Sen, Editor |
| 31 | Brand kit.png | image | shared with me | 2.6 MB | 1 d | Sam Lee, Viewer |
| 32 | Q4 Report.pdf | pdf | shared with me | 2.4 MB | 3 d | Maya Rao, Commenter |

Debug switches for design review: `FORCE_LOADING`, `FORCE_EMPTY`, `FORCE_ERROR`, `FORCE_OFFLINE`, `SLOW_NETWORK`, so every state in this document can be opened on demand.

## 9. Quality checklist for every screen

- Loading skeleton, empty, error, offline and permission states are designed and implemented.
- Works in light and dark, at 200% font size, in portrait and landscape.
- Touch targets at least 48 px; text contrast at least 4.5:1; every icon button has a label for screen readers.
- Back gesture always goes up one level; destructive actions can be undone or need confirmation.
- Reduced-motion setting is respected.
- Same layout with mock and live data (no layout shift).
