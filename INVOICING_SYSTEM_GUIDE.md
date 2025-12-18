# Számlázó Rendszer Implementációs Útmutató

## 📋 Projekt státusz

### ✅ Elkészült komponensek

#### 1. **Modellek (Entities)**
- ✅ `CompanySettings` - Vállalati beállítások (adószám, bankszámla, NAV beállítások)
- ✅ `Customer` - Vevő/vásárló entitás (teljes címkezelés, GDPR kompatibilis)
- ✅ `Invoice` - Számla entitás (állapotkezelés, összegszámítás)
- ✅ `InvoiceItem` - Számla tétel (automatikus ÁFA számítás, kedvezmények)

#### 2. **DTO-k**
- ✅ `MonthlyWorkSummary` - Havi munkanapló összesítések
- ✅ `EmployeeMonthlySummary` - Alkalmazotti havi statisztikák
- ✅ `PageRequest` és `Page<T>` - Pagination támogatás

#### 3. **Service réteg**
- ✅ `MonthlyReportService` - Havi kimutatások kezelése
- ✅ `NavInvoiceXmlGenerator` - NAV Online Invoice 3.0 XML generátor
- ✅ Pagination támogatás repository rétegben

#### 4. **Teljesítmény optimalizálás**
- ✅ Pagination implementáció (Firebase és JDBC)
- ✅ Batch operations támogatás
- ✅ Connection pooling (HikariCP)

#### 5. **Verzió frissítések**
- ✅ Spring Boot 3.2.0 → 3.4.1
- ✅ JavaFX 21.0.1 → 23.0.1
- ✅ Apache POI 5.2.3 → 5.3.0
- ✅ Hibernate 6.4.1 → 6.6.4
- ✅ Java 17 → Java 21 LTS
- ✅ iText PDF library hozzáadva (8.0.5)
- ✅ JAXB XML processing library-k

---

## 🔨 Szükséges további fejlesztések

### 1. Repository és Service réteg bővítése

#### Customer Repository
```java
// Létrehozandó fájlok:
- CustomerRepository interface
- JdbcCustomerRepository implementation
- FirebaseCustomerRepository implementation
- CustomerService interface
- CustomerServiceImpl implementation
```

Főbb funkciók:
- CRUD műveletek vevőkre
- Keresés név, adószám szerint
- Aktív/inaktív vevők szűrése
- Lapozható lista

#### Invoice Repository
```java
// Létrehozandó fájlok:
- InvoiceRepository interface
- JdbcInvoiceRepository implementation (tételekkel együtt)
- FirebaseInvoiceRepository implementation
- InvoiceItemRepository interface
- InvoiceService interface
- InvoiceServiceImpl implementation
```

Főbb funkciók:
- Számla CRUD műveletek
- Számla generálás (számlaszám automatikus)
- Számla státusz kezelés (draft → issued → paid)
- Keresés számlaszám, vevő, dátum szerint
- NAV-ba küldés státusz kezelése
- PDF generálás

#### CompanySettings Repository
```java
// Létrehozandó fájlok:
- CompanySettingsRepository interface
- CompanySettingsServiceImpl bővítése
```

### 2. Database Schema frissítések

Adatbázis táblák létrehozása minden schema initializer-ben:

**MySQL/PostgreSQL/H2 táblák:**
```sql
CREATE TABLE company_settings (
    id VARCHAR(50) PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    company_address VARCHAR(500),
    company_zip_code VARCHAR(10),
    company_city VARCHAR(100),
    company_tax_number VARCHAR(50) UNIQUE,
    company_eu_tax_number VARCHAR(50),
    company_bank_account VARCHAR(50),
    company_bank_name VARCHAR(100),
    company_email VARCHAR(100),
    company_phone VARCHAR(30),
    company_website VARCHAR(255),
    company_logo_path VARCHAR(500),
    nav_technical_user VARCHAR(100),
    nav_signature_key VARCHAR(255),
    nav_replacement_key VARCHAR(255),
    nav_test_mode BOOLEAN DEFAULT TRUE,
    invoice_prefix VARCHAR(10) DEFAULT 'INV',
    invoice_next_number INT DEFAULT 1,
    invoice_footer_text TEXT,
    default_payment_deadline_days INT DEFAULT 8,
    default_currency VARCHAR(3) DEFAULT 'HUF',
    default_vat_rate DECIMAL(5,2) DEFAULT 27.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE customers (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tax_number VARCHAR(50),
    eu_tax_number VARCHAR(50),
    zip_code VARCHAR(10),
    city VARCHAR(100),
    address VARCHAR(500),
    country VARCHAR(100) DEFAULT 'Magyarország',
    email VARCHAR(100),
    phone VARCHAR(30),
    contact_person VARCHAR(100),
    billing_address VARCHAR(500),
    billing_zip_code VARCHAR(10),
    billing_city VARCHAR(100),
    billing_country VARCHAR(100),
    payment_deadline_days INT DEFAULT 8,
    notes TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    is_company BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tax_number (tax_number),
    INDEX idx_name (name),
    INDEX idx_active (is_active)
);

CREATE TABLE invoices (
    id VARCHAR(50) PRIMARY KEY,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    invoice_date DATE NOT NULL,
    payment_deadline DATE,
    delivery_date DATE,
    payment_date DATE,
    payment_method VARCHAR(50) DEFAULT 'Átutalás',
    currency VARCHAR(3) DEFAULT 'HUF',
    net_amount DECIMAL(12,2) NOT NULL,
    vat_amount DECIMAL(12,2) NOT NULL,
    gross_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    is_paid BOOLEAN DEFAULT FALSE,
    is_sent_to_nav BOOLEAN DEFAULT FALSE,
    nav_transaction_id VARCHAR(100),
    nav_sent_at TIMESTAMP,
    footer_text TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    INDEX idx_invoice_number (invoice_number),
    INDEX idx_customer (customer_id),
    INDEX idx_invoice_date (invoice_date),
    INDEX idx_status (status),
    INDEX idx_is_paid (is_paid)
);

CREATE TABLE invoice_items (
    id VARCHAR(50) PRIMARY KEY,
    invoice_id VARCHAR(50) NOT NULL,
    line_number INT NOT NULL,
    description VARCHAR(500) NOT NULL,
    unit_of_measure VARCHAR(20) DEFAULT 'db',
    quantity DECIMAL(12,4) NOT NULL DEFAULT 1.0000,
    unit_price DECIMAL(12,2) NOT NULL,
    vat_rate DECIMAL(5,2) NOT NULL DEFAULT 27.00,
    net_amount DECIMAL(12,2) NOT NULL,
    vat_amount DECIMAL(12,2) NOT NULL,
    gross_amount DECIMAL(12,2) NOT NULL,
    discount_percent DECIMAL(5,2) DEFAULT 0.00,
    discount_amount DECIMAL(12,2) DEFAULT 0.00,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    INDEX idx_invoice (invoice_id)
);
```

### 3. PDF generálás iText használatával

Létrehozandó: `InvoicePdfGenerator.java`

Funkciók:
- Vállalati fejléc (logó, adatok)
- Vevő adatok
- Számla tételek táblázat
- ÁFA összesítő
- Fizetési információk
- Magyar nyelvi formázás
- QR kód NAV ellenőrzéshez (opcionális)

### 4. NAV Online Invoice API integráció

#### NAV API technikai követelmények:

**Hivatalos dokumentáció:**
- https://onlineszamla.nav.gov.hu/dokumentaciok
- NAV Online Számla Rendszer 3.0
- Technikai felhasználó és aláíró kulcs szükséges

**Szükséges lépések:**
1. ✅ XML generálás (elkészült: `NavInvoiceXmlGenerator`)
2. ⏳ SHA512 hash számítás aláíráshoz
3. ⏳ Base64 kódolás
4. ⏳ HTTP POST kérés NAV API-hoz
5. ⏳ Válasz feldolgozás
6. ⏳ Hibakezelés és retry logika

**NAV API végpontok:**
- TESZT: https://api-test.onlineszamla.nav.gov.hu/invoiceService/v3
- ÉLES: https://api.onlineszamla.nav.gov.hu/invoiceService/v3

**Implementálandó műveletek:**
- `manageInvoice` - Számla adatszolgáltatás
- `queryInvoiceStatus` - Számla státusz lekérdezés
- `queryTaxpayer` - Adózó lekérdezés (adószám validáció)

#### NavApiService létrehozása

```java
@Service
public class NavApiService {

    // Számla beküldése NAV-hoz
    CompletableFuture<NavResponse> sendInvoice(Invoice invoice);

    // Számla státusz lekérdezése
    NavInvoiceStatus queryInvoiceStatus(String transactionId);

    // Adószám validáció
    NavTaxpayerData queryTaxpayer(String taxNumber);

    // SHA512 aláírás generálás
    String generateRequestSignature(String requestXml, String timestamp);
}
```

### 5. UI komponensek létrehozása

#### CustomerDialog.java
- Vevő létrehozás/szerkesztés
- Validáció (adószám formátum)
- Számlázási cím kezelés

#### InvoiceDialog.java
- Számla létrehozás/szerkesztés
- Vevő választó ComboBox
- Tételek táblázat szerkesztő
- Automatikus összegszámítás
- ÁFA kulcs választó
- Dátumválasztók

#### CompanySettingsDialog.java
- Vállalati adatok szerkesztése
- NAV beállítások
- Számla sablonok

#### MainView.fxml bővítése
Új tabok hozzáadása:
- **"Vevők"** tab - Customer management
- **"Számlák"** tab - Invoice management
- **"Beállítások"** tab - Company settings

### 6. Menürendszer bővítése

```xml
<MenuBar>
    <Menu text="Számlázás">
        <MenuItem text="Új számla" />
        <MenuItem text="Számla lista" />
        <MenuItem text="Új vevő" />
        <MenuItem text="Vevők kezelése" />
        <SeparatorMenuItem />
        <MenuItem text="NAV-ba küldés" />
        <MenuItem text="PDF generálás" />
    </Menu>
    <Menu text="Beállítások">
        <MenuItem text="Vállalati adatok" />
        <MenuItem text="NAV beállítások" />
        <MenuItem text="Adatbázis kapcsolat" />
    </Menu>
</MenuBar>
```

---

## 🔐 Biztonsági követelmények

### 1. Adatvédelem (GDPR)
- ✅ Vevő adatok titkosítása adatbázisban (opcionális)
- ⏳ Audit log implementálása (ki, mikor, mit módosított)
- ⏳ Adatok exportálása (vevő kérésére)
- ⏳ Adatok törlése (right to be forgotten)
- ⏳ Hozzájárulás kezelés

### 2. NAV kulcsok biztonsága
- NAV kulcsok titkosított tárolása
- Környezeti változók használata (production)
- `.env` fájl gitignore-ban

### 3. Bejelentkezési rendszer (későbbi verzió)
- Spring Security integráció
- Felhasználói szerepkörök (Admin, Manager, Viewer)
- Jelszó titkosítás (BCrypt)

---

## 📊 Jogszabályi megfelelés

### Számla kötelező adattartalma (2007. évi CXXVII. törvény)

#### Kiállító (eladó) adatai:
- ✅ Név
- ✅ Cím
- ✅ Adószám
- ✅ Bankszámlaszám (ha átutalásos fizetés)

#### Vevő adatai:
- ✅ Név
- ✅ Cím
- ✅ Adószám (ha adóalany)

#### Számla adatai:
- ✅ Számla sorszáma (egyedi, folyamatos)
- ✅ Kiállítás dátuma
- ✅ Teljesítés dátuma
- ✅ Fizetési határidő
- ✅ Fizetési mód

#### Tételek:
- ✅ Megnevezés
- ✅ Mennyiség
- ✅ Mértékegység
- ✅ Egységár
- ✅ ÁFA kulcs (%)
- ✅ Nettó érték
- ✅ ÁFA összeg
- ✅ Bruttó érték

#### Összesítők:
- ✅ Nettó végösszeg
- ✅ ÁFA végösszeg
- ✅ Bruttó végösszeg

### NAV Online Számla kötelezettség

**Köteles beküldeni:**
- Évi 100 millió Ft+ árbevétel VAGY
- Opcionálisan csatlakozás (ajánlott)

**Határidők:**
- Belföldi számla: 4 nap
- Export számla: 4 nap
- Egyszerűsített számla: 4 nap

**Adatszolgáltatás tartalma:**
- XML formátumú számla adatok
- Kriptográfiai aláírás (SHA512)
- Valós idejű validáció

---

## 🧪 Tesztelési terv

### Unit tesztek
```java
// Létrehozandó tesztek:
- InvoiceItemTest - összegszámítás tesztelése
- InvoiceTest - számla műveletek tesztelése
- NavInvoiceXmlGeneratorTest - XML validáció
- MonthlyReportServiceTest - összesítések tesztelése
```

### Integrációs tesztek
- Repository réteg tesztelése (H2 adatbázis)
- Service réteg tesztelése
- NAV API mock tesztek

### Manuális tesztek
- Számla létrehozás és PDF generálás
- NAV-ba küldés TESZT környezetben
- Vevő kezelés
- Excel export tesztelése

---

## 📦 Deployment checklist

### Fejlesztési környezet (Development)
- ✅ H2 in-memory adatbázis
- ✅ NAV teszt környezet
- ✅ Debug logging engedélyezve

### Teszt környezet (Staging)
- ⏳ MySQL/PostgreSQL adatbázis
- ⏳ NAV teszt környezet
- ⏳ INFO level logging

### Éles környezet (Production)
- ⏳ MySQL/PostgreSQL adatbázis (biztonsági mentéssel)
- ⏳ NAV éles környezet
- ⏳ WARN level logging
- ⏳ Titkosított NAV kulcsok
- ⏳ SSL/TLS kapcsolat
- ⏳ Automatikus backup

---

## 🚀 Következő lépések prioritás szerint

### 1. Magas prioritás (1-2 hét)
1. ✅ Modellek és DTO-k (KÉSZ)
2. ✅ NAV XML generátor (KÉSZ)
3. ⏳ Repository implementációk
4. ⏳ Service réteg implementációk
5. ⏳ Database schema frissítések

### 2. Közepes prioritás (2-4 hét)
6. ⏳ PDF generálás (iText)
7. ⏳ NAV API integráció
8. ⏳ UI komponensek (Customer, Invoice dialógok)
9. ⏳ MainView bővítése új tabokkal

### 3. Alacsony prioritás (1-2 hónap)
10. ⏳ Bejelentkezési rendszer
11. ⏳ Audit log
12. ⏳ GDPR funkciók
13. ⏳ Email integrációk
14. ⏳ Riportok bővítése

---

## 📚 Hasznos linkek és dokumentációk

### NAV Online Számla
- [NAV Online Számla főoldal](https://onlineszamla.nav.gov.hu/)
- [Technikai dokumentáció 3.0](https://onlineszamla.nav.gov.hu/api/files/container/download/Online%20Szamla_Interfesz%20specifik%C3%A1ci%C3%B3_v3.0.pdf)
- [XSD sémák letöltése](https://onlineszamla.nav.gov.hu/api/files/container/download/invoice_xsd.zip)
- [Fejlesztői portál](https://onlineszamla-test.nav.gov.hu/)

### Magyar jogszabályok
- [2007. évi CXXVII. törvény az ÁFA-ról](https://net.jogtar.hu/jogszabaly?docid=a0700127.tv)
- [2017. évi CL. törvény az adózás rendjéről](https://net.jogtar.hu/jogszabaly?docid=a1700150.tv)

### Technológiák
- [iText PDF dokumentáció](https://itextpdf.com/en/resources/api-documentation)
- [Spring Boot 3.4 dokumentáció](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [JavaFX 23 dokumentáció](https://openjfx.io/javadoc/23/)

---

## ✅ Elkészült funkciók összefoglalása

1. **Verzió frissítések**: Java 21, Spring Boot 3.4.1, JavaFX 23
2. **Havi munkanapló**: MonthlyReportService, összesítések
3. **Pagination**: Teljes implementáció Firebase és JDBC
4. **Számlázó modellek**: Customer, Invoice, InvoiceItem, CompanySettings
5. **NAV XML generátor**: Teljes NAV 3.0 XML formátum támogatás
6. **Adatbázis tervezés**: Teljes schema definíció

**Következő lépés**: Repository és Service réteg implementációja, majd UI komponensek.
