package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Đề tài 1 & 2: Utility class quản lý file test
 * ------------------------------------------------
 * Chức năng:
 * - Tạo file text test (có nội dung tiếng Việt)
 * - Tạo file binary test
 * - Dọn dẹp file test sau khi benchmark
 *
 * Tách riêng để tránh duplicate code giữa các class.
 *
 * @author Cao Duy Quốc Khánh
 */
public class TestFileUtils {

    private static final String TEST_DIR = "testdata";

    /** 1 MB = 1024 * 1024 byte. Đặt tên hằng để tránh magic number rải rác. */
    private static final int BYTES_PER_MB = 1024 * 1024;

    /**
     * Lấy đường dẫn thư mục test.
     */
    public static String getTestDir() {
        return TEST_DIR;
    }

    /**
     * Tạo thư mục test nếu chưa tồn tại.
     */
    public static void ensureTestDir() {
        File dir = new File(TEST_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Tạo file TEXT test (UTF-8) với nội dung tiếng Việt có dấu.
     *
     * VÌ SAO PHẢI CHỈ ĐỊNH UTF-8 TƯỜNG MINH?
     *   FileWriter không tham số charset sẽ dùng Charset.defaultCharset().
     *   Giá trị đó phụ thuộc phiên bản JDK và OS (xem {@link EncodingDemo}),
     *   nên cùng một đoạn code có thể tạo ra file với số byte khác nhau
     *   trên 2 máy => benchmark không tái lập được.
     *
     * Nội dung CÓ DẤU là cố ý: ký tự có dấu chiếm 2-3 byte trong UTF-8,
     * nhờ đó CharStream phải decode thật sự thay vì đi đường nhanh ASCII.
     *
     * @param filePath đường dẫn file cần tạo
     * @param sizeInMB kích thước mong muốn (MB)
     * @throws IOException nếu không ghi được file
     */
    public static void generateTextFile(String filePath, int sizeInMB) throws IOException {
        long target = (long) sizeInMB * BYTES_PER_MB;
        if (hasExpectedSize(filePath, target)) {
            return; // File đúng kích thước đã có sẵn, không cần tạo lại
        }

        String sample = "Lập trình mạng máy tính - Network Programming - "
                + "Đề tài 1: So sánh ByteStream và CharStream. "
                + "Đây là dòng text mẫu dùng để test hiệu suất đọc file. "
                + "Tiếng Việt có dấu: ă â đ ê ô ơ ư. "
                + "Số liệu: 0123456789.\n";

        // Đếm theo SỐ BYTE thực tế khi encode UTF-8, không phải String.length().
        // String.length() đếm char => với ký tự có dấu sẽ ra file NHỎ hơn dự kiến.
        int sampleBytes = sample.getBytes(StandardCharsets.UTF_8).length;

        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            long written = 0;
            while (written < target) {
                writer.write(sample);
                written += sampleBytes;
            }
            // close() của try-with-resources tự gọi flush() => dữ liệu xuống đĩa đủ.
        }
    }

    /**
     * Tạo file BINARY test.
     *
     * Dữ liệu là byte 0x00..0xFF lặp lại — phần lớn KHÔNG phải chuỗi UTF-8 hợp lệ.
     * Đó chính là mục đích: khi CharStream đọc file này, CharsetDecoder buộc phải
     * thay các byte không decode được bằng U+FFFD, chứng minh rằng CharStream
     * làm HỎNG dữ liệu nhị phân (xem {@link StreamBenchmark}).
     *
     * @param filePath đường dẫn file cần tạo
     * @param sizeInMB kích thước (MB)
     * @throws IOException nếu không ghi được file
     */
    public static void generateBinaryFile(String filePath, int sizeInMB) throws IOException {
        long target = (long) sizeInMB * BYTES_PER_MB;
        if (hasExpectedSize(filePath, target)) {
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            byte[] chunk = new byte[BYTES_PER_MB]; // ghi từng khối 1MB
            for (int i = 0; i < chunk.length; i++) {
                chunk[i] = (byte) (i % 256);
            }
            for (int i = 0; i < sizeInMB; i++) {
                fos.write(chunk);
            }
        }
    }

    /**
     * Kiểm tra file đã tồn tại VÀ đúng kích thước mong đợi hay chưa.
     *
     * Bản cũ chỉ kiểm tra {@code file.exists()} nên nếu lần chạy trước để lại
     * file cùng tên nhưng sai kích thước (ví dụ bị ngắt giữa lúc ghi), benchmark
     * sẽ âm thầm đo trên file sai => số liệu vô nghĩa mà không có cảnh báo nào.
     *
     * @param expectedBytes kích thước mong đợi (byte)
     * @return true nếu file dùng lại được
     */
    private static boolean hasExpectedSize(String filePath, long expectedBytes) {
        File file = new File(filePath);
        if (!file.isFile()) {
            return false;
        }
        // Cho phép lệch nhỏ vì vòng ghi dừng khi VƯỢT target, không dừng đúng mốc
        long tolerance = BYTES_PER_MB / 1024;
        if (Math.abs(file.length() - expectedBytes) <= tolerance) {
            return true;
        }
        file.delete(); // Sai kích thước => xoá để tạo lại
        return false;
    }

    /**
     * Dọn dẹp toàn bộ file test và thư mục.
     */
    public static void cleanupTestFiles() {
        File dir = new File(TEST_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
        }
    }
}
