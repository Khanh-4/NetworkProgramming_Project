package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Đề tài 1: Hai bản demo dạng báo cáo chữ, hiển thị ở 2 tab của GUI.
 * ===================================================================
 *
 * 1) {@link #readWriteReport()} — DEMO ĐỌC/GHI tệp .txt bằng cả hai loại luồng
 *    (đúng phần "Demo" của đề bài: FileInputStream và FileReader).
 *
 *    Khi ĐỌC, hai loại luồng chỉ khác nhau ở tốc độ và đơn vị (byte vs char).
 *    Khi GHI thì khác biệt lộ rõ hơn:
 *
 *      ByteStream (FileOutputStream):
 *          String → [LẬP TRÌNH VIÊN tự gọi getBytes(charset)] → byte[] → đĩa
 *                    ↑ quên chỉ định charset ở đây là hỏng tiếng Việt
 *      CharStream (FileWriter / OutputStreamWriter):
 *          String → [CharsetEncoder làm tự động]              → byte[] → đĩa
 *
 * 2) {@link #encodingReport()} — BẪY ENCODING: vì sao CharStream "tự xử lý
 *    encoding" vẫn đọc sai. Đây là khác biệt QUAN TRỌNG NHẤT, hơn cả nhanh/chậm:
 *
 *      ByteStream : trả về ĐÚNG byte có trong file. Không diễn giải.
 *      CharStream : PHẢI diễn giải byte thành ký tự bằng một charset.
 *                   Đoán sai charset => đọc ra ký tự rác (mojibake).
 *
 *    Ký tự 'ế' (UTF-8 = 3 byte E1 BA BF):
 *      [E1 BA BF] → decode UTF-8        → 'ế'     ✅ đúng
 *      [E1 BA BF] → decode windows-1252 → "áº¿"   ❌ sai
 *
 *    Từ JDK 18 (JEP 400) default charset luôn là UTF-8 nên bẫy nhẹ đi, nhưng
 *    vẫn còn khi đọc file do hệ thống cũ sinh ra hoặc chạy trên JDK cũ hơn.
 *    → KẾT LUẬN: luôn truyền charset tường minh, đừng tin giá trị mặc định.
 *
 * @author Cao Duy Quốc Khánh
 */
public final class StreamDemos {

    /** Nội dung mẫu có dấu tiếng Việt, để thấy rõ vấn đề encoding khi ghi. */
    private static final String SAMPLE_LINE =
            "Lập trình mạng máy tính — Đề tài 1: luồng byte và luồng ký tự.\n";

    /** Số dòng ghi ra file demo. Đủ lớn để đo được, đủ nhỏ để chạy nhanh. */
    private static final int LINE_COUNT = 20_000;

    private static final String FILE_BY_BYTE_STREAM = "demo_write_bytestream.txt";
    private static final String FILE_BY_FILE_WRITER = "demo_write_filewriter.txt";
    private static final String FILE_BY_CHAR_STREAM = "demo_write_charstream.txt";

    /**
     * Câu gốc của demo encoding. Bắt buộc CÓ DẤU: ký tự có dấu chiếm 2-3 byte
     * trong UTF-8, nhờ đó mới thấy "số byte khác số ký tự" và tái hiện được mojibake.
     */
    private static final String ORIGINAL_TEXT =
            "Lập trình mạng: luồng byte và luồng ký tự — Tiếng Việt có dấu: ă â đ ê ô ơ ư";

    private static final String ENCODING_FILE = "encoding_demo_utf8.txt";

    /** Ký tự thay thế U+FFFD mà decoder chèn vào khi gặp byte không decode được. */
    private static final char REPLACEMENT_CHAR = '�';

    /** Độ dài tối đa khi in một dòng kết quả decode ra log. */
    private static final int PREVIEW_LIMIT = 58;

    private StreamDemos() {
    }

    // =====================================================================
    // DEMO 1: ĐỌC / GHI TỆP .txt
    // =====================================================================

    public static String readWriteReport() {
        StringBuilder out = new StringBuilder();
        out.append("DEMO ĐỌC/GHI TỆP .txt BẰNG HAI LOẠI LUỒNG\n");
        out.append("=".repeat(64)).append("\n\n");

        String content = SAMPLE_LINE.repeat(LINE_COUNT);
        BenchmarkUtils.ensureTestDir();
        String dir = BenchmarkUtils.getTestDir() + File.separator;
        String bytePath = dir + FILE_BY_BYTE_STREAM;
        String writerPath = dir + FILE_BY_FILE_WRITER;
        String charPath = dir + FILE_BY_CHAR_STREAM;

        try {
            appendWriteSection(out, content, bytePath, writerPath, charPath);
            appendVerifySection(out, bytePath, writerPath, charPath);
            appendReadSection(out, bytePath);
            appendWriteSummary(out);
        } catch (IOException e) {
            out.append("\n[LỖI] Không đọc/ghi được file demo: ").append(e.getMessage()).append("\n");
        } finally {
            // Dọn file demo, không để rác lại trong project
            new File(bytePath).delete();
            new File(writerPath).delete();
            new File(charPath).delete();
        }
        return out.toString();
    }

    /** Phần 1: ghi cùng một nội dung bằng ba cách, đo thời gian. */
    private static void appendWriteSection(StringBuilder out, String content, String bytePath,
                                           String writerPath, String charPath) throws IOException {
        out.append("1) GHI FILE — ").append(LINE_COUNT).append(" dòng tiếng Việt có dấu\n");
        out.append("   Ba cách ghi cùng một nội dung, để thấy ai lo việc encode.\n\n");

        long byteNanos = BenchmarkUtils.medianNanos(() -> writeWithByteStream(bytePath, content));
        out.append("   • ByteStream — FileOutputStream\n");
        out.append("     Thời gian   : ").append(BenchmarkUtils.formatMs(byteNanos)).append("\n");
        out.append("     Cách viết   : fos.write(content.getBytes(StandardCharsets.UTF_8))\n");
        out.append("     Ai encode?  : LẬP TRÌNH VIÊN — phải tự gọi getBytes() kèm charset\n\n");

        long writerNanos = BenchmarkUtils.medianNanos(() -> writeWithFileWriter(writerPath, content));
        out.append("   • CharStream — FileWriter (lớp ghi ký tự dành riêng cho file)\n");
        out.append("     Thời gian   : ").append(BenchmarkUtils.formatMs(writerNanos)).append("\n");
        out.append("     Cách viết   : new FileWriter(path, StandardCharsets.UTF_8)\n");
        out.append("     Ai encode?  : THƯ VIỆN — CharsetEncoder lo, ta chỉ đưa String\n\n");

        long charNanos = BenchmarkUtils.medianNanos(() -> writeWithCharStream(charPath, content));
        out.append("   • CharStream — OutputStreamWriter bọc FileOutputStream\n");
        out.append("     Thời gian   : ").append(BenchmarkUtils.formatMs(charNanos)).append("\n");
        out.append("     Cách viết   : new OutputStreamWriter(new FileOutputStream(p), UTF_8)\n");
        out.append("     Ai encode?  : THƯ VIỆN — giống FileWriter, nhưng bọc được MỌI\n");
        out.append("                   OutputStream (socket, ZIP...) chứ không chỉ file\n\n");
    }

    /** Phần 2: kiểm chứng ba file giống hệt nhau. */
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
    private static void appendReadSection(StringBuilder out, String filePath) throws IOException {
        out.append("3) ĐỌC LẠI FILE VỪA GHI\n\n");

        long bytesRead = StreamBenchmark.readWithByteStream(filePath);
        long charsRead = StreamBenchmark.readWithCharStream(filePath, StandardCharsets.UTF_8);

        out.append("   • ByteStream (FileInputStream) đọc được : ").append(bytesRead).append(" BYTE\n");
        out.append("   • CharStream (FileReader) đọc được      : ").append(charsRead).append(" KÝ TỰ\n\n");
        out.append("   Chênh lệch: ").append(bytesRead - charsRead).append(" — đây KHÔNG phải lỗi.\n");
        out.append("   Ký tự tiếng Việt có dấu chiếm 2-3 byte khi encode UTF-8,\n");
        out.append("   nên một file có số byte nhiều hơn số ký tự. Hai luồng đếm\n");
        out.append("   hai đại lượng KHÁC NHAU trên cùng một file.\n\n");
    }

    /** Phần 4: chốt lại khi nào dùng cái nào. */
    private static void appendWriteSummary(StringBuilder out) {
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
    private static void writeWithByteStream(String filePath, String content) throws IOException {
        // getBytes(UTF_8) chính là bước mà CharStream làm tự động.
        // Nếu gọi getBytes() không tham số, kết quả phụ thuộc charset mặc định.
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(data);
        }
    }

    /**
     * Ghi bằng FileWriter — đối xứng của FileReader mà đề bài nêu.
     * Constructor có tham số Charset chỉ có từ Java 11; trước đó FileWriter KHÔNG
     * cho chỉ định charset nên nhiều tài liệu cũ khuyên tránh nó.
     */
    private static void writeWithFileWriter(String filePath, String content) throws IOException {
        try (Writer writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    /**
     * Ghi bằng OutputStreamWriter bọc FileOutputStream. Kết quả giống FileWriter
     * nhưng bọc được MỌI OutputStream — ví dụ socket của chương trình mạng.
     * Tầng nào làm gì: String → [OutputStreamWriter + Charset] → byte[] → [FileOutputStream] → đĩa
     */
    private static void writeWithCharStream(String filePath, String content) throws IOException {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            writer.write(content);
            // close() của try-with-resources tự gọi flush().
            // Quên flush() khi tự quản lý stream => mất phần dữ liệu cuối buffer.
        }
    }

    // =====================================================================
    // DEMO 2: BẪY ENCODING
    // =====================================================================

    public static String encodingReport() {
        StringBuilder out = new StringBuilder();
        out.append("BẪY ENCODING: CHARSTREAM ĐỌC SAI NHƯ THẾ NÀO\n");
        out.append("=".repeat(64)).append("\n\n");

        appendEnvironmentSection(out);

        BenchmarkUtils.ensureTestDir();
        String demoPath = BenchmarkUtils.getTestDir() + File.separator + ENCODING_FILE;
        try {
            writeWithCharStream(demoPath, ORIGINAL_TEXT);
            byte[] rawBytes = Files.readAllBytes(Path.of(demoPath));

            out.append("2) GHI FILE BẰNG UTF-8 (chỉ định tường minh)\n");
            out.append("   Chuỗi gốc        : ").append(ORIGINAL_TEXT).append("\n");
            out.append("   Số KÝ TỰ (char)  : ").append(ORIGINAL_TEXT.length()).append("\n");
            out.append("   Số BYTE trên đĩa : ").append(rawBytes.length).append("\n");
            out.append("   → Số byte KHÁC số ký tự vì ký tự có dấu chiếm 2-3 byte trong UTF-8.\n");
            out.append("     Đây là lý do ByteStream và CharStream đếm ra 2 con số KHÁC nhau\n");
            out.append("     trên cùng một file: một bên đếm byte, một bên đếm ký tự.\n\n");

            out.append("3) ĐỌC LẠI CÙNG MỘT FILE BẰNG CÁC CHARSET KHÁC NHAU\n");
            out.append("   (file trên đĩa KHÔNG hề thay đổi — chỉ đổi cách diễn giải byte)\n\n");
            appendReadAttempt(out, demoPath, StandardCharsets.UTF_8,
                    "ĐÚNG — khớp với charset đã dùng khi ghi");
            appendReadAttempt(out, demoPath, Charset.forName("windows-1252"),
                    "SAI — mỗi byte thành 1 ký tự Latin => mojibake");
            appendReadAttempt(out, demoPath, StandardCharsets.ISO_8859_1,
                    "SAI — map trọn 256 byte, KHÔNG báo lỗi gì cả");
            appendReadAttempt(out, demoPath, StandardCharsets.US_ASCII,
                    "SAI — byte > 127 bị thay bằng U+FFFD => mất dữ liệu thật sự");

            appendEncodingConclusion(out);
        } catch (UnsupportedCharsetException e) {
            out.append("\n[LỖI] JVM này không hỗ trợ charset: ").append(e.getCharsetName()).append("\n");
        } catch (IOException e) {
            out.append("\n[LỖI] Không đọc/ghi được file demo: ").append(e.getMessage()).append("\n");
        } finally {
            new File(demoPath).delete();
        }
        return out.toString();
    }

    private static void appendEnvironmentSection(StringBuilder out) {
        out.append("1) MÔI TRƯỜNG JVM ĐANG CHẠY\n");
        out.append("   Java version             : ").append(System.getProperty("java.version")).append("\n");
        out.append("   Charset.defaultCharset() : ").append(Charset.defaultCharset().name()).append("\n");
        out.append("   file.encoding            : ")
           .append(System.getProperty("file.encoding", "(không có)")).append("\n");
        // native.encoding = charset thật sự của OS; JDK 18+ tách riêng khỏi file.encoding
        out.append("   native.encoding (của OS) : ")
           .append(System.getProperty("native.encoding", "(không có)")).append("\n");
        out.append("   → Từ JDK 18 (JEP 400), defaultCharset luôn là UTF-8,\n");
        out.append("     còn native.encoding mới là charset thật sự của OS.\n\n");
    }

    /** Đọc file bằng một charset cụ thể rồi so sánh với chuỗi gốc. */
    private static void appendReadAttempt(StringBuilder out, String filePath,
                                          Charset charset, String note) {
        out.append("   • ").append(charset.name()).append("\n");
        String decoded;
        try {
            decoded = readAll(filePath, charset);
        } catch (IOException e) {
            out.append("     Lỗi đọc file: ").append(e.getMessage()).append("\n\n");
            return;
        }
        boolean matches = ORIGINAL_TEXT.equals(decoded);
        long badChars = decoded.chars().filter(c -> c == REPLACEMENT_CHAR).count();

        out.append("     Kết quả đọc  : ").append(truncate(decoded)).append("\n");
        out.append("     Giống gốc?   : ").append(matches ? "CÓ" : "KHÔNG").append("\n");
        out.append("     Số char hỏng : ").append(badChars).append("\n");
        out.append("     Nhận xét     : ").append(note).append("\n\n");
    }

    private static void appendEncodingConclusion(StringBuilder out) {
        out.append("4) KẾT LUẬN & CÁCH VIẾT ĐÚNG\n");
        out.append("   SAI : new FileReader(path)          // phụ thuộc default charset\n");
        out.append("   SAI : new FileWriter(path)          // ghi theo default charset\n");
        out.append("   ĐÚNG: new FileReader(path, StandardCharsets.UTF_8)\n");
        out.append("   ĐÚNG: new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)\n");
        out.append("   ĐÚNG: Files.newBufferedReader(Path.of(path), StandardCharsets.UTF_8)\n\n");
        out.append("   Với file NHỊ PHÂN (ảnh, video, .zip, .exe):\n");
        out.append("   → TUYỆT ĐỐI không dùng CharStream. Decoder sẽ thay byte không hợp lệ\n");
        out.append("     bằng U+FFFD mà KHÔNG ném exception => file hỏng âm thầm.\n");
        out.append("     Chỉ dùng ByteStream (FileInputStream/FileOutputStream).\n");
    }

    /** Đọc toàn bộ file thành String bằng charset chỉ định. */
    private static String readAll(String filePath, Charset charset) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(filePath, charset)) {
            char[] buffer = new char[1024];
            int read;
            while ((read = fr.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
        }
        return sb.toString();
    }

    /** Cắt ngắn chuỗi cho vừa một dòng log. */
    private static String truncate(String text) {
        String oneLine = text.replace('\n', ' ');
        return oneLine.length() <= PREVIEW_LIMIT ? oneLine : oneLine.substring(0, PREVIEW_LIMIT) + "...";
    }
}
