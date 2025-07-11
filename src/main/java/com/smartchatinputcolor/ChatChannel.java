package com.smartchatinputcolor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.annotations.Varp;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.ui.JagexColors;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Getter
enum ChatChannel {
    PUBLIC(
        "PublicChat",
        VarPlayerID.OPTION_CHAT_COLOUR_PUBLIC_TRANSPARENT,
        VarPlayerID.OPTION_CHAT_COLOUR_PUBLIC_OPAQUE,
        JagexColors.CHAT_TYPED_TEXT_TRANSPARENT_BACKGROUND,
        JagexColors.CHAT_TYPED_TEXT_OPAQUE_BACKGROUND,
        Pattern.compile("^/(@p|p ).*"),
        0
    ),
    FRIEND(
        "ClanChatMessage",
        VarPlayerID.OPTION_CHAT_COLOUR_FRIENDSCHAT_TRANSPARENT,
        VarPlayerID.OPTION_CHAT_COLOUR_FRIENDSCHAT_OPAQUE,
        JagexColors.CHAT_FC_TEXT_TRANSPARENT_BACKGROUND,
        JagexColors.CHAT_FC_TEXT_OPAQUE_BACKGROUND,
        Pattern.compile("^/(@?f).*"),
        1
    ),
    CLAN(
        "ClanMessage",
        VarPlayerID.OPTION_CHAT_COLOUR_CLANCHAT_TRANSPARENT,
        VarPlayerID.OPTION_CHAT_COLOUR_CLANCHAT_OPAQUE,
        DefaultColors.CLAN_AND_GIM,
        DefaultColors.CLAN_AND_GIM,
        Pattern.compile("^/(@c|c ).*"),
        2
    ),
    GUEST(
        "ClanGuestMessage",
        VarPlayerID.OPTION_CHAT_COLOUR_GUESTCLAN_TRANSPARENT,
        VarPlayerID.OPTION_CHAT_COLOUR_GUESTCLAN_OPAQUE,
        DefaultColors.GUEST_CLAN_TRANSPARENT,
        DefaultColors.GUEST_CLAN_OPAQUE,
        Pattern.compile("^/(@gc|gc ).*"),
        3
    ),
    GIM(
        null,
        VarPlayerID.OPTION_CHAT_COLOUR_GIMCHAT_TRANSPARENT,
        VarPlayerID.OPTION_CHAT_COLOUR_GIMCHAT_OPAQUE,
        DefaultColors.CLAN_AND_GIM,
        DefaultColors.CLAN_AND_GIM,
        Pattern.compile("^/(@g[^c]|g ).*"),
        4
    );

    private final String colorConfigKey;
    @Getter(onMethod_ = {@Varp})
    private final @Varp int transparentVarpId;
    @Getter(onMethod_ = {@Varp})
    private final @Varp int opaqueVarpId;
    private final Color transparentDefaultColor;
    private final Color opaqueDefaultColor;

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
    public static ChatChannel fromSlashPrefix(String text) {
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
    public static ChatChannel fromSlashCount(int count) {
        return slashPrefixMap.get(count);
    }

    /**
     * @param chatModeVarClientIntValue VarClientInt value
     * @return Current chat mode channel
     */
    @Nullable
    public static ChatChannel fromChatModeVarClientInt(int chatModeVarClientIntValue) {
        switch (chatModeVarClientIntValue) {
            case 1:
                return FRIEND;
            case 2:
                return CLAN;
            case 3:
                return GUEST;
            case 4:
                return GIM;
        }

        return null;
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
