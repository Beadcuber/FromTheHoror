package com.fromthehorror.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Zero-setup text-to-speech using the voice your operating system already has. */
public final class Tts {
    private Tts() {}

    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "fromthehorror-tts");
        t.setDaemon(true);
        return t;
    });

    public static void speak(String text) {
        final String clean = text.replaceAll("[^A-Za-z0-9 .,!?'-]", "").replaceFirst("^[\\s-]+", "");
        if (clean.isBlank()) return;
        POOL.submit(() -> {
            String os = System.getProperty("os.name", "").toLowerCase();
            try {
                ProcessBuilder pb;
                if (os.contains("win")) {
                    pb = new ProcessBuilder("powershell", "-NoProfile", "-Command",
                            "Add-Type -AssemblyName System.Speech; $s = New-Object System.Speech.Synthesis.SpeechSynthesizer; $s.Rate = -2; $s.Speak('" + clean.replace("'", "''") + "')");
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("say", clean);
                } else {
                    pb = new ProcessBuilder("espeak", clean);
                }
                pb.redirectErrorStream(true);
                Process p;
                try {
                    p = pb.start();
                } catch (java.io.IOException e) {
                    if (os.contains("win") || os.contains("mac")) throw e;
                    p = new ProcessBuilder("espeak-ng", clean).redirectErrorStream(true).start();
                }
                p.getInputStream().readAllBytes();
                p.waitFor();
            } catch (Exception ignored) {
                // No voice available on this system: stay silent, the chat text still shows.
            }
        });
    }
}
