package me.quesia.practiceseedmod;

import me.quesia.practiceseedmod.core.Seed;
import me.quesia.practiceseedmod.core.Ws;
import me.quesia.practiceseedmod.core.WorldConstants;
import me.quesia.practiceseedmod.core.config.ConfigPresets;
import me.quesia.practiceseedmod.core.config.ConfigWrapper;
import me.quesia.practiceseedmod.core.RandomSeedGenerator;
import me.quesia.practiceseedmod.core.UpdateChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.world.GeneratorType;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import org.apache.logging.log4j.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PracticeSeedMod implements ClientModInitializer {
    public static final ModContainer MOD_CONTAINER = FabricLoader.getInstance().getModContainer("practiceseedmod").orElse(null);

    public static final String LOGGER_NAME = Objects.requireNonNull(MOD_CONTAINER).getMetadata().getName();
    public static final Logger LOGGER = LogManager.getLogger(LOGGER_NAME);
    public static Ws ws;
    public static final URI WS_URI = URI.create("ws://127.0.0.1:3000/connect");

    public static boolean RUNNING = false;
    public static UUID UUID;
    public static final List<Seed> QUEUE = new ArrayList<>();
    public static Seed CURRENT_SEED;

    public static boolean IS_RACE = false;
    public static String RACE_PASSWORD;
    public static String RACE_HOST;

    public static final AtomicBoolean SAVING = new AtomicBoolean(false);
    public static final Object SAVE_LOCK = new Object();

    public static Random BARTERING_RANDOM;
    public static Random GRAVEL_DROP_RANDOM;
    public static Random BLAZE_DROP_RANDOM;

    public static int MAXIMUM_PERCH_SECONDS;

    public static void log(Object msg) {
        LOGGER.log(Level.INFO, msg);
    }

    public static void initialiseLevelData(long l) {
        WorldConstants.reset();

        MinecraftClient client = MinecraftClient.getInstance();
        ConfigWrapper wrapper = new ConfigWrapper();

        client.execute(() -> client.method_29970(new SaveLevelScreen(new LiteralText("Initialising barter seed"))));
        log("Initialising barter seed.");
        int limit = ConfigPresets.BarterSeedPresets.values().length - 1;
        int barterSeedPresetIndex = wrapper.getIntValue("barterSeedPresetIndex", 0, limit);
        ConfigPresets.BarterSeedPresets preset = List.of(ConfigPresets.BarterSeedPresets.values()).get(barterSeedPresetIndex);
        BARTERING_RANDOM = new Random();
        if (preset.getSeeds() != null) {
            BARTERING_RANDOM.setSeed(preset.getSeeds().get(new Random().nextInt(preset.getSeeds().size() - 1)));
        }

        client.execute(() -> client.method_29970(new SaveLevelScreen(new LiteralText("Initialising gravel drop seed"))));
        log("Initialising gravel drop seed.");
        int flint = wrapper.getIntValue("gravelDropFlint", 1, 10);
        int gravel = wrapper.getIntValue("gravelDropGravel", 1, 10);
        if (gravel == -1) {
            gravel = flint + new Random(l).nextInt(4);
        }
        GRAVEL_DROP_RANDOM = new Random(new RandomSeedGenerator().getSeed(flint, gravel));

        client.execute(() -> client.method_29970(new SaveLevelScreen(new LiteralText("Initialising blaze drop seed"))));
        log("Initialising blaze drop seed.");
        int rods = wrapper.getIntValue("blazeDropRods", 6, 16);
        int kills = wrapper.getIntValue("blazeDropKills", 6, 16);
        if (kills == -1) {
            kills = rods + new Random(l).nextInt(6);
        }
        BLAZE_DROP_RANDOM = new Random(new RandomSeedGenerator().getSeed(rods, kills));

        client.execute(() -> client.method_29970(new SaveLevelScreen(new LiteralText("Initialising dragon perch"))));
        log("Initialising dragon perch.");
        limit = ConfigPresets.DragonPerchTimes.values().length - 1;
        int index = wrapper.getIntValue("dragonPerchTimeIndex", 0, limit);
        ConfigPresets.DragonPerchTimes dragonPreset = List.of(ConfigPresets.DragonPerchTimes.values()).get(index);
        MAXIMUM_PERCH_SECONDS = dragonPreset.getSeconds();
    }

    public static boolean playNextSeed() {
        if (!QUEUE.isEmpty() && RUNNING) {
            playNextSeed(QUEUE.get(0));
            return true;
        }
        return false;
    }

    public static void playNextSeed(Seed l) {
        MinecraftClient client = MinecraftClient.getInstance();

        LevelInfo levelInfo = new LevelInfo(
                "Practice Seed",
                GameMode.SURVIVAL,
                false,
                Difficulty.EASY,
                !IS_RACE,
                new GameRules(),
                DataPackSettings.SAFE_MODE
        );
        RegistryTracker.Modifiable registryTracker = RegistryTracker.create();
        GeneratorOptions generatorOptions = GeneratorType.DEFAULT.method_29077(
                registryTracker,
                l.seed,
                true,
                false
        );

        client.execute(() -> {
            if (client.world != null) {
                client.world.disconnect();
                if (client.isInSingleplayer()) {
                    client.disconnect(new SaveLevelScreen(new TranslatableText("menu.savingLevel")));
                } else {
                    client.disconnect();
                }
            }

            client.openScreen(
                    new CreateWorldScreen(
                            null,
                            levelInfo,
                            generatorOptions,
                            null,
                            registryTracker
                    )
            );
        });
        RUNNING = true;
        QUEUE.remove(l);
        CURRENT_SEED = l;

        log("Playing practice seed: " + l);
    }

    @Override
    public void onInitializeClient() {
        WorldConstants.reset();
        UpdateChecker.check();

        PracticeSeedMod.log("connecting to local ws server");
        ws = new Ws(WS_URI);
        ws.connect();
    }
}
