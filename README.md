# فاکتورینو — توسعه توسط تیم شاعرپور

## گرفتن APK از گیت‌هاب
1. مخزن جدید بسازید و همه‌ی فایل‌های این پوشه را push کنید (روی شاخه‌ی main).
2. تب Actions → «Build APK» اجرا می‌شود (یا Run workflow بزنید).
3. بعد از پایان، از پایین صفحه‌ی اجرا، فایل Factorino-release-apk را دانلود کنید.

## کلید امضا برای کافه‌بازار (مهم)
بدون تنظیم زیر، هر بار یک کلید موقت ساخته می‌شود و نمی‌توانید آپدیت بدهید.
یک‌بار روی سیستم خودتان بسازید:
    keytool -genkeypair -v -keystore release.jks -alias factorino -keyalg RSA -keysize 2048 -validity 10000
    base64 -w0 release.jks
در Settings → Secrets → Actions این‌ها را اضافه کنید: KEYSTORE_BASE64 (خروجی base64)، KEYSTORE_PASSWORD، KEY_ALIAS (رمز کلید و فایل را یکی بگیرید).
فایل release.jks را جای امن نگه دارید و هرگز commit نکنید.
