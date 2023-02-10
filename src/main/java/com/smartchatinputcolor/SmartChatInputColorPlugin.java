package com.smartchatinputcolor;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.*;
import net.runelite.api.events.FriendsChatChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.vars.AccountType;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
		name = "Smart Chat Input Color"
)
public class SmartChatInputColorPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	private ChatPanel selectedChatPanel = null;

	private boolean isInFriendsChat = false;

	private boolean hoppingWorlds = false;

	private Map<ChatChannel, Color> chatChannelColorMap;

	@Override
	protected void startUp() {
		log.debug("Smart Chat Input Color started!");
		if (client.getGameState() == GameState.LOGGED_IN) {
			initialize();
		}
	}

	@Override
	protected void shutDown() {
		log.debug("Smart Chat Input Color stopped!");
		// Reset when stopping plugin
		selectedChatPanel = null;
		isInFriendsChat = false;
	}

	protected void initialize() {
		selectedChatPanel = ChatPanel.fromVarcIntValue(client.getVarcIntValue(41));
		isInFriendsChat = client.getFriendsChatManager() != null;
		// TODO: Repopulate color map when config changes
		populateChatChannelColorMap();
	}

	/**
	 * Recolor the chat input when the player selects a chat tab, or when the user is typing
	 *
	 * @param scriptPostFired information about the fired script
	 */
	@Subscribe
	public void onScriptPostFired(ScriptPostFired scriptPostFired) {
		if (!Arrays.asList(73, 175, ScriptID.CHAT_PROMPT_INIT).contains(scriptPostFired.getScriptId())) {
			return;
		}
		Widget inputWidget = client.getWidget(WidgetInfo.CHATBOX_INPUT);
		if (inputWidget == null) {
			return;
		}

		String input = inputWidget.getText();

		try {
			String playerName = client.getLocalPlayer().getName();
			if (input.split(":", 2)[1].equals(" Press Enter to Chat...")) {
				return;
			}
			String name = input.contains(":") ? input.split(":")[0] + ":" : playerName + ":";
			String text = client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT);
			Color color = chatChannelColorMap.get(deriveChatChannel(name, text));
			inputWidget.setText(name + " " + ColorUtil.wrapWithColorTag(Text.escapeJagex(text) + "*", color));
		} catch (NullPointerException ignored) {
			log.debug("Player name cannot be retrieved (NullPointerException)");
		}
	}

	/**
	 * Decide which channel color the input text should get
	 * TODO: Check whether player is a clan member and / or guest
	 *
	 * @param name Chat prefix (player name, or active chat mode)
	 * @param text Chat input text typed by the player
	 * @return Chat channel whose color the input text should be recolored to
	 */
	private ChatChannel deriveChatChannel(String name, String text) {
		// First check if the text starts with one of the prefixes
		if (ChatChannel.GUEST.matchesRegex(text)) {
			return ChatChannel.GUEST;
		}
		if (ChatChannel.GIM.matchesRegex(text)) {
			return getGIMChatChannel(text);
		}
		if (ChatChannel.CLAN.matchesRegex(text)) {
			return ChatChannel.CLAN;
		}
		if (ChatChannel.FRIEND.matchesRegex(text)) {
			return getFriendsChatChannel();
		}
		if (ChatChannel.PUBLIC.matchesRegex(text)) {
			return ChatChannel.PUBLIC;
		}

		// If not a prefix, check if in a certain chat mode
		if (name.contains("(")) {
			name = name.split("\\(")[1].split("\\)")[0];
			switch (name) {
				case "channel":
					return getFriendsChatChannel();
				case "clan":
					return ChatChannel.CLAN;
				case "guest clan":
					return ChatChannel.GUEST;
				case "group":
					return ChatChannel.GIM;
			}
		}

		// If the text contains no indicators, the message will be sent to the open chat tab
		return getSelectedChatPanelChannel();
	}

	private Color computeChannelColor(ChatChannel channel) {
		boolean transparent = client.isResized() && client.getVarbitValue(Varbits.TRANSPARENT_CHATBOX) == 1;

		String colorConfigKey = channel.getColorConfigKey();
		if (colorConfigKey != null) {
			Color color = configManager.getConfiguration(
					"textrecolor",
					(transparent ? "transparent" : "opaque") + colorConfigKey,
					Color.class
			);
			if (color != null) {
				return color;
			}
		}

		int colorCode = client.getVarpValue((transparent ? channel.getTransparentVarPId() : channel.getOpaqueVarPId()).getId()) - 1;
		if (colorCode == 0) {
			return Color.BLACK;
		}
		if (colorCode == -1) {
			return new Color(transparent ? channel.getTransparentDefaultRgb() : channel.getOpaqueDefaultRgb());
		}

		return new Color(colorCode);
	}

	private void populateChatChannelColorMap() {
		for (ChatChannel channel : ChatChannel.values()) {
			chatChannelColorMap.put(channel, computeChannelColor(channel));
		}
	}

	/**
	 * Find the chat channel that a message will be sent to if trying to send to friends channel
	 *
	 * @return Chat channel that the message will go to
	 */
	public ChatChannel getFriendsChatChannel() {
		return isInFriendsChat ? ChatChannel.FRIEND : ChatChannel.PUBLIC;
	}

	/**
	 * Find the chat channel that a message will be sent to if trying to send to group ironman channel
	 * If an account is a Group Ironman, the Group Ironman chat channel is available.
	 * Otherwise, the logic is a bit more involved.
	 *
	 * @return Chat channel that the message will go to
	 */
	private ChatChannel getGIMChatChannel(String text) {
		if (client.getAccountType() == AccountType.GROUP_IRONMAN) {
			return ChatChannel.GIM;
		}

		if (text.startsWith("/g")) {
			return getFriendsChatChannel();
		}

		if (text.startsWith("/@g")) {
			return ChatChannel.CLAN;
		}

		if (text.startsWith("////")) {
			return ChatChannel.GUEST;
		}

		return ChatChannel.GIM;
	}

	private ChatChannel getSelectedChatPanelChannel() {
		switch (selectedChatPanel) {
			case CHANNEL:
				return getFriendsChatChannel();
			case CLAN:
				return ChatChannel.CLAN;
			default:
				return ChatChannel.PUBLIC;
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
				initialize();
			}
		}
	}

	/**
	 * Capture when the player clicks on a chat tab, and save the newly selected chat tab
	 *
	 * @param varClientIntChanged VarClientInt changed event object
	 */
	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged varClientIntChanged) {
		if (varClientIntChanged.getIndex() == 41) {
			selectedChatPanel = ChatPanel.fromVarcIntValue(client.getVarcIntValue(41));
		}
	}

	/**
	 * Update whether player is in a friends chat channel when the friends chat changes
	 *
	 * @param friendsChatChanged FriendsChat changed event object
	 */
	@Subscribe
	public void onFriendsChatChanged(FriendsChatChanged friendsChatChanged) {
		isInFriendsChat = friendsChatChanged.isJoined();
	}
}
