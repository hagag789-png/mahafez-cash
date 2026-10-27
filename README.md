# محافظي كاش

تطبيق شخصي لمتابعة أرصدة **فودافون كاش** و**اتصالات كاش** و**الأهلي فون كاش**: إيداع وسحب مع جهة إجبارية، بحث بالجهة، اتجاه الرصيد، أكثر الجهات تعاملًا، نسخ احتياطي واستعادة.

## التنزيل

| | الرابط |
|---|---|
| 📱 تطبيق أندرويد (APK) | https://github.com/hagag789-png/mahafez-cash/releases/download/latest/MahafezCash.apk |
| 🌐 نسخة الويب (تعمل بدون إنترنت، تُثبَّت من المتصفح) | https://hagag789-png.github.io/mahafez-cash/ |

التثبيت فوق نسخة أقدم يحتفظ بالبيانات. البيانات تُحفظ على الجهاز فقط، فاستخدم زر **نسخة احتياطية** داخل التطبيق بانتظام.

## التحديث

1. عدّل `app/src/main/assets/index.html` (كل واجهة البرنامج فيه) وارفع رقم `versionCode`/`versionName` في `app/build.gradle`.
2. اضغط Commit على الفرع `main`.
3. خلال 3–5 دقائق يُبنى APK جديد على نفس رابط التنزيل، وتُحدَّث نسخة الويب تلقائيًا.

سجل البناء: تبويب **Actions**. لو فشل البناء يُفتح **Issue** تلقائيًا فيه سبب الفشل.

## البنية

```
app/src/main/assets/index.html   واجهة البرنامج (HTML/JS، تخزين محلي)
app/src/main/assets/sw.js        للعمل بدون إنترنت في نسخة الويب
app/src/main/java/.../MainActivity.java   غلاف أندرويد: WebView + حفظ/مشاركة الملفات
.github/workflows/build-apk.yml  البناء والنشر التلقائي
```

## البناء محليًا
Android Studio ← Open ← هذا المجلد ← Build APK، أو `./gradlew assembleRelease`.
