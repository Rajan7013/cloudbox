import React, { useEffect, useRef } from 'react';
import { Animated, Text, Pressable } from 'react-native';
import { type } from '../theme';

// Message at the bottom with an optional Undo. Hides itself after 4 seconds.
export default function Toast({ toast, onClose }) {
  const o = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    if (!toast) return;
    o.setValue(0);
    Animated.timing(o, { toValue: 1, duration: 250, useNativeDriver: true }).start();
    const id = setTimeout(onClose, 4000);
    return () => clearTimeout(id);
  }, [toast]);
  if (!toast) return null;
  return (
    <Animated.View style={{ position: 'absolute', left: 16, right: 90, bottom: 24, opacity: o, transform: [{ translateY: o.interpolate({ inputRange: [0, 1], outputRange: [20, 0] }) }], backgroundColor: '#1C2040', borderRadius: 16, paddingVertical: 12, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center' }}>
      <Text style={[type.body, { color: '#fff', flex: 1 }]}>{toast.msg}</Text>
      {toast.undo && (
        <Pressable onPress={() => { toast.undo(); onClose(); }}>
          <Text style={[type.label, { color: '#A5B4FC', marginLeft: 12 }]}>Undo</Text>
        </Pressable>
      )}
    </Animated.View>
  );
}
