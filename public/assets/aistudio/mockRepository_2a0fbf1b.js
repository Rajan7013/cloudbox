// MockRepository. A real DriveRepository will export the same functions later.
export const FLAGS = { delay: 900, forceError: false, forceEmpty: false };

let items = [
  { id: 1, parent: null, name: 'Work', type: 'folder', size: 0, d: 5 },
  { id: 2, parent: null, name: 'Photos', type: 'folder', size: 0, d: 3 },
  { id: 3, parent: null, name: 'Study', type: 'folder', size: 0, d: 9 },
  { id: 4, parent: null, name: 'Resume.pdf', type: 'pdf', size: 420, d: 1, star: true },
  { id: 5, parent: null, name: 'Budget 2026.xlsx', type: 'sheet', size: 310, d: 2 },
  { id: 6, parent: null, name: 'Trip video.mp4', type: 'video', size: 84000, d: 4 },
  { id: 7, parent: null, name: 'Podcast ep1.mp3', type: 'audio', size: 9200, d: 6 },
  { id: 8, parent: null, name: 'Notes.docx', type: 'doc', size: 55, d: 0, star: true },
  { id: 9, parent: 1, name: 'Projects', type: 'folder', size: 0, d: 7 },
  { id: 10, parent: 1, name: 'Proposal.docx', type: 'doc', size: 120, d: 2 },
  { id: 11, parent: 1, name: 'Invoice.pdf', type: 'pdf', size: 210, d: 3 },
  { id: 12, parent: 9, name: 'App design.pdf', type: 'pdf', size: 1800, d: 1 },
  { id: 13, parent: 9, name: 'Backup.zip', type: 'zip', size: 52000, d: 8 },
  { id: 14, parent: 2, name: 'Beach.jpg', type: 'img', size: 3400, d: 3 },
  { id: 15, parent: 2, name: 'Sunset.png', type: 'img', size: 2800, d: 5 },
  { id: 16, parent: 3, name: 'Physics notes.pdf', type: 'pdf', size: 900, d: 10 },
];
let nextId = 100;

export const DEFAULT_OPTS = { key: 'd', dir: 1, type: 'all' };   // key: d=modified n=name s=size t=type
const wait = (ms) => new Promise((r) => setTimeout(r, ms));
const patch = (id, fn) => { items = items.map((i) => (i.id === id ? { ...i, ...fn(i) } : i)); };

// Instant version (no delay). Used after changes so the screen updates smoothly.
export function peek(parentId = null, o = DEFAULT_OPTS) {
  let rows = items.filter((i) => i.parent === parentId && !i.trashed);
  if (o.type !== 'all') rows = rows.filter((i) => i.type === o.type);
  const cmp = {
    d: (a, b) => a.d - b.d,
    n: (a, b) => a.name.localeCompare(b.name),
    s: (a, b) => a.size - b.size,
    t: (a, b) => a.type.localeCompare(b.type),
  }[o.key];
  return rows.sort((a, b) => ((a.type === 'folder') !== (b.type === 'folder')
    ? (a.type === 'folder' ? -1 : 1)           // folders always first
    : cmp(a, b) * o.dir));
}

// Slow version with fake network delay, so you can see the skeleton.
export async function listFiles(parentId = null, o = DEFAULT_OPTS) {
  await wait(FLAGS.delay);
  if (FLAGS.forceError) throw new Error('Could not load files');
  if (FLAGS.forceEmpty) return [];
  return peek(parentId, o);
}

const checkName = (parent, name, exceptId) => {
  if (!name) throw new Error('Please enter a name');
  const dup = items.some((i) => i.parent === parent && !i.trashed && i.id !== exceptId && i.name.toLowerCase() === name.toLowerCase());
  if (dup) throw new Error('An item with this name already exists');
};

export function createFolder(parent, name) {
  checkName(parent, name);
  items = [...items, { id: nextId++, parent, name, type: 'folder', size: 0, d: 0 }];
}
export function rename(id, name) {
  const it = items.find((i) => i.id === id);
  checkName(it.parent, name, id);
  patch(id, () => ({ name }));
}
export const toggleStar = (id) => patch(id, (i) => ({ star: !i.star }));
export const trash = (id) => patch(id, () => ({ trashed: true }));
export const restore = (id) => patch(id, () => ({ trashed: false }));
export const getItem = (id) => items.find((i) => i.id === id);
