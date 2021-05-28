package com.inputRecolor;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Input Recolorer"
)
public class InputRecolorPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private InputRecolorConfig config;

	int selected=0;

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
		Widget chat = client.getWidget(WidgetInfo.CHATBOX_INPUT);
		if (chat == null)
			return;
		String name = client.getLocalPlayer().getName() + ":";
		String text = client.getVar(VarClientStr.CHATBOX_TYPED_TEXT);
		if (text.startsWith("///") || text.startsWith("/g ")) {
			text = " <col="+Integer.toHexString(config.guestColor().getRGB()).substring(2)+">" + text + "*</col>";
			chat.setText(name + text);
		} else if (text.startsWith("//") || text.startsWith("/c ")) {
			text = " <col="+Integer.toHexString(config.clanColor().getRGB()).substring(2)+">" + text + "*</col>";
			chat.setText(name + text);
		} else if (text.startsWith("/")) {
			text = " <col="+Integer.toHexString(config.friendColor().getRGB()).substring(2)+">" + text + "*</col>";
			chat.setText(name + text);
		} else {
			if(selected==0){
				text = " <col="+Integer.toHexString(config.publicColor().getRGB()).substring(2)+">" + text + "*</col>";
				chat.setText(name + text);
			}else if(selected==1){
				text = " <col="+Integer.toHexString(config.clanColor().getRGB()).substring(2)+">" + text + "*</col>";
				chat.setText(name + text);
			}else if(selected==2){
				text = " <col="+Integer.toHexString(config.friendColor().getRGB()).substring(2)+">" + text + "*</col>";
				chat.setText(name + text);
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

	@Provides
	InputRecolorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InputRecolorConfig.class);
	}
}
