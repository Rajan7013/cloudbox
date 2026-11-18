import React, { useCallback, useEffect, useRef, useState } from 'react';
import { View, Text, FlatList, Pressable, RefreshControl, TextInput, useWindowDimensions } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTheme, type, radius, fileTypes } from '../theme';
import * as repo from '../data/mockRepository';
import FileItem from '../components/FileItem';
import Skeleton from '../components/Skeleton';
import Button from '../components/Button';
import Sheet from '../components/Sheet';
import Toast from '../components/Toast';
import { Chip, SheetRow, Overline } from '../components/Bits';

const SORTS = [['d', 'Modified'], ['n', 'Name'], ['s', 'Size'], ['t', 'Type']];
const TYPES = ['all', 'folder', 'doc', 'pdf', 'img', 'video', 'audio', 'sheet', 'zip'];
const TYPE_LABEL = { all: 'All', folder: 'Folders', doc: 'Documents', pdf: 'PDF', img: 'Images', video: 'Video', audio: 'Audio', sheet: 'Sheets', zip: 'Archives' };

export default function FilesScreen() {
  const t = useTheme();
  const { width } = useWindowDimensions();
  const [path, setPath] = useState([null]);
  const [mode, setMode] = useState('grid');
  const [status, setStatus] = useState('loading');
  const [items, setItems] = useState([]);
  const [refreshing, setRefreshing] = useState(false);
  const [opts, setOpts] = useState(repo.DEFAULT_OPTS);     // sort + filter in use
  const [draft, setDraft] = useState(repo.DEFAULT_OPTS);   // changes inside the sort sheet
  const [sheet, setSheet] = useState(null);                // { name, item }
  const [text, setText] = useState('');
  const [err, setErr] = useState('');
  const [toast, setToast] = useState(null);

  const optsRef = useRef(opts); optsRef.current = opts;
  const current = path[path.length - 1];
  const cols = mode === 'grid' ? Math.max(2, Math.floor((width - 32) / 160)) : 1;
  const filtered = opts.type !== 'all' || opts.key !== 'd' || opts.dir !== 1;

  const load = useCallback(async (pull = false) => {
    pull ? setRefreshing(true) : setStatus('loading');
    try { setItems(await repo.listFiles(current, optsRef.current)); setStatus('success'); }
    catch (e) { setStatus('error'); }
    setRefreshing(false);
  }, [current]);
  useEffect(() => { load(); }, [load]);

  const refresh = (o = optsRef.current) => setItems(repo.peek(current, o));   // instant, no skeleton
  const show = (msg, undo) => setToast({ msg, undo });
  const close = () => setSheet(null);
  const openSheet = (name, item) => {
    setErr(''); setText(name === 'rename' ? item.name : '');
    if (name === 'sort') setDraft(opts);
    setSheet({ name, item });
  };

  // ----- actions -----
  const makeFolder = () => { try { repo.createFolder(current, text.trim()); refresh(); close(); show('Folder created'); } catch (e) { setErr(e.message); } };
  const doRename = () => { try { repo.rename(sheet.item.id, text.trim()); refresh(); close(); } catch (e) { setErr(e.message); } };
  const star = (it) => { repo.toggleStar(it.id); refresh(); close(); };
  const trash = (it) => { repo.trash(it.id); refresh(); close(); show('Moved to trash', () => { repo.restore(it.id); refresh(); }); };
  const apply = () => { setOpts(draft); refresh(draft); close(); };
  const open = (it) => { if (it.type === 'folder') setPath([...path, it.id]); };

  const names = path.map((id) => (id === null ? 'My Files' : repo.getItem(id).name));

  const input = (placeholder) => (
    <View>
      <TextInput autoFocus value={text} onChangeText={(v) => { setText(v); setErr(''); }} placeholder={placeholder} placeholderTextColor={t.sub}
        style={{ height: 48, borderRadius: radius.input, borderWidth: 1.5, borderColor: err ? t.error : t.primary, paddingHorizontal: 14, color: t.text, backgroundColor: t.bg, fontSize: 15 }} />
      {!!err && <Text style={[type.caption, { color: t.error, marginTop: 6 }]}>{err}</Text>}
    </View>
  );

  const header = (
    <View style={{ paddingHorizontal: 6, paddingBottom: 8 }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 8 }}>
        {path.length > 1 && (
          <Pressable onPress={() => setPath(path.slice(0, -1))} style={{ marginRight: 10, padding: 6 }}>
            <Text style={{ fontSize: 22, color: t.text }}>←</Text>
          </Pressable>
        )}
        <Text style={[type.h1, { color: t.text, flex: 1 }]} numberOfLines={1}>{names[names.length - 1]}</Text>
      </View>
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginBottom: 12 }}>
        {names.map((n, i) => (
          <Pressable key={i} onPress={() => setPath(path.slice(0, i + 1))}>
            <Text style={[type.caption, { color: i === names.length - 1 ? t.sub : t.primary }]}>{i > 0 ? '  ›  ' : ''}{n}</Text>
          </Pressable>
        ))}
      </View>
      <View style={{ flexDirection: 'row' }}>
        <Pressable onPress={() => openSheet('sort')} style={{ backgroundColor: t.surface, borderRadius: radius.chip, paddingHorizontal: 14, height: 36, justifyContent: 'center', marginRight: 8 }}>
          <Text style={[type.label, { color: t.text }]}>⇅  Sort & filter{filtered ? '  •' : ''}</Text>
        </Pressable>
        <Pressable onPress={() => setMode(mode === 'grid' ? 'list' : 'grid')} style={{ backgroundColor: t.surface, borderRadius: radius.chip, paddingHorizontal: 14, height: 36, justifyContent: 'center' }}>
          <Text style={[type.label, { color: t.text }]}>{mode === 'grid' ? '☰  List' : '▦  Grid'}</Text>
        </Pressable>
      </View>
    </View>
  );

  let body;
  if (status === 'loading') {
    body = (
      <FlatList key={'s' + cols} numColumns={cols} data={[1, 2, 3, 4, 5, 6]} keyExtractor={(i) => 's' + i}
        ListHeaderComponent={header} contentContainerStyle={{ padding: 10 }}
        renderItem={() => mode === 'grid'
          ? <View style={{ flex: 1, margin: 6 }}><Skeleton style={{ height: 130, borderRadius: radius.card }} /></View>
          : <Skeleton style={{ height: 64, marginBottom: 10, borderRadius: 16 }} />} />
    );
  } else if (status === 'error') {
    body = (<View style={{ flex: 1, padding: 16 }}>{header}
      <View style={{ backgroundColor: t.surface, borderRadius: radius.card, padding: 20, alignItems: 'center' }}>
        <Text style={{ fontSize: 40 }}>⚠️</Text>
        <Text style={[type.title, { color: t.text, marginTop: 8 }]}>Something went wrong</Text>
        <Text style={[type.body, { color: t.sub, marginBottom: 14 }]}>We could not load your files.</Text>
        <Button label="Retry" onPress={() => load()} style={{ alignSelf: 'stretch' }} />
      </View></View>);
  } else {
    body = (
      <FlatList key={'d' + cols + mode} numColumns={cols} data={items} keyExtractor={(i) => String(i.id)}
        ListHeaderComponent={header} contentContainerStyle={{ padding: 10, paddingBottom: 110 }}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => load(true)} tintColor={t.primary} />}
        renderItem={({ item }) => <FileItem item={item} mode={mode} onPress={() => open(item)} onMore={() => openSheet('actions', item)} />}
        ListEmptyComponent={filtered
          ? <View style={{ alignItems: 'center', paddingTop: 40 }}>
              <Text style={{ fontSize: 48 }}>🔎</Text>
              <Text style={[type.title, { color: t.text, marginTop: 8 }]}>No files match your filter</Text>
              <Button variant="secondary" label="Clear filter" style={{ marginTop: 14, alignSelf: 'stretch', marginHorizontal: 30 }} onPress={() => { setOpts(repo.DEFAULT_OPTS); refresh(repo.DEFAULT_OPTS); }} />
            </View>
          : <View style={{ alignItems: 'center', paddingTop: 40 }}>
              <Text style={{ fontSize: 56 }}>🗂️</Text>
              <Text style={[type.h1, { color: t.text, marginTop: 8 }]}>This folder is empty</Text>
              <Text style={[type.body, { color: t.sub, textAlign: 'center', marginTop: 4, marginBottom: 16 }]}>Tap + to add files or create a folder</Text>
              <Button label="New folder" onPress={() => openSheet('folder')} style={{ alignSelf: 'stretch', marginHorizontal: 30 }} />
            </View>} />
    );
  }

  // ----- sheet contents -----
  const it = sheet && sheet.item;
  let content = null;
  if (sheet && sheet.name === 'create') content = (<View>
    <Text style={[type.title, { color: t.text, marginBottom: 6 }]}>Create new</Text>
    <SheetRow icon="📁" label="New folder" onPress={() => openSheet('folder')} />
    <SheetRow icon="⬆️" label="Upload file (next step)" onPress={() => { close(); show('Upload comes in a later step'); }} />
  </View>);
  if (sheet && sheet.name === 'folder') content = (<View>
    <Text style={[type.title, { color: t.text, marginBottom: 12 }]}>New folder</Text>
    {input('Folder name')}
    <View style={{ flexDirection: 'row', marginTop: 14 }}>
      <Button variant="secondary" label="Cancel" onPress={close} style={{ flex: 1, marginRight: 10 }} />
      <Button label="Create" onPress={makeFolder} disabled={!text.trim()} style={{ flex: 1 }} />
    </View>
  </View>);
  if (sheet && sheet.name === 'rename') content = (<View>
    <Text style={[type.title, { color: t.text, marginBottom: 12 }]}>Rename</Text>
    {input('Name')}
    <View style={{ flexDirection: 'row', marginTop: 14 }}>
      <Button variant="secondary" label="Cancel" onPress={close} style={{ flex: 1, marginRight: 10 }} />
      <Button label="Save" onPress={doRename} disabled={!text.trim()} style={{ flex: 1 }} />
    </View>
  </View>);
  if (sheet && sheet.name === 'actions') content = (<View>
    <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 8 }}>
      <View style={{ width: 44, height: 44, borderRadius: 12, backgroundColor: fileTypes[it.type][1] + '22', alignItems: 'center', justifyContent: 'center', marginRight: 12 }}>
        <Text style={{ fontSize: 22 }}>{fileTypes[it.type][0]}</Text>
      </View>
      <Text style={[type.title, { color: t.text, flex: 1 }]} numberOfLines={1}>{it.name}</Text>
    </View>
    <SheetRow icon={it.star ? '⭐' : '☆'} label={it.star ? 'Remove star' : 'Add star'} onPress={() => star(it)} />
    <SheetRow icon="✏️" label="Rename" onPress={() => openSheet('rename', it)} />
    <View style={{ height: 1, backgroundColor: t.border, marginVertical: 6 }} />
    <SheetRow icon="🗑️" label="Move to trash" danger onPress={() => trash(it)} />
  </View>);
  if (sheet && sheet.name === 'sort') content = (<View>
    <Text style={[type.title, { color: t.text }]}>Sort by</Text>
    <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginTop: 10 }}>
      {SORTS.map(([k, l]) => <Chip key={k} label={l} on={draft.key === k} onPress={() => setDraft({ ...draft, key: k })} />)}
      <Chip label={draft.dir === 1 ? '↑ Ascending' : '↓ Descending'} onPress={() => setDraft({ ...draft, dir: -draft.dir })} />
    </View>
    <Overline>Filter by type</Overline>
    <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
      {TYPES.map((k) => <Chip key={k} label={TYPE_LABEL[k]} on={draft.type === k} onPress={() => setDraft({ ...draft, type: k })} />)}
    </View>
    <View style={{ flexDirection: 'row', marginTop: 8 }}>
      <Button variant="secondary" label="Reset" onPress={() => setDraft(repo.DEFAULT_OPTS)} style={{ flex: 1, marginRight: 10 }} />
      <Button label="Apply" onPress={apply} style={{ flex: 1 }} />
    </View>
  </View>);

  return (
    <SafeAreaView edges={['top']} style={{ flex: 1, backgroundColor: t.bg }}>
      {body}
      {status === 'success' && (
        <Pressable onPress={() => openSheet('create')} style={({ pressed }) => ({ position: 'absolute', right: 20, bottom: 24, width: 58, height: 58, borderRadius: 20, backgroundColor: t.primary, alignItems: 'center', justifyContent: 'center', transform: [{ scale: pressed ? 0.95 : 1 }], shadowColor: t.primary, shadowOpacity: 0.5, shadowRadius: 12, shadowOffset: { width: 0, height: 8 }, elevation: 8 })}>
          <Text style={{ color: '#fff', fontSize: 30, marginTop: -2 }}>+</Text>
        </Pressable>
      )}
      <Toast toast={toast} onClose={() => setToast(null)} />
      <Sheet visible={!!sheet} onClose={close}>{content}</Sheet>
    </SafeAreaView>
  );
}
