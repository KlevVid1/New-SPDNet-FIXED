package com.shatteredpixel.shatteredpixeldungeon.spdnet.ui.scene;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.Mode;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.NetInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.watabou.noosa.Game;

public class ModeButton extends StyledButton {

	public ModeButton(Mode mode) {
		super(Chrome.Type.RED_BUTTON, getDisplayText(mode), 9);
		icon(mode.getIcon());
		width = 70;
		height = 20;
	}

	private static String getDisplayText(Mode mode) {
		if (mode == Mode.DAILY && NetInProgress.isDailyChallenge()) {
			String suffix = getDailySuffix(NetInProgress.dailyGroupIndex);
			return mode.getName() + "-" + suffix;
		}
		return mode.getName();
	}

	private static String getDailySuffix(int groupIndex) {
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		if (isZh) {
			if (groupIndex == 0) return "新手";
			if (groupIndex == 1) return "高手";
			if (groupIndex == 2) return "大师";
		} else if (isRu) {
			if (groupIndex == 0) return "Новичок";
			if (groupIndex == 1) return "Опытный";
			if (groupIndex == 2) return "Мастер";
		} else {
			if (groupIndex == 0) return "Beginner";
			if (groupIndex == 1) return "Advanced";
			if (groupIndex == 2) return "Master";
		}
		return "";
	}

	public void setMode(Mode mode) {
		text(getDisplayText(mode));
		icon(mode.getIcon());
	}

	@Override
	protected void onClick() {
		Game.runOnRenderThread(() -> {
			ShatteredPixelDungeon.scene().add(new ModeWindow());
		});
	}

	@Override
	public void update() {
		super.update();
		setMode(NetInProgress.mode);
	}
}
