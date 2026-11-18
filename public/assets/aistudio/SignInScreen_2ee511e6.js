import React, { useState } from 'react';
import { View, Text } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTheme, type } from '../theme';
import Button from '../components/Button';

export default function SignInScreen({ onDone }) {
  const t = useTheme();
  const [loading, setLoading] = useState(false);

  // Later this will call Google Sign-In. For now it just waits a moment.
  const google = () => { setLoading(true); setTimeout(onDone, 1000); };

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: t.bg, padding: 24, justifyContent: 'center' }}>
      <View style={{ alignItems: 'center', marginBottom: 32 }}>
        <View style={{ width: 92, height: 92, borderRadius: 28, backgroundColor: t.primary, alignItems: 'center', justifyContent: 'center', shadowColor: t.primary, shadowOpacity: 0.45, shadowRadius: 20, shadowOffset: { width: 0, height: 10 }, elevation: 8 }}>
          <Text style={{ fontSize: 44 }}>☁️</Text>
        </View>
        <Text style={[type.h1, { color: t.text, marginTop: 20, fontSize: 28 }]}>Welcome to CloudBox</Text>
        <Text style={[type.body, { color: t.sub, marginTop: 6 }]}>Sign in to open your Drive</Text>
      </View>
      <Button label="G   Continue with Google" onPress={google} loading={loading} />
      <Button label="Use demo data (no login)" variant="secondary" onPress={onDone} disabled={loading} style={{ marginTop: 12 }} />
      <Text style={[type.caption, { color: t.sub, textAlign: 'center', marginTop: 24 }]}>By continuing you accept the Terms and Privacy Policy</Text>
    </SafeAreaView>
  );
}
