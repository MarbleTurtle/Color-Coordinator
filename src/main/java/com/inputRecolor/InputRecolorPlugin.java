package com.inputRecolor;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
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

@Slf4j
@PluginDescriptor(
	name = "Input Recolorer"
)
public class InputRecolorPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	int selected=0;
	Color friendColorConfigColor;
	Color clanColorConfigColor;
	Color guestColorConfigColor;
	Color unselectedColorConfigColor;

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

	@Subscribe
	public void onScriptPostFired(ScriptPostFired scriptPostFired)
	{
		if(scriptPostFired.getScriptId()!=73&&scriptPostFired.getScriptId()!=175)
			return;
		boolean transparent = client.isResized() && client.getVar(Varbits.TRANSPARENT_CHATBOX) != 0;
		Widget chat = client.getWidget(WidgetInfo.CHATBOX_INPUT);
		if (chat == null)
			return;
		String name = chat.getText().contains(":") ? chat.getText().split(":")[0] + ":" : client.getLocalPlayer().getName() + ":";
		String text = client.getVar(VarClientStr.CHATBOX_TYPED_TEXT);
		switch (text){
			case "/p":
				unselectedColorConfigColor = transparent ? new Color(0x9090ff) : new Color(0x0000FF);
				text = " <col=" + Integer.toHexString(unselectedColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
				chat.setText(name + text);
				break;
			case "/f":
				friendColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentClanChatMessage" : "opaqueClanChatMessage", Color.class);
				if (friendColorConfigColor == null) {
					int colorCode = transparent ? client.getVarpValue(3004) - 1 : client.getVarpValue(2996) - 1;
					if (colorCode == 0) {
						friendColorConfigColor = Color.BLACK;
					} else if (colorCode == -1) {
						friendColorConfigColor = transparent ? new Color(0xEF5050) : new Color(8323072);
					} else {
						friendColorConfigColor = new Color(colorCode);
					}
				}
				text = " <col=" + Integer.toHexString(friendColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
				chat.setText(name + text);
				break;
			case "/c":
				clanColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentClanMessage" : "opaqueClanMessage", Color.class);
				if (clanColorConfigColor == null) {
					int colorCode = transparent ? client.getVarpValue(3005) - 1 : client.getVarpValue(2997) - 1;
					if (colorCode == 0) {
						clanColorConfigColor = Color.BLACK;
					} else if (colorCode == -1) {
						clanColorConfigColor = transparent ? new Color(8323072) : new Color(8323072);
					} else {
						clanColorConfigColor = new Color(colorCode);
					}
				}
				text = " <col=" + Integer.toHexString(clanColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
				chat.setText(name + text);
				break;
			case "/g":
				guestColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentClanGuestMessage" : "opaqueClanGuestMessage", Color.class);
				if (guestColorConfigColor == null) {
					int colorCode = transparent ? client.getVarpValue(3005) - 1 : client.getVarpValue(2997) - 1;
					if (colorCode == 0) {
						guestColorConfigColor = Color.BLACK;
					} else if (colorCode == -1) {
						guestColorConfigColor = transparent ? new Color(8323072) : new Color(8323072);
					} else {
						guestColorConfigColor = new Color(colorCode);
					}
				}
				text = " <col=" + Integer.toHexString(guestColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
				chat.setText(name + text);
				break;
			default:
			switch (name) {
				case "Friends Chat:":
					if(client.getFriendsChatManager()==null)
						return;
					friendColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentClanChatMessage" : "opaqueClanChatMessage", Color.class);
					if (friendColorConfigColor == null) {
						int colorCode = transparent ? client.getVarpValue(3004) - 1 : client.getVarpValue(2996) - 1;
						if (colorCode == 0) {
							friendColorConfigColor = Color.BLACK;
						} else if (colorCode == -1) {
							friendColorConfigColor = transparent ? new Color(0xEF5050) : new Color(8323072);
						} else {
							friendColorConfigColor = new Color(colorCode);
						}
					}
					text = " <col=" + Integer.toHexString(friendColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
					chat.setText(name + text);
					break;
				case "Clan Chat:":
					clanColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentClanMessage" : "opaqueClanMessage", Color.class);
					if (clanColorConfigColor == null) {
						int colorCode = transparent ? client.getVarpValue(3005) - 1 : client.getVarpValue(2997) - 1;
						if (colorCode == 0) {
							clanColorConfigColor = Color.BLACK;
						} else if (colorCode == -1) {
							clanColorConfigColor = transparent ? new Color(8323072) : new Color(8323072);
						} else {
							clanColorConfigColor = new Color(colorCode);
						}
					}
					text = " <col=" + Integer.toHexString(clanColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
					chat.setText(name + text);
					break;
				case "Guest Clan Chat:":
					guestColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentClanGuestMessage" : "opaqueClanGuestMessage", Color.class);
					if (guestColorConfigColor == null) {
						int colorCode = transparent ? client.getVarpValue(3005) - 1 : client.getVarpValue(2997) - 1;
						if (colorCode == 0) {
							guestColorConfigColor = Color.BLACK;
						} else if (colorCode == -1) {
							guestColorConfigColor = transparent ? new Color(8323072) : new Color(8323072);
						} else {
							guestColorConfigColor = new Color(colorCode);
						}
					}
					text = " <col=" + Integer.toHexString(guestColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
					chat.setText(name + text);
					break;
				default:
					if (text.startsWith("///") || (text.startsWith("/g ") || text.matches("/g"))) {
						guestColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentClanGuestMessage" : "opaqueClanGuestMessage", Color.class);
						if (guestColorConfigColor == null) {
							int colorCode = transparent ? client.getVarpValue(3005) - 1 : client.getVarpValue(2997) - 1;
							if (colorCode == 0) {
								guestColorConfigColor = Color.BLACK;
							} else if (colorCode == -1) {
								guestColorConfigColor = transparent ? new Color(8323072) : new Color(8323072);
							} else {
								guestColorConfigColor = new Color(colorCode);
							}
						}
						text = " <col=" + Integer.toHexString(guestColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
						chat.setText(name + text);
					} else if (text.startsWith("//") || (text.startsWith("/c ") || text.matches("/c"))) {
						clanColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentClanMessage" : "opaqueClanMessage", Color.class);
						if (clanColorConfigColor == null) {
							int colorCode = transparent ? client.getVarpValue(3005) - 1 : client.getVarpValue(2997) - 1;
							if (colorCode == 0) {
								clanColorConfigColor = Color.BLACK;
							} else if (colorCode == -1) {
								clanColorConfigColor = transparent ? new Color(8323072) : new Color(8323072);
							} else {
								clanColorConfigColor = new Color(colorCode);
							}
						}
						text = " <col=" + Integer.toHexString(clanColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
						chat.setText(name + text);
					} else if (text.startsWith("/") && client.getFriendsChatManager() != null) {
						friendColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentClanChatMessage" : "opaqueClanChatMessage", Color.class);
						if (friendColorConfigColor == null) {
							int colorCode = transparent ? client.getVarpValue(3004) - 1 : client.getVarpValue(2996) - 1;
							if (colorCode == 0) {
								friendColorConfigColor = Color.BLACK;
							} else if (colorCode == -1) {
								friendColorConfigColor = transparent ? new Color(0xEF5050) : new Color(8323072);
							} else {
								friendColorConfigColor = new Color(colorCode);
							}
						}
						text = " <col=" + Integer.toHexString(friendColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
						chat.setText(name + text);
					} else {
						if (selected == 0) {
							unselectedColorConfigColor = transparent ? new Color(0x9090ff) : new Color(0x0000FF);
							text = " <col=" + Integer.toHexString(unselectedColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
							chat.setText(name + text);
						} else if (selected == 1) { //clan
							clanColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentClanMessage" : "opaqueClanMessage", Color.class);
							if (clanColorConfigColor == null) {
								int colorCode = transparent ? client.getVarpValue(3005) - 1 : client.getVarpValue(2997) - 1;
								if (colorCode == 0) {
									clanColorConfigColor = Color.BLACK;
								} else if (colorCode == -1) {
									clanColorConfigColor = transparent ? new Color(8323072) : new Color(8323072);
								} else {
									clanColorConfigColor = new Color(colorCode);
								}
							}
							text = " <col=" + Integer.toHexString(clanColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
							chat.setText(name + text);
						} else if (selected == 2 && client.getFriendsChatManager() != null) { //friend
							friendColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentClanChatMessage" : "opaqueClanChatMessage", Color.class);
							if (friendColorConfigColor == null) {
								int colorCode = transparent ? client.getVarpValue(3004) - 1 : client.getVarpValue(2996) - 1;
								if (colorCode == 0) {
									friendColorConfigColor = Color.BLACK;
								} else if (colorCode == -1) {
									friendColorConfigColor = transparent ? new Color(0xEF5050) : new Color(8323072);
								} else {
									friendColorConfigColor = new Color(colorCode);
								}
							}
							text = " <col=" + Integer.toHexString(friendColorConfigColor.getRGB()).substring(2) + ">" + text + "*</col>";
							chat.setText(name + text);
						}
					}
			}
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired scriptPreFired)
	{
		if(scriptPreFired.getScriptId()!=175)
			return;
		Widget clicked=scriptPreFired.getScriptEvent().getSource();
		if(clicked.getId()==10616855){ //clanchat
			selected=1;
		}else if(clicked.getId()==10616851){ //friendchat
			selected=2;
		} else {
			selected=0;
		}
	}
}
