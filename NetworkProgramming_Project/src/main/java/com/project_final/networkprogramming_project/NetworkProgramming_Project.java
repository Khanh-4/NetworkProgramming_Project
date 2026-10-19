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
            System.out.print("-> Chon de tai (0 de thoat): ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("[!] Vui long nhap so tu 0-6!\n");
                continue;
            }

            System.out.println(); // Dòng trống trước output

            switch (choice) {
                case 0:
                    running = false;
                    System.out.println("Tam biet! Ket thuc chuong trinh.");
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
                    System.out.println("[!] Khong co de tai so " + choice + "! Vui long chon 0-6.");
            }

            if (running) {
                System.out.println("\n" + "=".repeat(60));
                System.out.println("Nhan Enter de quay lai menu...");
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
        System.out.println("+==============================================================+");
        System.out.println("|          PROJECT CUOI KY - LAP TRINH MANG MAY TINH           |");
        System.out.println("|                  Nhom: Khanh - Quan - Hieu                   |");
        System.out.println("+==============================================================+");
        System.out.println("|                                                              |");
        System.out.println("|  [1] De tai 1: Luong Byte vs Ky tu        (Khanh)            |");
        System.out.println("|  [2] De tai 2: Buffered Stream            (Khanh)            |");
        System.out.println("|  [3] De tai 3: Server da luong TCP        (Quan)             |");
        System.out.println("|  [4] De tai 4: TCP Client-Server          (Quan)             |");
        System.out.println("|  [5] De tai 5: UDP Ping-Pong              (Hieu)             |");
        System.out.println("|  [6] De tai 6: Java RMI                   (Hieu)             |");
        System.out.println("|                                                              |");
        System.out.println("|  [0] Thoat                                                   |");
        System.out.println("|                                                              |");
        System.out.println("+==============================================================+");
    }

    /**
     * Sub-menu cho Đề tài 3: Server TCP đa luồng dùng ThreadPool (trả chuỗi đảo ngược).
     * Chọn chạy Server, Client tương tác, hoặc demo tự động nhiều client song song.
     */
    private static void runDetai3Menu(Scanner scanner) {
        System.out.println("--- DE TAI 3: SERVER DA LUONG (THREADPOOL) ---");
        System.out.println("  [a] Chay Server (chay truoc)");
        System.out.println("  [b] Chay Client (mo >=3 terminal khac, moi terminal 1 client)");
        System.out.println("  [c] Demo tu dong: 5 client song song + tu kiem tra ket qua");
        System.out.print("-> Chon: ");
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
                System.out.println("[!] Lua chon khong hop le!");
        }
    }

    /**
     * Sub-menu cho Đề tài 4: TCP Client-Server.
     * Chọn chạy Server hoặc Client.
     */
    private static void runDetai4Menu(Scanner scanner) {
        System.out.println("--- DE TAI 4: TCP CLIENT-SERVER ---");
        System.out.println("  [a] Chay Server (chay truoc)");
        System.out.println("  [b] Chay Client (chay sau, o terminal khac)");
        System.out.print("-> Chon: ");
        String sub = scanner.nextLine().trim().toLowerCase();

        switch (sub) {
            case "a":
                TCPServer.run();
                break;
            case "b":
                TCPClient.run(scanner);
                break;
            default:
                System.out.println("[!] Lua chon khong hop le!");
        }
    }

    /**
     * Sub-menu cho Đề tài 5: UDP Ping-Pong.
     * Chọn chạy Server hoặc Client.
     */
    private static void runDetai5Menu(Scanner scanner) {
        System.out.println("--- DE TAI 5: UDP PING-PONG ---");
        System.out.println("  [a] Chay UDP Server (Pong) - chay truoc");
        System.out.println("  [b] Chay UDP Client (Ping) - chay sau");
        System.out.print("-> Chon: ");
        String sub = scanner.nextLine().trim().toLowerCase();

        switch (sub) {
            case "a":
                UDPServer.run();
                break;
            case "b":
                UDPClient.run(scanner);
                break;
            default:
                System.out.println("[!] Lua chon khong hop le!");
        }
    }

    /**
     * Sub-menu cho Đề tài 6: Java RMI.
     * Chọn chạy Server hoặc Client.
     */
    private static void runDetai6Menu(Scanner scanner) {
        System.out.println("--- DE TAI 6: JAVA RMI ---");
        System.out.println("  [a] Chay RMI Server (dung rmiregistry co san hoac tu tao) - chay truoc");
        System.out.println("  [b] Chay RMI Client (goi ham tu xa) - chay sau");
        System.out.print("-> Chon: ");
        String sub = scanner.nextLine().trim().toLowerCase();

        switch (sub) {
            case "a":
                CalculatorServer.run();
                break;
            case "b":
                CalculatorClient.run(scanner);
                break;
            default:
                System.out.println("[!] Lua chon khong hop le!");
        }
    }
}
