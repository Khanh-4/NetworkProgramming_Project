package com.project_final.networkprogramming_project.detai3_threadpoolserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Đề tài 3: Công việc (task) phục vụ MỘT client — chạy trên một worker của ThreadPool.
 * ====================================================================================
 *
 * Giao thức (protocol) dạng dòng văn bản, mã hoá UTF-8:
 *
 *   Client                                Server (worker-N)
 *     │ ── "Xin chào\n" ───────────────────▶ │  đọc 1 dòng
 *     │ ◀─────────────────── "oàhc niX\n" ── │  đảo ngược, gửi lại
 *     │ ── "abc\n" ────────────────────────▶ │
 *     │ ◀───────────────────────── "cba\n" ── │
 *     │ ── [đóng socket / EOF] ────────────▶ │  readLine() == null → kết thúc task,
 *                                               worker quay về pool chờ client khác
 *
 * VÌ SAO PHẢI CHỈ ĐỊNH UTF-8?
 *   new InputStreamReader(in) không truyền charset sẽ dùng charset mặc định của máy.
 *   Client chạy trên Windows (Cp1252) gửi cho server chạy Linux (UTF-8) thì chuỗi
 *   tiếng Việt sẽ thành ký tự rác. Hai đầu phải thống nhất cùng một charset.
 *
 * VÌ SAO PrintWriter(..., true)?
 *   autoFlush = true → mỗi println() tự đẩy dữ liệu xuống socket. Nếu quên flush,
 *   chuỗi trả lời nằm kẹt trong buffer của PrintWriter, client chờ readLine() mãi
 *   không thấy gì → hai bên cùng treo (lỗi kinh điển của sinh viên).
 *
 * @author Cao Duy Quốc Khánh
 */
final class ClientHandler implements Runnable {

    private final Socket socket;
    private final ConnectionCounter counter;

    ClientHandler(Socket socket, ConnectionCounter counter) {
        this.socket = socket;
        this.counter = counter;
    }

    /**
     * Đảo ngược chuỗi theo KÝ TỰ, không theo byte.
     * StringBuilder.reverse() giữ nguyên các cặp surrogate (emoji như 😀 chiếm
     * 2 char) nên không làm vỡ ký tự. Tiếng Việt dạng dựng sẵn (NFC) mỗi chữ
     * là một code point nên đảo ngược cũng đúng.
     */
    static String reverse(String text) {
        return new StringBuilder(text).reverse().toString();
    }

    @Override
    public void run() {
        String client = String.valueOf(socket.getRemoteSocketAddress());
        int activeNow = counter.enter();
        ConsoleLog.print("Bắt đầu phục vụ client " + client + " (đang phục vụ: " + activeNow + ")");

        // try-with-resources: đóng reader/writer cũng đóng luôn socket bên dưới,
        // kể cả khi có exception giữa chừng → không rò rỉ file descriptor.
        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String line;
            // readLine() BLOCK worker này cho đến khi client gửi đủ 1 dòng.
            // Chỉ worker này bị chặn; các worker khác trong pool vẫn chạy bình thường.
            while ((line = in.readLine()) != null) {
                String reversed = reverse(line);
                ConsoleLog.print("Client " + client + " gửi \"" + line + "\" → trả \"" + reversed + "\"");
                out.println(reversed);
            }
            ConsoleLog.print("Client " + client + " đã ngắt kết nối");
        } catch (IOException e) {
            // Client tắt ngang (đóng cửa sổ, mất mạng) → "Connection reset".
            // Chỉ ảnh hưởng client này, server và các client khác vẫn chạy tiếp.
            ConsoleLog.print("Lỗi với client " + client + ": " + e.getMessage());
        } finally {
            int remaining = counter.exit();
            ConsoleLog.print("Worker rảnh, trả về pool (còn phục vụ: " + remaining + ")");
        }
    }
}
