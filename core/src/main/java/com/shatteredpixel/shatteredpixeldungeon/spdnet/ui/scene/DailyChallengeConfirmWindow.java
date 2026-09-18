package com.shatteredpixel.shatteredpixeldungeon.spdnet.ui.scene;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

public class DailyChallengeConfirmWindow extends Window {
	private static final int WIDTH = 120;
	private static final int MARGIN = 4;
	private static final int BUTTON_HEIGHT = 18;

	public DailyChallengeConfirmWindow(Runnable onConfirm) {
		super(WIDTH, 0, Chrome.get(Chrome.Type.WINDOW));

		int y = MARGIN;

		boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
		boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
				|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;

		String titleStr = isRu ? "Внимание" : (isZh ? "警告" : "Warning");
		RenderedTextBlock title = PixelScene.renderTextBlock(titleStr, 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((WIDTH - title.width()) / 2, y);
		add(title);
		y += title.height() + MARGIN;

		String msgStr = isRu ? "Вы уже проходили эту категорию сегодня.\n\nИгра запустится в обычном режиме, результат не попадёт в ежедневную таблицу лидеров." :
				(isZh ? "你已经创建过该组别的挑战。\n\n将以普通模式游玩，成绩不会计入每日挑战排行榜。" :
						"You have already played this category today.\n\nIt will run in normal mode without leaderboard score submission.");
		RenderedTextBlock message = PixelScene.renderTextBlock(msgStr, 7);
		message.maxWidth(WIDTH - MARGIN * 2);
		message.setPos(MARGIN, y);
		add(message);
		y += message.height() + MARGIN * 2;

		int buttonWidth = (WIDTH - MARGIN * 3) / 2;

		String contStr = isRu ? "Продолжить" : (isZh ? "继续" : "Continue");
		StyledButton confirmBtn = new StyledButton(Chrome.Type.RED_BUTTON, contStr, 8) {
			@Override
			protected void onClick() {
				onConfirm.run();
				hide();
			}
		};
		confirmBtn.setRect(MARGIN, y, buttonWidth, BUTTON_HEIGHT);
		add(confirmBtn);

		String cancelStr = isRu ? "Отмена" : (isZh ? "取消" : "Cancel");
		StyledButton cancelBtn = new StyledButton(Chrome.Type.RED_BUTTON, cancelStr, 8) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		cancelBtn.setRect(MARGIN * 2 + buttonWidth, y, buttonWidth, BUTTON_HEIGHT);
		add(cancelBtn);
		y += BUTTON_HEIGHT + MARGIN;

		resize(WIDTH, y);
	}
}
