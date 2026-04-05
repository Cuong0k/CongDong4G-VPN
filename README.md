# CongDong4G VPN — React Native App

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Platform](https://img.shields.io/badge/platform-Android-green)

App VPN Android cho CongDong4G.com — Hiddify/Xray-core, không dùng SingBox.

## Tính năng
- Đăng ký / Đăng nhập / Quên mật khẩu
- Bật/Tắt VPN (Hiddify/Xray via Android VpnService)
- Xem & mua gói cước
- Lịch sử đơn hàng
- Cài đặt tài khoản
- Chọn máy chủ (VLESS/VMess/Trojan/SS)

## Build APK
```bash
npm install
cd android && ./gradlew assembleRelease
```

## API Config
Sửa `src/constants/config.ts`:
```ts
export const API_BASE_URL = 'https://panel.befast.cfd/api';
```
