package app.jsch.androidtunnel.utils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Locale;



public class ResolveAddrs {
    private static BigInteger ipAddress;
    public static String resolveHostDomain(String hostIP) {
            try {
                int mask = 128;
                InetAddress a = InetAddress.getByName(hostIP);
                hostIP = a.getHostAddress();

                if (hostIP.contains(":")) {
                    ipAddress = BigInteger.ZERO;
                    for (byte byt : a.getAddress()) {
                        mask -= 8;
                        ipAddress = ipAddress.add(BigInteger.valueOf((byt & 0xFF)).shiftLeft(mask));
                    }
                    long ip = ipAddress.longValue();
                    return String.format(Locale.US,  "%d.%d.%d.%d", (ip >> 24) % 256, (ip >> 16) % 256, (ip >> 8) % 256, ip % 256);
                }
            } catch (Exception e) {
                return hostIP;
            }
            return hostIP;

    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
