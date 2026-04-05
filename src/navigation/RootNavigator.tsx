import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import {useAuth} from '../context/AuthContext';
import SplashScreen from '../screens/SplashScreen';
import AuthNavigator from './AuthNavigator';
import MainTabNavigator from './MainTabNavigator';
const Stack=createNativeStackNavigator();
export default function RootNavigator(){
  const{user,isLoading}=useAuth();
  if(isLoading)return <SplashScreen/>;
  return(<Stack.Navigator screenOptions={{headerShown:false,animation:'fade'}}>{user?<Stack.Screen name="Main" component={MainTabNavigator}/>:<Stack.Screen name="Auth" component={AuthNavigator}/>}</Stack.Navigator>);
}
