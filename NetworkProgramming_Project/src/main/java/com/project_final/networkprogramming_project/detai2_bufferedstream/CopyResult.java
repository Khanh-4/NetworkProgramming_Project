package com.project_final.networkprogramming_project.detai2_bufferedstream;

import com.project_final.networkprogramming_project.detai1_bytecharstream.MedianTimer;

/**
 * Đề tài 2: Kết quả đo của MỘT phương pháp sao chép.
 * ===================================================
 *
 * Dùng {@code record} nên mọi field đều final → object BẤT BIẾN (immutable).
 *
 * Thời gian lưu bằng NANOSECOND, không phải millisecond. Lý do: các phương pháp
 * có buffer sao chép file 10MB chỉ mất khoảng 7-11 ms. Nếu lưu bằng long ms thì
 * sai số làm tròn đã là ±1 ms, tức ±14% ở mốc 7 ms — không đủ để kết luận
 * "buffer 64KB nhanh hơn buffer 8KB".
 *
 * Trường {@code fileSizeMB} là bắt buộc: các phương pháp KHÔNG chạy trên cùng
 * kích thước file (byte-by-byte quá chậm nên chỉ chạy với file nhỏ). Thiếu
 * trường này thì biểu đồ sẽ vẽ chung một trục và so sánh nhầm giữa hai nhóm.
 *
 * @param methodName tên phương pháp, hiển thị trên bảng và biểu đồ
 * @param fileSizeMB kích thước file đã dùng để đo (MB)
 * @param nanos      thời gian trung vị (nanosecond)
 *
 * @author Cao Duy Quốc Khánh
 */
public record CopyResult(String methodName, int fileSizeMB, long nanos) {

    /** Thời gian tính bằng millisecond, giữ phần thập phân. */
    public double ms() {
        return MedianTimer.toMs(nanos);
    }

    /**
     * Thông lượng tính theo MB/s.
     *
     * Lưu ý: đây là tốc độ SAO CHÉP, tính trên lượng dữ liệu nguồn.
     * Thực tế đĩa phải chịu gấp đôi vì vừa đọc vừa ghi.
     */
    public double throughputMBps() {
        if (nanos <= 0) {
            return 0;
        }
        double seconds = nanos / 1_000_000_000.0;
        return fileSizeMB / seconds;
    }

    /** Nhanh hơn kết quả tham chiếu bao nhiêu lần. */
    public double speedupOver(CopyResult baseline) {
        if (nanos <= 0) {
            return 0;
        }
        return (double) baseline.nanos() / nanos;
    }
}
