package com.shatteredpixel.shatteredpixeldungeon.spdnet.ui;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.Net;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.windows.NetWindow;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.windows.NetWndChat;
import com.shatteredpixel.shatteredpixeldungeon.ui.Tag;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;

public class ChatBtn extends Tag {

	private Image icon;

	public ChatBtn() {
		super(0xFF4C4C);
		setSize(icon.width() + 6, icon.height() + 6);
		flip(true);
		visible = active = Net.isConnected();
		bg.hardlight(0xffff44);
	}

	@Override
	protected void createChildren() {
		super.createChildren();

		icon = NetIcons.get(NetIcons.CHAT);
		icon.scale.set(0.72f);
		add(icon);
	}

	@Override
	protected void layout() {
		super.layout();
		icon.x = (right() - icon.width() - 2) / 2;
		icon.y = y + 3;
	}

	@Override
	protected void onClick() {
		if (Net.isConnected()) {
			Game.runOnRenderThread(() -> ShatteredPixelDungeon.scene().add(new NetWndChat()));
		} else {
			boolean isRu = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.RUSSIAN;
			boolean isZh = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_SMPL
					|| com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang() == com.shatteredpixel.shatteredpixeldungeon.messages.Languages.CHI_TRAD;
			String title = isRu ? "Не подключено" : (isZh ? "未连接" : "Not connected");
			String msg = isRu ? "Для использования чата необходимо подключение к серверу или локальному хосту." :
					(isZh ? "你必须连接后才能与其他玩家畅聊" : "You must connect to chat with other players.");
			NetWindow.error(title, msg);
		}
	}

	@Override
	public synchronized void update() {
		super.update();
		visible = active = Net.isConnected();
	}
}

