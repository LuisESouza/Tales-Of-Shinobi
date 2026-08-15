package com.cachorrovascaino.plugin.Cosmetics.appearance;

// Portado do mod "Wardrobe" (dev.hardaway.wardrobe.api.cosmetic.appearance.TextureConfig).
public interface TextureConfig {

    String getTexture(String variantId);

    default String getGradientSet() {
        return null;
    }

    String[] collectVariants();
}