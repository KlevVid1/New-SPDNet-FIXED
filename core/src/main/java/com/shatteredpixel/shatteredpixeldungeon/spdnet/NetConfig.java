package com.shatteredpixel.shatteredpixeldungeon.spdnet;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;

public class NetConfig {
	public static JSONObject config;

	static {
		config = new JSONObject();
		config.put("serverUrl", "http://127.0.0.1:32814/spdnet");
		config.put("webUrl", "http://127.0.0.1:32814");
	}

	public static void refreshConfig() {
	}

	/**
	 * Полностью автономный режим без внешних запросов к Gitee/GitHub
	 */
	public static void refreshConfig(final Net.HttpResponseListener externalListener) {
		if (externalListener != null) {
			externalListener.cancelled();
		}
	}

	public static void getHttpStringFromUrl(String url, Net.HttpResponseListener listener) {
		if (listener != null) {
			listener.cancelled();
		}
	}
}
