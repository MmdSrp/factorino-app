#!/bin/sh
# برای ساخت روی کامپیوتر خودتان یک‌بار اجرا کنید
mkdir -p app/src/main/assets/fonts
for W in Regular Bold; do
  L=$(echo $W | tr 'A-Z' 'a-z')
  curl -fsSL -o app/src/main/assets/fonts/vazir_$L.ttf "https://cdn.jsdelivr.net/npm/vazirmatn@33.0.3/fonts/ttf/Vazirmatn-$W.ttf"
done
