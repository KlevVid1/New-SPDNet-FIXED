package com.shatteredpixel.shatteredpixeldungeon.spdnet.ui.scene;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.utils.SPDUtils;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndChallenges;

public class ChallengeButton extends StyledButton {
	public ChallengeButton() {
		super(Chrome.Type.WINDOW, getDisabledText(), 9);
		icon(Icons.get(Icons.CHALLENGE_GREY));
		width = 120;
		height = 20;
	}

	private static String getDisabledText() {
		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
		return isRu ? "Без испытаний" : (isZh ? "挑战未开启" : "No challenges");
	}

	@Override
	protected void onClick() {
		ShatteredPixelDungeon.scene().addToFront(new WndChallenges(SPDSettings.challenges(), true));
	}

	@Override
	public void update() {
		super.update();
		if (SPDSettings.challenges() > 0) {
			boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
			boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
					|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
			String prefix = isRu ? "Испытаний: " : (isZh ? "当前挑战数量: " : "Challenges: ");
			text(prefix + SPDUtils.activeChallenges(SPDSettings.challenges()));
			icon(Icons.get(Icons.CHALLENGE_COLOR));
		} else {
			text(getDisabledText());
			icon(Icons.get(Icons.CHALLENGE_GREY));
		}
	}
}
