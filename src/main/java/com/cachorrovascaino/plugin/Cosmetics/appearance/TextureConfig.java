package com.cachorrovascaino.plugin.Cosmetics.appearance;

public interface TextureConfig {

    String getTexture(String variantId);

    default String getGradientSet() {
        return null;
    }

    String[] collectVariants();
}