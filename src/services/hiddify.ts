export interface ProxyNode {
  type: 'vless'|'vmess'|'trojan'|'ss';
  name: string; address: string; port: number;
  id?: string; password?: string; method?: string;
  network: 'tcp'|'ws'|'grpc'|'h2';
  tls: boolean; sni?: string; wsPath?: string; wsHost?: string;
  grpcServiceName?: string; alpn?: string[];
  fp?: string; pbk?: string; sid?: string; flow?: string;
}
function b64decode(str:string):string {
  const s = str.replace(/-/g,'+').replace(/_/g,'/');
  try { return decodeURIComponent(escape(atob(s))); } catch { return atob(s); }
}
export function parseSubscription(raw:string):ProxyNode[] {
  const decoded = b64decode(raw.trim());
  const lines = decoded.split('\n').map(l=>l.trim()).filter(Boolean);
  const nodes:ProxyNode[] = [];
  for (const line of lines) {
    try {
      if (line.startsWith('vless://')) nodes.push(parseVless(line));
      else if (line.startsWith('vmess://')) nodes.push(parseVmess(line));
      else if (line.startsWith('trojan://')) nodes.push(parseTrojan(line));
      else if (line.startsWith('ss://')) nodes.push(parseSS(line));
    } catch(_) {}
  }
  return nodes;
}
function parseVless(uri:string):ProxyNode {
  const url=new URL(uri); const p=url.searchParams;
  return {type:'vless',name:decodeURIComponent(url.hash.slice(1)),id:url.username,address:url.hostname,port:parseInt(url.port),network:(p.get('type')||'tcp') as any,tls:p.get('security')==='tls'||p.get('security')==='reality',sni:p.get('sni')||undefined,wsPath:p.get('path')||undefined,wsHost:p.get('host')||undefined,fp:p.get('fp')||undefined,pbk:p.get('pbk')||undefined,sid:p.get('sid')||undefined,flow:p.get('flow')||undefined};
}
function parseVmess(uri:string):ProxyNode {
  const j=JSON.parse(b64decode(uri.slice(8)));
  return {type:'vmess',name:j.ps||j.add,id:j.id,address:j.add,port:parseInt(j.port),network:j.net||'tcp',tls:j.tls==='tls',sni:j.sni||j.host||undefined,wsPath:j.path||undefined,wsHost:j.host||undefined};
}
function parseTrojan(uri:string):ProxyNode {
  const url=new URL(uri); const p=url.searchParams;
  return {type:'trojan',name:decodeURIComponent(url.hash.slice(1)),password:url.username,address:url.hostname,port:parseInt(url.port),network:(p.get('type')||'tcp') as any,tls:true,sni:p.get('sni')||url.hostname,wsPath:p.get('path')||undefined};
}
function parseSS(uri:string):ProxyNode {
  const url=new URL(uri); const [method,password]=b64decode(url.username).split(':');
  return {type:'ss',name:decodeURIComponent(url.hash.slice(1)),method,password,address:url.hostname,port:parseInt(url.port),network:'tcp',tls:false};
}
export function buildXrayConfig(node:ProxyNode,socksPort=10808,httpPort=10809):string {
  const out = buildOutbound(node);
  return JSON.stringify({log:{loglevel:'warning'},inbounds:[{tag:'socks',port:socksPort,listen:'127.0.0.1',protocol:'socks',settings:{auth:'noauth',udp:true},sniffing:{enabled:true,destOverride:['http','tls','quic']}},{tag:'http',port:httpPort,listen:'127.0.0.1',protocol:'http'}],outbounds:[out,{tag:'direct',protocol:'freedom',settings:{}},{tag:'block',protocol:'blackhole',settings:{}}],routing:{domainStrategy:'IPIfNonMatch',rules:[{type:'field',ip:['geoip:private'],outboundTag:'direct'},{type:'field',domain:['geosite:category-ads-all'],outboundTag:'block'}]},dns:{servers:['1.1.1.1','8.8.8.8']}},null,2);
}
function buildOutbound(n:ProxyNode):any {
  const ss:any={network:n.network};
  if(n.network==='ws') ss.wsSettings={path:n.wsPath||'/',headers:{Host:n.wsHost||n.address}};
  if(n.network==='grpc') ss.grpcSettings={serviceName:n.grpcServiceName||''};
  if(n.tls){if(n.pbk){ss.security='reality';ss.realitySettings={serverName:n.sni||n.address,fingerprint:n.fp||'chrome',publicKey:n.pbk,shortId:n.sid||''};}else{ss.security='tls';ss.tlsSettings={serverName:n.sni||n.address,fingerprint:n.fp||''};}}
  if(n.type==='vless') return {tag:'proxy',protocol:'vless',settings:{vnext:[{address:n.address,port:n.port,users:[{id:n.id,encryption:'none',flow:n.flow||''}]}]},streamSettings:ss};
  if(n.type==='vmess') return {tag:'proxy',protocol:'vmess',settings:{vnext:[{address:n.address,port:n.port,users:[{id:n.id,alterId:0,security:'auto'}]}]},streamSettings:ss};
  if(n.type==='trojan') return {tag:'proxy',protocol:'trojan',settings:{servers:[{address:n.address,port:n.port,password:n.password}]},streamSettings:ss};
  return {tag:'proxy',protocol:'shadowsocks',settings:{servers:[{address:n.address,port:n.port,method:n.method||'aes-256-gcm',password:n.password}]},streamSettings:{network:'tcp'}};
}
