package com.smartchatinputcolor;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
enum ChatPanel {
    NONE(1337),
    ALL(0),
    GAME(1),
    PUBLIC(2),
    PRIVATE(3),
    CHANNEL(4),
    CLAN(5),
    TRADE_OR_GIM(6);

    private final int varClientIntValue;

    public static ChatPanel fromVarClientInt(int varClientIntValue) {
        for (ChatPanel chatPanel : ChatPanel.values()) {
            if (chatPanel.varClientIntValue == varClientIntValue) {
                return chatPanel;
            }
        }
        return NONE;
    }
}
