package org.blackum.blackaddons.core.util;

import org.blackum.blackaddons.Blackaddons;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.DyedItemColor;
import org.blackum.blackaddons.core.model.SkyblockItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import net.minecraft.world.item.component.ResolvableProfile;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.google.common.collect.ImmutableMultimap;
import java.util.Optional;
import java.util.Base64;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ItemDeserializer {

    public static List<SkyblockItem> deserializeList(String base64) {
        List<SkyblockItem> items = new ArrayList<>();
        CompoundTag root = NbtUtils.decodeBase64Compressed(base64);
        if (root == null || !root.contains("i"))
            return items;

        Object listObj = root.get("i");
        ListTag list = null;
        if (listObj instanceof Optional<?> opt) {
            Object inner = opt.orElse(null);
            if (inner instanceof ListTag lt)
                list = lt;
        } else if (listObj instanceof ListTag lt) {
            list = lt;
        }

        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                Object compObj = list.getCompound(i);
                CompoundTag comp = null;
                if (compObj instanceof Optional<?> opt) {
                    Object inner = opt.orElse(null);
                    if (inner instanceof CompoundTag ct)
                        comp = ct;
                } else if (compObj instanceof CompoundTag ct) {
                    comp = ct;
                }
                if (comp != null) {
                    items.add(deserialize(comp));
                }
            }
        }
        return items;
    }

    public static SkyblockItem deserialize(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty() || !nbt.contains("id")) {
            return new SkyblockItem(ItemStack.EMPTY, null, null);
        }

        CompoundTag tag = unwrapCompound(nbt.getCompound("tag"));
        CompoundTag extra = unwrapCompound(tag.getCompound("ExtraAttributes"));
        String skyblockId = unwrapString(extra.getString("id"));

        int id = unwrapShort(nbt.getShort("id"));
        int damage = unwrapShort(nbt.getShort("Damage"));
        int count = unwrapByte(nbt.getByte("Count"));

        Item baseItem = LegacyItemResolver.resolve(id, damage);
        if (baseItem == Items.BARRIER && id != 0) {
            Blackaddons.LOGGER.warn("Item with ID {} ({}) resolved to BARRIER. NBT: {}", id, skyblockId, nbt);
        }

        ItemStack stack = new ItemStack(baseItem);
        stack.setCount(count > 0 ? count : 1);

        if (skyblockId != null && !skyblockId.isEmpty()) {
            if (id == 397 && tag.contains("SkullOwner")) {
                CompoundTag skullOwner = unwrapCompound(tag.getCompound("SkullOwner"));
                if (!skullOwner.contains("Properties")) {
                    Blackaddons.LOGGER.warn("Skull item {} missing Properties in SkullOwner. NBT: {}", skyblockId, tag);
                }
            }

            if (tag.contains("display")) {
                CompoundTag display = unwrapCompound(tag.getCompound("display"));
                if (display.contains("Name")) {
                    stack.set(DataComponents.CUSTOM_NAME, Component.literal(unwrapString(display.getString("Name"))));
                }

                display.getInt("color").ifPresent(color -> {
                    stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
                });

                if (extra.contains("color")) {
                    extra.getInt("color").ifPresent(color -> {
                        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
                    });
                    extra.getString("color").ifPresent(colorStr -> {
                        int color = parseColorString(colorStr);
                        if (color != -1 && color != 0) {
                            stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
                        }
                    });
                }

                if (display.contains("Lore")) {
                    Object loreObj = display.getList("Lore");
                    ListTag loreList = null;
                    if (loreObj instanceof Optional<?> opt) {
                        Object inner = opt.orElse(null);
                        if (inner instanceof ListTag lt)
                            loreList = lt;
                    } else if (loreObj instanceof ListTag lt) {
                        loreList = lt;
                    }

                    if (loreList != null) {
                        List<Component> lore = new ArrayList<>();
                        for (int i = 0; i < loreList.size(); i++) {
                            lore.add(Component.literal(unwrapString(loreList.getString(i))));
                        }
                        stack.set(DataComponents.LORE, new ItemLore(lore));
                    }
                }
            }
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(extra));

            if (id == 397 && tag.contains("SkullOwner")) {
                CompoundTag skullOwner = unwrapCompound(tag.getCompound("SkullOwner"));
                if (skullOwner.contains("Properties")) {
                    CompoundTag properties = unwrapCompound(skullOwner.getCompound("Properties"));
                    if (properties.contains("textures")) {
                        ListTag textures = unwrapList(properties.getList("textures"));
                        if (!textures.isEmpty()) {
                            Object firstObj = textures.getCompound(0);
                            CompoundTag firstTexture = unwrapCompound(firstObj);
                            String texture = unwrapString(firstTexture.getString("Value"));
                            texture = fixBase64Padding(texture);
                            if (isValidBase64(texture)) {
                                ImmutableMultimap.Builder<String, Property> builder = ImmutableMultimap.builder();
                                builder.put("textures", new Property("textures", texture));
                                GameProfile profile = new GameProfile(UUID.randomUUID(), "BlackAddonsItem");
                                GameProfile profileWithProps = new GameProfile(profile.id(), profile.name(),
                                        new PropertyMap(builder.build()));
                                stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profileWithProps));
                            } else {
                                Blackaddons.LOGGER.warn("Unrecoverable Base64 texture for item {}: {}", skyblockId,
                                        texture);
                            }
                        }
                    }
                }
            }
        }

        return new SkyblockItem(stack, skyblockId, null);
    }

    private static final Map<String, String> DEFAULT_PET_TEXTURES = new HashMap<>();
    // yes, i did it by hand, yes, i need therapy
    static {
        DEFAULT_PET_TEXTURES.put("HEDGEHOG", "5f5e835c116e8e200e2e06aa593cab8f1a9f8c40e7f005a9c76f12e24f4c6370");
        DEFAULT_PET_TEXTURES.put("BABY_YETI", "ef9f6a57d8f686fedd5185d178550913f48f91b57c9162d70d368b1fd9fe2b52");
        DEFAULT_PET_TEXTURES.put("MOOSHROOM_COW", "47f1bc3fa91cd86cf4ba7745586d67207b58e7cf27bdf7a717780843785bf9b5");
        DEFAULT_PET_TEXTURES.put("GOLDEN_DRAGON", "2e9f9b1fc014166cb46a093e5349b2bf6edd201b680d62e48dbf3af9b0459116");
        DEFAULT_PET_TEXTURES.put("ENDER_DRAGON", "aec3ff563290b13ff3bcc36898af7eaa988b6cc18dc254147f58374afe9b21b9");
        DEFAULT_PET_TEXTURES.put("PHOENIX", "66b1b59bc890c9c97527787dde20600c8b86f6b9912d51a6bfcdb0e4c2aa3c97");
        DEFAULT_PET_TEXTURES.put("SLUG", "7a79d0fd677b54530961117ef84adc206e2cc5045c1344d61d776bf8ac2fe1ba");
        DEFAULT_PET_TEXTURES.put("ARMADILLO", "c1eb6df4736ae24dd12a3d00f91e6e3aa7ade6bbefb0978afef2f0f92461018f");
        DEFAULT_PET_TEXTURES.put("BAT", "377ade8afb257086fb1861da43c8ae468b8eb0578fada84303858517ba17217e");
        DEFAULT_PET_TEXTURES.put("ENDERMAN", "9f31c9b5a2bfe7443645b4e7e8ea408b04d554a9200b00afd574ce924b555ecb");
        DEFAULT_PET_TEXTURES.put("ENDERMITE", "18ae7046da98dcb33f3ed42f1dc41d08ac8dfa5db3a3860de5b1b5c056804187");
        DEFAULT_PET_TEXTURES.put("GRIFFIN", "4c27e3cb52a64968e60c861ef1ab84e0a0cb5f07be103ac78da67761731f00c8");
        DEFAULT_PET_TEXTURES.put("GUARDIAN", "221025434045bda7025b3e514b316a4b770c6faa4ba9adb4be3809526db77f9d");
        DEFAULT_PET_TEXTURES.put("FLYING_FISH", "b0e2363c2d41a9d323ba625de8c0637063a36fe85a045de275a7b7739ded6051");
        DEFAULT_PET_TEXTURES.put("MITHRIL_GOLEM", "c1b2dfe8ed5dffc5b1687bc1c249c39de2d8a6c3d90305c95f6d1a1a330a0b1");
        DEFAULT_PET_TEXTURES.put("RABBIT", "63438555e899bd9a051a95dbea49eb2ecfa52a69dbba8998f3673819e277fdf5");
        DEFAULT_PET_TEXTURES.put("HERMIT_CRAB", "26629dfa3fdfef04054024e0156d5e19da5401b1911f59b4bd3982685fe54c2c");
        DEFAULT_PET_TEXTURES.put("SPIDER", "cd541541daaff50896cd258bdbdd4cf80c3ba816735726078bfe393927e57f1");
        DEFAULT_PET_TEXTURES.put("TARANTULA", "b81b5cf3a65bf9cab4bca55781721a86d567d9e106551647f35b715edf00a13f");
        DEFAULT_PET_TEXTURES.put("JERRY", "822d8e751c8f2fd4c8942c44bdb2f5ca4d8ae8e575ed3eb34c18a86e93b");
        DEFAULT_PET_TEXTURES.put("AMMONITE", "a074a7bd976fe6aba1624161793be547d54c835cf422243a851ba09d1e650553");
        DEFAULT_PET_TEXTURES.put("BLUE_WHALE", "9ee72d3435c183dbdda2290cc8fcc9054b6e4d6e8f5338db4f4c1d9a30854b5d");
        DEFAULT_PET_TEXTURES.put("EERIE", "c3af70c6ff76ba48f24ee8a2063a5b50bbfabf409f4795248a292f8289f47c98");
        DEFAULT_PET_TEXTURES.put("BAL", "c469ba2047122e0a2de3c7437ad3dd5d31f1ac2d27abde9f8841e1d92a8c5b75");
        DEFAULT_PET_TEXTURES.put("FROG", "45852a95928897746012988fbd5dbaa1b70b7a5fb65157016f4ff3f245374c08");
        DEFAULT_PET_TEXTURES.put("GIRAFFE", "2528ce6ad51fcb6e27c85566e70da351075185f184d910f4996e4311d4a1bfa5");
        DEFAULT_PET_TEXTURES.put("ELEPHANT", "4ef8efea450632d2753a38a23a4f1502a8a685f6e99bf69205ec04420fd64f6");
        DEFAULT_PET_TEXTURES.put("GOBLIN", "7309d8dc35a638a04b915a3b15a1452ceeae0d7ea42bcdadb21b03046987515c");
        DEFAULT_PET_TEXTURES.put("JELLYFISH", "64c9ad5400fa660cbd121d4415a33e07fdf20ecf212a6bd4e68a43c7f646f5c4");
        DEFAULT_PET_TEXTURES.put("MONKEY", "96e1836c73af42cf3c299fcb56d7342bf305f720be3c8cce2a0fae199794207a");
        DEFAULT_PET_TEXTURES.put("MOLE", "727baaafc09978d4bda73e16afdde85ec13b0f95ad989524c5fcaa717cf06b4a");
        DEFAULT_PET_TEXTURES.put("GRANDMA_WOLF", "4e794274c1bb197ad306540286a7aa952974f5661bccf2b725424f6ed79c7884");
        DEFAULT_PET_TEXTURES.put("OCELOT", "5657cd5c2989ff97570fec4ddcdc6926a68a3393250c1be1f0b114a1db1");
        DEFAULT_PET_TEXTURES.put("PENGUIN", "37534e97f36e5a8335928e171ec99608bee7fb16e260afb301025b3b17eeefc4");
        DEFAULT_PET_TEXTURES.put("GLACITE_GOLEM", "af132a6593876d3c377d503fd66eca3fb938743251f7b16a9870c60b7388c8a3");
        DEFAULT_PET_TEXTURES.put("MEGALODON", "a94ae433b301c7fb7c68cba625b0bd36b0b14190f20e34a7c8ee0d9de06d53b9");
        DEFAULT_PET_TEXTURES.put("LION", "38ff473bd52b4db2c06f1ac87fe1367bce7574fac330ffac7956229f82efba1");
        DEFAULT_PET_TEXTURES.put("PARROT", "5df4b3401a4d06ad66ac8b5c4d189618ae617f9c143071c8ac39a563cf4e4208");
        DEFAULT_PET_TEXTURES.put("PIG", "621668ef7cb79dd9c22ce3d1f3f4cb6e2559893b6df4a469514e667c16aa4");
        DEFAULT_PET_TEXTURES.put("REINDEER", "a2df65c6fd19a58bee38252192ac7ce2cf1dc8632c3547a9228b6b697240d098");
        DEFAULT_PET_TEXTURES.put("ROCK", "7df8aab57136df2296c7c6f969ff25d58116fe2ec59b96a85ba4927e1f6779e6");
        DEFAULT_PET_TEXTURES.put("SILVERFISH", "811a1173af3bead305e6339f555662e990d5faadb87e07299fa68ca828a6d2fb");
        DEFAULT_PET_TEXTURES.put("SNAIL", "50a9933a3b10489d38f6950c4e628bfcf9f7a27f8d84666f04f14d5374252972");
        DEFAULT_PET_TEXTURES.put("SQUID", "01433be242366af126da434b8735df1eb5b3cb2cede39145974e9c483607bac");
        DEFAULT_PET_TEXTURES.put("SHEEP", "64e22a46047d272e89a1cfa13e9734b7e12827e235c2012c1a95962874da0");
        DEFAULT_PET_TEXTURES.put("WOLF", "81483105ab8c33f04c97c7493c9871c8a51cc49df6d9b5e23b26652094bf3030");
        DEFAULT_PET_TEXTURES.put("BEE", "7e941987e825a24ea7baafab9819344b6c247c75c54a691987cd296bc163c263");
        DEFAULT_PET_TEXTURES.put("MOSQUITO", "52a9fe05bc663efcd12e56a3ccc5ec035bf577b78708548b6f4ffcf1d30eccfe");
        DEFAULT_PET_TEXTURES.put("SPINOSAURUS", "d3c9d479471a2f13f22548315159591720992e70c920fef83a901b7186720e3c");
        DEFAULT_PET_TEXTURES.put("DOLPHIN", "1415d2c543e34bb88ede94d79b9427691fc9be72daad8831a9ef297180546e18");
        DEFAULT_PET_TEXTURES.put("SPIRIT", "8d9ccc670677d0cebaad4058d6aaf9acfab09abea5d86379a059902f2fe22655");
        DEFAULT_PET_TEXTURES.put("OWL", "da3216da54e7368fb40b721239ad95e07ef4f97d93f1c42ff319bab9a53882af");
        DEFAULT_PET_TEXTURES.put("HORSE", "36fcd3ec3bc84bafb4123ea479471f9d2f42d8fb9c5f11cf5f4e0d93226");
        DEFAULT_PET_TEXTURES.put("SKELETON", "fca445749251bdd898fb83f667844e38a1dff79a1529f79a42447a0599310ea4");
        DEFAULT_PET_TEXTURES.put("RAT", "a8abb471db0ab78703011979dc8b40798a941f3a4dec3ec61cbeec2af8cffe8");
        DEFAULT_PET_TEXTURES.put("TIGER", "fc42638744922b5fcf62cd9bf27eeab91b2e72d6c70e86cc5aa3883993e9d84");
        DEFAULT_PET_TEXTURES.put("CHICKEN", "c6e573262297df2bfa81562498a22a90478e5c96e80d05885d87084de24d2b18");
        DEFAULT_PET_TEXTURES.put("GOLEM", "89091d79ea0f59ef7ef94d7bba6e5f17f2f7d4572c44f90f76c4819a714");
        DEFAULT_PET_TEXTURES.put("MAGMA_CUBE", "38957d5023c937c4c41aa2412d43410bda23cf79a9f6ab36b76fef2d7c429");
        DEFAULT_PET_TEXTURES.put("ANKYLOSAURUS", "c1aa836b9096c417903299a6c5ab41738c19648ac439fed4bcbe6c32605338dc");
        DEFAULT_PET_TEXTURES.put("ZOMBIE", "56fc854bb84cf4b7697297973e02b79bc10698460b51a639c60e5e417734e11");
        DEFAULT_PET_TEXTURES.put("HOUND", "1290ad550c376d0108b5be22dd5a4082a8621cf9d40b2807156ca1950d246207");
        DEFAULT_PET_TEXTURES.put("PIGMAN", "d15afad53d1726fae86b3fb11ba01e51112b1055e8e5af8b7df86ef956f1d4a1");
        DEFAULT_PET_TEXTURES.put("MAMMOTH", "6b10715732cd1fd49fa1b6187947c307dd4687105cf033840607f9d6234743ad");
        DEFAULT_PET_TEXTURES.put("SKELETON_HORSE", "7ca9d8c4428e15fdc1f03c28afed48ebec1cea30b3e09a812a9768f0279c5c");
        DEFAULT_PET_TEXTURES.put("SNOWMAN", "609e161bdc325c71572a548a79bb15481c924d63e4fb821379d5dd6c8929f39f");
        DEFAULT_PET_TEXTURES.put("TYRANNOSAURUS", "93f28ec96df59c67e9d2fc2e7e3d055fa31646e4111add9fe26a692801964126");
        DEFAULT_PET_TEXTURES.put("TURTLE", "242f86b51c8992b3758e85fe35bb2a20399ad4685095466e7feb54c6902fad8e");
        DEFAULT_PET_TEXTURES.put("WITHER_SKELETON", "d50e972d26c3dbc4e1663aaf583388ff321e958d195f8a465b71a596a736fe9c");
        DEFAULT_PET_TEXTURES.put("KUUDRA", "1f0239fb498e5907ede12ab32629ee95f0064574a9ffdff9fc3a1c8e2ec17587");
        DEFAULT_PET_TEXTURES.put("RIFT_FERRET", "b6b11399448260185da1d17e54c984515faab6d8585f00972451ec2b43d46f94");
        DEFAULT_PET_TEXTURES.put("WISP", "1d8ad9936d758c5ea30b0b7cc7c67c2bfcea829ecf2425c0b50fc92a26ae23d0");
        DEFAULT_PET_TEXTURES.put("BLACK_CAT", "8282b5a9bbe2cd3223724023cd4f6ad413f5bb9e0edef81700b8afc3072d04a5");
        DEFAULT_PET_TEXTURES.put("JADE_DRAGON", "4099589796de185787ab92c3066d0d0af832ffad7153a42bb2e2d23598e7ea60");
        DEFAULT_PET_TEXTURES.put("ROSE_DRAGON", "9b7c3de075a2bb238ef51431206b10d586cb2a5b1cc41fe851cc5f0b02d357c7");
        DEFAULT_PET_TEXTURES.put("SCATHA", "df03ad96092f3f789902436709cdf69de6b727c121b3c2daef9ffa1ccaed186c");
        DEFAULT_PET_TEXTURES.put("PRECURSOR_DRONE", "51aae288ab9d5354fda4e395a36421bd261a9b6213dbb786d7ab39465e479ce9");
        DEFAULT_PET_TEXTURES.put("BLAZE", "b78ef2e4cf2c41a2d14bfde9caff10219f5b1bf5b35a49eb51c6467882cb5f0");
        DEFAULT_PET_TEXTURES.put("CROW", "c31e7e1c614639bbc95a0d81ff7e5f7f606d9b33115cf2bf2a40cdcbbb531324");
        DEFAULT_PET_TEXTURES.put("GHOUL", "1e1109bbd1f82c182363ed53b624b688924447f9f68b929674ad37f3cef762a8");
    }

    public static SkyblockItem deserializePet(JsonObject pet) {
        if (pet == null)
            return new SkyblockItem(ItemStack.EMPTY, null, null);

        String type = pet.has("type") ? pet.get("type").getAsString() : "UNKNOWN";
        String rarity = pet.has("tier") ? pet.get("tier").getAsString() : "COMMON";
        String name = pet.has("display_name") ? pet.get("display_name").getAsString() : formatPetName(type);
        long exp = pet.has("exp") ? pet.get("exp").getAsLong() : 0;
        int level = PetUtils.getLevel(type, rarity, exp);
        String rarityCode = PetUtils.getRarityCode(rarity);
        String formattedName = String.format("§7[Lvl %d] %s%s", level, rarityCode, name);

        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(formattedName));

        if (pet.has("lore")) {
            List<Component> lore = new ArrayList<>();
            for (JsonElement line : pet.getAsJsonArray("lore")) {
                lore.add(Component.literal(line.getAsString()));
            }
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }

        String texture = null;
        if (pet.has("texture") && !pet.get("texture").isJsonNull() && !pet.get("texture").getAsString().isEmpty()) {
            texture = pet.get("texture").getAsString();
        } else if (pet.has("skin") && !pet.get("skin").isJsonNull() && !pet.get("skin").getAsString().isEmpty()) {
        }

        if (texture == null) {
            String hash = DEFAULT_PET_TEXTURES.get(type.toUpperCase());
            if (hash != null) {
                texture = Base64.getEncoder().encodeToString(
                        String.format(
                                "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/%s\"}}}",
                                hash).getBytes());
            }
        }

        if (texture != null) {
            texture = fixBase64Padding(texture);
            if (isValidBase64(texture)) {
                ImmutableMultimap.Builder<String, Property> builder = ImmutableMultimap.builder();
                builder.put("textures", new Property("textures", texture));
                GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("BlackAddonsPet:" + type).getBytes()),
                        "BlackAddonsPet");
                GameProfile profileWithProps = new GameProfile(profile.id(), profile.name(),
                        new PropertyMap(builder.build()));
                stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profileWithProps));
            }
        }

        return new SkyblockItem(stack, "PET_" + type, rarity, String.valueOf(level), PetUtils.getRarityColor(rarity),
                pet);
    }

    private static String formatPetName(String type) {
        if (type == null || type.isEmpty())
            return "Unknown Pet";
        String[] parts = type.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static CompoundTag unwrapCompound(Object obj) {
        if (obj instanceof Optional<?> opt) {
            Object inner = opt.orElse(null);
            if (inner instanceof CompoundTag comp)
                return comp;
        }
        if (obj instanceof CompoundTag comp)
            return comp;
        return new CompoundTag();
    }

    private static ListTag unwrapList(Object obj) {
        if (obj instanceof Optional<?> opt) {
            Object inner = opt.orElse(null);
            if (inner instanceof ListTag list)
                return list;
        }
        if (obj instanceof ListTag list)
            return list;
        return new ListTag();
    }

    private static short unwrapShort(Object obj) {
        if (obj instanceof Optional<?> opt) {
            Object inner = opt.orElse(null);
            if (inner instanceof Number n)
                return n.shortValue();
        }
        if (obj instanceof Number n)
            return n.shortValue();
        return 0;
    }

    private static byte unwrapByte(Object obj) {
        if (obj instanceof Optional<?> opt) {
            Object inner = opt.orElse(null);
            if (inner instanceof Number n)
                return n.byteValue();
        }
        if (obj instanceof Number n)
            return n.byteValue();
        return 0;
    }

    private static String unwrapString(Object obj) {
        if (obj instanceof Optional<?> opt) {
            obj = opt.orElse(null);
        }
        if (obj instanceof String s)
            return s;
        if (obj != null)
            return obj.toString();
        return "";
    }

    private static int parseColorString(String s) {
        if (s == null || s.isEmpty())
            return -1;
        try {
            if (s.contains(":")) {
                String[] split = s.split(":");
                if (split.length == 3) {
                    return (Integer.parseInt(split[0].trim()) << 16) | (Integer.parseInt(split[1].trim()) << 8)
                            | Integer.parseInt(split[2].trim());
                }
            }
            if (s.contains(",")) {
                String[] split = s.split(",");
                if (split.length == 3) {
                    return (Integer.parseInt(split[0].trim()) << 16) | (Integer.parseInt(split[1].trim()) << 8)
                            | Integer.parseInt(split[2].trim());
                }
            }
            return Integer.parseInt(s.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String fixBase64Padding(String s) {
        if (s == null || s.isEmpty())
            return s;
        String cleaned = s.replaceAll("[^a-zA-Z0-9+/]", "");
        int pad = (4 - (cleaned.length() % 4)) % 4;
        return cleaned + "=".repeat(pad);
    }

    private static boolean isValidBase64(String s) {
        if (s == null || s.isEmpty())
            return false;
        try {
            Base64.getDecoder().decode(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
