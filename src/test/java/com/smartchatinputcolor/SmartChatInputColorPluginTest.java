package com.smartchatinputcolor;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SmartChatInputColorPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SmartChatInputColorPlugin.class);
		RuneLite.main(args);
	}
}