package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.world.UpdateEditorWeatherOverride;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherUtils {

    private static final Map<UUID, String> activePlayerWeathers = new ConcurrentHashMap<>();

    /**
     * Aplica um override de clima genérico ao jogador.
     */
    public static void applyPlayerWeather(PlayerRef playerRef, PacketHandler packetHandler, String weatherId) {
        if (playerRef == null || packetHandler == null || weatherId == null || weatherId.isEmpty()) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;

        try {
            IndexedLookupTableAssetMap<String, Weather> assetMap = Weather.getAssetMap();
            if (assetMap == null) return;

            int weatherIndex = assetMap.getIndex(weatherId);
            if (weatherIndex < 0) {
                System.err.println("[WeatherUtils] Clima com ID '" + weatherId + "' não encontrado no AssetMap.");
                return;
            }

            UpdateEditorWeatherOverride packet = new UpdateEditorWeatherOverride(weatherIndex);
            packetHandler.write(packet);
            packetHandler.tryFlush();

            if (playerRef.getUuid() != null) {
                activePlayerWeathers.put(playerRef.getUuid(), weatherId);
            }

        } catch (Exception e) {
            System.err.println("[WeatherUtils] Erro ao aplicar clima '" + weatherId + "': " + e.getMessage());
        }
    }

    /**
     * Reseta o clima do jogador enviando o clima padrão (ex: "Sun").
     */
    public static void resetPlayerWeather(PlayerRef playerRef, PacketHandler packetHandler, String defaultWeatherId) {
        if (playerRef == null || packetHandler == null) return;

        try {
            if (playerRef.getUuid() != null) {
                activePlayerWeathers.remove(playerRef.getUuid());
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
            System.err.println("[WeatherUtils] Erro ao resetar clima do jogador: " + e.getMessage());
        }
    }

    /**
     * Verifica se o jogador está sob o efeito de um clima específico.
     */
    public static boolean hasWeatherActive(UUID playerUuid, String weatherId) {
        if (playerUuid == null || weatherId == null) return false;
        return weatherId.equals(activePlayerWeathers.get(playerUuid));
    }

    /**
     * Retorna o clima customizado ativo atualmente no jogador, se houver.
     */
    public static String getActiveWeather(UUID playerUuid) {
        if (playerUuid == null) return null;
        return activePlayerWeathers.get(playerUuid);
    }
}