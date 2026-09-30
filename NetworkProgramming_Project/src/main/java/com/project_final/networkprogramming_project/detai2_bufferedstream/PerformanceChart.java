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

    /**
     * Dưới ngưỡng chênh lệch này (%) thì coi hai phương pháp là như nhau.
     * Đặt 10% vì các phép đo ở nhóm B chỉ vài chục ms, dao động giữa các lần
     * chạy vốn đã cỡ vài phần trăm.
     */
    private static final double NEGLIGIBLE_PERCENT = 10;

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
        System.out.println("╔═══════╦════════════════════════╦════════╦═════════════╦══════════════════╗");
        System.out.println("║ Nhóm  ║      Phương pháp       ║  File  ║ Thời gian   ║ So sánh trong nhóm║");
        System.out.println("╠═══════╬════════════════════════╬════════╬═════════════╬══════════════════╣");

        // Mốc so sánh của nhóm A: bản KHÔNG buffer, tức phần tử đầu tiên
        CopyResult baselineA = results[0];

        for (CopyResult r : results) {
            boolean isGroupA = r.fileSizeMB() == CopyBenchmark.SMALL_FILE_MB;

            // Hai nhóm dùng hai đơn vị khác nhau nên ghi đơn vị thẳng vào ô:
            // nhóm A quá chậm (chưa tới 1 MB/s) nên hiển thị MB/s sẽ làm tròn
            // thành 0 và trông như lỗi; bội số mới là con số có ý nghĩa ở đó.
            String metric = isGroupA
                    ? String.format("%.1fx nhanh hơn", r.speedupOver(baselineA))
                    : String.format("%.0f MB/s", r.throughputMBps());

            System.out.printf("║   %s   ║ %-22s ║ %-6s ║ %8.2f ms ║ %-17s║%n",
                    isGroupA ? "A" : "B", r.methodName(), r.fileSizeMB() + " MB",
                    r.ms(), metric);
        }

        System.out.println("╚═══════╩════════════════════════╩════════╩═════════════╩══════════════════╝");
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
        // Vẽ THÔNG LƯỢNG (MB/s) chứ không phải thời gian: đề bài yêu cầu
        // "in biểu đồ TỐC ĐỘ I/O", mà tốc độ là MB/s còn thời gian là ms.
        System.out.println("📊 BIỂU ĐỒ TỐC ĐỘ I/O — MB/s (càng dài càng nhanh):");
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

        double maxSpeed = 0.001;
        for (CopyResult r : group) {
            maxSpeed = Math.max(maxSpeed, r.throughputMBps());
        }

        for (CopyResult r : group) {
            double speed = r.throughputMBps();
            int barLen = (int) (speed * MAX_BAR_WIDTH / maxSpeed);
            barLen = Math.max(barLen, 1); // luôn thấy được thanh

            // Tốc độ dưới 10 MB/s cần phần thập phân mới phân biệt được
            String speedText = speed < 10
                    ? String.format("%7.1f MB/s", speed)
                    : String.format("%7.0f MB/s", speed);

            System.out.printf("    %-22s │%-40s│ %s (%.2f ms)%n",
                    r.methodName(), String.valueOf(blockChar).repeat(barLen),
                    speedText, r.ms());
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

        // --- Nhóm B: tách thành 2 câu hỏi RIÊNG, vì chúng có độ tin cậy khác nhau ---
        //
        // Thứ tự phần tử do CopyBenchmark.runAll quy định:
        //   [0] Unbuf chunk 8KB   [1] Buffered 8KB   [2..] các cỡ buffer lớn hơn
        if (groupB.length >= 3) {
            CopyResult unbufChunk = groupB[0];
            CopyResult buffered8k = groupB[1];

            // === Câu hỏi 1: bọc buffer lên code VỐN ĐÃ đọc theo khối thì được gì? ===
            // Kết quả này lặp lại ổn định qua nhiều lần chạy nên kết luận được.
            double diffPercent = Math.abs(buffered8k.ms() - unbufChunk.ms())
                    / Math.max(unbufChunk.ms(), 0.001) * 100;

            System.out.println("▸ Bọc BufferedStream lên code ĐÃ đọc theo khối 8KB:");
            System.out.printf("  %-22s %8.2f ms%n", unbufChunk.methodName(), unbufChunk.ms());
            System.out.printf("  %-22s %8.2f ms%n", buffered8k.methodName(), buffered8k.ms());

            // Nhận xét bám theo con số thật, KHÔNG in cứng "không được gì":
            // trên Windows hai giá trị này gần như bằng nhau, nhưng trên ext4
            // đã quan sát thấy chênh tới 19% — nói "không được gì" là sai.
            if (diffPercent < NEGLIGIBLE_PERCENT) {
                System.out.printf("  → Chênh %.0f%% — gần như KHÔNG được gì.%n", diffPercent);
                System.out.println("  Buffer chỉ cắt system call khi code đọc lắt nhắt từng byte.");
                System.out.println("  Đã tự đọc khối 8KB rồi thì nó chỉ thêm một lần copy thừa.");
            } else {
                String faster = buffered8k.ms() < unbufChunk.ms()
                        ? "CÓ buffer nhanh hơn" : "KHÔNG buffer nhanh hơn";
                System.out.printf("  → Chênh %.0f%% (%s).%n", diffPercent, faster);
                System.out.println("  Vẫn nhỏ hơn hẳn mức chênh của nhóm A, vì ở đây buffer không");
                System.out.println("  còn cắt được system call nữa — code đã tự đọc theo khối rồi.");
            }
            System.out.println();

            // === Câu hỏi 2: tăng buffer lên TRÊN 8KB có lợi không? ===
            CopyResult bestLarge = groupB[2];
            for (int i = 3; i < groupB.length; i++) {
                if (groupB[i].ms() < bestLarge.ms()) {
                    bestLarge = groupB[i];
                }
            }

            double largeVs8k = bestLarge.speedupOver(buffered8k);

            System.out.println("▸ Tăng cỡ buffer lên trên 8KB:");
            System.out.printf("  Nhanh nhất trong các cỡ lớn: %-16s %8.2f ms%n",
                    bestLarge.methodName(), bestLarge.ms());

            // Hệ số < 1 nghĩa là CHẬM hơn. In "nhanh hơn 0.9 lần" là vô nghĩa,
            // nên phải đổi cách diễn đạt theo dấu của kết quả.
            if (largeVs8k >= 1) {
                System.out.printf("  → Nhanh hơn Buffered 8KB %.1f lần.%n", largeVs8k);
            } else {
                System.out.printf("  → Lần chạy này KHÔNG nhanh hơn: chậm hơn Buffered 8KB %.1f lần.%n",
                        1 / largeVs8k);
                System.out.println("  Tức là ở máy này, tăng cỡ buffer quá 8KB không còn lợi.");
            }
            System.out.println();

            // === Cảnh báo: phần KHÔNG kết luận được ===
            // Các cỡ buffer lớn chỉ chênh nhau vài ms nên thứ tự giữa chúng đảo
            // qua đảo lại giữa các lần chạy. Đã quan sát thấy trên cùng một máy
            // Windows: 2 lần đầu 64KB nhanh nhất, lần thứ 3 lại là 32KB.
            System.out.println("⚠️  KHÔNG kết luận được cỡ buffer nào TỐI ƯU từ một lần chạy:");
            System.out.println("    các cỡ lớn chỉ chênh nhau vài ms nên thứ tự giữa 32KB và 64KB");
            System.out.println("    đảo qua đảo lại giữa các lần chạy, ngay trên cùng một máy.");
            System.out.println("    Muốn chọn cỡ tối ưu thì phải đo nhiều lần trên môi trường thật.");
            System.out.println();
        }

        System.out.println("▸ KHUYẾN NGHỊ SỬ DỤNG:");
        System.out.println("  - Đọc/ghi lắt nhắt từng byte → BẮT BUỘC bọc Buffered, lợi hơn 100 lần");
        System.out.println("  - Đã tự đọc theo khối ≥ 8KB  → lợi ích của Buffered giảm mạnh");
        System.out.println("  - Cần tối ưu thêm            → dùng khối lớn hơn 8KB, nhưng phải ĐO");
        System.out.println("    để biết cỡ nào hợp với máy mình, đừng tin một con số cố định");
        System.out.println("  - Luôn dùng try-with-resources để stream tự đóng và tự flush()");
        System.out.println();
    }
}
