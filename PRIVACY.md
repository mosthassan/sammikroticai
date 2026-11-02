# سياسة الخصوصية — تطبيق سام ميكروتك (SamMikrotik)
**تاريخ السريان:** نوفمبر 2026

تلتزم إدارة تطبيق **سام ميكروتك (SamMikrotik)** بحماية خصوصية المستخدمين وأمان بياناتهم المالية والتشغيلية بأعلى المعايير المهنية والتقنية. يوضح هذا المستند تفاصيل جمع البيانات واستخدامها وحمايتها.

---

### 1. مبدأ الحفظ المحلي أولاً (Local-Only Data Architecture)
- يتم تخزين كافة البيانات المحاسبية والمالية وسندات القبض والصرف، وفواتير المبيعات والمشتريات، وبيانات العملاء والموردين، ونتائج مسح قنوات الواي فاي، محلياً وبشكل كامل داخل قاعدة بيانات مشفرة ومحمية على جهاز المستخدم (Room SQLite Database).
- لا يقوم التطبيق بنقل أو مزامنة أي بيانات محاسبية تلقائياً إلى خوادم خارجية أو أطراف ثالثة دون إجراء صريح ومباشر من المستخدم.

### 2. الوصول إلى جهات الاتصال (No Contact Access)
- **لا يطلب التطبيق إذن قراءة جهات الاتصال العام (`READ_CONTACTS`) إطلاقاً.**
- عند رغبة المستخدم في تعبئة اسم أو رقم هاتف عميل أو مورد من دفتر العناوين، يُستخدم منتقي النظام الرسمي لنظام أندرويد (`Intent.ACTION_PICK`).
- يمنح منتقي النظام صلاحية قراءة مؤقتة لمعاينة السجل الفردي الذي حدده المستخدم يدوياً فقط، ولا يمكن للتطبيق قراءة باقي جهات الاتصال أو استخراج دفتر العناوين.

### 3. النسخ الاحتياطي السحابي المشفر الاختياري (Encrypted Cloud Backup)
- ميزة النسخ الاحتياطي السحابي عبر Google Firebase هي ميزة **اختيارية تماماً** ويتم تفعيلها حصرياً بطلب مباشر من المستخدم بعد تسجيل دخوله بحسابه الخاص.
- تُشفّر حزم النسخ الاحتياطي محلياً على جهاز المستخدم قبل إرسالها باستخدام خوارزمية التشفير المتماثل **AES-256-GCM**.
- يُشتق مفتاح التشفير من عبارة مرور سرية يحددها المستخدم عبر خوارزمية **PBKDF2WithHmacSHA256** بعدد 210,000 تكرار مع ملح تشفيري عشوائي (Salt).
- يتم تقسيم البيانات المشفرة إلى أجزاء صغيرة (≤ 400KB) مع توليد متجه تهيئة فريد (IV) لكل جزء على حدة، ولا يمكن لأي طرف (بما في ذلك مطورو التطبيق وخوادم Firebase) فك تشفير النسخة دون عبارة المرور التي يحتفظ بها المستخدم.

### 4. مساعد الذكاء الاصطناعي (AI Assistant)
- ميزة مساعد الذكاء الاصطناعي اختيارية وتعتمد على نموذج "إحضار مفتاحك الخاص" (Bring-Your-Own-Key - BYOK).
- لا يقوم التطبيق بتسريب سجلات الدفاتر المحاسبية في الخلفية، ويقتصر الإرسال فقط على نص الاستفسار الصريح الذي يكتبه المستخدم للنموذج المختار (Google Gemini أو OpenAI أو DeepSeek) عبر الاتصال المباشر والمشفر (HTTPS) بمزود الخدمة.

### 5. خلو تام من الإعلانات وأدوات التتبع (Zero Ads & Analytics)
- التطبيق خالٍ بنسبة 100% من حزم الإعلانات البرمجية (No Ad Networks).
- لا يحتوي التطبيق على أي مكتبات لتحليل السلوك أو تتبع نشاط المستخدم (No Analytics / No Trackers).

### 6. حق الحذف والتحكم بالبيانات (Data Deletion Path)
- يملك المستخدم السيطرة الكاملة على بياناته:
  - **محلياً:** يمكن للمستخدم مسح جميع البيانات من داخل إعدادات التطبيق أو عبر مسح بيانات التطبيق من إعدادات نظام أندرويد ("Clear Storage").
  - **سحابياً:** يمكن للمستخدم حذف نسخ التطبيق الاحتياطية السحابية من داخل واجهة إدارة النسخ السحابي في التطبيق في أي وقت.

---

## English Summary (Privacy Policy Overview)

**SamMikrotik** is committed to absolute data privacy and confidentiality:
1. **Local-First Accounting:** All general ledger records, invoices, vouchers, and party details are stored locally on your device in a sandboxed SQLite database.
2. **No Contact List Access:** The application completely eliminates the `READ_CONTACTS` permission. Contact imports strictly rely on Android's native single-contact picker with temporary permission grants.
3. **Client-Side Encrypted Cloud Backup:** Cloud backup to Firebase is purely optional and user-initiated. Backups are encrypted locally using AES-256-GCM with keys derived from the user's passphrase via PBKDF2 (210,000 rounds) before being split into chunks (≤ 400KB). The plaintext is never visible to cloud storage.
4. **Independent AI Integration:** The optional AI Assistant operates on a Bring-Your-Own-Key model and only transmits the user's explicit prompt to the chosen provider over HTTPS.
5. **No Ads or Analytics:** The app contains zero advertising frameworks, telemetry, or user tracking SDKs.
6. **Data Deletion:** Users retain full control to wipe local data via app/system settings and permanently delete cloud backups at any time.
