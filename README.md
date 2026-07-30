# Battle Debrief

Backend REST per la raccolta e l'analisi delle prestazioni dei giocatori di
Broken Arrow.

## Requisiti

- Java 21
- Docker e Docker Compose per l'ambiente completo

Il progetto include Maven Wrapper, quindi non richiede un'installazione globale
di Maven.

## Verifica del progetto

Su Windows:

```powershell
.\mvnw.cmd clean verify
```

Su Linux e macOS:

```bash
./mvnw clean verify
```

La configurazione predefinita usa il profilo `dev` e si collega a MySQL in
locale. I test usano un database H2 in memoria configurato con compatibilità
MySQL.
