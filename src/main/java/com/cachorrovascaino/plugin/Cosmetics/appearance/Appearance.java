package com.cachorrovascaino.plugin.Cosmetics.appearance;

public interface Appearance {

    String getModel(String variantId);

    TextureConfig getTextureConfig(String variantId);

    String[] collectVariants();

    float getScale(String variantId);
}