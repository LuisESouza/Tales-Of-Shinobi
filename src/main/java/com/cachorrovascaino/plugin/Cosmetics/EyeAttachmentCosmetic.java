package com.cachorrovascaino.plugin.Cosmetics;

import com.cachorrovascaino.plugin.Cosmetics.appearance.ModelAppearance;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Cosmético de "attachment" (peça anexada ao personagem) - usado para os olhos
 * de Dōjutsu (Sharingan/Byakugan), entre outras peças que precisam ser
 * anexadas ao esqueleto do jogador em vez de substituir o corpo inteiro.
 * <p>
 * Diferente da {@link PlayerModelCosmetic} (que guardava só strings soltas de
 * model/texture), esta classe usa {@link ModelAppearance}, que valida o model
 * e a texture como {@code MODEL_CHARACTER_ATTACHMENT}/{@code TEXTURE_CHARACTER_ATTACHMENT}
 * - a mesma categoria de asset que o Hytale usa nativamente pra peças anexadas.
 * Sem essa validação/categorização, o attachment é criado mas o engine não
 * sabe renderizá-lo (era exatamente o bug que estávamos vendo).
 */
public class EyeAttachmentCosmetic extends CosmeticAsset {

    public static final BuilderCodec<EyeAttachmentCosmetic> CODEC =
            BuilderCodec.builder(EyeAttachmentCosmetic.class, EyeAttachmentCosmetic::new, CosmeticAsset.ABSTRACT_CODEC)
                    .append(new KeyedCodec<>("Appearance", ModelAppearance.CODEC, true),
                            (t, value) -> t.appearance = value, (t) -> t.appearance)
                    .add()
                    .build();

    private ModelAppearance appearance;

    private EyeAttachmentCosmetic() {
    }

    public EyeAttachmentCosmetic(String id, String cosmeticSlotId, String[] hiddenCosmeticSlots, ModelAppearance appearance) {
        super(id, cosmeticSlotId, hiddenCosmeticSlots);
        this.appearance = appearance;
    }

    public ModelAppearance getAppearance() {
        return this.appearance;
    }

    public String getModel() {
        return this.appearance.getModel(null);
    }

    public String getTexture() {
        return this.appearance.getTextureConfig(null).getTexture(null);
    }
}