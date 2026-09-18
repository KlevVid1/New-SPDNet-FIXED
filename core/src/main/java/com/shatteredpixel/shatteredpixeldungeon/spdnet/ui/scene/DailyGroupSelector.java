package com.shatteredpixel.shatteredpixeldungeon.spdnet.ui.scene;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.Net;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.Sender;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions.CRequestDailyChallenge;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

public class DailyGroupSelector extends Window {
	private static final int WIDTH_P = 130;
	private static final int WIDTH_L = 225;
	private static final int MARGIN = 4;
	private static final int BUTTON_HEIGHT = 20;

	public DailyGroupSelector() {
		super(PixelScene.landscape() ? WIDTH_L : WIDTH_P, 110, Chrome.get(Chrome.Type.WINDOW));

		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		String titleStr = isRu ? "Категория испытания" : (isZh ? "选择每日挑战组别" : "Daily Challenge Category");
		RenderedTextBlock title = PixelScene.renderTextBlock(titleStr, 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((width - title.width()) / 2, MARGIN);
		add(title);

		String[] groupNames = isRu ?
				new String[]{"Новичок (0-3)", "Опытный (4-6)", "Мастер (7-9)"} :
				(isZh ? new String[]{"新手(0-3挑)", "高手(4-6挑)", "大师(7-9挑)"} :
						new String[]{"Beginner (0-3)", "Advanced (4-6)", "Master (7-9)"});
		String[] seedKeys = {"dailyGroup0", "dailyGroup1", "dailyGroup2"};

		int buttonWidth = (width - MARGIN * 4) / 3;
		int startY = (int) (title.bottom() + MARGIN * 2);

		for (int i = 0; i < 3; i++) {
			GroupButton button = new GroupButton(this, i, groupNames[i], seedKeys[i], buttonWidth, BUTTON_HEIGHT);
			button.setPos(MARGIN + i * (buttonWidth + MARGIN), startY);
			add(button);
		}

		String hintStr = isRu ? "Каждую категорию можно пройти один раз в день" :
				(isZh ? "每个组别每天只能挑战一次" : "Each category can only be attempted once a day");
		RenderedTextBlock hint = PixelScene.renderTextBlock(hintStr, 7);
		hint.setPos((width - hint.width()) / 2, startY + BUTTON_HEIGHT + MARGIN);
		add(hint);
	}

	private static class GroupButton extends StyledButton {
		private final DailyGroupSelector window;
		private final int groupIndex;
		private final String seedKey;

		public GroupButton(DailyGroupSelector window, int groupIndex, String groupName, String seedKey, int width, int height) {
			super(Chrome.Type.RED_BUTTON, groupName, 8);
			this.window = window;
			this.groupIndex = groupIndex;
			this.seedKey = seedKey;
			this.width = width;
			this.height = height;

			Long seed = Net.seeds.get(seedKey);
			if (seed != null) {
				RenderedTextBlock seedText = PixelScene.renderTextBlock(formatSeed(seed), 6);
				seedText.hardlight(0xCCCCCC);
			}
		}

		private String formatSeed(long seed) {
			String hex = Long.toHexString(seed).toUpperCase();
			while (hex.length() < 8) {
				hex = "0" + hex;
			}
			return hex.substring(0, 4) + "-" + hex.substring(4, 8);
		}

		@Override
		protected void onClick() {
			Sender.sendRequestDailyChallenge(new CRequestDailyChallenge(groupIndex));
			window.destroy();
		}
	}
}
