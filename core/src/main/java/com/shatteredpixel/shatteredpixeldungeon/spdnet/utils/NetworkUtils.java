package com.shatteredpixel.shatteredpixeldungeon.spdnet.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Утилиты для работы с сетевыми интерфейсами и локальными IP-адресами.
 */
public class NetworkUtils {

	/**
	 * Получить наиболее подходящий локальный IPv4 адрес для отображения хосту.
	 * Приоритет отдается Wi-Fi (wlan) и точкам доступа Hotspot (ap, softap).
	 */
	public static String getBestLocalIp() {
		List<String> wifiIps = new ArrayList<>();
		List<String> otherIps = new ArrayList<>();

		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			if (interfaces == null) {
				return "127.0.0.1";
			}

			for (NetworkInterface iface : Collections.list(interfaces)) {
				if (iface.isLoopback() || !iface.isUp()) {
					continue;
				}

				String ifaceName = iface.getName().toLowerCase();
				boolean isWifiOrHotspot = ifaceName.contains("wlan") ||
						ifaceName.contains("ap") ||
						ifaceName.contains("softap") ||
						ifaceName.contains("p2p") ||
						ifaceName.contains("rndis");

				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				for (InetAddress addr : Collections.list(addresses)) {
					if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
						String hostAddress = addr.getHostAddress();
						if (isWifiOrHotspot) {
							wifiIps.add(hostAddress);
						} else {
							otherIps.add(hostAddress);
						}
					}
				}
			}
		} catch (SocketException e) {
			NLog.w("NetworkUtils error: " + e.getMessage());
		}

		// Если есть Wi-Fi или точка доступа
		if (!wifiIps.isEmpty()) {
			// На Android точка доступа часто имеет адрес 192.168.43.1
			for (String ip : wifiIps) {
				if (ip.startsWith("192.168.43.")) {
					return ip;
				}
			}
			return wifiIps.get(0);
		}

		// Другие сетевые интерфейсы (Ethernet и т.д.)
		if (!otherIps.isEmpty()) {
			return otherIps.get(0);
		}

		return "127.0.0.1";
	}

	/**
	 * Получить список всех найденных локальных IPv4 адресов.
	 */
	public static List<String> getAllLocalIps() {
		List<String> list = new ArrayList<>();
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			if (interfaces != null) {
				for (NetworkInterface iface : Collections.list(interfaces)) {
					if (iface.isLoopback() || !iface.isUp()) {
						continue;
					}
					for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
						if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
							list.add(addr.getHostAddress());
						}
					}
				}
			}
		} catch (SocketException ignored) {
		}
		if (list.isEmpty()) {
			list.add("127.0.0.1");
		}
		return list;
	}
}
