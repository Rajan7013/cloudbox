# CloudBox Android file viewers

## What ships in 1.1

CloudBox opens the selected Drive item or its saved offline copy. The Android app renders the bytes it receives. Private PDF ink belongs to the signed-in account, Drive item and revision on this device. It is separate from the original file and from Drive comments. PDF page images contain the original page.

| Viewer | Content and tools in this APK | Setting or limit |
|---|---|---|
| PDF | Continuous vertical scroll or horizontal pages, full screen, zoom, page-number jump, thumbnail strip, bookmarked pages, word search with previous/next and highlighted rectangles, read aloud, local ink, PNG export | Page direction in Viewer settings. Light, dark and sepia paper in the reader. Saved reading position per revision. |
| Word and Google Docs | DOCX and ODT paragraph text with find, match navigation, text scaling and theme. Native Google Docs use the existing Drive PDF export and its page tools. | Reading theme. DOCX/ODT text view omits floating objects and pagination. |
| PowerPoint and Google Slides | PPTX and ODP slide text in tabbed presentation order. Select a slide, find text, zoom type or enter full screen. Native Google Slides retain their visual layout through PDF export. | Reading theme. Slide-text previews omit images, animations, transitions and speaker notes. |
| Excel and Google Sheets | XLSX and ODS named sheet tabs, cell addresses and values, row filtering, column sort, numeric summary. CSV and TSV open in the grid. Online Google Sheets use Drive's XLSX export for all workbook tabs. Previously saved CSV offline copies show their first sheet. | Choose grid or summary. Workbook preview covers the first 500 rows and 128 columns per sheet. CSV/TSV preview covers 500 records. XLSX shows stored results for formula cells. |
| Markdown | Rendered CommonMark and source, case-insensitive find, occurrence count, previous/next, text scaling and paper themes. | Choose rendered or source on open. Raw HTML displays as text. |
| Text and code | Plain-text preview, highlighted occurrences, previous/next with scroll-to-match, font size and paper theme. | Default text size. A visible notice marks previews truncated at 250,000 characters. |
| Image | Pinch, double-tap, fit/fill, rotation, zoom reset, dimensions and MIME when available, previous/next for the actual folder selection. | Choose fit or fill. Rotation affects the preview. |
| Video | Media3 playback, seek, skip, variable speed, repeat, mute, sleep timer and full screen. | Choose default skip interval. Media controls use the player's actual position and duration. |
| Audio | Media3 playback, seek, skip, speed, mute, repeat, sleep timer and full screen. | Independent default skip interval. Playback status comes from the player. |

### Reading and marking a PDF

Open the pen toolbar, choose the round pen, square marker or angled calligraphy nib, and select a colour and width. Highlighter has its own palette, width and opacity. Eraser removes intersecting strokes with an adjustable radius. Undo removes the last stroke on the active page, and Clear removes that page's ink. Return to View & Zoom to navigate.

Find highlights occurrences on the page. Orange marks the selected result and yellow marks the others. Previous and Next move across pages and wrap. For pages without a text layer, open Find and choose **Recognize scanned pages**. Recognition runs on the phone using the bundled Latin-script model. It can be stopped, and its results last for the current viewing session. Recognition quality depends on scan contrast, rotation and language.

Tap the page counter in the bottom bar. Enter a number or select a thumbnail, then tap **Go**. Use **Bookmarked pages** to narrow the thumbnail strip. Select **Save original page as PNG** for the Android location picker, or **Save page to gallery** for Pictures/CloudBox on Android 10 and newer. Output scales to twice the PDF page dimensions, capped at 3,200 pixels on the longest edge.

### Layouts in the APK

The Folder tool for image, video and audio displays loaded siblings of the same type and parent. Search by filename, sort by name, size or modification time, reverse the order, or narrow the list to starred or offline items. Document text search also offers case sensitivity and whole-word matching.

Each bracket represents a control or content region. The controls depend on the file and its Drive capabilities. The detailed design reference below includes separate drawings for each target tool and settings screen.

```text
PDF
[Back] [Document title and page]
[Settings] [Direction] [Find] [Read aloud] [Annotate] [More]
[Find query] [Occurrence] [Previous] [Next]       when finding
[View] [Pen] [Highlighter] [Eraser] [Undo] [Clear]
[Nib / Colour / Width / Opacity or eraser radius]
[                 PDF page and ink                  ]
[Previous] [Page / total] [Next] [Slider] [Full] [Bookmark] [Theme]

Page chooser
[Page number] [Bookmarked pages]
[Thumbnail] [Thumbnail] [Thumbnail] ...
[Save original page as PNG...] [Save page to gallery]
[Status]                                  [Close] [Go]

Word / Docs                  PowerPoint / Slides
[Title] [Settings] [Full]     [Title] [Settings] [Full]
[Paragraphs or PDF page]      [Slide tabs or paginated Google export]
[Find] [Size] [Paper theme]   [Slide text or visual slide] [Find]

Excel / Sheets               Markdown
[Named worksheet tabs]       [Title] [Settings] [Full]
[Filter rows] [Cell value]   [Rendered document / source]
[Column and row headers]     [Find query] [Occurrence] [Prev] [Next]
[Grid] [Sort] [Summary]      [Rendered / Source] [Find] [Size] [Theme]

Text / code                  Image
[Title] [Settings] [Full]     [Title] [Settings] [Full]
[Find query and navigation]  [Previous] [Actual image] [Next]
[Text with highlighted hits] [Dimensions / zoom]
[Find] [Size] [Theme]         [Info] [Fit / Fill] [Zoom] [Rotate] [Reset]

Video                        Audio
[Title] [Settings] [Full]     [Title] [Settings] [Full]
[Video frame]                [File name and playback state]
[Seek] [Time / duration]      [Seek] [Time / duration]
[Skip] [Play] [Skip]          [Skip] [Play] [Skip]
[Speed] [Loop] [Mute]         [Speed] [Loop] [Mute]
[Skip interval] [Sleep]       [Skip interval] [Sleep]

Settings > Viewer settings
[PDF] [Docs] [Slides] [Sheets] [Markdown] [Text] [Image] [Video] [Audio]
[Selected viewer and opening preference]
[Available choices]
[Restore default] [Done]
```

### Backend and permissions

Reading, searching, OCR, private ink, image export, media playback and viewer preferences run inside Android. Existing Drive authentication, file download and Workspace export supply the data. An existing offline copy can open without a network connection. The update adds one local Room migration to retain Drive's download capability. Page export and viewer download actions use that capability. Existing owner and sharing checks still apply.

Shared annotations across devices would need a shared store with access checks, or Drive-backed sidecar files with explicit permission handling. Collaborative editing and suggestions need the corresponding Google editor or a supported write API, including concurrency and revision handling. Full visual fidelity for arbitrary uploaded Office documents would need a compatible renderer SDK or a conversion service. A custom server is one option for conversion. The current reader does not upload documents to a conversion service.

### Scope of the design reference

The remainder preserves the requested target layouts, settings, filters, tools and permission model for future implementation. Compare it with the release table above when checking what can be used today. Work still open includes collaborative editing and comments, PDF merge/split and form filling, legacy binary Office rendering, password unlock, advanced Markdown extensions, background media sessions, Cast, external subtitles, equalizer, and the full accessibility and device acceptance matrix. Values and filenames in the wireframes illustrate layouts only.

---

## Target design reference

Version 2.0 · Covers every viewer: **PDF, Word / Google Docs, PowerPoint / Google Slides, Excel / Google Sheets, Markdown, Text, Image, Video, Audio**.
For each viewer: full-screen layout (ASCII), dedicated tools, settings, search / sort / filter, states, gestures and permission rules.

How to read the ASCII: `[ ]` button · `( )` chip or round control · `░` highlight or empty · `▓` filled · `~~~` pen stroke or skeleton · `(:)` more menu · `(S)` search icon. Layouts are drawn for a 360 px wide phone in portrait.

---

## 0. Contents
1. Common viewer shell
2. Viewer index (file type to viewer)
3. PDF viewer (reading, search, page jump, annotate, extract, tools, settings)
4. Word and Google Docs viewer
5. PowerPoint and Google Slides viewer
6. Excel and Google Sheets viewer
7. Markdown viewer
8. Text file viewer
9. Image viewer
10. Video viewer
11. Audio viewer
12. Global viewer settings hub
13. Permission rules inside viewers
14. Technical notes
15. Delivery checklist

---

## 1. Common viewer shell

Every viewer uses the same frame so the app feels consistent.

```
┌──────────────────────────────┐
│ [<] File name.ext  (S) (:)   │  <- top bar (auto-hides)
│     PDF · 649 KB · Viewer    │  <- type, size, your role
│ ┌──────────────────────────┐ │
│ │                          │ │
│ │     CONTENT AREA        │ │  <- type specific
│ │                          │ │
│ └──────────────────────────┘ │
│ [Tool][Tool][Tool][Tool][...]│  <- bottom tool dock
└──────────────────────────────┘
```

**Top bar:** back, file name (middle-ellipsis), type · size · your role badge, search, more menu (:).
**More menu (all viewers):** Share · Download · Save to device · Make available offline · Star · Rename (if allowed) · Details · Open with another app · Print (documents) · Report a problem.
**Bottom tool dock:** 5 visible tools, horizontal scroll for more, each tool tile 64 px with icon + 11 px label. Active tool = filled primary.

**Immersive mode:** one tap on the content hides the top bar and dock (fade 200 ms); tap again shows them. The system bars hide too. Video and image start in immersive mode, documents start with bars visible.

**Common gestures:** pinch to zoom · double tap to zoom to fit or 2.5x · long press to select · swipe from left edge to go back · volume keys can turn pages (setting).

**Common states**
| State | What the user sees |
|---|---|
| Loading | Type specific skeleton (page shape, grid, waveform), shimmer 1.2 s, cancel button after 5 s |
| Password protected | Lock icon, password field, primary **Unlock**, wrong password error under the field |
| Unsupported type | File tile, name, size, primary **Open with another app**, secondary **Download** |
| Error / corrupt | “Can’t open this file” + **Retry** + **Download instead** |
| Offline | Only offline files open; others show “Needs internet” with **Save offline** hint |
| No access | Lock icon, “You don’t have access”, **Request access** |
| Large file | Progress ring with percentage and size, opens as soon as first part is ready |

**Responsive:** Compact phones use full-screen viewer. Medium (600+) puts tools on a left or right rail. Expanded (840+) shows the viewer in the right pane with the file list on the left, tool rail on the far right, and properties panel optional.

**Accessibility:** every tool has a text label for screen readers, 48 px touch targets, content scales with system font size, reduced motion disables page-turn animation, high contrast theme available.

---

## 2. Viewer index

| Viewer | Extensions and sources |
|---|---|
| PDF | .pdf |
| Docs | .doc .docx .odt .rtf, Google Docs (exported on open) |
| Slides | .ppt .pptx .odp, Google Slides |
| Sheets | .xls .xlsx .ods .csv .tsv, Google Sheets |
| Markdown | .md .markdown |
| Text | .txt .log .json .xml .yaml .ini .csv (as text) .html .css .js .py .java .kt .c .cpp and other code files |
| Image | .jpg .png .gif .webp .heic .bmp .svg .raw (DNG) |
| Video | .mp4 .mkv .webm .mov .avi .3gp |
| Audio | .mp3 .m4a .wav .flac .ogg .aac .opus |

---

## 3. PDF viewer

### 3.1 Full-screen reading view

```
┌──────────────────────────────┐
│ [<] 3D III notes.pdf (S)(:)  │  top bar
│ ┌──────────────────────────┐ │
│ │ Selective Laser          │ │
│ │ Sintering (SLS)          │ │
│ │ ░░░░░░ highlighted ░░░░  │ │
│ │ ┌────────┐               │ │
│ │ │ figure │               │ │
│ │ └────────┘               │ │
│ │ ~~~ pen stroke ~~~       │ │
│ └──────────────────────────┘ │
│            ┌────────┐        │
│            │ 1 / 22 │        │  page pill
│            └────────┘        │
│ [Find][Pen][Mark][Pages][...]│  bottom tool dock
└──────────────────────────────┘
```
- **Page pill** (1 / 22) shows the current page while scrolling and fades after 1.5 s. Tap it to open **Go to page**.
- **Thin scroll handle** on the right edge with a page bubble while dragging.
- Page rendering: low-resolution preview first, sharp page fades in (150 ms). Pages beyond the screen are pre-rendered one ahead.
- Zoom: 50% to 500%. Double tap: fit width, 200%, fit page, cycling. Pan when zoomed. Page edges snap when swiping horizontally at fit width.
- Annotations always draw on top of the page and stay attached to the page when zoomed or rotated.

### 3.2 Reading modes (swipe or scroll)

| Mode | How it moves | Best for |
|---|---|---|
| **Vertical scroll (continuous)** | Swipe up or down, pages stacked with a 8 px gap, momentum scrolling | Reading long notes, default |
| **Horizontal swipe (single page)** | Swipe left or right to change page with a slide or curl animation, page snaps | Slides, scanned books, presentations |
| **Vertical single page** | Swipe up or down changes whole page | Small phones with large text |
| **Two-page spread** | Landscape or tablet shows pages 2-3 side by side | Books, tablets |
| **Auto-scroll** | Page scrolls by itself, speed slider 1 to 10 | Hands-free reading |

```
Mode switcher (sheet from the Pages or View tool)
┌──────────────────────────────┐
│ View mode                    │
│ (Scroll v)(Swipe >)(Single v)│
│ Page fit   (Width)(Page)     │
│ Transition (Slide)(Curl)(None)│
│ Spread     [ Auto ]          │
│ Auto-scroll ▓▓▓░░░░░  3      │
└──────────────────────────────┘
```
Switching mode keeps the current page. In swipe mode the edge arrows ‹ › also work, and tapping the left or right 20% of the screen turns the page (setting).

### 3.3 Search in document

```
┌──────────────────────────────┐
│ [<] ┌──────────────────────┐ │
│     │ sintering        (x) │ │  search field
│     └──────────────────────┘ │
│ 12 matches · 5 pages   [^][v]│  count + previous / next
│ (Aa)(Whole word)(Regex off)  │  options
│ ┌──────────────────────────┐ │
│ │ Selective Laser          │ │
│ │ ▒▒▒▒▒▒▒▒▒▒ (current)     │ │  current match, orange
│ │ powder ░░░░░░░░ fuse     │ │  other matches, yellow
│ └──────────────────────────┘ │
│ [ Results list ]             │
└──────────────────────────────┘
```
- Results appear while typing (250 ms debounce). **All matches are highlighted yellow**, the **current match is orange** and the page scrolls so it is centered.
- Next / previous buttons (or Enter / Shift+Enter) jump between matches, including across pages, and wrap at the end (“Search restarted from the first page”).
- **Results list sheet:** each row = page number + a text snippet with the match in bold; tap to jump. Group by page. Sort: page order (default) or best match.
- Options: match case, whole word, include annotations text, include bookmarks. Regex is an advanced option.
- **Scanned PDFs:** if no text layer, the bar shows “This PDF is a scan. Run OCR to search” with **Run OCR** (progress bar per page, cancel allowed); after OCR the search works and the text is cached.
- **No results:** “No results for ‘sintering’”, tip “Check spelling or turn off Whole word”.
- Recent searches appear as chips when the field is empty (last 5, clear all).
- Closing the search keeps the highlights until the user taps **Clear highlights** (a small chip) so they can keep reading with matches visible.

### 3.4 Page jump and navigation

```
┌──────────────────────────────┐
│ ╭──────────────────────────╮ │
│ │          ━━━━            │ │
│ │ (Pages)(Outline)(Marks)  │ │  tabs
│ │ Go to page [ 12 ] [ Go ] │ │
│ │ ───●───────────── 12/22  │ │  slider
│ │ ┌───┐ ┌───┐ ┌───┐ ┌───┐  │ │
│ │ │ 1 │ │ 2 │ │ 3 │ │ 4 │  │ │  thumbnails
│ │ └───┘ └───┘ └───┘ └───┘  │ │
│ │ ┌───┐ ┌───┐ ┌───┐ ┌───┐  │ │
│ │ │ 5 │ │ 6 │ │ 7 │ │ 8 │  │ │
│ │ └───┘ └───┘ └───┘ └───┘  │ │
│ ╰──────────────────────────╯ │
└──────────────────────────────┘
```
- **Pages tab:** thumbnail grid (3 columns portrait), current page has a primary border, pages with annotations show a pen dot, bookmarked pages a ribbon. Long press a thumbnail to select several pages (used for extract, rotate, delete, reorder).
- **Outline tab:** table of contents tree from the PDF headings (expand and collapse); if none, “No outline in this file”.
- **Bookmarks tab:** user bookmarks with page and optional name; swipe to delete; add with the ribbon icon in the dock.
- **Go to page:** numeric field (validates 1 to total), pressing Go jumps with a short animation. The bottom slider shows live page number while dragging.
- **Back to previous position:** after any jump a small chip “↩ Back to page 3” appears for 5 s.
- **Remember last page:** reopening the file returns to the last page and zoom (setting, on by default).

### 3.5 Annotation tools (highlighter, pens, brushes, eraser)

Tap **Mark** in the dock to open the annotation toolbar. It slides up above the dock and the page stays fully visible.

```
┌──────────────────────────────┐
│ [<] 3D III notes.pdf   [Done]│
│ ┌──────────────────────────┐ │
│ │ ░░░░░░░░░░░░░░░░░░░░░░░░ │ │
│ │ ~~~~~~~~ pen ~~~~~~~~    │ │
│ │ page content             │ │
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │
│ │(●)(●)(●)(●)(●)(●)(●)(+)  │ │  colour palette
│ │ Size  ───●───────  4 px  │ │  thickness slider
│ │ Opacity ─────●───  100%  │ │
│ ├──────────────────────────┤ │
│ │[Sel][Pen][Brush][Mark][Er]│ │  tools
│ │[Text][Shape][Note][Sign]  │ │
│ │ [Undo][Redo]    [Clear pg]│ │
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

**Toolbar rows:** (1) colour palette, (2) size and opacity sliders with a live stroke preview, (3) tool row, (4) undo, redo, clear page.

| Tool | What it does | Tip / size range | Default | Notes |
|---|---|---|---|---|
| **Select / Pan** | Move, resize or delete an annotation, or scroll the page | – | – | Tap an annotation to see handles and a floating menu: colour, thickness, copy, delete |
| **Pen** | Fine ballpoint line, smooth curves | 1 to 12 px | 3 px | Velocity smoothing, pressure sensitive with stylus |
| **Brush** (sub-types) | Marker (flat, 6 to 30 px), Calligraphy (angled tip), Pencil (textured, 1 to 8 px), Fountain (pressure varies width) | see type | Marker 12 px | Long press the Brush icon to pick the sub-type |
| **Highlighter (free)** | Wide translucent stroke over text or drawings | 8 to 40 px | 18 px | 40% opacity, multiply blend so text stays readable, flat chisel tip |
| **Highlighter (text)** | Select words, tap **Highlight** in the floating menu | – | – | Snaps to text lines and follows line breaks; also Underline, Strikethrough, Squiggly |
| **Eraser** | Removes annotations | 8 to 48 px | 20 px | Modes: **Stroke eraser** (removes the whole stroke you touch), **Precise eraser** (rubs out only the touched part), **Clear page** (with confirmation and Undo) |
| **Text box** | Type on the page | 8 to 48 pt, font, colour, bold, italic | 14 pt | Drag to move, handles to resize |
| **Shapes** | Line, arrow, rectangle, ellipse, polygon | Same size and colour controls | – | Hold still at the end of a stroke to snap to a straight line or perfect shape |
| **Sticky note** | A comment pin with text | – | Yellow | Shows as an icon, tap to read |
| **Signature** | Draw, type or import a signature, place and resize it | – | – | Saved signatures list in settings |

**Colour palette:** 12 colours + custom picker + 5 recents.
- Pen / brush: black #111827, blue #2563EB, red #DC2626, green #16A34A, purple #7C3AED, orange #EA580C, brown #92400E, grey #6B7280, white, plus custom.
- Highlighter: yellow #FACC15, green #4ADE80, pink #F472B6, blue #60A5FA, orange #FB923C, purple #C084FC (all at 40% opacity).
- Tapping the selected colour again opens the full picker (hue slider, brightness, hex field, eyedropper from the page).

**Thickness (size) controls:** slider with – and + steps (1 px per tap), live preview dot that shows the real size, size presets as 3 dots (thin, medium, thick). Each tool remembers its own size and colour.

**Eraser controls:**
```
┌──────────────────────────────┐
│ Eraser                       │
│ (Stroke)(Precise)            │
│ Size  ────●────────  20 px   │
│ [ Clear this page ]          │
└──────────────────────────────┘
```
A circle outline follows the finger to show the eraser size. A highlighter eraser option “Erase only highlights” is available so pen notes stay untouched.

**Input behaviour**
- **Stylus-only drawing** (setting): finger scrolls and zooms, stylus draws; palm rejection on.
- **Two-finger tap** = undo, **three-finger tap** = redo (setting).
- Quick switch: a stylus button toggles pen and eraser.
- Drawing near the screen edge scrolls the page automatically.

**Saving annotations**
| Where | When | What happens |
|---|---|---|
| **Autosave on device** | Every stroke | Stored as vector data beside the file, so they reopen exactly and can be edited |
| **Save to Drive** | The user is Owner or Editor and taps **Save** | Writes a flattened copy or updates the original (choice dialog: “Update original” or “Save as copy”) |
| **Viewer / Commenter role** | Always | Annotations stay private on the device, or saved as a copy. The original is never changed |
| **Export** | More menu | “Export with annotations” (flattened PDF) or “Export without annotations” |

**Annotation list:** the Marks tab in the Pages sheet lists every annotation (type icon, page, preview). Filter chips: All, Highlights, Pen, Notes, Text. Tap a row to jump to it, swipe to delete.

### 3.6 Extract a page as an image and save to the device

Triggered from: Pages sheet (select pages) → **Extract**, the More menu → **Extract pages**, or long press a page → **Save page as image**.

```
┌──────────────────────────────┐
│ ╭──────────────────────────╮ │
│ │          ━━━━            │ │
│ │ Extract pages            │ │
│ │ Pages  (Current)(Range)  │ │
│ │        (Selected)(All)   │ │
│ │ Range  [ 3 ] to [ 7 ]    │ │
│ │ Format (PNG)(JPG)(WebP)  │ │
│ │ Quality  ───────●─ 90%   │ │
│ │ Resolution (150)(300)(600)│ │  DPI
│ │ Annotations   [ on  (●)] │ │
│ │ Save to                  │ │
│ │ [ Gallery > CloudBox ] v │ │
│ │ [ Cancel ]  [ Extract ]  │ │
│ ╰──────────────────────────╯ │
└──────────────────────────────┘
```
| Option | Choices | Default |
|---|---|---|
| Pages | Current, Range, Selected, All | Current |
| Format | PNG (sharp, large), JPG (small), WebP | PNG |
| Quality | 50 to 100% (JPG and WebP) | 90% |
| Resolution | 72, 150, 300, 600 DPI (shows estimated size) | 150 |
| Include annotations | on / off | on |
| Background | White or transparent (PNG) | White |
| Destination | **Gallery** (Pictures/CloudBox), **Choose folder…** (system folder picker), **Share** | Gallery |
| Also as | A new **PDF with only these pages** | off |

**Flow**
1. User taps **Extract**. A progress sheet shows “Saving page 2 of 5” with a bar and **Cancel**.
2. Files are named `3D III notes_page_3.png` (a counter is added if the name exists).
3. On Android the image is saved through the MediaStore API into `Pictures/CloudBox`, so it appears in the **device gallery immediately** with no storage permission on Android 10+. With **Choose folder…** the app uses the system folder picker (Storage Access Framework) and remembers the folder. On iOS the image is added to Photos (add-only permission).
4. Success toast: “5 images saved to Gallery” with actions **View** (opens the gallery) and **Share**.
5. Errors: “Not enough space on this device” (with **Manage storage**), “Folder no longer available” (pick another), “Permission needed” (open settings).

Large page counts (more than 20) run in the background with a notification (“Extracting 3D III notes: 12 of 30”).

### 3.7 Other PDF tools

Collected under the **More tools** sheet in the dock (grid of tiles):

| Tool | Description |
|---|---|
| **Rotate pages** | Rotate current or selected pages 90° left or right, saved on export |
| **Delete pages** | Remove selected pages (confirm + Undo) |
| **Reorder pages** | Drag thumbnails into a new order |
| **Insert blank page / from another PDF** | Add a page before or after |
| **Merge PDFs** | Choose files, reorder, create a new PDF |
| **Split PDF** | By range, every N pages, or by bookmarks |
| **Extract as PDF** | Create a new PDF from selected pages |
| **Compress** | Low, Medium, High compression with estimated size |
| **Protect / Unlock** | Add or remove a password and set print / copy restrictions |
| **Fill form** | Detects form fields, tap to type, checkboxes, dropdowns, **Clear form** |
| **Sign** | Place saved signature and date |
| **Crop margins** | Auto-crop white margins for easier reading on small screens |
| **Reflow / Text mode** | Shows extracted text with adjustable font size (good for small phones) |
| **Read aloud** | Text to speech, speed, voice, highlights the sentence being read |
| **Translate** | Translate selected text |
| **Copy text / Select all** | Long press to select; floating menu: Copy, Highlight, Note, Share, Define |
| **Extract images** | Save embedded images from the PDF |
| **Print** | System print dialog with page range |
| **Share** | Link (with role), original file, or flattened copy |
| **Details** | Title, author, pages, size, created, modified, PDF version, encryption |
| **Night mode** | Inverts colours with an option to keep images unchanged |
| **Bookmark page** | Ribbon toggle |

### 3.8 PDF settings

```
┌──────────────────────────────┐
│ [<] PDF settings             │
│ DISPLAY                      │
│ Default mode      [Scroll v] │
│ Page fit          [Width v]  │
│ Theme   (Light)(Sepia)(Dark) │
│ Night mode keeps images (●)  │
│ Brightness  ─────●──── 80%   │
│ NAVIGATION                   │
│ Tap edges to turn page  (●)  │
│ Volume keys turn page   ( )  │
│ Remember last page      (●)  │
│ Page transition  [Slide v]   │
│ ANNOTATION                   │
│ Stylus only drawing     ( )  │
│ Palm rejection          (●)  │
│ Default pen colour  (●)      │
│ Default highlighter (●)      │
│ Snap shapes             (●)  │
│ EXPORT                       │
│ Default image format [PNG v] │
│ Default DPI         [150 v]  │
│ Save images to [Gallery v]   │
│ OTHER                        │
│ Keep screen on          (●)  │
│ Read aloud voice  [Default]  │
│ Clear cached pages           │
└──────────────────────────────┘
```
Brightness here is the in-app dimming overlay. All changes apply instantly and can be reset with **Restore defaults**.

---

## 4. Word and Google Docs viewer

### 4.1 Layout

```
┌──────────────────────────────┐
│ [<] Team plan.docx (S)(:)    │
│     1,248 words · 6 min read │
│ ┌──────────────────────────┐ │
│ │ Team plan                │ │
│ │ 1. Overview              │ │
│ │ Cloud storage keeps your │ │
│ │ files safe and available │ │
│ │ ░░░░░░░ comment ░░░░ [1] │ │
│ │ 2. Goals                 │ │
│ │ ...                      │ │
│ └──────────────────────────┘ │
│ [Find][Outline][Aa][Theme][..]│
└──────────────────────────────┘
```

### 4.2 Modes
| Mode | Description |
|---|---|
| **Reading (reflow)** | Text reflows to fit the screen, adjustable font, best for phones (default) |
| **Print layout** | Exact page layout with page breaks, headers and footers, zoom and pan |
| **Viewing** (Google Docs) | Clean read-only view |
| **Suggesting** | Editors and commenters add suggestions (shown as coloured text) |
| **Editing** | Opens the Docs editor (in Google Docs app or inline for Editors) |

Google Docs files (native Docs format) are fetched through the Drive API: **exported as DOCX or PDF** for in-app viewing, with **Open in Google Docs** as the editing path. Embedded images, tables, lists, headings and links are preserved; unsupported objects (some drawings, smart chips) show a placeholder.

### 4.3 Tools
| Tool | Description |
|---|---|
| **Find** (and **Replace** for editors) | Match count, next / previous, highlights all (yellow) and current (orange), options: match case, whole word |
| **Outline** | Headings tree, tap to jump, current section highlighted |
| **Text (Aa)** | Font size slider 10 to 32, font family, line spacing, paragraph spacing, margins |
| **Theme** | Light, Sepia, Dark, Black (AMOLED), High contrast |
| **Comments** | Side list of comments with author, time, replies; Viewers see them, Commenters and Editors can add and **resolve** (Drive comments API) |
| **Highlight / Underline** | Select text and use the floating menu; stays local for Viewers |
| **Suggest edits / Edit** | Depending on role |
| **Version history** | List of versions with date and author, tap to preview, **Restore** (Editors) |
| **Word count** | Words, characters, pages, reading time |
| **Read aloud** | TTS with sentence highlight |
| **Translate** | Translate document or selection |
| **Download as** | PDF, DOCX, TXT, HTML |
| **Print / Share / Details** | Standard |
| **Page jump** | Slider or “Go to page” in print layout |

**Floating selection menu:** Copy · Highlight · Comment · Define · Translate · Share · Search web.

### 4.4 Search / sort / filter
- Search: described above, plus results list (heading + snippet).
- Comments panel: filter **All / Open / Resolved / Mine**, sort **Newest / Oldest / Position in document**.
- Version history: sort newest first; filter **Named versions**.

### 4.5 Settings
| Section | Settings |
|---|---|
| Display | Default mode (Reading, Print), font, size, line spacing, margins, theme, text alignment (original, left, justify) |
| Behaviour | Remember scroll position, keep screen on, show word count bar, show comments inline, show tracked changes |
| Google Docs | Open Google Docs files in: (In-app viewer, Google Docs app), export format (DOCX, PDF) |
| Accessibility | Dyslexia-friendly font, bigger tap targets, read-aloud voice and speed |
| Offline | Auto-cache last 10 opened documents |

---

## 5. PowerPoint and Google Slides viewer

### 5.1 Layout

```
┌──────────────────────────────┐
│ [<] Pitch deck.pptx (S)(:)   │
│     Slide 2 of 5             │
│ ┌──────────────────────────┐ │
│ │                          │ │
│ │   The problem            │ │  16:9 slide
│ │   Files are scattered    │ │
│ │                          │ │
│ └──────────────────────────┘ │
│ ┌──┐┌──┐┌──┐┌──┐┌──┐         │  filmstrip
│ │1 ││2 ││3 ││4 ││5 │         │
│ └──┘└──┘└──┘└──┘└──┘         │
│ [Prev][Next][Grid][Notes][Play]│
└──────────────────────────────┘
```

### 5.2 Modes
| Mode | Description |
|---|---|
| **Slide view** | One slide at a time, swipe left or right to change slide |
| **Grid overview** | All slides as thumbnails, tap to jump, long press to select |
| **Outline** | Slide titles and text as a list (good for reading and searching) |
| **Presenter mode** | Full-screen landscape, slide + timer + notes, laser pointer and pen |
| **Reading (vertical)** | Slides stacked vertically like a document |

### 5.3 Tools
| Tool | Description |
|---|---|
| **Next / Previous / Go to slide** | Buttons, swipe, volume keys, or enter a number |
| **Notes** | Speaker notes panel under the slide (size adjustable) |
| **Laser pointer** | Touch and hold to show a red dot with fading trail |
| **Pen and highlighter on slides** | Same tools, colours and thickness as PDF (3.5); strokes can be kept or cleared per slide |
| **Auto-play** | Advance every 3, 5, 10, 30 seconds, loop on / off |
| **Timer** | Elapsed and countdown timer in presenter mode |
| **Transitions** | Play original transitions or disable them |
| **Slide numbers** | Show or hide |
| **Cast / external display** | Mirror or present on a TV or projector, phone becomes remote with notes |
| **Export** | As PDF, as images (same Extract sheet as 3.6), or “Save current slide as image” |
| **Search** | Finds text in slides and notes, shows slide number and snippet, highlights match |
| **Share / Download / Print / Details** | Standard |
| **Google Slides** | “Open in Google Slides”, “Present in Google Slides”, comments panel |

### 5.4 Search / sort / filter
Search result list grouped by slide; Grid overview filter: **All / With notes / With comments / Hidden slides**. Hidden slides are shown with a slashed-eye icon and are skipped in play mode (setting).

### 5.5 Settings
| Section | Settings |
|---|---|
| Display | Default mode, slide fit (fit screen, fill), background (black, theme), slide numbers |
| Presenter | Show notes, show timer, laser colour (red, green, blue), pen default colour, keep screen on |
| Playback | Auto-advance interval, loop, play transitions, skip hidden slides |
| Navigation | Tap edges to change slide, volume keys, swipe direction |
| Export | Image format, DPI, include notes page |

---

## 6. Excel and Google Sheets viewer

### 6.1 Layout

```
┌──────────────────────────────┐
│ [<] Sales sheet.xlsx (S)(:)  │
│ fx │ =B2*C2                  │  formula bar
│ ┌───┬──────┬───┬─────┬─────┐ │
│ │   │  A   │ B │  C  │  D  │ │  sticky header
│ │ 1 │ Item │Qty│Price│Total│ │
│ │ 2 │Laptop│ 2 │ 640 │1280 │ │  selected cell outlined
│ │ 3 │Phone │ 5 │ 310 │1550 │ │
│ └───┴──────┴───┴─────┴─────┘ │
│ Sum 2830 · Avg 1415 · Count 2│  status bar for selection
│ [Sheet1][Summary][Chart] [+] │  sheet tabs
│ [Find][Filter][Sort][Freeze][..]│
└──────────────────────────────┘
```

### 6.2 Tools
| Tool | Description |
|---|---|
| **Cell select / range select** | Tap a cell, drag handles to select a range; status bar shows Sum, Average, Count, Min, Max |
| **Formula bar** | Shows value or formula of the selected cell, expandable for long text; **Show formulas** toggle shows formulas in all cells |
| **Sheet tabs** | Swipe or tap between sheets; hidden sheets are listed in a menu; long press tab for sheet options |
| **Freeze panes** | Freeze first row, first column, or both |
| **Zoom** | 50% to 300%, pinch or buttons; auto-fit columns |
| **Find** | Match highlighting (yellow, current orange), search in: this sheet / all sheets / formulas / values; next / previous |
| **Sort** | Sort by column A to Z / Z to A, multi-level sort sheet; header row detected automatically |
| **Filter** | Per-column filter: text contains, number ranges, values checklist, colour; active filter shows a funnel on the header; **Clear filters** chip |
| **Go to cell / range** | Type `B12` or `A1:D20` |
| **Cell details sheet** | Long press: full value, formula, number format, cell note, comments, **Copy** |
| **Charts view** | Shows charts embedded in the file, tap to enlarge |
| **Column / row options** | Resize, wrap text (viewer-only), hide, show |
| **Row card view** | Shows one row as a card (good for narrow phones) |
| **Edit cell** (Editors) | Tap twice to edit, formula suggestions, **Save to Drive** |
| **Export / Download as** | XLSX, CSV, PDF (current sheet), image of selected range |
| **Share / Print / Details** | Standard |

### 6.3 Search / sort / filter summary
- Find across sheets with a results list (sheet, cell address, value).
- Sort and filter never change the original file unless the user is an Editor and taps **Apply to file**; otherwise they are a temporary view.

### 6.4 Settings
| Section | Settings |
|---|---|
| Display | Gridlines on / off, header row highlight, zebra stripes, text wrap, number format (original / plain), zoom default, theme |
| Behaviour | Freeze header by default, show formula bar, show status bar sums, show hidden sheets, open at last position |
| Data | Large file mode (loads in blocks, limit 100,000 rows in preview, **Load more**), date format, CSV delimiter (auto, comma, semicolon, tab), encoding |
| Editing | Allow editing (Editors), confirm before saving, autosave |
| Google Sheets | Open in: (In-app viewer, Google Sheets app) |

---

## 7. Markdown viewer (.md)

### 7.1 Layout (Rendered, Source and Split)

```
Rendered                          Source
┌──────────────────────────────┐ ┌──────────────────────────────┐
│ [<] README.md (S)(:)         │ │ [<] README.md (S)(:)         │
│ (Rendered)(Source)(Split)    │ │ (Rendered)(Source)(Split)    │
│ ┌──────────────────────────┐ │ │ ┌──────────────────────────┐ │
│ │ # CloudBox               │ │ │ │ 1 # CloudBox             │ │
│ │ A simple file manager.   │ │ │ │ 2                        │ │
│ │ - [x] Upload             │ │ │ │ 3 A simple file manager. │ │
│ │ - [ ] Share              │ │ │ │ 4 - [x] Upload           │ │
│ │ ┌────────────────────┐   │ │ │ │ 5 - [ ] Share            │ │
│ │ │ code block   [Copy]│   │ │ │ │ 6 ```kotlin              │ │
│ │ └────────────────────┘   │ │ │ └──────────────────────────┘ │
│ └──────────────────────────┘ │ │ [Find][TOC][Aa][Theme][...]  │
│ [Find][TOC][Aa][Theme][...]  │ └──────────────────────────────┘
└──────────────────────────────┘
```
**Split** (landscape or tablet) shows Source on the left and Rendered on the right with synchronised scrolling.

### 7.2 What is rendered
Headings (H1 to H6) · bold, italic, strikethrough · ordered, unordered and **task lists** · block quotes · inline code and **code blocks with syntax highlighting and a Copy button** · **tables** (scroll sideways when wide, sticky first column option) · images (local, relative path from Drive, remote; tap to open the Image viewer) · links (tap opens; internal `#anchors` scroll; relative links to other files open in CloudBox) · horizontal rules · footnotes · YAML front matter shown as a collapsible info card · **math** (`$...$`, `$$...$$` via KaTeX) · **Mermaid diagrams** · emoji shortcodes · HTML (sanitised).

### 7.3 Tools
| Tool | Description |
|---|---|
| **Mode switch** | Rendered, Source, Split |
| **TOC / Outline** | Headings tree from the file, tap to jump, current heading highlighted |
| **Find** | Works in rendered and source; highlights all matches, current in orange; options match case, whole word, regex |
| **Theme** | GitHub light, GitHub dark, Paper (sepia), Black, Solarized |
| **Text (Aa)** | Body font size, font (Sans, Serif), code font size, line height, content width (Narrow, Normal, Wide) |
| **Code options** | Wrap long lines on / off, line numbers on / off, code theme |
| **Task list toggle** | Editors can tick checkboxes in the rendered view; the change is saved to the file |
| **Edit** (Editors) | Switches to the editor with a formatting bar: **B I S H1 H2 • 1. [ ] ` ``` link image table**, undo, redo, live preview, autosave |
| **Copy** | Copy as Markdown, copy as plain text, copy a code block |
| **Export** | HTML, PDF (uses rendered theme), plain text |
| **Share / Download / Print / Details** | Standard |

### 7.4 Search / sort / filter
Find results list shows line number and snippet; the TOC can be filtered by typing a heading name.

### 7.5 Settings
| Section | Settings |
|---|---|
| Display | Default mode, theme, body font, size, code font size, content width, line height |
| Rendering | Math on / off, Mermaid on / off, load remote images (always, Wi-Fi only, never), show front matter, render HTML |
| Code | Wrap lines, line numbers, syntax theme, tab size |
| Editing | Autosave, spell check, auto-continue lists, smart quotes |
| Links | Open links in: in-app browser, system browser, ask |

---

## 8. Text file viewer (.txt, .log, .json, .xml, code…)

### 8.1 Layout

```
┌──────────────────────────────┐
│ [<] server.log   (S)(:)      │
│     UTF-8 · 2.4 MB · 18,203  │  encoding · size · lines
│ ┌──────────────────────────┐ │
│ │ 1  2026-03-12 INFO start │ │
│ │ 2  2026-03-12 WARN slow  │ │
│ │ 3  2026-03-12 ERROR fail │ │
│ │ 4  ░░ matched line ░░░░  │ │
│ │ 5  2026-03-12 INFO ok    │ │
│ └──────────────────────────┘ │
│ Ln 3, Col 14        UTF-8    │  status bar
│ [Find][Go to][Wrap][Aa][...] │
└──────────────────────────────┘
```

### 8.2 Tools
| Tool | Description |
|---|---|
| **Find** | Plain or **regex**, match case, whole word; highlights all, current orange; match count; next / previous; **Replace** and Replace all (Editors) |
| **Go to line** | Enter a line number; the line flashes |
| **Word wrap** | On / off (off enables horizontal scroll with a mini-map) |
| **Line numbers** | On / off |
| **Font** | Monospace family (system mono, JetBrains Mono style), size 8 to 28 |
| **Theme** | Light, Dark, Solarized, Monokai, High contrast |
| **Syntax highlighting** | Auto by extension (json, xml, html, css, js, py, java, kt, c, cpp, sql, yaml, sh); can be changed manually |
| **Encoding** | Auto-detect; manual choice: UTF-8, UTF-16, ASCII, ISO-8859-1, Windows-1252, Shift-JIS, GBK; **Reopen with encoding** |
| **Line ending** | Shows LF / CRLF / CR |
| **JSON / XML tools** | **Pretty print**, **tree view** (expand, collapse, search keys), minify, copy path of a node, validate with an error line shown |
| **Log mode** | Auto for .log: colours ERROR red, WARN amber, INFO blue, DEBUG grey; **filter by level**, **Tail / Follow** (auto-scroll to end), timestamp highlight |
| **Select / Copy** | Long press to select; copy, share selection, search selection |
| **Fold** | Collapse code blocks (JSON, code) |
| **Edit** (Editors) | Inline editing, undo, redo, autosave, **Save to Drive** |
| **Open large file** | Streams in 1 MB blocks; shows “Showing lines 1 to 20,000” with **Load more**; search runs in the background with progress |
| **Export / Share / Download / Details** | Standard |

### 8.3 Search / sort / filter
- Find with regex and results list (line number + snippet).
- Log mode: filter chips **All / Error / Warn / Info / Debug**, text filter (“show only lines containing…”), **Sort** not applicable.
- CSV opened as text offers **Open as table** which switches to the Sheets viewer.

### 8.4 Settings
| Section | Settings |
|---|---|
| Display | Font, size, theme, line numbers, word wrap, show whitespace characters, mini-map |
| Syntax | Auto detect, default language, tab width |
| Encoding | Default encoding (Auto), line ending, BOM handling |
| Performance | Large file threshold (default 5 MB), block size, background search |
| Editing | Autosave, auto-indent, trim trailing spaces on save |

---

## 9. Image viewer

### 9.1 Layout

```
┌──────────────────────────────┐
│ [<] Beach.jpg  4/12   (i)(:) │  top bar, auto-hides
│                              │
│ ‹        ┌────────┐        › │
│          │        │          │
│          │ image  │  [140%]  │  zoom badge
│          │        │          │
│          └────────┘          │
│                              │
│ ┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐     │  filmstrip
│ [Edit][Rotate][Info][Slide][..]│
└──────────────────────────────┘
```

### 9.2 Gestures
Swipe left or right = previous / next image · pinch = zoom 1x to 8x · double tap = toggle fit and 2.5x · drag when zoomed = pan · swipe down = close (with image shrink animation) · swipe up = info panel · long press = quick menu (Share, Save, Set as…).

### 9.3 Tools
| Tool | Description |
|---|---|
| **Zoom / fit / fill** | Buttons and gestures, 100% pixel view |
| **Rotate / flip** | 90° steps, flip horizontal / vertical, straighten slider |
| **Crop** | Free, 1:1, 4:3, 16:9, 3:4, 9:16 |
| **Adjust** | Brightness, contrast, saturation, warmth, sharpness, vignette (sliders −100 to +100, **Compare** by holding) |
| **Filters** | None, B&W, Warm, Cool, Vivid, Fade, Sepia with strength slider |
| **Markup** | Pen, highlighter, arrow, text, shapes, blur / pixelate region (same colour and thickness controls as PDF 3.5) |
| **Info panel** | Name, size, dimensions, date, camera, lens, ISO, exposure, location on a mini-map, owner, sharing, **Edit description** |
| **Slideshow** | Interval 2, 4, 6, 10 s, order (normal, shuffle), transition, loop |
| **Filmstrip** | Thumbnails of the folder, tap to jump |
| **Save copy / Save to gallery** | Saves the edited image as a copy (JPG, PNG, WebP) into `Pictures/CloudBox` |
| **Set as** | Wallpaper, contact photo |
| **GIF / animated** | Play, pause, speed, frame by frame |
| **SVG** | Rendered with zoom; **View code** opens the Text viewer |
| **RAW / HEIC** | Decoded with embedded preview first |
| **Share / Download / Print / Details** | Standard |

### 9.4 Search / sort / filter (gallery of the folder)
Sort: **Name, Date taken, Date modified, Size**, ascending or descending. Filter: **All, Photos, GIFs, Screenshots, Favourites, Edited**. Search by file name and by description.

### 9.5 Settings
| Section | Settings |
|---|---|
| Display | Background (black, theme, blur), show filmstrip, show zoom badge, default fit (fit, fill), show EXIF in info |
| Gestures | Double tap action, swipe down to close, tap to hide controls |
| Slideshow | Interval, order, transition, loop |
| Editing | Default save format, JPG quality, save as copy (always), keep EXIF |
| Loading | Load full resolution always / on zoom, cache size |

---

## 10. Video viewer

### 10.1 Layout (portrait and landscape)

```
Portrait                       Landscape (full screen)
┌──────────────────────────────┐ ┌────────────────────────────────────┐
│ [<] demo.mp4      (cc)(:)    │ │ [<] demo.mp4            (cc)(:)    │
│ ┌──────────────────────────┐ │ │                                    │
│ │                          │ │ │    ⏪10      ( ▶ )      10⏩         │
│ │         ( ▶ )            │ │ │                                    │
│ │                          │ │ │ ──────●────────────────  0:42/3:04 │
│ └──────────────────────────┘ │ │ [Speed][CC][Audio][Lock][PiP][...] │
│ ──────●──────────  0:42/3:04 │ └────────────────────────────────────┘
│ [⏪10][⏩10][Speed][CC][⚙]    │
└──────────────────────────────┘
```

### 10.2 Gestures
Tap = show / hide controls (auto-hide 3 s while playing) · double tap left / right = −10 s / +10 s (repeat taps add more) · double tap centre = play / pause · swipe vertically on the **left half = brightness**, **right half = volume** · swipe horizontally = seek preview with thumbnail and time · pinch = zoom / fill · long press = 2x speed while held · swipe down in portrait = minimise to mini player.

### 10.3 Tools
| Tool | Description |
|---|---|
| **Play / pause, ±10 s** | Button size 70 px centre, skip amount configurable (5, 10, 15, 30 s) |
| **Seek bar** | Time labels, thumbnail preview on drag, buffered range shown lighter, chapter marks |
| **Speed** | 0.25x to 4x (0.25 steps), remembered per video optional |
| **Quality** | Auto, 1080p, 720p, 480p, 360p (when available) |
| **Subtitles / CC** | Embedded tracks; **Load subtitle file** (.srt, .vtt, .ass) from device or Drive; size, colour, background, position, delay ± |
| **Audio track** | Switch language track; audio delay ± |
| **Loop** | Off, repeat one; **A-B repeat** set A and B on the bar |
| **Aspect ratio** | Fit, Fill, Stretch, 16:9, 4:3, Original |
| **Rotate / orientation lock** | Auto, portrait, landscape |
| **Lock controls** | Prevents accidental touches, tap the lock icon to unlock |
| **Picture in picture** | Floating mini player |
| **Cast** | Send to TV |
| **Background audio** | Keeps playing audio with the screen off and a notification |
| **Screenshot frame** | Saves the current frame as an image into the device gallery (`Pictures/CloudBox`) |
| **Bookmarks** | Add a timestamp note, tap to jump |
| **Sleep timer** | 15, 30, 45, 60 minutes or end of video |
| **Resume** | “Resume from 0:42?” on reopening |
| **Share / Download / Offline / Details** | Standard (details: codec, resolution, frame rate, bitrate, duration) |

### 10.4 Search / sort / filter
In the folder playlist: **Sort** by name, date, duration, size; **Filter** by All, Unwatched, In progress, Watched; search by name; **Up next** list with drag to reorder and **Autoplay next** toggle.

### 10.5 Settings
| Section | Settings |
|---|---|
| Playback | Default speed, skip interval, autoplay next, resume playback, loop default, hardware acceleration |
| Gestures | Brightness and volume swipes, seek swipe, double tap seek, long press 2x |
| Subtitles | Default language, size, colour, background, auto-load file with the same name |
| Audio | Default audio language, volume boost up to 200%, background audio, normalise |
| Display | Default aspect ratio, keep screen on, orientation, show battery and time in landscape |
| Network | Default quality (Auto, data saver), stream on mobile data (ask, always, never) |

---

## 11. Audio viewer

### 11.1 Layout

```
┌──────────────────────────────┐
│ [<] Now playing   (Q)(:)     │
│                              │
│        ┌──────────────┐      │
│        │   album art  │      │  disc spins while playing
│        └──────────────┘      │
│   Episode 1 · Getting started│
│   CloudBox Podcast           │
│ ▏▎▍▌▋▊▉▊▋▌▍▎▏▎▍▌▋▍▎▏       │  waveform with progress
│ ──────●──────────  1:10/4:02 │
│ (Shuf)(Prev)(⟲15)(▶)(15⟳)(Next)(Rep)│
│ [Speed][Sleep][EQ][Loop A-B][..]│
│ 🔈 ──────────●─────── 🔊     │
└──────────────────────────────┘
```

### 11.2 Tools
| Tool | Description |
|---|---|
| **Play / pause, previous / next** | Large button 66 px; previous restarts the track if more than 3 s played |
| **Skip back / forward** | 15 s by default (5, 10, 15, 30 configurable), good for podcasts |
| **Waveform and seek** | Draggable, time labels, tap on waveform to jump |
| **Speed** | 0.5x to 3x with pitch correction |
| **Sleep timer** | 5, 15, 30, 45, 60 minutes, end of track, end of queue; fades out the last 10 s |
| **Equalizer** | Presets (Flat, Bass boost, Vocal, Treble, Rock, Pop, Classical) + 5-band sliders −12 to +12 dB, preamp, bass boost, virtualizer |
| **Loop** | Off, repeat all, repeat one; **A-B loop** |
| **Shuffle** | On / off |
| **Queue** | List with drag to reorder, swipe to remove, **Add folder to queue**, **Clear queue**, **Save as playlist** |
| **Lyrics** | Reads embedded lyrics or a .lrc file, synced line highlights, font size |
| **Bookmarks** | Timestamp notes for podcasts and audiobooks |
| **Skip silence** | Removes quiet gaps |
| **Volume normalisation / Crossfade** | Even loudness, 0 to 12 s crossfade |
| **Metadata** | Title, artist, album, year, genre, cover (view); **Edit tags** for Editors |
| **Background and lock screen** | Notification and lock screen controls with seek bar and cover art |
| **Share / Download / Offline / Details** | Standard (details: codec, bitrate, sample rate, channels, duration) |

### 11.3 Search / sort / filter
Queue and folder list: search by title, artist, album; **Sort** by title, artist, album, duration, date added, size; **Filter** by All, Favourites, Recently played, Not played, Offline.

### 11.4 Settings
| Section | Settings |
|---|---|
| Playback | Default speed, skip interval, resume, autoplay next, gapless, crossfade, skip silence, normalise |
| Sound | Equalizer preset, bass boost, mono audio, balance |
| Sleep | Default timer, fade-out length, shake to extend |
| Display | Show waveform, show lyrics, album art style (disc, square), theme |
| Controls | Headset buttons, lock screen controls, notification style, audio focus (pause on call, duck for notifications) |

---

## 12. Global viewer settings hub

```
┌──────────────────────────────┐
│ [<] Viewer settings          │
│ GENERAL                      │
│ Theme               [System] │
│ Keep screen on while viewing │
│ Remember last position  (●)  │
│ Open files with    [In-app]  │
│ PER VIEWER                   │
│ PDF                        > │
│ Word and Docs              > │
│ Slides                     > │
│ Sheets                     > │
│ Markdown                   > │
│ Text                       > │
│ Image                      > │
│ Video                      > │
│ Audio                      > │
│ STORAGE                      │
│ Cache size      148 MB [Clear]│
│ Save exported images to      │
│ [ Gallery > CloudBox ]  v    │
│ [ Restore all defaults ]     │
└──────────────────────────────┘
```
Each viewer also has a ⚙ shortcut inside the viewer that opens the same settings as a bottom sheet for that viewer only, so users never have to leave the file.

---

## 13. Permission rules inside viewers

| Role | Can do | Cannot do |
|---|---|---|
| **Owner** | Everything: edit, annotate and save to original, share, change roles, remove people, delete or trash | – |
| **Editor** | View, annotate, edit, save to the file, comment, download (unless blocked) | **Delete or trash** the file or folder, remove or change the owner, change other people’s roles |
| **Commenter** | View, comment, private annotations, download (unless blocked) | Edit the file, save annotations into the original |
| **Viewer** | View, private annotations on the device, download (unless blocked), extract images | Edit, comment, save into the original, share (unless allowed) |

UI behaviour: tools the user can’t use are hidden or shown greyed with a lock and a short reason (“Only editors can edit”). Permission reasons come from the file’s Drive `capabilities` (canEdit, canComment, canDelete, canShare, canDownload), never from guesses. Files with **download restricted** hide Download, Print, Extract and Copy, and show a banner “The owner turned off downloads”.

---

## 14. Technical notes

| Area | Suggested approach |
|---|---|
| PDF rendering | Android PdfRenderer or PdfiumAndroid / MuPDF (for text search and selection); iOS PDFKit |
| PDF annotations | Store strokes as vector JSON per page (points, pressure, tool, colour, size); render on a canvas overlay; flatten on export with PDFBox / iText / PDFKit |
| Extract page as image | Render the page to a bitmap at the chosen DPI, write with MediaStore (Android 10+) to `Pictures/CloudBox`, or the folder chosen through the Storage Access Framework; Photos add-only on iOS |
| OCR | ML Kit text recognition (on device) |
| Docs, Slides, Sheets | Native Google files through Drive `files.export` (DOCX, PPTX, XLSX, PDF); Office files rendered by a converter or by a document SDK; editing via “Open in Google app” or the Docs, Slides and Sheets APIs |
| Markdown | Markwon (Android) or a commonmark parser with a syntax highlighter; KaTeX and Mermaid in a sandboxed WebView |
| Text | Memory-mapped reading, chunked loading, regex search in a background thread |
| Image | Coil or Glide with downsampling; EXIF via ExifInterface |
| Video and audio | Media3 ExoPlayer with MediaSession for notifications, PiP and Cast |
| Caching | Rendered pages, thumbnails and converted documents cached with an LRU limit set in settings |
| Security | No file content in logs; password-protected files unlocked in memory only; sandboxed WebView with JavaScript limited to the renderers |

---

## 15. Delivery checklist

**MVP (first release)**
- Common shell, states, role-aware tools.
- PDF: scroll and swipe modes, search with highlight, page jump, thumbnails, highlighter, pens with colour and thickness, eraser, extract page to gallery, settings.
- Image, video and audio viewers with core tools.
- Text and Markdown viewers (rendered, source, search, themes).
- Docs, Slides and Sheets in read mode with search, outline or grid, and export.

**Version 2**
- PDF page management (merge, split, reorder, compress, protect), forms and signatures, OCR, read aloud.
- Editing in Docs, Sheets and Markdown, comments and suggestions, version history.
- Slides presenter mode with cast, subtitle tools, lyrics, equalizer.

**Quality bar for every viewer**
- Loading skeleton, error, offline, password, no-access and unsupported states designed.
- Works in light and dark, portrait and landscape, at 200% text size.
- Search always highlights matches and moves to the current one.
- Every setting has a default and **Restore defaults**.
- Touch targets at least 48 px; every icon has a label for screen readers.