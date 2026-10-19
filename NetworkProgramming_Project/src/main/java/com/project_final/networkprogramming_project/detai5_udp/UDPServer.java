package com.project_final.networkprogramming_project.detai5_udp;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Đề tài 5: Nguyên lý hoạt động của UDP
 * ----------------------------------------
 * UDP Server: nhận gói "Ping", phản hồi "Pong".
 *
 * UDP KHÁC TCP Ở ĐÂU? (so với đề tài 3, 4)
 * ┌──────────────────────┬────────────────────────────┬────────────────────────────┐
 * │                      │ TCP (Socket)               │ UDP (DatagramSocket)       │
 * ├──────────────────────┼────────────────────────────┼────────────────────────────┤
 * │ Kết nối              │ Bắt tay 3 bước trước       │ KHÔNG kết nối, gửi luôn    │
 * │ Số socket ở server   │ ServerSocket + 1 Socket    │ MỘT DatagramSocket cho     │
 * │                      │ riêng cho mỗi client       │ tất cả client              │
 * │ Đơn vị dữ liệu       │ Luồng byte liên tục        │ Từng gói (datagram) độc    │
 * │                      │                            │ lập, giữ nguyên ranh giới  │
 * │ Đảm bảo              │ Đến đủ, đúng thứ tự        │ Có thể mất, trùng, đảo thứ │
 * │                      │                            │ tự — ứng dụng tự xử lý     │
 * └──────────────────────┴────────────────────────────┴────────────────────────────┘
 *
 * Vì không có kết nối, server biết "trả lời cho ai" là nhờ ĐỊA CHỈ NGƯỜI GỬI
 * nằm ngay trong gói nhận được: packet.getAddress() + packet.getPort().
 *
 *   Client (cổng tạm 50123)                   Server (cổng 9876)
 *     │ ── DatagramPacket "Ping" ──────────────▶ │ receive(): biết người gửi là
 *     │                                          │            IP_client:50123
 *     │ ◀────────────── DatagramPacket "Pong" ── │ send() tới IP_client:50123
 *
 * @author Nguyễn Đức Hiếu
 */
public class UDPServer {

    public static final int PORT = 9876;
    public static final String PING = "Ping";
    public static final String PONG = "Pong";

    /**
     * Kích thước buffer nhận. Gói lớn hơn buffer sẽ bị CẮT BỚT mà không báo lỗi,
     * nên buffer phải đủ chứa thông điệp lớn nhất của giao thức.
     */
    private static final int BUFFER_SIZE = 1024;

    public static void run() {
        start(PORT);
    }

    private static void start(int port) {
        System.out.println("=== DE TAI 5: UDP SERVER (PONG) ===");
        // new DatagramSocket(port) gắn (bind) vào 0.0.0.0:port → nhận gói từ mọi card mạng,
        // nên máy khác trong mạng LAN cũng gửi tới được.
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("Server UDP lang nghe cong " + socket.getLocalPort() + ".");
            printLocalAddresses();
            printNetstatHint(port);
            System.out.println("Nhan Ctrl+C de dung server.\n");

            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                // receive() BLOCK cho đến khi có một gói tới.
                socket.receive(request);
                reply(socket, request);
            }
        } catch (BindException e) {
            System.out.println("[X] Cong UDP " + port + " dang bi chuong trinh khac chiem.");
        } catch (IOException e) {
            System.out.println("[X] Loi server UDP: " + e.getMessage());
        }
    }

    private static void reply(DatagramSocket socket, DatagramPacket request) throws IOException {
        // Chỉ giải mã đúng getLength() byte. Dùng buffer.length sẽ dính cả phần rác
        // còn sót lại từ gói trước (lỗi rất hay gặp khi tái sử dụng buffer).
        String message = new String(request.getData(), request.getOffset(), request.getLength(),
                StandardCharsets.UTF_8).trim();
        String sender = request.getAddress().getHostAddress() + ":" + request.getPort();

        String answer = PING.equalsIgnoreCase(message)
                ? PONG
                : "Khong hieu \"" + message + "\" - hay gui \"" + PING + "\"";
        byte[] data = answer.getBytes(StandardCharsets.UTF_8);

        // Gửi về đúng địa chỉ + cổng lấy từ gói vừa nhận.
        DatagramPacket response = new DatagramPacket(data, data.length,
                request.getAddress(), request.getPort());
        socket.send(response);
        System.out.println("Nhan \"" + message + "\" tu " + sender + "  ->  tra \"" + answer + "\"");
    }

    /** In các IPv4 của máy này để máy thứ hai biết gửi Ping tới địa chỉ nào. */
    private static void printLocalAddresses() {
        System.out.println("Dia chi IP cua may nay (nhap o Client tren may khac):");
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback()) {
                    continue;
                }
                Collections.list(nic.getInetAddresses()).stream()
                        .filter(Inet4Address.class::isInstance)
                        .forEach(ip -> System.out.println("   * " + ip.getHostAddress()
                                + "  (" + nic.getDisplayName() + ")"));
            }
        } catch (SocketException e) {
            System.out.println("   (khong doc duoc danh sach card mang: " + e.getMessage() + ")");
        }
    }

    static void printNetstatHint(int port) {
        System.out.println("Kiem tra cong bang netstat:");
        System.out.println("   Windows    : netstat -an -p UDP | findstr " + port);
        System.out.println("   Linux/macOS: netstat -anu | grep " + port + "   (hoac: ss -uan | grep " + port + ")");
    }

    /** Chạy độc lập: java ...UDPServer [port] */
    public static void main(String[] args) {
        start(args.length > 0 ? Integer.parseInt(args[0]) : PORT);
    }
}
