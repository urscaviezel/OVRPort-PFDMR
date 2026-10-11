# OVRPort PFDMR Edition

**0.1.0-beta.1 · Experimentelle Vorabversion · Windows x64**

Eine inoffizielle PFDMR-Variante von [ovrport/app](https://github.com/ovrport/app), mit optionaler Stereo-Korrektur für Play For Dream MR. Vielen Dank an die ursprünglichen OVRPort-Entwickler und die Mitwirkenden der verwendeten Bibliotheken. Dieses Projekt ist unabhängig von OVRPort, Play For Dream, Meta und den Spieleherstellern.

[English](README.md) · [Downloads](https://github.com/urscaviezel/OVRPort-PFDMR/releases) · [Build-Anleitung](docs/BUILD.md)

## Download und Start

1. Bei den Releases das Windows-ZIP herunterladen und **vollständig entpacken**.
2. `OVRPort-PFDMR.exe` starten. Java ist im Paket enthalten.
3. Alternativ `Start-OVRPort-PFDMR.cmd` verwenden.

Windows 10/11 x64. Der EXE-Starter verwendet das in Windows enthaltene .NET Framework 4.x. Internet wird für Overport-Bibliotheksversionen und Release-Abfragen benötigt. Wie das Original verwendet die Anwendung `%USERPROFILE%\overport\cache`, einschließlich vorhandener Signaturschlüssel.

## APK mit Stereo-Korrektur erstellen

1. Eine eigene APK laden und eine Overport-Bibliotheksversion auswählen.
2. **PFDMR OpenXR compatibility (minimum)** einschalten. Dieser Schalter setzt die anderen Optionen zurück und ergänzt die OpenXR-Systemberechtigung.
3. Danach **PFDMR Stereo-Fix: parallel eyes (experimental)** aktivieren.
4. Weitere für diese App benötigte Overport-Optionen einschalten, etwa das Kopieren der Overport-Bibliotheken.
5. Mit **Confirm** unter einem eigenen Namen speichern, etwa `Spiel.PFDMR.StereoFix.apk`.
6. Wie gewohnt auf der PFDMR installieren und im Headset testen.

Der Stereo-Fix ist standardmäßig **aus**; es gibt keine automatische Spieleerkennung. Der Dateinamenvorschlag enthält `.PFDMR`; `.StereoFix` beim Speichern selbst ergänzen. Die Grundoption allein verändert nur das Manifest und konvertiert nicht automatisch alle Quest-APIs.

## Funktionsweise und Teststand

Die Korrektur ersetzt die Orientierungen beider OpenXR-Ansichten durch eine gemeinsame Orientierung. **Augenpositionen, Augenabstand und Sichtfeldwerte bleiben erhalten; es bleibt echtes Stereo-3D.** Die zusätzliche Bibliothek arbeitet nur innerhalb der ausgewählten APK. Der ursprüngliche Loader bleibt als `libpfdmr_original_openxr.so` erhalten. Firmware und systemweite OpenXR-Laufzeit werden nicht ersetzt.

- Der zugrunde liegende Ansatz beseitigte im bisherigen privaten Test von **Batman: Arkham Shadow 1.4.1-350961** die Doppelbilder. Das war kein vollständiger Test aller Spielszenen und keine Bestätigung dieser Beta für weitere Spiele.
- Automatisierte Tests prüfen APK-Neubau, Signaturen, wiederholtes Patchen und Rückkehr ohne Fix. Native Tests verwenden eine **simulierte OpenXR-Laufzeit** auf der PFDMR und ersetzen keinen Sichttest.
- Nur ARM64-APKs mit passendem OpenXR-Loader werden unterstützt. Unpassende Loader, zusätzliche nicht abgedeckte direkte OpenXR-Exporte und gemischte OpenXR-Architekturen werden abgelehnt.
- Unveränderte Sichtfeldwerte sind Teil dieses Ansatzes. Bildränder, Tiefenwirkung und schnelle Kopfbewegungen müssen pro App geprüft werden.
- Andere Ursachen für Doppelbilder werden nicht automatisch behoben. Für diese Beta werden keine Android-, Linux- oder macOS-Patcher veröffentlicht.

## Vergleich und Rückkehr

Dieselbe Ausgangs-APK mit denselben Optionen nochmals patchen, aber den Stereo-Fix ausschalten. Beide Ausgaben separat behalten. Eine von dieser Edition erzeugte Stereo-APK kann erneut geladen werden: Ist der Fix aus, wird ihre Loader-Sicherung wiederhergestellt. Wiederholtes Einschalten verschachtelt die Korrektur nicht.

Die alte private Batman-Sonderversion mit `libmanta_overport.so` bitte nicht erneut mit Stereo-Fix patchen; stattdessen die APK ohne alten Fix verwenden.

## Rückmeldungen und Lizenz

[Issues](https://github.com/urscaviezel/OVRPort-PFDMR/issues): Bitte App-Version, Firmware, Overport-Bibliotheksversion, Patch-Optionen und Beobachtungen mit/ohne Fix angeben. Besonders hilfreich sind Doppelbilder, Tiefenwirkung, Bildränder und Kopfbewegungen. **Keine APKs, OBBs oder privaten Signaturschlüssel hochladen.**

Basis: [ovrport/app](https://github.com/ovrport/app), Commit `1c7cb335c81369fbfd2957ad0edcb238956a8e3c`. Unsere Versionsnummer ist unabhängig von der des Originals und der Laufzeitbibliotheken. Frühere v1–v5 waren interne Teststände.

**GPLv3**, siehe [LICENSE](LICENSE) und [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md). Der vollständige Anwendungsquellcode mit PFDMR-Erweiterungen, nativer Bibliothek, Starter und Build-Skripten ist in diesem Repository enthalten; keine interne v4/v5-Datei wird zum Bauen benötigt. Verwende nur rechtmäßig erworbene Spiele. Die Releases enthalten keine Spiele oder privaten Signaturschlüssel.
