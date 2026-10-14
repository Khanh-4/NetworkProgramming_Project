package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Đề tài 1: Đọc file bằng ByteStream / CharStream và đo tốc độ.
 * ==============================================================
 *
 * Đây là phần LÕI của đề tài 1 — hai cách đọc file mà đề bài yêu cầu:
 *
 *   ByteStream (FileInputStream): đĩa → byte[]                     (không diễn giải)
 *   CharStream (FileReader)     : đĩa → byte[] → CharsetDecoder → char[]
 *                                                  ↑ bước thêm này làm CharStream chậm hơn
 *
 * Phép so sánh là ma trận 2x2, vì chỉ so 2 loại stream trên 1 loại file thì
 * không trả lời được câu hỏi "đọc file nhị phân vs ký tự" của đề bài:
 *
 *                    │ File TEXT (.txt)  │ File NHỊ PHÂN (.bin)
 *   ─────────────────┼───────────────────┼──────────────────────
 *   ByteStream       │  đúng, nhanh      │  đúng, nhanh
 *   CharStream       │  đúng, chậm hơn   │  chậm hơn VÀ HỎNG dữ liệu
 *
 * Mọi phép đo đi qua {@link BenchmarkUtils#medianNanos} (warm-up + trung vị),
 * nên ByteStream và CharStream được đo trong CÙNG điều kiện page cache, thay vì
 * cái đo trước chịu cache lạnh còn cái đo sau ăn sẵn.
 *
 * @author Cao Duy Quốc Khánh
 */
public final class StreamBenchmark {

    /**
     * Dãy kích thước file test (MB).
     *
     * VÌ SAO BỎ MỐC 1 MB?
     *   Đọc 1 MB từ page cache chỉ mất khoảng 0.2-0.6 ms, tức sát sàn phân giải
     *   của phép đo. Chỉ cần OS hoặc antivirus chen ngang một nhịp là con số lệch
     *   hẳn, kéo theo tỉ lệ "CharStream chậm hơn mấy lần" vọt lên bất thường
     *   (đã quan sát: 53.8x ở mốc 1 MB trong khi các mốc khác chỉ 17-23x).
     *   Bắt đầu từ 5 MB thì mọi phép đo đều đủ lớn để thoát vùng nhiễu đó.
     */
    private static final int[] DEFAULT_SIZES_MB = {5, 10, 20, 40};

    /** Buffer đọc của ByteStream: 8 KB, khớp block size phổ biến của đĩa. */
    private static final int BYTE_BUFFER_SIZE = 8192;

    /** Buffer đọc của CharStream: 4096 char (= 8 KB RAM vì char là 16-bit). */
    private static final int CHAR_BUFFER_SIZE = 4096;

    /** Ký tự U+FFFD mà decoder chèn vào khi gặp byte không decode được. */
    private static final char REPLACEMENT_CHAR = '�';

    /** Class tiện ích, không cho tạo instance. */
    private StreamBenchmark() {
    }

    /**
     * Kết quả đo cho MỘT kích thước file.
     *
     * Dùng {@code record} nên mọi field đều final → object BẤT BIẾN (immutable):
     * không class nào vô tình sửa được kết quả sau khi đã đo xong.
     * Thời gian lưu bằng NANOSECOND vì đọc file nhỏ từ page cache có thể < 1 ms.
     *
     * @param sizeMB           kích thước file test (MB)
     * @param byteOnTextNs     thời gian ByteStream đọc file .txt
     * @param charOnTextNs     thời gian CharStream đọc file .txt
     * @param byteOnBinaryNs   thời gian ByteStream đọc file .bin
     * @param charOnBinaryNs   thời gian CharStream đọc file .bin
     * @param binaryTotalChars tổng số char CharStream đọc được từ file .bin
     * @param binaryBadChars   số char bị thay bằng U+FFFD (dữ liệu đã hỏng)
     */
    public record Result(
            int sizeMB,
            long byteOnTextNs,
            long charOnTextNs,
            long byteOnBinaryNs,
            long charOnBinaryNs,
            long binaryTotalChars,
            long binaryBadChars
    ) {
        public double byteOnTextMs()   { return BenchmarkUtils.toMs(byteOnTextNs); }
        public double charOnTextMs()   { return BenchmarkUtils.toMs(charOnTextNs); }
        public double byteOnBinaryMs() { return BenchmarkUtils.toMs(byteOnBinaryNs); }
        public double charOnBinaryMs() { return BenchmarkUtils.toMs(charOnBinaryNs); }

        /** CharStream chậm hơn ByteStream bao nhiêu lần khi đọc file TEXT. */
        public double textSlowdownFactor() {
            return byteOnTextNs <= 0 ? 0 : (double) charOnTextNs / byteOnTextNs;
        }

        /** CharStream chậm hơn ByteStream bao nhiêu lần khi đọc file NHỊ PHÂN. */
        public double binarySlowdownFactor() {
            return byteOnBinaryNs <= 0 ? 0 : (double) charOnBinaryNs / byteOnBinaryNs;
        }

        /**
         * Tỉ lệ ký tự bị hỏng khi dùng CharStream đọc file nhị phân (%).
         * BẰNG CHỨNG cho thấy CharStream không chỉ chậm hơn mà còn LÀM SAI DỮ LIỆU.
         */
        public double binaryCorruptionPercent() {
            return binaryTotalChars <= 0 ? 0 : binaryBadChars * 100.0 / binaryTotalChars;
        }
    }

    /**
     * Trả về dãy kích thước file test.
     * Trả BẢN SAO chứ không trả mảng gốc: mảng trong Java luôn thay đổi được,
     * nếu trả thẳng thì bên gọi có thể sửa và làm hỏng dãy dùng chung.
     */
    public static int[] defaultSizesMB() {
        return DEFAULT_SIZES_MB.clone();
    }

    /**
     * Đo đủ 4 tổ hợp (ByteStream/CharStream) x (file text/file nhị phân)
     * cho một kích thước file.
     *
     * @param listener nơi nhận log tiến độ (có thể null)
     * @throws FileNotFoundException nếu file test chưa được tạo
     */
    public static Result measure(int sizeMB, String textFile, String binaryFile,
                                 Consumer<String> listener) throws IOException {
        Charset utf8 = StandardCharsets.UTF_8;
        report(listener, "  [" + sizeMB + "MB] warm-up + do nhieu lan, lay trung vi...");

        long byteOnText = BenchmarkUtils.medianNanos(() -> readWithByteStream(textFile));
        report(listener, "  [" + sizeMB + "MB] TEXT   | ByteStream: " + BenchmarkUtils.formatMs(byteOnText));

        long charOnText = BenchmarkUtils.medianNanos(() -> readWithCharStream(textFile, utf8));
        report(listener, "  [" + sizeMB + "MB] TEXT   | CharStream: " + BenchmarkUtils.formatMs(charOnText));

        long byteOnBinary = BenchmarkUtils.medianNanos(() -> readWithByteStream(binaryFile));
        report(listener, "  [" + sizeMB + "MB] BINARY | ByteStream: " + BenchmarkUtils.formatMs(byteOnBinary));

        long charOnBinary = BenchmarkUtils.medianNanos(() -> readWithCharStream(binaryFile, utf8));
        report(listener, "  [" + sizeMB + "MB] BINARY | CharStream: " + BenchmarkUtils.formatMs(charOnBinary));

        // Kiểm tra dữ liệu có bị hỏng không (chạy 1 lần, không tính thời gian)
        long[] corruption = countCorruption(binaryFile, utf8);
        report(listener, "  [" + sizeMB + "MB] CharStream doc file .bin -> "
                + corruption[1] + "/" + corruption[0] + " ky tu BI HONG");

        return new Result(sizeMB, byteOnText, charOnText,
                byteOnBinary, charOnBinary, corruption[0], corruption[1]);
    }

    /**
     * Đọc hết file bằng ByteStream (FileInputStream), trả về số BYTE đọc được.
     * Đọc theo mảng byte[] 8KB, mỗi lần read() = 1 system call.
     */
    static long readWithByteStream(String filePath) throws IOException {
        long total = 0;
        // try-with-resources: stream tự đóng dù có exception → không leak file descriptor
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[BYTE_BUFFER_SIZE];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                total += read;
            }
        }
        return total;
    }

    /**
     * Đọc hết file bằng CharStream (FileReader), trả về số KÝ TỰ đọc được.
     *
     * Luôn truyền charset vào FileReader (constructor có từ Java 11).
     * Nếu để {@code new FileReader(path)} thì nó lấy Charset.defaultCharset(),
     * tức là phụ thuộc môi trường chạy → kết quả không tái lập được.
     */
    static long readWithCharStream(String filePath, Charset charset) throws IOException {
        long total = 0;
        try (FileReader fr = new FileReader(filePath, charset)) {
            char[] buffer = new char[CHAR_BUFFER_SIZE];
            int read;
            // Mỗi vòng lặp: byte[] từ đĩa -> CharsetDecoder -> char[]
            // Chính bước DECODE này làm CharStream chậm hơn ByteStream.
            while ((read = fr.read(buffer)) != -1) {
                total += read;
            }
        }
        return total;
    }

    /**
     * Đếm số ký tự bị hỏng khi dùng CharStream đọc file nhị phân.
     *
     * CƠ CHẾ: byte trong file nhị phân phần lớn KHÔNG phải chuỗi UTF-8 hợp lệ.
     * CharsetDecoder mặc định hành xử theo CodingErrorAction.REPLACE:
     * thay mỗi byte lỗi bằng ký tự U+FFFD thay vì ném exception.
     * → Chương trình KHÔNG crash, nhưng dữ liệu đã bị thay đổi âm thầm.
     *
     * @return mảng 2 phần tử: [0] = tổng số char đọc được, [1] = số char bị hỏng
     */
    private static long[] countCorruption(String filePath, Charset charset) throws IOException {
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

    /** Gửi log tiến độ nếu có listener. */
    private static void report(Consumer<String> listener, String message) {
        if (listener != null) {
            listener.accept(message);
        }
    }
}
