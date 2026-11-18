import React, { useEffect, useRef } from 'react';
import { Animated } from 'react-native';
import { useTheme } from '../theme';

// A grey block that gently pulses while data loads.
export default function Skeleton({ style }) {
  const t = useTheme();
  const o = useRef(new Animated.Value(0.4)).current;
  useEffect(() => {
    const loop = Animated.loop(Animated.sequence([
      Animated.timing(o, { toValue: 1, duration: 600, useNativeDriver: true }),
      Animated.timing(o, { toValue: 0.4, duration: 600, useNativeDriver: true }),
    ]));
    loop.start();
    return () => loop.stop();
  }, [o]);
  return <Animated.View style={[{ backgroundColor: t.border, borderRadius: 12, opacity: o }, style]} />;
}
