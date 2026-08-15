package com.cachorrovascaino.plugin.Cosmetics;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class PlayerModelCosmetic extends CosmeticAsset {

    public static final BuilderCodec<PlayerModelCosmetic> CODEC;

    // Caminho/id da MALHA 3D (ex: "Models/Face/Eyes_Sharingan.model").
    // Se ficar null/vazio, o código que aplica o cosmético deve manter o modelo base do jogador
    // e trocar apenas a textura.
    private String modelPath;

    // Caminho/id da TEXTURA a aplicar sobre a malha (base ou customizada).
    private String texturePath;

    private PlayerModelCosmetic() {
    }

    public PlayerModelCosmetic(String id, String cosmeticSlotId, String[] hiddenCosmeticSlots, String modelPath, String texturePath) {
        super(id, cosmeticSlotId, hiddenCosmeticSlots);
        this.modelPath = modelPath;
        this.texturePath = texturePath;
    }

    /**
     * Construtor de conveniência para cosméticos que só trocam a textura
     * (mantendo a malha 3D original do jogador) — é o caso do Sharingan/Byakugan,
     * que reaproveitam o modelo de olho vanilla.
     */
    public PlayerModelCosmetic(String id, String cosmeticSlotId, String[] hiddenCosmeticSlots, String texturePath) {
        this(id, cosmeticSlotId, hiddenCosmeticSlots, null, texturePath);
    }

    public String getModelPath() {
        return this.modelPath;
    }

    public String getTexturePath() {
        return this.texturePath;
    }

    static {
        CODEC = BuilderCodec.builder(PlayerModelCosmetic.class, PlayerModelCosmetic::new, CosmeticAsset.ABSTRACT_CODEC)
                .append(
                        new KeyedCodec<>("ModelPath", Codec.STRING, true),
                        (t, value) -> t.modelPath = value,
                        (t) -> t.modelPath
                )
                .add()
                .append(
                        new KeyedCodec<>("TexturePath", Codec.STRING, false),
                        (t, value) -> t.texturePath = value,
                        (t) -> t.texturePath
                )
                .add()
                .build();
    }
}