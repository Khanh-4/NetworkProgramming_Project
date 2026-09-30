package com.project_final.networkprogramming_project.detai2_bufferedstream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Đề tài 2: Ứng dụng kỹ thuật luồng đệm (Buffered Stream)
 * ----------------------------------------------------------
 * Class chính: Chạy so sánh tốc độ sao chép file giữa:
 *   1. Không buffer (từng byte) → CHẬM NHẤT
 *   2. Không buffer (khối 8KB) → TRUNG BÌNH
 *   3. Có BufferedStream (8KB) → NHANH
 *   4. Có BufferedStream (64KB) → NHANH NHẤT
 *
 * Kết quả: In biểu đồ so sánh tốc độ I/O trên console.
 *
 * @author Cao Duy Quốc Khánh
 */
public class PerformanceChart {

    private static final String TEST_DIR = "testdata";
    // Dùng file nhỏ hơn cho byte-by-byte (vì rất chậm)
    private static final int SMALL_FILE_MB = 1;
    // File lớn hơn cho các phương pháp nhanh
    private static final int LARGE_FILE_MB = 10;

    public static void run() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  ĐỀ TÀI 2: SO SÁNH BUFFERED vs UNBUFFERED STREAM            ║");
        System.out.println("║  Người thực hiện: Cao Duy Quốc Khánh                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // ========== PHẦN 1: GIẢI THÍCH LÝ THUYẾT ==========
        printTheory();

        // ========== PHẦN 2: TẠO FILE TEST ==========
        File testDir = new File(TEST_DIR);
        if (!testDir.exists()) {
            testDir.mkdirs();
        }

        String srcSmall = TEST_DIR + "/source_" + SMALL_FILE_MB + "MB.dat";
        String srcLarge = TEST_DIR + "/source_" + LARGE_FILE_MB + "MB.dat";
        generateBinaryFile(srcSmall, SMALL_FILE_MB);
        generateBinaryFile(srcLarge, LARGE_FILE_MB);

        // ========== PHẦN 3: CHẠY CÁC PHƯƠNG PHÁP SAO CHÉP ==========
        System.out.println("━".repeat(60));
        System.out.println("🔬 TEST 1: Sao chép file " + SMALL_FILE_MB + " MB - So sánh byte-by-byte");
        System.out.println("━".repeat(60));

        // 1. Không buffer, từng byte (CHẬM - chỉ dùng file nhỏ)
        long time1 = UnbufferedCopy.copyByteByByte(
                srcSmall, TEST_DIR + "/copy_unbuf_byte.dat");
        System.out.println();

        // 2. Có buffer, từng byte (cải thiện lớn)
        long time2 = BufferedCopy.copyWithDefaultBuffer(
                srcSmall, TEST_DIR + "/copy_buf_byte.dat");
        System.out.println();

        System.out.println("━".repeat(60));
        System.out.println("🔬 TEST 2: Sao chép file " + LARGE_FILE_MB + " MB - So sánh buffer size");
        System.out.println("━".repeat(60));

        // 3. Không buffer, khối 8KB
        long time3 = UnbufferedCopy.copyWithChunk(
                srcLarge, TEST_DIR + "/copy_unbuf_chunk.dat", 8192);
        System.out.println();

        // 4. Buffered 8KB (mặc định)
        long time4 = BufferedCopy.copyWithCustomBuffer(
                srcLarge, TEST_DIR + "/copy_buf_8k.dat", 8192);
        System.out.println();

        // 5. Buffered 32KB
        long time5 = BufferedCopy.copyWithCustomBuffer(
                srcLarge, TEST_DIR + "/copy_buf_32k.dat", 32768);
        System.out.println();

        // 6. Buffered 64KB
        long time6 = BufferedCopy.copyWithCustomBuffer(
                srcLarge, TEST_DIR + "/copy_buf_64k.dat", 65536);
        System.out.println();

        // ========== PHẦN 4: IN KẾT QUẢ ==========
        printResultTable(time1, time2, time3, time4, time5, time6);

        // ========== PHẦN 5: VẼ BIỂU ĐỒ ==========
        printBarChart(time1, time2, time3, time4, time5, time6);

        // ========== PHẦN 6: KẾT LUẬN ==========
        printConclusion(time1, time2);

        // ========== DỌN DẸP ==========
        cleanupTestFiles();
    }

    /**
     * In phần lý thuyết về Buffered Stream.
     */
    private static void printTheory() {
        System.out.println("📖 LÝ THUYẾT:");
        System.out.println("─".repeat(60));
        System.out.println("▸ Vấn đề của Unbuffered Stream:");
        System.out.println("  - Mỗi read()/write() = 1 system call đến OS Kernel");
        System.out.println("  - System call tốn ~1000 CPU cycles (User→Kernel mode)");
        System.out.println("  - File 10MB đọc byte-by-byte = 10 TRIỆU system calls!");
        System.out.println();
        System.out.println("▸ Giải pháp: Buffered Stream");
        System.out.println("  - Tạo vùng đệm (buffer) trong RAM, mặc định 8KB");
        System.out.println("  - 1 system call đọc 8KB vào buffer");
        System.out.println("  - Các lần read() tiếp lấy từ buffer (cực nhanh)");
        System.out.println("  - File 10MB chỉ cần ~1,280 system calls (giảm 8000x)");
        System.out.println();
        System.out.println("▸ Minh họa:");
        System.out.println("  Không buffer: App ←→ Disk (mỗi byte)");
        System.out.println("  Có buffer:    App ←→ Buffer(RAM) ←→ Disk (mỗi 8KB)");
        System.out.println();
    }

    /**
     * In bảng kết quả.
     */
    private static void printResultTable(long t1, long t2, long t3,
                                          long t4, long t5, long t6) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      BẢNG KẾT QUẢ SO SÁNH                      ║");
        System.out.println("╠══════════════════════════════════════╦═══════════╦═══════════════╣");
        System.out.println("║ Phương pháp                         ║ File Size ║ Thời gian     ║");
        System.out.println("╠══════════════════════════════════════╬═══════════╬═══════════════╣");
        System.out.printf("║ ① Unbuffered (byte-by-byte)          ║ %-7s   ║ %-11s   ║%n",
                SMALL_FILE_MB + " MB", t1 + " ms");
        System.out.printf("║ ② Buffered 8KB (byte-by-byte)        ║ %-7s   ║ %-11s   ║%n",
                SMALL_FILE_MB + " MB", t2 + " ms");
        System.out.printf("║ ③ Unbuffered (chunk 8KB)             ║ %-7s   ║ %-11s   ║%n",
                LARGE_FILE_MB + " MB", t3 + " ms");
        System.out.printf("║ ④ Buffered 8KB (chunk 8KB)           ║ %-7s   ║ %-11s   ║%n",
                LARGE_FILE_MB + " MB", t4 + " ms");
        System.out.printf("║ ⑤ Buffered 32KB (chunk 32KB)         ║ %-7s   ║ %-11s   ║%n",
                LARGE_FILE_MB + " MB", t5 + " ms");
        System.out.printf("║ ⑥ Buffered 64KB (chunk 64KB)         ║ %-7s   ║ %-11s   ║%n",
                LARGE_FILE_MB + " MB", t6 + " ms");
        System.out.println("╚══════════════════════════════════════╩═══════════╩═══════════════╝");
    }

    /**
     * Vẽ biểu đồ bar chart so sánh tốc độ I/O.
     */
    private static void printBarChart(long t1, long t2, long t3,
                                       long t4, long t5, long t6) {
        System.out.println();
        System.out.println("📊 BIỂU ĐỒ SO SÁNH TỐC ĐỘ I/O:");
        System.out.println("─".repeat(65));

        // --- Nhóm 1: File nhỏ, byte-by-byte ---
        System.out.println("  🔸 File " + SMALL_FILE_MB + " MB (byte-by-byte):");
        long maxSmall = Math.max(t1, t2);
        if (maxSmall == 0) maxSmall = 1;

        printBar("Unbuffered", t1, maxSmall, "█");
        printBar("Buffered  ", t2, maxSmall, "▓");
        if (t2 > 0) {
            System.out.printf("  → Buffered nhanh hơn %.1fx%n", (double) t1 / t2);
        }
        System.out.println();

        // --- Nhóm 2: File lớn, các buffer size ---
        System.out.println("  🔸 File " + LARGE_FILE_MB + " MB (so sánh buffer size):");
        long maxLarge = Math.max(t3, Math.max(t4, Math.max(t5, t6)));
        if (maxLarge == 0) maxLarge = 1;

        printBar("No buf 8KB ", t3, maxLarge, "█");
        printBar("Buf 8KB    ", t4, maxLarge, "▓");
        printBar("Buf 32KB   ", t5, maxLarge, "▒");
        printBar("Buf 64KB   ", t6, maxLarge, "░");
        System.out.println();

        System.out.println("  Chú thích: █ Unbuffered | ▓ Buf 8K | ▒ Buf 32K | ░ Buf 64K");
    }

    /**
     * Vẽ 1 thanh bar.
     */
    private static void printBar(String label, long value, long maxValue, String symbol) {
        int maxWidth = 35;
        int barLen = (int) (value * maxWidth / maxValue);
        barLen = Math.max(barLen, 1);
        System.out.printf("    %s ║%s║ %d ms%n", label, symbol.repeat(barLen), value);
    }

    /**
     * In kết luận.
     */
    private static void printConclusion(long unbufferedTime, long bufferedTime) {
        System.out.println();
        System.out.println("📝 KẾT LUẬN:");
        System.out.println("─".repeat(60));
        System.out.println("▸ BufferedStream NHANH hơn Unbuffered rất nhiều lần.");
        System.out.println("  → Đặc biệt rõ khi đọc/ghi TỪNG BYTE (byte-by-byte)");
        if (bufferedTime > 0) {
            System.out.printf("  → Tốc độ cải thiện: ~%.0fx lần%n",
                    (double) unbufferedTime / bufferedTime);
        }
        System.out.println();
        System.out.println("▸ Tăng buffer size giúp cải thiện thêm, nhưng có giới hạn:");
        System.out.println("  - 8KB  → tốt cho đa số trường hợp (mặc định Java)");
        System.out.println("  - 32KB → tối ưu hơn cho file lớn");
        System.out.println("  - 64KB+ → cải thiện không đáng kể, tốn RAM");
        System.out.println();
        System.out.println("▸ BẪY THƯỜNG GẶP:");
        System.out.println("  ❌ Quên flush() với BufferedOutputStream → mất dữ liệu cuối");
        System.out.println("  ❌ Quên đóng stream → resource leak, file bị lock");
        System.out.println("  ✅ Luôn dùng try-with-resources để tự động đóng stream");
        System.out.println();
    }

    /**
     * Tạo file nhị phân cho test.
     */
    private static void generateBinaryFile(String filePath, int sizeInMB) {
        File file = new File(filePath);
        if (file.exists()) {
            System.out.println("📁 File test đã tồn tại: " + filePath);
            return;
        }

        System.out.println("📁 Đang tạo file test: " + filePath + " (" + sizeInMB + " MB)...");
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            byte[] chunk = new byte[1024 * 1024]; // 1MB
            for (int i = 0; i < chunk.length; i++) {
                chunk[i] = (byte) (i % 256);
            }
            for (int i = 0; i < sizeInMB; i++) {
                fos.write(chunk);
            }
            fos.flush();
        } catch (IOException e) {
            System.err.println("❌ Lỗi tạo file: " + e.getMessage());
        }
        System.out.println("✅ File test đã tạo!\n");
    }

    /**
     * Dọn dẹp file test.
     */
    private static void cleanupTestFiles() {
        System.out.println("🗑️  Dọn dẹp file test...");
        File testDir = new File(TEST_DIR);
        if (testDir.exists()) {
            File[] files = testDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.delete()) {
                        System.out.println("   Đã xóa: " + file.getName());
                    }
                }
            }
            testDir.delete();
        }
        System.out.println("✅ Dọn dẹp xong!");
    }
}
