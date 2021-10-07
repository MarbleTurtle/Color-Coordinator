package com.smartchatinputcolor;

import java.util.Arrays;
import javax.inject.Inject;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.VarClientStr;
import net.runelite.api.Varbits;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.awt.*;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Smart Chat Input Color"
)
public class SmartChatInputColorPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@AllArgsConstructor
	enum ChatChannel
	{
		PUBLIC("PublicChat", 3000, 2992, 0x9090FF, 0x0000FF),
		FRIEND("ClanChatMessage", 3004, 2996, 0xEF5050, 0x7F0000),
		CLAN("ClanMessage", 3005, 2997, 0x7F0000, 0x7F0000),
		GUEST("ClanGuestMessage", 3061, 3060, 0x7F0000, 0x7F0000);

		private final String colorConfigKey;
		private final int transparentVarPId;
		private final int opaqueVarPId;
		private final int transparentDefaultRgb;
		private final int opaqueDefaultRgb;
	}

	private ChatChannel selectedChat = ChatChannel.PUBLIC;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Input Recolorer started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Input Recolorer stopped!");
	}

	/**
	 * Recolor the chat input when the player selects a chat tab, or when the user is typing
	 *
	 * @param scriptPostFired information about the fired script
	 */
	@Subscribe
	public void onScriptPostFired(ScriptPostFired scriptPostFired)
	{
		if (!Arrays.asList(73, 175, ScriptID.CHAT_PROMPT_INIT).contains(scriptPostFired.getScriptId()))
		{
			return;
		}
		Widget inputWidget = client.getWidget(WidgetInfo.CHATBOX_INPUT);
		if (inputWidget == null)
		{
			return;
		}

		String input = inputWidget.getText();

		try
		{
			String playerName = client.getLocalPlayer().getName();
			if (input.equals(playerName + ": Press Enter to Chat...") ||
				input.equals("Friends Chat: Press Enter to Chat...") ||
				input.equals("Clan Chat: Press Enter to Chat...") ||
				input.equals("Guest Clan Chat: Press Enter to Chat..."))
			{
				return;
			}

			String name = input.contains(":") ? input.split(":")[0] + ":" : playerName + ":";
			String text = client.getVar(VarClientStr.CHATBOX_TYPED_TEXT);
			Color color = computeChannelColor(deriveChatChannel(name, text));
			inputWidget.setText(name + " " + ColorUtil.wrapWithColorTag(Text.escapeJagex(text) + "*", color));
		}
		catch (NullPointerException ignored)
		{
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
	private ChatChannel deriveChatChannel(String name, String text)
	{
		// First check if the text is exactly one of the prefixes
		switch (text)
		{
			case "/p":
				return ChatChannel.PUBLIC;
			case "/f":
				return ChatChannel.FRIEND;
			case "/c":
				return ChatChannel.CLAN;
			case "/g":
				return ChatChannel.GUEST;
		}

		// If not a prefix, check if in a certain chat mode
		switch (name)
		{
			case "Friends Chat:":
				return client.getFriendsChatManager() != null ? ChatChannel.FRIEND : ChatChannel.PUBLIC;
			case "Clan Chat:":
				return ChatChannel.CLAN;
			case "Guest Clan Chat:":
				return ChatChannel.GUEST;
		}

		// If not in a chat mode, check if the typed texts starts with one of the prefixes
		if (text.startsWith("///") || (text.startsWith("/g ") || text.matches("/g")))
		{
			return ChatChannel.GUEST;
		}
		if (text.startsWith("//") || (text.startsWith("/c ") || text.matches("/c")))
		{
			return ChatChannel.CLAN;
		}
		if (text.startsWith("/") && client.getFriendsChatManager() != null)
		{
			return ChatChannel.FRIEND;
		}

		// If the text contains no indicators, the message will be sent to the open chat tab
		return selectedChat;
	}

	private Color computeChannelColor(ChatChannel channel)
	{
		boolean transparent = client.isResized() && client.getVar(Varbits.TRANSPARENT_CHATBOX) == 1;

		Color color = configManager.getConfiguration(
			"textrecolor",
			(transparent ? "transparent" : "opaque") + channel.colorConfigKey,
			Color.class
		);
		if (color != null)
		{
			return color;
		}

		int colorCode = client.getVarpValue(transparent ? channel.transparentVarPId : channel.opaqueVarPId) - 1;
		if (colorCode == 0)
		{
			return Color.BLACK;
		}
		if (colorCode == -1)
		{
			return new Color(transparent ? channel.transparentDefaultRgb : channel.opaqueDefaultRgb);
		}

		return new Color(colorCode);
	}

	/**
	 * Capture when the player clicks on a chat tab, and save the newly selected chat tab
	 *
	 * @param scriptPreFired Information object about script that is about to fire
	 */
	@Subscribe
	public void onScriptPreFired(ScriptPreFired scriptPreFired)
	{
		if (scriptPreFired.getScriptId() != 175)
		{
			return;
		}
		Widget clicked = scriptPreFired.getScriptEvent().getSource();
		switch (clicked.getId())
		{
			case 10616855:
				selectedChat = ChatChannel.CLAN;
				break;
			case 10616851:
				selectedChat = client.getFriendsChatManager() != null ? ChatChannel.FRIEND : ChatChannel.PUBLIC;
				break;
			default:
				selectedChat = ChatChannel.PUBLIC;
				break;
		}
	}
}
