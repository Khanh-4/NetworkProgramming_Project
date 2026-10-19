package com.project_final.networkprogramming_project.detai2_bufferedstream;

import com.project_final.networkprogramming_project.detai1_bytecharstream.BenchmarkUtils;
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
 *   không tự in ra màn hình. Việc đo giao cho BenchmarkUtils (có warm-up + trung
 *   vị), việc hiển thị giao cho PerformanceChartGUI.
 *   Tách như vậy thì cùng một đoạn code sao chép dùng được cho cả main() của
 *   file này lẫn CopyBenchmark (GUI), không phải viết hai lần.
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
        System.out.println("=== CHUONG TRINH SAO CHEP KHONG DUNG BUFFER ===");
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
            System.err.println("[X] Loi: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Chế độ demo: tự tạo file test, đo bằng warm-up + trung vị, rồi dọn dẹp. */
    private static void runDemo() throws IOException {
        BenchmarkUtils.ensureTestDir();
        String src = BenchmarkUtils.getTestDir() + "/unbuf_demo_src.dat";
        String dest = BenchmarkUtils.getTestDir() + "/unbuf_demo_dest.dat";

        System.out.println("Khong co tham so -> chay demo voi file tu tao "
                + DEMO_FILE_MB + " MB.");
        BenchmarkUtils.generateBinaryFile(src, DEMO_FILE_MB);

        try {
            System.out.println("Dang sao chep TUNG BYTE (khong buffer)...");
            long nanos = BenchmarkUtils.medianNanos(() -> copyByteByByte(src, dest));

            System.out.printf("[OK] Xong: %.2f ms (trung vi, da warm-up)%n",
                    BenchmarkUtils.toMs(nanos));
            System.out.println();
            System.out.println("Moi byte ton 1 system call read() + 1 system call write()");
            System.out.println("-> file " + DEMO_FILE_MB + " MB = hon 2 trieu system call.");
            System.out.println("So sanh voi BufferedCopy de thay chenh lech.");
        } finally {
            BenchmarkUtils.cleanupTestFiles(); // dọn dù thành công hay thất bại
        }
    }

    /** Sao chép file người dùng chỉ định, đo 1 lần. */
    private static void copyUserFile(String src, String dest) throws IOException {
        System.out.println("Nguon: " + src);
        System.out.println("Dich : " + dest);

        long start = System.nanoTime();
        copyByteByByte(src, dest);
        double ms = (System.nanoTime() - start) / 1_000_000.0;

        System.out.printf("[OK] Sao chep xong trong %.2f ms%n", ms);
        System.out.println("[!] Day la phep do MOT LAN nen chi mang tinh tham khao;");
        System.out.println("    muon so lieu so sanh duoc thi chay menu De tai 2.");
    }

    /** In hướng dẫn khi tham số không hợp lệ. */
    private static void printUsage() {
        System.out.println("Cach dung:");
        System.out.println("  java ...UnbufferedCopy                 -> chay demo tu tao file");
        System.out.println("  java ...UnbufferedCopy <nguon> <dich>   -> sao chep file co san");
    }
}
