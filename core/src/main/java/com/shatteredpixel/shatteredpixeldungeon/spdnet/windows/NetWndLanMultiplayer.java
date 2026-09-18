package com.shatteredpixel.shatteredpixeldungeon.spdnet.windows;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.NetSettings;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.lan.LanServer;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.ui.BlueButton;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.ui.NetIcons;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.utils.NetworkUtils;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.Net;
import com.shatteredpixel.shatteredpixeldungeon.spdnetbutcopy.ui.SPDNetTextInput;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.PointerArea;

import java.io.IOException;

/**
 * Окно локального мультиплеера (LAN Co-op).
 * Позволяет создать локальный сервер (Хост) или подключиться по IP (Клиент).
 */
public class NetWndLanMultiplayer extends NetWindow {

	private static final int WIDTH = 145;
	private static final int MARGIN = 2;
	private static final int BUTTON_HEIGHT = 16;
	private static final int INPUT_HEIGHT = 16;

	private SPDNetTextInput nameInput;
	private SPDNetTextInput hostIpInput;
	private PointerArea nameClickArea;
	private PointerArea ipClickArea;
	private BlueButton btnHost;
	private BlueButton btnJoin;
	private RenderedTextBlock statusText;

	public NetWndLanMultiplayer() {
		super();

		if (PixelScene.landscape()) {
			offset(0, -35);
		} else {
			offset(0, -35);
		}

		float pos = MARGIN;

		IconTitle title = new IconTitle(NetIcons.get(NetIcons.PLAYERS), "Локальная сеть (LAN)");
		title.setRect(0, pos, WIDTH, 18);
		add(title);
		pos = title.bottom() + MARGIN * 2;

		// ---- Никнейм ----
		RenderedTextBlock nameLabel = PixelScene.renderTextBlock("Ваш никнейм:", 7);
		nameLabel.maxWidth(WIDTH);
		nameLabel.setPos(MARGIN, pos);
		add(nameLabel);
		pos = nameLabel.bottom() + MARGIN;

		int textSize = (int) PixelScene.uiCamera.zoom * 9;
		float nameTop = pos;

		nameInput = new SPDNetTextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, textSize) {
			@Override
			public void enterPressed() {
				if (hostIpInput != null) {
					hostIpInput.setActive(true);
				}
			}

			@Override
			public void onActivated() {
				if (hostIpInput != null) {
					hostIpInput.setActive(false);
				}
			}
		};
		nameInput.setText(NetSettings.getLanName());
		nameInput.setMaxLength(25);
		add(nameInput);

		nameClickArea = new PointerArea(MARGIN, pos, WIDTH - MARGIN * 2, INPUT_HEIGHT) {
			@Override
			protected void onClick(PointerEvent event) {
				nameInput.setActive(true);
			}
		};
		add(nameClickArea);

		pos += INPUT_HEIGHT + MARGIN * 3;

		// ---- Секция Хоста ----
		String bestIp = NetworkUtils.getBestLocalIp();
		RenderedTextBlock hostHeader = PixelScene.renderTextBlock("--- Создать сервер (Хост) ---", 7);
		hostHeader.hardlight(TITLE_COLOR);
		hostHeader.maxWidth(WIDTH);
		hostHeader.setPos((WIDTH - hostHeader.width()) / 2, pos);
		add(hostHeader);
		pos = hostHeader.bottom() + MARGIN;

		RenderedTextBlock myIpText = PixelScene.renderTextBlock("Ваш IP: " + bestIp, 8);
		myIpText.hardlight(0xFFFF88);
		myIpText.maxWidth(WIDTH);
		myIpText.setPos(MARGIN, pos);
		add(myIpText);
		pos = myIpText.bottom() + MARGIN;

		RenderedTextBlock hostHint = PixelScene.renderTextBlock("(Раздайте точку доступа или подключитесь к одному Wi-Fi)", 6);
		hostHint.hardlight(0xAAAAAA);
		hostHint.maxWidth(WIDTH);
		hostHint.setPos(MARGIN, pos);
		add(hostHint);
		pos = hostHint.bottom() + MARGIN;

		btnHost = new BlueButton("Создать сервер") {
			@Override
			public synchronized void update() {
				super.update();
				if (LanServer.isRunning()) {
					text.text("Остановить сервер");
					text.hardlight(0xFF7777);
				} else {
					text.text("Создать сервер");
					text.hardlight(0xFFFFFF);
				}
			}

			@Override
			protected void onPointerDown() {
				super.onPointerDown();
				PointerEvent.clearKeyboardThisPress = false;
			}

			@Override
			protected void onPointerUp() {
				super.onPointerUp();
				PointerEvent.clearKeyboardThisPress = false;
			}

			@Override
			protected void onClick() {
				saveSettings();
				if (LanServer.isRunning()) {
					LanServer.stop();
					Net.disConnect();
				} else {
					try {
						String nick = nameInput.getText().trim();
						if (nick.isEmpty()) nick = "Host";
						LanServer.start(LanServer.DEFAULT_PORT);
						LanServer.setHostPlayerName(nick);
						Net.connectTo("http://127.0.0.1:" + LanServer.DEFAULT_PORT + "/spdnet", nick);
					} catch (IOException e) {
						NetWindow.error("Ошибка запуска сервера: " + e.getMessage());
					}
				}
			}
		};
		btnHost.setRect(MARGIN, pos, WIDTH - MARGIN * 2, BUTTON_HEIGHT);
		add(btnHost);
		pos = btnHost.bottom() + MARGIN * 3;

		// ---- Секция Клиента ----
		RenderedTextBlock joinHeader = PixelScene.renderTextBlock("--- Подключиться к хосту ---", 7);
		joinHeader.hardlight(TITLE_COLOR);
		joinHeader.maxWidth(WIDTH);
		joinHeader.setPos((WIDTH - joinHeader.width()) / 2, pos);
		add(joinHeader);
		pos = joinHeader.bottom() + MARGIN;

		RenderedTextBlock ipLabel = PixelScene.renderTextBlock("IP адрес хоста:", 7);
		ipLabel.maxWidth(WIDTH);
		ipLabel.setPos(MARGIN, pos);
		add(ipLabel);
		pos = ipLabel.bottom() + MARGIN;

		float hostIpTop = pos;

		hostIpInput = new SPDNetTextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, textSize) {
			@Override
			public void enterPressed() {
				connectAsClient();
			}

			@Override
			public void onActivated() {
				if (nameInput != null) {
					nameInput.setActive(false);
				}
			}
		};
		hostIpInput.setText(NetSettings.getLanHostIp());
		hostIpInput.setMaxLength(30);
		add(hostIpInput);

		ipClickArea = new PointerArea(MARGIN, pos, WIDTH - MARGIN * 2, INPUT_HEIGHT) {
			@Override
			protected void onClick(PointerEvent event) {
				hostIpInput.setActive(true);
			}
		};
		add(ipClickArea);

		pos += INPUT_HEIGHT + MARGIN;

		btnJoin = new BlueButton("Подключиться") {
			@Override
			public synchronized void update() {
				super.update();
				if (Net.isConnected() && !LanServer.isRunning()) {
					text.text("Отключиться");
					text.hardlight(0xFF7777);
				} else {
					text.text("Подключиться");
					text.hardlight(0xFFFFFF);
				}
			}

			@Override
			protected void onPointerDown() {
				super.onPointerDown();
				PointerEvent.clearKeyboardThisPress = false;
			}

			@Override
			protected void onPointerUp() {
				super.onPointerUp();
				PointerEvent.clearKeyboardThisPress = false;
			}

			@Override
			protected void onClick() {
				if (Net.isConnected() && !LanServer.isRunning()) {
					Net.disConnect();
				} else {
					connectAsClient();
				}
			}
		};
		btnJoin.setRect(MARGIN, pos, WIDTH - MARGIN * 2, BUTTON_HEIGHT);
		add(btnJoin);
		pos = btnJoin.bottom() + MARGIN * 2;

		// ---- Статус ----
		statusText = new RenderedTextBlock("Статус: Проверка...", 7) {
			@Override
			public synchronized void update() {
				super.update();
				if (LanServer.isRunning()) {
					text("Хост запущен | Игроков: " + LanServer.getPlayerCount() + (Net.isConnected() ? " (В сети)" : ""));
					hardlight(0x00FF00);
				} else if (Net.isConnected()) {
					text("Подключено (" + Net.name + ")");
					hardlight(0x00FF00);
				} else {
					text("Не подключено");
					hardlight(0xAAAAAA);
				}
			}
		};
		statusText.maxWidth(WIDTH);
		statusText.setPos(MARGIN, pos);
		add(statusText);
		pos = statusText.bottom() + MARGIN * 2;

		// ---- Кнопка Закрыть ----
		BlueButton btnClose = new BlueButton("Закрыть") {
			@Override
			protected void onPointerDown() {
				super.onPointerDown();
				PointerEvent.clearKeyboardThisPress = false;
			}

			@Override
			protected void onPointerUp() {
				super.onPointerUp();
				PointerEvent.clearKeyboardThisPress = false;
			}

			@Override
			protected void onClick() {
				saveSettings();
				hide();
			}
		};
		btnClose.setRect((WIDTH - 60) / 2, pos, 60, BUTTON_HEIGHT);
		add(btnClose);
		pos += BUTTON_HEIGHT + MARGIN;
		resize(WIDTH, (int) pos);

		float inputWidth = WIDTH - MARGIN * 2;
		nameInput.setRect(MARGIN, nameTop, inputWidth, INPUT_HEIGHT);
		nameClickArea.x = MARGIN;
		nameClickArea.y = nameTop;
		nameClickArea.width = inputWidth;
		nameClickArea.height = INPUT_HEIGHT;

		hostIpInput.setRect(MARGIN, hostIpTop, inputWidth, INPUT_HEIGHT);
		ipClickArea.x = MARGIN;
		ipClickArea.y = hostIpTop;
		ipClickArea.width = inputWidth;
		ipClickArea.height = INPUT_HEIGHT;
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		if (nameInput != null) {
			nameInput.setRect(nameInput.left(), nameInput.top(), nameInput.width(), nameInput.height());
		}
		if (hostIpInput != null) {
			hostIpInput.setRect(hostIpInput.left(), hostIpInput.top(), hostIpInput.width(), hostIpInput.height());
		}
	}

	private void saveSettings() {
		String nick = nameInput.getText().trim();
		if (!nick.isEmpty()) {
			NetSettings.setLanName(nick);
		}
		String host = hostIpInput.getText().trim();
		if (!host.isEmpty()) {
			NetSettings.setLanHostIp(host);
		}
	}

	private void connectAsClient() {
		saveSettings();
		if (LanServer.isRunning()) {
			NetWindow.info("Вы уже являетесь хостом. Для подключения к другому хосту сначала остановите свой сервер.");
			return;
		}
		String ip = hostIpInput.getText().trim();
		if (ip.isEmpty()) {
			ip = "192.168.43.1";
		}
		String nick = nameInput.getText().trim();
		if (nick.isEmpty()) {
			nick = "Client";
		}
		Net.connectTo("http://" + ip + ":" + LanServer.DEFAULT_PORT + "/spdnet", nick);
	}

	@Override
	public void hide() {
		saveSettings();
		super.hide();
		if (com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon.scene() instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene) {
			com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.ready();
		}
	}
}
