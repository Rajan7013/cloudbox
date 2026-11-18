import React, { useState } from 'react';
import { Text, View, StatusBar, useColorScheme } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { useTheme } from './src/theme';
import SignInScreen from './src/screens/SignInScreen';
import FilesScreen from './src/screens/FilesScreen';

const Tab = createBottomTabNavigator();

// Placeholder tabs - we build these in the next steps.
function Soon({ name }) {
  const t = useTheme();
  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: t.bg }}>
      <Text style={{ color: t.text, fontSize: 20, fontWeight: '700' }}>{name}</Text>
      <Text style={{ color: t.sub, marginTop: 6 }}>Coming in the next step</Text>
    </View>
  );
}

export default function App() {
  const [signedIn, setSignedIn] = useState(false);
  const t = useTheme();
  const dark = useColorScheme() === 'dark';
  const icon = (e) => () => <Text style={{ fontSize: 20 }}>{e}</Text>;

  return (
    <SafeAreaProvider>
      <StatusBar barStyle={dark ? 'light-content' : 'dark-content'} />
      {!signedIn ? (
        <SignInScreen onDone={() => setSignedIn(true)} />
      ) : (
        <NavigationContainer>
          <Tab.Navigator
            screenOptions={{
              headerShown: false,
              tabBarActiveTintColor: t.primary,
              tabBarInactiveTintColor: t.sub,
              tabBarStyle: { backgroundColor: t.surface, borderTopColor: t.border },
              tabBarLabelStyle: { fontSize: 11, fontWeight: '600' },
            }}
          >
            <Tab.Screen name="Home" options={{ tabBarIcon: icon('🏠') }}>{() => <Soon name="Home" />}</Tab.Screen>
            <Tab.Screen name="Files" component={FilesScreen} options={{ tabBarIcon: icon('📂') }} />
            <Tab.Screen name="Shared" options={{ tabBarIcon: icon('👥') }}>{() => <Soon name="Shared" />}</Tab.Screen>
            <Tab.Screen name="Starred" options={{ tabBarIcon: icon('⭐') }}>{() => <Soon name="Starred" />}</Tab.Screen>
          </Tab.Navigator>
        </NavigationContainer>
      )}
    </SafeAreaProvider>
  );
}
