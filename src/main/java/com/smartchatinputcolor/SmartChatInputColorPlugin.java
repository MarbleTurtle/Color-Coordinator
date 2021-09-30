package com.smartchatinputcolor;

import java.util.Arrays;
import javax.inject.Inject;

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

enum ChatPanel
{
	PUBLIC,
	FRIEND,
	CLAN,
}

enum ChatChannel
{
	PUBLIC("PublicChat", 3000, 2992, 0x9090FF, 0x0000FF),
	FRIEND("ClanChatMessage", 3004, 2996, 0xEF5050, 0x7F0000),
	CLAN("ClanMessage", 3005, 2997, 0x7F0000, 0x7F0000),
	GUEST("ClanGuestMessage", 3061, 3060, 0x7F0000, 0x7F0000);

	final String colorConfigKey;
	final int transparentVarPId;
	final int opaqueVarPId;
	final int transparentFallbackRgb;
	final int opaqueFallbackRgb;

	ChatChannel(String colorConfigKey,
				int transparentVarPlayerId,
				int opaqueVarPlayerId,
				int transparentFallbackColor,
				int opaqueFallbackColor)
	{
		this.colorConfigKey = colorConfigKey;
		this.transparentVarPId = transparentVarPlayerId;
		this.opaqueVarPId = opaqueVarPlayerId;
		this.transparentFallbackRgb = transparentFallbackColor;
		this.opaqueFallbackRgb = opaqueFallbackColor;
	}
}

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

	private ChatPanel selectedChat = ChatPanel.PUBLIC;

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
		Widget chat = client.getWidget(WidgetInfo.CHATBOX_INPUT);
		if (chat == null)
		{
			return;
		}
		if (chat.getText().equals(client.getLocalPlayer().getName() + ": Press Enter to Chat...") ||
			chat.getText().equals("Friends Chat: Press Enter to Chat...") ||
			chat.getText().equals("Clan Chat: Press Enter to Chat...") ||
			chat.getText().equals("Guest Clan Chat: Press Enter to Chat..."))
		{
			return;
		}

		String name = chat.getText().contains(":") ? chat.getText().split(":")[0] + ":" : client.getLocalPlayer().getName() + ":";
		String text = client.getVar(VarClientStr.CHATBOX_TYPED_TEXT);

		chat.setText(name + " " + ColorUtil.wrapWithColorTag(text + "*", getChatColor(text ,name)));
	}

	/**
	 * Decide which channel color the input text should get
	 * TODO: Check whether player is a clan member and guest
	 *
	 * @param text Input text typed by the player
	 * @param name Input prefix (player name, or active chat mode)
	 * @return Chat channel that the input text should be colored as
	 */
	private ChatChannel deriveChatChannel(String text, String name)
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
			default:
				boolean inFriendsChat = client.getFriendsChatManager() != null;
				// If not a prefix, check if in a certain chat mode
				switch (name)
				{
					case "Friends Chat:":
						return inFriendsChat ? ChatChannel.FRIEND : ChatChannel.PUBLIC;
					case "Clan Chat:":
						return ChatChannel.CLAN;
					case "Guest Clan Chat:":
						return ChatChannel.GUEST;
					default:
						// If not in a chat mode, check if the typed texts starts with one of the prefixes
						if (text.startsWith("///") || (text.startsWith("/g ") || text.matches("/g")))
						{
							return ChatChannel.GUEST;
						}
						else if (text.startsWith("//") || (text.startsWith("/c ") || text.matches("/c")))
						{
							return ChatChannel.CLAN;
						}
						else if (text.startsWith("/") && inFriendsChat)
						{
							return ChatChannel.FRIEND;
						}
						else
						{
							// Finally, if the text contains no indicators, choose color based on active chat
							switch (selectedChat)
							{
								case CLAN:
									return ChatChannel.CLAN;
								case FRIEND:
									return inFriendsChat ? ChatChannel.FRIEND : ChatChannel.PUBLIC;
								default:
									return ChatChannel.PUBLIC;
							}
						}
				}
		}
	}

	private Color getChatColor(String text, String name)
	{
		ChatChannel channel = deriveChatChannel(text, name);
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
		else if (colorCode == -1)
		{
			return new Color(transparent ? channel.transparentFallbackRgb : channel.opaqueFallbackRgb);
		}
		else
		{
			return new Color(colorCode);
		}
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
				selectedChat = ChatPanel.CLAN;
				break;
			case 10616851:
				selectedChat = ChatPanel.FRIEND;
				break;
			default:
				selectedChat = ChatPanel.PUBLIC;
				break;
		}
	}
}
