package com.cachorrovascaino.plugin.Manager;

import com.cachorrovascaino.plugin.Data.PlayerData;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final Path pluginDataFolder;
    private final File playersFolder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(Path pluginDataFolder) {
        this.pluginDataFolder = pluginDataFolder;
        this.playersFolder = pluginDataFolder.resolve("players").toFile();

        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }
    }

    /**
     * Carrega os dados do arquivo YAML ou cria um novo perfil se for novato.
     */
    public PlayerData loadPlayer(UUID playerUUID, String playerName) {
        File file = new File(playersFolder, playerUUID.toString() + ".yml");

        if (!file.exists()) {
            PlayerData newProfile = new PlayerData(playerName, playerUUID.toString());
            savePlayerFile(file, newProfile);
            cache.put(playerUUID, newProfile);
            return newProfile;
        }

        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor = new Constructor(PlayerData.class, loaderOptions);
        Yaml yaml = new Yaml(constructor);

        try (InputStream input = new FileInputStream(file)) {
            PlayerData data = yaml.load(input);
            cache.put(playerUUID, data);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtém os dados da RAM.
     */
    public PlayerData getPlayerData(UUID playerUUID) {
        return cache.get(playerUUID);
    }

    /**
     * Salva o perfil do cache de volta para o arquivo .yml.
     */
    public void savePlayer(UUID playerUUID) {
        PlayerData data = cache.get(playerUUID);
        if (data != null) {
            File file = new File(playersFolder, playerUUID.toString() + ".yml");
            savePlayerFile(file, data);
        }
    }

    /**
     * Salva e remove da memória quando o jogador sai.
     */
    public void unloadPlayer(UUID playerUUID) {
        savePlayer(playerUUID);
        cache.remove(playerUUID);
    }

    /**
     * Escreve o objeto no disco em formato YAML formatado sem tags de classe.
     */
    private void savePlayerFile(File file, PlayerData data) {
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Representer representer = new Representer(options);
            representer.addClassTag(PlayerData.class, org.yaml.snakeyaml.nodes.Tag.MAP);

            Yaml yaml = new Yaml(representer, options);

            try (FileWriter writer = new FileWriter(file)) {
                yaml.dump(data, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}