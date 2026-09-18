package com.shatteredpixel.shatteredpixeldungeon.spdnet;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.Image;

import lombok.Getter;

public enum Mode {
	IRONMAN("铁人模式", "玩家之间没有实质性交互，随机种子，使用你的技巧争夺排行榜上的高分，证明自己是地牢高手。", Assets.Sprites.RATKING, 0, 3, 14, 14),
	FUN("娱乐模式", "随便摸，乐就行了。 : )", Assets.Sprites.RAT, 0, 2, 14, 13),
	DAILY("每日挑战", "每天都有新的地牢，每天都有新排行榜。选择组别开始挑战！", Assets.Sprites.RAT, 0, 17, 14, 13);
	// SPECTATOR("观战模式", "观看其他玩家的游戏", Assets.Sprites.RAT, 0, 17, 14, 13);
	private final String name;
	private final String description;
	private final String icon;
	private final int iconLeft;
	private final int iconTop;
	private final int iconWidth;
	private final int iconHeight;

	Mode(String name, String description, String icon, int iconLeft, int iconTop, int iconWidth, int iconHeight) {
		this.name = name;
		this.description = description;
		this.icon = icon;
		this.iconLeft = iconLeft;
		this.iconTop = iconTop;
		this.iconWidth = iconWidth;
		this.iconHeight = iconHeight;
	}

	public String getName() {
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		if (isZh) {
			return name;
		}
		if (isRu) {
			switch (this) {
				case IRONMAN: return "Железный человек";
				case FUN: return "Кооператив";
				case DAILY: return "Ежедневное испытание";
				default: return name;
			}
		}
		switch (this) {
			case IRONMAN: return "Ironman Mode";
			case FUN: return "Fun Mode";
			case DAILY: return "Daily Challenge";
			default: return name;
		}
	}

	public String getDescription() {
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		if (isZh) {
			return description;
		}
		if (isRu) {
			switch (this) {
				case IRONMAN: return "Случайный сид без прямого взаимодействия между игроками. Испытайте навыки и займите высшие места в таблице лидеров!";
				case FUN: return "Свободная совместная игра без ограничений. Играйте и получайте удовольствие : )";
				case DAILY: return "Каждый день новое подземелье и новая таблица лидеров. Выберите категорию и начните испытание!";
				default: return description;
			}
		}
		switch (this) {
			case IRONMAN: return "Random seed with no direct interaction between players. Compete for top leaderboard scores!";
			case FUN: return "Casual co-op multiplayer. Just have fun! : )";
			case DAILY: return "New dungeon and leaderboard every day. Choose a category and challenge!";
			default: return description;
		}
	}

	// 不能在枚举中直接放Image, 不然切换Scene的时候图像会失效, 每次调用都需要重新创建Image对象
	public Image getIcon() {
		return new Image(icon, iconLeft, iconTop, iconWidth, iconHeight);
	}
}