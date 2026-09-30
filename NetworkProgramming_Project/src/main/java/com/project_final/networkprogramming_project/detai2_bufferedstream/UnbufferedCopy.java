package com.project_final.networkprogramming_project.detai2_bufferedstream;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Đề tài 2: Sao chép file KHÔNG dùng Buffered Stream.
 * =====================================================
 *
 * ▸ Cơ chế: đọc/ghi trực tiếp xuống tầng OS, không có vùng đệm trung gian
 * ▸ Mỗi lần gọi read()/write() = 1 system call
 * ▸ System call rất tốn kém vì phải chuyển User Mode → Kernel Mode
 *
 * Luồng xử lý KHÔNG Buffered (đọc từng byte):
 *
 *   Ứng dụng → [read() system call] → OS Kernel → Disk → trả về 1 byte
 *   → Lặp lại hàng TRIỆU lần cho file lớn = RẤT CHẬM!
 *
 * GHI CHÚ VỀ THIẾT KẾ:
 *   Các method ở đây chỉ THỰC HIỆN việc sao chép, không tự đo thời gian và
 *   không tự in ra màn hình. Việc đo giao cho MedianTimer (có warm-up + trung
 *   vị), việc hiển thị giao cho PerformanceChart / PerformanceChartGUI.
 *   Tách như vậy thì cùng một đoạn code sao chép dùng được cho cả console lẫn
 *   GUI, và không còn 2 bản logic trùng nhau như trước.
 *
 * @author Cao Duy Quốc Khánh
 */
public class UnbufferedCopy {

    /**
     * Sao chép file bằng cách đọc/ghi TỪNG BYTE một, không buffer.
     * Đây là cách CHẬM NHẤT, dùng để thấy rõ chi phí của system call.
     *
     * @param srcPath  file nguồn
     * @param destPath file đích
     * @throws IOException nếu đọc/ghi thất bại
     */
    public static void copyByteByByte(String srcPath, String destPath) throws IOException {
        // try-with-resources: tự động đóng cả 2 stream, kể cả khi có exception
        try (FileInputStream fis = new FileInputStream(srcPath);
             FileOutputStream fos = new FileOutputStream(destPath)) {

            int byteData;
            // Mỗi lần read() và write() đều là 1 system call riêng
            // → Với file 1MB = 1.048.576 lần read + 1.048.576 lần write!
            while ((byteData = fis.read()) != -1) {
                fos.write(byteData);
            }
            // close() của try-with-resources tự gọi flush()
        }
    }

    /**
     * Sao chép file theo KHỐI nhưng KHÔNG bọc BufferedStream.
     *
     * Nhanh hơn hẳn byte-by-byte vì số system call giảm đi chunkSize lần.
     * So sánh method này với {@link BufferedCopy#copyWithCustomBuffer} cùng
     * kích thước khối sẽ thấy: khi đã tự đọc theo khối lớn thì BufferedStream
     * không còn tạo ra khác biệt lớn nữa — buffer chỉ cứu những code đọc lắt nhắt.
     *
     * @param srcPath   file nguồn
     * @param destPath  file đích
     * @param chunkSize kích thước khối đọc (bytes)
     * @throws IOException nếu đọc/ghi thất bại
     */
    public static void copyWithChunk(String srcPath, String destPath, int chunkSize)
            throws IOException {
        try (FileInputStream fis = new FileInputStream(srcPath);
             FileOutputStream fos = new FileOutputStream(destPath)) {

            byte[] chunk = new byte[chunkSize];
            int bytesRead;

            while ((bytesRead = fis.read(chunk)) != -1) {
                // Ghi đúng số byte đọc được, KHÔNG ghi cả mảng:
                // lần đọc cuối thường không đầy chunk → ghi thừa sẽ làm hỏng file
                fos.write(chunk, 0, bytesRead);
            }
        }
    }
}
