package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Đề tài 1: Engine đo tốc độ đọc file — dùng chung cho cả console và GUI.
 * ========================================================================
 *
 * VÌ SAO PHẢI CÓ WARM-UP VÀ ĐO NHIỀU LẦN?
 * ----------------------------------------
 * Nếu chỉ đo 1 lần theo thứ tự "ghi file → đọc Byte → đọc Char" thì số liệu SAI
 * vì 2 nguyên nhân ở tầng dưới:
 *
 *  1) OS PAGE CACHE:
 *     File vừa ghi xong còn nằm nguyên trong RAM (page cache của kernel).
 *     Lần đọc ĐẦU tiên có thể phải chờ đĩa, lần đọc SAU lấy thẳng từ RAM.
 *     → Cái nào chạy sau sẽ được lợi → CharStream (chạy sau) trông nhanh giả tạo.
 *
 *         Đọc lần 1:  App → read() → Kernel → [cache miss] → ĐĨA CỨNG  (chậm)
 *         Đọc lần 2:  App → read() → Kernel → [cache hit]  → RAM       (nhanh)
 *
 *  2) JIT COMPILER:
 *     Vòng lặp đọc file lần đầu chạy ở chế độ thông dịch (interpreted).
 *     Sau vài nghìn lần lặp, JIT mới biên dịch sang mã máy → nhanh hơn nhiều.
 *
 * CÁCH XỬ LÝ trong class này:
 *   - Chạy {@value #WARMUP_RUNS} lần WARM-UP và BỎ kết quả
 *     → nạp file vào page cache + để JIT biên dịch xong.
 *   - Sau đó đo {@value #MEASURED_RUNS} lần và lấy TRUNG VỊ (median), không lấy
 *     trung bình, vì median không bị một lần đo lỗi (do OS đi làm việc khác)
 *     kéo lệch toàn bộ kết quả.
 *   → Nhờ vậy cả ByteStream và CharStream đều được đo trong CÙNG điều kiện cache.
 *
 * @author Cao Duy Quốc Khánh
 */
public final class StreamBenchmark {

    /** Số lần chạy nháp để nạp page cache + kích hoạt JIT (kết quả bị bỏ). */
    private static final int WARMUP_RUNS = 2;

    /** Số lần đo thật, lấy trung vị. Số lẻ để median rơi đúng 1 phần tử. */
    private static final int MEASURED_RUNS = 3;

    /** Buffer đọc của ByteStream: 8 KB, khớp block size phổ biến của đĩa. */
    private static final int BYTE_BUFFER_SIZE = 8192;

    /** Buffer đọc của CharStream: 4096 char (= 8 KB RAM vì char là 16-bit). */
    private static final int CHAR_BUFFER_SIZE = 4096;

    /** Ký tự U+FFFD mà decoder chèn vào khi gặp byte không decode được. */
    private static final char REPLACEMENT_CHAR = '�';

    /** Class tiện ích, không cho tạo instance. */
    private StreamBenchmark() {
    }

    /** Nhận thông báo tiến độ để console in ra, hoặc GUI đẩy vào log. */
    public interface ProgressListener {
        void onProgress(String message);
    }

    /** Một tác vụ đọc file có thể ném IOException — dùng cho hàm đo thời gian. */
    private interface ReadTask {
        void execute() throws IOException;
    }

    /**
     * Đo đủ 4 tổ hợp (ByteStream/CharStream) x (file text/file nhị phân)
     * cho một kích thước file.
     *
     * @param sizeMB     kích thước file (chỉ để ghi vào kết quả)
     * @param textFile   đường dẫn file .txt đã tạo sẵn
     * @param binaryFile đường dẫn file .bin đã tạo sẵn
     * @param listener   nơi nhận log tiến độ (có thể null)
     * @return kết quả đo, immutable
     * @throws FileNotFoundException nếu file test chưa được tạo
     * @throws IOException           nếu đọc file thất bại
     */
    public static BenchmarkResult measure(int sizeMB,
                                          String textFile,
                                          String binaryFile,
                                          ProgressListener listener)
            throws IOException {

        Charset utf8 = StandardCharsets.UTF_8;

        report(listener, "  [" + sizeMB + "MB] warm-up " + WARMUP_RUNS
                + " lan + do " + MEASURED_RUNS + " lan, lay trung vi...");

        // --- File TEXT ---
        long byteOnText = medianNanos(() -> readWithByteStream(textFile));
        report(listener, "  [" + sizeMB + "MB] TEXT   | ByteStream: "
                + format(byteOnText));

        long charOnText = medianNanos(() -> readWithCharStream(textFile, utf8));
        report(listener, "  [" + sizeMB + "MB] TEXT   | CharStream: "
                + format(charOnText));

        // --- File NHỊ PHÂN ---
        long byteOnBinary = medianNanos(() -> readWithByteStream(binaryFile));
        report(listener, "  [" + sizeMB + "MB] BINARY | ByteStream: "
                + format(byteOnBinary));

        long charOnBinary = medianNanos(() -> readWithCharStream(binaryFile, utf8));
        report(listener, "  [" + sizeMB + "MB] BINARY | CharStream: "
                + format(charOnBinary));

        // --- Kiểm tra dữ liệu có bị hỏng không (chạy 1 lần, không tính thời gian) ---
        long[] corruption = countCorruption(binaryFile, utf8);
        report(listener, "  [" + sizeMB + "MB] CharStream doc file .bin -> "
                + corruption[1] + "/" + corruption[0] + " ky tu BI HONG");

        return new BenchmarkResult(sizeMB, byteOnText, charOnText,
                byteOnBinary, charOnBinary, corruption[0], corruption[1]);
    }

    /**
     * Chạy warm-up rồi đo nhiều lần, trả về TRUNG VỊ thời gian (nanosecond).
     */
    private static long medianNanos(ReadTask task) throws IOException {
        // Warm-up: nạp file vào page cache + để JIT biên dịch vòng lặp đọc.
        for (int i = 0; i < WARMUP_RUNS; i++) {
            task.execute();
        }

        long[] samples = new long[MEASURED_RUNS];
        for (int i = 0; i < MEASURED_RUNS; i++) {
            samples[i] = timeOnce(task);
        }

        // clone() trước khi sort: KHÔNG sửa mảng gốc (nguyên tắc immutability)
        long[] sorted = samples.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    /** Đo một lần thực thi, trả về nanosecond. */
    private static long timeOnce(ReadTask task) throws IOException {
        // nanoTime() là đồng hồ đơn điệu, dùng để đo khoảng thời gian.
        // KHÔNG dùng currentTimeMillis() vì nó có thể bị NTP chỉnh giật lùi.
        long start = System.nanoTime();
        task.execute();
        return System.nanoTime() - start;
    }

    /**
     * Đọc hết file bằng ByteStream (FileInputStream).
     * Đọc theo mảng byte[] 8KB, mỗi lần read() = 1 system call.
     */
    private static void readWithByteStream(String filePath) throws IOException {
        // try-with-resources: stream tự đóng dù có exception → không leak file descriptor
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[BYTE_BUFFER_SIZE];
            while (fis.read(buffer) != -1) {
                // Chỉ đọc để đo tốc độ, không xử lý nội dung
            }
        }
    }

    /**
     * Đọc hết file bằng CharStream (FileReader) với charset CHỈ ĐỊNH TƯỜNG MINH.
     *
     * Luôn truyền charset vào FileReader (constructor có từ Java 11).
     * Nếu để {@code new FileReader(path)} thì nó lấy Charset.defaultCharset(),
     * tức là phụ thuộc môi trường chạy → kết quả benchmark không tái lập được.
     */
    private static void readWithCharStream(String filePath, Charset charset)
            throws IOException {
        try (FileReader fr = new FileReader(filePath, charset)) {
            char[] buffer = new char[CHAR_BUFFER_SIZE];
            while (fr.read(buffer) != -1) {
                // Mỗi vòng lặp: byte[] từ đĩa -> CharsetDecoder -> char[]
                // Chính bước DECODE này làm CharStream chậm hơn ByteStream.
            }
        }
    }

    /**
     * Đếm số ký tự bị hỏng khi dùng CharStream đọc file nhị phân.
     *
     * CƠ CHẾ: byte trong file nhị phân phần lớn KHÔNG phải chuỗi UTF-8 hợp lệ.
     * CharsetDecoder mặc định hành xử theo CodingErrorAction.REPLACE:
     * thay mỗi byte lỗi bằng ký tự U+FFFD thay vì ném exception.
     * → Chương trình KHÔNG crash, nhưng dữ liệu đã bị thay đổi âm thầm.
     *   Đây là lỗi nguy hiểm nhất: sai mà không báo lỗi.
     *
     * @return mảng 2 phần tử: [0] = tổng số char đọc được, [1] = số char bị hỏng
     */
    private static long[] countCorruption(String filePath, Charset charset)
            throws IOException {
        long total = 0;
        long bad = 0;

        try (FileReader fr = new FileReader(filePath, charset)) {
            char[] buffer = new char[CHAR_BUFFER_SIZE];
            int charsRead;
            while ((charsRead = fr.read(buffer)) != -1) {
                total += charsRead;
                for (int i = 0; i < charsRead; i++) {
                    if (buffer[i] == REPLACEMENT_CHAR) {
                        bad++;
                    }
                }
            }
        }

        return new long[]{total, bad};
    }

    /** Định dạng nanosecond thành chuỗi "x.xx ms" để in log. */
    private static String format(long nanos) {
        return String.format("%.2f ms", BenchmarkResult.toMs(nanos));
    }

    /** Gửi log tiến độ nếu có listener. */
    private static void report(ProgressListener listener, String message) {
        if (listener != null) {
            listener.onProgress(message);
        }
    }
}
