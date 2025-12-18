# Számlázási Jogszabályi Követelmények 2025

## 📜 Jogszabályi Háttér

### Fő jogszabályok:
- **2007. évi CXXVII. törvény** - Általános Forgalmi Adóról (ÁFA tv.) 169. §
- **23/2014. (VI. 30.) NGM rendelet** - Számla és nyugta adóigazgatási azonosításáról
- **EU HÉA-irányelv** - Uniós harmonizált szabályok

---

## ✅ Számla Kötelező Adattartalma (Áfa tv. 169. §)

### 1. Alapvető kötelező elemek:

#### Számlakibocsátó (Eladó) adatai:
- ✅ Név
- ✅ Cím (irányítószám, település, közterület)
- ✅ Adószám (amely alatt a teljesítést végezte)
- ✅ Bankszámlaszám (átutalás esetén)

#### Vevő adatai:
- ✅ Név
- ✅ Cím (irányítószám, település, közterület)
- ✅ Adószám (ha belföldi adóalany) - **2025. július 1-től kötelező!**

#### Számla azonosítók:
- ✅ Számla sorszáma (egyedi, folyamatos, törés nélküli)
- ✅ Számla kibocsátásának kelte (ÉÉÉÉ-HH-NN)
- ✅ Teljesítés dátuma (vagy időszak kezdete és vége)

#### Termék/Szolgáltatás adatok:
- ✅ Megnevezés (egyértelmű, pontos)
- ✅ Mennyiség
- ✅ Mértékegység
- ✅ Nettó egységár (ÁFA nélkül)

#### Pénzügyi adatok:
- ✅ Nettó érték (adó nélküli összeg)
- ✅ ÁFA kulcs (%)
- ✅ ÁFA összeg
- ✅ Bruttó végösszeg
- ✅ Pénznem (HUF, EUR, stb.)
- ✅ Fizetési mód (átutalás, készpénz, kártya, stb.)
- ✅ Fizetési határidő (ha van)

---

## 🔍 Esetfüggő Kötelező Elemek

### 1. Fordított adózás
**Feltétel:** Belföldi fordított adózás esetén
**Kötelező megjelölés:** `"fordított adózás"` kifejezés
**ÁFA záradék kód:** Kötelező kitölteni!

### 2. Pénzforgalmi elszámolás
**Feltétel:** Ha az adóalany pénzforgalmi elszámolást választott
**Kötelező megjelölés:** `"pénzforgalmi elszámolás"` kifejezés

### 3. Adómentes értékesítés
**Feltétel:** ÁFA mentes termék/szolgáltatás
**Kötelező:** Jogszabályi hivatkozás vagy utalás (pl. "ÁFA mentes")
**Indok mező:** Kötelező kitölteni az indokot!

### 4. ÁFA tv. hatályán kívüli
**Feltétel:** Áfa törvény hatálya nem terjed ki a tételre
**Kötelező:** ÁFA hatályán kívüliség kódja

### 5. Önszámlázás
**Feltétel:** A vevő állítja ki a számlát
**Kötelező megjelölés:** `"önszámlázás"` kifejezés

### 6. Különbözet szerinti szabályozás
**Feltétel:** Utazásszervezési szolgáltatás
**Kötelező megjelölés:** `"különbözet szerinti szabályozás - utazási irodák"`

---

## 📊 NAV Online Számla 3.0 Követelmények

### Kötelező XML mezők:

#### Hitelesítési adatok:
- ✅ Technikai felhasználó neve
- ✅ Aláíró kulcs (requestSignature)
- ✅ Időbélyeg

#### Címadatok (3 kötelező elem):
- ✅ Irányítószám (`postalCode`)
- ✅ Település neve (`city`)
- ✅ Közterület neve (`additionalAddressDetail`)

#### Pénzügyi mezők:
- ✅ Árfolyam - **MINDEN számlánál kötelező** (forintos számlánál is! érték: 1)
- ✅ Fizetési mód - **Kötelező az adatszolgáltatásra kötelezett tételeknél**
- ✅ Pénznem kód (`currencyCode`)

#### ÁFA specifikus:
- ✅ ÁFA kulcs (`lineVatRate`)
- ✅ ÁFA tartalom jelzése (`lineVatContent`)
- ✅ ÁFA záradék kód (fordított adózás esetén)
- ✅ Indok (adómentes/hatályon kívüli esetén)

---

## 🆕 2025-ös Változások

### 1. Kötelező Elektronikus Számlázás (2025. július 1.)
- Közműszolgáltatók (villamos energia, földgáz) **kötelezően e-számlát állítanak ki** vállalkozásoknak
- Vállalkozások **csak elektronikus formában fogadhatják be** közműszámlákat

### 2. Belföldi Adóalany Vevő Adószáma
- **2025. július 1-től kötelező** feltüntetni a belföldi adóalany vevő adószámát a számlán

### 3. Elektronikus Számla Megőrzés
- Elektronikus számlákat **kizárólag elektronikus formában** kell megőrizni Magyarországon
- Feldolgozás céljára kinyomtatható, de a megőrzés digitális!

### 4. Adódigitalizációs Változások
- Bővült az Összesítő Jelentés (M lap) adattartalma
- Önkéntesen jelenthetőek a befogadott számlákon levonásba helyezett összegek
- Cél: adategyezőség és számlaadat-rekonsziliáció

---

## ✋ NEM Kötelező Elemek

### Aláírás
Az EU HÉA-irányelv szerint a tagállamok **nem követelhetik meg** a számla aláírását.
➡️ **A számla aláírás nélkül is érvényes!**

### KATA "Kisadózó" jelölés
2025-től a KATA változásával a **"Kisadózó" kifejezés feltüntetése már NEM kötelező**.

---

## 🔐 Hitelesség, Sértetlenség, Olvashatóság

### Követelmények (változatlanok):
1. **Hitelesség:** Biztosítani kell, hogy a számla kiállítója beazonosítható legyen
2. **Sértetlenség:** A számla tartalma ne legyen módosítható utólag
3. **Olvashatóság:** A számla könnyen olvasható és értelmezhető formában legyen tárolva

### Megoldási módok:
- Elektronikus aláírás
- EDI (Electronic Data Interchange)
- Bármilyen egyéb üzleti kontroll

---

## 📋 Implementációs Checklist

### Minimális Kötelező Mezők:
- [ ] Számla sorszám (egyedi, folyamatos)
- [ ] Kiállítás dátuma
- [ ] Teljesítés dátuma
- [ ] Eladó: név, cím, adószám, bankszámla
- [ ] Vevő: név, cím, adószám (ha adóalany)
- [ ] Termék/szolgáltatás megnevezése
- [ ] Mennyiség + mértékegység
- [ ] Nettó egységár
- [ ] Nettó összeg
- [ ] ÁFA kulcs (%)
- [ ] ÁFA összeg
- [ ] Bruttó végösszeg
- [ ] Pénznem
- [ ] Fizetési mód
- [ ] Fizetési határidő

### Esetfüggő Mezők:
- [ ] "fordított adózás" (ha alkalmazandó)
- [ ] "pénzforgalmi elszámolás" (ha alkalmazandó)
- [ ] Adómentességi hivatkozás (ha alkalmazandó)
- [ ] ÁFA hatályon kívüli kód (ha alkalmazandó)

### NAV Online Számla Export:
- [ ] XML 3.0 formátum
- [ ] Hitelesítési adatok (technikai felhasználó)
- [ ] Árfolyam mező (MINDEN számlánál!)
- [ ] Fizetési mód kód
- [ ] Címadatok: irányítószám, település, közterület

---

## 📚 Források

- [Áfa tv. - 2007. évi CXXVII. törvény](https://net.jogtar.hu/jogszabaly?docid=a0700127.tv)
- [Számla kötelező tartalmi elemei 2025](https://www.naturasoft.hu/cikkek/szamla-kotelezo-tartalmi-elemei.php)
- [NAV Online Számla 3.0 Interfész Specifikáció](https://autosoft.hu/wp-content/uploads/2025/04/Online_Szamla_interfesz-specifikacio_HU_v3.0.pdf)
- [Kötelező elektronikus számlázás 2025](https://www.pwc.com/hu/hu/sajtoszoba/2024/kotelezo_elektronikus_szamlazas.html)
- [NAV Információs Füzet - Számla, nyugta kibocsátásának alapvető szabályai](https://nav.gov.hu/pfile/file?path=/ugyfeliranytu/nezzen-utana/inf_fuz/rejtett/Informacios-fuzetek---Aktualis/18.-informacios-fuzet---A-szamla-nyugta-kibocsatasanak-alapveto-szabalyai)

---

**Utolsó frissítés:** 2025-12-18
**Verzió:** 1.0
