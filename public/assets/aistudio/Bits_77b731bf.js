import React from 'react';
import { Pressable, Text, View } from 'react-native';
import { useTheme, type, radius } from '../theme';

// Chip: selected = primary fill, otherwise soft.
export function Chip({ label, on, onPress, style }) {
  const t = useTheme();
  return (
    <Pressable onPress={onPress} style={[{ height: 36, paddingHorizontal: 14, borderRadius: radius.chip, justifyContent: 'center', backgroundColor: on ? t.primary : t.primaryContainer, marginRight: 8, marginBottom: 8 }, style]}>
      <Text style={[type.label, { color: on ? '#fff' : t.primary }]}>{label}</Text>
    </Pressable>
  );
}

// One tappable row in an actions sheet.
export function SheetRow({ icon, label, onPress, danger }) {
  const t = useTheme();
  return (
    <Pressable onPress={onPress} style={({ pressed }) => ({ flexDirection: 'row', alignItems: 'center', paddingVertical: 10, opacity: pressed ? 0.6 : 1 })}>
      <View style={{ width: 36, height: 36, borderRadius: 12, backgroundColor: danger ? t.error + '22' : t.primaryContainer, alignItems: 'center', justifyContent: 'center', marginRight: 14 }}>
        <Text style={{ fontSize: 17 }}>{icon}</Text>
      </View>
      <Text style={[type.body, { fontWeight: '500', color: danger ? t.error : t.text }]}>{label}</Text>
    </Pressable>
  );
}

export const Overline = ({ children }) => {
  const t = useTheme();
  return <Text style={{ fontSize: 11, fontWeight: '700', letterSpacing: 0.5, color: t.sub, marginTop: 12, marginBottom: 8 }}>{String(children).toUpperCase()}</Text>;
};
