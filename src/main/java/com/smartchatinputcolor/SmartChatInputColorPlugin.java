package com.smartchatinputcolor;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.annotations.VarCInt;
import net.runelite.api.annotations.Varp;
import net.runelite.api.events.*;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@PluginDescriptor(name = "Smart Chat Input Color")
public class SmartChatInputColorPlugin extends Plugin {
    @VarCInt
    static final int OPEN_CHAT_PANEL = 41;

    @VarCInt
    static final int CHAT_MODE = 945;

    @Inject
    private Client client;

    @Inject
    private SmartChatInputColorPluginConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private ClientThread clientThread;

    private ChatPanel selectedChatPanel;

    private ChatChannel friendsChatChannel;

    private boolean hoppingWorlds;

    private boolean shouldInitialize;

    private boolean slashSwapperBug;

    private final Map<ChatChannel, Color> channelColorMap = new HashMap<>();

    @Override
    protected void startUp() {
        log.debug("Smart Chat Input Color starting!");
        if (client.getGameState() == GameState.LOGGED_IN) {
            shouldInitialize = true;
        }
    }

    @Override
    protected void shutDown() {
        log.debug("Smart Chat Input Color stopping!");
        // Reset when stopping plugin
        selectedChatPanel = null;
        friendsChatChannel = null;
        channelColorMap.clear();
    }

    @Provides
    SmartChatInputColorPluginConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SmartChatInputColorPluginConfig.class);
    }

    /**
     * Recolor the text typed in the chat, based on the channel that the message will be sent to
     */
    private void recolorChatTypedText() {
        Widget inputWidget = client.getWidget(ComponentID.CHATBOX_INPUT);
        if (inputWidget == null) {
            return;
        }

        String input = inputWidget.getText();
        // Key Remapping is active and chat is locked, do not recolor
        if (input.endsWith("Press Enter to Chat...")) {
            return;
        }

        int colonIndex = input.indexOf(':');
        if (colonIndex == -1) {
            return;
        }
        String name = input.substring(0, colonIndex);
        String typedText = client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT);
        Color chatColor = channelColorMap.get(deriveChatChannel(typedText));
        inputWidget.setText(name + ": " + ColorUtil.wrapWithColorTag(Text.escapeJagex(typedText) + "*", chatColor));
    }

    /**
     * Decide which channel color the input text should get
     *
     * @param text Chat input text typed by the player
     * @return Chat channel whose color the input text should be recolored to
     */
    private ChatChannel deriveChatChannel(String text) {
        // First check if the text starts with one of the prefixes
        ChatChannel channel = findChannelByMessagePrefix(text);
        if (channel != null) {
            return channel;
        }

        // If it didn't match a prefix, check if in a certain chat mode
        channel = ChatChannel.fromChatModeVarClientInt(client.getVarcIntValue(CHAT_MODE));
        if (channel != null) {
            return channel;
        }

        // No indicators from message prefix or chat mode, so the message will be sent to the open chat panel
        return getSelectedChatPanelChannel();
    }

    /**
     * Find the channel that a message would be sent to based on the prefix.
     *
     * @param text Chat message input
     * @return Channel that the message would be sent to or null
     */
    private ChatChannel findChannelByMessagePrefix(String text) {
        // First check if the prefix regex matches
        for (ChatChannel channel : ChatChannel.values()) {
            if (channel.matchesPrefixRegex(text)) {
                return getResultingChannel(channel, text);
            }
        }

        // Check the slash prefix if there is no regex match
        ChatChannel channel = ChatChannel.fromSlashPrefix(text);

        // If Slash Swapper bug is active and the result is guest chat (///) or GIM chat (////), return friend instead
        if (slashSwapperBug && (channel == ChatChannel.GUEST || channel == ChatChannel.GIM && !isGroupIronman())) {
            channel = ChatChannel.FRIEND;
        }

        return getResultingChannel(channel, text);
    }

    /**
     * Compute the color of a chat channel based on RL and in-game settings
     *
     * @param channel Chat channel
     * @return Color that the text should be colored for the given chat channel
     */
    private Color computeChannelColor(ChatChannel channel, boolean transparent) {
        String colorConfigKey = channel.getColorConfigKey();
        if (colorConfigKey != null) {
            Color color = configManager.getConfiguration(
                "textrecolor", (transparent ? "transparent" : "opaque") + colorConfigKey, Color.class);
            if (color != null) {
                return color;
            }
        }

        int colorCode = client.getVarpValue(transparent ? channel.getTransparentVarpId() : channel.getOpaqueVarpId());
        // Zero means there is no value set, return the default value for this channel
        if (colorCode == 0) {
            return transparent ? channel.getTransparentDefaultColor() : channel.getOpaqueDefaultColor();
        }

        // Color code saved in the varp is offset by 1
        return new Color(colorCode - 1);
    }

    /**
     * Update the mapped color for each chat channel
     */
    private void populateChatChannelColorMap() {
        boolean transparent = client.isResized() && client.getVarbitValue(Varbits.TRANSPARENT_CHATBOX) == 1;
        for (ChatChannel c : ChatChannel.values()) {
            channelColorMap.put(c, computeChannelColor(c, transparent));
        }
    }

    /**
     * Set the chat channel that a message will be sent to if trying to send to friends channel
     */
    private void setFriendsChatChannel(boolean isInFriendsChat) {
        friendsChatChannel = isInFriendsChat ? ChatChannel.FRIEND : ChatChannel.PUBLIC;
    }

    /**
     * Set the currently opened chat panel
     */
    private void setOpenChatPanel() {
        selectedChatPanel = ChatPanel.fromVarClientInt(client.getVarcIntValue(OPEN_CHAT_PANEL));
    }

    /**
     * Find the resulting channel, keeping in mind whether the player is currently in a friends channel
     *
     * @param channel Chat channel trying to send the message to
     * @return Chat channel that the message will really go to
     */
    @Nullable
    private ChatChannel getResultingChannel(ChatChannel channel) {
        if (channel == null) {
            return null;
        }

        return channel == ChatChannel.FRIEND ? friendsChatChannel : channel;
    }

    /**
     * Find the resulting channel, checking whether the player is currently in a friends channel or has a GIM account
     *
     * @param channel Chat channel trying to send the message to
     * @return Chat channel that the message will really go to
     */
    @Nullable
    private ChatChannel getResultingChannel(ChatChannel channel, String text) {
        if (channel == null) {
            return null;
        }

        switch (channel) {
            case FRIEND:
                return friendsChatChannel;
            case GIM:
                return getGIMChatChannel(text);
            default:
                return channel;
        }
    }

    /**
     * Find the chat channel that a message will be sent to when trying to send to group ironman channel. If an account
     * is a Group Ironman, the Group Ironman chat channel is available. Otherwise, a bit more logic is involved.
     *
     * @return Chat channel that the message will go to
     */
    private ChatChannel getGIMChatChannel(String text) {
        if (isGroupIronman()) {
            return ChatChannel.GIM;
        }

        if (text.startsWith("/g")) {
            return getResultingChannel(ChatChannel.fromSlashCount(1));
        }

        if (text.startsWith("/@g")) {
            return ChatChannel.CLAN;
        }

        if (text.startsWith("////")) {
            return getResultingChannel(ChatChannel.fromSlashCount(3));
        }

        // This never happens because the string passed into this function
        // will always start with one of the prefixes handled above
        return ChatChannel.GIM;
    }

    /**
     * Get the chat channel that a message should be sent to, based on the currently selected chat panel
     *
     * @return Chat channel that the message will go to
     */
    private ChatChannel getSelectedChatPanelChannel() {
        switch (selectedChatPanel) {
            case CHANNEL:
                return friendsChatChannel;
            case CLAN:
                return ChatChannel.CLAN;
            case TRADE_OR_GIM:
                return isGroupIronman() ? ChatChannel.GIM : ChatChannel.PUBLIC;
        }

        return ChatChannel.PUBLIC;
    }

    private boolean isGroupIronman() {
        switch (client.getVarbitValue(Varbits.ACCOUNT_TYPE)) {
            case 4: // GIM
            case 5: // HCGIM
            case 6: // UGIM
                return true;
        }
        return false;
    }

    private boolean getSlashSwapperGuestChatConfig() {
        return configManager.getConfiguration("slashswapper", "slashGuestChat", boolean.class);
    }

    /**
     * Configure up slash prefixes based on whether Slash Swapper is active
     */
    private void configureSlashPrefixes() {
        boolean slashSwapperEnabled = pluginManager
            .getPlugins()
            .stream()
            .anyMatch(p -> p.getName().equals("Slash Swapper") && pluginManager.isPluginEnabled(p));

        if (slashSwapperEnabled) {
            useSlashSwapperPrefixes();
            return;
        }

        slashSwapperBug = false;
        ChatChannel.useDefaultSlashPrefixes();
    }

    private void useSlashSwapperPrefixes() {
        boolean guestChatConfig = getSlashSwapperGuestChatConfig();
        slashSwapperBug = !guestChatConfig && config.slashSwapperBug();
        ChatChannel.useSlashSwapperPrefixes(guestChatConfig);
    }

    /**
     * Recolor the chat input when the player selects a chat tab, or when the user is typing
     *
     * @param scriptPostFired information about the fired script
     */
    @Subscribe
    public void onScriptPostFired(ScriptPostFired scriptPostFired) {
        if (scriptPostFired.getScriptId() == ScriptID.CHAT_PROMPT_INIT) {
            recolorChatTypedText();
        }
    }

    /**
     * Initialize after an account is logged in, but not when hopping worlds
     *
     * @param gameStateChanged GameState changed event object
     */
    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        switch (gameStateChanged.getGameState()) {
            case HOPPING:
                hoppingWorlds = true;
                break;
            case LOGGED_IN: {
                if (hoppingWorlds) {
                    hoppingWorlds = false;
                    return;
                }
                shouldInitialize = true;
            }
        }
    }

    /**
     * Initialize the plugin on the game tick after shouldInitialize is set, it requires Varbits / VarPlayers to be set
     */
    @Subscribe
    public void onGameTick(GameTick ignored) {
        if (!shouldInitialize) {
            return;
        }

        setOpenChatPanel();
        setFriendsChatChannel(client.getFriendsChatManager() != null);
        configureSlashPrefixes();
        populateChatChannelColorMap();
        shouldInitialize = false;
        recolorChatTypedText();
    }

    /**
     * Update chat channel color map when a relevant RL config is changed
     *
     * @param configChanged Config changed event object
     */
    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        String configGroup = configChanged.getGroup();
        if (configGroup.equals(SmartChatInputColorPluginConfig.GROUP) || configGroup.equals("slashswapper")) {
            clientThread.invoke(() -> {
                configureSlashPrefixes();
                recolorChatTypedText();
            });
        }

        if (configGroup.equals("textrecolor")) {
            clientThread.invoke(() -> {
                populateChatChannelColorMap();
                recolorChatTypedText();
            });
        }
    }

    /**
     * Update chat channel color map when Slash Swapper is turned on or off
     *
     * @param pluginChanged Plugin changed event object
     */
    @Subscribe
    public void onPluginChanged(PluginChanged pluginChanged) {
        Plugin plugin = pluginChanged.getPlugin();
        if (!plugin.getName().equals("Slash Swapper")) {
            return;
        }

        if (pluginManager.isPluginEnabled(plugin)) {
            useSlashSwapperPrefixes();
        } else {
            slashSwapperBug = false;
            ChatChannel.useDefaultSlashPrefixes();
        }

        recolorChatTypedText();
    }

    /**
     * Update chat channel color map when a relevant in-game setting is changed
     *
     * @param varbitChanged Varbit changed event object
     */
    @Subscribe
    public void onVarbitChanged(VarbitChanged varbitChanged) {
        // Check if the setting for transparent chat box changed
        if (varbitChanged.getVarbitId() == Varbits.TRANSPARENT_CHATBOX) {
            populateChatChannelColorMap();
            return;
        }

        // Check if any of the chat channel colours was changed
        @Varp int varPlayerId = varbitChanged.getVarpId();
        for (ChatChannel channel : ChatChannel.values()) {
            if (varPlayerId == channel.getOpaqueVarpId() || varPlayerId == channel.getTransparentVarpId()) {
                populateChatChannelColorMap();
                return;
            }
        }
    }

    /**
     * Update selected chat panel when a new chat panel is opened
     *
     * @param varClientIntChanged VarClientInt changed event object
     */
    @Subscribe
    public void onVarClientIntChanged(VarClientIntChanged varClientIntChanged) {
        if (varClientIntChanged.getIndex() == OPEN_CHAT_PANEL) {
            setOpenChatPanel();
        }
    }

    /**
     * Update state when client joins or leaves a friends chat and recolor the typed text
     *
     * @param friendsChatChanged FriendsChat changed event object
     */
    @Subscribe
    public void onFriendsChatChanged(FriendsChatChanged friendsChatChanged) {
        setFriendsChatChannel(friendsChatChanged.isJoined());
        recolorChatTypedText();
    }
}
