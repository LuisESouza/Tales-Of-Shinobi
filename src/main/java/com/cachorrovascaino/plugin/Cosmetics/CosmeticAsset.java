package com.cachorrovascaino.plugin.Cosmetics;

import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.function.Function;
import java.util.function.Supplier;

// Sem alterações nesta classe - nenhum bug encontrado aqui.
// Incluída apenas para você ter o pacote completo e consistente.
public abstract class CosmeticAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, CosmeticAsset>> {

    public static final BuilderCodec<CosmeticAsset> ABSTRACT_CODEC;
    public static final AssetCodecMapCodec<String, CosmeticAsset> CODEC;
    public static final Supplier<AssetStore<String, CosmeticAsset, DefaultAssetMap<String, CosmeticAsset>>> ASSET_STORE;

    public static final Function<CosmeticAsset, String> KEY_FUNCTION = CosmeticAsset::getId;

    private String id;
    private AssetExtraInfo.Data data;
    private String icon;
    private String cosmeticSlotId;
    private String[] hiddenCosmeticSlots = new String[0];

    public static DefaultAssetMap<String, CosmeticAsset> getAssetMap() {
        return (DefaultAssetMap<String, CosmeticAsset>) ASSET_STORE.get().getAssetMap();
    }

    protected CosmeticAsset() {
    }

    public CosmeticAsset(String id, String cosmeticSlotId, String[] hiddenCosmeticSlots) {
        this.id = id;
        this.cosmeticSlotId = cosmeticSlotId;
        this.hiddenCosmeticSlots = hiddenCosmeticSlots;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getIconPath() {
        return this.icon;
    }

    public String getCosmeticSlotId() {
        return this.cosmeticSlotId;
    }

    public String[] getHiddenCosmeticSlotIds() {
        return this.hiddenCosmeticSlots;
    }

    static {
        ABSTRACT_CODEC = BuilderCodec.abstractBuilder(CosmeticAsset.class)
                .append(new KeyedCodec<>("Icon", Codec.STRING), (t, value) -> t.icon = value, (t) -> t.icon)
                .add()
                .appendInherited(new KeyedCodec<>("CosmeticSlot", Codec.STRING), (c, value) -> c.cosmeticSlotId = value, (c) -> c.cosmeticSlotId, (c, p) -> c.cosmeticSlotId = p.cosmeticSlotId)
                .add()
                .appendInherited(new KeyedCodec<>("HiddenCosmeticSlots", Codec.STRING_ARRAY), (c, value) -> c.hiddenCosmeticSlots = value, (c) -> c.hiddenCosmeticSlots, (c, p) -> c.hiddenCosmeticSlots = p.hiddenCosmeticSlots)
                .add()
                .build();

        CODEC = new AssetCodecMapCodec<>(Codec.STRING, (t, k) -> t.id = k, (t) -> t.id, (t, data) -> t.data = data, (t) -> t.data, true);

        ASSET_STORE = Main.createAssetStore(CosmeticAsset.class);
    }
}