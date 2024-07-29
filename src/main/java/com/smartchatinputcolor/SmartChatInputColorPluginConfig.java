package com.smartchatinputcolor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SmartChatInputColorPluginConfig.GROUP)
public interface SmartChatInputColorPluginConfig extends Config {
    String GROUP = "smartchatinputcolor";

    @ConfigItem(
        keyName = "slashSwapperBug",
        name = "Slash Swapper bug",
        description = "Assume that messages starting with /// are sent to friends chat "
            + "if Slash Swapper is active without swap guest chat config enabled.",
        position = 1)
    default boolean slashSwapperBug() {
        return true;
    }
}
