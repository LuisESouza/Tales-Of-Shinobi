package com.cachorrovascaino.plugin.Utils;

import com.cachorrovascaino.plugin.Cosmetics.CosmeticAsset;
import com.cachorrovascaino.plugin.Cosmetics.EyeAttachmentCosmetic;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPartTexture;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EyesUtils {

    /**
     * Atualiza os olhos do jogador aceitando tanto cosméticos customizados do mod
     * quanto o fallback vanilla sem quebrar o ECS.
     */
    public void updateHytalePlayerEyes(PlayerRef playerRef, String eyesId, String eyesColor) {
        if (playerRef == null) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;
        Store<EntityStore> store = ref.getStore();

        ModelComponent modelComponent = store.getComponent(ref, ModelComponent.getComponentType());
        if (modelComponent == null || modelComponent.getModel() == null) return;

        Model currentModel = modelComponent.getModel();

        CosmeticAsset customAsset = null;
        try {
            DefaultAssetMap<String, CosmeticAsset> map = CosmeticAsset.getAssetMap();
            if (map != null && eyesId != null) {
                customAsset = map.getAssetMap().get(eyesId);
            }
        } catch (Exception ignored) {}

        Model newModel = null;

        if (customAsset instanceof EyeAttachmentCosmetic customEye) {
            PlayerSkinComponent skinCompForBase = store.getComponent(ref, PlayerSkinComponent.getComponentType());
            Model freshBaseModel = currentModel;
            List<ModelAttachment> nativeAttachments = new ArrayList<>();

            if (skinCompForBase != null) {
                try {
                    freshBaseModel = CosmeticsModule.get().createModel(skinCompForBase.getPlayerSkin());
                } catch (Exception e) {
                    System.err.println("[ClanManager]error CosmeticsModule: " + e.getMessage());
                }
                nativeAttachments = resolveNativeAttachments(skinCompForBase.getPlayerSkin());
            }

            String eyeModel = customEye.getModel();
            String eyeTexture = customEye.getTexture();

            if(freshBaseModel == null){return;}

            ModelAttachment eyeAttachment = new ModelAttachment(eyeModel, eyeTexture, freshBaseModel.getGradientSet(), null, 1.0);

            List<ModelAttachment> allAttachments = new ArrayList<>(nativeAttachments);
            allAttachments.add(eyeAttachment);

            String modelAssetId = "Dojutsu_" + playerRef.getUuid() + "_" + freshBaseModel.getModelAssetId();
            newModel = buildFinalModel(freshBaseModel, modelAssetId, allAttachments.toArray(new ModelAttachment[0]));
        }
        else {
            PlayerSkinComponent skinComp = store.getComponent(ref, PlayerSkinComponent.getComponentType());
            if (skinComp != null) {
                PlayerSkin protocolSkin = skinComp.getPlayerSkin();
                String targetAsset = (eyesId != null && !eyesId.isEmpty()) ? eyesId : "Plain_Eyes";
                String finalEyeString = (eyesColor != null && !eyesColor.trim().isEmpty())
                        ? targetAsset + "." + eyesColor.trim()
                        : targetAsset;

                String previousEyes = protocolSkin.eyes;
                protocolSkin.eyes = finalEyeString;

                try {
                    newModel = CosmeticsModule.get().createModel(protocolSkin);
                    skinComp.setNetworkOutdated();
                } catch (Exception e) {
                    protocolSkin.eyes = "Medium_Eyes.Brown";
                    try {
                        newModel = CosmeticsModule.get().createModel(protocolSkin);
                        skinComp.setNetworkOutdated();
                    } catch (Exception ignored) {
                        protocolSkin.eyes = previousEyes;
                    }
                }
            }
        }

        if (newModel != null) {
            store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));

            PlayerSkinComponent skinComp = store.getComponent(ref, PlayerSkinComponent.getComponentType());
            if (skinComp != null) {
                skinComp.setNetworkOutdated();
            }

            System.out.println("[ClanManager] ModelComponent atualizado no ECS com sucesso para: " + eyesId);
        } else {
            System.err.println("[ClanManager] Falha ao gerar o novo Model para o jogador.");
        }
    }


    private Model buildFinalModel(Model baseModel, String modelAssetId, ModelAttachment[] attachments) {
        return new Model(
                modelAssetId,
                baseModel.getScale(),
                baseModel.getRandomAttachmentIds(),
                attachments,
                baseModel.getBoundingBox(),
                baseModel.getModel(),
                baseModel.getTexture(),
                baseModel.getGradientSet(),
                baseModel.getGradientId(),
                baseModel.getEyeHeight(),
                baseModel.getCrouchOffset(),
                baseModel.getSittingOffset(),
                baseModel.getSleepingOffset(),
                baseModel.getAnimationSetMap(),
                baseModel.getCamera(),
                baseModel.getLight(),
                baseModel.getParticles(),
                baseModel.getTrails(),
                baseModel.getPhysicsValues(),
                baseModel.getDetailBoxes(),
                baseModel.getPhobia(),
                baseModel.getPhobiaModelAssetId()
        );
    }

    /**
     * Reconstrói cabelo, sobrancelha, pelo facial, calça, sobrecalça, camisa, jaqueta,
     * tênis, luva, capa e acessórios (cabeça/rosto/orelha) como ModelAttachment explícitos,
     * a partir dos campos nativos do PlayerSkin - replicando exatamente a lógica de
     * HytaleCosmetic.createAttachment() do Wardrobe, já que CosmeticsModule.createModel()
     * NÃO faz isso sozinho.
     */
    private List<ModelAttachment> resolveNativeAttachments(PlayerSkin protocolSkin) {
        List<ModelAttachment> attachments = new ArrayList<>();
        CosmeticRegistry registry = CosmeticsModule.get().getRegistry();

        addIfPresent(attachments, resolveNativeAttachment(registry.getHaircuts(), protocolSkin.haircut));
        addIfPresent(attachments, resolveNativeAttachment(registry.getEyebrows(), protocolSkin.eyebrows));
        addIfPresent(attachments, resolveNativeAttachment(registry.getFacialHairs(), protocolSkin.facialHair));
        addIfPresent(attachments, resolveNativeAttachment(registry.getPants(), protocolSkin.pants));
        addIfPresent(attachments, resolveNativeAttachment(registry.getOverpants(), protocolSkin.overpants));
        addIfPresent(attachments, resolveNativeAttachment(registry.getUndertops(), protocolSkin.undertop));
        addIfPresent(attachments, resolveNativeAttachment(registry.getOvertops(), protocolSkin.overtop));
        addIfPresent(attachments, resolveNativeAttachment(registry.getShoes(), protocolSkin.shoes));
        addIfPresent(attachments, resolveNativeAttachment(registry.getGloves(), protocolSkin.gloves));
        addIfPresent(attachments, resolveNativeAttachment(registry.getHeadAccessories(), protocolSkin.headAccessory));
        addIfPresent(attachments, resolveNativeAttachment(registry.getFaceAccessories(), protocolSkin.faceAccessory));
        addIfPresent(attachments, resolveNativeAttachment(registry.getEarAccessories(), protocolSkin.earAccessory));
        addIfPresent(attachments, resolveNativeAttachment(registry.getCapes(), protocolSkin.cape));
        addIfPresent(attachments, resolveNativeAttachment(registry.getUnderwear(), protocolSkin.underwear));
        addIfPresent(attachments, resolveNativeAttachment(registry.getSkinFeatures(), protocolSkin.skinFeature));

        String bodyTextureId = extractTextureId(protocolSkin.bodyCharacteristic);
        addIfPresent(attachments, resolveAttachmentFromParts(registry.getFaces(), protocolSkin.face, bodyTextureId, null));
        addIfPresent(attachments, resolveAttachmentFromParts(registry.getEars(), protocolSkin.ears, bodyTextureId, null));
        addIfPresent(attachments, resolveAttachmentFromParts(registry.getMouths(), protocolSkin.mouth, bodyTextureId, null));

        return attachments;
    }

    /** Extrai o textureId (segunda parte, ex: "cor" em "id.cor") de um valor de skin com notação por ponto. */
    private String extractTextureId(String skinValue) {
        if (skinValue == null || skinValue.isEmpty()) return null;
        String[] parts = skinValue.split("\\.");
        return parts.length > 1 && !parts[1].isEmpty() ? parts[1] : null;
    }

    private void addIfPresent(List<ModelAttachment> list, ModelAttachment attachment) {
        if (attachment != null) {
            list.add(attachment);
        }
    }

    /**
     * Resolve um único campo do PlayerSkin (ex: "Pants_A.Blue" ou "Pants_A.Blue.Style2")
     * num ModelAttachment, replicando exatamente HytaleCosmetic.createAttachment():
     * - idParts[0] = id da peça no registro de cosméticos
     * - idParts[1] = id da textura/cor
     * - idParts[2] = id do "option"/variante de estilo (opcional)
     */
    private ModelAttachment resolveNativeAttachment(Map<String, PlayerSkinPart> registryMap, String skinValue) {
        if (skinValue == null || skinValue.isEmpty() || registryMap == null) return null;

        String[] idParts = skinValue.split("\\.");
        String id = idParts[0];
        String textureId = idParts.length > 1 && !idParts[1].isEmpty() ? idParts[1] : null;
        String optionId = idParts.length > 2 && !idParts[2].isEmpty() ? idParts[2] : null;

        return resolveAttachmentFromParts(registryMap, id, textureId, optionId);
    }

    /** Núcleo da resolução, já com id/textureId/optionId separados (usado tanto pro caso normal quanto pro caso especial de Face/Orelha/Boca). */
    private ModelAttachment resolveAttachmentFromParts(Map<String, PlayerSkinPart> registryMap, String id, String textureId, String optionId) {
        if (id == null || id.isEmpty() || registryMap == null) return null;

        PlayerSkinPart part = registryMap.get(id);
        if (part == null) return null;

        String model;
        String texture;
        String gradientSet = null;
        String gradientId = null;

        Map<String, PlayerSkinPart.Variant> variants = part.getVariants();
        if (variants != null && !variants.isEmpty()) {
            PlayerSkinPart.Variant variant = variants.get(optionId);
            if (variant == null) return null;

            model = variant.getModel();
            if (variant.getTextures() != null) {
                PlayerSkinPartTexture partTexture = variant.getTextures().get(textureId);
                if (partTexture == null) return null;
                texture = partTexture.getTexture();
            } else {
                texture = variant.getGreyscaleTexture();
                gradientSet = part.getGradientSet();
                gradientId = textureId;
            }
        } else {
            model = part.getModel();
            if (part.getTextures() != null) {
                PlayerSkinPartTexture partTexture = part.getTextures().get(textureId);
                if (partTexture == null) return null;
                texture = partTexture.getTexture();
            } else {
                texture = part.getGreyscaleTexture();
                gradientSet = part.getGradientSet();
                gradientId = textureId;
            }
        }

        return new ModelAttachment(model, texture, gradientSet, gradientId, 1.0);
    }

    /**
     * Captura o olho REAL do jogador (id + cor) direto do PlayerSkinComponent, na primeira
     * vez que o Dōjutsu é ativado - só se ainda não tiver sido capturado.
     */
    public void captureOriginalEyesIfNeeded(PlayerRef playerRef, PlayerData playerData) {
        if (playerData.getOriginalEyesId() != null && !playerData.getOriginalEyesId().isEmpty()) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;
        Store<EntityStore> store = ref.getStore();

        PlayerSkinComponent skinComp = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        if (skinComp == null) return;

        String rawEyes = skinComp.getPlayerSkin().eyes;
        if (rawEyes == null || rawEyes.isEmpty()) return;

        String[] parts = rawEyes.split("\\.");
        String id = parts[0];
        String color = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : "";

        playerData.setOriginalEyesId(id);
        playerData.setOriginalEyesColor(color);

        System.out.println("[ClanManager] Olho original capturado para " + playerRef.getUuid()
                + ": id=" + id + ", cor=" + color);
    }
}
