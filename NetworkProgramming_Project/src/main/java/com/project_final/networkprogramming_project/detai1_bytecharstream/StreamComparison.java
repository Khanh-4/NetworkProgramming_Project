package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.io.IOException;

/**
 * Đề tài 1: Luồng byte và luồng ký tự trong Java — bản chạy trên console.
 * ========================================================================
 *
 * Đáp ứng đủ 3 phần đề bài yêu cầu:
 *   - Lý thuyết : cơ chế hoạt động ở tầng OS (xem {@link #printTheory()})
 *   - Demo      : đọc/ghi tệp .txt bằng FileInputStream và FileReader
 *   - Kết quả   : so sánh tốc độ đọc file NHỊ PHÂN vs KÝ TỰ + log thời gian
 *
 * Phép so sánh là ma trận 2x2, vì chỉ so 2 loại stream trên 1 loại file
 * thì không trả lời được câu hỏi của đề bài:
 *
 *                    │ File TEXT (.txt)  │ File NHỊ PHÂN (.bin)
 *   ─────────────────┼───────────────────┼──────────────────────
 *   ByteStream       │       đo          │        đo
 *   CharStream       │       đo          │  đo + kiểm tra hỏng dữ liệu
 *
 * @author Cao Duy Quốc Khánh
 */
public class StreamComparison {

    /** Các kích thước file test (MB). */
    private static final int[] TEST_SIZES_MB = {1, 5, 10, 20};

    /** Kích thước file dùng cho phần DEMO đọc/ghi cơ bản (nhỏ cho nhanh). */
    private static final int DEMO_SIZE_MB = 2;

    /** Thư mục lưu file test (đã có trong .gitignore). */
    private static final String TEST_DIR = TestFileUtils.getTestDir();

    /** Độ rộng tối đa của thanh bar khi vẽ biểu đồ console. */
    private static final int MAX_BAR_WIDTH = 34;

    public static void run() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║     ĐỀ TÀI 1: SO SÁNH BYTE STREAM vs CHAR STREAM            ║");
        System.out.println("║     Người thực hiện: Cao Duy Quốc Khánh                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        printTheory();

        try {
            TestFileUtils.ensureTestDir();

            runBasicDemo();
            BenchmarkResult[] results = runBenchmark();

            printResultTable(results);
            printBarChart(results);
            printConclusion(results);

            // Phần bẫy encoding: vì sao CharStream "tự xử lý encoding" vẫn sai
            System.out.println();
            EncodingDemo.run();

        } catch (IOException e) {
            // Không nuốt lỗi: in rõ nguyên nhân để biết vì sao không có số liệu
            System.err.println("❌ Không thể hoàn tất benchmark: " + e.getMessage());
        } finally {
            // finally: dọn file test dù thành công hay thất bại, tránh để rác vài chục MB
            TestFileUtils.cleanupTestFiles();
            System.out.println("🗑️  Đã dọn dẹp file test.");
        }
    }

    /**
     * PHẦN DEMO theo đúng đề bài: đọc/ghi tệp .txt bằng FileInputStream và FileReader.
     * In ra số byte, số ký tự và thời gian của từng cách đọc.
     */
    private static void runBasicDemo() throws IOException {
        System.out.println("━".repeat(62));
        System.out.println("🔧 PHẦN DEMO: ĐỌC TỆP .txt BẰNG FileInputStream & FileReader");
        System.out.println("━".repeat(62));

        String demoFile = TEST_DIR + "/demo_" + DEMO_SIZE_MB + "MB.txt";
        TestFileUtils.generateTextFile(demoFile, DEMO_SIZE_MB);

        ByteStreamDemo.run(demoFile);
        CharStreamDemo.run(demoFile);

        System.out.println("💡 Lưu ý: số BYTE và số KÝ TỰ khác nhau trên cùng một file,");
        System.out.println("   vì ký tự tiếng Việt có dấu chiếm 2-3 byte khi encode UTF-8.");
        System.out.println();
    }

    /**
     * PHẦN KẾT QUẢ: chạy benchmark 2x2 cho từng kích thước file.
     */
    private static BenchmarkResult[] runBenchmark() throws IOException {
        System.out.println("━".repeat(62));
        System.out.println("📊 PHẦN KẾT QUẢ: BENCHMARK (warm-up + lấy trung vị)");
        System.out.println("━".repeat(62));

        BenchmarkResult[] results = new BenchmarkResult[TEST_SIZES_MB.length];

        for (int i = 0; i < TEST_SIZES_MB.length; i++) {
            int sizeMB = TEST_SIZES_MB[i];
            String textFile = TEST_DIR + "/test_text_" + sizeMB + "MB.txt";
            String binaryFile = TEST_DIR + "/test_binary_" + sizeMB + "MB.bin";

            System.out.println("\n▶ Chuẩn bị file test " + sizeMB + " MB (text + binary)...");
            TestFileUtils.generateTextFile(textFile, sizeMB);
            TestFileUtils.generateBinaryFile(binaryFile, sizeMB);

            // Truyền System.out::println làm listener để log tiến độ ra console
            results[i] = StreamBenchmark.measure(
                    sizeMB, textFile, binaryFile, System.out::println);
        }

        System.out.println();
        return results;
    }

    /**
     * In phần giải thích lý thuyết và cơ chế ở tầng OS.
     */
    private static void printTheory() {
        System.out.println("📖 LÝ THUYẾT & CƠ CHẾ HOẠT ĐỘNG:");
        System.out.println("─".repeat(62));
        System.out.println("▸ ByteStream (FileInputStream / FileOutputStream):");
        System.out.println("  - Đơn vị: BYTE (8-bit). Lớp gốc: InputStream / OutputStream");
        System.out.println("  - Trả về ĐÚNG byte có trong file, không diễn giải nội dung");
        System.out.println("  - Phù hợp: file nhị phân (ảnh, video, .exe, .zip)");
        System.out.println();
        System.out.println("▸ CharStream (FileReader / FileWriter):");
        System.out.println("  - Đơn vị: KÝ TỰ (char, 16-bit UTF-16). Lớp gốc: Reader / Writer");
        System.out.println("  - PHẢI diễn giải byte thành ký tự thông qua một charset");
        System.out.println("  - Phù hợp: file text (.txt, .csv, .json, .java)");
        System.out.println();
        System.out.println("▸ Luồng dữ liệu trong OS — đây là gốc của chênh lệch tốc độ:");
        System.out.println();
        System.out.println("  ByteStream:");
        System.out.println("    Đĩa → [page cache] → byte[] → Ứng dụng");
        System.out.println();
        System.out.println("  CharStream:");
        System.out.println("    Đĩa → [page cache] → byte[] → CharsetDecoder → char[] → Ứng dụng");
        System.out.println("                                  └── bước THÊM: tốn CPU");
        System.out.println();
        System.out.println("  → CharStream luôn có thêm 1 bước DECODE, nên chậm hơn.");
        System.out.println("    Với file nhị phân, bước decode này còn LÀM HỎNG dữ liệu.");
        System.out.println();
    }

    /**
     * In bảng kết quả dạng ma trận 2x2 (stream x loại file).
     */
    private static void printResultTable(BenchmarkResult[] results) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                  BẢNG KẾT QUẢ (đơn vị: ms, trung vị)                ║");
        System.out.println("╠════════╦═══════════════════════╦═══════════════════════╦═════════════╣");
        System.out.println("║        ║      FILE TEXT        ║    FILE NHỊ PHÂN      ║ CharStream  ║");
        System.out.println("║  Size  ╠═══════════╦═══════════╬═══════════╦═══════════╣ hỏng dữ     ║");
        System.out.println("║        ║   Byte    ║   Char    ║   Byte    ║   Char    ║ liệu?       ║");
        System.out.println("╠════════╬═══════════╬═══════════╬═══════════╬═══════════╬═════════════╣");

        for (BenchmarkResult r : results) {
            String corruption = r.isBinaryCorrupted()
                    ? String.format("%.1f%% hỏng", r.binaryCorruptionPercent())
                    : "không";

            System.out.printf("║ %-6s ║ %9.2f ║ %9.2f ║ %9.2f ║ %9.2f ║ %-11s ║%n",
                    r.sizeMB() + " MB",
                    r.byteOnTextMs(), r.charOnTextMs(),
                    r.byteOnBinaryMs(), r.charOnBinaryMs(),
                    corruption);
        }

        System.out.println("╚════════╩═══════════╩═══════════╩═══════════╩═══════════╩═════════════╝");
        System.out.println();
    }

    /**
     * Vẽ biểu đồ bar chart trên console, 4 thanh cho mỗi kích thước file.
     */
    private static void printBarChart(BenchmarkResult[] results) {
        System.out.println("📊 BIỂU ĐỒ SO SÁNH THỜI GIAN ĐỌC (ms):");
        System.out.println("─".repeat(62));

        // Tìm giá trị lớn nhất trên TOÀN BỘ số liệu để mọi thanh cùng một tỉ lệ.
        // Nếu scale riêng từng nhóm thì không so sánh được giữa các nhóm.
        double maxMs = 1;
        for (BenchmarkResult r : results) {
            maxMs = Math.max(maxMs, Math.max(r.byteOnTextMs(), r.charOnTextMs()));
            maxMs = Math.max(maxMs, Math.max(r.byteOnBinaryMs(), r.charOnBinaryMs()));
        }

        for (BenchmarkResult r : results) {
            System.out.println("  " + r.sizeMB() + " MB:");
            printBar("TEXT   Byte", r.byteOnTextMs(), maxMs, '█');
            printBar("TEXT   Char", r.charOnTextMs(), maxMs, '▓');
            printBar("BINARY Byte", r.byteOnBinaryMs(), maxMs, '▒');
            printBar("BINARY Char", r.charOnBinaryMs(), maxMs, '░');
            System.out.println();
        }

        System.out.println("  Chú thích: █ Byte/text  ▓ Char/text  ▒ Byte/binary  ░ Char/binary");
        System.out.println();
    }

    /** In một thanh bar kèm nhãn và giá trị. */
    private static void printBar(String label, double valueMs, double maxMs, char blockChar) {
        int barLen = (int) (valueMs * MAX_BAR_WIDTH / maxMs);
        barLen = Math.max(barLen, 1); // luôn thấy được thanh, kể cả giá trị rất nhỏ
        System.out.printf("    %-11s ║%-34s║ %7.2f ms%n",
                label, String.valueOf(blockChar).repeat(barLen), valueMs);
    }

    /**
     * In kết luận dựa trên số liệu đo được, không nói suông.
     */
    private static void printConclusion(BenchmarkResult[] results) {
        System.out.println("📝 KẾT LUẬN (dựa trên số liệu vừa đo):");
        System.out.println("─".repeat(62));

        // Lấy trung bình hệ số chậm để phát biểu có căn cứ
        double textFactorSum = 0;
        double binaryFactorSum = 0;
        int corruptedCount = 0;

        for (BenchmarkResult r : results) {
            textFactorSum += r.textSlowdownFactor();
            binaryFactorSum += r.binarySlowdownFactor();
            if (r.isBinaryCorrupted()) {
                corruptedCount++;
            }
        }

        double avgTextFactor = textFactorSum / results.length;
        double avgBinaryFactor = binaryFactorSum / results.length;

        System.out.printf("▸ Trên file TEXT      : CharStream chậm hơn ByteStream %.1f lần%n",
                avgTextFactor);
        System.out.printf("▸ Trên file NHỊ PHÂN  : CharStream chậm hơn ByteStream %.1f lần%n",
                avgBinaryFactor);
        System.out.println("  → Nguyên nhân: CharStream phải chạy CharsetDecoder cho mọi byte.");
        System.out.println();

        System.out.println("▸ VỀ TÍNH ĐÚNG ĐẮN — quan trọng hơn cả tốc độ:");
        if (corruptedCount > 0) {
            System.out.println("  ⚠️  CharStream làm HỎNG dữ liệu ở " + corruptedCount + "/"
                    + results.length + " file nhị phân đã test.");
            System.out.println("  Decoder thay byte không hợp lệ bằng U+FFFD và KHÔNG ném exception");
            System.out.println("  → chương trình chạy bình thường nhưng dữ liệu đã sai, rất khó phát hiện.");
        } else {
            System.out.println("  Không phát hiện hỏng dữ liệu trong lần chạy này.");
        }
        System.out.println();

        System.out.println("▸ KHUYẾN NGHỊ SỬ DỤNG:");
        System.out.println("  - File nhị phân (ảnh, video, .zip, .exe) → BẮT BUỘC ByteStream");
        System.out.println("  - File text (.txt, .csv, .java)          → CharStream + charset tường minh");
        System.out.println("  - Cần tối ưu tốc độ                      → bọc thêm Buffered (Đề tài 2)");
        System.out.println();
    }
}
