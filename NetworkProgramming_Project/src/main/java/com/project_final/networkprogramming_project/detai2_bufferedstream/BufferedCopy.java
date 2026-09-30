package com.project_final.networkprogramming_project.detai2_bufferedstream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Đề tài 2: Sao chép file CÓ dùng BufferedInputStream / BufferedOutputStream.
 * ===========================================================================
 *
 * ▸ Cơ chế Buffered:
 *   - BufferedInputStream tạo một VÙNG ĐỆM trong RAM (mặc định 8192 byte = 8KB)
 *   - Khi gọi read(): nạp nguyên 8KB từ đĩa vào buffer trong 1 system call
 *   - Các lần read() tiếp theo lấy từ BUFFER (RAM) → không tốn system call
 *   - Chỉ khi buffer cạn mới gọi system call nạp tiếp
 *
 * Luồng xử lý CÓ Buffered:
 *
 *   Ứng dụng → [read()] → Buffer (RAM) → trả byte
 *                            ↑
 *                  [tự động nạp 8KB từ đĩa khi buffer cạn]
 *
 * So sánh số system call khi sao chép file 10MB:
 *   - Không buffer (byte-by-byte): 10.485.760 lần
 *   - Có buffer (8KB)            :      1.280 lần
 *   → Giảm 8192 lần → đây chính là nguồn gốc của chênh lệch tốc độ.
 *
 * GHI CHÚ VỀ THIẾT KẾ: xem {@link UnbufferedCopy} — các method ở đây chỉ sao
 * chép, không tự đo thời gian và không tự in kết quả.
 *
 * @author Cao Duy Quốc Khánh
 */
public class BufferedCopy {

    /** Kích thước buffer mặc định của BufferedInputStream trong JDK. */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Sao chép file bằng BufferedStream nhưng vẫn đọc/ghi TỪNG BYTE.
     *
     * Đây là phép so sánh công bằng với {@link UnbufferedCopy#copyByteByByte}:
     * cùng đọc từng byte, chỉ khác là có thêm lớp đệm.
     * Chênh lệch thời gian giữa hai method này chính là chi phí của system call.
     *
     * @param srcPath  file nguồn
     * @param destPath file đích
     * @throws IOException nếu đọc/ghi thất bại
     */
    public static void copyWithDefaultBuffer(String srcPath, String destPath)
            throws IOException {
        // BufferedInputStream BỌC (wrap) FileInputStream → thêm lớp đệm 8KB
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(srcPath));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destPath))) {

            int byteData;
            // Vẫn gọi read() từng byte, nhưng phần lớn lấy từ buffer trong RAM
            while ((byteData = bis.read()) != -1) {
                bos.write(byteData);
            }
            // close() của try-with-resources tự gọi flush().
            // BẪY KINH ĐIỂN: nếu tự quản lý stream mà quên flush() thì phần dữ
            // liệu còn nằm trong buffer sẽ KHÔNG bao giờ xuống đĩa → mất đuôi file.
        }
    }

    /**
     * Sao chép file bằng BufferedStream với kích thước buffer tùy chỉnh.
     *
     * Dùng để khảo sát: tăng buffer từ 8KB lên 32KB, 64KB có nhanh thêm không?
     * (Thường lợi ích giảm dần, vì khi buffer đã đủ lớn thì nút thắt chuyển từ
     * số system call sang băng thông của đĩa.)
     *
     * @param srcPath    file nguồn
     * @param destPath   file đích
     * @param bufferSize kích thước buffer (bytes)
     * @throws IOException nếu đọc/ghi thất bại
     */
    public static void copyWithCustomBuffer(String srcPath, String destPath, int bufferSize)
            throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(
                     new FileInputStream(srcPath), bufferSize);
             BufferedOutputStream bos = new BufferedOutputStream(
                     new FileOutputStream(destPath), bufferSize)) {

            byte[] chunk = new byte[bufferSize];
            int bytesRead;

            while ((bytesRead = bis.read(chunk)) != -1) {
                bos.write(chunk, 0, bytesRead); // ghi đúng số byte đọc được
            }
        }
    }

    /** Đổi số byte thành nhãn dễ đọc, ví dụ 8192 → "8KB". */
    public static String bufferLabel(int bufferSize) {
        return bufferSize >= 1024 ? (bufferSize / 1024) + "KB" : bufferSize + "B";
    }
}
