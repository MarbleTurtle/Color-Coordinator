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
	TRADE(6);

	private final int id;

	public static ChatPanel fromVarcIntValue(int value) {
		for (ChatPanel chatPanel : ChatPanel.values()) {
			if (chatPanel.id == value) {
				return chatPanel;
			}
		}
		return NONE;
	}
}
