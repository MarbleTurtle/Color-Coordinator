package com.smartchatinputcolor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.VarPlayer;

import java.util.regex.Pattern;

@AllArgsConstructor
@Getter
enum ChatChannel {
	PUBLIC("PublicChat",
			VarPlayer.SETTINGS_TRANSPARENT_CHAT_PUBLIC,
			VarPlayer.SETTINGS_OPAQUE_CHAT_PUBLIC,
			0x9090FF,
			0x0000FF,
			Pattern.compile("^/@?p.*")),
	FRIEND("ClanChatMessage",
			VarPlayer.SETTINGS_TRANSPARENT_CHAT_FRIEND,
			VarPlayer.SETTINGS_OPAQUE_CHAT_FRIEND,
			0xEF5050,
			0x7F0000,
			Pattern.compile("^/(@?f)?.*")),
	CLAN(
			"ClanMessage",
			VarPlayer.SETTINGS_TRANSPARENT_CHAT_CLAN,
			VarPlayer.SETTINGS_OPAQUE_CHAT_CLAN,
			0x7F0000,
			0x7F0000,
			Pattern.compile("^/(@?c|/).*")),
	GUEST(
			"ClanGuestMessage",
			VarPlayer.SETTINGS_TRANSPARENT_CHAT_GUEST_CLAN,
			VarPlayer.SETTINGS_OPAQUE_CHAT_GUEST_CLAN,
			0x7F0000,
			0x7F0000,
			Pattern.compile("^/(@?gc|//).*")),
	GIM(
			null,
			VarPlayer.SETTINGS_TRANSPARENT_CHAT_IRON_GROUP_CHAT,
			VarPlayer.SETTINGS_OPAQUE_CHAT_IRON_GROUP_CHAT,
			0x7F0000,
			0x7F0000,
			Pattern.compile("^/(@?g|/{3}).*"));

	private final String colorConfigKey;
	private final VarPlayer transparentVarPId;
	private final VarPlayer opaqueVarPId;
	private final int transparentDefaultRgb;
	private final int opaqueDefaultRgb;
	private final Pattern regex;

	public boolean matchesRegex(String text) {
		return regex.matcher(text).matches();
	}
}