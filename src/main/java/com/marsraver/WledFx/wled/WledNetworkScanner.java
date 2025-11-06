package com.marsraver.WledFx.wled;

import com.marsraver.WledFx.wled.model.WledInfo;
import java.net.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WledNetworkScanner {

    public static List<WledInfo> discover() throws Exception {
        String subnet = detectLocalSubnet();
        if (subnet == null) {
            System.err.println("❌ Could not determine local subnet.");
            return Collections.emptyList();
        }

        System.out.println("📡 Detected subnet: " + subnet + ".x");
        List<WledInfo> devices = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(64);

        for (int i = 1; i < 255; i++) {
            final int host = i;
            pool.submit(() -> {
                String ip = subnet + "." + host;
                try {
                    WledClient client = new WledClient(ip);
                    WledInfo info = client.getInfo();
                    
                    devices.add(info);
                    System.out.println("✅ Found " + info);
                } catch (Exception ignored) {
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(25, TimeUnit.SECONDS);
        return devices;
    }

    private static String detectLocalSubnet() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;

            for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                InetAddress inetAddr = addr.getAddress();
                if (inetAddr instanceof Inet4Address && !inetAddr.isLoopbackAddress()) {
                    byte[] bytes = inetAddr.getAddress();
                    return String.format("%d.%d.%d",
                            bytes[0] & 0xFF,
                            bytes[1] & 0xFF,
                            bytes[2] & 0xFF);
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("🔍 Discovering WLED devices on local network...");
        List<WledInfo> devices = discover();
        System.out.printf("%n=== %d WLED devices found ===%n", devices.size());
        devices.forEach(System.out::println);
    }
}

