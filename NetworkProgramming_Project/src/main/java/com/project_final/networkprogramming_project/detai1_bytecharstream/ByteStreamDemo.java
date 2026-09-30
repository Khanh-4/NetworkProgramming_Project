package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Đề tài 1: Luồng byte và luồng ký tự trong Java
 * -------------------------------------------------
 * Demo đọc file bằng FileInputStream (Byte Stream).
 *
 * ▸ FileInputStream đọc dữ liệu dạng BYTE (8-bit), phù hợp cho:
 *   - File nhị phân (ảnh, video, .exe, .zip)
 *   - File text (nhưng không xử lý encoding tự động)
 * ▸ Đọc từng byte hoặc mảng byte[] → nhanh với dữ liệu nhị phân
 * ▸ KHÔNG tự chuyển đổi encoding (UTF-8, UTF-16...) → có thể lỗi ký tự đặc biệt
 *
 * @author Cao Duy Quốc Khánh
 */
public class ByteStreamDemo {

    /**
     * Đọc file bằng FileInputStream và đo thời gian.
     *
     * @param filePath đường dẫn file cần đọc
     * @return thời gian đọc (milliseconds)
     */
    public static long run(String filePath) {
        System.out.println("=== BYTE STREAM DEMO (FileInputStream) ===");
        System.out.println("File: " + filePath);

        long startTime = System.nanoTime();
        long totalBytes = 0;

        // try-with-resources: tự động đóng stream khi kết thúc
        // → Tránh resource leak (lỗi phổ biến khi không đóng stream)
        try (FileInputStream fis = new FileInputStream(filePath)) {

            byte[] buffer = new byte[8192]; // Đọc mỗi lần 8KB (tối ưu I/O)
            int bytesRead;

            // Đọc cho đến khi hết file (read() trả về -1)
            while ((bytesRead = fis.read(buffer)) != -1) {
                totalBytes += bytesRead;
            }

        } catch (IOException e) {
            System.err.println("❌ Lỗi đọc file bằng ByteStream: " + e.getMessage());
            return -1;
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000; // Nano → Milli

        System.out.println("✅ Đọc xong: " + totalBytes + " bytes");
        System.out.println("⏱️  Thời gian: " + durationMs + " ms");
        System.out.println();

        return durationMs;
    }

    // Việc tạo file test đã chuyển hết sang TestFileUtils.generateBinaryFile(),
    // để chỉ có MỘT nơi chịu trách nhiệm sinh file test cho cả đề tài 1 và 2.
}
