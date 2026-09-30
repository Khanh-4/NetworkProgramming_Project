package com.project_final.networkprogramming_project.detai2_bufferedstream;

import com.project_final.networkprogramming_project.detai1_bytecharstream.MedianTimer;
import com.project_final.networkprogramming_project.detai1_bytecharstream.TestFileUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Đề tài 2: Sao chép file CÓ dùng BufferedInputStream / BufferedOutputStream.
 * ===========================================================================
 *
 * ▸ Cơ chế Buffered:
 *   - BufferedInputStream tạo một VÙNG ĐỆM trong RAM (mặc định 8192 byte = 8KB)
 *   - Khi gọi read(): nạp nguyên 8KB từ đĩa vào buffer trong 1 system call
 *   - Các lần read() tiếp theo lấy từ BUFFER (RAM) → không tốn system call
 *   - Chỉ khi buffer cạn mới gọi system call nạp tiếp
 *
 * Luồng xử lý CÓ Buffered:
 *
 *   Ứng dụng → [read()] → Buffer (RAM) → trả byte
 *                            ↑
 *                  [tự động nạp 8KB từ đĩa khi buffer cạn]
 *
 * So sánh số system call khi sao chép file 10MB:
 *   - Không buffer (byte-by-byte): 10.485.760 lần
 *   - Có buffer (8KB)            :      1.280 lần
 *   → Giảm 8192 lần → đây chính là nguồn gốc của chênh lệch tốc độ.
 *
 * GHI CHÚ VỀ THIẾT KẾ: xem {@link UnbufferedCopy} — các method ở đây chỉ sao
 * chép, không tự đo thời gian và không tự in kết quả.
 *
 * @author Cao Duy Quốc Khánh
 */
public class BufferedCopy {

    /** Kích thước buffer mặc định của BufferedInputStream trong JDK. */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Sao chép file bằng BufferedStream nhưng vẫn đọc/ghi TỪNG BYTE.
     *
     * Đây là phép so sánh công bằng với {@link UnbufferedCopy#copyByteByByte}:
     * cùng đọc từng byte, chỉ khác là có thêm lớp đệm.
     * Chênh lệch thời gian giữa hai method này chính là chi phí của system call.
     *
     * @param srcPath  file nguồn
     * @param destPath file đích
     * @throws IOException nếu đọc/ghi thất bại
     */
    public static void copyWithDefaultBuffer(String srcPath, String destPath)
            throws IOException {
        // BufferedInputStream BỌC (wrap) FileInputStream → thêm lớp đệm 8KB
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(srcPath));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destPath))) {

            int byteData;
            // Vẫn gọi read() từng byte, nhưng phần lớn lấy từ buffer trong RAM
            while ((byteData = bis.read()) != -1) {
                bos.write(byteData);
            }
            // close() của try-with-resources tự gọi flush().
            // BẪY KINH ĐIỂN: nếu tự quản lý stream mà quên flush() thì phần dữ
            // liệu còn nằm trong buffer sẽ KHÔNG bao giờ xuống đĩa → mất đuôi file.
        }
    }

    /**
     * Sao chép file bằng BufferedStream với kích thước buffer tùy chỉnh.
     *
     * Dùng để khảo sát: tăng buffer từ 8KB lên 32KB, 64KB có nhanh thêm không?
     * (Thường lợi ích giảm dần, vì khi buffer đã đủ lớn thì nút thắt chuyển từ
     * số system call sang băng thông của đĩa.)
     *
     * @param srcPath    file nguồn
     * @param destPath   file đích
     * @param bufferSize kích thước buffer (bytes)
     * @throws IOException nếu đọc/ghi thất bại
     */
    public static void copyWithCustomBuffer(String srcPath, String destPath, int bufferSize)
            throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(
                     new FileInputStream(srcPath), bufferSize);
             BufferedOutputStream bos = new BufferedOutputStream(
                     new FileOutputStream(destPath), bufferSize)) {

            byte[] chunk = new byte[bufferSize];
            int bytesRead;

            while ((bytesRead = bis.read(chunk)) != -1) {
                bos.write(chunk, 0, bytesRead); // ghi đúng số byte đọc được
            }
        }
    }

    /** Đổi số byte thành nhãn dễ đọc, ví dụ 8192 → "8KB". */
    public static String bufferLabel(int bufferSize) {
        return bufferSize >= 1024 ? (bufferSize / 1024) + "KB" : bufferSize + "B";
    }

    // =====================================================================
    // CHƯƠNG TRÌNH ĐỘC LẬP — đây là "chương trình sao chép CÓ dùng
    // BufferedInputStream" trong yêu cầu "viết hai chương trình có/không
    // dùng BufferedInputStream".
    //
    // Chạy:  java com.project_final.networkprogramming_project
    //             .detai2_bufferedstream.BufferedCopy [nguồn] [đích]
    //
    // Không truyền tham số → tự tạo file test và chạy demo.
    // Chương trình đối chứng: UnbufferedCopy
    // =====================================================================

    /** Kích thước file tự tạo khi chạy demo không tham số (MB). */
    private static final int DEMO_FILE_MB = 1;

    public static void main(String[] args) {
        System.out.println("=== CHƯƠNG TRÌNH SAO CHÉP CÓ DÙNG BUFFER ===");
        System.out.println();

        try {
            if (args.length == 0) {
                runDemo();
            } else if (args.length == 2) {
                copyUserFile(args[0], args[1]);
            } else {
                printUsage();
            }
        } catch (IOException e) {
            // Chương trình độc lập: báo lỗi rõ ràng và trả exit code khác 0
            System.err.println("❌ Lỗi: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Chế độ demo: tự tạo file test, đo bằng warm-up + trung vị, rồi dọn dẹp. */
    private static void runDemo() throws IOException {
        TestFileUtils.ensureTestDir();
        String src = TestFileUtils.getTestDir() + "/buf_demo_src.dat";
        String dest = TestFileUtils.getTestDir() + "/buf_demo_dest.dat";

        System.out.println("Không có tham số → chạy demo với file tự tạo "
                + DEMO_FILE_MB + " MB.");
        TestFileUtils.generateBinaryFile(src, DEMO_FILE_MB);

        try {
            System.out.println("Đang sao chép TỪNG BYTE qua buffer "
                    + bufferLabel(DEFAULT_BUFFER_SIZE) + "...");
            long nanos = MedianTimer.medianNanos(() -> copyWithDefaultBuffer(src, dest));

            System.out.printf("✅ Xong: %.2f ms (trung vị, đã warm-up)%n",
                    MedianTimer.toMs(nanos));
            System.out.println();
            System.out.println("Vẫn gọi read() từng byte, nhưng phần lớn lấy từ buffer trong RAM");
            System.out.println("→ file " + DEMO_FILE_MB + " MB chỉ còn khoảng "
                    + (DEMO_FILE_MB * 1024 * 1024 / DEFAULT_BUFFER_SIZE)
                    + " system call cho mỗi chiều.");
            System.out.println("So sánh với UnbufferedCopy để thấy chênh lệch.");
        } finally {
            TestFileUtils.cleanupTestFiles(); // dọn dù thành công hay thất bại
        }
    }

    /** Sao chép file người dùng chỉ định, đo 1 lần. */
    private static void copyUserFile(String src, String dest) throws IOException {
        System.out.println("Nguồn: " + src);
        System.out.println("Đích : " + dest);

        long start = System.nanoTime();
        copyWithDefaultBuffer(src, dest);
        double ms = (System.nanoTime() - start) / 1_000_000.0;

        System.out.printf("✅ Sao chép xong trong %.2f ms%n", ms);
        System.out.println("⚠️  Đây là phép đo MỘT LẦN nên chỉ mang tính tham khảo;");
        System.out.println("    muốn số liệu so sánh được thì chạy menu Đề tài 2.");
    }

    /** In hướng dẫn khi tham số không hợp lệ. */
    private static void printUsage() {
        System.out.println("Cách dùng:");
        System.out.println("  java ...BufferedCopy                 → chạy demo tự tạo file");
        System.out.println("  java ...BufferedCopy <nguồn> <đích>   → sao chép file có sẵn");
    }
}
