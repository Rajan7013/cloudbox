import React from 'react';
import { Pressable, Text, ActivityIndicator } from 'react-native';
import { useTheme, radius, type } from '../theme';

// variant: 'primary' (filled) or 'secondary' (soft). loading shows a spinner.
export default function Button({ label, onPress, variant = 'primary', loading, disabled, style }) {
  const t = useTheme();
  const primary = variant === 'primary';
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      style={({ pressed }) => [{
        height: 48, borderRadius: radius.button, alignItems: 'center', justifyContent: 'center',
        backgroundColor: primary ? t.primary : t.primaryContainer,
        opacity: disabled ? 0.4 : 1, transform: [{ scale: pressed ? 0.97 : 1 }],
      }, style]}
    >
      {loading ? <ActivityIndicator color={primary ? '#fff' : t.primary} />
        : <Text style={[type.label, { fontSize: 15, color: primary ? '#fff' : t.primary }]}>{label}</Text>}
    </Pressable>
  );
}
