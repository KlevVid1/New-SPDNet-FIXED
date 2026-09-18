package com.shatteredpixel.shatteredpixeldungeon.spdnet.web;

import static com.shatteredpixel.shatteredpixeldungeon.spdnet.web.Net.getSocket;

import com.alibaba.fastjson.JSON;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.Mode;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.NetInProgress;
import me.catand.spdnet.protocol.Actions;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CAchievement;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CAnkhUsed;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CArmorUpdate;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CCatalogUpdate;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CChatMessage;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CBestiaryUpdate;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CDocumentUpdate;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CEnterDungeon;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CError;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CFloatingText;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CGameEnd;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CGiveItem;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CHero;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CLeaveDungeon;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CNoteCreate;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CNoteId;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CPlayerChangeFloor;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CPlayerMove;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CItemDrop;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CItemPickUp;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CTerrainChange;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CChestOpen;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CMobDamage;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CMobDie;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CMobMove;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CMobAttack;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CMobSpawn;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CRequestLeaderboard;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CRequestPlayerList;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CRequestDailyChallenge;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CViewHero;

/**
 * 此类用于发送消息给服务器
 */
public class Sender {
	private static void emit(String action, String data) {
		if (!Net.isConnected()) {
			return;
		}
		try {
			getSocket().emit(action, data);
		} catch (Exception ignored) {
		}
	}

	public static void sendAchievement(CAchievement achievement) {
		emit(Actions.ACHIEVEMENT.getName(), JSON.toJSONString(achievement));
	}

	public static void sendAnkhUsed(CAnkhUsed ankhUsed) {
		emit(Actions.ANKH_USED.getName(), JSON.toJSONString(ankhUsed));
	}

	public static void sendArmorUpdate(CArmorUpdate armorUpdate) {
		if (NetInProgress.mode == null || NetInProgress.mode == Mode.IRONMAN) {
			return;
		}
		emit(Actions.ARMOR_UPDATE.getName(), JSON.toJSONString(armorUpdate));
	}

	public static void sendChatMessage(CChatMessage message) {
		emit(Actions.CHAT_MESSAGE.getName(), JSON.toJSONString(message));
	}

	public static void sendEnterDungeon(CEnterDungeon enterDungeon) {
		emit(Actions.ENTER_DUNGEON.getName(), JSON.toJSONString(enterDungeon));
	}

	public static void sendError(CError message) {
		emit(Actions.ERROR.getName(), JSON.toJSONString(message));
	}

	public static void sendFloatingText(CFloatingText floatingText) {
		if (NetInProgress.mode == null || NetInProgress.mode == Mode.IRONMAN) {
			return;
		}
		emit(Actions.FLOATING_TEXT.getName(), JSON.toJSONString(floatingText));
	}

	public static void sendGameEnd(CGameEnd gameEnd) {
		emit(Actions.GAME_END.getName(), JSON.toJSONString(gameEnd));
	}

	public static void sendGiveItem(CGiveItem giveItem) {
		emit(Actions.GIVE_ITEM.getName(), JSON.toJSONString(giveItem));
	}

	public static void sendHero(CHero hero) {
		emit(Actions.HERO.getName(), JSON.toJSONString(hero));
	}

	public static void sendLeaveDungeon(CLeaveDungeon leaveDungeon) {
		emit(Actions.LEAVE_DUNGEON.getName(), "{}");
	}

	public static void sendPlayerChangeFloor(CPlayerChangeFloor playerChangeFloor) {
		if (NetInProgress.mode == null || NetInProgress.mode == Mode.IRONMAN) {
			return;
		}
		emit(Actions.PLAYER_CHANGE_FLOOR.getName(), JSON.toJSONString(playerChangeFloor));
	}

	public static void sendPlayerMove(CPlayerMove playerMove) {
		if (NetInProgress.mode == null || NetInProgress.mode == Mode.IRONMAN) {
			return;
		}
		emit(Actions.PLAYER_MOVE.getName(), JSON.toJSONString(playerMove));
	}

	public static void sendRequestLeaderboard(CRequestLeaderboard requestLeaderboard) {
		emit(Actions.REQUEST_LEADERBOARD.getName(), JSON.toJSONString(requestLeaderboard));
	}

	public static void sendRequestPlayerList(CRequestPlayerList requestPlayerList) {
		emit(Actions.REQUEST_PLAYER_LIST.getName(), "{}");
	}

	public static void sendRequestDailyChallenge(CRequestDailyChallenge requestDailyChallenge) {
		emit(Actions.REQUEST_DAILY_CHALLENGE.getName(), JSON.toJSONString(requestDailyChallenge));
	}

	public static void sendViewHero(CViewHero viewHero) {
		emit(Actions.VIEW_HERO.getName(), JSON.toJSONString(viewHero));
	}

	// SPDNet: 地牢留言(Ping)系统 - 创建留言；FUN/DAILY 玩家才可留言（IRONMAN 静默跳过）
	public static void sendNote(CNoteCreate note) {
		if (NetInProgress.mode == null || NetInProgress.mode == Mode.IRONMAN) {
			return;
		}
		emit(Actions.NOTE_CREATE.getName(), JSON.toJSONString(note));
	}

	// SPDNet: 地牢留言(Ping)系统 - 点赞/取消点赞（toggle）；IRONMAN 静默跳过
	public static void sendNoteLike(CNoteId noteId) {
		if (NetInProgress.mode == null || NetInProgress.mode == Mode.IRONMAN) {
			return;
		}
		emit(Actions.NOTE_LIKE.getName(), JSON.toJSONString(noteId));
	}

	// SPDNet: 地牢留言(Ping)系统 - 删除留言；IRONMAN 静默跳过
	public static void sendNoteDelete(CNoteId noteId) {
		if (NetInProgress.mode == null || NetInProgress.mode == Mode.IRONMAN) {
			return;
		}
		emit(Actions.NOTE_DELETE.getName(), JSON.toJSONString(noteId));
	}

	// SPDNet: 发送 Catalog 更新到服务器
	public static void sendCatalogUpdate(CCatalogUpdate catalogUpdate) {
		emit(Actions.CATALOG_UPDATE.getName(), JSON.toJSONString(catalogUpdate));
	}

	// SPDNet: 发送 Bestiary 更新到服务器
	public static void sendBestiaryUpdate(CBestiaryUpdate bestiaryUpdate) {
		emit(Actions.BESTIARY_UPDATE.getName(), JSON.toJSONString(bestiaryUpdate));
	}

	// SPDNet: 发送 Document 更新到服务器
	public static void sendDocumentUpdate(CDocumentUpdate documentUpdate) {
		emit(Actions.DOCUMENT_UPDATE.getName(), JSON.toJSONString(documentUpdate));
	}

	// SPDNet Co-op: Синхронизация предметов на полу
	public static void sendItemDrop(int depth, int pos, Item item) {
		if (item == null) return;
		emit(Actions.ITEM_DROP.getName(), JSON.toJSONString(new CItemDrop(depth, pos, item)));
	}

	public static void sendItemPickUp(int depth, int pos) {
		emit(Actions.ITEM_PICKUP.getName(), JSON.toJSONString(new CItemPickUp(depth, pos)));
	}

	// SPDNet Co-op: Синхронизация мобов и боссов
	public static void sendMobDamage(int depth, int syncId, int pos, int damage, int currentHP, String attackerName) {
		emit(Actions.MOB_DAMAGE.getName(), JSON.toJSONString(new CMobDamage(depth, syncId, pos, damage, currentHP, attackerName)));
	}

	public static void sendMobDie(int depth, int syncId, int pos) {
		emit(Actions.MOB_DIE.getName(), JSON.toJSONString(new CMobDie(depth, syncId, pos)));
	}

	public static void sendMobMove(int depth, int syncId, int fromPos, int toPos) {
		emit(Actions.MOB_MOVE.getName(), JSON.toJSONString(new CMobMove(depth, syncId, fromPos, toPos)));
	}

	public static void sendMobAttack(int depth, int syncId, int targetPos, String targetName, int damage) {
		emit(Actions.MOB_ATTACK.getName(), JSON.toJSONString(new CMobAttack(depth, syncId, targetPos, targetName, damage)));
	}

	public static void sendMobSpawn(int depth, int syncId, String mobClass, int pos, int hp, int ht) {
		emit(Actions.MOB_SPAWN.getName(), JSON.toJSONString(new CMobSpawn(depth, syncId, mobClass, pos, hp, ht)));
	}

	public static void sendTerrainChange(int depth, int pos, int terrain) {
		emit(Actions.TERRAIN_CHANGE.getName(), JSON.toJSONString(new CTerrainChange(depth, pos, terrain)));
	}

	public static void sendChestOpen(int depth, int pos) {
		emit(Actions.CHEST_OPEN.getName(), JSON.toJSONString(new CChestOpen(depth, pos)));
	}
}
