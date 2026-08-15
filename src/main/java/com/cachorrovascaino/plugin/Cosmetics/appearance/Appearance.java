package com.cachorrovascaino.plugin.Cosmetics.appearance;

// Portado do mod "Wardrobe" (dev.hardaway.wardrobe.api.cosmetic.appearance.Appearance),
// usado sob a licença aberta do projeto original (creditado na descrição do CurseForge).
//
// Interface simplificada: no mod original, a implementação concreta é escolhida via
// codec polimórfico (campo "Type" no JSON). Aqui simplificamos para usar diretamente
// ModelAppearance, então "Type" não é necessário no seu JSON.
public interface Appearance {

    String getModel(String variantId);

    TextureConfig getTextureConfig(String variantId);

    String[] collectVariants();

    float getScale(String variantId);
}