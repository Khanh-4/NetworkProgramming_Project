package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Đề tài 1: Demo ĐỌC và GHI tệp .txt bằng cả hai loại luồng.
 * ===========================================================
 *
 * Đây là phần "Demo" mà đề bài yêu cầu: đọc/ghi nội dung tệp .txt bằng
 * FileInputStream và FileReader.
 *
 * ĐIỂM MẤU CHỐT CỦA PHẦN GHI:
 * ----------------------------
 * Khi ĐỌC, hai loại luồng chỉ khác nhau ở tốc độ và đơn vị (byte vs char).
 * Nhưng khi GHI thì khác biệt lộ ra rõ hơn nhiều:
 *
 *   ByteStream (FileOutputStream):
 *       String  →  [LẬP TRÌNH VIÊN tự gọi getBytes(charset)]  →  byte[]  →  đĩa
 *                   ↑ quên chỉ định charset ở đây là hỏng tiếng Việt
 *
 *   CharStream (OutputStreamWriter):
 *       String  →  [CharsetEncoder làm tự động]               →  byte[]  →  đĩa
 *
 * Nghĩa là ByteStream buộc người viết code phải TỰ xử lý encoding. Nó không
 * biết gì về khái niệm "ký tự" — chỉ nhận byte. Đó là lý do ghi văn bản bằng
 * ByteStream dễ sai, còn ghi dữ liệu nhị phân thì bắt buộc phải dùng nó.
 *
 * Cả hai cách dưới đây đều cho ra file GIỐNG HỆT nhau từng byte — demo sẽ
 * kiểm chứng điều đó bằng cách so sánh kích thước hai file.
 *
 * @author Cao Duy Quốc Khánh
 */
public final class ReadWriteDemo {

    /** Nội dung mẫu có dấu tiếng Việt, để thấy rõ vấn đề encoding khi ghi. */
    private static final String SAMPLE_LINE =
            "Lập trình mạng máy tính — Đề tài 1: luồng byte và luồng ký tự.\n";

    /** Số dòng ghi ra file demo. Đủ lớn để đo được, đủ nhỏ để chạy nhanh. */
    private static final int LINE_COUNT = 20_000;

    private static final String FILE_BY_BYTE_STREAM = "demo_write_bytestream.txt";
    private static final String FILE_BY_FILE_WRITER = "demo_write_filewriter.txt";
    private static final String FILE_BY_CHAR_STREAM = "demo_write_charstream.txt";

    private ReadWriteDemo() {
    }

    /** Chạy demo và in báo cáo ra console. */
    public static void run() {
        System.out.println(buildReport());
    }

    /**
     * Sinh báo cáo dạng text (console in trực tiếp, GUI đưa vào tab riêng).
     */
    public static String buildReport() {
        StringBuilder out = new StringBuilder();

        out.append("DEMO ĐỌC/GHI TỆP .txt BẰNG HAI LOẠI LUỒNG\n");
        out.append("=".repeat(64)).append("\n\n");

        String content = SAMPLE_LINE.repeat(LINE_COUNT);
        TestFileUtils.ensureTestDir();

        String dir = TestFileUtils.getTestDir() + File.separator;
        String bytePath = dir + FILE_BY_BYTE_STREAM;
        String writerPath = dir + FILE_BY_FILE_WRITER;
        String charPath = dir + FILE_BY_CHAR_STREAM;

        try {
            appendWriteSection(out, content, bytePath, writerPath, charPath);
            appendVerifySection(out, bytePath, writerPath, charPath);
            appendReadSection(out, bytePath);
            appendSummary(out);

        } catch (IOException e) {
            out.append("\n[LỖI] Không đọc/ghi được file demo: ")
               .append(e.getMessage()).append("\n");
        } finally {
            // Dọn file demo, không để rác lại trong project
            new File(bytePath).delete();
            new File(writerPath).delete();
            new File(charPath).delete();
        }

        return out.toString();
    }

    /** Phần 1: ghi cùng một nội dung bằng hai cách, đo thời gian. */
    private static void appendWriteSection(StringBuilder out, String content, String bytePath,
                                           String writerPath, String charPath)
            throws IOException {
        out.append("1) GHI FILE — ").append(LINE_COUNT).append(" dòng tiếng Việt có dấu\n");
        out.append("   Ba cách ghi cùng một nội dung, để thấy ai lo việc encode.\n\n");

        long byteNanos = MedianTimer.medianNanos(() -> writeWithByteStream(bytePath, content));
        out.append("   • ByteStream — FileOutputStream\n");
        out.append("     Thời gian   : ").append(formatMs(byteNanos)).append("\n");
        out.append("     Cách viết   : fos.write(content.getBytes(StandardCharsets.UTF_8))\n");
        out.append("     Ai encode?  : LẬP TRÌNH VIÊN — phải tự gọi getBytes() kèm charset\n\n");

        long writerNanos = MedianTimer.medianNanos(() -> writeWithFileWriter(writerPath, content));
        out.append("   • CharStream — FileWriter (lớp ghi ký tự dành riêng cho file)\n");
        out.append("     Thời gian   : ").append(formatMs(writerNanos)).append("\n");
        out.append("     Cách viết   : new FileWriter(path, StandardCharsets.UTF_8)\n");
        out.append("     Ai encode?  : THƯ VIỆN — CharsetEncoder lo, ta chỉ đưa String\n\n");

        long charNanos = MedianTimer.medianNanos(() -> writeWithCharStream(charPath, content));
        out.append("   • CharStream — OutputStreamWriter bọc FileOutputStream\n");
        out.append("     Thời gian   : ").append(formatMs(charNanos)).append("\n");
        out.append("     Cách viết   : new OutputStreamWriter(new FileOutputStream(p), UTF_8)\n");
        out.append("     Ai encode?  : THƯ VIỆN — giống FileWriter, nhưng bọc được MỌI\n");
        out.append("                   OutputStream (socket, ZIP...) chứ không chỉ file\n\n");
    }

    /** Phần 2: kiểm chứng hai file giống hệt nhau. */
    private static void appendVerifySection(StringBuilder out, String bytePath,
                                            String writerPath, String charPath) {
        long byteSize = new File(bytePath).length();
        long writerSize = new File(writerPath).length();
        long charSize = new File(charPath).length();
        boolean allSame = byteSize == writerSize && writerSize == charSize;

        out.append("2) KIỂM CHỨNG — ba cách ghi có ra cùng kết quả không?\n\n");
        out.append("   FileOutputStream   : ").append(byteSize).append(" byte\n");
        out.append("   FileWriter         : ").append(writerSize).append(" byte\n");
        out.append("   OutputStreamWriter : ").append(charSize).append(" byte\n");
        out.append("   Giống nhau?        : ").append(allSame ? "CÓ" : "KHÔNG").append("\n\n");
        out.append("   → Cùng kích thước vì cả ba đều encode UTF-8. Khác biệt KHÔNG nằm\n");
        out.append("     ở kết quả mà ở AI làm việc encode: ByteStream bắt ta tự làm,\n");
        out.append("     hai lớp CharStream làm hộ.\n");
        out.append("     Nếu quên charset, cả ba đều rơi về charset mặc định và file có\n");
        out.append("     thể khác nhau giữa 2 máy (xem phần Bẫy encoding).\n\n");
    }

    /** Phần 3: đọc lại file bằng hai cách, so số byte với số ký tự. */
    private static void appendReadSection(StringBuilder out, String filePath)
            throws IOException {
        out.append("3) ĐỌC LẠI FILE VỪA GHI\n\n");

        long bytesRead = readWithByteStream(filePath);
        long charsRead = readWithCharStream(filePath);

        out.append("   • ByteStream (FileInputStream) đọc được : ")
           .append(bytesRead).append(" BYTE\n");
        out.append("   • CharStream (FileReader) đọc được      : ")
           .append(charsRead).append(" KÝ TỰ\n\n");

        out.append("   Chênh lệch: ").append(bytesRead - charsRead)
           .append(" — đây KHÔNG phải lỗi.\n");
        out.append("   Ký tự tiếng Việt có dấu chiếm 2-3 byte khi encode UTF-8,\n");
        out.append("   nên một file có số byte nhiều hơn số ký tự. Hai luồng đếm\n");
        out.append("   hai đại lượng KHÁC NHAU trên cùng một file.\n\n");
    }

    /** Phần 4: chốt lại khi nào dùng cái nào. */
    private static void appendSummary(StringBuilder out) {
        out.append("4) DÙNG CÁI NÀO KHI GHI FILE?\n\n");
        out.append("   Ghi VĂN BẢN xuống FILE → FileWriter, ngắn gọn nhất\n");
        out.append("     new FileWriter(p, StandardCharsets.UTF_8)\n\n");
        out.append("   Ghi VĂN BẢN xuống CHỖ KHÁC (socket, ZIP...) → OutputStreamWriter\n");
        out.append("     new OutputStreamWriter(anyOutputStream, StandardCharsets.UTF_8)\n\n");
        out.append("   Ghi NHỊ PHÂN → BẮT BUỘC ByteStream, vì ảnh/video/.zip không phải\n");
        out.append("     văn bản, đưa qua encoder sẽ hỏng dữ liệu\n");
        out.append("     new FileOutputStream(p)\n\n");
        out.append("   Dù dùng cách nào cũng LUÔN chỉ định charset tường minh.\n");
    }

    /**
     * Ghi bằng ByteStream: phải TỰ đổi String sang byte[] trước.
     * FileOutputStream không có khái niệm "ký tự", chỉ nhận byte.
     */
    private static void writeWithByteStream(String filePath, String content)
            throws IOException {
        // getBytes(UTF_8) chính là bước mà CharStream làm tự động.
        // Nếu gọi getBytes() không tham số, kết quả phụ thuộc charset mặc định.
        byte[] data = content.getBytes(StandardCharsets.UTF_8);

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(data);
        }
    }

    /**
     * Ghi bằng FileWriter — lớp CharStream dành riêng cho file.
     *
     * Đây là đối xứng của FileReader mà đề bài nêu: FileReader để đọc ký tự từ
     * file, FileWriter để ghi ký tự xuống file.
     *
     * Constructor có tham số Charset chỉ tồn tại từ Java 11. Trước đó FileWriter
     * KHÔNG cho chỉ định charset, buộc phải dùng OutputStreamWriter — đó là lý
     * do nhiều tài liệu cũ khuyên tránh FileWriter.
     */
    private static void writeWithFileWriter(String filePath, String content)
            throws IOException {
        try (Writer writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    /**
     * Ghi bằng OutputStreamWriter bọc FileOutputStream.
     *
     * Kết quả giống hệt FileWriter, nhưng linh hoạt hơn: bọc được MỌI
     * OutputStream chứ không riêng file — ví dụ ghi ký tự xuống socket của
     * chương trình mạng, hay vào một entry trong file ZIP.
     */
    private static void writeWithCharStream(String filePath, String content)
            throws IOException {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            writer.write(content);
            // close() của try-with-resources tự gọi flush().
            // Quên flush() khi tự quản lý stream => mất phần dữ liệu cuối buffer.
        }
    }

    /** Đọc bằng ByteStream, trả về số BYTE đọc được. */
    private static long readWithByteStream(String filePath) throws IOException {
        long total = 0;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                total += read;
            }
        }
        return total;
    }

    /** Đọc bằng CharStream, trả về số KÝ TỰ đọc được. */
    private static long readWithCharStream(String filePath) throws IOException {
        long total = 0;
        try (FileReader fr = new FileReader(filePath, StandardCharsets.UTF_8)) {
            char[] buffer = new char[4096];
            int read;
            while ((read = fr.read(buffer)) != -1) {
                total += read;
            }
        }
        return total;
    }

    /** Định dạng nanosecond thành chuỗi "x.xx ms". */
    private static String formatMs(long nanos) {
        return String.format("%.2f ms", MedianTimer.toMs(nanos));
    }
}
