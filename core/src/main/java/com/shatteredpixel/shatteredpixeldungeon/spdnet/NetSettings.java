package com.shatteredpixel.shatteredpixeldungeon.spdnet;

import static com.watabou.utils.GameSettings.getString;
import static com.watabou.utils.GameSettings.put;

/**
 * 用来存储某些长期保存的变量
 */
public class NetSettings {
	public static final String KEY_AUTH_NAME = "net_auth_name";
	public static final String KEY_AUTH_PASSWORD = "net_auth_password";

	public static void setName(String value) {
		put(KEY_AUTH_NAME, value);
	}

	public static String getName() {
		return getString(KEY_AUTH_NAME, "");
	}

	public static void setPassword(String value) {
		put(KEY_AUTH_PASSWORD, value);
	}

	public static String getPassword() {
		return getString(KEY_AUTH_PASSWORD, "");
	}

	public static boolean hasCredentials() {
		return !getName().isEmpty() && !getPassword().isEmpty();
	}

	public static final String KEY_LAN_NAME = "net_lan_name";
	public static final String KEY_LAN_HOST_IP = "net_lan_host_ip";
	public static final String KEY_LAN_PORT = "net_lan_port";

	public static void setLanName(String value) {
		put(KEY_LAN_NAME, value);
	}

	public static String getLanName() {
		String lanName = getString(KEY_LAN_NAME, "");
		if (lanName.isEmpty()) {
			lanName = getName();
		}
		if (lanName.isEmpty()) {
			lanName = "Player";
		}
		return lanName;
	}

	public static void setLanHostIp(String value) {
		put(KEY_LAN_HOST_IP, value);
	}

	public static String getLanHostIp() {
		return getString(KEY_LAN_HOST_IP, "192.168.43.1");
	}

	public static void setLanPort(int value) {
		put(KEY_LAN_PORT, value);
	}

	public static int getLanPort() {
		return com.watabou.utils.GameSettings.getInt(KEY_LAN_PORT, 32814);
	}

	public static void clearCredentials() {
		put(KEY_AUTH_NAME, "");
		put(KEY_AUTH_PASSWORD, "");
	}
}
