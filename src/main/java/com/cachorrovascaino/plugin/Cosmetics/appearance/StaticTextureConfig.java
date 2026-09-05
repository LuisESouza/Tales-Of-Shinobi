package com.cachorrovascaino.plugin.Cosmetics.appearance;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.Metadata;
import com.hypixel.hytale.codec.schema.metadata.ui.UIPropertyTitle;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;

public class StaticTextureConfig implements TextureConfig {

    public static final BuilderCodec<StaticTextureConfig> CODEC =
            BuilderCodec.builder(StaticTextureConfig.class, StaticTextureConfig::new)
                    .append(new KeyedCodec<>("Texture", Codec.STRING, true),
                            (t, value) -> t.texture = value, t -> t.texture)
                    .addValidator(CommonAssetValidator.TEXTURE_CHARACTER_ATTACHMENT)
                    .metadata(new UIPropertyTitle("Texture"))
                    .documentation("The texture to use.")
                    .add()
                    .build();

    private String texture;

    private StaticTextureConfig() {
    }

    public StaticTextureConfig(String texture) {
        this.texture = texture;
    }

    @Override
    public String getTexture(String variantId) {
        return this.texture;
    }

    @Override
    public String[] collectVariants() {
        return new String[0];
    }
}