package com.project_final.networkprogramming_project.detai2_bufferedstream;

import com.project_final.networkprogramming_project.detai1_bytecharstream.MedianTimer;
import com.project_final.networkprogramming_project.detai1_bytecharstream.TestFileUtils;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Đề tài 2: Sao chép file KHÔNG dùng Buffered Stream.
 * =====================================================
 *
 * ▸ Cơ chế: đọc/ghi trực tiếp xuống tầng OS, không có vùng đệm trung gian
 * ▸ Mỗi lần gọi read()/write() = 1 system call
 * ▸ System call rất tốn kém vì phải chuyển User Mode → Kernel Mode
 *
 * Luồng xử lý KHÔNG Buffered (đọc từng byte):
 *
 *   Ứng dụng → [read() system call] → OS Kernel → Disk → trả về 1 byte
 *   → Lặp lại hàng TRIỆU lần cho file lớn = RẤT CHẬM!
 *
 * GHI CHÚ VỀ THIẾT KẾ:
 *   Các method ở đây chỉ THỰC HIỆN việc sao chép, không tự đo thời gian và
 *   không tự in ra màn hình. Việc đo giao cho MedianTimer (có warm-up + trung
 *   vị), việc hiển thị giao cho PerformanceChart / PerformanceChartGUI.
 *   Tách như vậy thì cùng một đoạn code sao chép dùng được cho cả console lẫn
 *   GUI, và không còn 2 bản logic trùng nhau như trước.
 *
 * @author Cao Duy Quốc Khánh
 */
public class UnbufferedCopy {

    /**
     * Sao chép file bằng cách đọc/ghi TỪNG BYTE một, không buffer.
     * Đây là cách CHẬM NHẤT, dùng để thấy rõ chi phí của system call.
     *
     * @param srcPath  file nguồn
     * @param destPath file đích
     * @throws IOException nếu đọc/ghi thất bại
     */
    public static void copyByteByByte(String srcPath, String destPath) throws IOException {
        // try-with-resources: tự động đóng cả 2 stream, kể cả khi có exception
        try (FileInputStream fis = new FileInputStream(srcPath);
             FileOutputStream fos = new FileOutputStream(destPath)) {

            int byteData;
            // Mỗi lần read() và write() đều là 1 system call riêng
            // → Với file 1MB = 1.048.576 lần read + 1.048.576 lần write!
            while ((byteData = fis.read()) != -1) {
                fos.write(byteData);
            }
            // close() của try-with-resources tự gọi flush()
        }
    }

    /**
     * Sao chép file theo KHỐI nhưng KHÔNG bọc BufferedStream.
     *
     * Nhanh hơn hẳn byte-by-byte vì số system call giảm đi chunkSize lần.
     * So sánh method này với {@link BufferedCopy#copyWithCustomBuffer} cùng
     * kích thước khối sẽ thấy: khi đã tự đọc theo khối lớn thì BufferedStream
     * không còn tạo ra khác biệt lớn nữa — buffer chỉ cứu những code đọc lắt nhắt.
     *
     * @param srcPath   file nguồn
     * @param destPath  file đích
     * @param chunkSize kích thước khối đọc (bytes)
     * @throws IOException nếu đọc/ghi thất bại
     */
    public static void copyWithChunk(String srcPath, String destPath, int chunkSize)
            throws IOException {
        try (FileInputStream fis = new FileInputStream(srcPath);
             FileOutputStream fos = new FileOutputStream(destPath)) {

            byte[] chunk = new byte[chunkSize];
            int bytesRead;

            while ((bytesRead = fis.read(chunk)) != -1) {
                // Ghi đúng số byte đọc được, KHÔNG ghi cả mảng:
                // lần đọc cuối thường không đầy chunk → ghi thừa sẽ làm hỏng file
                fos.write(chunk, 0, bytesRead);
            }
        }
    }

    // =====================================================================
    // CHƯƠNG TRÌNH ĐỘC LẬP — đây là "chương trình sao chép KHÔNG dùng buffer"
    // trong yêu cầu "viết hai chương trình có/không dùng BufferedInputStream".
    //
    // Chạy:  java com.project_final.networkprogramming_project
    //             .detai2_bufferedstream.UnbufferedCopy [nguồn] [đích]
    //
    // Không truyền tham số → tự tạo file test và chạy demo.
    // Chương trình đối chứng: BufferedCopy
    // =====================================================================

    /** Kích thước file tự tạo khi chạy demo không tham số (MB). */
    private static final int DEMO_FILE_MB = 1;

    public static void main(String[] args) {
        System.out.println("=== CHƯƠNG TRÌNH SAO CHÉP KHÔNG DÙNG BUFFER ===");
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
            // để script gọi nó biết là đã thất bại.
            System.err.println("❌ Lỗi: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Chế độ demo: tự tạo file test, đo bằng warm-up + trung vị, rồi dọn dẹp. */
    private static void runDemo() throws IOException {
        TestFileUtils.ensureTestDir();
        String src = TestFileUtils.getTestDir() + "/unbuf_demo_src.dat";
        String dest = TestFileUtils.getTestDir() + "/unbuf_demo_dest.dat";

        System.out.println("Không có tham số → chạy demo với file tự tạo "
                + DEMO_FILE_MB + " MB.");
        TestFileUtils.generateBinaryFile(src, DEMO_FILE_MB);

        try {
            System.out.println("Đang sao chép TỪNG BYTE (không buffer)...");
            long nanos = MedianTimer.medianNanos(() -> copyByteByByte(src, dest));

            System.out.printf("✅ Xong: %.2f ms (trung vị, đã warm-up)%n",
                    MedianTimer.toMs(nanos));
            System.out.println();
            System.out.println("Mỗi byte tốn 1 system call read() + 1 system call write()");
            System.out.println("→ file " + DEMO_FILE_MB + " MB = hơn 2 triệu system call.");
            System.out.println("So sánh với BufferedCopy để thấy chênh lệch.");
        } finally {
            TestFileUtils.cleanupTestFiles(); // dọn dù thành công hay thất bại
        }
    }

    /** Sao chép file người dùng chỉ định, đo 1 lần. */
    private static void copyUserFile(String src, String dest) throws IOException {
        System.out.println("Nguồn: " + src);
        System.out.println("Đích : " + dest);

        long start = System.nanoTime();
        copyByteByByte(src, dest);
        double ms = (System.nanoTime() - start) / 1_000_000.0;

        System.out.printf("✅ Sao chép xong trong %.2f ms%n", ms);
        System.out.println("⚠️  Đây là phép đo MỘT LẦN nên chỉ mang tính tham khảo;");
        System.out.println("    muốn số liệu so sánh được thì chạy menu Đề tài 2.");
    }

    /** In hướng dẫn khi tham số không hợp lệ. */
    private static void printUsage() {
        System.out.println("Cách dùng:");
        System.out.println("  java ...UnbufferedCopy                 → chạy demo tự tạo file");
        System.out.println("  java ...UnbufferedCopy <nguồn> <đích>   → sao chép file có sẵn");
    }
}
