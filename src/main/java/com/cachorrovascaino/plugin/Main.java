package com.cachorrovascaino.plugin;

import com.cachorrovascaino.plugin.Commands.CommandAdmin;
import com.cachorrovascaino.plugin.Commands.CommandMenu;
import com.cachorrovascaino.plugin.Cosmetics.CosmeticAsset;
import com.cachorrovascaino.plugin.Cosmetics.EyeAttachmentCosmetic;
import com.cachorrovascaino.plugin.Cosmetics.PlayerModelCosmetic;
import com.cachorrovascaino.plugin.Data.Components.DeathProcessed;
import com.cachorrovascaino.plugin.Listener.PacketListener;
import com.cachorrovascaino.plugin.Listener.PlayerListener;
import com.cachorrovascaino.plugin.Manager.ClanManager;
import com.cachorrovascaino.plugin.Manager.CooldownManager;
import com.cachorrovascaino.plugin.Manager.JutsuManager;
import com.cachorrovascaino.plugin.Manager.PlayerDataManager;
import com.cachorrovascaino.plugin.Systems.ComboTickSystem;
import com.cachorrovascaino.plugin.Systems.DamageTrackingSystem;
import com.cachorrovascaino.plugin.Systems.DeathDetectionSystem;
import com.cachorrovascaino.plugin.Utils.EyesUtils;
import com.cachorrovascaino.plugin.Utils.WeatherUtils;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.lookup.Priority;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.plugin.*;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.*;
import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

public class Main extends JavaPlugin {

    private static Main instance;
    private static int CHAKRA_STAT_INDEX = -1;
    private static PlayerDataManager dataManager;
    private static JutsuManager jutsuManager;
    private static CooldownManager cooldownManager;
    private static ClanManager clanManager;

    private static EyesUtils eyesUtils;
    private static WeatherUtils weatherUtils;

    private Map<UUID, UUID> lastAttackers;
    private ComponentType<EntityStore, DeathProcessed> deathMarkerType;

    public Main(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static Main get() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();

        this.lastAttackers = new ConcurrentHashMap<>();
        this.deathMarkerType = this.getEntityStoreRegistry().registerComponent(DeathProcessed.class, "DeathProcessed", DeathProcessed.CODEC);

        File pluginFolder = new File("TalesOfShinobi/");
        if (!pluginFolder.exists()) { pluginFolder.mkdirs(); }

        dataManager = new PlayerDataManager(pluginFolder.toPath());
        jutsuManager = new JutsuManager(this);
        clanManager = new ClanManager(this);
        cooldownManager = new CooldownManager();

        this.getCodecRegistry(CosmeticAsset.CODEC)
                .register(Priority.NORMAL, "PlayerModel", PlayerModelCosmetic.class, PlayerModelCosmetic.CODEC)
                .register(Priority.NORMAL, "EyeAttachment", EyeAttachmentCosmetic.class, EyeAttachmentCosmetic.CODEC);

        HytaleAssetStore.Builder builder = HytaleAssetStore.builder(CosmeticAsset.class, new DefaultAssetMap<>());
        builder.setPath("TalesOfShinobi/Cosmetics");
        builder.setCodec(CosmeticAsset.CODEC);
        builder.setKeyFunction(CosmeticAsset.KEY_FUNCTION);
        builder.loadsAfter(new Class[]{ModelAsset.class});

        AssetRegistry.register(builder.build());

        PacketAdapters.registerInbound(new PacketListener(this));
        RegisterCommand();
        RegisterListener();
        RegisterSystem();
    }

    public static int getChakraStatIndex() { return CHAKRA_STAT_INDEX; }
    public static PlayerDataManager getDataManager() { return dataManager; }
    public static JutsuManager getJutsuManager() { return jutsuManager; }
    public static CooldownManager getCooldownManager(){ return cooldownManager; }
    public static ClanManager getClanManager() { return clanManager; }
    public static EyesUtils getEyesUtils() { return eyesUtils; }
    public static WeatherUtils getWeatherUtils() { return weatherUtils; }

    public void RegisterCommand() {
        this.getCommandRegistry().registerCommand(new CommandMenu());
        this.getCommandRegistry().registerCommand(new CommandAdmin());
    }

    public void RegisterSystem(){
        this.getEntityStoreRegistry().registerSystem(new ComboTickSystem(getJutsuManager(), getDataManager(), getEyesUtils(), getWeatherUtils()));
        this.getEntityStoreRegistry().registerSystem(new DamageTrackingSystem(this.lastAttackers));
        this.getEntityStoreRegistry().registerSystem(new DeathDetectionSystem(this.lastAttackers, this.deathMarkerType));
    }

    public void RegisterListener() {
        EventRegistry eventBus = getEventRegistry();
        eventBus.registerGlobal(PlayerReadyEvent.class, PlayerListener::onPlayerReady);
    }

    @Override
    protected void start() {
        try {
            CHAKRA_STAT_INDEX = EntityStatType.getAssetMap().getIndex("Chakra");
        } catch (Exception e) {
            this.getLogger().at(Level.WARNING).log("Não foi possível registrar o índice do Stat 'Chakra'. Verifique seus assets.", e);
        }

        System.out.println("===========================================");
        System.out.println("         NARUTO MOD INITIALIZED            ");
        System.out.println("===========================================");
    }

    public static <T extends JsonAssetWithMap<String, DefaultAssetMap<String, T>>> Supplier<AssetStore<String, T, DefaultAssetMap<String, T>>> createAssetStore(final Class<T> clazz) {
        return new Supplier<AssetStore<String, T, DefaultAssetMap<String, T>>>() {
            AssetStore<String, T, DefaultAssetMap<String, T>> value;

            public AssetStore<String, T, DefaultAssetMap<String, T>> get() {
                if (this.value == null) {
                    this.value = AssetRegistry.getAssetStore(clazz);
                }

                return this.value;
            }
        };
    }
}