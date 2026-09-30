package com.project_final.networkprogramming_project.detai2_bufferedstream;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Đề tài 2: Ứng dụng kỹ thuật luồng đệm (Buffered Stream)
 * ----------------------------------------------------------
 * Demo sao chép file KHÔNG dùng Buffered Stream.
 *
 * ▸ Cơ chế: Đọc/ghi từng byte hoặc khối nhỏ trực tiếp từ đĩa
 * ▸ Mỗi lần gọi read()/write() = 1 lần system call đến OS
 * ▸ System call rất TỐN THỜI GIAN vì phải chuyển từ User Mode → Kernel Mode
 *
 * Luồng xử lý KHÔNG Buffered:
 *   Ứng dụng → [read() system call] → OS Kernel → Disk → trả 1 byte
 *   → Lặp lại hàng TRIỆU lần cho file lớn = RẤT CHẬM!
 *
 * @author Cao Duy Quốc Khánh
 */
public class UnbufferedCopy {

    /**
     * Sao chép file KHÔNG dùng buffer (đọc từng byte).
     * Đây là cách CHẬM NHẤT để demo sự khác biệt.
     *
     * @param srcPath  file nguồn
     * @param destPath file đích
     * @return thời gian sao chép (milliseconds)
     */
    public static long copyByteByByte(String srcPath, String destPath) {
        System.out.println("📋 Sao chép TỪNG BYTE (không buffer)...");

        long startTime = System.nanoTime();
        long totalBytes = 0;

        // try-with-resources: tự động đóng cả 2 stream
        try (FileInputStream fis = new FileInputStream(srcPath);
             FileOutputStream fos = new FileOutputStream(destPath)) {

            int byteData;
            // Đọc từng byte: mỗi lần read() = 1 system call
            // → Với file 10MB = 10,485,760 system calls!
            while ((byteData = fis.read()) != -1) {
                fos.write(byteData);
                totalBytes++;
            }

            fos.flush(); // Đảm bảo ghi hết dữ liệu xuống đĩa

        } catch (IOException e) {
            System.err.println("❌ Lỗi sao chép: " + e.getMessage());
            return -1;
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        System.out.println("✅ Xong: " + totalBytes + " bytes | ⏱️ " + durationMs + " ms");
        return durationMs;
    }

    /**
     * Sao chép file KHÔNG dùng BufferedStream nhưng đọc theo khối.
     * Nhanh hơn byte-by-byte nhưng vẫn chậm hơn Buffered.
     *
     * @param srcPath   file nguồn
     * @param destPath  file đích
     * @param chunkSize kích thước khối đọc (bytes)
     * @return thời gian sao chép (milliseconds)
     */
    public static long copyWithChunk(String srcPath, String destPath, int chunkSize) {
        System.out.println("📋 Sao chép KHỐI " + chunkSize + " bytes (không buffer)...");

        long startTime = System.nanoTime();
        long totalBytes = 0;

        try (FileInputStream fis = new FileInputStream(srcPath);
             FileOutputStream fos = new FileOutputStream(destPath)) {

            byte[] chunk = new byte[chunkSize];
            int bytesRead;

            while ((bytesRead = fis.read(chunk)) != -1) {
                fos.write(chunk, 0, bytesRead);
                totalBytes += bytesRead;
            }

            fos.flush();

        } catch (IOException e) {
            System.err.println("❌ Lỗi sao chép: " + e.getMessage());
            return -1;
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        System.out.println("✅ Xong: " + totalBytes + " bytes | ⏱️ " + durationMs + " ms");
        return durationMs;
    }
}
