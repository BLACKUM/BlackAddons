package org.blackum.blackaddons.core.util;

import org.blackum.blackaddons.feature.chat.ChatUtils;
import java.util.List;

public interface AnimatedTextColorAccessor {
    void ba$setAnimated(boolean animated);

    void ba$setStops(List<ChatUtils.ColorStop> stops);

    void ba$setSpeed(float speed);

    void ba$setOffset(float offset);

    boolean ba$isAnimated();
}
