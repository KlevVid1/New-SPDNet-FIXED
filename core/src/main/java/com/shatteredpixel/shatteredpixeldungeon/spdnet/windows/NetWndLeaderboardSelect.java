package com.shatteredpixel.shatteredpixeldungeon.spdnet.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.Mode;
import com.shatteredpixel.shatteredpixeldungeon.spdnetbutcopy.scene.NetRankingsScene;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.Net;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.Sender;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CRequestLeaderboard;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.ColorBlock;

public class NetWndLeaderboardSelect extends Window {
	private static final int WIDTH = 130;
	private static final int GAP = 2;
	private static final int BTN_HEIGHT = 18;

	RenderedTextBlock title;
	ColorBlock sep1;
	RedButton btnPlayerType;
	RedButton btnPlayerName;
	ColorBlock sep2;
	RedButton btnChallenge;
	CheckBox chkWinOnly;
	RedButton btnGameMode;
	ColorBlock sep3;
	CheckBox chkBannedOnly;
	RedButton btnSortCriteria;

	public NetWndLeaderboardSelect() {
		int currentHeight = 0;

		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		String titleStr = isRu ? "Настройки таблицы лидеров" : (isZh ? "排行榜设置" : "Leaderboard Settings");
		title = PixelScene.renderTextBlock(titleStr, 9);
		title.hardlight(TITLE_COLOR);
		add(title);
		currentHeight += 12;

		sep1 = new ColorBlock(WIDTH, 1, 0xFF000000);
		add(sep1);
		currentHeight += 3;

		btnPlayerType = new RedButton(getPlayerTypeText()) {
			@Override
			protected void onClick() {
				String optTitle = isRu ? "Тип таблицы" : (isZh ? "排行榜类型" : "Board Type");
				String optDesc = isRu ? "Выберите тип таблицы лидеров" : (isZh ? "选择要查看的排行榜类型" : "Select leaderboard type");
				String optMine = isRu ? "Мои рекорды" : (isZh ? "我的记录" : "My Records");
				String optAll = isRu ? "Все игроки" : (isZh ? "所有玩家" : "All Players");

				ShatteredPixelDungeon.scene().addToFront(new WndOptions(optTitle, optDesc, optMine, optAll) {
					@Override
					protected void onSelect(int index) {
						if (index == 0) {
							NetRankingsScene.playerName = Net.name;
						} else {
							NetRankingsScene.playerName = null;
						}
						btnPlayerType.text(getPlayerTypeText());
						updatePlayerNameButton();
					}
				});
			}
		};
		add(btnPlayerType);
		currentHeight += BTN_HEIGHT + GAP;

		btnPlayerName = new RedButton(getPlayerNameText()) {
			@Override
			protected void onClick() {
				if (NetRankingsScene.playerName != null) {
					return;
				}
				String inputTitle = isRu ? "Поиск игрока" : (isZh ? "输入玩家名" : "Player Name");
				String btnOk = isRu ? "OK" : (isZh ? "确定" : "OK");
				String btnCancel = isRu ? "Отмена" : (isZh ? "取消" : "Cancel");

				ShatteredPixelDungeon.scene().addToFront(new WndTextInput(inputTitle, null, NetRankingsScene.playerName, 30, false, btnOk, btnCancel) {
					@Override
					public void onSelect(boolean positive, String text) {
						if (positive && text != null && !text.trim().isEmpty()) {
							NetRankingsScene.playerName = text.trim();
							btnPlayerName.text(getPlayerNameText());
						}
					}
				});
			}
		};
		btnPlayerName.enable(NetRankingsScene.playerName == null);
		add(btnPlayerName);
		currentHeight += BTN_HEIGHT + GAP;

		sep2 = new ColorBlock(WIDTH, 1, 0xFF000000);
		add(sep2);
		currentHeight += 3;

		btnChallenge = new RedButton(getChallengeText()) {
			@Override
			protected void onClick() {
				String[] options = new String[11];
				options[0] = isRu ? "Все" : (isZh ? "不筛选" : "Any");
				for (int i = 0; i <= 9; i++) {
					options[i + 1] = i + (isRu ? " исп." : (isZh ? "挑战" : " chlng."));
				}
				String chTitle = isRu ? "Испытания" : (isZh ? "挑战数量" : "Challenges");
				String chDesc = isRu ? "Фильтр по количеству испытаний" : (isZh ? "选择挑战数量筛选条件" : "Filter by challenge count");
				ShatteredPixelDungeon.scene().addToFront(new WndOptions(chTitle, chDesc, options) {
					@Override
					protected void onSelect(int index) {
						if (index == 0) {
							NetRankingsScene.challengeCount = null;
						} else {
							NetRankingsScene.challengeCount = index - 1;
						}
						btnChallenge.text(getChallengeText());
					}
				});
			}
		};
		add(btnChallenge);
		currentHeight += BTN_HEIGHT + GAP;

		String winOnlyStr = isRu ? "Только победы" : (isZh ? "只显示胜利" : "Wins only");
		chkWinOnly = new CheckBox(winOnlyStr) {
			@Override
			protected void onClick() {
				super.onClick();
				NetRankingsScene.winOnly = checked() ? true : null;
			}
		};
		chkWinOnly.checked(NetRankingsScene.winOnly != null && NetRankingsScene.winOnly);
		add(chkWinOnly);
		currentHeight += BTN_HEIGHT + GAP;

		btnGameMode = new RedButton(getGameModeText()) {
			@Override
			protected void onClick() {
				String modeTitle = isRu ? "Режим игры" : (isZh ? "游戏模式" : "Game Mode");
				String modeDesc = isRu ? "Фильтр по режиму игры" : (isZh ? "选择游戏模式筛选条件" : "Filter by game mode");
				String optAny = isRu ? "Все" : (isZh ? "不筛选" : "Any");
				String optFun = isRu ? "Кооператив" : (isZh ? "标准模式" : "Standard");
				String optIron = isRu ? "Железный человек" : (isZh ? "铁人模式" : "Ironman");
				String optDaily = isRu ? "Ежедневное испытание" : (isZh ? "每日挑战" : "Daily Challenge");

				ShatteredPixelDungeon.scene().addToFront(new WndOptions(modeTitle, modeDesc, optAny, optFun, optIron, optDaily) {
					@Override
					protected void onSelect(int index) {
						if (index == 0) {
							NetRankingsScene.gameMode = null;
						} else if (index == 1) {
							NetRankingsScene.gameMode = "FUN";
						} else if (index == 2) {
							NetRankingsScene.gameMode = "IRONMAN";
						} else if (index == 3) {
							NetRankingsScene.gameMode = "DAILY";
						}
						btnGameMode.text(getGameModeText());
					}
				});
			}
		};
		add(btnGameMode);
		currentHeight += BTN_HEIGHT + GAP;

		String bannedOnlyStr = isRu ? "Только заблокированные" : (isZh ? "只显示被封禁玩家" : "Banned only");
		chkBannedOnly = new CheckBox(bannedOnlyStr) {
			@Override
			protected void onClick() {
				super.onClick();
				NetRankingsScene.bannedOnly = checked() ? true : null;
			}
		};
		chkBannedOnly.checked(NetRankingsScene.bannedOnly != null && NetRankingsScene.bannedOnly);
		add(chkBannedOnly);
		currentHeight += BTN_HEIGHT + GAP;

		sep3 = new ColorBlock(WIDTH, 1, 0xFF000000);
		add(sep3);
		currentHeight += 3;

		btnSortCriteria = new RedButton(getSortCriteriaText()) {
			@Override
			protected void onClick() {
				String sortTitle = isRu ? "Сортировка" : (isZh ? "排序方式" : "Sort By");
				String sortDesc = isRu ? "Выберите порядок сортировки" : (isZh ? "选择排序方式" : "Select sorting order");
				String optRecent = isRu ? "Недавние" : (isZh ? "最近通关" : "Most Recent");
				String optScore = isRu ? "По очкам" : (isZh ? "分数最高" : "Highest Score");
				String optTime = isRu ? "По времени" : (isZh ? "通关时间最短" : "Fastest Time");

				ShatteredPixelDungeon.scene().addToFront(new WndOptions(sortTitle, sortDesc, optRecent, optScore, optTime) {
					@Override
					protected void onSelect(int index) {
						switch (index) {
							case 0:
								NetRankingsScene.sortCriteria = "id";
								break;
							case 1:
								NetRankingsScene.sortCriteria = "score";
								break;
							case 2:
								NetRankingsScene.sortCriteria = "duration";
								break;
						}
						btnSortCriteria.text(getSortCriteriaText());
					}
				});
			}
		};
		add(btnSortCriteria);
		currentHeight += BTN_HEIGHT;

		resize(WIDTH, currentHeight);
		layout();
	}

	private String getPlayerTypeText() {
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		if (NetRankingsScene.playerName == null) {
			return isRu ? "Таблица: Все игроки" : (isZh ? "排行榜: 所有玩家" : "Board: All Players");
		} else {
			return isRu ? "Таблица: Мои рекорды" : (isZh ? "排行榜: 我的记录" : "Board: My Records");
		}
	}

	private String getPlayerNameText() {
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		if (NetRankingsScene.playerName == null) {
			return isRu ? "Поиск игрока: Ввести" : (isZh ? "搜索玩家: 点击输入" : "Search player: Tap to enter");
		} else if (NetRankingsScene.playerName.equals(Net.name)) {
			return isRu ? "Игрок: Я (" + Net.name + ")" : (isZh ? "当前: 我 (" + Net.name + ")" : "Player: Me (" + Net.name + ")");
		} else {
			return isRu ? "Игрок: " + NetRankingsScene.playerName : (isZh ? "当前: " + NetRankingsScene.playerName : "Player: " + NetRankingsScene.playerName);
		}
	}

	private void updatePlayerNameButton() {
		btnPlayerName.enable(NetRankingsScene.playerName == null);
		btnPlayerName.text(getPlayerNameText());
	}

	private String getChallengeText() {
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		if (NetRankingsScene.challengeCount == null) {
			return isRu ? "Испытания: Все" : (isZh ? "挑战数量: 不筛选" : "Challenges: Any");
		} else {
			return isRu ? ("Испытания: " + NetRankingsScene.challengeCount + " исп.") :
					(isZh ? ("挑战数量: " + NetRankingsScene.challengeCount + "挑战") :
							("Challenges: " + NetRankingsScene.challengeCount));
		}
	}

	private String getGameModeText() {
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		if (NetRankingsScene.gameMode == null) {
			return isRu ? "Режим: Все" : (isZh ? "游戏模式: 不筛选" : "Mode: Any");
		} else if (NetRankingsScene.gameMode.equals("FUN")) {
			return isRu ? "Режим: Кооператив" : (isZh ? "游戏模式: 标准模式" : "Mode: Standard");
		} else if (NetRankingsScene.gameMode.equals("IRONMAN")) {
			return isRu ? "Режим: Железный человек" : (isZh ? "游戏模式: 铁人模式" : "Mode: Ironman");
		} else if (NetRankingsScene.gameMode.equals("DAILY")) {
			return isRu ? "Режим: Ежедневное испытание" : (isZh ? "游戏模式: 每日挑战" : "Mode: Daily Challenge");
		} else {
			Mode mode = Mode.valueOf(NetRankingsScene.gameMode);
			return isRu ? ("Режим: " + mode.getName()) : (isZh ? ("游戏模式: " + mode.getName()) : ("Mode: " + mode.getName()));
		}
	}

	private String getSortCriteriaText() {
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		if (NetRankingsScene.sortCriteria == null || NetRankingsScene.sortCriteria.equals("id")) {
			return isRu ? "Сортировка: Недавние" : (isZh ? "排序: 最近通关" : "Sort: Most Recent");
		} else if (NetRankingsScene.sortCriteria.equals("score")) {
			return isRu ? "Сортировка: По очкам" : (isZh ? "排序: 分数最高" : "Sort: Highest Score");
		} else {
			return isRu ? "Сортировка: По времени" : (isZh ? "排序: 通关时间最短" : "Sort: Fastest Time");
		}
	}

	private void layout() {
		float y = 0;

		title.setPos((width - title.width()) / 2, y);
		y = title.bottom() + 3;

		sep1.y = y;
		y += 3;

		btnPlayerType.setRect(0, y, width, BTN_HEIGHT);
		y = btnPlayerType.bottom() + GAP;

		btnPlayerName.setRect(0, y, width, BTN_HEIGHT);
		y = btnPlayerName.bottom() + GAP;

		sep2.y = y;
		y += 3;

		btnChallenge.setRect(0, y, width, BTN_HEIGHT);
		y = btnChallenge.bottom() + GAP;

		chkWinOnly.setRect(0, y, width, BTN_HEIGHT);
		y = chkWinOnly.bottom() + GAP;

		btnGameMode.setRect(0, y, width, BTN_HEIGHT);
		y = btnGameMode.bottom() + GAP;

		// SPDNet: 只显示被封禁玩家复选框位置
		chkBannedOnly.setRect(0, y, width, BTN_HEIGHT);
		y = chkBannedOnly.bottom() + GAP;

		sep3.y = y;
		y += 3;

		btnSortCriteria.setRect(0, y, width, BTN_HEIGHT);
	}

	@Override
	public void hide() {
		super.hide();
		Sender.sendRequestLeaderboard(new CRequestLeaderboard(
				NetRankingsScene.playerName,
				NetRankingsScene.challengeCount,
				NetRankingsScene.winOnly,
				NetRankingsScene.gameMode,
				NetRankingsScene.sortCriteria,
				1, 10, NetRankingsScene.bannedOnly));
	}
}
