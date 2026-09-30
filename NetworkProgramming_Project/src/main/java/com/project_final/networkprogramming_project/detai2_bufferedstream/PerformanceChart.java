package com.project_final.networkprogramming_project.detai2_bufferedstream;

import com.project_final.networkprogramming_project.detai1_bytecharstream.TestFileUtils;
import java.io.IOException;

/**
 * Đề tài 2: Ứng dụng kỹ thuật luồng đệm (Buffered Stream) — bản console.
 * =======================================================================
 *
 * Đáp ứng đủ 3 phần đề bài yêu cầu:
 *   - Lý thuyết : cơ chế buffer và chi phí system call ({@link #printTheory()})
 *   - Demo      : hai chương trình sao chép tệp lớn có/không BufferedInputStream
 *   - Kết quả   : so sánh thời gian xử lý + in biểu đồ tốc độ I/O
 *
 * Phép đo chia làm HAI NHÓM và KHÔNG so chéo giữa hai nhóm — xem
 * {@link CopyBenchmark} để biết lý do.
 *
 * @author Cao Duy Quốc Khánh
 */
public class PerformanceChart {

    /** Thư mục lưu file test (đã có trong .gitignore). */
    private static final String TEST_DIR = TestFileUtils.getTestDir();

    /** Độ rộng tối đa của thanh bar khi vẽ biểu đồ console. */
    private static final int MAX_BAR_WIDTH = 40;

    public static void run() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  ĐỀ TÀI 2: SO SÁNH BUFFERED vs UNBUFFERED STREAM            ║");
        System.out.println("║  Người thực hiện: Cao Duy Quốc Khánh                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        printTheory();

        try {
            System.out.println("━".repeat(62));
            System.out.println("🔬 CHẠY BENCHMARK (warm-up + lấy trung vị)");
            System.out.println("━".repeat(62));

            CopyResult[] results = CopyBenchmark.runAll(TEST_DIR, System.out::println);

            printResultTable(results);
            printBarChart(results);
            printConclusion(results);

        } catch (IOException e) {
            // Không nuốt lỗi: in rõ nguyên nhân để biết vì sao không có số liệu
            System.err.println("❌ Không thể hoàn tất benchmark: " + e.getMessage());
        } finally {
            // finally: dọn file test dù thành công hay thất bại
            TestFileUtils.cleanupTestFiles();
            System.out.println("🗑️  Đã dọn dẹp file test.");
        }
    }

    /**
     * In phần lý thuyết về Buffered Stream và chi phí system call.
     */
    private static void printTheory() {
        System.out.println("📖 LÝ THUYẾT & CƠ CHẾ HOẠT ĐỘNG:");
        System.out.println("─".repeat(62));
        System.out.println("▸ Vấn đề của Unbuffered Stream:");
        System.out.println("  - Mỗi read()/write() = 1 system call đến OS Kernel");
        System.out.println("  - Mỗi system call phải chuyển User Mode → Kernel Mode (tốn CPU)");
        System.out.println("  - File 10MB đọc từng byte = hơn 10 TRIỆU system call!");
        System.out.println();
        System.out.println("▸ Giải pháp: BufferedStream");
        System.out.println("  - Tạo vùng đệm trong RAM, mặc định 8192 byte (8KB)");
        System.out.println("  - 1 system call nạp nguyên 8KB vào buffer");
        System.out.println("  - Các lần read() sau lấy từ buffer → không tốn system call");
        System.out.println("  - File 10MB chỉ còn khoảng 1.280 system call (giảm 8192 lần)");
        System.out.println();
        System.out.println("▸ Sơ đồ so sánh:");
        System.out.println();
        System.out.println("  KHÔNG buffer:");
        System.out.println("    Ứng dụng ──read()──> [system call] ──> Kernel ──> Đĩa   (mỗi byte)");
        System.out.println();
        System.out.println("  CÓ buffer:");
        System.out.println("    Ứng dụng ──read()──> Buffer (RAM)                       (mỗi byte)");
        System.out.println("                            ↑");
        System.out.println("                    [system call] ──> Kernel ──> Đĩa        (mỗi 8KB)");
        System.out.println();
    }

    /**
     * In bảng kết quả, tách rõ 2 nhóm.
     */
    private static void printResultTable(CopyResult[] results) {
        System.out.println();
        System.out.println("╔═══════╦════════════════════════╦════════╦═════════════╦═════════════╗");
        System.out.println("║ Nhóm  ║      Phương pháp       ║  File  ║ Thời gian   ║ Thông lượng ║");
        System.out.println("╠═══════╬════════════════════════╬════════╬═════════════╬═════════════╣");

        for (CopyResult r : results) {
            String group = r.fileSizeMB() == CopyBenchmark.SMALL_FILE_MB ? "A" : "B";
            System.out.printf("║   %s   ║ %-22s ║ %-6s ║ %8.2f ms ║ %7.0f MB/s ║%n",
                    group, r.methodName(), r.fileSizeMB() + " MB",
                    r.ms(), r.throughputMBps());
        }

        System.out.println("╚═══════╩════════════════════════╩════════╩═════════════╩═════════════╝");
        System.out.println("⚠️  CHỈ so sánh các dòng TRONG CÙNG một nhóm: nhóm A chạy trên file "
                + CopyBenchmark.SMALL_FILE_MB + " MB,");
        System.out.println("    nhóm B chạy trên file " + CopyBenchmark.LARGE_FILE_MB
                + " MB nên số liệu 2 nhóm không so trực tiếp được.");
        System.out.println();
    }

    /**
     * Vẽ biểu đồ tốc độ I/O trên console, mỗi nhóm một thang đo riêng.
     */
    private static void printBarChart(CopyResult[] results) {
        System.out.println("📊 BIỂU ĐỒ TỐC ĐỘ I/O (càng ngắn càng nhanh):");
        System.out.println("─".repeat(62));

        printChartGroup("NHÓM A — đọc/ghi từng byte (file "
                        + CopyBenchmark.SMALL_FILE_MB + " MB)",
                CopyBenchmark.filterBySize(results, CopyBenchmark.SMALL_FILE_MB), '█');

        printChartGroup("NHÓM B — đọc theo khối (file "
                        + CopyBenchmark.LARGE_FILE_MB + " MB)",
                CopyBenchmark.filterBySize(results, CopyBenchmark.LARGE_FILE_MB), '▓');

        System.out.println();
    }

    /**
     * Vẽ một nhóm với thang đo riêng.
     *
     * Thang riêng từng nhóm là bắt buộc: nếu dùng chung thang, cột 3000 ms của
     * nhóm A sẽ nén toàn bộ nhóm B (7-26 ms) thành những vạch không nhìn ra gì.
     */
    private static void printChartGroup(String title, CopyResult[] group, char blockChar) {
        System.out.println();
        System.out.println("  " + title);

        double maxMs = 1;
        for (CopyResult r : group) {
            maxMs = Math.max(maxMs, r.ms());
        }

        for (CopyResult r : group) {
            int barLen = (int) (r.ms() * MAX_BAR_WIDTH / maxMs);
            barLen = Math.max(barLen, 1); // luôn thấy được thanh
            System.out.printf("    %-22s │%-40s│ %9.2f ms%n",
                    r.methodName(), String.valueOf(blockChar).repeat(barLen), r.ms());
        }
    }

    /**
     * In kết luận dựa trên số liệu đo được.
     */
    private static void printConclusion(CopyResult[] results) {
        System.out.println("📝 KẾT LUẬN (dựa trên số liệu vừa đo):");
        System.out.println("─".repeat(62));

        CopyResult[] groupA = CopyBenchmark.filterBySize(results, CopyBenchmark.SMALL_FILE_MB);
        CopyResult[] groupB = CopyBenchmark.filterBySize(results, CopyBenchmark.LARGE_FILE_MB);

        // --- Nhóm A: buffer cứu được code đọc lắt nhắt bao nhiêu? ---
        if (groupA.length >= 2) {
            CopyResult unbuffered = groupA[0];
            CopyResult buffered = groupA[1];
            System.out.printf("▸ Khi đọc/ghi TỪNG BYTE: BufferedStream nhanh hơn %.1f lần%n",
                    buffered.speedupOver(unbuffered));
            System.out.println("  → Đây là giá trị thật sự của buffer: cắt bỏ hàng triệu system call.");
            System.out.println();
        }

        // --- Nhóm B: khi đã đọc theo khối thì cỡ buffer còn quan trọng không? ---
        if (groupB.length >= 2) {
            CopyResult fastest = groupB[0];
            CopyResult slowest = groupB[0];
            for (CopyResult r : groupB) {
                if (r.ms() < fastest.ms()) {
                    fastest = r;
                }
                if (r.ms() > slowest.ms()) {
                    slowest = r;
                }
            }

            System.out.println("▸ Khi đã đọc THEO KHỐI, chênh lệch giữa các cỡ buffer:");
            System.out.printf("  Nhanh nhất: %-22s %.2f ms%n", fastest.methodName(), fastest.ms());
            System.out.printf("  Chậm nhất : %-22s %.2f ms%n", slowest.methodName(), slowest.ms());
            System.out.printf("  → Chênh %.1f lần — nhỏ hơn hẳn so với nhóm A.%n",
                    fastest.speedupOver(slowest));
            System.out.println("  Lý do: khi buffer đã đủ lớn, nút thắt chuyển từ số system call");
            System.out.println("  sang băng thông của đĩa, nên tăng buffer thêm không lợi bao nhiêu.");
            System.out.println();
        }

        System.out.println("▸ KHUYẾN NGHỊ SỬ DỤNG:");
        System.out.println("  - Luôn bọc BufferedInputStream/BufferedOutputStream khi đọc/ghi lắt nhắt");
        System.out.println("  - Nếu đã tự đọc theo khối ≥ 8KB thì buffer không còn quan trọng");
        System.out.println("  - Luôn dùng try-with-resources để stream tự đóng và tự flush()");
        System.out.println();
    }
}
