# Relazione tecnica — Battle Debrief

**Project work Java Backend**
**Autore:** Alessio Gori
**Versione:** 1.0
**Data:** luglio 2026

## 1. Introduzione

Battle Debrief è un backend REST per la raccolta e l'analisi delle prestazioni
dei giocatori di *Broken Arrow*. Il sistema gestisce utenti e profili, importa
partite, espone lo storico, amministra un catalogo di unità e calcola statistiche
personali e aggregate sul dataset locale.

Il progetto è un monolite modulare sviluppato con Java 21 e Spring Boot. Questa
scelta mantiene semplice l'avvio, separando comunque le responsabilità dei
diversi domini applicativi.

## 2. Obiettivi e ambito

Gli obiettivi sono:

- realizzare API REST tramite Spring Web MVC;
- suddividere il codice in più moduli funzionali;
- gestire utenti, autenticazione e autorizzazioni;
- persistere i dati con Spring Data JPA e MySQL;
- implementare tutte le tipologie di relazione richieste;
- importare partite evitando duplicati;
- produrre metriche affidabili anche con dati incompleti;
- verificare il comportamento con test unitari e d'integrazione;
- rendere l'ambiente riproducibile tramite Docker;
- fornire SQL, dati demo e Collection Postman.

Frontend, microservizi, Steam OpenID e provider esterni non documentati non
rientrano nei requisiti obbligatori. L'importazione JSON rende l'applicazione
utilizzabile offline.

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

## 4. Architettura

L'applicazione segue una struttura a livelli per modulo.

~~~text
HTTP request
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

Ogni account possiede un profilo giocatore. I campi Steam e commander esterno
sono opzionali e non condizionano il funzionamento locale.

### 5.3 unit

- **UnitCatalogController:** catalogo pubblico, filtri e paginazione;
- **SpecializationController:** elenco specializzazioni;
- **AdminUnitController:** CRUD amministrativo delle unità;
- **AdminSpecializationController:** creazione specializzazioni;
- **UnitCatalogService:** logica applicativa del catalogo;
- **UnitSpecifications:** filtri dinamici;
- **CatalogDatasetInitializer:** caricamento del JSON versionato.

Il catalogo locale evita dipendenze obbligatorie da servizi esterni.

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
- **DatasetAnalyticsService:** aggregazioni di unità e mappe sul dataset
  locale;
- **PlayerCareerAggregate, PlayerUnitAggregate, DatasetUnitAggregate e
  DatasetMapAggregate:** proiezioni delle query JPQL;
- i Controller analytics espongono i rispettivi casi d'uso.

Le aggregazioni vengono eseguite dal database per ridurre memoria e traffico.

### 5.6 common

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

Il flusso è stateless:

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
check, login amministrativo, verifica del catalogo e arresto dei container.

## 12. Testing

La suite comprende test del calcolatore, Service, Repository, relazioni JPA,
JWT, autorizzazioni, filtri, paginazione, Controller REST, schema SQL, dati demo
e health endpoint.

I test usano H2 in modalità MySQL e non dipendono dalla rete.

~~~text
Test: 78
Fallimenti: 0
Errori: 0
Copertura linee JaCoCo: 94,48%
~~~

Il requisito minimo del 35% è ampiamente superato. La fase Maven `verify`
esegue inoltre `jacoco:check` e interrompe la build qualora la copertura
complessiva delle linee scenda sotto il 35%.

## 13. Postman

La cartella postman contiene Collection ed environment locale. Le nove cartelle
numerate includono tutti gli endpoint e l'health check. Gli script propagano
automaticamente token e identificativi.

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
- provider esterno documentato;
- analytics per specializzazioni;
- ranking dei giocatori per unità;
- ricostruzione parziale dei deck;
- frontend grafico;
- migrazioni Flyway;
- test Docker end-to-end in CI.

Le API comunitarie non documentate non sono obbligatorie, per preservare
affidabilità e riproducibilità.

## 16. Rispondenza alla consegna

| Requisito | Implementazione |
|---|---|
| Maven | pom.xml e Maven Wrapper |
| MVC REST | Controller Spring Web per modulo |
| Moduli funzionali | auth, user, player, unit, match, analytics, common |
| Modulo utenti | entity, repository, service, controller e DTO |
| Spring Data JPA | entità e Repository |
| MySQL | driver, profili, schema e container |
| OneToOne | User–PlayerProfile |
| OneToMany | match–prestazioni e prestazione–unità |
| ManyToOne | prestazioni verso match, profilo e unità |
| ManyToMany | Unit–Specialization |
| Spring Security | JWT, BCrypt, ruoli e ownership |
| Coverage minima | 76 test e 93,89% line coverage |
| Docker | Dockerfile multi-stage e Compose |
| Best practice | DTO, validazione, interfacce, transazioni, error handling |
| Script SQL | schema e dati demo |
| Postman | Collection completa ed environment |
| Relazione | questo documento |

## 17. Conclusioni

Battle Debrief soddisfa i requisiti obbligatori con un backend REST modulare,
autenticato e persistente. Il dominio dimostra CRUD, relazioni JPA, query
dinamiche, aggregazioni, sicurezza, validazione e gestione uniforme degli
errori.

Maven Wrapper, SQL, dati demo, Postman e Docker rendono il progetto
riproducibile. La suite di test offre un ampio margine rispetto alla copertura
minima richiesta.
