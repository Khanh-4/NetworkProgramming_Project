package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.io.IOException;
import java.util.Arrays;

/**
 * Đề tài 1 & 2: Bộ đo thời gian có warm-up và lấy trung vị.
 * ==========================================================
 *
 * VÌ SAO ĐO MỘT LẦN LÀ SAI?
 * -------------------------
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
 * TỰ ĐIỀU CHỈNH THEO ĐỘ NẶNG CỦA TÁC VỤ:
 *   Tác vụ nặng (ví dụ copy từng byte file 1MB mất ~3 giây) thì nhiễu vài chục
 *   ms là không đáng kể — lặp 8 lần chỉ tốn 24 giây mà không chính xác thêm.
 *   Nên nếu lần chạy thử vượt ngưỡng 1 giây, bộ đo tự chuyển sang chế độ rút
 *   gọn: 1 warm-up + 1 lần đo.
 *
 * @author Cao Duy Quốc Khánh
 */
public final class MedianTimer {

    /** Số lần chạy nháp cho tác vụ nhanh (kết quả bị bỏ). */
    private static final int WARMUP_RUNS = 3;

    /** Số lần đo thật cho tác vụ nhanh. Số lẻ để trung vị rơi đúng 1 phần tử. */
    private static final int MEASURED_RUNS = 5;

    /**
     * Ngưỡng coi là "tác vụ nặng" = 1 giây.
     * Trên ngưỡng này, nhiễu vài chục ms chỉ chiếm vài phần trăm → không cần lặp.
     */
    private static final long SLOW_TASK_THRESHOLD_NS = 1_000_000_000L;

    private MedianTimer() {
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
     *
     * @param task tác vụ cần đo
     * @return thời gian trung vị (nanosecond)
     * @throws IOException nếu tác vụ thất bại
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
}
