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

## قانون آپدیت بدون خطا در کافه‌بازار
بازار سه چیز را چک می‌کند: ۱) applicationId همان قبلی (ir.shaerpour.factorino، هرگز تغییر نده) ۲) همان کلید امضا ۳) versionCode بزرگ‌تر.
- versionCode خودکار برابر شماره‌ی اجرای Actions است و هر build بزرگ‌تر می‌شود؛ لازم نیست دستی عوض کنی.
- امضا همیشه از Secrets می‌آید؛ اگر Secrets نباشند، build متوقف می‌شود.
- در لاگ هر build مقدار SHA256 گواهی چاپ می‌شود؛ باید همیشه یکسان باشد.
- مخزن را پاک و دوباره نساز (شماره‌ی اجرا از ۱ شروع می‌شود). اگر مجبور شدی، در build.gradle.kts مقدار versionCode را با یک عدد پایه جمع کن.
