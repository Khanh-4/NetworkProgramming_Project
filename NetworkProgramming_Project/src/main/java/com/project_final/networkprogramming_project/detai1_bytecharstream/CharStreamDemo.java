package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Đề tài 1: Luồng byte và luồng ký tự trong Java
 * -------------------------------------------------
 * Demo đọc file bằng FileReader (Character Stream).
 *
 * ▸ FileReader đọc dữ liệu dạng KÝ TỰ (char, 16-bit Unicode), phù hợp cho:
 *   - File text (.txt, .csv, .xml, .json, .java)
 *   - Tự động xử lý encoding (UTF-8, UTF-16...)
 * ▸ Đọc từng char hoặc mảng char[] → chính xác cho text, nhưng chậm hơn với nhị phân
 * ▸ TỰ ĐỘNG chuyển đổi encoding → hiển thị tiếng Việt, emoji đúng
 *
 * SO SÁNH VỚI ByteStream:
 * ┌──────────────────┬──────────────────┬──────────────────┐
 * │                  │  ByteStream      │  CharStream      │
 * ├──────────────────┼──────────────────┼──────────────────┤
 * │ Đơn vị đọc       │  byte (8-bit)    │  char (16-bit)   │
 * │ Lớp chính        │  InputStream     │  Reader          │
 * │ File phù hợp     │  Nhị phân        │  Văn bản         │
 * │ Encoding         │  Không xử lý     │  Tự động         │
 * │ Tốc độ nhị phân  │  Nhanh hơn       │  Chậm hơn        │
 * │ Tốc độ văn bản   │  Tương đương     │  Chính xác hơn   │
 * └──────────────────┴──────────────────┴──────────────────┘
 *
 * @author Cao Duy Quốc Khánh
 */
public class CharStreamDemo {

    /**
     * Đọc file bằng FileReader và đo thời gian.
     *
     * @param filePath đường dẫn file cần đọc
     * @return thời gian đọc (milliseconds)
     */
    public static long run(String filePath) {
        System.out.println("=== CHAR STREAM DEMO (FileReader) ===");
        System.out.println("File: " + filePath);

        long startTime = System.nanoTime();
        long totalChars = 0;

        // try-with-resources: tự động đóng Reader khi kết thúc
        // CHỈ ĐỊNH UTF-8 TƯỜNG MINH: nếu để new FileReader(filePath) thì nó dùng
        // Charset.defaultCharset() — giá trị phụ thuộc JDK/OS, đọc sai tiếng Việt
        // trên môi trường khác. Xem EncodingDemo để thấy bằng chứng.
        try (FileReader fr = new FileReader(filePath, StandardCharsets.UTF_8)) {

            char[] buffer = new char[4096]; // Đọc mỗi lần 4K ký tự
            int charsRead;

            // Đọc cho đến khi hết file (read() trả về -1)
            while ((charsRead = fr.read(buffer)) != -1) {
                totalChars += charsRead;
            }

        } catch (IOException e) {
            System.err.println("❌ Lỗi đọc file bằng CharStream: " + e.getMessage());
            return -1;
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        System.out.println("✅ Đọc xong: " + totalChars + " ký tự");
        System.out.println("⏱️  Thời gian: " + durationMs + " ms");
        System.out.println();

        return durationMs;
    }

    // Việc tạo file test đã chuyển hết sang TestFileUtils.generateTextFile().
    // Trước đây class này có bản riêng, trùng logic với TestFileUtils và dùng
    // FileWriter không charset => 2 nơi tạo file với encoding khác nhau.
}
