package com.project_final.networkprogramming_project.detai2_bufferedstream;

import com.project_final.networkprogramming_project.detai1_bytecharstream.BenchmarkUtils;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Đề tài 2: Engine đo tốc độ sao chép — dùng cho GUI.
 * =====================================================================
 *
 * VÌ SAO PHẢI VIẾT LẠI PHẦN ĐO?
 * ------------------------------
 * Bản trước chạy MỖI phương pháp ĐÚNG MỘT LẦN và đo bằng long millisecond.
 * Với nhóm phương pháp có buffer (chỉ mất 7-11 ms) thì:
 *   - sai số làm tròn ±1 ms đã là ±14%
 *   - lần chạy đầu còn gánh cả cache lạnh lẫn JIT chưa biên dịch
 * → Kết quả đo được (8KB: 11 ms, 32KB: 11 ms, 64KB: 7 ms) nằm trọn trong vùng
 *   nhiễu, không đủ căn cứ để kết luận buffer nào nhanh hơn buffer nào.
 *
 * Nay mọi phương pháp đều đo qua {@link BenchmarkUtils}: có warm-up, lặp nhiều lần,
 * lấy trung vị, và trả về nanosecond.
 *
 * HAI NHÓM PHÉP ĐO — KHÔNG ĐƯỢC SO CHÉO NHAU:
 *
 *   Nhóm A (file nhỏ)  : byte-by-byte có/không buffer
 *       → trả lời: "buffer giúp được bao nhiêu khi code đọc lắt nhắt?"
 *
 *   Nhóm B (file lớn)  : đọc theo khối, khảo sát các cỡ buffer
 *       → trả lời: "khi đã đọc theo khối rồi thì cỡ buffer còn quan trọng không?"
 *
 * Byte-by-byte phải dùng file nhỏ vì với file 10MB nó mất hàng chục giây.
 * Chính vì 2 nhóm chạy trên 2 kích thước file khác nhau nên biểu đồ BẮT BUỘC
 * phải tách nhóm, nếu không sẽ so sánh nhầm (xem {@link HBarChartPanel}).
 *
 * @author Cao Duy Quốc Khánh
 */
public final class CopyBenchmark {

    /**
     * Kích thước file cho nhóm A (MB).
     *
     * Phải để nhỏ vì đọc/ghi từng byte cực chậm: 1 MB đã mất khoảng 2.6 giây,
     * nếu dùng 100 MB như nhóm B thì riêng phép đo này mất hơn 4 phút.
     * Đây là ràng buộc kỹ thuật, không phải lười — chính sự chậm đó là thứ
     * đề tài muốn chứng minh.
     */
    public static final int SMALL_FILE_MB = 1;

    /**
     * Kích thước file cho nhóm B (MB) — đây là "tệp lớn" mà đề bài yêu cầu.
     *
     * 100 MB đủ lớn để thoát khỏi vùng nhiễu của phép đo mà vẫn chỉ mất khoảng
     * 0.1 giây mỗi lần sao chép, nên chạy warm-up nhiều lần vẫn nhanh.
     */
    public static final int LARGE_FILE_MB = 100;

    /** Các cỡ buffer đem ra khảo sát ở nhóm B. */
    private static final int[] BUFFER_SIZES = {8192, 32768, 65536};

    private CopyBenchmark() {
    }

    /**
     * Kết quả đo của MỘT phương pháp sao chép.
     *
     * Dùng {@code record} nên mọi field đều final → object BẤT BIẾN (immutable).
     * Thời gian lưu bằng NANOSECOND: nhóm có buffer chỉ mất vài ms, lưu long ms
     * thì sai số làm tròn ±1 ms đã là ±14% — không đủ để so các cỡ buffer.
     *
     * Trường {@code fileSizeMB} là bắt buộc vì hai nhóm chạy trên hai kích thước
     * file khác nhau; thiếu nó thì biểu đồ sẽ vẽ chung một trục và so sánh nhầm.
     *
     * @param methodName tên phương pháp, hiển thị trên bảng và biểu đồ
     * @param fileSizeMB kích thước file đã dùng để đo (MB)
     * @param nanos      thời gian trung vị (nanosecond)
     */
    public record Result(String methodName, int fileSizeMB, long nanos) {

        public double ms() {
            return BenchmarkUtils.toMs(nanos);
        }

        /**
         * Thông lượng tính theo MB/s, trên lượng dữ liệu nguồn.
         * Thực tế đĩa phải chịu gấp đôi vì vừa đọc vừa ghi.
         */
        public double throughputMBps() {
            return nanos <= 0 ? 0 : fileSizeMB / (nanos / 1_000_000_000.0);
        }

        /** Nhanh hơn kết quả tham chiếu bao nhiêu lần. */
        public double speedupOver(Result baseline) {
            return nanos <= 0 ? 0 : (double) baseline.nanos() / nanos;
        }
    }

    /**
     * Chạy toàn bộ phép đo và trả về kết quả theo đúng thứ tự hiển thị.
     *
     * @param testDir thư mục chứa file test
     * @param log     nơi nhận log tiến độ (có thể null)
     * @return mảng kết quả: 2 phần tử nhóm A rồi 4 phần tử nhóm B
     * @throws IOException nếu tạo/đọc/ghi file thất bại
     */
    public static Result[] runAll(String testDir, Consumer<String> log) throws IOException {
        BenchmarkUtils.ensureTestDir();

        String srcSmall = testDir + "/source_" + SMALL_FILE_MB + "MB.dat";
        String srcLarge = testDir + "/source_" + LARGE_FILE_MB + "MB.dat";

        report(log, "Tao file test " + SMALL_FILE_MB + "MB va " + LARGE_FILE_MB + "MB...");
        BenchmarkUtils.generateBinaryFile(srcSmall, SMALL_FILE_MB);
        BenchmarkUtils.generateBinaryFile(srcLarge, LARGE_FILE_MB);

        Result[] results = new Result[3 + BUFFER_SIZES.length];
        int idx = 0;

        // ===== NHÓM A: đọc/ghi từng byte, file nhỏ =====
        report(log, "\n--- Nhom A: doc/ghi tung byte (file " + SMALL_FILE_MB + "MB) ---");

        // Mọi phương pháp trong cùng một nhóm ghi vào CHUNG một file đích.
        // Chúng chạy tuần tự nên không đụng nhau, mà lại tiết kiệm đáng kể dung
        // lượng đĩa: nếu mỗi phương pháp một file đích riêng thì nhóm B cần tới
        // 4 x 100 MB, nay chỉ còn 100 MB.
        String destSmall = testDir + "/copy_small.dat";
        String destLarge = testDir + "/copy_large.dat";

        results[idx++] = measure("Unbuf byte-by-byte", SMALL_FILE_MB, log,
                () -> UnbufferedCopy.copyByteByByte(srcSmall, destSmall));

        results[idx++] = measure("Buffered byte-by-byte", SMALL_FILE_MB, log,
                () -> BufferedCopy.copyWithDefaultBuffer(srcSmall, destSmall));

        // ===== NHÓM B: đọc theo khối, file lớn =====
        report(log, "\n--- Nhom B: doc theo khoi (file " + LARGE_FILE_MB + "MB) ---");

        results[idx++] = measure("Unbuf chunk 8KB", LARGE_FILE_MB, log,
                () -> UnbufferedCopy.copyWithChunk(srcLarge, destLarge, 8192));

        for (int bufferSize : BUFFER_SIZES) {
            results[idx++] = measure("Buffered " + BufferedCopy.bufferLabel(bufferSize),
                    LARGE_FILE_MB, log,
                    () -> BufferedCopy.copyWithCustomBuffer(srcLarge, destLarge, bufferSize));
        }

        return results;
    }

    /**
     * Đo một phương pháp qua BenchmarkUtils rồi đóng gói thành Result.
     */
    private static Result measure(String methodName, int fileSizeMB,
                                      Consumer<String> log,
                                      BenchmarkUtils.TimedTask task) throws IOException {
        report(log, "  Dang do: " + methodName + "...");
        long nanos = BenchmarkUtils.medianNanos(task);

        Result result = new Result(methodName, fileSizeMB, nanos);
        report(log, String.format("  => %s : %.2f ms (%.1f MB/s)",
                methodName, result.ms(), result.throughputMBps()));
        return result;
    }

    /** Lọc kết quả theo kích thước file, dùng để tách nhóm khi vẽ biểu đồ. */
    public static Result[] filterBySize(Result[] results, int fileSizeMB) {
        int count = 0;
        for (Result r : results) {
            if (r.fileSizeMB() == fileSizeMB) {
                count++;
            }
        }

        Result[] filtered = new Result[count];
        int i = 0;
        for (Result r : results) {
            if (r.fileSizeMB() == fileSizeMB) {
                filtered[i++] = r;
            }
        }
        return filtered;
    }

    /** Gửi log nếu có nơi nhận. */
    private static void report(Consumer<String> log, String message) {
        if (log != null) {
            log.accept(message);
        }
    }
}
