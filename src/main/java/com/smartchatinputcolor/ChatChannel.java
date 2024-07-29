package com.smartchatinputcolor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.VarPlayer;
import net.runelite.api.annotations.Varp;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Getter
enum ChatChannel {
    PUBLIC(
        "PublicChat",
        VarPlayer.SETTINGS_TRANSPARENT_CHAT_PUBLIC,
        VarPlayer.SETTINGS_OPAQUE_CHAT_PUBLIC,
        0x9090FF,
        0x0000FF,
        Pattern.compile("^/(@p|p ).*"),
        0
    ),
    FRIEND(
        "ClanChatMessage",
        VarPlayer.SETTINGS_TRANSPARENT_CHAT_FRIEND,
        VarPlayer.SETTINGS_OPAQUE_CHAT_FRIEND,
        0xEF5050,
        0x7F0000,
        Pattern.compile("^/(@?f).*"),
        1
    ),
    CLAN(
        "ClanMessage",
        VarPlayer.SETTINGS_TRANSPARENT_CHAT_CLAN,
        VarPlayer.SETTINGS_OPAQUE_CHAT_CLAN,
        0x7F0000,
        0x7F0000,
        Pattern.compile("^/(@c|c ).*"),
        2
    ),
    GUEST(
        "ClanGuestMessage",
        VarPlayer.SETTINGS_TRANSPARENT_CHAT_GUEST_CLAN,
        VarPlayer.SETTINGS_OPAQUE_CHAT_GUEST_CLAN,
        0x7F0000,
        0x7F0000,
        Pattern.compile("^/(@gc|gc ).*"),
        3
    ),
    GIM(
        null,
        VarPlayer.SETTINGS_TRANSPARENT_CHAT_IRON_GROUP_CHAT,
        VarPlayer.SETTINGS_OPAQUE_CHAT_IRON_GROUP_CHAT,
        0x7F0000,
        0x7F0000,
        Pattern.compile("^/(@g[^c]|g ).*"),
        4
    );

    private final String colorConfigKey;
    @Getter(onMethod_ = {@Varp})
    private final @Varp int transparentVarpId;
    @Getter(onMethod_ = {@Varp})
    private final @Varp int opaqueVarpId;
    private final int transparentDefaultRgb;
    private final int opaqueDefaultRgb;

    @Getter(AccessLevel.NONE)
    private final Pattern prefixRegex;
    @Getter(AccessLevel.NONE)
    private final int defaultSlashPrefixCount;

    private final static Map<Integer, ChatChannel> slashPrefixMap = new HashMap<>();

    /**
     * Set the slash prefixes to their default values
     */
    public static void useDefaultSlashPrefixes() {
        slashPrefixMap.clear();
        for (ChatChannel channel : ChatChannel.values()) {
            slashPrefixMap.put(channel.defaultSlashPrefixCount, channel);
        }
        slashPrefixMap.remove(PUBLIC.defaultSlashPrefixCount);
    }

    /**
     * Configure slash prefixes when Slash Swapper is enabled
     *
     * @param guestChatConfig Whether the guest chat config is enabled on Slash Swapper
     */
    public static void useSlashSwapperPrefixes(boolean guestChatConfig) {
        slashPrefixMap.clear();
        slashPrefixMap.put(2, FRIEND);
        slashPrefixMap.put(4, GIM);

        if (guestChatConfig) {
            slashPrefixMap.put(3, CLAN);
            slashPrefixMap.put(1, GUEST);
            return;
        }

        slashPrefixMap.put(1, CLAN);
        slashPrefixMap.put(3, GUEST);
    }

    @Nullable
    public static ChatChannel getBySlashPrefix(String text) {
        int slashCount = 0;
        while (slashCount < text.length() && text.charAt(slashCount) == '/') {
            slashCount++;
        }

        return slashPrefixMap.get(Math.min(slashCount, 4));
    }

    /**
     * Get the chat channel whose slash prefix has the given number of slashes
     *
     * @param count Number of slashes
     * @return Chat channel whose slash prefix has the given number of slashes
     */
    @Nullable
    public static ChatChannel getBySlashCount(int count) {
        return slashPrefixMap.get(count);
    }

    /**
     * Check if the input text matches this channel's prefix regex
     *
     * @param text Input text
     * @return Whether the text matches this channel's prefix regex
     */
    public boolean matchesPrefixRegex(String text) {
        return prefixRegex.matcher(text).matches();
    }
}
