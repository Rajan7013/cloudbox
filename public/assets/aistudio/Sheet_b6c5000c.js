import React from 'react';
import { Modal, View, Pressable, KeyboardAvoidingView, Platform } from 'react-native';
import { useTheme } from '../theme';

// Bottom sheet: dark scrim, rounded top, grabber. Tap outside to close.
export default function Sheet({ visible, onClose, children }) {
  const t = useTheme();
  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={{ flex: 1, backgroundColor: 'rgba(10,12,30,0.5)' }} onPress={onClose} />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={{ backgroundColor: t.surface, borderTopLeftRadius: 26, borderTopRightRadius: 26, padding: 18, paddingBottom: 30 }}>
          <View style={{ width: 42, height: 5, borderRadius: 5, backgroundColor: t.border, alignSelf: 'center', marginBottom: 14 }} />
          {children}
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}
