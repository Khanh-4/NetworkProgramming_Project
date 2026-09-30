package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

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
 * CÁCH XỬ LÝ: toàn bộ việc warm-up và lấy trung vị giao cho {@link MedianTimer},
 * nên đề tài 1 và đề tài 2 dùng CHUNG một phương pháp đo, số liệu so sánh được
 * với nhau. Nhờ vậy cả ByteStream lẫn CharStream đều được đo trong CÙNG điều
 * kiện page cache, thay vì cái đo trước chịu cache lạnh còn cái đo sau ăn sẵn.
 *
 * @author Cao Duy Quốc Khánh
 */
public final class StreamBenchmark {

    /** Buffer đọc của ByteStream: 8 KB, khớp block size phổ biến của đĩa. */
    private static final int BYTE_BUFFER_SIZE = 8192;

    /** Buffer đọc của CharStream: 4096 char (= 8 KB RAM vì char là 16-bit). */
    private static final int CHAR_BUFFER_SIZE = 4096;

    /** Ký tự U+FFFD mà decoder chèn vào khi gặp byte không decode được. */
    private static final char REPLACEMENT_CHAR = '\uFFFD';

    /** Class tiện ích, không cho tạo instance. */
    private StreamBenchmark() {
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
                                          Consumer<String> listener)
            throws IOException {

        Charset utf8 = StandardCharsets.UTF_8;

        report(listener, "  [" + sizeMB + "MB] warm-up + do nhieu lan, lay trung vi...");

        // --- File TEXT ---
        long byteOnText = MedianTimer.medianNanos(() -> readWithByteStream(textFile));
        report(listener, "  [" + sizeMB + "MB] TEXT   | ByteStream: "
                + format(byteOnText));

        long charOnText = MedianTimer.medianNanos(() -> readWithCharStream(textFile, utf8));
        report(listener, "  [" + sizeMB + "MB] TEXT   | CharStream: "
                + format(charOnText));

        // --- File NHỊ PHÂN ---
        long byteOnBinary = MedianTimer.medianNanos(() -> readWithByteStream(binaryFile));
        report(listener, "  [" + sizeMB + "MB] BINARY | ByteStream: "
                + format(byteOnBinary));

        long charOnBinary = MedianTimer.medianNanos(() -> readWithCharStream(binaryFile, utf8));
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
    private static void report(Consumer<String> listener, String message) {
        if (listener != null) {
            listener.accept(message);
        }
    }
}
