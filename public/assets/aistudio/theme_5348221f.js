import { useColorScheme } from 'react-native';

// Colour tokens from the design spec (light and dark).
const light = { bg: '#F5F6FB', surface: '#FFFFFF', text: '#161B2E', sub: '#6B7190', primary: '#4F46E5', primaryContainer: '#EEF0FF', border: '#E7E9F4', error: '#EF4444', warning: '#F59E0B' };
const dark = { bg: '#0E1020', surface: '#1A1D33', text: '#EEF0FF', sub: '#9296B5', primary: '#7C83FF', primaryContainer: '#24284A', border: '#2A2E4F', error: '#F87171', warning: '#FBBF24' };

export const useTheme = () => (useColorScheme() === 'dark' ? dark : light);

export const radius = { chip: 20, input: 14, button: 14, card: 18, sheet: 26 };
export const space = { xs: 4, sm: 8, md: 12, lg: 16, xl: 24 };

// Text styles from the typography table.
export const type = {
  h1: { fontSize: 24, lineHeight: 32, fontWeight: '700' },
  title: { fontSize: 17, lineHeight: 24, fontWeight: '600' },
  body: { fontSize: 15, lineHeight: 22 },
  label: { fontSize: 14, lineHeight: 20, fontWeight: '600' },
  caption: { fontSize: 12, lineHeight: 16, fontWeight: '500' },
};

// [emoji, colour] for each file type.
export const fileTypes = {
  folder: ['📁', '#F59E0B'], doc: ['📄', '#3B82F6'], sheet: ['📊', '#10B981'], pdf: ['📕', '#EF4444'],
  img: ['🖼️', '#EC4899'], video: ['🎬', '#8B5CF6'], audio: ['🎵', '#14B8A6'], zip: ['🗜️', '#64748B'],
};

export const formatSize = (kb) => (!kb ? '' : kb >= 1024 ? (kb / 1024).toFixed(1) + ' MB' : kb + ' KB');
export const ago = (d) => (d === 0 ? 'Today' : d === 1 ? 'Yesterday' : d + ' days ago');
