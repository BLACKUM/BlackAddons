package org.blackum.blackaddons.mixin.core;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.feature.modhider.SpoofMode;

import net.minecraft.client.multiplayer.KnownPacksManager;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(KnownPacksManager.class)
public class KnownPacksManagerMixin {
    @WrapOperation(method = "trySelectingPacks", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private <V> V redirectSelectPacks(Map<KnownPack, V> instance, Object object, Operation<V> original) {
        KnownPack pack = (KnownPack) object;
        if (!pack.namespace().equalsIgnoreCase("fabric") ||
                ConfigManager.data.modHiderSpoofMode == SpoofMode.OFF) {
            return original.call(instance, pack);
        }
        if (ConfigManager.data.modHiderSpoofMode == SpoofMode.VANILLA) {
            return null;
        }
        if (ConfigManager.data.modHiderSpoofMode == SpoofMode.MODDED) {
            return original.call(instance, pack);
        }
        if (ConfigManager.data.modHiderSpoofMode == SpoofMode.CUSTOM) {
            for (String mod : ConfigManager.data.modHiderAllowedMods) {
                if (pack.id().toLowerCase().startsWith(mod.toLowerCase())) {
                    return original.call(instance, pack);
                }
            }
        }
        return null;
    }
}
