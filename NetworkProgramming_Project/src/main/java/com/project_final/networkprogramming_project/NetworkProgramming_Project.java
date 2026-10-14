package com.project_final.networkprogramming_project;

import java.util.Scanner;

// Import các đề tài
import com.project_final.networkprogramming_project.detai3_threadpoolserver.ConcurrentClientsDemo;
import com.project_final.networkprogramming_project.detai3_threadpoolserver.ReverseClient;
import com.project_final.networkprogramming_project.detai3_threadpoolserver.ReverseServer;
import com.project_final.networkprogramming_project.detai4_clientserver.TCPServer;
import com.project_final.networkprogramming_project.detai4_clientserver.TCPClient;
import com.project_final.networkprogramming_project.detai5_udp.UDPServer;
import com.project_final.networkprogramming_project.detai5_udp.UDPClient;
import com.project_final.networkprogramming_project.detai6_rmi.CalculatorServer;
import com.project_final.networkprogramming_project.detai6_rmi.CalculatorClient;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║   PROJECT CUỐI KỲ - LẬP TRÌNH MẠNG MÁY TÍNH               ║
 * ║   Nhóm: Khánh - Quân - Hiếu                                ║
 * ╚══════════════════════════════════════════════════════════════╝
 * 
 * Main Menu điều hướng chạy demo từng đề tài.
 * Chọn số đề tài tương ứng để chạy demo.
 * 
 * @author Cao Duy Quốc Khánh
 * @author Trần Gia Quân
 * @author Nguyễn Đức Hiếu
 */
public class NetworkProgramming_Project {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            System.out.print("👉 Chọn đề tài (0 để thoát): ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("⚠️  Vui lòng nhập số từ 0-6!\n");
                continue;
            }

            System.out.println(); // Dòng trống trước output

            switch (choice) {
                case 0:
                    running = false;
                    System.out.println("👋 Tạm biệt! Kết thúc chương trình.");
                    break;
                case 1:
                    com.project_final.networkprogramming_project.detai1_bytecharstream.StreamComparisonGUI.run();
                    break;
                case 2:
                    com.project_final.networkprogramming_project.detai2_bufferedstream.PerformanceChartGUI.run();
                    break;
                case 3:
                    runDetai3Menu(scanner);
                    break;
                case 4:
                    runDetai4Menu(scanner);
                    break;
                case 5:
                    runDetai5Menu(scanner);
                    break;
                case 6:
                    runDetai6Menu(scanner);
                    break;
                default:
                    System.out.println("⚠️  Không có đề tài số " + choice + "! Vui lòng chọn 0-6.");
            }

            if (running) {
                System.out.println("\n" + "=".repeat(60));
                System.out.println("Nhấn Enter để quay lại menu...");
                scanner.nextLine();
            }
        }

        scanner.close();
    }

    /**
     * In menu chính ra console.
     */
    private static void printMenu() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║         PROJECT CUỐI KỲ - LẬP TRÌNH MẠNG MÁY TÍNH         ║");
        System.out.println("║              Nhóm: Khánh - Quân - Hiếu                     ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║                                                             ║");
        System.out.println("║  [1] Đề tài 1: Luồng Byte vs Ký tự       (Khánh)           ║");
        System.out.println("║  [2] Đề tài 2: Buffered Stream            (Khánh)           ║");
        System.out.println("║  [3] Đề tài 3: Server đa luồng TCP       (Quân)            ║");
        System.out.println("║  [4] Đề tài 4: TCP Client-Server          (Quân)            ║");
        System.out.println("║  [5] Đề tài 5: UDP Ping-Pong              (Hiếu)            ║");
        System.out.println("║  [6] Đề tài 6: Java RMI                   (Hiếu)            ║");
        System.out.println("║                                                             ║");
        System.out.println("║  [0] Thoát                                                  ║");
        System.out.println("║                                                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    /**
     * Sub-menu cho Đề tài 3: Server TCP đa luồng dùng ThreadPool (trả chuỗi đảo ngược).
     * Chọn chạy Server, Client tương tác, hoặc demo tự động nhiều client song song.
     */
    private static void runDetai3Menu(Scanner scanner) {
        System.out.println("--- ĐỀ TÀI 3: SERVER ĐA LUỒNG (THREADPOOL) ---");
        System.out.println("  [a] Chạy Server (chạy trước)");
        System.out.println("  [b] Chạy Client (mở ≥3 terminal khác, mỗi terminal 1 client)");
        System.out.println("  [c] Demo tự động: 5 client song song + tự kiểm tra kết quả");
        System.out.print("👉 Chọn: ");
        String sub = scanner.nextLine().trim().toLowerCase();

        switch (sub) {
            case "a":
                ReverseServer.run();
                break;
            case "b":
                ReverseClient.run(scanner);
                break;
            case "c":
                ConcurrentClientsDemo.run();
                break;
            default:
                System.out.println("⚠️  Lựa chọn không hợp lệ!");
        }
    }

    /**
     * Sub-menu cho Đề tài 4: TCP Client-Server.
     * Chọn chạy Server hoặc Client.
     */
    private static void runDetai4Menu(Scanner scanner) {
        System.out.println("--- ĐỀ TÀI 4: TCP CLIENT-SERVER ---");
        System.out.println("  [a] Chạy Server (chạy trước)");
        System.out.println("  [b] Chạy Client (chạy sau, ở terminal khác)");
        System.out.print("👉 Chọn: ");
        String sub = scanner.nextLine().trim().toLowerCase();

        switch (sub) {
            case "a":
                TCPServer.run();
                break;
            case "b":
                TCPClient.run(scanner);
                break;
            default:
                System.out.println("⚠️  Lựa chọn không hợp lệ!");
        }
    }

    /**
     * Sub-menu cho Đề tài 5: UDP Ping-Pong.
     * Chọn chạy Server hoặc Client.
     */
    private static void runDetai5Menu(Scanner scanner) {
        System.out.println("--- ĐỀ TÀI 5: UDP PING-PONG ---");
        System.out.println("  [a] Chạy UDP Server (Pong) - chạy trước");
        System.out.println("  [b] Chạy UDP Client (Ping) - chạy sau");
        System.out.print("👉 Chọn: ");
        String sub = scanner.nextLine().trim().toLowerCase();

        switch (sub) {
            case "a":
                UDPServer.run();
                break;
            case "b":
                UDPClient.run();
                break;
            default:
                System.out.println("⚠️  Lựa chọn không hợp lệ!");
        }
    }

    /**
     * Sub-menu cho Đề tài 6: Java RMI.
     * Chọn chạy Server hoặc Client.
     */
    private static void runDetai6Menu(Scanner scanner) {
        System.out.println("--- ĐỀ TÀI 6: JAVA RMI ---");
        System.out.println("  [a] Chạy RMI Server (khởi tạo Registry) - chạy trước");
        System.out.println("  [b] Chạy RMI Client (gọi hàm từ xa) - chạy sau");
        System.out.print("👉 Chọn: ");
        String sub = scanner.nextLine().trim().toLowerCase();

        switch (sub) {
            case "a":
                CalculatorServer.run();
                break;
            case "b":
                CalculatorClient.run();
                break;
            default:
                System.out.println("⚠️  Lựa chọn không hợp lệ!");
        }
    }
}
