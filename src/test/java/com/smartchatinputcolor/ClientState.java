package com.smartchatinputcolor;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClientState {
    private boolean groupIronman;

    private boolean inFriendsChat;

    private SlashSwapperMode slashSwapperMode;

    private ChatPanel openChatPanel;

    private ChatChannel chatMode;
}
