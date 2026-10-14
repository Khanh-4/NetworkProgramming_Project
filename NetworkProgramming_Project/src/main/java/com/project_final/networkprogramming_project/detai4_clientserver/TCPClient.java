package com.project_final.networkprogramming_project.detai4_clientserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Đề tài 4: Kiến trúc Client/Server và vai trò của Socket
 * ----------------------------------------------------------
 * TCP Client: gửi chuỗi đến Server, nhận phản hồi "Received OK".
 *
 * Phía client chỉ cần MỘT Socket. Lệnh new Socket(host, port) làm hai việc:
 *   1. OS cấp cho client một cổng tạm (ephemeral port, ví dụ 54021).
 *   2. Kernel thực hiện bắt tay 3 bước với server; khi constructor trả về
 *      thì kết nối đã ESTABLISHED, có thể đọc/ghi ngay.
 *
 * Từ Socket lấy ra hai luồng (stream) — đúng kiến thức đề tài 1, 2:
 *   socket.getOutputStream()  → ghi dữ liệu gửi đi
 *   socket.getInputStream()   → đọc dữ liệu server trả về
 * TCP là luồng byte liên tục, không có ranh giới gói tin, nên dùng ký tự
 * xuống dòng '\n' làm dấu kết thúc mỗi thông điệp (println / readLine).
 *
 * @author Trần Gia Quân
 */
public class TCPClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final String QUIT_COMMAND = "quit";

    /** Chờ server trả lời tối đa bấy nhiêu ms, tránh treo vĩnh viễn. */
    private static final int READ_TIMEOUT_MS = 10_000;

    /**
     * Điểm vào từ menu chính. Dùng chung Scanner của menu: tạo Scanner mới trên
     * System.in sẽ tranh buffer đầu vào với Scanner cũ và "nuốt" mất dòng nhập.
     */
    public static void run(Scanner scanner) {
        System.out.println("=== ĐỀ TÀI 4: TCP CLIENT ===");
        System.out.print("Nhập địa chỉ server (Enter = " + DEFAULT_HOST + "): ");
        String host = scanner.nextLine().trim();
        chat(host.isEmpty() ? DEFAULT_HOST : host, TCPServer.PORT, scanner);
    }

    private static void chat(String host, int port, Scanner scanner) {
        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            socket.setSoTimeout(READ_TIMEOUT_MS);
            System.out.println("✅ Đã kết nối server " + socket.getInetAddress().getHostAddress()
                    + ":" + socket.getPort() + " từ cổng cục bộ " + socket.getLocalPort());
            System.out.println("Gõ chuỗi rồi Enter để gửi. Gõ '" + QUIT_COMMAND + "' để thoát.\n");

            while (true) {
                System.out.print("Gửi  > ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase(QUIT_COMMAND)) {
                    break;
                }
                out.println(message);
                String reply = in.readLine();
                if (reply == null) {
                    System.out.println("⚠️  Server đã đóng kết nối.");
                    break;
                }
                System.out.println("Nhận < " + reply);
            }
            System.out.println("👋 Đã ngắt kết nối.");
        } catch (UnknownHostException e) {
            System.out.println("❌ Không tìm thấy máy chủ '" + host + "'.");
        } catch (ConnectException e) {
            System.out.println("❌ Không kết nối được " + host + ":" + port
                    + ". Server đã chạy chưa? (menu 4 → a)");
        } catch (SocketTimeoutException e) {
            System.out.println("❌ Quá " + READ_TIMEOUT_MS / 1000 + " giây server không phản hồi."
                    + " Có thể server đang bận phục vụ client khác.");
        } catch (IOException e) {
            System.out.println("❌ Lỗi kết nối: " + e.getMessage());
        }
    }

    /** Chạy độc lập: java ...TCPClient [host] [port] */
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : TCPServer.PORT;
        chat(host, port, new Scanner(System.in, StandardCharsets.UTF_8));
    }
}
