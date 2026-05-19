README — Sickbay Test Bench
===========================

This folder is a local test bench for the Sickbay integration in the Pulse Android app.
It stands in for the real Sickbay cloud server using a plain HTTP Socket.IO server.

PREREQUISITES
-------------
- Node.js v14+ installed on the test PC
- Android phone, test PC, and any other devices on the SAME WiFi network
  (personal hotspot or home router — NOT an institutional/corporate network,
   which typically blocks device-to-device traffic via client isolation)

SETUP (one-time)
----------------
  cd sickbay-test-script
  npm install

EACH SESSION
------------
1. Start the server:

     node socketListen.js

   The console immediately prints all LAN IPs detected on this machine, e.g.:

     Socket.IO server listening on port 3001
       LAN IP [Wi-Fi]: 192.168.137.5
       Loopback (Phase 1 / same-machine test): 127.0.0.1

   Note the LAN IP — you will enter it in the Android app (Phase 2).

PHASE 1 — Server infrastructure test (no Android needed)
---------------------------------------------------------
2. Open test.html in a web browser (File → Open, or drag into browser).

3. In the "Server IP" field, enter:  127.0.0.1
   (Always use 127.0.0.1 for the browser test — it works regardless of network.)

4. Click Connect.
   Server console: "Client connected."

5. Click "Send Test Data".
   Server console should show:
     -----DATA RECEIVED at <timestamp>-----
     { "data": { "channel": "BED001", "ns": "TATTOOWAVE", ... } }
     channel=BED001  ns=TATTOOWAVE  signals=3  dt=0.2s

6. Click "Stream (10 packets)" to simulate continuous streaming.

PHASE 2 — Android integration test (no BLE hardware needed)
-----------------------------------------------------------
7. In the Pulse app on the Android device:
   Tap menu (⋮) → "Enter Sickbay IP" → enter the LAN IP printed in step 1 → Set.

8. Tap menu (⋮) → "Debug Mode".
   This uses debug.init (Sine/Square/Sawtooth signals, all Sickbay-enabled).
   No physical BLE sensor is required.

9. Server console should begin receiving DATA RECEIVED events continuously (~5/sec).

PHASE 3 — Real BLE device test
-------------------------------
10. Power on a Sickbay-enabled device: nicbp, ear_v1, ppg_max30102, or ppg_max86141.
11. In the Pulse app, tap Scan and connect to the device.
12. Verify DATA RECEIVED events arrive and signal keys match the .sickbayID values
    in that device's .init file.

PASS CRITERIA
-------------
- "Client connected." logged when browser or Android connects
- DATA RECEIVED events logged with correct JSON:
    data.channel = your bed name (default "BED001")
    data.ns      = "TATTOOWAVE" (or namespace from the init file)
    data.signals = object keyed by sickbayID integers, each mapping to a float array
- No "[WARN]" lines in server output
- For Debug Mode: 3 signal keys (1000, 1001, 1002) appear at ~5 Hz continuously
