import React from 'react';
import { Pressable, View, Text } from 'react-native';
import { useTheme, radius, type, fileTypes, formatSize, ago } from '../theme';

export default function FileItem({ item, mode, onPress, onMore }) {
  const t = useTheme();
  const [emoji, color] = fileTypes[item.type];
  const meta = [formatSize(item.size), ago(item.d)].filter(Boolean).join(' · ');
  const name = (item.star ? '⭐ ' : '') + item.name;
  const shadow = { shadowColor: '#3C3C78', shadowOpacity: 0.09, shadowRadius: 10, shadowOffset: { width: 0, height: 4 }, elevation: 2 };
  const dots = (style) => (
    <Pressable onPress={onMore} hitSlop={10} style={[{ padding: 6 }, style]}>
      <Text style={{ fontSize: 20, fontWeight: '700', color: t.sub }}>⋮</Text>
    </Pressable>
  );

  if (mode === 'list') {
    return (
      <Pressable onPress={onPress} style={[{ flexDirection: 'row', alignItems: 'center', backgroundColor: t.surface, borderRadius: 16, padding: 10, marginBottom: 10 }, shadow]}>
        <View style={{ width: 44, height: 44, borderRadius: 12, backgroundColor: color + '22', alignItems: 'center', justifyContent: 'center', marginRight: 12 }}>
          <Text style={{ fontSize: 22 }}>{emoji}</Text>
        </View>
        <View style={{ flex: 1 }}>
          <Text numberOfLines={1} style={[type.body, { fontWeight: '600', color: t.text }]}>{name}</Text>
          <Text style={[type.caption, { color: t.sub }]}>{meta}</Text>
        </View>
        {dots()}
      </Pressable>
    );
  }
  return (
    <Pressable onPress={onPress} style={[{ flex: 1, margin: 6, backgroundColor: t.surface, borderRadius: radius.card, padding: 12 }, shadow]}>
      <View style={{ height: 74, borderRadius: 12, backgroundColor: color + '22', alignItems: 'center', justifyContent: 'center', marginBottom: 10 }}>
        <Text style={{ fontSize: 32 }}>{emoji}</Text>
      </View>
      <Text numberOfLines={1} style={[type.body, { fontWeight: '600', color: t.text, paddingRight: 14 }]}>{name}</Text>
      <Text style={[type.caption, { color: t.sub }]}>{meta}</Text>
      {dots({ position: 'absolute', right: 6, bottom: 20 })}
    </Pressable>
  );
}
