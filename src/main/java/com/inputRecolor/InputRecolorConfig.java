package com.inputRecolor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("example")
public interface InputRecolorConfig extends Config
{
	@ConfigItem(
		keyName = "public",
		name = "Public recolor",
		description = "Recolors messages that will be sent to public chat.",
		position=0
	)
	default Color publicColor(){return Color.blue;}
	@ConfigItem(
			keyName = "friend",
			name = "Friend recolor",
			description = "Recolors messages that will be sent to friend chat.",
			position=1
	)
	default Color friendColor(){return Color.orange;}
	@ConfigItem(
			keyName = "clan",
			name = "Clan recolor",
			description = "Recolors messages that will be sent to clan chat.",
			position=2
	)
	default Color clanColor(){return Color.pink;}
	@ConfigItem(
			keyName = "guest",
			name = "Guest recolor",
			description = "Recolors messages that will be sent to guest clan chat.",
			position=3
	)
	default Color guestColor(){return Color.green;}
}
