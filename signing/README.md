# Khoá ký phát hành (đã mã hoá)

`release-keystore.enc` là kho khoá PKCS12 dùng để ký APK, **mã hoá AES-256-CBC (PBKDF2, 200 000 vòng)** bằng mật khẩu lưu trong secret `KEYSTORE_PASSWORD` của GitHub Actions. Không có mật khẩu thì không giải mã được.

Giải mã thủ công (cần mật khẩu):

```bash
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -a -in signing/release-keystore.enc -out release.jks
```
