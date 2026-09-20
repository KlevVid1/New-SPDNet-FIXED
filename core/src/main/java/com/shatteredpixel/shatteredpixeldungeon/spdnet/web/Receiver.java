package com.shatteredpixel.shatteredpixeldungeon.spdnet.web;

import static com.shatteredpixel.shatteredpixeldungeon.spdnet.web.Net.getSocket;

import com.alibaba.fastjson.JSON;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.spdnetbutcopy.scene.NetRankingsScene;
import me.catand.spdnet.protocol.Events;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.NetInProgress;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.actors.NetHero;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.Status;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CEnterDungeon;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.events.*;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.utils.NLog;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.windows.NetWindow;
import com.watabou.noosa.Game;

import java.io.IOException;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * 此类用于接收并解析服务器发送的消息
 */
public class Receiver {
	public static void startAll() {
		Emitter.Listener onConnected = args -> {
			Game.runOnRenderThread(() -> {
				if (ShatteredPixelDungeon.scene() instanceof GameScene && Dungeon.hero != null) {
					Status status1 = new Status(Dungeon.challenges,
							Dungeon.seed,
							Dungeon.hero.heroClass.ordinal(),
							NetInProgress.mode.ordinal(),
							Dungeon.floorId(),
							Dungeon.hero.tier(),
							Dungeon.hero.pos);
					CEnterDungeon enterDungeon = new CEnterDungeon(status1);
					Sender.sendEnterDungeon(enterDungeon);
					NetHero.syncWithCurrentLevel();
				}
			});
		};
		Emitter.Listener onDisconnected = args -> {
			Badges.resetToLocalMode();
			if (getSocket() == null || !getSocket().io().isReconnecting()) {
				cancelAll();
			}

			boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
			boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
					|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
			String dcMsg = isRu ? "Отключено от сервера" : (isZh ? "与服务器断开连接" : "Disconnected from server");

			Game.runOnRenderThread(() -> {
				if (ShatteredPixelDungeon.scene() instanceof GameScene) {
					GameScene.clearPlayers();
					com.shatteredpixel.shatteredpixeldungeon.spdnet.utils.NLog.w(dcMsg);
				} else if (ShatteredPixelDungeon.scene() instanceof NetRankingsScene) {
					Game.switchScene(TitleScene.class);
				} else {
					NetWindow.error(dcMsg);
				}
			});
		};
		Emitter.Listener onConnectionError = args -> {
			try {
				if (getSocket() == null || !getSocket().io().isReconnecting()) {
					cancelAll();
				}

				boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
				boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
						|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
				String errorMessage = isRu ? "Не удалось подключиться к серверу" : (isZh ? "连接服务器失败" : "Connection to server failed");
				if (args != null && args.length > 0 && args[0] != null) {
					String errStr = args[0].toString();
					if (errStr.contains("ConnectException") || errStr.contains("Failed to connect") || errStr.contains("refused")) {
						errorMessage = isRu ? "Сервер недоступен или выключен" : (isZh ? "目标服务器未开启或不可达" : "Server unreachable or offline");
					} else if (errStr.length() > 60) {
						errStr = errStr.substring(0, 60) + "...";
						errorMessage += ":\n" + errStr;
					} else {
						errorMessage += ":\n" + errStr;
					}
				}

				String finalMsg = errorMessage;
				Game.runOnRenderThread(() -> {
					try {
						if (ShatteredPixelDungeon.scene() instanceof GameScene) {
							GameScene.clearPlayers();
							com.shatteredpixel.shatteredpixeldungeon.spdnet.utils.NLog.w(finalMsg);
						} else {
							NetWindow.error(finalMsg);
						}
					} catch (Throwable t) {
						t.printStackTrace();
					}
				});
			} catch (Throwable t) {
				t.printStackTrace();
			}
		};
		Emitter.Listener onAchievement = args -> {
			Handler.handleAchievement(JSON.parseObject(args[0].toString(), SAchievement.class));
		};
		Emitter.Listener onAnkhUsed = args -> {
			Handler.handleAnkhUsed(JSON.parseObject(args[0].toString(), SAnkhUsed.class));
		};
		Emitter.Listener onArmorUpdate = args -> {
			Handler.handleArmorUpdate(JSON.parseObject(args[0].toString(), SArmorUpdate.class));
		};
		Emitter.Listener onChatMessage = args -> {
			Handler.handleChatMessage(JSON.parseObject(args[0].toString(), SChatMessage.class));
		};
		Emitter.Listener onEnterDungeon = args -> {
			Handler.handleEnterDungeon(JSON.parseObject(args[0].toString(), SEnterDungeon.class));
		};
		Emitter.Listener onError = args -> {
			Handler.handleError(JSON.parseObject(args[0].toString(), SError.class));
		};
		Emitter.Listener onExit = args -> {
			Handler.handleExit(JSON.parseObject(args[0].toString(), SExit.class));
		};
		Emitter.Listener onFloatingText = args -> {
			Handler.handleFloatingText(JSON.parseObject(args[0].toString(), SFloatingText.class));
		};
		Emitter.Listener onGameEnd = args -> {
			Handler.handleGameEnd(JSON.parseObject(args[0].toString(), SGameEnd.class));
		};
		Emitter.Listener onGiveItem = args -> {
			Handler.handleGiveItem(JSON.parseObject(args[0].toString(), SGiveItem.class));
		};
		Emitter.Listener onHero = args -> {
			try {
				NLog.i("Receiver: onHero received");
				Handler.handleHero(JSON.parseObject(args[0].toString(), SHero.class));
			} catch (Exception e) {
				NLog.w("Receiver: onHero error: " + e.getMessage());
			}
		};
		Emitter.Listener onInit = args -> {
			Handler.handleInit(JSON.parseObject(args[0].toString(), SInit.class));
		};
		Emitter.Listener onJoin = args -> {
			Handler.handleJoin(JSON.parseObject(args[0].toString(), SJoin.class));
		};
		Emitter.Listener onLeaderboard = args -> {
			System.out.println(args[0].toString());
			Handler.handleLeaderboard(JSON.parseObject(args[0].toString(), SLeaderboard.class));
		};
		Emitter.Listener onLeaveDungeon = args -> {
			Handler.handleLeaveDungeon(JSON.parseObject(args[0].toString(), SLeaveDungeon.class));
		};
		Emitter.Listener onPlayerChangeFloor = args -> {
			Handler.handlePlayerChangeFloor(JSON.parseObject(args[0].toString(), SPlayerChangeFloor.class));
		};
		Emitter.Listener onPlayerList = args -> {
			Handler.handlePlayerList(JSON.parseObject(args[0].toString(), SPlayerList.class));
		};
		Emitter.Listener onPlayerMove = args -> {
			Handler.handlePlayerMove(JSON.parseObject(args[0].toString(), SPlayerMove.class));
		};
		Emitter.Listener onServerMessage = args -> {
			Handler.handleServerMessage(JSON.parseObject(args[0].toString(), SServerMessage.class));
		};
		Emitter.Listener onViewHero = args -> {
			Handler.handleViewHero(JSON.parseObject(args[0].toString(), SViewHero.class));
		};
		Emitter.Listener onJournals = args -> {
			Handler.handleJournals(JSON.parseObject(args[0].toString(), SJournals.class));
		};
		Emitter.Listener onAllowDailyChallenge = args -> {
			Handler.handleAllowDailyChallenge(JSON.parseObject(args[0].toString(), SAllowDailyChallenge.class));
		};
		Emitter.Listener onRejectDailyChallenge = args -> {
			Handler.handleRejectDailyChallenge(JSON.parseObject(args[0].toString(), SRejectDailyChallenge.class));
		};
		Emitter.Listener onNoteList = args -> {
			Handler.handleNoteList(JSON.parseObject(args[0].toString(), SNoteList.class));
		};
		// SPDNet: 地牢留言(Ping)系统 - 留言创建成功后的聊天通报（渲染为聊天窗口通报而非弹窗）
		Emitter.Listener onNoteNotify = args -> {
			Handler.handleNoteNotify(JSON.parseObject(args[0].toString(), SServerMessage.class));
		};
		// SPDNet Co-op: Синхронизация предметов на полу
		Emitter.Listener onItemDrop = args -> {
			Handler.handleItemDrop(JSON.parseObject(args[0].toString(), SItemDrop.class));
		};
		Emitter.Listener onItemPickUp = args -> {
			Handler.handleItemPickUp(JSON.parseObject(args[0].toString(), SItemPickUp.class));
		};
		// SPDNet Co-op: Синхронизация мобов и боссов
		Emitter.Listener onMobDamage = args -> {
			Handler.handleMobDamage(JSON.parseObject(args[0].toString(), SMobDamage.class));
		};
		Emitter.Listener onMobDie = args -> {
			Handler.handleMobDie(JSON.parseObject(args[0].toString(), SMobDie.class));
		};
		Emitter.Listener onMobMove = args -> {
			Handler.handleMobMove(JSON.parseObject(args[0].toString(), SMobMove.class));
		};
		Emitter.Listener onMobAttack = args -> {
			Handler.handleMobAttack(JSON.parseObject(args[0].toString(), SMobAttack.class));
		};
		Emitter.Listener onMobSpawn = args -> {
			try {
				Handler.handleMobSpawn(JSON.parseObject(args[0].toString(), SMobSpawn.class));
			} catch (Exception e) {
				NLog.w("Receiver: onMobSpawn error: " + e.getMessage());
			}
		};
		Emitter.Listener onTerrainChange = args -> {
			try {
				Handler.handleTerrainChange(JSON.parseObject(args[0].toString(), STerrainChange.class));
			} catch (Exception e) {
				NLog.w("Receiver: onTerrainChange error: " + e.getMessage());
			}
		};
		Emitter.Listener onChestOpen = args -> {
			try {
				Handler.handleChestOpen(JSON.parseObject(args[0].toString(), SChestOpen.class));
			} catch (Exception e) {
				NLog.w("Receiver: onChestOpen error: " + e.getMessage());
			}
		};
		Emitter.Listener onPotionThrow = args -> {
			try {
				Handler.handlePotionThrow(JSON.parseObject(args[0].toString(), SPotionThrow.class));
			} catch (Exception e) {
				NLog.w("Receiver: onPotionThrow error: " + e.getMessage());
			}
		};
		Emitter.Listener onEternalFireClear = args -> {
			try {
				Handler.handleEternalFireClear(JSON.parseObject(args[0].toString(), SEternalFireClear.class));
			} catch (Exception e) {
				NLog.w("Receiver: onEternalFireClear error: " + e.getMessage());
			}
		};
		Emitter.Listener onMobSync = args -> {
			try {
				Handler.handleMobSync(JSON.parseObject(args[0].toString(), SMobSync.class));
			} catch (Exception e) {
				NLog.w("Receiver: onMobSync error: " + e.getMessage());
			}
		};
		getSocket().on(Socket.EVENT_CONNECT, onConnected);
		getSocket().on(Socket.EVENT_DISCONNECT, onDisconnected);
		getSocket().on(Socket.EVENT_CONNECT_ERROR, onConnectionError);
		getSocket().on(Events.ACHIEVEMENT.getName(), onAchievement);
		getSocket().on(Events.ANKH_USED.getName(), onAnkhUsed);
		getSocket().on(Events.ARMOR_UPDATE.getName(), onArmorUpdate);
		getSocket().on(Events.CHAT_MESSAGE.getName(), onChatMessage);
		getSocket().on(Events.ENTER_DUNGEON.getName(), onEnterDungeon);
		getSocket().on(Events.ERROR.getName(), onError);
		getSocket().on(Events.EXIT.getName(), onExit);
		getSocket().on(Events.FLOATING_TEXT.getName(), onFloatingText);
		getSocket().on(Events.GAME_END.getName(), onGameEnd);
		getSocket().on(Events.GIVE_ITEM.getName(), onGiveItem);
		getSocket().on(Events.HERO.getName(), onHero);
		getSocket().on(Events.INIT.getName(), onInit);
		getSocket().on(Events.JOIN.getName(), onJoin);
		getSocket().on(Events.LEADERBOARD.getName(), onLeaderboard);
		getSocket().on(Events.LEAVE_DUNGEON.getName(), onLeaveDungeon);
		getSocket().on(Events.PLAYER_CHANGE_FLOOR.getName(), onPlayerChangeFloor);
		getSocket().on(Events.PLAYER_LIST.getName(), onPlayerList);
		getSocket().on(Events.PLAYER_MOVE.getName(), onPlayerMove);
		getSocket().on(Events.SERVER_MESSAGE.getName(), onServerMessage);
		getSocket().on(Events.VIEW_HERO.getName(), onViewHero);
		getSocket().on(Events.JOURNALS.getName(), onJournals);
		getSocket().on(Events.ALLOW_DAILY_CHALLENGE.getName(), onAllowDailyChallenge);
		getSocket().on(Events.REJECT_DAILY_CHALLENGE.getName(), onRejectDailyChallenge);
		getSocket().on(Events.NOTE_LIST.getName(), onNoteList);
		getSocket().on(Events.NOTE_NOTIFY.getName(), onNoteNotify);
		getSocket().on(Events.ITEM_DROP.getName(), onItemDrop);
		getSocket().on(Events.ITEM_PICKUP.getName(), onItemPickUp);
		getSocket().on(Events.MOB_DAMAGE.getName(), onMobDamage);
		getSocket().on(Events.MOB_DIE.getName(), onMobDie);
		getSocket().on(Events.MOB_MOVE.getName(), onMobMove);
		getSocket().on(Events.MOB_ATTACK.getName(), onMobAttack);
		getSocket().on(Events.MOB_SPAWN.getName(), onMobSpawn);
		getSocket().on(Events.MOB_SYNC.getName(), onMobSync);
		getSocket().on(Events.TERRAIN_CHANGE.getName(), onTerrainChange);
		getSocket().on(Events.CHEST_OPEN.getName(), onChestOpen);
		getSocket().on(Events.POTION_THROW.getName(), onPotionThrow);
		getSocket().on(Events.ETERNAL_FIRE_CLEAR.getName(), onEternalFireClear);
	}

	public static void cancelAll() {
		try {
			if (Net.hasSocket()) {
				getSocket().off();
			}
		} catch (Throwable ignored) {}
	}
}
