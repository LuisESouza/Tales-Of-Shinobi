package com.cachorrovascaino.plugin.Cosmetics.appearance;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.Metadata;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDefaultCollapsedState;
import com.hypixel.hytale.codec.schema.metadata.ui.UIPropertyTitle;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;

public class ModelAppearance implements Appearance {

    public static final BuilderCodec<ModelAppearance> CODEC =
            BuilderCodec.builder(ModelAppearance.class, ModelAppearance::new)
                    .append(new KeyedCodec<>("Model", Codec.STRING, true),
                            (a, value) -> a.model = value, a -> a.model)
                    .addValidator(CommonAssetValidator.MODEL_CHARACTER_ATTACHMENT)
                    .addValidator(Validators.nonNull())
                    .metadata(new UIPropertyTitle("Model"))
                    .documentation("The model to display for this appearance.")
                    .add()
                    .append(new KeyedCodec<>("TextureConfig", StaticTextureConfig.CODEC),
                            (a, value) -> a.textureConfig = value, a -> (StaticTextureConfig) a.textureConfig)
                    .addValidator(Validators.nonNull())
                    .metadata(new UIPropertyTitle("Texture Configuration"))
                    .documentation("The Texture Configuration for this appearance.")
                    .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add()
                    .build();

    protected String model;
    protected float scale = 1.0f;
    protected TextureConfig textureConfig;

    private ModelAppearance() {
    }

    public ModelAppearance(String model, TextureConfig textureConfig, float scale) {
        this.model = model;
        this.textureConfig = textureConfig;
        this.scale = scale;
    }

    @Override
    public String getModel(String variantId) {
        return this.model;
    }

    @Override
    public float getScale(String variantId) {
        return this.scale;
    }

    @Override
    public TextureConfig getTextureConfig(String variantId) {
        return this.textureConfig;
    }

    @Override
    public String[] collectVariants() {
        return new String[0];
    }
}