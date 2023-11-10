package com.smartchatinputcolor;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.*;
import net.runelite.api.annotations.Varp;
import net.runelite.api.events.*;
import net.runelite.api.widgets.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
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

	@Inject
	private ClientThread clientThread;

	private ChatPanel selectedChatPanel = null;

	private boolean isInFriendsChat = false;

	private boolean hoppingWorlds = false;

	private boolean shouldInitialize = false;

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
		isInFriendsChat = false;
		channelColorMap.clear();
	}

	/**
	 * Recolor the text typed in the chat, based on
	 * the channel that the message will be sent to
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

		// Get player, is null when just logging in, so check and abort
		Player player = client.getLocalPlayer();
		if (player == null) {
			return;
		}

		String name = (
			input.contains(":") ? input.split(":")[0] : player.getName()
		);
		String text = client.getVarcStrValue(
			VarClientStr.CHATBOX_TYPED_TEXT
		);
		inputWidget.setText(
			name + ": " + ColorUtil.wrapWithColorTag(
				Text.escapeJagex(text) + "*",
				channelColorMap.get(deriveChatChannel(name, text))
			)
		);
	}

	/**
	 * Decide which channel color the input text should get
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
			name = name.split("\\(")[1].replace(")", "");
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

		// Text contains no indicators, message is sent to the open chat panel
		return getSelectedChatPanelChannel();
	}

	/**
	 * Compute the color of a chat channel based on RL and in-game settings
	 *
	 * @param channel Chat channel
	 * @return Color that the text should be colored for the given chat channel
	 */
	private Color computeChannelColor(ChatChannel channel) {
		boolean transparent = client.isResized() &&
			client.getVarbitValue(Varbits.TRANSPARENT_CHATBOX) == 1;

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

		int colorCode = client.getVarpValue(
			(transparent
				? channel.getTransparentVarpId()
				: channel.getOpaqueVarpId()
			)
		) - 1;
		if (colorCode == 0) {
			return Color.BLACK;
		}
		if (colorCode == -1) {
			return new Color(
				transparent
					? channel.getTransparentDefaultRgb()
					: channel.getOpaqueDefaultRgb()
			);
		}

		return new Color(colorCode);
	}

	/**
	 * Update the mapped color for each chat channel
	 */
	private void populateChatChannelColorMap() {
		for (ChatChannel channel : ChatChannel.values()) {
			channelColorMap.put(channel, computeChannelColor(channel));
		}
	}

	/**
	 * Find the chat channel that a message will be
	 * sent to if trying to send to friends channel
	 *
	 * @return Chat channel that the message will go to
	 */
	public ChatChannel getFriendsChatChannel() {
		return isInFriendsChat ? ChatChannel.FRIEND : ChatChannel.PUBLIC;
	}

	/**
	 * Find the chat channel that a message will be sent to when
	 * trying to send to group ironman channel. If an account
	 * is a Group Ironman, the Group Ironman chat channel is
	 * available. Otherwise, a bit more logic is involved.
	 *
	 * @return Chat channel that the message will go to
	 */
	private ChatChannel getGIMChatChannel(String text) {
		switch (client.getVarbitValue(Varbits.ACCOUNT_TYPE)) {
			case 4: // GIM
			case 5: // HCGIM
			case 6: // UGIM
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

	/**
	 * Get the chat channel that a message should be sent
	 * to, based on the currently selected chat panel
	 *
	 * @return Chat channel that the message will go to
	 */
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
	 * Recolor the chat input when the player selects
	 * a chat tab, or when the user is typing
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
	 * Initialize the plugin on the game tick after shouldInitialize is set,
	 * because initialization requires Varbits and VarPlayers to be set
	 */
	@Subscribe
	public void onGameTick(GameTick ignored) {
		if (shouldInitialize) {
			selectedChatPanel = ChatPanel.fromInt(client.getVarcIntValue(41));
			isInFriendsChat = client.getFriendsChatManager() != null;
			populateChatChannelColorMap();
		}
	}

	/**
	 * Update chat channel color map when a relevant RL config is changed
	 *
	 * @param configChanged Config changed event object
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged) {
		// TODO: Update the color map with more granularity
		if (configChanged.getGroup().equals("textrecolor")) {
			clientThread.invoke(() -> {
				populateChatChannelColorMap();
				recolorChatTypedText();
			});
		}
	}

	/**
	 * Update chat channel color map when a relevant in-game setting is changed
	 *
	 * @param varbitChanged Varbit changed event object
	 */
	@Subscribe
	public void onVarbitChanged(VarbitChanged varbitChanged) {
		// TODO: Update the color map with more granularity
		@Varp int varPlayerId = varbitChanged.getVarpId();
		for (ChatChannel channel : ChatChannel.values()) {
			if (varPlayerId == channel.getOpaqueVarpId() ||
				varPlayerId == channel.getTransparentVarpId()) {
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
		if (varClientIntChanged.getIndex() == 41) {
			selectedChatPanel = ChatPanel.fromInt(client.getVarcIntValue(41));
		}
	}

	/**
	 * Update state when client joins or leaves a friends chat,
	 * and recolor the typed text in case it should change color
	 *
	 * @param friendsChatChanged FriendsChat changed event object
	 */
	@Subscribe
	public void onFriendsChatChanged(FriendsChatChanged friendsChatChanged) {
		isInFriendsChat = friendsChatChanged.isJoined();
		recolorChatTypedText();
	}
}
