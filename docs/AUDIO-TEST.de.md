# Audio-Testversion 0.1.0-beta.2-audio.1

Der neue Schalter **PFDMR Unreal Audio: 48 kHz (experimental)** ist standardmäßig aus. Er ist vom bisherigen Meta-XR-Audio-Patch unabhängig. Behalte die sonstigen Patch-Einstellungen deiner funktionierenden Action-Hero-Version bei und aktiviere zusätzlich diesen Schalter. Wenn du die PFDMR-Grundoption verwendest, aktiviere sie zuerst, da sie andere Auswahlen zurücksetzt.

Der Patcher erkennt bei Action Hero das Unreal-Projekt Solero und ergänzt vor dem nativen Spielstart einen kleinen Helfer. Dieser setzt in den privaten Spieldaten unter `files/UnrealGame/Solero/Solero/Saved/Config/Android/Engine.ini` ausschließlich `AudioSampleRate=48000` im Abschnitt `[/Script/AndroidRuntimeSettings.AndroidRuntimeSettings]`. Bestehende andere Einstellungen bleiben erhalten. Die ursprüngliche Datei wird einmalig als `Engine.ini.pfdmr-audio48.original` gesichert. Es werden keine systemweiten Audioeinstellungen geändert und weder OBB noch native Spielbibliotheken für diesen Fix verändert.

## Aussagekräftiger Test mit Action Hero

Die manuell gesetzte 48-kHz-Einstellung aus dem früheren Test würde den Vergleich verdecken. Falls du diese noch hast, schließe das Spiel vollständig und ändere in der bestehenden Engine.ini nur `AudioSampleRate=48000` zurück auf `AudioSampleRate=44100`. Keine App-Daten oder Spielstände löschen. Falls die Datei ausschließlich aus dem damaligen Abschnitt und dieser Zeile besteht, kannst du sie stattdessen zur Sicherung umbenennen. Vor einem solchen Umbenennen den Inhalt prüfen, damit keine anderen Einstellungen vorübergehend verloren gehen.

1. Patche deine funktionierende Ausgangs-APK mit denselben Optionen wie bisher plus dem neuen Audio-Schalter. Speichere sie unter einem neuen Namen.
2. Installiere sie als Update mit demselben Signaturschlüssel. Bei einer Signaturmeldung nicht deinstallieren, sondern zunächst die Schlüsselzuordnung prüfen.
3. Starte Action Hero. Der Helfer soll die 48-kHz-Einstellung selbst anlegen beziehungsweise setzen. Prüfe das Knistern und einige Minuten Gameplay.
4. Zur technischen Kontrolle kann `adb -s D3HDXD3D4525000262 logcat -d -s PFDMRAudio48:I` den Helferstatus anzeigen. Der Eintrag bestätigt die Konfigurationsverarbeitung; der tatsächliche Audiostream und dein Hörtest bestätigen erst die Wirkung.

## Abschalten und Vergleich

Lade die **mit dieser Testversion erzeugte Audio-APK** erneut in den Patcher und lasse den Audio-Schalter aus. Beim nächsten Start dieser Ausgabe stellt der Helfer die vorherigen AudioSampleRate-Einträge aus seiner Sicherung wieder her und entfernt seine Sicherungsdatei. Spätere Änderungen anderer Einstellungen bleiben erhalten. Falls vorher kein Eintrag existierte, wird der Override entfernt.

Das bloße Installieren einer älteren APK ohne Helfer entfernt eine bereits gespeicherte Einstellung nicht. War die manuelle 48-kHz-Einstellung schon vor dem ersten Start der Testversion vorhanden, ist genau dieser Wert auch der Wiederherstellungswert. Daher den Ausgangswert für einen Vergleich zuerst wie oben beschrieben vorbereiten.

## Grenzen

Die Patcher-Integration und Dateioperationen werden automatisch geprüft. Ein Hörtest dieser APK-Integration steht noch aus. Andere Unreal-Spiele können andere Konfigurationspfade oder Ladeabläufe verwenden. Unity-Spiele, fehlende/mehrdeutige Unreal-Projekte und nicht unterstützte Aktivitäten werden mit einer Fehlermeldung abgelehnt. Nicht unterstützte INI-Zeichencodierung oder Schreibfehler werden protokolliert; das Spiel soll trotzdem starten. In dieser Testversion wird UTF-8, auch mit BOM, unterstützt.

Die Test-APK aus den automatischen Prüfungen verwendet einen separaten Testschlüssel und wird deshalb nicht ausgeliefert. Erstelle deine Ausgabe über den mitgelieferten Patcher mit deinem bisherigen Schlüsselbestand.
