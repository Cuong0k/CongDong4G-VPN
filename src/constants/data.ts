export const PACKAGES = [
  { id:1, name:'Gói 1 Ngày', price:5000, priceStr:'5,000đ', duration:1, popular:false, color:'#00d4ff', features:['100 Mbps','2 thiết bị','AES-256','Không log'] },
  { id:2, name:'Gói 7 Ngày', price:25000, priceStr:'25,000đ', duration:7, popular:true, color:'#00e676', features:['200 Mbps','3 thiết bị','AES-256','Không log'] },
  { id:3, name:'Gói 30 Ngày', price:79000, priceStr:'79,000đ', duration:30, popular:false, color:'#ffd700', features:['500 Mbps','5 thiết bị','AES-256','Không log','Hỗ trợ ưu tiên'] },
  { id:4, name:'Gói 90 Ngày', price:199000, priceStr:'199,000đ', duration:90, popular:false, color:'#ff3b5c', features:['Không giới hạn','10 thiết bị','AES-256','Không log','Hỗ trợ 24/7'] },
];
export const DEFAULT_SERVERS = [
  {id:'vn-hn', name:'Hà Nội', country:'Vietnam', flag:'🇻🇳', host:'vn.befast.cfd'},
  {id:'cn-bj', name:'Bắc Kinh', country:'China', flag:'🇨🇳', host:'bj.befast.cfd'},
  {id:'cn-sh', name:'Thượng Hải', country:'China', flag:'🇨🇳', host:'sh.befast.cfd'},
  {id:'jp-tk', name:'Tokyo', country:'Japan', flag:'🇯🇵', host:'jp.befast.cfd'},
  {id:'sg-01', name:'Singapore', country:'Singapore', flag:'🇸🇬', host:'sg.befast.cfd'},
  {id:'hk-01', name:'Hong Kong', country:'Hong Kong', flag:'🇭🇰', host:'hk.befast.cfd'},
];
export const PAYMENT_METHODS = [
  {id:'bank', name:'Chuyển khoản ngân hàng', icon:'🏦'},
  {id:'momo', name:'Ví Momo', icon:'💜'},
  {id:'zalopay', name:'Ví ZaloPay', icon:'💙'},
  {id:'card', name:'Thẻ cào điện thoại', icon:'📱'},
];
