package com.project_final.networkprogramming_project.detai3_threadpoolserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Đề tài 3: Demo TỰ ĐỘNG — nhiều client cùng lúc, tự kiểm tra từng câu trả lời.
 * =============================================================================
 *
 * Đáp ứng phần "Kết quả" của đề bài: chạy ít nhất 3 client song song, server
 * vẫn phản hồi đúng từng client.
 *
 * KỊCH BẢN:
 *
 *   1. Bật ReverseServer ngay trong JVM này (cổng 0 = OS tự chọn cổng trống,
 *      nên không đụng server đang chạy ở cổng 5000 nếu có).
 *   2. Tạo CLIENT_COUNT client, mỗi client là một thread riêng.
 *   3. Tất cả chờ ở "vạch xuất phát" (CountDownLatch) rồi cùng kết nối một lúc.
 *   4. Mỗi client gửi các chuỗi MANG TÊN RIÊNG của nó ("Client-2 gửi câu 3"),
 *      nghỉ giữa các câu để các kết nối chắc chắn chồng lên nhau theo thời gian.
 *   5. So sánh từng câu trả lời với chuỗi đảo ngược mong đợi.
 *
 * VÌ SAO CHUỖI PHẢI MANG TÊN CLIENT?
 *   Nếu server lẫn lộn dữ liệu giữa các kết nối (ví dụ dùng chung một biến
 *   buffer giữa các thread), client 1 có thể nhận câu trả lời của client 2.
 *   Chuỗi khác nhau cho từng client giúp phát hiện ngay lỗi đó.
 *
 * BẰNG CHỨNG "SONG SONG":
 *   Server đếm số client được phục vụ đồng thời (peak). Peak = CLIENT_COUNT
 *   nghĩa là mọi client đều được phục vụ cùng một lúc, không ai phải chờ ai.
 *
 * @author Cao Duy Quốc Khánh
 */
public final class ConcurrentClientsDemo {

    /** Đề bài yêu cầu tối thiểu 3 client song song. */
    private static final int REQUIRED_CONCURRENT_CLIENTS = 3;
    private static final int CLIENT_COUNT = 5;
    private static final int MESSAGES_PER_CLIENT = 3;
    private static final long PAUSE_BETWEEN_MESSAGES_MS = 200;
    private static final int READ_TIMEOUT_MS = 5_000;
    private static final long DEMO_TIMEOUT_SECONDS = 30;

    private ConcurrentClientsDemo() {
    }

    /** Kết quả của một client: số câu trả lời đúng trên tổng số câu đã gửi. */
    private record ClientReport(String name, int correct, int sent, String error) {
        boolean passed() {
            return error == null && correct == sent;
        }
    }

    /** Điểm vào từ menu chính. */
    public static void run() {
        printConclusion(runDemo());
    }

    /** Chạy độc lập; mã thoát 0 = đạt, 1 = không đạt (dùng được để kiểm tra tự động). */
    public static void main(String[] args) {
        boolean passed = runDemo();
        printConclusion(passed);
        System.exit(passed ? 0 : 1);
    }

    private static boolean runDemo() {
        System.out.println("=== DE TAI 3: DEMO " + CLIENT_COUNT + " CLIENT CHAY SONG SONG ===\n");
        ReverseServer server;
        List<ClientReport> reports;
        try (ReverseServer running = new ReverseServer(0, ReverseServer.POOL_SIZE)) {
            server = running;
            new Thread(running::serve, "server-accept").start();
            reports = runClients(running.port());
        } catch (IOException e) {
            System.out.println("[X] Khong khoi dong duoc server: " + e.getMessage());
            return false;
        }
        // In bảng SAU khi try-with-resources đã close() server: close() chờ các worker
        // ghi xong log "ngắt kết nối", nên bảng tổng kết không bị log chen ngang.
        printSummary(reports, server);
        boolean allCorrect = reports.stream().allMatch(ClientReport::passed);
        return allCorrect && server.peakConcurrentClients() >= REQUIRED_CONCURRENT_CLIENTS;
    }

    private static List<ClientReport> runClients(int port) {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService clients = Executors.newFixedThreadPool(CLIENT_COUNT);
        List<String> names = new ArrayList<>();
        List<Future<ClientReport>> futures = new ArrayList<>();

        for (int i = 1; i <= CLIENT_COUNT; i++) {
            String name = "Client-" + i;
            names.add(name);
            futures.add(clients.submit(() -> talk(name, port, startGate)));
        }
        startGate.countDown(); // Mở vạch xuất phát: tất cả client cùng kết nối

        List<ClientReport> reports = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            reports.add(awaitReport(names.get(i), futures.get(i)));
        }
        clients.shutdownNow();
        return reports;
    }

    private static ClientReport awaitReport(String name, Future<ClientReport> future) {
        try {
            return future.get(DEMO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return new ClientReport(name, 0, MESSAGES_PER_CLIENT,
                    "qua " + DEMO_TIMEOUT_SECONDS + " giay chua xong");
        } catch (ExecutionException e) {
            return new ClientReport(name, 0, MESSAGES_PER_CLIENT, String.valueOf(e.getCause()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ClientReport(name, 0, MESSAGES_PER_CLIENT, "bi ngat");
        }
    }

    /** Công việc của MỘT client: kết nối, gửi lần lượt các câu, kiểm tra từng câu trả lời. */
    private static ClientReport talk(String name, int port, CountDownLatch startGate) {
        Thread.currentThread().setName(name);
        int correct = 0;
        try {
            startGate.await();
            try (Socket socket = new Socket("localhost", port);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                socket.setSoTimeout(READ_TIMEOUT_MS);
                for (int msg = 1; msg <= MESSAGES_PER_CLIENT; msg++) {
                    String sent = name + " gui cau " + msg;
                    String expected = ClientHandler.reverse(sent);
                    out.println(sent);
                    String reply = in.readLine();
                    boolean ok = expected.equals(reply);
                    if (ok) {
                        correct++;
                    }
                    ConsoleLog.print((ok ? "[OK] " : "[X] ") + "\"" + sent + "\" -> nhan \"" + reply + "\"");
                    Thread.sleep(PAUSE_BETWEEN_MESSAGES_MS);
                }
            }
            return new ClientReport(name, correct, MESSAGES_PER_CLIENT, null);
        } catch (IOException e) {
            return new ClientReport(name, correct, MESSAGES_PER_CLIENT, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ClientReport(name, correct, MESSAGES_PER_CLIENT, "bi ngat");
        }
    }

    private static void printSummary(List<ClientReport> reports, ReverseServer server) {
        System.out.println();
        System.out.println("+------------+--------------+----------+");
        System.out.println("| Client     | Dung / Gui   | Ket qua  |");
        System.out.println("+------------+--------------+----------+");
        for (ClientReport r : reports) {
            System.out.printf("| %-10s | %5d / %-4d | %-8s |%n",
                    r.name(), r.correct(), r.sent(), r.passed() ? "DAT" : "LOI");
        }
        System.out.println("+------------+--------------+----------+");
        reports.stream()
                .filter(r -> r.error() != null)
                .forEach(r -> System.out.println("  " + r.name() + " loi: " + r.error()));
        System.out.println("Tong so client server da phuc vu : " + server.totalClients());
        System.out.println("So client duoc phuc vu DONG THOI : " + server.peakConcurrentClients()
                + " (ThreadPool " + ReverseServer.POOL_SIZE + " worker)");
    }

    private static void printConclusion(boolean passed) {
        System.out.println(passed
                ? "\n[OK] KET LUAN: server phuc vu dong thoi nhieu client va tra loi dung cho tung client."
                : "\n[X] KET LUAN: co client nhan sai hoac khong nhan duoc phan hoi - xem log phia tren.");
    }
}
