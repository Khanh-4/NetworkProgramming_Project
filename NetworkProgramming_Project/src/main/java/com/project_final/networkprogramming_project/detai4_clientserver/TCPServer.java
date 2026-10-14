package com.project_final.networkprogramming_project.detai4_clientserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Đề tài 4: Kiến trúc Client/Server và vai trò của Socket
 * ----------------------------------------------------------
 * TCP Server: nhận chuỗi từ Client, in ra console, phản hồi "Received OK".
 *
 * HAI LOẠI SOCKET Ở PHÍA SERVER — điểm hay bị nhầm nhất:
 *
 *   ServerSocket (cổng 12345)        → chỉ làm "lễ tân": lắng nghe và accept().
 *                                      Không bao giờ truyền dữ liệu.
 *   Socket (trả về từ accept())      → "đường dây riêng" với ĐÚNG MỘT client,
 *                                      dùng để đọc/ghi dữ liệu.
 *
 * VÒNG ĐỜI MỘT KẾT NỐI:
 *
 *   Server                                    Client
 *   new ServerSocket(12345)  [LISTEN]
 *   accept()  ── chờ ──┐
 *                      │ ◀── SYN ──────────── new Socket(host, 12345)
 *                      │ ─── SYN-ACK ───────▶       (bắt tay 3 bước do
 *                      │ ◀── ACK ────────────        kernel thực hiện)
 *   accept() trả về Socket  [ESTABLISHED]     [ESTABLISHED]
 *   readLine()  ◀────────── "Xin chào\n" ──── println("Xin chào")
 *   in ra console
 *   println("Received OK") ────────────────▶  readLine() → "Received OK"
 *          ...                                      ...
 *   readLine() == null  ◀─────── FIN ──────── close()
 *   close()  → quay lại accept() chờ client tiếp theo
 *
 * Một kết nối TCP được định danh bởi bộ 4 (IP server, cổng server, IP client,
 * cổng client). Cổng client do OS cấp ngẫu nhiên, nên log in ra cả hai đầu
 * để thấy rõ bộ 4 này.
 *
 * Server này phục vụ TUẦN TỰ (iterative): trong lúc đang nói chuyện với một
 * client, client thứ hai kết nối được (kernel xếp vào hàng chờ backlog) nhưng
 * phải đợi client thứ nhất thoát. Muốn phục vụ đồng thời thì xem đề tài 3
 * (server dùng ThreadPool).
 *
 * @author Trần Gia Quân
 */
public class TCPServer {

    public static final int PORT = 12345;
    public static final String REPLY = "Received OK";

    public static void run() {
        start(PORT);
    }

    private static void start(int port) {
        System.out.println("=== ĐỀ TÀI 4: TCP SERVER ===");
        // try-with-resources: ServerSocket được đóng khi thoát, giải phóng cổng 12345.
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server lắng nghe trên cổng " + serverSocket.getLocalPort()
                    + ". Mở cửa sổ console khác và chạy Client (menu 4 → b).");
            System.out.println("Nhấn Ctrl+C để dừng server.\n");
            while (true) {
                System.out.println("⏳ Đang chờ client kết nối (accept)...");
                // accept() BLOCK cho đến khi có client hoàn tất bắt tay 3 bước.
                Socket client = serverSocket.accept();
                serve(client);
            }
        } catch (BindException e) {
            System.out.println("❌ Cổng " + port + " đang bị chương trình khác chiếm."
                    + " Hãy tắt server cũ rồi chạy lại.");
        } catch (IOException e) {
            System.out.println("❌ Lỗi server: " + e.getMessage());
        }
    }

    /** Nói chuyện với một client cho đến khi client đóng kết nối. */
    private static void serve(Socket client) {
        String who = client.getInetAddress().getHostAddress() + ":" + client.getPort();
        System.out.println("✅ Client kết nối: " + who
                + "  →  server " + client.getLocalAddress().getHostAddress() + ":" + client.getLocalPort());

        // Hai đầu phải dùng CÙNG charset (UTF-8), nếu không tiếng Việt sẽ thành ký tự rác.
        try (client;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             // autoFlush = true: mỗi println() đẩy ngay xuống socket. Quên flush thì
             // "Received OK" kẹt trong buffer, client chờ mãi → cả hai cùng treo.
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String message;
            // readLine() trả về null khi client đóng kết nối (nhận được FIN).
            while ((message = in.readLine()) != null) {
                System.out.println("📩 [" + who + "] " + message);
                out.println(REPLY);
            }
            System.out.println("👋 Client " + who + " đã ngắt kết nối.\n");
        } catch (IOException e) {
            // Client tắt ngang cửa sổ → "Connection reset". Server không chết,
            // chỉ bỏ client này và quay lại chờ client khác.
            System.out.println("⚠️  Mất kết nối với " + who + ": " + e.getMessage() + "\n");
        }
    }

    /** Chạy độc lập: java ...TCPServer [port] */
    public static void main(String[] args) {
        start(args.length > 0 ? Integer.parseInt(args[0]) : PORT);
    }
}
