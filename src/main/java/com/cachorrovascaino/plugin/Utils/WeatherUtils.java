package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.world.UpdateEditorWeatherOverride;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WeatherUtils {

    private static final Set<UUID> activeSharinganPlayers = new HashSet<>();

    public void applyPlayerWeather(PlayerRef playerRef, PacketHandler packetHandler, String weatherId) {
        if (playerRef == null || packetHandler == null || weatherId == null || weatherId.isEmpty()) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;

        try {
            IndexedLookupTableAssetMap<String, Weather> assetMap = Weather.getAssetMap();
            if (assetMap == null) return;

            int weatherIndex = assetMap.getIndex(weatherId);
            if (weatherIndex < 0) {
                System.err.println("[ClanManager] Clima com ID '" + weatherId + "' não encontrado no AssetMap.");
                return;
            }

            UpdateEditorWeatherOverride packet = new UpdateEditorWeatherOverride(weatherIndex);
            packetHandler.write(packet);
            packetHandler.tryFlush();

            if (playerRef.getUuid() != null) {
                activeSharinganPlayers.add(playerRef.getUuid());
            }

        } catch (Exception e) {
            System.err.println("[ClanManager] Erro ao aplicar clima Sharingan: " + e.getMessage());
        }
    }

    /**
     * Reseta o clima enviando o índice de um clima normal existente no jogo (ex: "Sun").
     * NUNCA envie números negativos como -1.
     */
    public void resetPlayerWeather(PlayerRef playerRef, PacketHandler packetHandler, String defaultWeatherId) {
        if (playerRef == null || packetHandler == null) return;

        try {
            if (playerRef.getUuid() != null) {
                activeSharinganPlayers.remove(playerRef.getUuid());
            }

            IndexedLookupTableAssetMap<String, Weather> assetMap = Weather.getAssetMap();

            int defaultIndex = 0;
            if (assetMap != null && defaultWeatherId != null) {
                int indexFound = assetMap.getIndex(defaultWeatherId);
                if (indexFound >= 0) {
                    defaultIndex = indexFound;
                }
            }

            UpdateEditorWeatherOverride resetPacket = new UpdateEditorWeatherOverride(defaultIndex);
            packetHandler.write(resetPacket);
            packetHandler.tryFlush();

        } catch (Exception e) {
            System.err.println("[ClanManager] Erro ao resetar clima do jogador: " + e.getMessage());
        }
    }

    public static boolean hasSharinganActive(UUID playerUuid) {
        return activeSharinganPlayers.contains(playerUuid);
    }
}