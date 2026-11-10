<p align="center">
  <img src="art/banner.png" alt="Free Cam TV View" width="480">
</p>

<h1 align="center">Free Cam TV View</h1>

<p align="center">
  Visualizzazione live di telecamere IP, NVR e videocitofoni Dahua su Android TV e Amazon Fire TV.<br>
  Sviluppata da <a href="https://www.pcwebagency.it">PC Web Agency</a>.
</p>

<p align="center">
  <a href="https://www.amazon.it/dp/B0HQCFX3YN"><img src="https://img.shields.io/badge/Amazon_Appstore-Disponibile-FF9900?style=for-the-badge&logo=amazon&logoColor=white" alt="Disponibile su Amazon Appstore"></a>
  <a href="../../releases/latest"><img src="https://img.shields.io/github/v/release/pcwebagency/free-cam-tv-view?style=for-the-badge&label=APK&color=2ea44f" alt="Ultima versione APK"></a>
</p>

---

**Free Cam TV View** è un'app per Android TV e Fire TV, leggera e pensata per il telecomando, che mostra in tempo reale le telecamere della rete locale o di sedi remote. Riconosce automaticamente i dispositivi Dahua (telecamere IP, NVR/XVR, videocitofoni) e accetta qualsiasi flusso RTSP standard.

> *English summary — Free Cam TV View is a lightweight Android TV / Fire TV app to watch Dahua IP cameras, NVRs and video intercoms (plus any RTSP stream) live, with automatic network discovery, camera groups, remote-friendly controls and a door-open button for Dahua VTO intercoms.*

## Funzionalità

- **Ricerca automatica in rete** con il protocollo di discovery Dahua (DHIP), che riconosce modello e tipo (telecamera, NVR, videocitofono), più la scansione della porta RTSP per le telecamere di altre marche.
- **Import dei canali NVR**: dopo le credenziali vengono aggiunti tutti i canali con una telecamera collegata, con i loro nomi.
- **Aggiunta manuale** con IP o nome host (DDNS), porta, canale e URL RTSP personalizzato.
- **Griglia live** da 1, 4, 9 o 16 riquadri con pagine, e **schermo intero HD** con audio.
- **Gruppi** (es. *Studio*, *Casa*): barra di selezione **Tutte · Studio · Casa** nella schermata live.
- **Switch LD/HD** per ogni telecamera, separato per griglia e schermo intero, direttamente dal telecomando.
- **Apriporta per videocitofoni Dahua** (VTO): due serrature, con conferma.
- **Formato 16:9** fisso e centrato per tutte le telecamere.
- **Aggiornamenti dall'app**: controllo automatico delle nuove versioni pubblicate nelle Releases di GitHub e installazione con un clic (*≡ Menu → Informazioni e aggiornamenti*).
- **Riconnessione automatica** con watchdog. Con password errata i tentativi si diradano, per non bloccare l'account dell'NVR.
- Nessun account, nessun cloud, nessuna pubblicità: l'app comunica solo con le telecamere.

## Dispositivi supportati

| Tipo | Esempi | Note |
|---|---|---|
| Telecamere IP Dahua | IPC-HDW, IPC-HFW… | Flusso principale e secondario |
| NVR / XVR / DVR Dahua | NVR, XVR, HCVR | Import automatico dei canali (rete locale) |
| Videocitofoni Dahua | VTO2xxx, VTO3xxx | Video live + apriporta |
| Marchi OEM Dahua | Imou, Lorex, Amcrest… | Se espongono le API Dahua standard |
| Telecamere RTSP generiche | Qualsiasi marca | Tramite URL RTSP personalizzato |

I monitor interni dei videocitofoni (VTH) vengono riconosciuti ma non aggiunti, perché non hanno una telecamera.

## Requisiti

- Android TV o Fire TV con **Android 5.1** o superiore (Fire OS 5+).
- Telecamere raggiungibili in **RTSP** (porta 554 di default).

## Installazione

### Da Amazon Appstore (Fire TV)

Il modo più semplice su Fire TV: installa **Free Cam TV View** da [Amazon Appstore](https://www.amazon.it/dp/B0HQCFX3YN) oppure cercala dalla TV. Gli aggiornamenti arrivano dall'Appstore.

### Da APK (Android TV e Fire TV)

Scarica l'APK dalla pagina [Releases](../../releases).

#### Con ADB

1. Sulla TV attiva le opzioni sviluppatore. Su Fire TV: *Impostazioni → La mia Fire TV → Informazioni*, premi 7 volte sul nome del dispositivo, poi *Opzioni sviluppatore → Debug ADB*.
2. Dal computer:
   ```bash
   adb connect IP_DELLA_TV:5555        # conferma "Consenti" sulla TV
   adb install -r FreeCamTVView-1.0.6.apk
   ```

#### Senza computer

Abilita *App da origini sconosciute* e installa l'APK con l'app **Downloader** o da una chiavetta USB.

### Aggiornamenti

Installata da Amazon Appstore, l'app si aggiorna dall'Appstore. Installata da APK, l'app controlla da sola le nuove versioni e propone l'aggiornamento all'avvio. Il controllo si può fare anche a mano da *≡ Menu → Informazioni e aggiornamenti*. Al primo aggiornamento Android chiede di consentire a «Free Cam TV View» l'installazione di app (Fire TV: *Opzioni sviluppatore → Installa app sconosciute*).

## Utilizzo

Al primo avvio premi **OK → Cerca in rete**, seleziona i dispositivi, scegli il gruppo e inserisci utente e password.

### Comandi del telecomando

| Schermata | Tasto | Azione |
|---|---|---|
| Griglia | Frecce | Seleziona la telecamera |
| Griglia | **OK** | Schermo intero |
| Griglia | **◀ ▶** ai bordi | Pagina precedente / successiva |
| Griglia | **▲** dalla prima riga | Barra dei gruppi |
| Griglia | **OK tenuto premuto** | Menu rapido: LD/HD, schermo intero, apriporta |
| Griglia | **⏯** | Apriporta (videocitofoni) |
| Griglia | **▼** dall'ultima riga | Riquadri (1 · 4 · 9 · 16) e nomi (Aa) |
| Schermo intero | **◀ ▶** | Telecamera precedente / successiva |
| Schermo intero | **▲ ▼** | Passa da LD a HD e viceversa |
| Schermo intero | **OK** o **⏯** | Apriporta (videocitofoni) |
| Ovunque | **≡ Menu** | Impostazioni |

### Impostazioni di una telecamera

Nome, gruppo, IP o nome host, porta RTSP, utente, password, canale (anche più canali per gli NVR: `1-8`), URL RTSP personalizzato, porta HTTP, qualità LD/HD in griglia e a schermo intero, apriporta.

## Accesso da remoto

Per vedere le telecamere di un'altra sede, la TV deve raggiungere l'RTSP del dispositivo remoto. Le soluzioni sono due:

1. **VPN sito-sito** tra le due sedi (consigliata): i dispositivi remoti si aggiungono come se fossero in rete locale.
2. **Port forwarding** della porta **RTSP 554**, limitato all'IP pubblico della sede da cui si guarda. Per l'apriporta da remoto va inoltrata anche la porta **HTTP 80** del videocitofono.

> La porta **37777** usata dalle app Dahua (DMSS, SmartPSS) è un protocollo proprietario e **non** è supportata: serve la porta RTSP.

Esempio per router MikroTik:

```
/ip firewall nat add chain=dstnat in-interface-list=WAN protocol=tcp \
    src-address=IP_PUBBLICO_SEDE dst-port=47554 \
    action=dst-nat to-addresses=IP_LOCALE_NVR to-ports=554 \
    comment="RTSP NVR per app Free Cam TV View"
```

Nell'app si indicano il nome host DDNS e la porta esterna (es. `47554`). Per un NVR remoto scrivi nel campo **Canale** tutti i canali da vedere, ad esempio `1-8`: viene creata una voce per ciascuno. In alternativa, inoltrando anche la porta HTTP dell'NVR, *Importa tutti i canali NVR* legge in automatico canali e nomi.

## Sicurezza e privacy

- Le password sono salvate **solo sulla TV**, cifrate con una chiave AES-256 dell'**Android Keystore** che non lascia il dispositivo (Android 6.0 e successivi). Disinstallando l'app la chiave viene eliminata e le password vanno reinserite.
- L'app non usa servizi esterni, analytics o cloud.
- Con il port forwarding limita sempre l'accesso all'IP di origine e usa password robuste. Gli NVR Dahua bloccano l'account dopo ripetuti tentativi errati.

## Note tecniche

- Player: [AndroidX Media3](https://github.com/androidx/media) 1.4.1.
- Il modulo **RTSP di Media3** è incluso nei sorgenti, con due correzioni (dettagli in [NOTICE](NOTICE)):
  - risposte RTSP senza header `CSeq`, inviate da alcune telecamere economiche, che causavano il crash dell'app;
  - password contenenti `@`, che finivano nel nome host.
- Rendering su `TextureView`: i chip Amlogic di molte TV hanno un solo strato video hardware, che con più `SurfaceView` in griglia mostrerebbe una sola telecamera.
- Buffer ridotti per una latenza bassa sul live.

## Licenza

Distribuito con licenza **Apache 2.0**: vedi [LICENSE](LICENSE) e [NOTICE](NOTICE).

Dahua, Imou, Lorex, Amcrest, Amazon, Fire TV e Android sono marchi dei rispettivi proprietari. Questo progetto è indipendente e non è affiliato né approvato da essi.

## Autore

**PC Web Agency**
[www.pcwebagency.it](https://www.pcwebagency.it) · info@pcwebagency.it

Sviluppo software, consulenza informatica e servizi web per aziende, enti e professionisti.
