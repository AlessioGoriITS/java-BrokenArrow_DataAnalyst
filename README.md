# Battle Debrief

Battle Debrief è il project work che ho realizzato per il corso Java Backend.
È un'applicazione Spring Boot che raccoglie dati pubblici di *Broken Arrow* e
permette di consultare partite, unità e statistiche tramite API REST.

Il progetto va avviato principalmente con Docker. In questo modo vengono
preparati insieme sia il backend Java sia il database MySQL, senza dover
installare e configurare MySQL manualmente.

## Spiegazione tecnica

*Broken Arrow* è un gioco di strategia militare in tempo reale. Il giocatore
sceglie una fazione e costruisce un battlegroup usando diverse
specializzazioni, per esempio unità corazzate, aviotrasportate o di supporto.
Durante una partita schiera mezzi e reparti come carri armati, fanteria,
artiglieria, elicotteri e aerei.

Ogni unità ha un costo e caratteristiche differenti. Per capire l'andamento di
una partita non basta quindi contare quante unità sono state perse o distrutte:
è utile confrontarne anche il valore, i danni, il punteggio ottenuto e la
variazione del rating del giocatore.

Battle Debrief serve a organizzare questi dati in un unico backend. Il catalogo
locale permette di cercare le unità per fazione, categoria, costo e brigata. Le
partite importate vengono salvate su MySQL e usate per calcolare statistiche
del singolo giocatore e statistiche aggregate del dataset.

Il collegamento con Steam funziona tramite uno Steam ID pubblico. Il backend
interroga BArmory e, quando necessario, usa BattleGroup come provider di
fallback. I provider non pubblicano sempre la telemetria completa delle
singole unità: in questo caso l'applicazione segnala il dato mancante invece di
far credere che il giocatore non abbia utilizzato unità.

Per una prova si può usare il mio Steam ID:

```text
76561198776876377
```

La disponibilità di questa ricerca dipende dai provider esterni e dalla
connessione Internet. Il resto del progetto, compresi catalogo, utenti, import
JSON e analytics locali, continua a funzionare anche senza di essi.

## Funzionalità principali

- registrazione e login con token JWT;
- ruoli `USER` e `ADMIN`;
- collegamento univoco tra account e profilo Steam;
- gestione degli utenti da parte dell'amministratore;
- catalogo di 420 unità e 11 specializzazioni;
- ricerca con filtri, ordinamento e paginazione;
- CRUD amministrativo di unità e specializzazioni;
- importazione JSON idempotente delle partite;
- storico e dettaglio delle partite;
- statistiche di carriera, andamento ELO e utilizzo delle unità;
- statistiche aggregate per unità, mappe e specializzazioni;
- gestione uniforme degli errori e validazione degli input;
- metriche sulle chiamate ai provider esterni;
- health check dello stack Docker.

## Sito web

La consegna richiede principalmente un backend REST. Ho aggiunto anche un sito
web per uso personale, utile per consultare i dati senza Postman. Il sito usa
le stesse API del backend e non contiene una logica separata.

Dopo l'avvio è disponibile all'indirizzo:

```text
http://localhost:8080
```

Per la valutazione del backend non è necessario usare il sito: tutte le
funzioni richieste possono essere mostrate con la Collection Postman inclusa
nel progetto.

## Tecnologie utilizzate

- Java 21;
- Spring Boot 3.5;
- Maven e Maven Wrapper;
- Spring Web MVC;
- Spring Data JPA e Hibernate;
- Spring Security e JWT;
- MySQL 8.4;
- H2 per i test automatici;
- JUnit 5, MockMvc, AssertJ e JaCoCo;
- Docker e Docker Compose;
- HTML, CSS e JavaScript per il sito;
- Postman per provare le API.

## Architettura

Ho organizzato il codice per moduli funzionali:

```text
src/main/java/it/alessiogori/battledebrief
├── analytics    # calcolo e API delle statistiche
├── auth         # registrazione, login, JWT e Spring Security
├── common       # errori e risposte condivise
├── integration  # comunicazione con BArmory e BattleGroup
├── match        # importazione e consultazione delle partite
├── player       # profili dei giocatori
├── unit         # catalogo di unità e specializzazioni
└── user         # utenti, ruoli e collegamento del profilo Steam
```

Nei moduli principali sono separati:

- `Controller`, che ricevono le richieste HTTP;
- `Service`, che contengono la logica applicativa;
- `Repository`, che comunicano con il database;
- `Entity`, che rappresentano le tabelle JPA;
- `DTO`, usati come input e output delle API;
- `Mapper`, che convertono entità e DTO.

I Service sono definiti tramite interfacce e implementazioni separate. In
questo modo Controller, logica e persistenza non dipendono direttamente uno
dall'altro.

### Relazioni JPA

Il modello contiene tutte le tipologie di relazione richieste:

- `OneToOne`: `User` ↔ `PlayerProfile`;
- `OneToMany`: `GameMatch` → `MatchPerformance`;
- `OneToMany`: `MatchPerformance` → `UnitMatchPerformance`;
- `ManyToOne`: le prestazioni fanno riferimento a partita, profilo e unità;
- `ManyToMany`: `Unit` ↔ `Specialization`.

## Come vengono calcolate le statistiche

Le statistiche non sono valori casuali generati dal sito. Sono calcolate dal
backend usando le prestazioni salvate nel database.

```text
deploymentCost = unitCost × spawnedCount
lostValue = unitCost × lostCount
economicKd = destroyedValue / lostValue
deploymentEfficiency = destroyedValue / deploymentCost
survivalRate = (spawnedCount - lostCount) / spawnedCount × 100
damageRatio = damageDealt / damageReceived
```

Quando non è possibile eseguire una divisione, per esempio perché non ci sono
perdite, l'API restituisce `value: null` insieme a uno stato esplicativo come
`NO_LOSSES`, `NO_DEPLOYMENTS` o `NO_DAMAGE_RECEIVED`.

Le API `/api/analytics/**` lavorano sui dati presenti nel database locale. Non
devono quindi essere interpretate come statistiche globali di tutti i
giocatori di *Broken Arrow*.

## Avvio con Docker

### Requisiti

- Docker Desktop, oppure Docker Engine;
- Docker Compose v2;
- connessione Internet per la prima build e per le ricerche Steam.

Aprire un terminale nella cartella in cui si trova `docker-compose.yml`.

Creare il file `.env` copiando l'esempio.

PowerShell:

```powershell
Copy-Item .env.example .env
```

Prompt dei comandi di Windows:

```bat
copy .env.example .env
```

Linux o macOS:

```bash
cp .env.example .env
```

Avviare applicazione e database:

```bash
docker compose up --build --detach
```

Controllare lo stato:

```bash
docker compose ps
```

Attendere che `app` e `mysql` risultino `healthy`. A questo punto sono
disponibili:

- sito e API: `http://localhost:8080`;
- health check: `http://localhost:8080/actuator/health`.

MySQL non espone una porta verso il computer host: viene usato solamente
dall'applicazione nella rete interna di Docker.

Per fermare tutto conservando i dati:

```bash
docker compose down
```

Per eliminare anche il database e ripartire da zero:

```bash
docker compose down --volumes --remove-orphans
docker compose up --build --detach
```

Il primo comando elimina definitivamente il volume MySQL del progetto.

### Script di controllo Docker

Su Windows è presente anche uno script che costruisce lo stack, aspetta gli
health check e prova frontend, login amministratore e catalogo:

```powershell
.\scripts\docker-smoke-test.ps1 -ResetData
```

`-ResetData` elimina il database Docker esistente. Si può omettere per
conservare i dati oppure aggiungere `-KeepRunning` per lasciare i container
accesi alla fine.

## Account dimostrativi

Il database iniziale contiene questi account:

| Username | Password | Ruolo |
|---|---|---|
| `admin` | `Admin123!` | `ADMIN` |
| `demo` | `Demo123!` | `USER` |
| `analyst` | `Demo123!` | `USER` |

Le password non sono salvate in chiaro nel database, ma come hash BCrypt. Gli
account servono solo per la dimostrazione locale di autenticazione e ruoli.

## API principali

| Modulo | Endpoint | Accesso |
|---|---|---|
| Health | `GET /actuator/health` | pubblico |
| Metriche | `GET /actuator/metrics/**` | autenticato |
| Autenticazione | `/api/auth/register`, `/api/auth/login` | pubblico |
| Utente | `/api/auth/me`, `/api/users/{id}` | proprietario o admin |
| Collegamento Steam | `PUT /api/users/{id}/steam` | proprietario o admin |
| Gestione utenti | `/api/admin/users/**` | admin |
| Catalogo | `/api/units/**`, `/api/specializations` | pubblico |
| Gestione catalogo | `/api/admin/units/**` | admin |
| Import partite | `POST /api/matches/import` | proprietario o admin |
| Storico partite | `/api/players/{id}/matches` | proprietario o admin |
| Analytics giocatore | `/api/players/{id}/analysis/**` | proprietario o admin |
| Analytics unità | `/api/players/{id}/units/**` | proprietario o admin |
| Analytics dataset | `/api/analytics/**` | pubblico |
| Ricerca Steam | `/api/steam/players/{steamId}` | pubblico |

Le risposte di errore hanno sempre una struttura comune con timestamp, stato
HTTP, codice applicativo, messaggio e percorso della richiesta.

L'import restituisce `201 Created` quando salva almeno una partita nuova e
`200 OK` quando le partite erano già presenti. La risposta della ricerca Steam
contiene anche `diagnostics`, con provider utilizzato, durata della richiesta,
match caricati, match scartati e campi non validi.

## Prova con Postman

Importare in Postman entrambi i file:

- `postman/Battle_Debrief.postman_collection.json`;
- `postman/Local.postman_environment.json`.

Selezionare l'environment **Battle Debrief - Local**. Senza questo passaggio
Postman non sostituisce la variabile `{{baseUrl}}`.

La Collection contiene 32 richieste in 10 cartelle numerate. Gli script salvano
automaticamente token JWT e ID prodotti dalle richieste precedenti. Il mio
Steam ID `76561198776876377` è già inserito nell'environment e può essere
cambiato prima della prova.

La dimostrazione completa da seguire durante la consegna è descritta in
[`docs/GUIDA_POSTMAN_CONSEGNA.md`](docs/GUIDA_POSTMAN_CONSEGNA.md).

## Test

Maven non serve per avviare normalmente il progetto: per quello viene usato
Docker. Il Maven Wrapper serve soprattutto per compilare il codice ed eseguire
i test automatici.

Su Windows:

```powershell
.\mvnw.cmd clean verify
```

Su Linux o macOS:

```bash
./mvnw clean verify
```

I test usano un database H2 in memoria, quindi non richiedono MySQL o accesso a
Internet. Sono presenti test unitari, test di persistenza e test REST con
MockMvc.

Attualmente sono presenti 89 test e la copertura delle linee è del 90,31%. La
build fallisce automaticamente se la copertura scende sotto il 35% richiesto.
Il report viene generato in:

```text
target/site/jacoco/index.html
```

## File preparati per la consegna

```text
pom.xml                                      # progetto Maven
database/schema.sql                          # struttura MySQL
database/demo-data.sql                       # dati dimostrativi
postman/Battle_Debrief.postman_collection.json
postman/Local.postman_environment.json
Dockerfile
docker-compose.yml
.env.example
docs/RELAZIONE_TECNICA.md                    # relazione tecnica
docs/GUIDA_POSTMAN_CONSEGNA.md               # sequenza della dimostrazione
```

## Note di sicurezza

- `.env` non viene caricato su Git;
- le credenziali incluse servono soltanto per la dimostrazione locale;
- le password degli utenti sono salvate come hash BCrypt;
- l'applicazione usa sessioni stateless e token Bearer JWT;
- gli endpoint amministrativi richiedono il ruolo `ADMIN`;
- un utente normale non può leggere i dati di un altro profilo.
