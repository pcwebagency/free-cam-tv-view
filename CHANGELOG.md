# Changelog

Tutte le modifiche rilevanti del progetto sono documentate in questo file.

## [1.0.3] — 2026-11-03

### Novità
- Icone di layout nella barra in basso: 1, 4 o 9 telecamere per pagina (▼ dalla griglia)

### Correzioni
- La voce selezionata nella barra dei gruppi non perde più l'evidenziazione
- Più telecamere H.265 (HEVC) in griglia: se il decoder hardware non ha istanze libere si usa il decoder software, invece di mostrare l'errore "Failed to initialize OMX…hevc"

## [1.0.2] — 2026-11-03

### Novità
- NVR: nel campo Canale si possono inserire più canali (`1-8` oppure `1,3,5`), con una voce creata per ciascuno; utile da remoto, dove l'import automatico non è disponibile
- "Importa tutti i canali NVR" usa la porta HTTP e la porta RTSP indicate (funziona anche da remoto se la porta HTTP è inoltrata)

## [1.0.1] — 2026-11-03

### Modifiche
- L'app si chiama ora **Free Cam TV View** (nome, banner e menu Informazioni)

### Sicurezza
- Password delle telecamere cifrate con l'Android Keystore (AES-256-GCM); quelle già salvate vengono cifrate automaticamente

## [1.0.0] — 2026-11-03

Prima versione pubblica.

### Funzionalità
- Griglia live 2×2 con paginazione e schermo intero HD con audio
- Ricerca automatica dei dispositivi Dahua (protocollo DHIP) e delle telecamere RTSP in rete
- Import automatico dei canali NVR con i nomi delle telecamere
- Aggiunta manuale con IP o nome host DDNS, porta, canale e URL RTSP personalizzato
- Gruppi di telecamere (es. Studio, Casa) con barra di selezione
- Qualità del flusso per telecamera (Standard / Alta)
- Apriporta per videocitofoni Dahua (due serrature, con conferma)
- Video in formato 16:9 fisso, bordo di selezione che sparisce dopo un secondo
- Menu Informazioni con versione e credits; aggiornamenti dalle Releases di GitHub
- Riconnessione automatica con watchdog; tentativi diradati con credenziali errate

### Correzioni al modulo RTSP di Media3
- Nessun crash con telecamere che rispondono senza header `CSeq`
- Supporto per password contenenti `@`
