# Fonti del catalogo unità

Il catalogo locale usa il roster pubblico esposto da BArmory e le varianti
visibili nell'hangar di BA Data. Lo script `scripts/sync-unit-catalog.ps1`
acquisisce unità, costi, categorie e associazioni alle specializzazioni e genera
il file versionato
`src/main/resources/catalog/units.json`.

- Fonte dati: `https://barmory.net`
- Indice delle varianti visibili: `https://ba.puliaev.com/hangar`
- Endpoint pubblici consultati: `/spec/all`, `/unit/all`,
  `/spec/{id}/{id}/units` e, solo come fallback, `/unit/{id}`
- Immagini: URL remoti ImageKit restituiti secondo i nomi asset del catalogo;
  nessun asset grafico di terze parti viene incluso nel repository
- Data di sincronizzazione: 31 luglio 2026

I campi non presenti nella fonte, come hit point, velocità, corazza e arma
principale, restano null anziché essere stimati. Le varianti non assegnate
direttamente a una specializzazione conservano un'associazione vuota. Le unità
curate usate dalla telemetria dimostrativa mantengono i dettagli locali già
verificati.

Il progetto non è affiliato con Steel Balalaika, Slitherine, BArmory o BA Data.
