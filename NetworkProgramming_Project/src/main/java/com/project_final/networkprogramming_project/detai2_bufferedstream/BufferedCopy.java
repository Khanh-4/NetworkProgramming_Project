package com.project_final.networkprogramming_project.detai2_bufferedstream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Đề tài 2: Ứng dụng kỹ thuật luồng đệm (Buffered Stream)
 * ----------------------------------------------------------
 * Demo sao chép file CÓ dùng BufferedInputStream/BufferedOutputStream.
 *
 * ▸ Cơ chế Buffered:
 *   - BufferedInputStream tạo một VÙNG ĐỆM (mặc định 8192 bytes = 8KB)
 *   - Khi gọi read(): đọc 8KB từ đĩa vào buffer 1 lần
 *   - Các lần read() tiếp theo lấy từ BUFFER (trong RAM) → cực nhanh
 *   - Chỉ khi buffer hết mới gọi system call đọc từ đĩa
 *
 * Luồng xử lý CÓ Buffered:
 *   Ứng dụng → [read()] → Buffer (RAM) → trả byte/chunk
 *                              ↑
 *                     [Tự động nạp 8KB từ đĩa khi buffer hết]
 *
 * So sánh số lần System Call cho file 10MB:
 *   - Không buffer (byte-by-byte): 10,485,760 lần system call
 *   - Có buffer (8KB):             1,280 lần system call
 *   → Giảm 8,192 lần! → NHANH hơn rất nhiều
 *
 * @author Cao Duy Quốc Khánh
 */
public class BufferedCopy {

    /**
     * Sao chép file dùng BufferedInputStream/BufferedOutputStream.
     * Buffer mặc định 8192 bytes (8KB).
     *
     * @param srcPath  file nguồn
     * @param destPath file đích
     * @return thời gian sao chép (milliseconds)
     */
    public static long copyWithDefaultBuffer(String srcPath, String destPath) {
        System.out.println("📋 Sao chép BUFFERED (8KB mặc định)...");

        long startTime = System.nanoTime();
        long totalBytes = 0;

        // BufferedInputStream bọc (wrap) FileInputStream
        // → Thêm lớp đệm 8KB giữa ứng dụng và đĩa
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(srcPath));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destPath))) {

            int byteData;
            // Dù đọc từng byte, BufferedInputStream đã cache 8KB trong RAM
            // → read() lấy từ cache, không gọi system call mỗi lần
            while ((byteData = bis.read()) != -1) {
                bos.write(byteData);
                totalBytes++;
            }

            // flush() rất QUAN TRỌNG với BufferedOutputStream!
            // → Dữ liệu có thể vẫn nằm trong buffer chưa ghi
            // → Quên flush() = MẤT DỮ LIỆU CUỐI FILE
            bos.flush();

        } catch (IOException e) {
            System.err.println("❌ Lỗi sao chép: " + e.getMessage());
            return -1;
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        System.out.println("✅ Xong: " + totalBytes + " bytes | ⏱️ " + durationMs + " ms");
        return durationMs;
    }

    /**
     * Sao chép file dùng BufferedStream với buffer size tùy chỉnh.
     *
     * @param srcPath    file nguồn
     * @param destPath   file đích
     * @param bufferSize kích thước buffer (bytes)
     * @return thời gian sao chép (milliseconds)
     */
    public static long copyWithCustomBuffer(String srcPath, String destPath, int bufferSize) {
        String bufferLabel = bufferSize >= 1024
                ? (bufferSize / 1024) + "KB"
                : bufferSize + "B";
        System.out.println("📋 Sao chép BUFFERED (" + bufferLabel + " buffer)...");

        long startTime = System.nanoTime();
        long totalBytes = 0;

        // Chỉ định buffer size tùy chỉnh trong constructor
        try (BufferedInputStream bis = new BufferedInputStream(
                     new FileInputStream(srcPath), bufferSize);
             BufferedOutputStream bos = new BufferedOutputStream(
                     new FileOutputStream(destPath), bufferSize)) {

            byte[] chunk = new byte[bufferSize];
            int bytesRead;

            while ((bytesRead = bis.read(chunk)) != -1) {
                bos.write(chunk, 0, bytesRead);
                totalBytes += bytesRead;
            }

            bos.flush(); // KHÔNG QUÊN flush()!

        } catch (IOException e) {
            System.err.println("❌ Lỗi sao chép: " + e.getMessage());
            return -1;
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        System.out.println("✅ Xong: " + totalBytes + " bytes | ⏱️ " + durationMs + " ms");
        return durationMs;
    }
}
