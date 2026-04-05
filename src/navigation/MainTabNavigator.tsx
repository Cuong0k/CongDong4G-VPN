import React from 'react';
import {View,Text,StyleSheet} from 'react-native';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import HomeScreen from '../screens/HomeScreen';
import {PackagesScreen} from '../screens/PackagesScreen';
import OrdersScreen from '../screens/OrdersScreen';
import SettingsScreen from '../screens/SettingsScreen';
import {COLORS} from '../constants/colors';
import {useVpn} from '../context/VpnContext';
const Tab=createBottomTabNavigator();
function TabIcon({icon,label,focused,badge}:{icon:string;label:string;focused:boolean;badge?:boolean}){
  return(<View style={s.item}><View><Text style={[s.icon,{opacity:focused?1:0.4}]}>{icon}</Text>{badge&&<View style={s.badge}/>}</View><Text style={[s.label,{color:focused?COLORS.accent:COLORS.textMuted}]}>{label}</Text></View>);
}
export default function MainTabNavigator(){
  const{status}=useVpn();const on=status==='connected';
  return(<Tab.Navigator screenOptions={{headerShown:false,tabBarStyle:s.bar,tabBarShowLabel:false}}><Tab.Screen name="Home" component={HomeScreen} options={{tabBarIcon:({focused})=><TabIcon icon="🏠" label="Trang chủ" focused={focused} badge={on}/>}}/><Tab.Screen name="Packages" component={PackagesScreen} options={{tabBarIcon:({focused})=><TabIcon icon="📦" label="Gói cước" focused={focused}/>}}/><Tab.Screen name="Orders" component={OrdersScreen} options={{tabBarIcon:({focused})=><TabIcon icon="📋" label="Đơn hàng" focused={focused}/>}}/><Tab.Screen name="Settings" component={SettingsScreen} options={{tabBarIcon:({focused})=><TabIcon icon="⚙️" label="Cài đặt" focused={focused}/>}}/></Tab.Navigator>);
}
const s=StyleSheet.create({bar:{backgroundColor:'#0d1020',borderTopColor:COLORS.cardBorder,borderTopWidth:1,height:64,paddingBottom:8},item:{alignItems:'center',paddingTop:6},icon:{fontSize:22},label:{fontSize:10,fontWeight:'700',letterSpacing:0.5,marginTop:3},badge:{position:'absolute',top:-2,right:-2,width:8,height:8,borderRadius:4,backgroundColor:COLORS.green}});
