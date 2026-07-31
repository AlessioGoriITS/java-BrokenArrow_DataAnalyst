# Battle Debrief

Battle Debrief è un'applicazione web con backend REST per importare e
analizzare le prestazioni dei giocatori di *Broken Arrow*. L'applicazione
confronta il valore delle unità
schierate e perse con quello delle unità avversarie distrutte, sia per singolo
giocatore sia sull'intero dataset locale.

Il progetto è sviluppato come project work Java Backend con Spring Boot, Maven,
Spring Web MVC, Spring Data JPA, Spring Security, MySQL e Docker.

## Funzionalità

- Command dashboard e Analytics basate sullo Steam ID dell'account;
- Hangar con ricerca, filtri, paginazione e dettaglio degli asset;
- visualizzazioni aggregate per unità, mappe e specializzazioni;
- account personale con registrazione, login JWT e Steam ID collegato;
- dossier con KPI, andamento ELO e after-action report del profilo collegato;
- integrazione live con BArmory e fallback automatico BattleGroup;
- registrazione, JWT e ruoli `USER`/`ADMIN` integrati nel servizio;
- gestione degli utenti e dei profili giocatore;
- catalogo pubblico completo con 420 unità e 11 specializzazioni;
- CRUD amministrativo del catalogo;
- caricamento automatico di un catalogo JSON versionato;
- importazione transazionale della match history in formato JSON;
- ricerca paginata e filtrabile delle partite;
- dettaglio completo di partite e prestazioni;
- statistiche di carriera e andamento temporale del giocatore;
- statistiche delle unità utilizzate dal giocatore;
- statistiche aggregate sul dataset locale;
- gestione esplicita delle divisioni per zero;
- health check per l'ambiente Docker.

## Tecnologie

- Java 21;
- Spring Boot 3.5;
- Maven e Maven Wrapper;
- Spring Web MVC;
- Spring Data JPA e Hibernate;
- Spring Security;
- JWT tramite JJWT;
- MySQL 8.4;
- H2 in modalità compatibilità MySQL per i test;
- JUnit 5, MockMvc e AssertJ;
- JaCoCo;
- Docker e Docker Compose;
- HTML, CSS e JavaScript senza dipendenze runtime esterne;
- Postman.

## Architettura

Il codice è organizzato per modulo funzionale:

```text
src/main/java/it/alessiogori/battledebrief
├── analytics    # calcoli e API statistiche
├── auth         # login, JWT e configurazione Security
├── common       # errori e DTO condivisi
├── integration  # client BArmory e ricerca pubblica tramite Steam ID
├── match        # importazione e consultazione partite
├── player       # profili giocatore
├── unit         # catalogo unità e specializzazioni
└── user         # gestione utenti e ruoli

src/main/resources/static
├── index.html   # applicazione web
└── assets       # stile, client API e grafici Canvas
```

I moduli applicativi sono separati in Controller, Service, Repository, Entity e
DTO. I Service sono definiti tramite interfacce e implementazioni separate.

### Relazioni JPA

Il modello implementa tutte le tipologie richieste dalla consegna:

- `OneToOne`: `User` ↔ `PlayerProfile`;
- `OneToMany`: `GameMatch` → `MatchPerformance`;
- `OneToMany`: `MatchPerformance` → `UnitMatchPerformance`;
- `ManyToOne`: prestazione → partita, profilo e unità;
- `ManyToMany`: `Unit` ↔ `Specialization`.

## Metriche

```text
deploymentCost = unitCost × spawnedCount
lostValue = unitCost × lostCount
economicKd = destroyedValue / lostValue
deploymentEfficiency = destroyedValue / deploymentCost
survivalRate = (spawnedCount - lostCount) / spawnedCount × 100
damageRatio = damageDealt / damageReceived
```

Quando un denominatore è zero, l'API restituisce `value: null` e uno stato
esplicativo come `NO_LOSSES`, `NO_DEPLOYMENTS` o `NO_DAMAGE_RECEIVED`.

Le statistiche esposte da `/api/analytics/**` aggregano unità, mappe e
specializzazioni esclusivamente sui dati memorizzati localmente. Ogni risposta
include la dimensione del campione e non rappresenta una statistica globale
assoluta di *Broken Arrow*.

## Avvio con Docker

### Prerequisiti

- Docker Desktop oppure Docker Engine;
- Docker Compose v2.

Creare il file locale delle variabili d'ambiente:

```powershell
Copy-Item .env.example .env
```

Su Linux o macOS:

```bash
cp .env.example .env
```

I valori forniti sono esclusivamente dimostrativi. Prima di usare il progetto
fuori dall'ambiente locale, modificare password MySQL e chiave JWT nel file
`.env`.

Costruire e avviare l'intero ambiente:

```bash
docker compose up --build
```

Servizi disponibili:

- sito e API: `http://localhost:8080`;
- health check: `http://localhost:8080/actuator/health`;
- MySQL: `localhost:3306`.

Arrestare i container mantenendo i dati:

```bash
docker compose down
```

Per ricreare completamente il database e rieseguire gli script di
inizializzazione:

```bash
docker compose down --volumes
docker compose up --build
```

Il primo comando elimina il volume MySQL locale e tutti i dati contenuti al suo
interno.

### Smoke test Docker

Su Windows è disponibile uno script che costruisce lo stack, attende gli
health check, verifica frontend, login admin e catalogo e infine arresta i
container:

```powershell
.\scripts\docker-smoke-test.ps1 -ResetData
```

L'opzione `-ResetData` elimina preventivamente il volume del progetto ed è
quindi distruttiva per i dati Docker locali. Ometterla per conservare il volume;
usare `-KeepRunning` se si desidera lasciare i container attivi dopo il test.

## Avvio locale con Maven

### Prerequisiti

- JDK 21;
- MySQL 8;
- database `battle_debrief` già creato.

Eseguire nell'ordine:

```bash
mysql -u root -p battle_debrief < database/schema.sql
mysql -u root -p battle_debrief < database/demo-data.sql
```

Impostare le variabili richieste. Esempio PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/battle_debrief'
$env:SPRING_DATASOURCE_USERNAME='battle_debrief'
$env:SPRING_DATASOURCE_PASSWORD='your-password'
$env:JWT_SECRET='your-base64-secret-of-at-least-32-bytes'
.\mvnw.cmd spring-boot:run
```

Su Linux o macOS usare `./mvnw spring-boot:run` dopo aver esportato le stesse
variabili.

Il profilo `dev` è attivo per impostazione predefinita. Hibernate usa
`ddl-auto: validate`: lo schema deve quindi essere creato tramite lo script SQL,
non viene generato automaticamente dall'applicazione.

## Account dimostrativi delle API

| Username | Password | Ruolo |
|---|---|---|
| `admin` | `Admin123!` | `ADMIN` |
| `demo` | `Demo123!` | `USER` |
| `analyst` | `Demo123!` | `USER` |

Le password sono salvate nel database esclusivamente come hash BCrypt. Questi
account servono a verificare da Postman autenticazione, autorizzazione e ruoli.
Catalogo e statistiche aggregate sono pubblici; **My Debrief** richiede invece
un account e associa in modo univoco lo Steam ID al proprietario.

Il catalogo locale comprende il roster e le varianti pubbliche censite da
BArmory e BA Data. La provenienza e lo script di sincronizzazione riproducibile
sono documentati in `docs/CATALOG_SOURCES.md`. Il funzionamento normale resta
offline: le fonti esterne sono necessarie soltanto per rigenerare il dataset.

## API principali

| Modulo | Endpoint principali | Accesso |
|---|---|---|
| Health | `GET /actuator/health` | pubblico |
| Auth | `/api/auth/register`, `/api/auth/login` | pubblico |
| Utente | `/api/auth/me`, `/api/users/{id}`, `PUT /api/users/{id}/steam` | proprietario/admin |
| Admin utenti | `/api/admin/users/**` | admin |
| Catalogo | `/api/units/**`, `/api/specializations` | pubblico |
| Admin catalogo | `/api/admin/units/**` | admin |
| Import partite | `POST /api/matches/import` | proprietario/admin |
| Match history | `/api/players/{id}/matches` | proprietario/admin |
| Player analytics | `/api/players/{id}/analysis/**` | proprietario/admin |
| Unit analytics | `/api/players/{id}/units/**` | proprietario/admin |
| Dataset analytics | `/api/analytics/**` | pubblico |
| Steam debrief | `/api/steam/players/{steamId}` | pubblico |

Le risposte di errore hanno una struttura uniforme con timestamp, stato HTTP,
codice applicativo, messaggio e path della richiesta.

## Postman

Importare in Postman:

- `postman/Battle_Debrief.postman_collection.json`;
- `postman/Local.postman_environment.json`.

Selezionare l'environment **Battle Debrief - Local** ed eseguire le cartelle in
ordine numerico. Gli script Postman salvano automaticamente token JWT e ID
necessari alle richieste successive.

La collection contiene 32 richieste organizzate in dieci cartelle, inclusi
registrazione, login, collegamento Steam, flussi amministrativi e debrief.

## Test e copertura

Su Windows:

```powershell
.\mvnw.cmd clean verify
```

Su Linux o macOS:

```bash
./mvnw clean verify
```

I test usano H2 in memoria e non richiedono MySQL né accesso alla rete. Sono
presenti test unitari, test di persistenza e test d'integrazione REST con
MockMvc.

Il report JaCoCo viene generato in:

```text
target/site/jacoco/index.html
```

Gli 85 test correnti raggiungono il 91,56% di copertura delle linee e superano
il requisito minimo del 35%. Il goal `jacoco:check`, eseguito durante la fase Maven `verify`, fa
fallire automaticamente la build se la copertura complessiva delle linee
scende sotto tale soglia.

## File di consegna

```text
database/schema.sql                         # struttura MySQL
database/demo-data.sql                      # account e profili demo
postman/Battle_Debrief.postman_collection.json
postman/Local.postman_environment.json
Dockerfile
docker-compose.yml
.env.example
docs/RELAZIONE_TECNICA.md                    # relazione di progetto
```

## Note di sicurezza

- `.env` è escluso da Git;
- non vengono commesse chiavi JWT o password di produzione;
- le credenziali incluse sono soltanto dimostrative;
- l'applicazione è stateless e autentica le richieste tramite Bearer token;
- gli endpoint amministrativi richiedono il ruolo `ADMIN`;
- utenti normali non possono leggere dati appartenenti ad altri profili.
