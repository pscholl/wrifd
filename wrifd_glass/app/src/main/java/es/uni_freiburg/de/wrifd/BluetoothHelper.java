package es.uni_freiburg.de.wrifd;

        import android.bluetooth.BluetoothDevice;

        import java.util.UUID;

        import android.content.Context;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.ArrayAdapter;
        import android.widget.TextView;

        import org.apache.http.util.ByteArrayBuffer;

        import java.util.regex.Pattern;

public class BluetoothHelper {


        public static int PRINTABLE_ASCII_MIN = 0x20; // ' '
        public static int PRINTABLE_ASCII_MAX = 0x7E; // '~'

        public static boolean isPrintableAscii(int c) {
            return c >= PRINTABLE_ASCII_MIN && c <= PRINTABLE_ASCII_MAX;
        }

        public static String bytesToHex(byte[] data) {
            return bytesToHex(data, 0, data.length);
        }

        public static String bytesToHex(byte[] data, int offset, int length) {
            if (length <= 0) {
                return "";
            }

            StringBuilder hex = new StringBuilder();
            for (int i = offset; i < offset + length; i++) {
                hex.append(String.format(" %02X", data[i] % 0xFF));
            }
            hex.deleteCharAt(0);
            return hex.toString();
        }

        public static String bytesToAsciiMaybe(byte[] data) {
            return bytesToAsciiMaybe(data, 0, data.length);
        }

        public static String bytesToAsciiMaybe(byte[] data, int offset, int length) {
            StringBuilder ascii = new StringBuilder();
            boolean zeros = false;
            for (int i = offset; i < offset + length; i++) {
                int c = data[i] & 0xFF;
                if (isPrintableAscii(c)) {
                    if (zeros) {
                        return null;
                    }
                    ascii.append((char) c);
                } else if (c == 0) {
                    zeros = true;
                } else {
                    return null;
                }
            }
            return ascii.toString();
        }

        public static byte[] hexToBytes(String hex) {
            ByteArrayBuffer bytes = new ByteArrayBuffer(hex.length() / 2);
            for (int i = 0; i < hex.length(); i++) {
                if (hex.charAt(i) == ' ') {
                    continue;
                }

                String hexByte;
                if (i + 1 < hex.length()) {
                    hexByte = hex.substring(i, i + 2).trim();
                    i++;
                } else {
                    hexByte = hex.substring(i, i + 1);
                }

                bytes.append(Integer.parseInt(hexByte, 16));
            }
            return bytes.buffer();
        }

    public static String shortUuidFormat = "0000%04X-0000-1000-8000-00805F9B34FB";

    public static UUID sixteenBitUuid(long shortUuid) {
        assert shortUuid >= 0 && shortUuid <= 0xFFFF;
        return UUID.fromString(String.format(shortUuidFormat, shortUuid & 0xFFFF));
    }

    public static String getDeviceInfoText(BluetoothDevice device, int rssi, byte[] scanRecord) {
        return new StringBuilder()
                .append("Name: ").append(device.getName())
                .append("\nMAC: ").append(device.getAddress())
                .append("\nRSSI: ").append(rssi)
                .append("\nScan Record:").append(parseScanRecord(scanRecord))
                .toString();
    }

    // Bluetooth Spec V4.0 - Vol 3, Part C, section 8
    private static String parseScanRecord(byte[] scanRecord) {
        StringBuilder output = new StringBuilder();
        int i = 0;
        while (i < scanRecord.length) {
            int len = scanRecord[i++] & 0xFF;
            if (len == 0) break;
            switch (scanRecord[i] & 0xFF) {
                // https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile
                case 0x0A: // Tx Power
                    output.append("\n  Tx Power: ").append(scanRecord[i+1]);
                    break;
                case 0xFF: // Manufacturer Specific data (RFduinoBLE.advertisementData)
                    output.append("\n  Advertisement Data: ")
                            .append(bytesToHex(scanRecord, i + 3, len));

                    String ascii = bytesToAsciiMaybe(scanRecord, i + 3, len);
                    if (ascii != null) {
                        output.append(" (\"").append(ascii).append("\")");
                    }
                    break;
            }
            i += len;
        }
        return output.toString();
    }
}
