package com.project_final.networkprogramming_project.detai1_bytecharstream;

/**
 * Đề tài 1: Luồng byte và luồng ký tự trong Java
 * -------------------------------------------------
 * Class chính chạy so sánh tốc độ giữa ByteStream và CharStream.
 * Gọi ByteStreamDemo và CharStreamDemo, log thời gian thực thi.
 * 
 * TODO (Khánh):
 * - Tạo file test (hoặc dùng file có sẵn)
 * - Chạy cả 2 phương pháp đọc
 * - So sánh và in bảng kết quả thời gian
 * 
 * @author Cao Duy Quốc Khánh
 */
public class StreamComparison {

    public static void run() {
        System.out.println("=== ĐỀ TÀI 1: SO SÁNH BYTE STREAM vs CHAR STREAM ===");
        System.out.println("------------------------------------------------------");

        // TODO: Gọi ByteStreamDemo.run() và CharStreamDemo.run()
        // TODO: Đo thời gian và in bảng so sánh
        ByteStreamDemo.run();
        System.out.println();
        CharStreamDemo.run();

        System.out.println("\n[TODO] Chưa implement bảng so sánh thời gian");
    }
}
