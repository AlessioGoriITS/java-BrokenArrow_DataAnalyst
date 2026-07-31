# Guida Postman per la presentazione del Project Work

## 1. Scopo della guida

Questa procedura mostra il funzionamento del backend esclusivamente attraverso
le API REST. Il frontend non è necessario e non viene utilizzato.

La dimostrazione permette di verificare:

- avvio dell'applicazione e collegamento a MySQL;
- registrazione, login e autenticazione JWT;
- autorizzazione per proprietario e ruolo amministrativo;
- modulo utenti e collegamento OneToOne con il profilo Steam;
- catalogo, filtri e gestione delle specializzazioni;
- operazioni CRUD amministrative;
- importazione idempotente delle partite;
- storico e dettaglio delle partite;
- analytics di giocatori, unità, mappe e brigate;
- integrazione con i provider pubblici tramite Steam ID;
- gestione degli errori e validazione degli input;
- metriche di funzionamento dei provider esterni.

## 2. File da importare

In Postman importare entrambi i file:

1. `postman/Battle_Debrief.postman_collection.json`;
2. `postman/Local.postman_environment.json`.

Dopo l'importazione selezionare in alto a destra l'environment:

```text
Battle Debrief - Local
```

Controllare che la variabile `baseUrl` abbia il valore:

```text
http://localhost:8080
```

Non è necessario sostituire manualmente `{{baseUrl}}` nelle richieste. Se
Postman lo evidenzia in rosso, l'environment non è stato selezionato.

## 3. Preparazione pulita dell'ambiente

Aprire il terminale nella cartella che contiene `docker-compose.yml`.

### PowerShell

```powershell
Copy-Item .env.example .env
docker compose down --volumes --remove-orphans
docker compose up --build --detach
docker compose ps
```

### Prompt dei comandi di Windows

```bat
copy .env.example .env
docker compose down --volumes --remove-orphans
docker compose up --build --detach
docker compose ps
```

Il comando `down --volumes` elimina i dati Docker precedenti. Per la
presentazione è utile perché rende ripetibile la Collection ed evita che lo
Steam ID sia già collegato a un vecchio utente.

Attendere che `docker compose ps` mostri entrambi i servizi come `healthy`:

```text
app       healthy
mysql     healthy
```

MySQL non pubblica una porta sull'host. Viene raggiunto dall'applicazione
attraverso la rete interna di Docker Compose.

## 4. Come funziona la Collection

Le cartelle devono essere eseguite nell'ordine numerico da `00` a `09`.

Gli script Postman salvano automaticamente nell'environment:

- username ed email generati;
- token JWT dell'utente;
- token JWT dell'amministratore;
- ID dell'utente e del profilo giocatore;
- ID di unità, specializzazione e partita;
- identificativi esterni univoci per i dati di prova.

Prima della dimostrazione non bisogna compilare manualmente queste variabili.
La sola variabile che si può sostituire è `steamId`, se si vuole provare un
profilo Broken Arrow differente.

## 5. Sequenza completa da mostrare al docente

### 5.1 Cartella `00 - Health`

Eseguire `Application health`.

Risultato atteso:

```http
HTTP/1.1 200 OK
```

```json
{
  "status": "UP"
}
```

Questa richiesta dimostra che Spring Boot è avviato e che il controllo di
salute, comprendente il datasource MySQL, è positivo.

### 5.2 Cartella `01 - Authentication`

Eseguire nell'ordine:

1. `Register local user`;
2. `Login local user`;
3. `Login demo admin`;
4. `Current user`.

Risultati da evidenziare:

| Richiesta | Status | Cosa dimostra |
|---|---:|---|
| Register local user | 201 | validazione, creazione utente e header `Location` |
| Login local user | 200 | autenticazione e generazione JWT |
| Login demo admin | 200 | autenticazione dell'account con ruolo `ADMIN` |
| Current user | 200 | lettura dell'identità dal bearer token |

Il token dell'utente viene salvato in `accessToken`; quello amministrativo in
`adminToken`. Le password non devono comparire in nessuna risposta.

Credenziali demo amministrative, già configurate nell'environment:

```text
username: admin
password: Admin123!
```

#### Prova negativa 401

Duplicare temporaneamente `Current user`, selezionare `No Auth` nella scheda
Authorization ed eseguire la copia.

Risultato atteso:

```http
HTTP/1.1 401 Unauthorized
```

La risposta deve avere il codice applicativo `INVALID_CREDENTIALS`. Questa è
la prova che gli endpoint protetti non sono accessibili anonimamente.

### 5.3 Cartella `02 - Users`

Eseguire:

1. `Link Steam ID to account`;
2. `Get current user details`;
3. `Update current user email`.

Il collegamento Steam recupera l'identità pubblica del giocatore e crea il
profilo associato all'account. Nella risposta mostrare:

```text
playerProfile.id
playerProfile.steamId
playerProfile.displayName
playerProfile.currentElo
```

La presenza di `playerProfile` dentro la risposta dell'utente rende visibile la
relazione OneToOne `User`-`PlayerProfile`.

Questa operazione richiede accesso a Internet. Se entrambi i provider esterni
sono indisponibili, il backend restituisce un errore controllato `502` senza
compromettere le altre funzioni locali.

### 5.4 Cartella `03 - Admin Users`

Eseguire con il token amministrativo già configurato nella cartella:

1. `List users`;
2. `Set user role`;
3. `Enable user`.

La lista restituisce una risposta paginata. Le altre richieste dimostrano che
ruolo e stato dell'account possono essere modificati solamente da un admin.

#### Prova negativa 403

Duplicare temporaneamente `List users` e, nella scheda Authorization della
copia, impostare un Bearer Token con:

```text
{{accessToken}}
```

Risultato atteso:

```http
HTTP/1.1 403 Forbidden
```

La risposta deve contenere `FORBIDDEN_OPERATION`. Questa prova distingue
autenticazione e autorizzazione basata sui ruoli.

### 5.5 Cartella `04 - Unit Catalog`

Eseguire:

1. `Search units`;
2. `Get unit`;
3. `List specializations`.

Questi endpoint sono pubblici. `Search units` mostra contemporaneamente:

- filtri per fazione e categoria;
- intervallo di costo;
- paginazione;
- ordinamento;
- DTO con dati tecnici e specializzazioni.

Per mostrare il filtro per brigata, prendere un `id` dalla risposta di
`List specializations` e aggiungere a `Search units` il parametro:

```text
specializationId=<ID ottenuto>
```

Questo mostra anche la relazione ManyToMany tra unità e specializzazioni.

### 5.6 Cartella `05 - Admin Catalog`

Eseguire nell'ordine:

1. `Create specialization` — atteso `201 Created`;
2. `Create unit` — atteso `201 Created`;
3. `Update unit` — atteso `200 OK`;
4. `Delete unit` — atteso `204 No Content`.

La nuova unità viene creata indicando `specializationIds`, quindi la prova non
è un CRUD isolato: salva anche l'associazione ManyToMany.

Le richieste utilizzano `{{adminToken}}`. Con un token utente devono essere
rifiutate con `403 Forbidden`.

### 5.7 Cartella `06 - Match History`

Eseguire:

1. `Import JSON match`;
2. `List player matches`;
3. `Get match detail`.

Alla prima importazione il risultato atteso è:

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/players/<playerId>/matches
```

Il corpo indica `importedCount: 1` e salva automaticamente `matchId`.

Nel dettaglio della partita mostrare:

- dati generali del match;
- prestazione del giocatore;
- rating precedente e successivo;
- danni, punteggio e obiettivi;
- unità schierate e relative statistiche.

Questa risposta rende osservabili le relazioni:

- OneToMany tra partita e prestazioni;
- ManyToOne dalla prestazione alla partita;
- ManyToOne dalla prestazione al profilo giocatore;
- OneToMany tra prestazione e statistiche delle unità;
- ManyToOne dalla statistica alla relativa unità.

#### Prova di idempotenza

Per reinviare esattamente lo stesso match:

1. aprire `Import JSON match`;
2. disabilitare temporaneamente il Pre-request Script che genera
   `externalMatchId`;
3. premere nuovamente Send senza modificare il body.

Il risultato atteso diventa:

```http
HTTP/1.1 200 OK
```

```json
{
  "importedCount": 0,
  "skippedCount": 1,
  "importedMatchIds": [],
  "skippedExternalMatchIds": ["..."]
}
```

Questo dimostra che l'import non duplica una partita già presente.

### 5.8 Cartella `07 - Player Analytics`

Eseguire:

1. `Player career analysis`;
2. `Player performance trend`;
3. `Player unit analysis`;
4. `Player unit detail`;
5. `Player unit match history`.

Queste richieste utilizzano i dati persistiti in MySQL dalla cartella
precedente. Mostrano logica di business oltre al CRUD:

- numero di partite e percentuale di vittorie;
- andamento del rating;
- medie e totali delle prestazioni;
- aggregazione per unità;
- storico paginato dell'utilizzo di una singola unità.

Gli endpoint sono accessibili solamente dal proprietario del profilo o da un
amministratore.

### 5.9 Cartella `08 - Dataset Analytics`

Eseguire:

1. `Dataset unit analytics`;
2. `Dataset unit detail`;
3. `Dataset map analytics`;
4. `Dataset specialization analytics`.

Queste API sono pubbliche e aggregano i dati dell'intero dataset. Dimostrano
query JPQL, proiezioni e logiche statistiche per unità, mappe e brigate.

### 5.10 Cartella `09 - Steam Debrief`

Eseguire:

1. `Player debrief by Steam ID`;
2. `Reject invalid Steam ID`.

La prima richiesta deve restituire identità, carriera, match recenti e unità
impiegate. Evidenziare inoltre:

```text
source
diagnostics.durationMs
diagnostics.requestedMatches
diagnostics.loadedMatches
diagnostics.discardedMatches
diagnostics.discardedMatchIds
diagnostics.invalidFields
diagnostics.warnings
```

`source` indica quale provider ha fornito il risultato. `diagnostics` rende
espliciti eventuali dati incompleti: un match non disponibile non viene più
ignorato silenziosamente.

La seconda richiesta usa uno Steam ID non valido e deve restituire:

```http
HTTP/1.1 400 Bad Request
```

con il dettaglio della validazione nel formato di errore comune.

## 6. Verifica delle metriche dei provider

Dopo avere eseguito il debrief Steam, creare in Postman una richiesta GET:

```text
{{baseUrl}}/actuator/metrics
```

Nella scheda Authorization selezionare Bearer Token e inserire:

```text
{{adminToken}}
```

La risposta deve elencare:

```text
battle.debrief.steam.lookups
battle.debrief.steam.lookup.duration
battle.debrief.steam.matches.discarded
battle.debrief.steam.fields.invalid
```

`fields.invalid` compare dopo che il provider ha restituito almeno un campo non
valido. Il contatore dei match scartati viene invece inizializzato anche a zero.

Per vedere il dettaglio della durata:

```text
{{baseUrl}}/actuator/metrics/battle.debrief.steam.lookup.duration
```

Le statistiche disponibili comprendono conteggio, tempo totale, tempo massimo
e tag relativi a provider ed esito.

## 7. Dimostrazione breve consigliata

Se il tempo è limitato, eseguire almeno questa sequenza:

1. `Application health`;
2. `Register local user`;
3. `Login local user`;
4. `Login demo admin`;
5. `Current user`;
6. prova manuale `401`;
7. `Link Steam ID to account`;
8. `List users` e prova manuale `403`;
9. `Search units` e `List specializations`;
10. intera cartella `05 - Admin Catalog`;
11. intera cartella `06 - Match History`, inclusa la prova di idempotenza;
12. `Player career analysis` e `Player unit analysis`;
13. intera cartella `08 - Dataset Analytics`;
14. intera cartella `09 - Steam Debrief`;
15. endpoint Actuator delle metriche.

Questa sequenza dimostra tutti i principali requisiti senza aprire il sito.

## 8. Problemi comuni

### `{{baseUrl}}` non viene sostituito

Selezionare l'environment **Battle Debrief - Local**. Importare il solo file
della Collection non basta: serve anche il file environment.

### Risposta 401

Il token manca o è scaduto. Rieseguire `Login local user` oppure
`Login demo admin`; gli script aggiorneranno automaticamente i token.

### Risposta 403

Il token è valido, ma non possiede il ruolo o non è proprietario della risorsa.
Controllare se la richiesta richiede `{{accessToken}}` oppure `{{adminToken}}`.

### Risposta 409 durante il collegamento Steam

Lo Steam ID è già associato a un altro account nel database persistente.
Ripartire da un volume pulito con:

```bash
docker compose down --volumes
docker compose up --build --detach
```

### Risposta 502 durante il collegamento o il debrief Steam

I provider pubblici esterni sono temporaneamente indisponibili o hanno superato
i timeout configurati. Catalogo, utenti, import e analytics locali restano
utilizzabili. Riprovare in seguito o mostrare i test automatici del fallback.

### `Get match detail` non ha un `matchId`

Eseguire prima `Import JSON match`. Lo script della risposta salva
automaticamente il primo ID importato.

### `Player unit detail` non ha un `unitId`

Eseguire prima `Player unit analysis`, che salva automaticamente l'ID della
prima unità restituita.

## 9. Chiusura della dimostrazione

Per arrestare i container conservando il database:

```bash
docker compose down
```

Per eliminare anche tutti i dati creati durante la dimostrazione:

```bash
docker compose down --volumes
```
