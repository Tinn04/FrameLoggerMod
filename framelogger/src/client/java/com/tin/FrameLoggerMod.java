package com.tin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class FrameLoggerMod implements ClientModInitializer {
    // LOGGING is used to function as an on/off switch for logging. Make's sure different threads don't lead to inconsistencies
    // lastNs stores the previous frame's timestamp in ns. Used later to get frametime
    // out opens the CSV file handle
    // togglekey will hold the keybind object. Used to track presses
    private static final AtomicBoolean LOGGING = new AtomicBoolean(false);
    private static long lastNs = 0L;
    private static BufferedWriter out;
    private static KeyBinding toggleKey;
    private static final java.util.List<Double> FRAME_TIMES = new java.util.ArrayList<>();

    @Override
    public void onInitializeClient() {
        // Register a toggle key (this will be set to F6 by default but can obviously be changed in-game)
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.framelogger.toggle", GLFW.GLFW_KEY_F6, "key.categories.misc"
        ));

        // Toggle Start/Stop on keypress
        // toggleKey.wasPressed() will return true when pressed. Used to handle multiple clicks
        // Simple logic. If LOGGING.get() returns true then this click is intended to stop
        // otherwise start the logging
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                if (LOGGING.get()) stopLogging(client);
                else startLogging(client);
            }
        });

        // Per-frame hook at end of world render (AKA the bread and butter of this mod)
        // WorldRenderEvents.END fires once per every rendered frame: "Called after all world rendering is complete and changes to GL state are unwound"
        // We are using a render event as it provides more accurate time stamps compared to a tick (20 ticks = 1 sec)
        // checks to see if logging is turned on
        WorldRenderEvents.END.register(ctx -> {
            if (!LOGGING.get()) return;
            long now = System.nanoTime();
            // log the current time in ns. For the first frame we won't have a previous frame so lastNs will be empty so we skip
            // dtNs is the delta between the two END events (duration between frames as seen by the client)
            // convert ns to ms (ms is more standard)
            // convert framtime into fps (1000/frametime_ms)
            // add it into the csv
            if (lastNs != 0L) {
                long dtNs = now - lastNs;
                double ftMs = dtNs / 1_000_000.0;
                double fps = ftMs > 0 ? 1000.0 / ftMs : 0.0;
                writeRow(ftMs, fps);
            }
            // update lastNs before repeating
            lastNs = now;
        });
    }

    private static void startLogging(MinecraftClient client) {
        try {
            // build frame_logs/ directory to store the collected data
            // create the object (dir) and check if it exists, if not then create it
            // if you ever forget https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
            String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now());
            File dir = Paths.get(client.runDirectory.getAbsolutePath(), "frame_logs").toFile();
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "mc_frametimes_" + stamp + ".csv");
            out = new BufferedWriter(new FileWriter(file, false));
            out.write("timestamp_ms,frametime_ms,fps\n");
            out.flush();
            lastNs = 0L;
            LOGGING.set(true);
            toast(client, "[FrameLogger] START (" + file.getName() + ")");
        } catch (Exception e) {
            toast(client, "[FrameLogger] FAILED TO START: " + e.getMessage() + "What'd you do this time, Tin??");
        }
    }

    private static void stopLogging(MinecraftClient client) {
        // set LOGGING to false and reset lastNs
        LOGGING.set(false);
        lastNs = 0L;
        try {
            if (out != null) { out.flush(); out.close(); }
            toast(client, "[FrameLogger] STOP");
        } catch (Exception e) {
            toast(client, "[FrameLogger] STOP (with errors): " + e.getMessage() + "Fix your code, man :( (or maybe it's not your code :D)");
        }
    }

    private static void writeRow(double ftMs, double fps) {
        try {
            long tsMs = Instant.now().toEpochMilli();
            if (out != null) {
                out.write(tsMs + "," +
                        String.format(Locale.US, "%.3f", ftMs) + "," +
                        String.format(Locale.US, "%.2f", fps) + "\n");
            }
        } catch (Exception ignored) {}
    }

    // Help function to send message to the player
    private static void toast(MinecraftClient client, String msg) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(msg), false);
        }
    }
}
