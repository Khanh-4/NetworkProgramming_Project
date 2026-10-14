package com.project_final.networkprogramming_project.detai5_udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Đề tài 5: Nguyên lý hoạt động của UDP
 * ----------------------------------------
 * UDP Client: gửi gói "Ping", nhận phản hồi "Pong".
 *
 * Mô phỏng lệnh ping: gửi PING_COUNT gói, đo thời gian khứ hồi (RTT) của từng
 * gói và thống kê số gói bị mất.
 *
 * VÌ SAO PHẢI CÓ TIMEOUT?
 *   UDP không đảm bảo gói tới nơi. Nếu gói "Ping" hoặc "Pong" bị mất (hoặc server
 *   chưa chạy), receive() sẽ chờ VĨNH VIỄN. setSoTimeout() biến việc chờ vô hạn
 *   thành SocketTimeoutException sau một khoảng thời gian → coi như gói bị mất.
 *   Với TCP, việc phát hiện mất gói và gửi lại do kernel lo; với UDP thì ứng
 *   dụng phải tự làm.
 *
 * Client KHÔNG chỉ định cổng: new DatagramSocket() để OS cấp một cổng tạm.
 * Server sẽ đọc cổng này từ gói Ping để biết gửi Pong về đâu.
 *
 * @author Nguyễn Đức Hiếu
 */
public class UDPClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int PING_COUNT = 4;
    private static final int TIMEOUT_MS = 2_000;
    private static final long PAUSE_BETWEEN_PINGS_MS = 1_000;
    private static final int BUFFER_SIZE = 1024;

    /** Điểm vào từ menu chính; dùng chung Scanner của menu để không tranh System.in. */
    public static void run(Scanner scanner) {
        System.out.println("=== ĐỀ TÀI 5: UDP CLIENT (PING) ===");
        System.out.print("Nhập IP máy chạy Server (Enter = " + DEFAULT_HOST + "): ");
        String host = scanner.nextLine().trim();
        ping(host.isEmpty() ? DEFAULT_HOST : host, UDPServer.PORT);
    }

    private static void ping(String host, int port) {
        InetAddress server;
        try {
            server = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.out.println("❌ Không tìm thấy máy '" + host + "'.");
            return;
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            System.out.println("Gửi " + PING_COUNT + " gói \"" + UDPServer.PING + "\" tới "
                    + server.getHostAddress() + ":" + port
                    + " từ cổng cục bộ " + socket.getLocalPort() + "\n");

            int received = 0;
            for (int seq = 1; seq <= PING_COUNT; seq++) {
                if (sendOnePing(socket, server, port, seq)) {
                    received++;
                }
                if (seq < PING_COUNT) {
                    Thread.sleep(PAUSE_BETWEEN_PINGS_MS);
                }
            }
            printStatistics(received);
            UDPServer.printNetstatHint(port);
        } catch (IOException e) {
            System.out.println("❌ Lỗi socket UDP: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("⚠️  Bị ngắt giữa chừng.");
        }
    }

    /** Gửi một gói Ping, chờ Pong. Trả về true nếu nhận được phản hồi đúng. */
    private static boolean sendOnePing(DatagramSocket socket, InetAddress server, int port, int seq)
            throws IOException {
        byte[] data = UDPServer.PING.getBytes(StandardCharsets.UTF_8);
        DatagramPacket request = new DatagramPacket(data, data.length, server, port);

        long start = System.nanoTime();
        socket.send(request);
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            double rttMs = (System.nanoTime() - start) / 1_000_000.0;

            String reply = new String(response.getData(), response.getOffset(), response.getLength(),
                    StandardCharsets.UTF_8);
            boolean correct = UDPServer.PONG.equals(reply);
            System.out.printf("%s Gói #%d: nhận \"%s\" từ %s:%d — RTT = %.2f ms%n",
                    correct ? "✅" : "⚠️ ", seq, reply,
                    response.getAddress().getHostAddress(), response.getPort(), rttMs);
            return correct;
        } catch (SocketTimeoutException e) {
            System.out.println("❌ Gói #" + seq + ": quá " + TIMEOUT_MS + " ms không có phản hồi (mất gói"
                    + " hoặc server chưa chạy)");
            return false;
        } catch (PortUnreachableException e) {
            // Một số hệ điều hành báo ngay khi cổng đích không có ai lắng nghe (ICMP Port Unreachable).
            System.out.println("❌ Gói #" + seq + ": cổng " + port + " trên server không có chương trình nào nghe");
            return false;
        }
    }

    private static void printStatistics(int received) {
        int lost = PING_COUNT - received;
        System.out.println();
        System.out.println("--- Thống kê ---");
        System.out.printf("Đã gửi = %d, Nhận được = %d, Mất = %d (%.0f%% mất gói)%n",
                PING_COUNT, received, lost, 100.0 * lost / PING_COUNT);
    }

    /** Chạy độc lập: java ...UDPClient [host] [port] */
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : UDPServer.PORT;
        ping(host, port);
    }
}
