package com.project_final.networkprogramming_project.detai3_threadpoolserver;

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
 * Đề tài 3: Client tương tác — gõ chuỗi, nhận lại chuỗi đảo ngược từ server.
 * ==========================================================================
 *
 * Cách demo "ít nhất 3 client song song" bằng tay:
 *
 *   Terminal 1:  chạy Server            (menu 3 → a)
 *   Terminal 2:  chạy Client, gõ "abc"  (menu 3 → b)  → nhận "cba"
 *   Terminal 3:  chạy Client, gõ "xyz"  (menu 3 → b)  → nhận "zyx"
 *   Terminal 4:  chạy Client, gõ "123"  (menu 3 → b)  → nhận "321"
 *
 * Cả 3 client giữ kết nối cùng lúc; log server sẽ cho thấy mỗi client do
 * một worker khác nhau phục vụ (worker-1, worker-2, worker-3).
 *
 * @author Cao Duy Quốc Khánh
 */
public final class ReverseClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final String QUIT_COMMAND = "quit";

    /** Chờ server trả lời tối đa bấy nhiêu ms; tránh treo vĩnh viễn nếu server bị kẹt. */
    private static final int READ_TIMEOUT_MS = 10_000;

    private ReverseClient() {
    }

    /**
     * Điểm vào từ menu chính. Dùng chung Scanner của menu: nếu tạo Scanner mới
     * trên System.in, hai Scanner sẽ tranh nhau buffer đầu vào và "nuốt" mất dòng.
     */
    public static void run(Scanner scanner) {
        System.out.println("=== DE TAI 3: CLIENT (CHUOI DAO NGUOC) ===");
        System.out.print("Nhap dia chi server (Enter = " + DEFAULT_HOST + "): ");
        String host = scanner.nextLine().trim();
        chat(host.isEmpty() ? DEFAULT_HOST : host, ReverseServer.DEFAULT_PORT, scanner);
    }

    private static void chat(String host, int port, Scanner scanner) {
        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            socket.setSoTimeout(READ_TIMEOUT_MS);
            System.out.println("[OK] Da ket noi " + socket.getRemoteSocketAddress()
                    + " tu cong cuc bo " + socket.getLocalPort());
            System.out.println("Go chuoi roi Enter. Go '" + QUIT_COMMAND + "' de thoat.\n");

            while (true) {
                System.out.print("Ban gui  > ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String line = scanner.nextLine();
                if (line.equalsIgnoreCase(QUIT_COMMAND)) {
                    break;
                }
                out.println(line);
                String reply = in.readLine();
                if (reply == null) {
                    System.out.println("[!] Server da dong ket noi.");
                    break;
                }
                System.out.println("Server tra < " + reply);
            }
            System.out.println("Da ngat ket noi.");
        } catch (UnknownHostException e) {
            System.out.println("[X] Khong tim thay may chu '" + host + "'.");
        } catch (ConnectException e) {
            System.out.println("[X] Khong ket noi duoc " + host + ":" + port
                    + ". Server da chay chua? (menu 3 -> a)");
        } catch (SocketTimeoutException e) {
            System.out.println("[X] Qua " + READ_TIMEOUT_MS / 1000 + " giay server khong tra loi."
                    + " Co the pool da day va client nay dang phai xep hang.");
        } catch (IOException e) {
            System.out.println("[X] Loi ket noi: " + e.getMessage());
        }
    }

    /** Chạy độc lập: java ...ReverseClient [host] [port] */
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : ReverseServer.DEFAULT_PORT;
        chat(host, port, new Scanner(System.in, StandardCharsets.UTF_8));
    }
}
