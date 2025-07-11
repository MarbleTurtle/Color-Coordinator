package com.smartchatinputcolor;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.GameState;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SmartChatInputColorPluginTest {
    @Inject
    private SmartChatInputColorPlugin smartChatInputColorPlugin;

    @Mock
    @Bind
    private Client client;

    @Mock
    @Bind
    private ConfigManager configManager;

    @Mock
    @Bind
    private PluginManager pluginManager;

    @Before
    public void before() {
        Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
    }

    private void setupState(ClientState state) {
        // Mock the client

        // Currently opened chat panel
        when(client.getVarcIntValue(VarClientInt.OPEN_CHAT_PANEL))
            .thenReturn(state.getOpenChatPanel().getVarClientIntValue());

        // In a friends chat or not?
        when(client.getFriendsChatManager())
            .thenReturn(state.isInFriendsChat() ? mock(FriendsChatManager.class) : null);

        // Current chat mode, ordinals are the same as the var client int value
        when(client.getVarcIntValue(VarClientInt.ACTIVE_CHAT_MODE))
            .thenReturn(state.getChatMode().ordinal());

        // Group Ironman account
        when(client.getVarbitValue(VarbitID.IRONMAN)).thenReturn(state.isGroupIronman() ? 4 : 0);

        // Always logged in while testing
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);

        // Mock client color settings, always return unset value
        //noinspection MagicConstant
        when(client.getVarpValue(anyInt())).thenReturn(0);

        // Using a transparent chatbox, doesn't matter for current testing
        // since we only test the channel output, not the actual color
        when(client.isResized()).thenReturn(true);
        when(client.getVarbitValue(VarbitID.CHATBOX_TRANSPARENCY)).thenReturn(1);


        // Mock the plugin manager

        // Slash swapper plugin
        Plugin slashSwapperMock = mock(Plugin.class);
        when(slashSwapperMock.getName()).thenReturn("Slash Swapper");
        when(pluginManager.getPlugins()).thenReturn(List.of(slashSwapperMock));
        when(pluginManager.isPluginEnabled(slashSwapperMock))
            .thenReturn(state.getSlashSwapperMode() != SlashSwapperMode.OFF);


        // Mock the config manager

        // Slash swapper swapping clan and guest chat
        when(configManager.getConfiguration("slashswapper", "slashGuestChat", boolean.class))
            .thenReturn(state.getSlashSwapperMode() == SlashSwapperMode.ON_SWAP_GUEST_CHAT);

        // Runelite chat color config
        when(configManager.getConfiguration(eq("textrecolor"), anyString(), eq(Color.class)))
            .thenReturn(mock(Color.class));

        // Initialize the plugin

        // Start the plugin and fake a game tick to initialize the plugin

        smartChatInputColorPlugin.startUp();

        // Fake a game tick to let plugin initialize
        smartChatInputColorPlugin.onGameTick(new GameTick());
    }

    //region Assertion functions
    private void assertCorrectChatChannel(String input, ChatChannel expectedChannel) {
        assertEquals(expectedChannel, smartChatInputColorPlugin.deriveChatChannel(input));
    }

    private void assertPrefix(String prefix, ChatChannel expectedChannel) {
        // Prefix without @ requires a space after the prefix
        assertCorrectChatChannel("/" + prefix + " test", expectedChannel);
    }

    private void assertAtPrefix(String prefix, ChatChannel expectedChannel) {
        String fullPrefix = "/@" + prefix;
        // Prefix with @ can optionally have a space
        assertCorrectChatChannel(fullPrefix + "test", expectedChannel);
        assertCorrectChatChannel(fullPrefix + " test", expectedChannel);
    }

    private void assertAllPrefixes(String prefix, ChatChannel expectedChannel) {
        assertPrefix(prefix, expectedChannel);
        assertAtPrefix(prefix, expectedChannel);
    }

    private void assertSlash(int slashCount, ChatChannel expectedChannel) {
        assertCorrectChatChannel("/".repeat(slashCount) + "test", expectedChannel);
    }
    //endregion

    //region Non-GIM not in friends chat
    @Test
    public void test() {
        setupState(new ClientState(
            false,
            false,
            SlashSwapperMode.OFF,
            ChatPanel.ALL,
            ChatChannel.PUBLIC
        ));

        assertSlash(0, ChatChannel.PUBLIC);
        assertAllPrefixes("p", ChatChannel.PUBLIC);

        // Messages meant for friends chat go to public when not in a friends chat
        assertSlash(1, ChatChannel.PUBLIC);
        assertAllPrefixes("f", ChatChannel.PUBLIC);

        assertSlash(2, ChatChannel.CLAN);
        assertAllPrefixes("c", ChatChannel.CLAN);

        assertSlash(3, ChatChannel.GUEST);
        assertAllPrefixes("gc", ChatChannel.GUEST);

        // 4 slashes goes to guest chat when not on a GIM account
        assertSlash(4, ChatChannel.GUEST);
        // /g goes to friends chat when not on a GIM account, which goes to public when not in a friends chat
        assertPrefix("g", ChatChannel.PUBLIC);
        // For some reason, /@g goes to clan chat when not on a GIM account
        assertAtPrefix("g", ChatChannel.CLAN);
    }

    @Test
    public void testSlashSwapper() {
        setupState(new ClientState(
            false,
            false,
            SlashSwapperMode.ON,
            ChatPanel.ALL,
            ChatChannel.PUBLIC
        ));

        assertSlash(0, ChatChannel.PUBLIC);
        assertAllPrefixes("p", ChatChannel.PUBLIC);

        // Slash swapper sends / to clan chat
        assertSlash(1, ChatChannel.CLAN);
        assertAllPrefixes("f", ChatChannel.PUBLIC);

        // Slash swapper sends // to friends chat, which goes to public without friends chat
        assertSlash(2, ChatChannel.PUBLIC);
        assertAllPrefixes("c", ChatChannel.CLAN);

        assertSlash(3, ChatChannel.GUEST);
        assertAllPrefixes("gc", ChatChannel.GUEST);

        assertSlash(4, ChatChannel.GUEST);
        // /g goes to FC on a non-GIM, but Slash swapper makes it go to clan chat
        assertPrefix("g", ChatChannel.CLAN);
        // For some reason, /@g goes to clan chat when not on a GIM account
        assertAtPrefix("g", ChatChannel.CLAN);
    }

    @Test
    public void testSlashSwapperSwapGuestChat() {
        setupState(new ClientState(
            false,
            false,
            SlashSwapperMode.ON_SWAP_GUEST_CHAT,
            ChatPanel.ALL,
            ChatChannel.PUBLIC
        ));

        assertSlash(0, ChatChannel.PUBLIC);
        assertAllPrefixes("p", ChatChannel.PUBLIC);

        // Slash swapper + swap guest chat sends / to guest chat
        assertSlash(1, ChatChannel.GUEST);
        assertAllPrefixes("f", ChatChannel.PUBLIC);

        // Slash swapper sends // to friends chat, which goes to public without friends chat
        assertSlash(2, ChatChannel.PUBLIC);
        assertAllPrefixes("c", ChatChannel.CLAN);

        // Slash swapper + swap guest chat sends /// to clan chat
        assertSlash(3, ChatChannel.CLAN);
        assertAllPrefixes("gc", ChatChannel.GUEST);

        // Slash swapper + swap guest chat makes //// go to clan chat on a non-GIM account
        assertSlash(4, ChatChannel.CLAN);
        // /g goes to FC on a non-GIM, but Slash swapper makes it go to guest chat
        assertPrefix("g", ChatChannel.GUEST);
        // For some reason, /@g goes to clan chat when not on a GIM account
        assertAtPrefix("g", ChatChannel.CLAN);
    }
    //endregion

    //region Non-GIM in friends chat
    @Test
    public void testFriendsChat() {
        setupState(new ClientState(
            false,
            true,
            SlashSwapperMode.OFF,
            ChatPanel.ALL,
            ChatChannel.PUBLIC
        ));

        assertSlash(0, ChatChannel.PUBLIC);
        assertAllPrefixes("p", ChatChannel.PUBLIC);

        assertSlash(1, ChatChannel.FRIEND);
        assertAllPrefixes("f", ChatChannel.FRIEND);

        assertSlash(2, ChatChannel.CLAN);
        assertAllPrefixes("c", ChatChannel.CLAN);

        assertSlash(3, ChatChannel.GUEST);
        assertAllPrefixes("gc", ChatChannel.GUEST);

        // 4 slashes goes to guest chat when not on a GIM account
        assertSlash(4, ChatChannel.GUEST);
        // /g goes to friends chat when not on a GIM account
        assertPrefix("g", ChatChannel.FRIEND);
        // For some reason, /@g goes to clan chat when not on a GIM account
        assertAtPrefix("g", ChatChannel.CLAN);
    }

    @Test
    public void testFriendsChatSlashSwapper() {
        setupState(new ClientState(
            false,
            true,
            SlashSwapperMode.ON,
            ChatPanel.ALL,
            ChatChannel.PUBLIC
        ));

        assertSlash(0, ChatChannel.PUBLIC);
        assertAllPrefixes("p", ChatChannel.PUBLIC);

        // Slash swapper sends / to clan chat
        assertSlash(1, ChatChannel.CLAN);
        assertAllPrefixes("f", ChatChannel.FRIEND);

        // Slash swapper sends // to friends chat
        assertSlash(2, ChatChannel.FRIEND);
        assertAllPrefixes("c", ChatChannel.CLAN);

        assertSlash(3, ChatChannel.GUEST);
        assertAllPrefixes("gc", ChatChannel.GUEST);

        assertSlash(4, ChatChannel.GUEST);
        // /g goes to FC on a non-GIM, but Slash swapper makes it go to clan chat
        assertPrefix("g", ChatChannel.CLAN);
        // For some reason, /@g goes to clan chat when not on a GIM account
        assertAtPrefix("g", ChatChannel.CLAN);
    }

    @Test
    public void testFriendsChatSlashSwapperSwapGuestChat() {
        setupState(new ClientState(
            false,
            true,
            SlashSwapperMode.ON_SWAP_GUEST_CHAT,
            ChatPanel.ALL,
            ChatChannel.PUBLIC
        ));

        assertSlash(0, ChatChannel.PUBLIC);
        assertAllPrefixes("p", ChatChannel.PUBLIC);

        // Slash swapper + swap guest chat sends / to guest chat
        assertSlash(1, ChatChannel.GUEST);
        assertAllPrefixes("f", ChatChannel.FRIEND);

        // Slash swapper sends // to friends chat
        assertSlash(2, ChatChannel.FRIEND);
        assertAllPrefixes("c", ChatChannel.CLAN);

        // Slash swapper + swap guest chat sends /// to clan chat
        assertSlash(3, ChatChannel.CLAN);
        assertAllPrefixes("gc", ChatChannel.GUEST);

        // Slash swapper + swap guest chat makes //// go to clan chat on a non-GIM account
        assertSlash(4, ChatChannel.CLAN);
        // /g goes to FC on a non-GIM, but Slash swapper makes it go to guest chat
        assertPrefix("g", ChatChannel.GUEST);
        // For some reason, /@g goes to clan chat when not on a GIM account
        assertAtPrefix("g", ChatChannel.CLAN);
    }
    //endregion

    //region GIM not in friends chat
    @Test
    public void testGroupIronman() {
        setupState(new ClientState(
            true,
            false,
            SlashSwapperMode.OFF,
            ChatPanel.ALL,
            ChatChannel.PUBLIC
        ));

        assertSlash(0, ChatChannel.PUBLIC);
        assertAllPrefixes("p", ChatChannel.PUBLIC);

        assertSlash(1, ChatChannel.PUBLIC);
        assertAllPrefixes("f", ChatChannel.PUBLIC);

        assertSlash(2, ChatChannel.CLAN);
        assertAllPrefixes("c", ChatChannel.CLAN);

        assertSlash(3, ChatChannel.GUEST);
        assertAllPrefixes("gc", ChatChannel.GUEST);

        assertSlash(4, ChatChannel.GIM);
        assertAllPrefixes("g", ChatChannel.GIM);
    }

    @Test
    public void testGroupIronmanSlashSwapper() {
        setupState(new ClientState(
            true,
            false,
            SlashSwapperMode.ON,
            ChatPanel.ALL,
            ChatChannel.PUBLIC
        ));

        assertSlash(0, ChatChannel.PUBLIC);
        assertAllPrefixes("p", ChatChannel.PUBLIC);

        // Slash swapper sends / to clan chat
        assertSlash(1, ChatChannel.CLAN);
        assertAllPrefixes("f", ChatChannel.PUBLIC);

        // Slash swapper sends // to friends chat, which goes to public when not in a friends chat
        assertSlash(2, ChatChannel.PUBLIC);
        assertAllPrefixes("c", ChatChannel.CLAN);

        assertSlash(3, ChatChannel.GUEST);
        assertAllPrefixes("gc", ChatChannel.GUEST);

        assertSlash(4, ChatChannel.GIM);
        assertAllPrefixes("g", ChatChannel.GIM);
    }

    @Test
    public void testGroupIronmanSlashSwapperSwappedGuestChat() {
        setupState(new ClientState(
            true,
            false,
            SlashSwapperMode.ON_SWAP_GUEST_CHAT,
            ChatPanel.ALL,
            ChatChannel.PUBLIC
        ));

        assertSlash(0, ChatChannel.PUBLIC);
        assertAllPrefixes("p", ChatChannel.PUBLIC);

        // Slash swapper + swap guest chat sends / to guest chat
        assertSlash(1, ChatChannel.GUEST);
        assertAllPrefixes("f", ChatChannel.PUBLIC);

        // Slash swapper sends // to friends chat, which goes to public when not in a friends chat
        assertSlash(2, ChatChannel.PUBLIC);
        assertAllPrefixes("c", ChatChannel.CLAN);

        // Slash swapper + swap guest chat sends /// to clan chat
        assertSlash(3, ChatChannel.CLAN);
        assertAllPrefixes("gc", ChatChannel.GUEST);

        assertSlash(4, ChatChannel.GIM);
        assertAllPrefixes("g", ChatChannel.GIM);
    }
    //endregion

    //region GIM in friends chat
    @Test
    public void testGroupIronmanFriendsChat() {
        setupState(new ClientState(
            true,
            true,
            SlashSwapperMode.OFF,
            ChatPanel.ALL,
            ChatChannel.PUBLIC
        ));

        assertSlash(0, ChatChannel.PUBLIC);
        assertAllPrefixes("p", ChatChannel.PUBLIC);

        assertSlash(1, ChatChannel.FRIEND);
        assertAllPrefixes("f", ChatChannel.FRIEND);

        assertSlash(2, ChatChannel.CLAN);
        assertAllPrefixes("c", ChatChannel.CLAN);

        assertSlash(3, ChatChannel.GUEST);
        assertAllPrefixes("gc", ChatChannel.GUEST);

        assertSlash(4, ChatChannel.GIM);
        assertAllPrefixes("g", ChatChannel.GIM);
    }

    @Test
    public void testGroupIronmanFriendsChatSlashSwapper() {
        setupState(new ClientState(
            true,
            true,
            SlashSwapperMode.ON,
            ChatPanel.ALL,
            ChatChannel.PUBLIC
        ));

        assertSlash(0, ChatChannel.PUBLIC);
        assertAllPrefixes("p", ChatChannel.PUBLIC);

        // Slash swapper sends / to clan chat
        assertSlash(1, ChatChannel.CLAN);
        assertAllPrefixes("f", ChatChannel.FRIEND);

        // Slash swapper sends // to friends chat
        assertSlash(2, ChatChannel.FRIEND);
        assertAllPrefixes("c", ChatChannel.CLAN);

        assertSlash(3, ChatChannel.GUEST);
        assertAllPrefixes("gc", ChatChannel.GUEST);

        assertSlash(4, ChatChannel.GIM);
        assertAllPrefixes("g", ChatChannel.GIM);
    }

    @Test
    public void testGroupIronmanFriendsChatSlashSwapperSwappedGuestChat() {
        setupState(new ClientState(
            true,
            true,
            SlashSwapperMode.ON_SWAP_GUEST_CHAT,
            ChatPanel.ALL,
            ChatChannel.PUBLIC
        ));

        assertSlash(0, ChatChannel.PUBLIC);
        assertAllPrefixes("p", ChatChannel.PUBLIC);

        // Slash swapper + swap guest chat sends / to guest chat
        assertSlash(1, ChatChannel.GUEST);
        assertAllPrefixes("f", ChatChannel.FRIEND);

        // Slash swapper sends // to friends chat
        assertSlash(2, ChatChannel.FRIEND);
        assertAllPrefixes("c", ChatChannel.CLAN);

        // Slash swapper + swap guest chat sends /// to clan chat
        assertSlash(3, ChatChannel.CLAN);
        assertAllPrefixes("gc", ChatChannel.GUEST);

        assertSlash(4, ChatChannel.GIM);
        assertAllPrefixes("g", ChatChannel.GIM);
    }
    //endregion

    @Test
    public void testClanTabInFriendsChatMode() {
        setupState(new ClientState(
            false,
            false,
            SlashSwapperMode.OFF,
            ChatPanel.CLAN,
            ChatChannel.FRIEND
        ));

        assertSlash(0, ChatChannel.CLAN);
    }

    @Test
    public void testLeaveFriendChatAfterSettingFriendChatMode() {
        setupState(new ClientState(
            false,
            false,
            SlashSwapperMode.OFF,
            ChatPanel.ALL,
            ChatChannel.FRIEND
        ));

        // This message does not go anywhere but the plugin will pretend it goes to public chat to give it a color
        assertSlash(0, ChatChannel.PUBLIC);
    }

    public static void main(String[] args) throws Exception {
        @SuppressWarnings("unchecked")
        var plugins = (Class<? extends Plugin>[]) new Class[]{SmartChatInputColorPlugin.class};
        ExternalPluginManager.loadBuiltin(plugins);
        RuneLite.main(args);
    }
}
