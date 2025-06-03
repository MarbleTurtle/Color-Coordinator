package com.smartchatinputcolor;

import net.runelite.api.annotations.VarCInt;

@SuppressWarnings("MagicConstant")
public class VarClientInt {

    /**
     * Currently open chat panel
     * @see ChatPanel
     */
    @VarCInt
    static final int OPEN_CHAT_PANEL = 41;

    /**
     * Currently active chat mode
     * @see ChatChannel#fromChatModeVarClientInt(int)
     */
    @VarCInt
    static final int ACTIVE_CHAT_MODE = 945;
}
