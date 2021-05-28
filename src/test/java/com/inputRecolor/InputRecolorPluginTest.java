package com.inputRecolor;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class InputRecolorPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(InputRecolorPlugin.class);
		RuneLite.main(args);
	}
}