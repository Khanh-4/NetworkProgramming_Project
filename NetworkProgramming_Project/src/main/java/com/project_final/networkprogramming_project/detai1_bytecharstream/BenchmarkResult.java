package com.project_final.networkprogramming_project.detai1_bytecharstream;

/**
 * Đề tài 1: Kết quả benchmark cho MỘT kích thước file.
 * ------------------------------------------------------
 * Dùng {@code record} nên mọi field đều final → object BẤT BIẾN (immutable).
 * Không có setter: muốn đổi giá trị phải tạo object mới.
 * → Tránh việc một class khác vô tình sửa kết quả đo sau khi đã đo xong.
 *
 * Thời gian lưu bằng NANOSECOND vì với file 1 MB đọc từ page cache,
 * thời gian có thể < 1 ms → nếu lưu bằng ms sẽ bị làm tròn thành 0.
 *
 * @param sizeMB            kích thước file test (MB)
 * @param byteOnTextNs      thời gian ByteStream đọc file .txt (nanosecond)
 * @param charOnTextNs      thời gian CharStream đọc file .txt (nanosecond)
 * @param byteOnBinaryNs    thời gian ByteStream đọc file .bin (nanosecond)
 * @param charOnBinaryNs    thời gian CharStream đọc file .bin (nanosecond)
 * @param binaryTotalChars  tổng số char CharStream đọc được từ file .bin
 * @param binaryBadChars    số char bị thay bằng U+FFFD (dữ liệu đã hỏng)
 *
 * @author Cao Duy Quốc Khánh
 */
public record BenchmarkResult(
        int sizeMB,
        long byteOnTextNs,
        long charOnTextNs,
        long byteOnBinaryNs,
        long charOnBinaryNs,
        long binaryTotalChars,
        long binaryBadChars
) {

    /** Đổi nanosecond sang millisecond (giữ phần thập phân). */
    public static double toMs(long nanos) {
        return nanos / 1_000_000.0;
    }

    public double byteOnTextMs()   { return toMs(byteOnTextNs); }
    public double charOnTextMs()   { return toMs(charOnTextNs); }
    public double byteOnBinaryMs() { return toMs(byteOnBinaryNs); }
    public double charOnBinaryMs() { return toMs(charOnBinaryNs); }

    /**
     * CharStream chậm hơn ByteStream bao nhiêu lần khi đọc file TEXT.
     * > 1.0 nghĩa là CharStream chậm hơn (đúng như lý thuyết, vì có bước decode).
     */
    public double textSlowdownFactor() {
        if (byteOnTextNs <= 0) return 0;
        return (double) charOnTextNs / byteOnTextNs;
    }

    /** CharStream chậm hơn ByteStream bao nhiêu lần khi đọc file NHỊ PHÂN. */
    public double binarySlowdownFactor() {
        if (byteOnBinaryNs <= 0) return 0;
        return (double) charOnBinaryNs / byteOnBinaryNs;
    }

    /**
     * Tỉ lệ ký tự bị hỏng khi dùng CharStream đọc file nhị phân (%).
     * Đây là BẰNG CHỨNG cho thấy CharStream không chỉ chậm hơn mà còn
     * LÀM SAI DỮ LIỆU khi gặp byte không phải văn bản hợp lệ.
     */
    public double binaryCorruptionPercent() {
        if (binaryTotalChars <= 0) return 0;
        return binaryBadChars * 100.0 / binaryTotalChars;
    }

    /** Có bị mất dữ liệu khi đọc file nhị phân bằng CharStream hay không. */
    public boolean isBinaryCorrupted() {
        return binaryBadChars > 0;
    }
}
