# Changelog

Tutte le modifiche rilevanti del progetto sono documentate in questo file.

## [1.0.4] — 2026-11-03

### Correzioni
- Telecamere con flusso HD oltre la risoluzione gestita dal decoder della TV (es. 4K su TV 1080p): si usa automaticamente il flusso extra Dahua (subtype 2) e, se non disponibile, LD, con un avviso, invece di restare su «Connessione…»
- Banner dell'app dichiarato anche sull'activity di avvio (visibile nei launcher Android TV)
- Lo schermo non va più in standby mentre si guarda la griglia delle telecamere

### Compatibilità
- Aggiornata ad Android 16 (target SDK 36)

## [1.0.3] — 2026-11-03

### Novità
- Icona di layout nella barra in basso: OK apre la scelta tra 1, 4, 9 o 16 telecamere per pagina (▼ dalla griglia)
- Interruttore «Aa» nella barra in basso per mostrare o nascondere i nomi delle telecamere
- Switch LD/HD per singola telecamera: in griglia dal menu rapido (OK tenuto premuto), a schermo intero con ▲ ▼
- Nuova grafica delle impostazioni e delle finestre di dialogo
- Barra dei gruppi più compatta, con testo bianco sulla voce selezionata
- Dopo 5 secondi senza tasti premuti le barre spariscono e le telecamere vanno a tutto schermo; un tasto qualsiasi le fa ricomparire

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
