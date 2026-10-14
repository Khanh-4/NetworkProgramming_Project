package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Đề tài 1 & 2: Công cụ dùng chung cho việc đo — TẠO FILE TEST và ĐO THỜI GIAN.
 * ==============================================================================
 *
 * PHẦN 1 — ĐO THỜI GIAN CÓ WARM-UP VÀ LẤY TRUNG VỊ ({@link #medianNanos})
 * ----------------------------------------------------------------------
 * Một phép đo I/O đơn lẻ bị nhiễu bởi ít nhất 3 thứ nằm ngoài code:
 *
 *  1) OS PAGE CACHE — lần đọc đầu phải chờ đĩa, lần sau lấy từ RAM:
 *
 *       Lần 1: App → read() → Kernel → [cache miss] → ĐĨA   (chậm)
 *       Lần 2: App → read() → Kernel → [cache hit]  → RAM   (nhanh)
 *
 *  2) JIT COMPILER — vòng lặp đọc chạy thông dịch lúc đầu, chỉ sau vài nghìn
 *     vòng mới được biên dịch sang mã máy.
 *
 *  3) HỆ ĐIỀU HÀNH — antivirus quét file, tiến trình khác giành CPU/đĩa.
 *
 * CÁCH XỬ LÝ: chạy vài lần WARM-UP rồi bỏ kết quả, sau đó đo nhiều lần và lấy
 * TRUNG VỊ (median). Dùng trung vị chứ không dùng trung bình, vì chỉ cần MỘT
 * lần đo bị antivirus chen ngang là trung bình đã lệch hẳn, còn trung vị thì không.
 *
 * Tác vụ nặng (ví dụ copy từng byte file 1MB mất ~3 giây) thì nhiễu vài chục ms
 * là không đáng kể, nên nếu lần chạy thử vượt 1 giây, bộ đo tự rút gọn thành
 * 1 warm-up + 1 lần đo.
 *
 * PHẦN 2 — TẠO VÀ DỌN FILE TEST (thư mục {@value #TEST_DIR})
 * ---------------------------------------------------------
 * File text UTF-8 có dấu tiếng Việt và file nhị phân 0x00..0xFF; file đúng kích
 * thước đã có sẵn thì dùng lại, không tạo lại.
 *
 * @author Cao Duy Quốc Khánh
 */
public final class BenchmarkUtils {

    // ================================================================
    // PHẦN 1: ĐO THỜI GIAN
    // ================================================================

    /** Số lần chạy nháp cho tác vụ nhanh (kết quả bị bỏ). */
    private static final int WARMUP_RUNS = 3;

    /** Số lần đo thật cho tác vụ nhanh. Số lẻ để trung vị rơi đúng 1 phần tử. */
    private static final int MEASURED_RUNS = 5;

    /** Ngưỡng coi là "tác vụ nặng" = 1 giây; trên ngưỡng này không cần lặp. */
    private static final long SLOW_TASK_THRESHOLD_NS = 1_000_000_000L;

    // ================================================================
    // PHẦN 2: FILE TEST
    // ================================================================

    private static final String TEST_DIR = "testdata";

    /** 1 MB = 1024 * 1024 byte. Đặt tên hằng để tránh magic number rải rác. */
    private static final int BYTES_PER_MB = 1024 * 1024;

    private BenchmarkUtils() {
    }

    /** Một tác vụ I/O cần đo; được phép ném IOException. */
    public interface TimedTask {
        void execute() throws IOException;
    }

    /**
     * Đo tác vụ và trả về thời gian TRUNG VỊ tính bằng nanosecond.
     *
     * Trả về nanosecond chứ không phải millisecond vì các thao tác đọc file nhỏ
     * từ page cache có thể mất dưới 1 ms — làm tròn về ms sẽ ra 0 và mọi so sánh
     * đều vô nghĩa.
     */
    public static long medianNanos(TimedTask task) throws IOException {
        // Lần chạy thử vừa đóng vai warm-up, vừa để ước lượng tác vụ nặng hay nhẹ
        long probe = timeOnce(task);

        if (probe > SLOW_TASK_THRESHOLD_NS) {
            // Tác vụ nặng: đã warm-up 1 lần ở trên, đo thêm 1 lần rồi trả về luôn
            return timeOnce(task);
        }

        // Tác vụ nhẹ: warm-up thêm cho đủ rồi đo nhiều lần
        for (int i = 1; i < WARMUP_RUNS; i++) {
            task.execute();
        }

        long[] samples = new long[MEASURED_RUNS];
        for (int i = 0; i < MEASURED_RUNS; i++) {
            samples[i] = timeOnce(task);
        }
        return median(samples);
    }

    /**
     * Lấy trung vị của mảng mẫu.
     * clone() trước khi sort để KHÔNG sửa mảng của bên gọi (nguyên tắc immutability).
     */
    private static long median(long[] samples) {
        long[] sorted = samples.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    /** Đo một lần thực thi, trả về nanosecond. */
    private static long timeOnce(TimedTask task) throws IOException {
        // nanoTime() là đồng hồ đơn điệu, dùng để đo KHOẢNG thời gian.
        // KHÔNG dùng currentTimeMillis() vì nó có thể bị NTP chỉnh giật lùi.
        long start = System.nanoTime();
        task.execute();
        return System.nanoTime() - start;
    }

    /** Đổi nanosecond sang millisecond, giữ phần thập phân. */
    public static double toMs(long nanos) {
        return nanos / 1_000_000.0;
    }

    /** Định dạng nanosecond thành chuỗi "x.xx ms" để in log. */
    public static String formatMs(long nanos) {
        return String.format("%.2f ms", toMs(nanos));
    }

    public static String getTestDir() {
        return TEST_DIR;
    }

    /** Tạo thư mục test nếu chưa tồn tại. */
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
     *   Giá trị đó phụ thuộc phiên bản JDK và OS (xem StreamDemos#encodingReport),
     *   nên cùng một đoạn code có thể tạo ra file với số byte khác nhau
     *   trên 2 máy => benchmark không tái lập được.
     *
     * Nội dung CÓ DẤU là cố ý: ký tự có dấu chiếm 2-3 byte trong UTF-8,
     * nhờ đó CharStream phải decode thật sự thay vì đi đường nhanh ASCII.
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
     * Chỉ kiểm tra exists() là không đủ: file sót lại từ lần chạy bị ngắt giữa
     * chừng sẽ sai kích thước và benchmark âm thầm đo trên file sai.
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

    /** Dọn dẹp toàn bộ file test và thư mục. */
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
