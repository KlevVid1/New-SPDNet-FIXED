package com.shatteredpixel.shatteredpixeldungeon.spdnet.web;

import static com.watabou.utils.DeviceCompat.isDebug;
import static com.watabou.utils.DeviceCompat.isDesktop;

import com.shatteredpixel.shatteredpixeldungeon.spdnet.NetConfig;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.NetSettings;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.Player;
import com.watabou.noosa.Game;

import java.net.URISyntaxException;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import io.socket.client.IO;
import io.socket.client.Socket;
import lombok.Getter;

/**
 * 此类用于处理网络连接
 */
public class Net {
	static private Socket socket;
	// 服务器地址 (по умолчанию локальный адрес, полностью отвязано от внешних серверов)
	@Getter
	private static String serverUrl = "http://127.0.0.1:32814/spdnet";
	// 服务器的种子列表
	public static ConcurrentHashMap<String, Long> seeds = new ConcurrentHashMap<>();
	// 玩家名
	public static String name = "";

	public static String getDisplayName() {
		if (name == null || name.isEmpty() || name.equals("未登录")) {
			boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
			boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
					|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
			return isRu ? "Не в сети" : (isZh ? "未登录" : "Offline");
		}
		return name;
	}
	// <PlayerKey{玩家名, 玩家权限}, 玩家状态>, 如果当前玩家没在游戏内, Status的层数为-1
	public static ConcurrentHashMap<String, Player> playerList = new ConcurrentHashMap<>();
	public static Vector<String> chatMessages = new Vector<>();
	@lombok.Setter
	public static String webUrl = "http://127.0.0.1:32814";

	/**
	 * 获取一个socketIO对象
	 *
	 * @return socketIO对象
	 */
	public static Socket getSocket() {
		if (serverUrl.isEmpty()) {
			refreshServerUrl();
		}
		if (socket == null) {
			try {
				IO.Options opts = new IO.Options();
				opts.reconnection = true;
				opts.reconnectionAttempts = 10;
				opts.reconnectionDelay = 1000;
				opts.timeout = 20000;
				opts.transports = new String[] { "websocket" };
				String clientName = (name != null && !name.isEmpty() && !name.equals("未登录") && !name.equals("Не авторизован") && !name.equals("Not logged in")) ? name : NetSettings.getName();
				String clientPassword = NetSettings.getPassword();
				if (clientPassword == null) clientPassword = "";
				String encName = java.net.URLEncoder.encode(clientName, "UTF-8");
				String encPass = java.net.URLEncoder.encode(clientPassword, "UTF-8");
				String encVer = java.net.URLEncoder.encode(Game.version != null ? Game.version : "4.0.0", "UTF-8");
				String encNetVer = java.net.URLEncoder.encode(Game.netVersion != null ? Game.netVersion : "2.3", "UTF-8");
				opts.query = "name=" + encName + "&password=" + encPass + "&SPDVersion=" + encVer + "&NetVersion=" + encNetVer;
				socket = IO.socket(serverUrl, opts);
			} catch (Throwable e) {
				com.shatteredpixel.shatteredpixeldungeon.spdnet.utils.NLog.w("Net.getSocket error: " + e.getMessage());
				throw new RuntimeException("Ошибка создания сокета: " + e.getMessage(), e);
			}
		}
		return socket;
	}

	public static boolean hasSocket() {
		return socket != null;
	}

	public static void connect() {
		Receiver.startAll();
		getSocket().connect();
	}

	public static void disConnect() {
		try {
			Receiver.cancelAll();
		} catch (Throwable ignored) {}
		try {
			if (socket != null) {
				socket.disconnect();
			}
		} catch (Throwable ignored) {}
	}

	/**
	 * 获取一个已经连接的socketIO对象，如果当前没有连接，那么就尝试连接
	 *
	 * @return 连接的socketIO对象
	 */
	public static Socket getConnectedSocket() {
		if (!isConnected()) {
			connect();
		}
		return getSocket();
	}

	/**
	 * 切换socket连接状态
	 */
	public static void toggleSocket() {
		if (!isConnected() && !getSocket().io().isReconnecting()) {
			connect();
		} else {
			disConnect();
		}
	}

	/**
	 * 完全销毁所有socket相关的对象，重置连接
	 */
	public static void destroySocket() {
		if (socket != null) {
			try {
				if (isConnected()) {
					disConnect();
				}
				socket.off();
			} catch (Exception ignored) {
			}
			socket = null;
		}
	}

	/**
	 * Подключение к заданному серверу с заданным именем игрока (для LAN-коопа и локальных серверов)
	 */
	public static void connectTo(String url, String nickname) {
		try {
			destroySocket();
			serverUrl = url;
			name = nickname;
			connect();
		} catch (Throwable t) {
			com.shatteredpixel.shatteredpixeldungeon.spdnet.utils.NLog.w("Net.connectTo failed: " + t.getMessage());
			t.printStackTrace();
			com.shatteredpixel.shatteredpixeldungeon.spdnet.windows.NetWindow.error("Ошибка сети: " + t.getMessage());
		}
	}

	/**
	 * 刷新服务器地址
	 */
	public static void refreshServerUrl() {
		if (serverUrl == null || serverUrl.isEmpty()) {
			serverUrl = "http://127.0.0.1:32814/spdnet";
		}
	}

	public static boolean isConnected() {
		return socket != null && socket.connected();
	}

	/**
	 * 重置从服务器获取的状态信息
	 */
	public static void reset() {
		seeds.clear();
		name = "";
	}

	public static void setServerUrl(String serverUrl) {
		Net.serverUrl = serverUrl;
	}
}