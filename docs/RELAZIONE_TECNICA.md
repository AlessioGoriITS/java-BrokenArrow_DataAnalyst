# Relazione tecnica — Battle Debrief

**Project work Java Backend**
**Autore:** Alessio Gori
**Versione:** 1.0
**Data:** luglio 2026

## 1. Introduzione

*Broken Arrow* è un gioco di strategia tattica in tempo reale ambientato in un
conflitto moderno. Il giocatore costruisce un esercito scegliendo una brigata e
schiera fanteria, mezzi corazzati, artiglieria, elicotteri e aerei. Durante una
partita contano sia il controllo degli obiettivi sia l'impiego efficiente dei
punti spesi per le unità.

Battle Debrief è un'applicazione web con backend REST che rende consultabili il
catalogo delle unità e le prestazioni dei giocatori di *Broken Arrow*. Il
visitatore può consultare liberamente catalogo e statistiche aggregate; creando
un account collega il proprio Steam ID e ottiene un dossier persistente con
profilo, rating, statistiche di carriera, partite recenti e unità impiegate. Il
sistema offre inoltre importazione di partite e API amministrative per
verificare sicurezza, ruoli e CRUD.

Il progetto è un monolite modulare sviluppato con Java 21 e Spring Boot. Questa
scelta mantiene semplice l'avvio, separando comunque le responsabilità dei
diversi domini applicativi.

## 2. Obiettivi e ambito

Gli obiettivi sono:

- realizzare API REST tramite Spring Web MVC;
- suddividere il codice in più moduli funzionali;
- associare in modo univoco lo Steam ID a un account applicativo;
- gestire utenti, autenticazione JWT, proprietà e autorizzazioni per ruolo;
- persistere i dati con Spring Data JPA e MySQL;
- implementare tutte le tipologie di relazione richieste;
- importare partite evitando duplicati;
- produrre metriche affidabili anche con dati incompleti;
- verificare il comportamento con test unitari e d'integrazione;
- rendere l'ambiente riproducibile tramite Docker;
- fornire SQL, dati demo e Collection Postman.

I microservizi non rientrano nell'implementazione. Catalogo e analytics locali
restano utilizzabili offline; il solo debrief tramite Steam ID richiede la
  disponibilità di almeno uno dei provider comunitari BArmory e BattleGroup.

## 3. Tecnologie

| Componente | Tecnologia | Utilizzo |
|---|---|---|
| Linguaggio | Java 21 | implementazione |
| Build | Maven Wrapper | dipendenze, test e packaging |
| Framework | Spring Boot 3.5 | configurazione e avvio |
| API | Spring Web MVC | Controller REST e JSON |
| Persistenza | Spring Data JPA, Hibernate | mapping e query |
| Database | MySQL 8.4 | persistenza applicativa |
| Sicurezza | Spring Security | accessi, ruoli e ownership |
| Token | JJWT | emissione e validazione JWT |
| Validazione | Jakarta Validation | controllo degli input |
| Test | JUnit 5, MockMvc, AssertJ, H2 | unità e integrazione |
| Coverage | JaCoCo | copertura del codice |
| Container | Docker e Docker Compose | app e database |
| Client API | Postman | esecuzione dei flussi REST |
| Frontend | HTML, CSS, JavaScript, Canvas | dashboard web responsive |

## 4. Architettura

L'applicazione segue una struttura a livelli per modulo.

~~~text
Browser ---> frontend statico
                |
                v
HTTP request ---> API REST
    |
    v
Controller ---> DTO e validazione
    |
    v
Service interface ---> Service implementation
    |
    v
Repository ---> JPA/Hibernate ---> MySQL
~~~

I **Controller** gestiscono il contratto HTTP e delegano la logica. I **Service**
definiscono i casi d'uso e i confini transazionali. I **Repository** estendono
JpaRepository e usano Specification o query JPQL. I **DTO** separano API ed
entità JPA, ospitando anche i vincoli di validazione.

Le consultazioni sono read-only. Le operazioni che modificano più entità, come
l'importazione di un batch di partite, sono transazionali.

## 5. Moduli e responsabilità

### 5.1 auth

- **AuthController:** registrazione, login e utente corrente;
- **AuthService:** creazione account, verifica password ed emissione token;
- **JwtService:** generazione e validazione JWT;
- **JwtAuthenticationFilter:** autenticazione delle richieste;
- **DatabaseUserDetailsService:** caricamento account dal database;
- **SecurityConfig:** sessione stateless e regole di accesso.

### 5.2 user e player

- **UserController:** lettura e modifica del proprio account;
- **AdminUserController:** elenco, ruoli e abilitazione utenti;
- **UserService:** regole applicative degli account;
- **UserRepository e PlayerProfileRepository:** persistenza e ricerca.

Ogni account può possedere un profilo giocatore. Il caso d'uso
`PUT /api/users/{id}/steam` verifica il profilo presso il provider, impedisce
che lo stesso Steam ID sia assegnato a due utenti e crea o aggiorna la relazione
OneToOne con l'account autenticato.

### 5.3 unit

- **UnitCatalogController:** catalogo pubblico, filtri e paginazione;
- **SpecializationController:** elenco specializzazioni;
- **AdminUnitController:** CRUD amministrativo delle unità;
- **AdminSpecializationController:** creazione specializzazioni;
- **UnitCatalogService:** logica applicativa del catalogo;
- **UnitSpecifications:** filtri dinamici;
- **CatalogDatasetInitializer:** caricamento del JSON versionato.

Il catalogo locale versionato comprende 420 unità e 11 specializzazioni. Viene
generato da fonti pubbliche BArmory e BA Data tramite uno script riproducibile,
ma l'applicazione utilizza la copia inclusa nel progetto e non dipende da tali
servizi durante il normale funzionamento.

### 5.4 match

- **MatchImportController e MatchImportService:** importazione JSON;
- **PlayerMatchController:** storico paginato e filtrabile;
- **MatchController:** dettaglio della partita;
- **MatchQueryService:** query e mapping;
- **GameMatchSpecifications:** filtri dinamici;
- **ResourceAuthorizationService:** controllo della proprietà.

ExternalMatchId è univoco. Una nuova importazione non duplica una partita già
presente.

### 5.5 analytics

- **AnalyticsCalculator:** rapporti, percentuali e arrotondamenti;
- **PlayerAnalyticsService:** statistiche di carriera e trend temporale;
- **UnitAnalyticsService:** rendimento delle unità per giocatore;
- **DatasetAnalyticsService:** aggregazioni di unità, mappe e specializzazioni
  sul dataset locale;
- **PlayerCareerAggregate, PlayerUnitAggregate, DatasetUnitAggregate e
  DatasetMapAggregate, DatasetSpecializationAggregate:** proiezioni delle query
  JPQL;
- i Controller analytics espongono i rispettivi casi d'uso.

Le aggregazioni vengono eseguite dal database per ridurre memoria e traffico.
Nelle statistiche per specializzazione, una prestazione di unità contribuisce
a ogni specializzazione associata. Partite e vittorie sono conteggiate in modo
distinto all'interno di ciascun gruppo, evitando duplicazioni quando vengono
schierate più unità della stessa specializzazione.

### 5.6 integration/barmory

- **BarmoryGateway:** contratto astratto verso il provider;
- **BarmoryRestClient:** client HTTP, attestazione e rinnovo del token tecnico;
- **SteamPlayerService:** recupero e aggregazione di carriera, match e unità;
- **SteamPlayerController:** endpoint pubblico validato per Steam ID;
- **BattleGroupRestClient:** fallback automatico quando BArmory rifiuta o non
  completa la richiesta;
- i DTO del modulo isolano il formato esterno dal contratto REST locale.

L'integrazione applica un anti-corruption layer: eventuali variazioni dei JSON
del provider restano confinate nel modulo e non coinvolgono entità JPA o
frontend.

### 5.7 frontend

Il frontend single-page è servito dalle risorse statiche di Spring Boot e non
richiede un container o un package manager separato. Comprende:

- Command dashboard con indicatori sintetici e ranking;
- Hangar filtrabile con paginazione e schede di dettaglio;
- tabelle analytics per unità, mappe e specializzazioni;
- registrazione, login e collegamento guidato dello Steam ID;
- area personale con ELO, storico e unità del profilo collegato;
- grafici Canvas, layout responsive e stati vuoti espliciti.

Il client non memorizza password. Conserva il JWT in `sessionStorage`, quindi la
sessione termina chiudendo la scheda; lo Steam ID non è la credenziale e viene
persistito dal backend nel profilo appartenente all'utente.

### 5.8 common

- **PageResponse:** formato stabile di paginazione;
- **ApiException:** errori applicativi tipizzati;
- **GlobalExceptionHandler:** risposte JSON uniformi.

## 6. Modello dati

Le entità sono User, PlayerProfile, Unit, Specialization, GameMatch,
MatchPerformance e UnitMatchPerformance.

~~~mermaid
erDiagram
    USER ||--|| PLAYER_PROFILE : owns
    GAME_MATCH ||--o{ MATCH_PERFORMANCE : contains
    PLAYER_PROFILE ||--o{ MATCH_PERFORMANCE : records
    MATCH_PERFORMANCE ||--o{ UNIT_MATCH_PERFORMANCE : contains
    UNIT ||--o{ UNIT_MATCH_PERFORMANCE : identifies
    UNIT }o--o{ SPECIALIZATION : belongs_to
~~~

| Relazione richiesta | Implementazione |
|---|---|
| OneToOne | User ↔ PlayerProfile |
| OneToMany | GameMatch → MatchPerformance |
| OneToMany | MatchPerformance → UnitMatchPerformance |
| ManyToOne | prestazioni verso match, profilo e unità |
| ManyToMany | Unit ↔ Specialization |

La tabella unit_specializations realizza la ManyToMany. I vincoli univoci
proteggono account, profili, ID esterni e coppie prestazione-unità.

## 7. Metriche

Il costo usato è quello osservato nella partita, non quello corrente nel
catalogo. Le variazioni del catalogo non modificano retroattivamente lo storico.

~~~text
deploymentCost = unitCost × spawnedCount
lostValue = unitCost × lostCount
economicKd = destroyedValue / lostValue
deploymentEfficiency = destroyedValue / deploymentCost
survivalRate = (spawnedCount - lostCount) / spawnedCount × 100
damageRatio = damageDealt / damageReceived
~~~

Una divisione per zero restituisce un valore nullo e uno stato esplicito:
AVAILABLE, NO_MATCHES, NO_LOSSES, NO_DEPLOYMENTS o NO_DAMAGE_RECEIVED.

Le statistiche aggregate riportano sampleMatches, samplePlayers e totale delle
partite. Sono statistiche del campione locale, non globali assolute del gioco.

## 8. Sicurezza

Catalogo e analytics aggregate sono pubblici. L'area **My Debrief** usa invece
il modulo utenti reale: dopo login recupera l'account autenticato, legge lo
Steam ID collegato e carica i dati esterni del solo profilo scelto. Il flusso è
stateless:

1. login con username e password;
2. verifica BCrypt;
3. emissione di un JWT firmato e con scadenza;
4. invio del Bearer token;
5. validazione nel filtro JWT;
6. controllo di ruolo e proprietà con Spring Security e PreAuthorize.

| Risorsa | Accesso |
|---|---|
| registrazione e login | pubblico |
| catalogo e dataset analytics | pubblico |
| lettura provider tramite Steam ID | pubblico |
| collegamento Steam ID all'account | proprietario o admin |
| health check | pubblico, senza dettagli |
| account e statistiche personali | proprietario o admin |
| importazione e match history | proprietario o admin |
| gestione utenti e catalogo | solo admin |

Le password sono hash BCrypt e non vengono esposte. Chiave JWT e password MySQL
arrivano da variabili d'ambiente. Il file .env è escluso da Git.

## 9. Validazione ed errori

I Request DTO usano Jakarta Validation con vincoli come NotBlank, Size, Email,
Positive e PositiveOrZero. GlobalExceptionHandler traduce risorse mancanti,
conflitti, credenziali errate, accessi vietati, input non validi, errori JSON e
violazioni di vincoli in un formato uniforme.

La risposta di errore contiene timestamp, status HTTP, codice applicativo,
messaggio, path ed eventuali errori sui campi.

## 10. Persistenza e SQL

Hibernate usa ddl-auto validate e non modifica lo schema:

- **database/schema.sql:** tabelle, indici, vincoli e chiavi esterne;
- **database/demo-data.sql:** tre account e tre profili;
- **units.json:** catalogo locale versionato.

Gli inserimenti demo sono idempotenti. DatabaseSchemaValidationTests esegue lo
schema, valida Hibernate, ricarica i dati e controlla ruolo, relazione OneToOne
e password BCrypt.

## 11. Docker

Il Dockerfile è multi-stage:

1. Maven e JDK 21 eseguono clean verify e producono il JAR;
2. il JRE 21 avvia il JAR con un utente non root.

Docker Compose configura MySQL 8.4, volume persistente, inizializzazione SQL,
profilo docker, health check e variabili da .env. L'applicazione attende lo
stato healthy di MySQL.

La sintassi Compose è stata validata. L'esecuzione completa richiede Docker
Engine attivo e si avvia con docker compose up --build.

Lo script scripts/docker-smoke-test.ps1 automatizza build, attesa degli health
check, verifica del frontend, login amministrativo, verifica del catalogo e
arresto dei container.

## 12. Testing

La suite comprende test del calcolatore, Service, Repository, relazioni JPA,
JWT, autorizzazioni, filtri, paginazione, Controller REST, schema SQL, dati demo,
frontend pubblico e health endpoint.

I test usano H2 in modalità MySQL e non dipendono dalla rete.

~~~text
Test: 84
Fallimenti: 0
Errori: 0
Copertura linee JaCoCo: 91,49%
~~~

Il requisito minimo del 35% è ampiamente superato. La fase Maven `verify`
esegue inoltre `jacoco:check` e interrompe la build qualora la copertura
complessiva delle linee scenda sotto il 35%.

## 13. Postman

La cartella postman contiene Collection ed environment locale. Le dieci cartelle
numerate comprendono 32 richieste: health check, registrazione, login,
collegamento Steam, CRUD, ruoli, import, analytics e debrief. Gli script
propagano automaticamente token e identificativi. La lettura del provider è
pubblica, mentre il salvataggio dello Steam ID richiede il JWT del proprietario.

## 14. Scelte progettuali

- **Monolite modulare:** semplicità operativa e separazione per dominio.
- **Programmazione per interfacce:** dipendenza dai contratti dei Service.
- **DTO al confine:** nessuna esposizione diretta del grafo JPA.
- **Query aggregate:** somme e conteggi delegati al database.
- **Dataset versionato:** catalogo disponibile offline e tracciabile.
- **Stati espliciti:** i rapporti non calcolabili non diventano falsi zeri.
- **Open Session in View disabilitato:** accesso ai dati nei confini del Service.
- **Schema validato:** differenze tra SQL ed entità causano il fallimento dei
  test.

## 15. Limiti ed evoluzioni

Possibili sviluppi futuri:

- Steam OpenID;
- cache persistente e fallback per indisponibilità temporanee del provider;
- ranking dei giocatori per unità;
- ricostruzione parziale dei deck;
- migrazioni Flyway;
- test Docker end-to-end in CI.

Il debrief Steam dipende da un servizio comunitario non ufficiale e richiede
connessione Internet. In caso di indisponibilità il backend restituisce un
errore controllato `502 EXTERNAL_PROVIDER_ERROR`; il resto dell'applicazione
continua a funzionare sul dataset locale.

## 16. Rispondenza alla consegna

| Requisito | Implementazione |
|---|---|
| Maven | pom.xml e Maven Wrapper |
| MVC REST | Controller Spring Web per modulo |
| Moduli funzionali | auth, user, player, unit, match, analytics, integration, common |
| Modulo utenti | entity, repository, service, controller e DTO |
| Spring Data JPA | entità e Repository |
| MySQL | driver, profili, schema e container |
| OneToOne | User–PlayerProfile |
| OneToMany | match–prestazioni e prestazione–unità |
| ManyToOne | prestazioni verso match, profilo e unità |
| ManyToMany | Unit–Specialization |
| Spring Security | JWT, BCrypt, ruoli e ownership |
| Coverage minima | 85 test e 91,49% line coverage |
| Docker | Dockerfile multi-stage e Compose |
| Best practice | DTO, validazione, interfacce, transazioni, error handling |
| Script SQL | schema e dati demo |
| Postman | Collection completa ed environment |
| Relazione | questo documento |

## 17. Conclusioni

Battle Debrief soddisfa i requisiti obbligatori con un backend REST modulare,
persistente e protetto dove necessario. Il dominio dimostra CRUD, relazioni
JPA, query dinamiche, integrazione HTTP, aggregazioni, sicurezza, validazione e
gestione uniforme degli errori.

Maven Wrapper, SQL, dati demo, Postman e Docker rendono il progetto
riproducibile. La suite di test offre un ampio margine rispetto alla copertura
minima richiesta.

## 18. Verifica pratica suggerita al docente

### 18.1 Avvio e affidabilità

Da terminale, nella radice del progetto:

~~~bash
docker compose up --build
~~~

Attendere che entrambi i servizi risultino `healthy`, quindi aprire
`http://localhost:8080` e verificare:

1. dashboard e Hangar caricati senza autenticazione;
2. ricerca di `Abrams` nell'Hangar;
3. filtri per fazione, categoria e brigata;
4. apertura di una scheda unità con costo, HP, velocità, corazza, arma,
   immagine e specializzazioni;
5. sezione `My Debrief`, registrazione di un account e login automatico;
6. collegamento dello Steam ID di esempio `76561198157609957`;
7. caricamento del dossier e permanenza dell'associazione dopo un nuovo login;
8. pulsante `Cambia Steam ID` e logout.

Lo Steam ID di esempio appartiene a un record pubblico della leaderboard del
provider. Se il servizio esterno non è disponibile, questa sola verifica può
restituire 502 senza compromettere catalogo, database o analytics locali.

### 18.2 Ricerche REST rapide

Le seguenti richieste possono essere eseguite dal browser o da Postman:

~~~text
GET http://localhost:8080/actuator/health
GET http://localhost:8080/api/units?name=Abrams&page=0&size=20
GET http://localhost:8080/api/specializations
GET http://localhost:8080/api/units?specializationId=9&page=0&size=20
GET http://localhost:8080/api/analytics/units
GET http://localhost:8080/api/analytics/maps
GET http://localhost:8080/api/steam/players/76561198157609957?weeks=8&limit=10
~~~

L'ID della specializzazione va preso dalla risposta di
`/api/specializations`; il valore `9` è soltanto un esempio e può cambiare dopo
la ricreazione del database.

### 18.3 Sicurezza e ruoli

Per dimostrare Spring Security:

1. chiamare `GET /api/admin/users` senza token e verificare `401`;
2. eseguire `POST /api/auth/register` e poi `POST /api/auth/login`;
3. collegare Steam con `PUT /api/users/{id}/steam` e il Bearer token ricevuto;
4. verificare che un altro utente riceva `403` sul medesimo `{id}`;
5. eseguire il login con l'account demo amministrativo e verificare 200 su
   `GET /api/admin/users`;
6. autenticarsi come utente normale e verificare `403` sulla stessa risorsa;
7. eseguire le cartelle `01`, `02`, `03` e `05` della Collection Postman.

### 18.4 Persistenza e relazioni

Nel database MySQL si possono eseguire query semplici come:

~~~sql
SELECT COUNT(*) FROM game_units;

SELECT u.name AS unit_name, s.name AS brigade
FROM game_units u
JOIN unit_specializations us ON us.unit_id = u.id
JOIN specializations s ON s.id = us.specialization_id
WHERE s.name LIKE '%Stryker%';

SELECT gm.map_name, mp.won, mp.old_rating, mp.new_rating
FROM game_matches gm
JOIN match_performances mp ON mp.game_match_id = gm.id
ORDER BY gm.started_at DESC;
~~~

La seconda query rende visibile la relazione ManyToMany; la terza mostra le
ManyToOne/OneToMany. La OneToOne è verificabile collegando `app_users` e
`player_profiles` tramite `user_id`.

### 18.5 Test e copertura

~~~bash
./mvnw clean verify
~~~

Su Windows usare `mvnw.cmd`. La build deve terminare con tutti gli 85 test verdi
e produce `target/site/jacoco/index.html`. Il controllo Maven fallisce
automaticamente sotto il 35%; la misurazione corrente delle linee è 91,49%.

### 18.6 Collection Postman

Importare la Collection e l'environment presenti in `postman`, selezionare
`Battle Debrief - Local` ed eseguire le cartelle in ordine. Le prime nove
verificano il backend locale; `09 - Steam Debrief` verifica l'integrazione live
e contiene anche il caso negativo di Steam ID non valido.

## 19. Fonti dati esterne

- BArmory: `https://www.barmory.net`;
- BattleGroup: `https://battlegroup.website`;
- BA Data, Hangar: `https://ba.puliaev.com/hangar`.

Sono progetti comunitari non affiliati agli sviluppatori di *Broken Arrow*.
Nel repository viene conservata una copia versionata del catalogo per rendere
ripetibili avvio e test anche senza accesso a tali siti.
