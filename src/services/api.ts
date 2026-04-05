import axios, {AxiosInstance} from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {API_BASE_URL} from '../constants/config';

const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL, timeout: 15000,
  headers: {'Content-Type': 'application/json'},
});

api.interceptors.request.use(async config => {
  const token = await AsyncStorage.getItem('auth_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  r => r,
  async e => {
    if (e.response?.status === 401)
      await AsyncStorage.multiRemove(['auth_token','user_data']);
    return Promise.reject(e);
  },
);

export const authAPI = {
  login: (d:{email:string;password:string}) => api.post('/auth/login', d),
  register: (d:any) => api.post('/auth/register', d),
  forgotPassword: (email:string) => api.post('/auth/forgot-password', {email}),
  logout: () => api.post('/auth/logout'),
  profile: () => api.get('/auth/me'),
};
export const packagesAPI = {
  list: () => api.get('/packages'),
  buy: (pkgId:number, method:string) => api.post('/packages/buy', {package_id:pkgId, payment_method:method}),
};
export const ordersAPI = {
  list: (page=1) => api.get(`/orders?page=${page}`),
  detail: (id:string) => api.get(`/orders/${id}`),
};
export const vpnAPI = {
  getSubscription: () => api.get('/vpn/subscription'),
  servers: () => api.get('/vpn/servers'),
  reportConnect: (serverId:string, deviceId:string) => api.post('/vpn/connect', {server_id:serverId, device_id:deviceId}),
  reportDisconnect: (sessionId:string, bIn:number, bOut:number) => api.post('/vpn/disconnect', {session_id:sessionId, bytes_in:bIn, bytes_out:bOut}),
  stats: () => api.get('/vpn/stats'),
};
export default api;
