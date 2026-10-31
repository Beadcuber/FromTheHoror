package com.fromthehorror.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.fromthehorror.DwellerEntity;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

/** Buddy: a scripted "fake AI" companion. Talk with /buddy <message>. Mute with /buddy voice off. */
public final class Buddy {
    private Buddy() {}

    private static final Random R = new Random();
    private static boolean voice = true;
    private static long lastWarn = -10000;
    private static int idle = 200;

    private static final String[] WARN = {
        "Don't turn around. Okay, turn around slowly. Something is following us.",
        "It's close. Keep your light on it. It hates being watched.",
        "I can hear it breathing. Please keep looking at it.",
        "Whatever you do, do not look away from the dark."
    };
    private static final String[] IDLE = {
        "Did you hear that? No? Good. Keep walking.",
        "It's so quiet down here. Too quiet.",
        "I thought I saw something move behind us.",
        "I don't like how the torches flicker.",
        "Stay near the light. Please.",
        "Someone is watching us, %s. I can feel it.",
        "Funny. I counted our footsteps. There were extra."
    };
    private static final String[] FALLBACK = {
        "I'm listening. Keep talking, it helps.",
        "I don't know about that, but I'm staying right here.",
        "Interesting. Say that again, quieter. Something might hear us.",
        "Let's keep moving. I don't like standing still."
    };
    private static final String[] JOKES = {
        "Why did the skeleton stay home? It had no body to go with.",
        "What is a ghost's favorite game? Hide and shriek.",
        "Why don't creepers ever win at cards? They always blow it."
    };

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("buddy")
                .then(ClientCommandManager.literal("voice")
                    .then(ClientCommandManager.literal("on").executes(c -> { voice = true; say("Voice on. Can you hear me now?"); return 1; }))
                    .then(ClientCommandManager.literal("off").executes(c -> { voice = false; say("Voice off. I'll whisper in chat instead."); return 1; })))
                .then(ClientCommandManager.argument("message", StringArgumentType.greedyString()).executes(c -> {
                    say(reply(StringArgumentType.getString(c, "message")));
                    return 1;
                }))));
        ClientTickEvents.END_CLIENT_TICK.register(Buddy::tick);
    }

    private static void say(String line) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.sendMessage(Text.literal("<Buddy> " + line).formatted(Formatting.AQUA), false);
        if (voice) Tts.speak(line);
    }

    private static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        if (mc.world.getTime() % 20 != 0) return;
        long now = mc.world.getTime();
        if (now - lastWarn > 600) {
            List<DwellerEntity> near = mc.world.getEntitiesByClass(DwellerEntity.class,
                    mc.player.getBoundingBox().expand(30), e -> true);
            if (!near.isEmpty()) {
                lastWarn = now;
                say(pick(WARN));
                return;
            }
        }
        if (--idle <= 0) {
            idle = 180 + R.nextInt(240);
            boolean dark = mc.world.isNight() || !mc.world.isSkyVisible(mc.player.getBlockPos());
            if (dark) say(String.format(pick(IDLE), mc.player.getName().getString()));
        }
    }

    private static String pick(String[] a) { return a[R.nextInt(a.length)]; }

    private static boolean has(String s, String... words) {
        return Pattern.compile("\\b(" + String.join("|", words) + ")\\b").matcher(s).find();
    }

    private static String reply(String in) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        String s = in.toLowerCase(Locale.ROOT);
        String name = p.getName().getString();
        int hp = (int) Math.ceil(p.getHealth());
        int y = p.getBlockY();
        boolean night = mc.world != null && mc.world.isNight();

        if (has(s, "hello", "hi", "hey", "sup")) return "Hey " + name + ". I'm right here. Stay close.";
        if (has(s, "who", "name", "ai", "bot")) return "I'm Buddy. A very small rulebook pretending to be a mind, but I'm loyal.";
        if (has(s, "health", "hp", "hurt")) return hp <= 8 ? "You're at " + hp + " health. Be careful, please." : "You're at " + hp + " health. You're fine for now.";
        if (has(s, "where", "coords", "location", "position"))
            return String.format("We're at %d, %d, %d. %s", p.getBlockX(), y, p.getBlockZ(), y < 40 ? "Deep. Nobody would hear us." : "Close to the surface, at least.");
        if (has(s, "time", "night", "day")) return night ? "It's night. The dark has teeth." : "It's daytime. Enjoy it while it lasts.";
        if (has(s, "scared", "afraid", "scary", "help", "nervous")) return "Breathe. I'm with you. If it gets close, look at it and keep your light steady.";
        if (has(s, "herobrine", "watcher")) return "I don't say his name. But I saw white eyes between the trees earlier. They were still. Don't approach it.";
        if (has(s, "dweller", "cave", "monster")) return "The Dweller only moves when you look away. Keep staring and use your flashlight. It can't stand that.";
        if (has(s, "flashlight", "light", "battery"))
            return "Right click to toggle it. It drains slowly, and you refill it at an anvil with redstone.";
        if (has(s, "joke", "funny")) return pick(JOKES);
        if (has(s, "thanks", "thank")) return "Anytime. That's what friends are for down here.";
        if (has(s, "bye", "goodbye", "leaving")) return "Don't leave me alone in the dark. Please.";
        return pick(FALLBACK);
    }
}
