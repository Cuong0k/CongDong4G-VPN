import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import LoginScreen from '../screens/LoginScreen';
import RegisterScreen from '../screens/RegisterScreen';
import ForgotPasswordScreen from '../screens/ForgotPasswordScreen';
const Stack=createNativeStackNavigator();
export default function AuthNavigator(){
  return(<Stack.Navigator screenOptions={{headerShown:false,animation:'slide_from_right'}}><Stack.Screen name="Login" component={LoginScreen}/><Stack.Screen name="Register" component={RegisterScreen}/><Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen}/></Stack.Navigator>);
}
