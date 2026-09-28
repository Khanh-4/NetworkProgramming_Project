package com.project_final.networkprogramming_project.detai2_bufferedstream;

/**
 * Đề tài 2: Ứng dụng kỹ thuật luồng đệm (Buffered Stream)
 * ----------------------------------------------------------
 * Class chạy so sánh tốc độ và in biểu đồ I/O.
 * 
 * TODO (Khánh):
 * - Chạy UnbufferedCopy và BufferedCopy
 * - Đo thời gian cả 2
 * - In biểu đồ so sánh tốc độ I/O (text-based hoặc dùng thư viện)
 * 
 * @author Cao Duy Quốc Khánh
 */
public class PerformanceChart {

    public static void run() {
        System.out.println("=== ĐỀ TÀI 2: SO SÁNH TỐC ĐỘ BUFFERED vs UNBUFFERED ===");
        System.out.println("-----------------------------------------------------------");

        UnbufferedCopy.run();
        System.out.println();
        BufferedCopy.run();

        System.out.println("\n[TODO] Chưa implement biểu đồ so sánh");
    }
}
