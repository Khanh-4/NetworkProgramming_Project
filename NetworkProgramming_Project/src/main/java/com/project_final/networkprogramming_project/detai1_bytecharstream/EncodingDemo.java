package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Đề tài 1: BẪY ENCODING — vì sao CharStream "tự xử lý encoding" vẫn đọc sai.
 * ===========================================================================
 *
 * Đây là điểm khác biệt QUAN TRỌNG NHẤT giữa ByteStream và CharStream,
 * quan trọng hơn cả chuyện nhanh/chậm:
 *
 *   ByteStream (FileInputStream) : trả về ĐÚNG byte có trong file. Không diễn giải.
 *   CharStream (FileReader)      : PHẢI diễn giải byte thành ký tự bằng một charset.
 *                                  Đoán sai charset => đọc ra ký tự rác (mojibake).
 *
 * Sơ đồ luồng decode cho ký tự 'ế' (UTF-8 = 3 byte: E1 BA BF):
 *
 *   File trên đĩa      Kernel        CharsetDecoder        Ứng dụng nhận được
 *   [E1 BA BF]    →   byte[]   →   (UTF-8)          →    'ế'        ✅ đúng
 *   [E1 BA BF]    →   byte[]   →   (windows-1252)   →    "áº¿"      ❌ sai
 *
 * LƯU Ý VỀ JDK 18+ (JEP 400):
 *   Trước JDK 18: Charset.defaultCharset() phụ thuộc locale của OS
 *                 (máy Windows tiếng Việt thường là windows-1252/1258)
 *                 => cùng một đoạn code chạy trên 2 máy cho 2 kết quả khác nhau.
 *   Từ JDK 18   : default charset được cố định thành UTF-8 trên MỌI hệ điều hành.
 *                 => bẫy này nhẹ đi, NHƯNG chưa hết, vì:
 *                   - Đọc file do hệ thống cũ sinh ra (vẫn là cp1252) vẫn sai.
 *                   - Property native.encoding vẫn theo OS (dùng cho console).
 *                   - Chạy trên JDK cũ hơn 18 thì bẫy quay lại nguyên vẹn.
 *   => KẾT LUẬN THỰC HÀNH: luôn truyền charset tường minh, đừng tin giá trị mặc định.
 *
 * @author Cao Duy Quốc Khánh
 */
public final class EncodingDemo {

    /**
     * Câu tiếng Việt CÓ DẤU, dùng làm dữ liệu gốc để kiểm chứng.
     * Bắt buộc phải có dấu: ký tự có dấu chiếm 2-3 byte trong UTF-8,
     * nhờ đó mới thấy được "số byte khác số ký tự" và mới tái hiện được mojibake.
     */
    private static final String ORIGINAL_TEXT =
            "Lập trình mạng: luồng byte và luồng ký tự — Tiếng Việt có dấu: ă â đ ê ô ơ ư";

    /** Tên file demo, đặt trong thư mục test chung. */
    private static final String DEMO_FILE_NAME = "encoding_demo_utf8.txt";

    /** Ký tự thay thế U+FFFD mà decoder chèn vào khi gặp byte không decode được. */
    private static final char REPLACEMENT_CHAR = '�';

    /** Độ dài tối đa khi in một dòng kết quả decode ra log. */
    private static final int PREVIEW_LIMIT = 58;

    private EncodingDemo() {
    }

    /** Chạy demo và in báo cáo ra console. */
    public static void run() {
        System.out.println(buildReport());
    }

    /**
     * Sinh báo cáo dạng text (console in trực tiếp, GUI đưa vào tab riêng).
     * Tách ra khỏi việc in để không phải viết logic 2 lần cho 2 giao diện.
     */
    public static String buildReport() {
        StringBuilder out = new StringBuilder();

        out.append("BẪY ENCODING: CHARSTREAM ĐỌC SAI NHƯ THẾ NÀO\n");
        out.append("=".repeat(64)).append("\n\n");

        // ---- Phần 1: môi trường đang chạy ----
        out.append("1) MÔI TRƯỜNG JVM ĐANG CHẠY\n");
        out.append("   Java version             : ")
           .append(System.getProperty("java.version")).append("\n");
        out.append("   Charset.defaultCharset() : ")
           .append(Charset.defaultCharset().name()).append("\n");
        out.append("   file.encoding            : ")
           .append(System.getProperty("file.encoding", "(không có)")).append("\n");
        // native.encoding = charset thật sự của OS; JDK 18+ tách riêng khỏi file.encoding
        out.append("   native.encoding (của OS) : ")
           .append(System.getProperty("native.encoding", "(không có)")).append("\n");
        out.append("   → Từ JDK 18 (JEP 400), defaultCharset luôn là UTF-8,\n");
        out.append("     còn native.encoding mới là charset thật sự của OS.\n\n");

        TestFileUtils.ensureTestDir();
        String demoPath = TestFileUtils.getTestDir() + File.separator + DEMO_FILE_NAME;

        try {
            // ---- Phần 2: ghi file bằng UTF-8 tường minh ----
            writeAsUtf8(demoPath);
            byte[] rawBytes = Files.readAllBytes(Path.of(demoPath));

            out.append("2) GHI FILE BẰNG UTF-8 (chỉ định tường minh)\n");
            out.append("   Chuỗi gốc        : ").append(ORIGINAL_TEXT).append("\n");
            out.append("   Số KÝ TỰ (char)  : ").append(ORIGINAL_TEXT.length()).append("\n");
            out.append("   Số BYTE trên đĩa : ").append(rawBytes.length).append("\n");
            out.append("   → Số byte KHÁC số ký tự vì ký tự có dấu chiếm 2-3 byte trong UTF-8.\n");
            out.append("     Đây là lý do ByteStream và CharStream đếm ra 2 con số KHÁC nhau\n");
            out.append("     trên cùng một file: một bên đếm byte, một bên đếm ký tự.\n\n");

            // ---- Phần 3: đọc lại bằng nhiều charset khác nhau ----
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

            // ---- Phần 4: kết luận ----
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

        } catch (UnsupportedCharsetException e) {
            out.append("\n[LỖI] JVM này không hỗ trợ charset: ")
               .append(e.getCharsetName()).append("\n");
        } catch (IOException e) {
            out.append("\n[LỖI] Không đọc/ghi được file demo: ")
               .append(e.getMessage()).append("\n");
        } finally {
            // Dọn file demo, không để rác lại trong project
            new File(demoPath).delete();
        }

        return out.toString();
    }

    /**
     * Ghi {@link #ORIGINAL_TEXT} xuống file, ép charset UTF-8.
     *
     * Dùng OutputStreamWriter bọc FileOutputStream thay vì FileWriter thuần
     * để thấy rõ tầng nào chịu trách nhiệm encode:
     *   String -> [OutputStreamWriter + Charset] -> byte[] -> [FileOutputStream] -> đĩa
     */
    private static void writeAsUtf8(String filePath) throws IOException {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            writer.write(ORIGINAL_TEXT);
            // close() của try-with-resources tự gọi flush().
            // Bẫy kinh điển: tự quản lý stream rồi quên flush() => mất dữ liệu cuối buffer.
        }
    }

    /**
     * Đọc file bằng một charset cụ thể rồi so sánh với chuỗi gốc,
     * ghi kết quả vào báo cáo.
     */
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
        return oneLine.length() <= PREVIEW_LIMIT
                ? oneLine
                : oneLine.substring(0, PREVIEW_LIMIT) + "...";
    }
}
