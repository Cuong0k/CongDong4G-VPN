import React,{createContext,useContext,useState,useEffect,useRef,ReactNode} from 'react';
import {NativeModules,NativeEventEmitter,Platform} from 'react-native';
import {vpnAPI} from '../services/api';
import {parseSubscription,buildXrayConfig,ProxyNode} from '../services/hiddify';
import {DEFAULT_SERVERS} from '../constants/data';
const{XrayVpn}=NativeModules;
const emitter=XrayVpn?new NativeEventEmitter(XrayVpn):null;
export type VpnStatus='disconnected'|'connecting'|'connected'|'disconnecting'|'error';
export interface VpnNode extends ProxyNode{serverId:string;flag:string;country:string;ping?:number;load?:number;}
interface VpnStats{bytesIn:number;bytesOut:number;speedIn:number;speedOut:number;elapsedSeconds:number;}
interface VpnCtx{status:VpnStatus;selectedNode:VpnNode|null;nodes:VpnNode[];stats:VpnStats;errorMsg:string;connect:(n?:VpnNode)=>Promise<void>;disconnect:()=>Promise<void>;selectNode:(n:VpnNode)=>void;loadNodes:()=>Promise<void>;}
const blank:VpnStats={bytesIn:0,bytesOut:0,speedIn:0,speedOut:0,elapsedSeconds:0};
const VpnContext=createContext<VpnCtx|null>(null);
export const VpnProvider=({children}:{children:ReactNode})=>{
  const[status,setStatus]=useState<VpnStatus>('disconnected');
  const[nodes,setNodes]=useState<VpnNode[]>([]);
  const[selectedNode,setSel]=useState<VpnNode|null>(null);
  const[stats,setStats]=useState<VpnStats>(blank);
  const[errorMsg,setError]=useState('');
  const sessionId=useRef<string|null>(null);
  const tick=useRef<any>(null);
  useEffect(()=>{if(!emitter)return;const s1=emitter.addListener('XrayVpnStatus',(e:any)=>{setStatus(e.status);if(e.status==='connected')startTimer();if(e.status==='disconnected'||e.status==='error')stopTimer();if(e.status==='error')setError(e.msg||'Lỗi VPN');});const s2=emitter.addListener('XrayVpnStats',(e:VpnStats)=>setStats(e));return()=>{s1.remove();s2.remove();};},[]);
  const startTimer=()=>{tick.current=setInterval(()=>{if(XrayVpn?.queryStats){XrayVpn.queryStats();}else{setStats(p=>({bytesIn:p.bytesIn+Math.random()*600000+100000,bytesOut:p.bytesOut+Math.random()*80000+20000,speedIn:Math.random()*600000+100000,speedOut:Math.random()*80000+20000,elapsedSeconds:p.elapsedSeconds+1}));}},1000);};
  const stopTimer=()=>{clearInterval(tick.current);tick.current=null;setStats(blank);};
  const loadNodes=async()=>{try{const sr=await vpnAPI.getSubscription();const svr=await vpnAPI.servers();const parsed=parseSubscription(sr.data.subscription||'');const meta:any[]=svr.data.servers||DEFAULT_SERVERS;const merged:VpnNode[]=parsed.map((n,i)=>{const m=meta[i%meta.length];return{...n,serverId:m?.id||`n${i}`,flag:m?.flag||'🌐',country:m?.country||'',ping:m?.ping,load:m?.load};});setNodes(merged);if(!selectedNode&&merged.length>0)setSel(merged[0]);}catch(_){setNodes([]);}};
  const connect=async(node?:VpnNode)=>{const t=node||selectedNode;if(!t){setError('Chưa chọn server');return;}setStatus('connecting');setError('');try{const r=await vpnAPI.reportConnect(t.serverId,'android');sessionId.current=r.data.session_id;const cfg=buildXrayConfig(t);if(Platform.OS==='android'&&XrayVpn){await XrayVpn.startVpn(cfg,t.name);}else{setTimeout(()=>{setStatus('connected');startTimer();},2000);}setSel(t);}catch(e:any){setStatus('error');setError(e?.response?.data?.message||'Kết nối thất bại');}}
  const disconnect=async()=>{setStatus('disconnecting');try{if(Platform.OS==='android'&&XrayVpn){await XrayVpn.stopVpn();}else{setTimeout(()=>setStatus('disconnected'),800);}if(sessionId.current){await vpnAPI.reportDisconnect(sessionId.current,stats.bytesIn,stats.bytesOut);sessionId.current=null;}}catch{setStatus('disconnected');}stopTimer();};
  return <VpnContext.Provider value={{status,selectedNode,nodes,stats,errorMsg,connect,disconnect,selectNode:setSel,loadNodes}}>{children}</VpnContext.Provider>;
};
export const useVpn=()=>{const c=useContext(VpnContext);if(!c)throw new Error('outside VpnProvider');return c;};
