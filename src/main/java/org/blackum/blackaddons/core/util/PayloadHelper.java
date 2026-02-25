package org.blackum.blackaddons.core.util;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.blackum.blackaddons.Blackaddons;

public class PayloadHelper {

    private static final String ERROR_PAYLOAD_CREATION = "[ModHider] Failed to create payload via reflection constructor";

    public static CustomPacketPayload createRegisterPayload(CustomPacketPayload original, Set<String> channels) {
        try {
            Class<?> clazz = original.getClass();

            List<Object> idList = channels.stream()
                    .map(Identifier::tryParse)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                c.setAccessible(true);
                Class<?>[] paramTypes = c.getParameterTypes();

                if (c.getParameterCount() == 1) {
                    Class<?> paramType = paramTypes[0];
                    if (List.class.isAssignableFrom(paramType)) {
                        return (CustomPacketPayload) c.newInstance(idList);
                    } else if (Set.class.isAssignableFrom(paramType)) {
                        return (CustomPacketPayload) c.newInstance(new HashSet<>(idList));
                    } else if (Collection.class.isAssignableFrom(paramType)) {
                        return (CustomPacketPayload) c.newInstance(idList);
                    }
                } else if (c.getParameterCount() == 2) {
                    if (CustomPacketPayload.Type.class.isAssignableFrom(paramTypes[0])
                            && Collection.class.isAssignableFrom(paramTypes[1])) {
                        return (CustomPacketPayload) c.newInstance(original.type(), idList);
                    }
                }
            }
        } catch (Exception e) {
            Blackaddons.LOGGER.error(ERROR_PAYLOAD_CREATION, e);
        }
        return null;
    }
}
