import React from 'react';
import {StatusBar} from 'react-native';
import {NavigationContainer} from '@react-navigation/native';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import {GestureHandlerRootView} from 'react-native-gesture-handler';
import {AuthProvider} from './src/context/AuthContext';
import {VpnProvider} from './src/context/VpnContext';
import RootNavigator from './src/navigation/RootNavigator';
import {COLORS} from './src/constants/colors';

function App(): React.JSX.Element {
  return (
    <GestureHandlerRootView style={{flex: 1}}>
      <SafeAreaProvider>
        <AuthProvider>
          <VpnProvider>
            <NavigationContainer>
              <StatusBar
                barStyle="light-content"
                backgroundColor={COLORS.bg}
                translucent={false}
              />
              <RootNavigator />
            </NavigationContainer>
          </VpnProvider>
        </AuthProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

export default App;
