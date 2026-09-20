package com.shatteredpixel.shatteredpixeldungeon.spdnet.lan;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.utils.NLog;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.Player;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.Status;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.*;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.events.*;
import me.catand.spdnet.protocol.Actions;
import me.catand.spdnet.protocol.Events;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Встроенный легковесный сервер для локального мультиплеера (LAN Co-op).
 * Реализует протокол Engine.IO v3 / Socket.IO v2 поверх WebSocket.
 * Не требует внешних сервисов, баз данных или Spring Boot.
 */
public class LanServer {

	public static final int DEFAULT_PORT = 32814;

	private static WebSocketServer server;
	private static ScheduledExecutorService pingScheduler;
	private static int currentPort = DEFAULT_PORT;
	private static long currentSeed = 0;
	private static final Map<String, Long> seeds = new ConcurrentHashMap<>();
	private static final Map<WebSocket, LanSession> sessions = new ConcurrentHashMap<>();
	private static final Map<String, WebSocket> nameToConn = new ConcurrentHashMap<>();
	private static final Map<String, List<JSONObject>> notesByLevel = new ConcurrentHashMap<>();
	private static final Set<Integer> clearedEternalFiresByFloor = ConcurrentHashMap.newKeySet();
	private static int nextNoteId = 1;

	private static volatile String hostPlayerName = null;

	public static void setHostPlayerName(String name) {
		hostPlayerName = name;
	}

	public static String getHostPlayerName() {
		return hostPlayerName;
	}

	public static WebSocket getConnByName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return null;
		}
		String target = name.trim();
		WebSocket conn = nameToConn.get(target);
		if (conn != null && conn.isOpen()) {
			return conn;
		}
		for (Map.Entry<WebSocket, LanSession> entry : sessions.entrySet()) {
			WebSocket ws = entry.getKey();
			LanSession s = entry.getValue();
			if (s != null && s.name != null && ws != null && ws.isOpen()) {
				if (target.equalsIgnoreCase(s.name.trim())) {
					nameToConn.put(s.name, ws);
					return ws;
				}
			}
		}
		return null;
	}

	public static class LanSession {
		public final WebSocket conn;
		public final String name;
		public final String sid;
		public Status status;

		public LanSession(WebSocket conn, String name, String sid) {
			this.conn = conn;
			this.name = name;
			this.sid = sid;
		}
	}

	public static synchronized boolean isRunning() {
		return server != null;
	}

	public static synchronized int getPort() {
		return currentPort;
	}

	public static synchronized long getSeed() {
		return currentSeed;
	}

	public static synchronized void updateSeed(long newSeed) {
		if (newSeed == 0 || newSeed == currentSeed) {
			return;
		}
		currentSeed = newSeed;
		seeds.put("seedFUN", newSeed);
		NLog.i("LanServer: seed updated to " + newSeed);
		for (WebSocket conn : sessions.keySet()) {
			LanSession session = sessions.get(conn);
			if (session != null && conn.isOpen()) {
				SInit sInit = new SInit(session.name, "Локальный сервер SPDNet", seeds, new HashSet<>());
				emit(conn, Events.INIT.getName(), sInit);
			}
		}
	}

	public static synchronized int getPlayerCount() {
		return sessions.size();
	}

	public static synchronized List<String> getPlayerNames() {
		List<String> names = new ArrayList<>();
		for (LanSession s : sessions.values()) {
			names.add(s.name);
		}
		return names;
	}

	public static synchronized void start(int port) throws IOException {
		if (server != null) {
			stop();
		}

		currentPort = port;
		if (com.shatteredpixel.shatteredpixeldungeon.Dungeon.seed != 0) {
			currentSeed = com.shatteredpixel.shatteredpixeldungeon.Dungeon.seed;
		} else {
			currentSeed = DungeonSeed.randomSeed();
		}
		seeds.clear();
		seeds.put("seedFUN", currentSeed);
		sessions.clear();
		nameToConn.clear();
		clearedEternalFiresByFloor.clear();

		InetSocketAddress address = new InetSocketAddress("0.0.0.0", currentPort);
		final java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
		server = new WebSocketServer(address) {
			@Override
			public void onOpen(WebSocket conn, ClientHandshake handshake) {
				handleOpen(conn, handshake);
			}

			@Override
			public void onClose(WebSocket conn, int code, String reason, boolean remote) {
				handleClose(conn);
			}

			@Override
			public void onMessage(WebSocket conn, String message) {
				handleMessage(conn, message);
			}

			@Override
			public void onError(WebSocket conn, Exception ex) {
				NLog.w("LanServer error: " + (ex != null ? ex.getMessage() : "unknown"));
			}

			@Override
			public void onStart() {
				NLog.i("LanServer started on port " + currentPort);
				startLatch.countDown();
			}
		};

		server.setReuseAddr(true);
		server.setTcpNoDelay(true);
		server.setConnectionLostTimeout(60);
		server.start();

		try {
			startLatch.await(1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ignored) {}

		// Запуск периодического Engine.IO ping для предотвращения ping-timeout на клиенте
		pingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "LanServer-Heartbeat");
			t.setDaemon(true);
			return t;
		});
		pingScheduler.scheduleAtFixedRate(() -> {
			try {
				if (server != null) {
					for (WebSocket conn : server.getConnections()) {
						if (conn != null && conn.isOpen()) {
							try {
								conn.send("2");
							} catch (Exception ignored) {
							}
						}
					}
				}
			} catch (Exception ignored) {
			}
		}, 15, 15, TimeUnit.SECONDS);
	}

	public static synchronized void stop() {
		hostPlayerName = null;
		if (pingScheduler != null) {
			try {
				pingScheduler.shutdownNow();
			} catch (Exception ignored) {
			}
			pingScheduler = null;
		}
		if (server != null) {
			try {
				for (WebSocket conn : server.getConnections()) {
					try {
						conn.close();
					} catch (Exception ignored) {
					}
				}
				server.stop(500);
			} catch (Exception e) {
				NLog.w("LanServer stop error: " + e.getMessage());
			} finally {
				server = null;
				sessions.clear();
				nameToConn.clear();
		clearedEternalFiresByFloor.clear();
				seeds.clear();
			}
			NLog.i("LanServer stopped");
		}
	}

	private static void handleOpen(WebSocket conn, ClientHandshake handshake) {
		String descriptor = handshake.getResourceDescriptor();
		Map<String, String> queryParams = parseQueryParams(descriptor);
		String playerName = queryParams.getOrDefault("name", "Player_" + (sessions.size() + 1));

		String sid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		LanSession session = new LanSession(conn, playerName, sid);
		sessions.put(conn, session);
		nameToConn.put(playerName, conn);

		// Engine.IO v3/v4 open packet
		String openPacket = "0{\"sid\":\"" + sid + "\",\"upgrades\":[],\"pingInterval\":15000,\"pingTimeout\":30000,\"maxPayload\":1000000}";
		try {
			conn.send(openPacket);
		} catch (Exception ignored) {
		}
	}

	private static void handleClose(WebSocket conn) {
		LanSession session = sessions.remove(conn);
		if (session != null) {
			nameToConn.remove(session.name, conn);
			// Оповестить остальных об уходе игрока
			broadcast(Events.EXIT.getName(), new SExit(session.name, ""), conn);
			NLog.i("LanServer: player disconnected: " + session.name);
		}
	}

	private static void handleMessage(WebSocket conn, String message) {
		if (message == null || message.isEmpty()) {
			return;
		}

		// Engine.IO ping/pong
		if (message.equals("2") || message.startsWith("2")) {
			try {
				conn.send("3" + (message.length() > 1 ? message.substring(1) : ""));
			} catch (Exception ignored) {
			}
			return;
		}

		if (message.equals("3") || message.startsWith("3")) {
			return;
		}

		// Socket.IO namespace connection
		if (message.startsWith("40")) {
			LanSession session = sessions.get(conn);
			String sid = session != null ? session.sid : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
			try {
				if (message.contains("/spdnet")) {
					conn.send("40/spdnet,{\"sid\":\"" + sid + "\"}");
				} else {
					conn.send("40{\"sid\":\"" + sid + "\"}");
				}
			} catch (Exception ignored) {
			}

			if (session != null) {
				// 1. Отправляем INIT с сидом и именем
				SInit sInit = new SInit(session.name, "Локальный сервер SPDNet", seeds, new HashSet<>());
				emit(conn, Events.INIT.getName(), sInit);

				// 2. Отправляем PLAYER_LIST
				SPlayerList sPlayerList = new SPlayerList(collectAllPlayers());
				emit(conn, Events.PLAYER_LIST.getName(), sPlayerList);

				// 3. Рассылаем JOIN остальным игрокам
				SJoin sJoin = new SJoin(session.name, "PLAYER", "");
				broadcast(Events.JOIN.getName(), sJoin, conn);
			}
			return;
		}

		// Socket.IO event: 42/spdnet,["eventName", data]
		if (message.startsWith("42/spdnet,")) {
			String payload = message.substring("42/spdnet,".length());
			processSocketIoEvent(conn, payload);
		} else if (message.startsWith("42,")) {
			String payload = message.substring("42,".length());
			processSocketIoEvent(conn, payload);
		}
	}

	private static void processSocketIoEvent(WebSocket conn, String payload) {
		try {
			JSONArray array = JSON.parseArray(payload);
			if (array == null || array.isEmpty()) {
				return;
			}
			String actionName = array.getString(0);
			String dataStr = array.size() > 1 ? array.getString(1) : "{}";

			LanSession session = sessions.get(conn);
			if (session == null) {
				return;
			}
			if (session.name != null && !session.name.isEmpty()) {
				nameToConn.put(session.name, conn);
			}

			if (Actions.ENTER_DUNGEON.getName().equals(actionName)) {
				CEnterDungeon c = JSON.parseObject(dataStr, CEnterDungeon.class);
				session.status = c.getStatus();
				boolean isHost = (session.name != null && session.name.equals(hostPlayerName)) ||
						(conn.getRemoteSocketAddress() != null &&
						conn.getRemoteSocketAddress().getAddress() != null &&
						conn.getRemoteSocketAddress().getAddress().isLoopbackAddress());
				if (isHost && c.getStatus() != null && c.getStatus().getSeed() != 0) {
					updateSeed(c.getStatus().getSeed());
				} else if (!isHost && c.getStatus() != null && c.getStatus().getSeed() != 0 && currentSeed != 0 && c.getStatus().getSeed() != currentSeed) {
					String warnMsg = "ВНИМАНИЕ: Сид игрока " + session.name + " (" + DungeonSeed.convertToCode(c.getStatus().getSeed()) + ") не совпадает с миром хоста (" + DungeonSeed.convertToCode(currentSeed) + ")! Уровни будут разными. Начните новую игру!";
					SChatMessage chatWarn = new SChatMessage("LanServer", warnMsg, String.valueOf(System.currentTimeMillis()));
					emit(conn, Events.CHAT_MESSAGE.getName(), chatWarn);
					WebSocket hostConn = nameToConn.get(hostPlayerName);
					if (hostConn != null && hostConn.isOpen()) {
						emit(hostConn, Events.CHAT_MESSAGE.getName(), chatWarn);
					}
					NLog.w("LanServer: Seed mismatch detected for " + session.name + ": " + c.getStatus().getSeed() + " vs " + currentSeed);
				}
				broadcast(Events.ENTER_DUNGEON.getName(), new SEnterDungeon(session.name, session.status, ""), conn);
				sendNotesForSession(conn, session);
				sendMobsForSession(conn, session);

			} else if (Actions.PLAYER_MOVE.getName().equals(actionName)) {
				CPlayerMove c = JSON.parseObject(dataStr, CPlayerMove.class);
				if (session.status != null) {
					session.status.setPos(c.getPos());
				}
				broadcastDungeon(Events.PLAYER_MOVE.getName(), new SPlayerMove(session.name, c.getPos(), ""), session, true);

			} else if (Actions.PLAYER_CHANGE_FLOOR.getName().equals(actionName)) {
				CPlayerChangeFloor c = JSON.parseObject(dataStr, CPlayerChangeFloor.class);
				if (session.status != null) {
					session.status.setDepth(c.getDepth());
				}
				broadcast(Events.PLAYER_CHANGE_FLOOR.getName(), new SPlayerChangeFloor(session.name, c.getDepth(), ""), conn);
				sendNotesForSession(conn, session);
				sendMobsForSession(conn, session);

			} else if (Actions.CHAT_MESSAGE.getName().equals(actionName)) {
				CChatMessage c = JSON.parseObject(dataStr, CChatMessage.class);
				SChatMessage s = new SChatMessage(session.name, c.getMessage(), c.getTime());
				broadcast(Events.CHAT_MESSAGE.getName(), s, null);

			} else if (Actions.GIVE_ITEM.getName().equals(actionName)) {
				CGiveItem c = JSON.parseObject(dataStr, CGiveItem.class);
				WebSocket targetConn = getConnByName(c.getTargetName());
				if (targetConn != null && targetConn.isOpen()) {
					emit(targetConn, Events.GIVE_ITEM.getName(), new SGiveItem(session.name, c.getItem(), ""));
				}

			} else if (Actions.FLOATING_TEXT.getName().equals(actionName)) {
				CFloatingText c = JSON.parseObject(dataStr, CFloatingText.class);
				SFloatingText s = new SFloatingText(session.name, c.getColor(), c.getText(), c.getIcon(),
						c.getHeroHP(), c.getHeroShield(), c.getHeroHT(), "");
				broadcastDungeon(Events.FLOATING_TEXT.getName(), s, session, false);

			} else if (Actions.ARMOR_UPDATE.getName().equals(actionName)) {
				CArmorUpdate c = JSON.parseObject(dataStr, CArmorUpdate.class);
				if (session.status != null) {
					session.status.setArmorTier(c.getArmorTier());
				}
				broadcastDungeon(Events.ARMOR_UPDATE.getName(), new SArmorUpdate(session.name, c.getArmorTier(), ""), session, false);

			} else if (Actions.VIEW_HERO.getName().equals(actionName)) {
				CViewHero c = JSON.parseObject(dataStr, CViewHero.class);
				NLog.i("LanServer: VIEW_HERO request from " + session.name + " for " + c.getTargetName());
				WebSocket targetConn = getConnByName(c.getTargetName());
				if (targetConn != null && targetConn.isOpen()) {
					emit(targetConn, Events.VIEW_HERO.getName(), new SViewHero(session.name, ""));
				} else {
					NLog.w("LanServer: target " + c.getTargetName() + " not found in nameToConn or active sessions");
				}

			} else if (Actions.HERO.getName().equals(actionName)) {
				CHero c = JSON.parseObject(dataStr, CHero.class);
				NLog.i("LanServer: HERO response from " + session.name + " to " + c.getSourceName());
				WebSocket targetConn = getConnByName(c.getSourceName());
				if (targetConn != null && targetConn.isOpen()) {
					emit(targetConn, Events.HERO.getName(), new SHero(session.name, c.getHero(), ""));
				} else {
					NLog.w("LanServer: inspector " + c.getSourceName() + " not found in nameToConn or active sessions");
				}

			} else if (Actions.ANKH_USED.getName().equals(actionName)) {
				CAnkhUsed c = JSON.parseObject(dataStr, CAnkhUsed.class);
				broadcast(Events.ANKH_USED.getName(), new SAnkhUsed(session.name, c.getCause(),
						c.getUnusedBlessedAnkh(), c.getUnusedUnblessedAnkh(), ""), conn);

			} else if (Actions.LEAVE_DUNGEON.getName().equals(actionName)) {
				session.status = null;
				broadcast(Events.LEAVE_DUNGEON.getName(), new SLeaveDungeon(session.name, ""), conn);

			} else if (Actions.GAME_END.getName().equals(actionName)) {
				CGameEnd c = JSON.parseObject(dataStr, CGameEnd.class);
				broadcast(Events.GAME_END.getName(), new SGameEnd(session.name, c.getRecord(), ""), conn);

			} else if (Actions.REQUEST_PLAYER_LIST.getName().equals(actionName)) {
				emit(conn, Events.PLAYER_LIST.getName(), new SPlayerList(collectAllPlayers()));

			} else if (Actions.REQUEST_LEADERBOARD.getName().equals(actionName)) {
				emit(conn, Events.LEADERBOARD.getName(), new SLeaderboard(0, 0, 0, new ArrayList<>()));

			} else if (Actions.ACHIEVEMENT.getName().equals(actionName)) {
				CAchievement c = JSON.parseObject(dataStr, CAchievement.class);
				broadcast(Events.ACHIEVEMENT.getName(), new SAchievement(session.name, c.getBadgeEnumString(), true, ""), conn);

			} else if (Actions.NOTE_CREATE.getName().equals(actionName)) {
				CNoteCreate c = JSON.parseObject(dataStr, CNoteCreate.class);
				int depth = session.status != null ? session.status.getDepth() : 1;
				long seed = session.status != null ? session.status.getSeed() : currentSeed;
				String key = seed + "_" + depth;

				JSONObject noteObj = new JSONObject();
				int noteId = nextNoteId++;
				noteObj.put("id", noteId);
				noteObj.put("noteType", c.getNoteType());
				noteObj.put("pos", c.getPos());
				noteObj.put("message", c.getMessage());
				noteObj.put("targetName", c.getTargetName());
				noteObj.put("snapshot", c.getSnapshot());
				noteObj.put("author", session.name);
				noteObj.put("authorMode", "FUN");
				noteObj.put("likes", 0);
				noteObj.put("createTime", System.currentTimeMillis());

				List<JSONObject> list = notesByLevel.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));
				list.add(noteObj);

				SNoteList deltaAdd = new SNoteList("DELTA_ADD", seed, depth, Collections.singletonList(noteObj.toJSONString()), new ArrayList<>());
				broadcast(Events.NOTE_LIST.getName(), deltaAdd, null);

			} else if (Actions.NOTE_LIKE.getName().equals(actionName)) {
				CNoteId c = JSON.parseObject(dataStr, CNoteId.class);
				int depth = session.status != null ? session.status.getDepth() : 1;
				long seed = session.status != null ? session.status.getSeed() : currentSeed;
				String key = seed + "_" + depth;
				List<JSONObject> list = notesByLevel.get(key);
				if (list != null) {
					synchronized (list) {
						for (JSONObject n : list) {
							if (n.getIntValue("id") == c.getId()) {
								n.put("likes", n.getIntValue("likes") + 1);
								SNoteList deltaAdd = new SNoteList("DELTA_ADD", seed, depth, Collections.singletonList(n.toJSONString()), new ArrayList<>());
								broadcast(Events.NOTE_LIST.getName(), deltaAdd, null);
								break;
							}
						}
					}
				}

			} else if (Actions.NOTE_DELETE.getName().equals(actionName)) {
				CNoteId c = JSON.parseObject(dataStr, CNoteId.class);
				int depth = session.status != null ? session.status.getDepth() : 1;
				long seed = session.status != null ? session.status.getSeed() : currentSeed;
				String key = seed + "_" + depth;
				List<JSONObject> list = notesByLevel.get(key);
				if (list != null) {
					synchronized (list) {
						list.removeIf(n -> n.getIntValue("id") == c.getId());
					}
					JSONObject removed = new JSONObject();
					removed.put("id", c.getId());
					SNoteList deltaRemove = new SNoteList("DELTA_REMOVE", seed, depth, Collections.singletonList(removed.toJSONString()), new ArrayList<>());
					broadcast(Events.NOTE_LIST.getName(), deltaRemove, null);
				}

			} else if (Actions.ITEM_DROP.getName().equals(actionName)) {
				CItemDrop c = JSON.parseObject(dataStr, CItemDrop.class);
				SItemDrop s = new SItemDrop(session.name, c.getDepth(), c.getPos(), c.getItem());
				broadcastDungeon(Events.ITEM_DROP.getName(), s, session, c.getDepth());

			} else if (Actions.ITEM_PICKUP.getName().equals(actionName)) {
				CItemPickUp c = JSON.parseObject(dataStr, CItemPickUp.class);
				SItemPickUp s = new SItemPickUp(session.name, c.getDepth(), c.getPos());
				broadcastDungeon(Events.ITEM_PICKUP.getName(), s, session, c.getDepth());

			} else if (Actions.TERRAIN_CHANGE.getName().equals(actionName)) {
				CTerrainChange c = JSON.parseObject(dataStr, CTerrainChange.class);
				STerrainChange s = new STerrainChange(session.name, c.getDepth(), c.getPos(), c.getTerrain());
				broadcastDungeon(Events.TERRAIN_CHANGE.getName(), s, session, c.getDepth());

			} else if (Actions.CHEST_OPEN.getName().equals(actionName)) {
				CChestOpen c = JSON.parseObject(dataStr, CChestOpen.class);
				SChestOpen s = new SChestOpen(session.name, c.getDepth(), c.getPos());
				broadcastDungeon(Events.CHEST_OPEN.getName(), s, session, c.getDepth());

			} else if (Actions.MOB_DAMAGE.getName().equals(actionName)) {
				CMobDamage c = JSON.parseObject(dataStr, CMobDamage.class);
				SMobDamage s = new SMobDamage(session.name, c.getDepth(), c.getSyncId(), c.getPos(), c.getDamage(), c.getCurrentHP(), c.getAttackerName());
				broadcastDungeon(Events.MOB_DAMAGE.getName(), s, session, c.getDepth());

			} else if (Actions.MOB_DIE.getName().equals(actionName)) {
				CMobDie c = JSON.parseObject(dataStr, CMobDie.class);
				SMobDie s = new SMobDie(session.name, c.getDepth(), c.getSyncId(), c.getPos());
				broadcastDungeon(Events.MOB_DIE.getName(), s, session, c.getDepth());

			} else if (Actions.MOB_MOVE.getName().equals(actionName)) {
				CMobMove c = JSON.parseObject(dataStr, CMobMove.class);
				SMobMove s = new SMobMove(session.name, c.getDepth(), c.getSyncId(), c.getFromPos(), c.getToPos(), c.getMobClass(), c.getHp(), c.getHt());
				broadcastDungeon(Events.MOB_MOVE.getName(), s, session, c.getDepth());

			} else if (Actions.MOB_ATTACK.getName().equals(actionName)) {
				CMobAttack c = JSON.parseObject(dataStr, CMobAttack.class);
				SMobAttack s = new SMobAttack(session.name, c.getDepth(), c.getSyncId(), c.getTargetPos(), c.getTargetName(), c.getDamage(), c.getMobClass(), c.getMobPos());
				broadcastDungeon(Events.MOB_ATTACK.getName(), s, session, c.getDepth());

			} else if (Actions.MOB_SPAWN.getName().equals(actionName)) {
				CMobSpawn c = JSON.parseObject(dataStr, CMobSpawn.class);
				SMobSpawn s = new SMobSpawn(session.name, c.getDepth(), c.getSyncId(), c.getMobClass(), c.getPos(), c.getHp(), c.getHt());
				broadcastDungeon(Events.MOB_SPAWN.getName(), s, session, c.getDepth());
			} else if (Actions.POTION_THROW.getName().equals(actionName)) {
				CPotionThrow c = JSON.parseObject(dataStr, CPotionThrow.class);
				SPotionThrow s = new SPotionThrow(session.name, c.getDepth(), c.getFromPos(), c.getTargetPos(), c.getPotionClass(), c.getColor(), c.isKnown());
				broadcastDungeon(Events.POTION_THROW.getName(), s, session, c.getDepth());

			} else if (Actions.ETERNAL_FIRE_CLEAR.getName().equals(actionName)) {
				CEternalFireClear c = JSON.parseObject(dataStr, CEternalFireClear.class);
				clearedEternalFiresByFloor.add(c.getDepth());
				SEternalFireClear s = new SEternalFireClear(session.name, c.getDepth());
				broadcastDungeon(Events.ETERNAL_FIRE_CLEAR.getName(), s, session, c.getDepth());
			}
		} catch (Exception e) {
			NLog.w("LanServer event process error: " + e.getMessage());
		}
	}

	public static void emit(WebSocket conn, String eventName, Object data) {
		if (conn != null && conn.isOpen()) {
			try {
				String json = (data instanceof String) ? (String) data : JSON.toJSONString(data);
				conn.send("42/spdnet,[\"" + eventName + "\"," + json + "]");
			} catch (Exception e) {
				NLog.w("LanServer: emit error for " + eventName + ": " + e.getMessage());
			}
		}
	}

	public static void broadcast(String eventName, Object data, WebSocket excludeConn) {
		String json = (data instanceof String) ? (String) data : JSON.toJSONString(data);
		String message = "42/spdnet,[\"" + eventName + "\"," + json + "]";
		for (WebSocket c : sessions.keySet()) {
			if (c != null && c.isOpen() && (excludeConn == null || !c.equals(excludeConn))) {
				try {
					c.send(message);
				} catch (Exception ignored) {
				}
			}
		}
	}

	/**
	 * Рассылка событий игрокам в подземелье.
	 * Если matchDepth=true, рассылается только игрокам на том же этаже.
	 */
	private static void broadcastDungeon(String eventName, Object data, LanSession source, boolean matchDepth) {
		int depth = (source != null && source.status != null) ? source.status.getDepth() : -1;
		broadcastDungeon(eventName, data, source, matchDepth ? depth : -1);
	}

	private static void broadcastDungeon(String eventName, Object data, LanSession source, int depth) {
		if (source == null) {
			return;
		}
		String json = (data instanceof String) ? (String) data : JSON.toJSONString(data);
		String message = "42/spdnet,[\"" + eventName + "\"," + json + "]";

		for (LanSession target : sessions.values()) {
			if (target == null || target.conn == null || target.conn.equals(source.conn) || !target.conn.isOpen()) {
				continue;
			}
			if (depth > 0) {
				if (target.status != null && target.status.getDepth() != depth) {
					continue;
				}
			}
			try {
				target.conn.send(message);
			} catch (Exception ignored) {
			}
		}
	}

	private static void sendNotesForSession(WebSocket conn, LanSession session) {
		if (conn == null || !conn.isOpen() || session == null) {
			return;
		}
		int depth = session.status != null ? session.status.getDepth() : 1;
		long seed = session.status != null ? session.status.getSeed() : currentSeed;
		String key = seed + "_" + depth;
		List<JSONObject> list = notesByLevel.get(key);
		List<String> noteStrings = new ArrayList<>();
		if (list != null) {
			synchronized (list) {
				for (JSONObject n : list) {
					noteStrings.add(n.toJSONString());
				}
			}
		}
		SNoteList sNotes = new SNoteList("REPLACE", seed, depth, noteStrings, new ArrayList<>());
		emit(conn, Events.NOTE_LIST.getName(), sNotes);
		if (clearedEternalFiresByFloor.contains(depth)) {
			emit(conn, Events.ETERNAL_FIRE_CLEAR.getName(), new SEternalFireClear("SERVER", depth));
		}
	}

	private static void sendMobsForSession(WebSocket conn, LanSession session) {
		if (conn == null || !conn.isOpen() || session == null) {
			return;
		}
		int depth = session.status != null ? session.status.getDepth() : 1;
		if (com.shatteredpixel.shatteredpixeldungeon.Dungeon.level != null
				&& com.shatteredpixel.shatteredpixeldungeon.Dungeon.floorId() == depth
				&& com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.mobs != null) {
			List<SMobSpawn> list = new ArrayList<>();
			for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.mobs) {
				if (m != null && m.isAlive()) {
					if (m.syncId == 0) {
						m.syncId = ++com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.maxMobSyncId;
					}
					list.add(new SMobSpawn(session.name, depth, m.syncId, m.getClass().getName(), m.pos, m.HP, m.HT));
				}
			}
			SMobSync sync = new SMobSync(session.name, depth, list);
			emit(conn, Events.MOB_SYNC.getName(), sync);
		}
	}

	private static List<Player> collectAllPlayers() {
		List<Player> list = new ArrayList<>();
		for (LanSession session : sessions.values()) {
			Player p = new Player(session.name, "PLAYER", session.status);
			list.add(p);
		}
		return list;
	}

	private static Map<String, String> parseQueryParams(String descriptor) {
		Map<String, String> map = new HashMap<>();
		if (descriptor == null) {
			return map;
		}
		int queryStart = descriptor.indexOf('?');
		if (queryStart >= 0 && queryStart < descriptor.length() - 1) {
			String query = descriptor.substring(queryStart + 1);
			String[] pairs = query.split("&");
			for (String pair : pairs) {
				int eq = pair.indexOf('=');
				if (eq > 0) {
					try {
						String key = URLDecoder.decode(pair.substring(0, eq), "UTF-8");
						String val = URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
						map.put(key, val);
					} catch (UnsupportedEncodingException ignored) {
					}
				}
			}
		}
		return map;
	}
}
