package com.shatteredpixel.shatteredpixeldungeon.spdnet.web;

import com.alibaba.fastjson.JSON;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.Mode;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.NetInProgress;
import com.shatteredpixel.shatteredpixeldungeon.spdnetbutcopy.scene.NetRankingsScene;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.utils.NLog;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.utils.PrefixUtils;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.utils.SPDUtils;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.actors.NetHero;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.sprites.NetHeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.Player;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.Status;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CHero;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CRequestPlayerList;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.events.*;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.ui.scene.DailyChallengeDetailWindow;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.windows.NetWindow;
import com.shatteredpixel.shatteredpixeldungeon.spdnetbutcopy.windows.NetWndPlayerInfo;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 此类用于处理服务器发送的消息
 */
public class Handler {
	public static void handleAchievement(SAchievement achievement) {
		Badges.Badge badge = achievement.getBadge();
		String displayName = PrefixUtils.formatNameWithPrefix(achievement.getName(), achievement.getPrefix());
		if (achievement.isUnique()) {
			NLog.h(displayName + Messages.get(Badges.class, "new", badge.title() + " (" + badge.desc() + ")"));
		} else {
			NLog.h(displayName + Messages.get(Badges.class, "endorsed", badge.title()));
		}
	}

	public static void handleAnkhUsed(SAnkhUsed ankhUsed) {
		if (!ankhUsed.getName().equals(Net.name)) {
			Player player = Net.playerList.get(ankhUsed.getName());
			if (player == null) {
				syncPlayerList();
				return;
			}
			Game.runOnRenderThread(() -> {
				NetHero player1 = NetHero.getPlayerFromDungeon(ankhUsed.getName());
				if (player1 != null) {
					player1.useAnkh(true, ankhUsed.getUnusedBlessedAnkh(), ankhUsed.getUnusedUnblessedAnkh());
				}
			});
			String displayName = PrefixUtils.formatNameWithPrefix(ankhUsed.getName(), ankhUsed.getPrefix());
			boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
			boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
					|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
			int remaining = ankhUsed.getUnusedBlessedAnkh() + ankhUsed.getUnusedUnblessedAnkh();

			if (remaining == 0) {
				String msg = isRu ? (displayName + " использовал свой последний крест возрождения (причина: " + ankhUsed.getCause() + ")") :
						(isZh ? (displayName + "因为" + ankhUsed.getCause() + "用掉了他的最后一个十字架") :
								(displayName + " used their last Ankh (cause: " + ankhUsed.getCause() + ")"));
				NLog.w(msg);
			} else {
				String msg = isRu ? (displayName + " использовал крест возрождения (причина: " + ankhUsed.getCause() + "), осталось: " + remaining) :
						(isZh ? (displayName + "因为" + ankhUsed.getCause() + "用掉了他的十字架，剩余十字架: " + remaining) :
								(displayName + " used an Ankh (cause: " + ankhUsed.getCause() + "), remaining: " + remaining));
				NLog.w(msg);
			}
		}
	}

	public static void handleArmorUpdate(SArmorUpdate armorUpdate) {
		if (!armorUpdate.getName().equals(Net.name)) {
			Player player = Net.playerList.get(armorUpdate.getName());
			if (player == null) {
				syncPlayerList();
				return;
			}
			Status status = player.getStatus();
			if (status == null) {
				return;
			}
			status.setArmorTier(armorUpdate.getArmorTier());
			player.setStatus(status);
			Net.playerList.put(armorUpdate.getName(), player);
			Game.runOnRenderThread(() -> {
				NetHero player1 = NetHero.getPlayerFromDungeon(armorUpdate.getName());
				if (player1 != null) {
					player1.tier = armorUpdate.getArmorTier();
					if (player1.sprite instanceof NetHeroSprite) {
						((NetHeroSprite) (player1.sprite)).updateArmor();
					}
				}
			});
		}
	}

	public static void handleHero(SHero hero) {
		if (hero == null || hero.getHero() == null) {
			NLog.w("handleHero: hero payload is null");
			return;
		}
		NLog.i("handleHero: processing hero data for " + hero.getTargetName());
		Bundle bundle;
		try {
			bundle = Bundle.fromString(hero.getHero());
		} catch (Exception e) {
			NLog.w("handleHero: failed to parse bundle: " + e.getMessage());
			return;
		}
		// SPDNet: 源端 hero 数据无效/为空(如对方在主菜单且无英雄)时，直接跳过，视为正常情况而非崩溃
		if (bundle == null) {
			boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
			boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
					|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
			String err = isRu ? ("Данные персонажа " + hero.getTargetName() + " недействительны") :
					(isZh ? ("查看 " + hero.getTargetName() + " 的英雄数据无效或被截断") :
							("Hero data for " + hero.getTargetName() + " is invalid"));
			NLog.w(err);
			return;
		}
		// SPDNet: 将反序列化与窗口创建统一放到渲染线程执行。
		Game.runOnRenderThread(() -> {
			try {
				NetHero player = new NetHero(hero.getTargetName());
				player.restoreFromBundleOverride(bundle);
				if (ShatteredPixelDungeon.scene() instanceof GameScene) {
					GameScene.show(new NetWndPlayerInfo(hero.getTargetName(), player));
				} else {
					ShatteredPixelDungeon.scene().addToFront(new NetWndPlayerInfo(hero.getTargetName(), player));
				}
				NLog.i("handleHero: successfully opened NetWndPlayerInfo for " + hero.getTargetName());
			} catch (Throwable t) {
				NLog.w("handleHero: error showing NetWndPlayerInfo: " + t.getMessage());
				t.printStackTrace();
			}
		});
	}

	public static void handleChatMessage(SChatMessage chatMessage) {
		// SPDNet: 使用服务端传来的时间显示聊天消息，并显示前缀
		String displayName = PrefixUtils.formatNameWithPrefix(chatMessage.getName(), chatMessage.getPrefix());
		NLog.chat(displayName, chatMessage.getMessage(), chatMessage.getTime());
	}

	public static void handleEnterDungeon(SEnterDungeon enterDungeon) {
		if (!enterDungeon.getName().equals(Net.name)) {
			Player player = Net.playerList.get(enterDungeon.getName());
			if (player == null) {
				syncPlayerList();
				return;
			}
			player.setStatus(enterDungeon.getStatus());
			player.setPrefix(enterDungeon.getPrefix());
			Net.playerList.put(enterDungeon.getName(), player);
			NetHero.addPlayerToDungeon(player);
			String displayName = PrefixUtils.formatNameWithPrefix(enterDungeon.getName(), enterDungeon.getPrefix());

			boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
			boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
					|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
			String modeName = enterDungeon.getStatus().getGameModeEnum().getName();
			int chCount = SPDUtils.activeChallenges(enterDungeon.getStatus().getChallenges());

			String enterMsg = isRu ? (displayName + " вошел в подземелье (" + modeName + ", исп.: " + chCount + ")") :
					(isZh ? (displayName + "以" + (modeName.length() >= 2 ? modeName.substring(0, 2) : modeName) + "模式, " + chCount + "挑进入了地牢") :
							(displayName + " entered dungeon (" + modeName + ", " + chCount + " chlng.)"));
			NLog.h(enterMsg);
		}
	}

	public static void handleError(SError error) {
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
		String prefix = isRu ? "Ошибка сервера: " : (isZh ? "服务器错误:" : "Server error: ");
		NetWindow.error(prefix + error.getError());
		NLog.n(prefix + error.getError());
	}

	public static void handleExit(SExit exit) {
		if (!exit.getName().equals(Net.name)) {
			Player player = Net.playerList.get(exit.getName());
			if (player != null) {
				Net.playerList.remove(exit.getName());
				NetHero.removePlayerFromDungeon(exit.getName());
			}
			String displayName = PrefixUtils.formatNameWithPrefix(exit.getName(), exit.getPrefix());
			boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
			boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
					|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
			String exitMsg = isRu ? (displayName + " вышел из сети") : (isZh ? (displayName + " 下线了") : (displayName + " disconnected"));
			NLog.h(exitMsg);
		}
	}

	public static void handleGiveItem(SGiveItem giveItem) {
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		if (NetInProgress.isDailyChallenge()) {
			String displayName = PrefixUtils.formatNameWithPrefix(giveItem.getName(), giveItem.getPrefix());
			String msg = isRu ? (displayName + " хотел передать предмет, но в ежедневном испытании это запрещено") :
					(isZh ? (displayName + "想给你物品，但每日挑战模式下无法接收物品") :
							(displayName + " tried to give you an item, but items cannot be received in daily challenge"));
			NLog.h(msg);
			return;
		}
		Item item = giveItem.getItemObject();
		if (item != null) {
			Game.runOnRenderThread(() -> {
				if (!(ShatteredPixelDungeon.scene() instanceof GameScene) || Dungeon.hero == null) {
					return;
				}
				if (NetInProgress.mode == Mode.IRONMAN) {
					String displayName = PrefixUtils.formatNameWithPrefix(giveItem.getName(), giveItem.getPrefix());
					String msg = isRu ? (displayName + " хотел передать " + item.name() + ", но вы играете в режиме 'Железный человек'") :
							(isZh ? (displayName + "想给你 " + item.name() + ", 可惜你是铁人") :
									(displayName + " tried to give you " + item.name() + ", but you are in Ironman mode"));
					NLog.h(msg);
					return;
				}
				item.doPickUp(Dungeon.hero);
				String displayName = PrefixUtils.formatNameWithPrefix(giveItem.getName(), giveItem.getPrefix());
				String msg = isRu ? (displayName + " передал вам: " + item.name()) :
						(isZh ? (displayName + "给了你" + item.name()) :
								(displayName + " gave you " + item.name()));
				NLog.h(msg);
			});
		}
	}

	public static void handleFloatingText(SFloatingText floatingText) {
		if (!floatingText.getName().equals(Net.name)) {
			Game.runOnRenderThread(() -> {
				NetHero player = NetHero.getPlayerFromDungeon(floatingText.getName());
				if (player != null && player.sprite != null) {
					// 溅血效果
					if (player.HP > floatingText.getHeroHP()) {
						player.sprite.bloodBurstA(player.sprite.center(), (player.HP - floatingText.getHeroHP()) * 2);
					}
					player.HP = floatingText.getHeroHP();
					player.shield = floatingText.getHeroShield();
					player.HT = floatingText.getHeroHT();
					player.sprite.showStatusWithIcon(floatingText.getColor(), floatingText.getText(), floatingText.getIcon());
				}
			});
		}
	}

	public static void handleGameEnd(SGameEnd gameEnd) {
		if (!gameEnd.getName().equals(Net.name)) {
			Game.runOnRenderThread(() -> {
				NetHero player = NetHero.getPlayerFromDungeon(gameEnd.getName());
				if (player != null) {
					player.die(null);
				}
			});
		}
		GameRecord record = JSON.parseObject(gameEnd.getRecord(), GameRecord.class);
		String displayName = PrefixUtils.formatNameWithPrefix(gameEnd.getName(), gameEnd.getPrefix());
		String modeName = Mode.valueOf(record.getGameMode()).getName();
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
		String outcome = record.isWin() ?
				(isRu ? "победил!" : (isZh ? "胜利" : "won!")) :
				(isRu ? ("погиб на " + record.getDepth() + " этаже") : (isZh ? ("死亡, 到达了第" + record.getDepth() + "层") : ("died on floor " + record.getDepth())));
		String endMsg = isRu ? (displayName + " в режиме " + modeName + " (" + record.getChallengeAmount() + " исп.) " + outcome) :
				(isZh ? (displayName + "在" + modeName + record.getChallengeAmount() + "挑" + outcome) :
						(displayName + " in " + modeName + " (" + record.getChallengeAmount() + " chlng.) " + outcome));
		NLog.w(endMsg);
	}

	public static void handleInit(SInit init) {
		Net.seeds = new ConcurrentHashMap<>(init.getSeeds());
		Net.name = init.getName();
		if (init.getMotd() != null && !init.getMotd().isEmpty()) {
			NLog.i(init.getMotd());
		}

		// SPDNet: 从服务器加载云端成就
		Badges.loadFromCloud(init.getAchievements());

		// SPDNet: 默认使用娱乐模式的种子，而不是从服务器种子里随机取一个
		// 处于每日挑战选择/游戏中时不覆盖，避免打断每日挑战
		if (!NetInProgress.isDailyChallenge()) {
			Long funSeed = Net.seeds.get("seedFUN");
			if (funSeed != null) {
				NetInProgress.seedName = "seedFUN";
				NetInProgress.seed = funSeed;
			} else {
				// 兜底：服务器未下发 seedFUN 时，使用第一个种子
				Enumeration<String> keysEnumeration = Net.seeds.keys();
				ArrayList<String> keysList = Collections.list(keysEnumeration);
				if (!keysList.isEmpty()) {
					NetInProgress.seedName = keysList.get(0);
					NetInProgress.seed = Net.seeds.get(NetInProgress.seedName);
				}
			}
		}
	}

	public static void handleJoin(SJoin join) {
		if (!join.getName().equals(Net.name)) {
			Player player = new Player(join.getName(), join.getRole(), null);
			player.setPrefix(join.getPrefix());
			Net.playerList.put(join.getName(), player);
			String displayName = PrefixUtils.formatNameWithPrefix(join.getName(), join.getPrefix());
			boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
			boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
					|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
			String joinMsg = isRu ? (displayName + " вошел в сеть") : (isZh ? (displayName + " 上线了") : (displayName + " connected"));
			NLog.h(joinMsg);
		}
	}

	public static void handleLeaderboard(SLeaderboard leaderboard) {
		ArrayList<GameRecord> records = new ArrayList<>();
		if (ShatteredPixelDungeon.scene() instanceof NetRankingsScene) {
			try {
				List<String> recordsString = leaderboard.getGameRecords();
				for (String record : recordsString) {
					records.add(JSON.parseObject(record, GameRecord.class));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			((NetRankingsScene) ShatteredPixelDungeon.scene()).setRankings(leaderboard.getTotalPages(), leaderboard.getCurrentPage(), leaderboard.getTotalElements(), records);
		}
	}

	public static void handleLeaveDungeon(SLeaveDungeon leaveDungeon) {
		if (!leaveDungeon.getName().equals(Net.name)) {
			Player player = Net.playerList.get(leaveDungeon.getName());
			if (player != null) {
				player.setStatus(null);
				Net.playerList.put(leaveDungeon.getName(), player);
				NetHero.removePlayerFromDungeon(leaveDungeon.getName());
			}
		}
	}

	public static void handlePlayerChangeFloor(SPlayerChangeFloor playerChangeFloor) {
		if (!playerChangeFloor.getName().equals(Net.name)) {
			Player player = Net.playerList.get(playerChangeFloor.getName());
			if (player == null) {
				syncPlayerList();
				return;
			}
			Status status = player.getStatus();
			if (status == null) {
				return;
			}
			status.setDepth(playerChangeFloor.getDepth());
			player.setStatus(status);
			Net.playerList.put(playerChangeFloor.getName(), player);
			NetHero.addPlayerToDungeon(player);
		}
	}

	public static void handlePlayerList(SPlayerList playerList) {
		Net.playerList.clear();
		for (Player player : playerList.getPlayers()) {
			Net.playerList.put(player.getName(), player);
		}
		NetHero.syncWithCurrentLevel();
	}

	public static void handlePlayerMove(SPlayerMove playerMove) {
		if (!playerMove.getName().equals(Net.name)) {
			Player player = Net.playerList.get(playerMove.getName());
			if (player == null) {
				syncPlayerList();
				return;
			}
			Status status = player.getStatus();
			if (status == null) {
				return;
			}
			status.setPos(playerMove.getPos());
			player.setStatus(status);
			Net.playerList.put(playerMove.getName(), player);
			// Если этот игрок на текущем этаже
			Game.runOnRenderThread(() -> {
				NetHero player1 = NetHero.getPlayerFromDungeon(playerMove.getName());
				if (player1 != null) {
					if (ShatteredPixelDungeon.scene() instanceof GameScene) {
						player1.move(playerMove.getPos(), false);
					} else {
						player1.pos = playerMove.getPos();
					}
				}
			});
		}
	}

	public static void handleServerMessage(SServerMessage serverMessage) {
		NetWindow.message(serverMessage.getMessage());
	}

	// SPDNet: 地牢留言(Ping)系统 - 留言创建成功后的聊天通报。与"xxx进入地牢"一致，
	// 渲染进聊天窗口(NLog.h 高亮通报)，而非服务端弹窗。
	public static void handleNoteNotify(SServerMessage noteNotify) {
		NLog.h(noteNotify.getMessage());
	}

	public static void handleJournals(SJournals journals) {
		// SPDNet: 从服务器加载 Journal 数据
		Journal.loadFromCloud(journals.getCatalogs(), journals.getBestiaries(), journals.getDocuments());
	}

	public static void handleAllowDailyChallenge(SAllowDailyChallenge allowDailyChallenge) {
		Game.runOnRenderThread(() -> {
			Game.scene().add(new DailyChallengeDetailWindow(
				allowDailyChallenge.getGroupIndex(),
				allowDailyChallenge.getSeed(),
				allowDailyChallenge.getRecordDate(),
				allowDailyChallenge.isHasExistingRecord(),
				allowDailyChallenge.getChallenges()
			));
		});
	}

	public static void handleRejectDailyChallenge(SRejectDailyChallenge rejectDailyChallenge) {
		NetInProgress.resetDailyChallenge();
		String reason = rejectDailyChallenge.getReason();
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		String errTitle = isRu ? ("Ежедневное испытание: " + reason) : (isZh ? ("每日挑战: " + reason) : ("Daily challenge: " + reason));
		String logMsg = isRu ? ("Ежедневное испытание отклонено: " + reason) : (isZh ? ("每日挑战被拒绝: " + reason) : ("Daily challenge rejected: " + reason));
		NetWindow.error(errTitle);
		NLog.n(logMsg);
	}

	public static void handleViewHero(SViewHero viewHero) {
		if (Dungeon.hero != null) {
			Bundle heroBundle = new Bundle();
			Dungeon.hero.storeInBundle(heroBundle);
			// SPDNet: 地牢留言(Ping)系统 - forNote 模式下回 CHero 时带 forNote=true，服务端拦截落库，
			// 不回传触发查看窗；且本端不打"你被查看"提示（留言取快照是静默的）。
			CHero ch = new CHero(viewHero.getSourceName(), heroBundle.toString());
			ch.setForNote(viewHero.isForNote());
			Sender.sendHero(ch);
		}
		// SPDNet: forNote 模式静默，不提示"你被查看"
		if (!viewHero.isForNote()) {
			String displayName = PrefixUtils.formatNameWithPrefix(viewHero.getSourceName(), viewHero.getPrefix());
			boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
			boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
					|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
			String viewMsg = isRu ? (displayName + " осмотрел вашего персонажа") :
					(isZh ? ("你被" + displayName + "查看了") :
							(displayName + " inspected your hero"));
			NLog.h(viewMsg);
		}
	}

	/**
	 * SPDNet: 地牢留言(Ping)系统 - 处理进/换层单播 REPLACE 与同层 DELTA 增量。
	 * 仅在 seed+depth 与当前层一致时才更新缓存与 Overlay；运行在 socket 线程，
	 * 缓存更新与 setData 统一回渲染线程执行（与 hero 查看同款处理）。
	 */
	public static void handleNoteList(SNoteList noteList) {
		// 铁人模式同种子的留言本就互不可见，无需额外过滤；直接按 seed+depth 过滤
		if (noteList.getSeed() != Dungeon.seed || noteList.getDepth() != Dungeon.depth) {
			return;
		}
		String mode = noteList.getMode();
		List<String> notes = noteList.getNotes();
		List<Integer> likedIds = noteList.getMyLikedIds();
		Game.runOnRenderThread(() -> {
			if ("REPLACE".equals(mode)) {
				NetNoteStore.replace(notes, likedIds);
			} else if ("DELTA_ADD".equals(mode)) {
				if (notes != null && !notes.isEmpty()) {
					NetNoteStore.upsert(notes.get(0));
				}
			} else if ("DELTA_REMOVE".equals(mode)) {
				if (notes != null && !notes.isEmpty()) {
					int id = JSON.parseObject(notes.get(0)).getIntValue("id");
					NetNoteStore.remove(id);
				}
			}
			// overlay 可能尚未创建（极端时序），做空指针容忍
			if (GameScene.noteOverlay != null) {
				GameScene.noteOverlay.setData();
			}
		});
	}

	/**
	 * 同步玩家列表
	 * 如果出现任何列表不同步的情况, 请调用此方法
	 */
	public static void syncPlayerList() {
		Sender.sendRequestPlayerList(new CRequestPlayerList());
		NetHero.syncWithCurrentLevel();
	}

	// SPDNet Co-op: Синхронизация предметов на полу
	public static void handleItemDrop(SItemDrop itemDrop) {
		if (itemDrop == null || itemDrop.getName() == null || itemDrop.getName().equals(Net.name)) {
			return;
		}
		if (itemDrop.getDepth() != Dungeon.depth || Dungeon.level == null) {
			return;
		}
		Item item = itemDrop.getItemObject();
		if (item != null) {
			Game.runOnRenderThread(() -> {
				if (Dungeon.level != null) {
					Level.isRemoteDrop = true;
					try {
						Heap heap = Dungeon.level.drop(item, itemDrop.getPos());
						if (heap != null && heap.sprite != null && ShatteredPixelDungeon.scene() instanceof GameScene) {
							heap.sprite.drop(itemDrop.getPos());
						}
					} finally {
						Level.isRemoteDrop = false;
					}
				}
			});
		}
	}

	public static void handleItemPickUp(SItemPickUp itemPickUp) {
		if (itemPickUp == null || itemPickUp.getName() == null || itemPickUp.getName().equals(Net.name)) {
			return;
		}
		if (itemPickUp.getDepth() != Dungeon.depth || Dungeon.level == null) {
			return;
		}
		Game.runOnRenderThread(() -> {
			if (Dungeon.level != null && Dungeon.level.heaps != null) {
				Heap heap = Dungeon.level.heaps.get(itemPickUp.getPos());
				if (heap != null) {
					Heap.isNetRemote = true;
					try {
						heap.pickUp();
					} finally {
						Heap.isNetRemote = false;
					}
				}
			}
		});
	}

	// SPDNet Co-op: Синхронизация местности (трава, двери, баррикады)
	public static void handleTerrainChange(STerrainChange terrainChange) {
		if (terrainChange == null || terrainChange.getName() == null || terrainChange.getName().equals(Net.name)) {
			return;
		}
		if (terrainChange.getDepth() != Dungeon.depth || Dungeon.level == null) {
			return;
		}
		Game.runOnRenderThread(() -> {
			if (Dungeon.level != null) {
				int cell = terrainChange.getPos();
				int newTerrain = terrainChange.getTerrain();
				int oldTerrain = Dungeon.level.map[cell];
				Level.isRemoteTerrainChange = true;
				try {
					Level.set(cell, newTerrain, Dungeon.level);
				} finally {
					Level.isRemoteTerrainChange = false;
				}
				if (ShatteredPixelDungeon.scene() instanceof GameScene) {
					GameScene.updateMap(cell);
					if ((oldTerrain == Terrain.HIGH_GRASS || oldTerrain == Terrain.FURROWED_GRASS) &&
							(newTerrain == Terrain.GRASS || newTerrain == Terrain.FURROWED_GRASS)) {
						CellEmitter.get(cell).burst(LeafParticle.LEVEL_SPECIFIC, 4);
					}
					if (Dungeon.level.heroFOV[cell]) {
						Dungeon.observe();
					}
				}
			}
		});
	}

	// SPDNet Co-op: Синхронизация открытия сундуков
	public static void handleChestOpen(SChestOpen chestOpen) {
		if (chestOpen == null || chestOpen.getName() == null || chestOpen.getName().equals(Net.name)) {
			return;
		}
		if (chestOpen.getDepth() != Dungeon.depth || Dungeon.level == null) {
			return;
		}
		Game.runOnRenderThread(() -> {
			if (Dungeon.level != null) {
				if (Dungeon.level.heaps != null) {
					Heap heap = Dungeon.level.heaps.get(chestOpen.getPos());
					if (heap != null && heap.type != Heap.Type.HEAP) {
						heap.open(null);
					}
				}
				Char ch = Actor.findChar(chestOpen.getPos());
				if (ch instanceof Mimic) {
					((Mimic) ch).stopHiding();
				}
			}
		});
	}

	// SPDNet Co-op: Синхронизация мобов и боссов
	public static void handleMobDamage(SMobDamage mobDamage) {
		if (mobDamage == null || mobDamage.getName() == null || mobDamage.getName().equals(Net.name)) {
			return;
		}
		if (mobDamage.getDepth() != Dungeon.depth || Dungeon.level == null) {
			return;
		}
		Game.runOnRenderThread(() -> {
			Mob mob = Mob.findBySyncId(mobDamage.getSyncId(), mobDamage.getPos());
			if (mob != null && mob.isAlive()) {
				mob.HP = mobDamage.getCurrentHP();
				if (mob instanceof Mimic && ((Mimic) mob).alignment == Char.Alignment.NEUTRAL) {
					((Mimic) mob).stopHiding();
				}
				// Выравнивание позиции при рассинхронизации клеток
				if (mob.pos != mobDamage.getPos() && mobDamage.getPos() >= 0 && mobDamage.getPos() < Dungeon.level.length()) {
					mob.pos = mobDamage.getPos();
					if (mob.sprite != null && ShatteredPixelDungeon.scene() instanceof GameScene) {
						mob.sprite.place(mob.pos);
					}
				}
				if (mob.sprite != null && ShatteredPixelDungeon.scene() instanceof GameScene) {
					mob.sprite.bloodBurstA(mob.sprite.center(), mobDamage.getDamage());
					mob.sprite.showStatusWithIcon(CharSprite.NEGATIVE, Integer.toString(mobDamage.getDamage()), FloatingText.PHYS_DMG);
				}
				NetHero attacker = NetHero.getPlayerFromDungeon(mobDamage.getAttackerName());
				if (attacker != null) {
					mob.aggro(attacker);
				}
				if (mob.HP <= 0) {
					mob.isNetRemote = true;
					Level.suppressMobDrops = true;
					try {
						mob.die(attacker);
					} finally {
						Level.suppressMobDrops = false;
						mob.isNetRemote = false;
					}
				}
			}
		});
	}

	public static void handleMobDie(SMobDie mobDie) {
		if (mobDie == null || mobDie.getName() == null || mobDie.getName().equals(Net.name)) {
			return;
		}
		if (mobDie.getDepth() != Dungeon.depth || Dungeon.level == null) {
			return;
		}
		Game.runOnRenderThread(() -> {
			Mob mob = Mob.findBySyncId(mobDie.getSyncId(), mobDie.getPos());
			if (mob != null && mob.isAlive()) {
				mob.isNetRemote = true;
				Level.suppressMobDrops = true;
				try {
					mob.die(null);
				} finally {
					Level.suppressMobDrops = false;
					mob.isNetRemote = false;
				}
			}
		});
	}

	public static void handleMobMove(SMobMove mobMove) {
		if (mobMove == null || mobMove.getName() == null || mobMove.getName().equals(Net.name)) {
			return;
		}
		if (mobMove.getDepth() != Dungeon.depth || Dungeon.level == null) {
			return;
		}
		Game.runOnRenderThread(() -> {
			Mob mob = Mob.findBySyncId(mobMove.getSyncId(), mobMove.getFromPos());
			if (mob == null && mobMove.getMobClass() != null && !mobMove.getMobClass().isEmpty()) {
				// Автоматически спавним отсутствующего моба на клиенте
				try {
					Class<?> cl = Class.forName(mobMove.getMobClass());
					mob = (Mob) Reflection.newInstance(cl);
					if (mob != null) {
						mob.syncId = mobMove.getSyncId();
						mob.pos = mobMove.getToPos();
						if (mobMove.getHp() > 0) mob.HP = mobMove.getHp();
						if (mobMove.getHt() > 0) mob.HT = mobMove.getHt();
						if (ShatteredPixelDungeon.scene() instanceof GameScene) {
							GameScene.add(mob);
						} else if (Dungeon.level != null) {
							Dungeon.level.mobs.add(mob);
						}
					}
				} catch (Exception ignored) {}
			}
			if (mob != null && mob.isAlive()) {
				mob.isNetRemote = true;
				try {
					int from = mob.pos;
					mob.pos = mobMove.getToPos();
					if (mob.sprite != null && ShatteredPixelDungeon.scene() instanceof GameScene) {
						mob.moveSprite(from, mob.pos);
					}
					// Устанавливаем цель моба на игрока, вызвавшего перемещение
					NetHero sender = NetHero.getPlayerFromDungeon(mobMove.getName());
					if (sender != null) {
						mob.assignRemoteTarget(sender);
					}
				} finally {
					mob.isNetRemote = false;
				}
			}
		});
	}

	public static void handleMobAttack(SMobAttack mobAttack) {
		if (mobAttack == null || mobAttack.getDepth() != Dungeon.depth || Dungeon.level == null) {
			return;
		}
		Game.runOnRenderThread(() -> {
			Mob mob = Mob.findBySyncId(mobAttack.getSyncId(), -1);
			if (mob == null && mobAttack.getMobClass() != null && !mobAttack.getMobClass().isEmpty()) {
				// Автоматически спавним моба, наносящего урон, если он ещё не появился на клиенте
				try {
					Class<?> cl = Class.forName(mobAttack.getMobClass());
					mob = (Mob) Reflection.newInstance(cl);
					if (mob != null) {
						mob.syncId = mobAttack.getSyncId();
						mob.pos = mobAttack.getMobPos() >= 0 ? mobAttack.getMobPos() : mobAttack.getTargetPos();
						if (ShatteredPixelDungeon.scene() instanceof GameScene) {
							GameScene.add(mob);
						} else if (Dungeon.level != null) {
							Dungeon.level.mobs.add(mob);
						}
					}
				} catch (Exception ignored) {}
			}
			if (mob instanceof Mimic && ((Mimic) mob).alignment == Char.Alignment.NEUTRAL) {
				((Mimic) mob).stopHiding();
			}
			if (mob != null && mob.sprite != null && ShatteredPixelDungeon.scene() instanceof GameScene) {
				mob.sprite.attack(mobAttack.getTargetPos());
			}
		});
	}

	public static void handleMobSpawn(SMobSpawn mobSpawn) {
		if (mobSpawn == null || mobSpawn.getName() == null || mobSpawn.getName().equals(Net.name)) {
			return;
		}
		if (mobSpawn.getDepth() != Dungeon.depth || Dungeon.level == null) {
			return;
		}
		Game.runOnRenderThread(() -> {
			if (mobSpawn.getSyncId() > 0) {
				for (Mob m : Dungeon.level.mobs) {
					if (m != null && m.syncId == mobSpawn.getSyncId()) {
						return;
					}
				}
			}
			try {
				Class<?> cl = Class.forName(mobSpawn.getMobClass());
				Mob mob = (Mob) Reflection.newInstance(cl);
				if (mob != null) {
					mob.syncId = mobSpawn.getSyncId();
					mob.pos = mobSpawn.getPos();
					mob.HT = mobSpawn.getHt();
					mob.HP = mobSpawn.getHp();
					if (ShatteredPixelDungeon.scene() instanceof GameScene) {
						GameScene.add(mob);
					} else if (Dungeon.level != null) {
						Dungeon.level.mobs.add(mob);
					}
					Dungeon.level.maxMobSyncId = Math.max(Dungeon.level.maxMobSyncId, mob.syncId);
				}
			} catch (Exception e) {
				// silent
			}
		});
	}

	public static void handleMobSync(SMobSync mobSync) {
		if (mobSync == null || mobSync.getDepth() != Dungeon.depth || Dungeon.level == null || mobSync.getMobs() == null) {
			return;
		}
		Game.runOnRenderThread(() -> {
			Set<Integer> hostMobIds = new HashSet<>();
			for (SMobSpawn s : mobSync.getMobs()) {
				if (s != null && s.getSyncId() > 0) {
					hostMobIds.add(s.getSyncId());
				}
			}

			// 1. Удаляем с клиента мобов, которых больше нет на хосте (убиты или деспавнились)
			List<Mob> toRemove = new ArrayList<>();
			for (Mob m : Dungeon.level.mobs) {
				if (m != null && m.syncId > 0 && !hostMobIds.contains(m.syncId)) {
					toRemove.add(m);
				}
			}
			for (Mob m : toRemove) {
				m.destroy();
				Dungeon.level.mobs.remove(m);
				Actor.remove(m);
			}

			// 2. Обновляем существующие и спавним отсутствующие мобы
			for (SMobSpawn s : mobSync.getMobs()) {
				if (s == null) continue;
				Mob existing = null;
				for (Mob m : Dungeon.level.mobs) {
					if (m != null && m.syncId == s.getSyncId()) {
						existing = m;
						break;
					}
				}
				if (existing != null) {
					existing.HP = s.getHp();
					existing.HT = s.getHt();
					if (existing.pos != s.getPos()) {
						existing.pos = s.getPos();
						if (existing.sprite != null) {
							existing.sprite.place(s.getPos());
						}
					}
				} else {
					try {
						Class<?> cl = Class.forName(s.getMobClass());
						Mob mob = (Mob) Reflection.newInstance(cl);
						if (mob != null) {
							mob.syncId = s.getSyncId();
							mob.pos = s.getPos();
							mob.HT = s.getHt();
							mob.HP = s.getHp();
							if (ShatteredPixelDungeon.scene() instanceof GameScene) {
								GameScene.add(mob);
							} else {
								Dungeon.level.mobs.add(mob);
							}
						}
					} catch (Exception ignored) {}
				}
				Dungeon.level.maxMobSyncId = Math.max(Dungeon.level.maxMobSyncId, s.getSyncId());
			}
		});
	}
}
