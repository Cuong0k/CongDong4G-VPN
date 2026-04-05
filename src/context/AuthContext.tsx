import React,{createContext,useContext,useState,useEffect,ReactNode} from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {authAPI} from '../services/api';
interface User{id:number;name:string;email:string;phone?:string;current_package?:{name:string;expires_at:string;days_left:number}|null;}
interface AuthCtx{user:User|null;token:string|null;isLoading:boolean;login:(e:string,p:string)=>Promise<void>;register:(d:any)=>Promise<void>;forgotPassword:(e:string)=>Promise<void>;logout:()=>Promise<void>;refreshProfile:()=>Promise<void>;}
const AuthContext=createContext<AuthCtx|null>(null);
export const AuthProvider=({children}:{children:ReactNode})=>{
  const[user,setUser]=useState<User|null>(null);
  const[token,setToken]=useState<string|null>(null);
  const[isLoading,setLoading]=useState(true);
  useEffect(()=>{(async()=>{try{const t=await AsyncStorage.getItem('auth_token');const u=await AsyncStorage.getItem('user_data');if(t&&u){setToken(t);setUser(JSON.parse(u));}}catch(_){}finally{setLoading(false);}})();},[]);
  const login=async(email:string,password:string)=>{const r=await authAPI.login({email,password});const{token:t,user:u}=r.data;await AsyncStorage.setItem('auth_token',t);await AsyncStorage.setItem('user_data',JSON.stringify(u));setToken(t);setUser(u);};
  const register=async(d:any)=>{await authAPI.register(d);};
  const forgotPassword=async(e:string)=>{await authAPI.forgotPassword(e);};
  const logout=async()=>{try{await authAPI.logout();}catch(_){}await AsyncStorage.multiRemove(['auth_token','user_data']);setToken(null);setUser(null);};
  const refreshProfile=async()=>{try{const r=await authAPI.profile();setUser(r.data.user);await AsyncStorage.setItem('user_data',JSON.stringify(r.data.user));}catch(_){}};
  return <AuthContext.Provider value={{user,token,isLoading,login,register,forgotPassword,logout,refreshProfile}}>{children}</AuthContext.Provider>;
};
export const useAuth=()=>{const c=useContext(AuthContext);if(!c)throw new Error('useAuth outside provider');return c;};
