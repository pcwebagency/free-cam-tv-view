# Changelog

Tutte le modifiche rilevanti del progetto sono documentate in questo file.

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
