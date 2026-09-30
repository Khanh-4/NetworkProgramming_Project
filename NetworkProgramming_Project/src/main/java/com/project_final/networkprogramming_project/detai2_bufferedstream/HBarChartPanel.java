package com.project_final.networkprogramming_project.detai2_bufferedstream;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Đề tài 2: Biểu đồ thanh ngang, vẽ THEO NHÓM với thang đo riêng từng nhóm.
 * ==========================================================================
 *
 * VÌ SAO PHẢI TÁCH NHÓM?
 * -----------------------
 * Bản trước vẽ cả 6 phương pháp trên CÙNG một trục. Nhưng 2 phương pháp đầu
 * chạy trên file 1MB còn 4 phương pháp sau chạy trên file 10MB. Nhìn biểu đồ
 * cũ sẽ tưởng "Buffered 64KB nhanh hơn Unbuf byte-by-byte 424 lần", trong khi
 * thực tế chúng còn khác nhau cả kích thước file — so sánh như vậy là SAI.
 *
 * Bản này vẽ mỗi nhóm thành một khối riêng, có tiêu đề ghi rõ kích thước file
 * và có thang đo riêng:
 *
 *   ┌ Nhom A - file 1 MB ─────────────────────────┐
 *   │ Unbuf byte-by-byte    ████████████ 2970 ms  │  ← thang riêng của nhóm A
 *   │ Buffered byte-by-byte ▏ 36 ms               │
 *   ├ Nhom B - file 10 MB ────────────────────────┤
 *   │ Unbuf chunk 8KB       ████████████ 26 ms    │  ← thang riêng của nhóm B
 *   │ Buffered 8KB          █████ 11 ms           │
 *   └─────────────────────────────────────────────┘
 *
 * Thang riêng từng nhóm giúp nhóm B không bị cột 2970 ms của nhóm A nén bẹp
 * thành những vạch không nhìn thấy gì.
 *
 * @author Cao Duy Quốc Khánh
 */
public class HBarChartPanel extends JPanel {

    /** Tiêu đề từng nhóm, ví dụ "Nhom A - file 1 MB". */
    private String[] groupTitles;

    /** Nhãn các thanh trong từng nhóm: [nhóm][thanh]. */
    private String[][] barLabels;

    /** Giá trị các thanh trong từng nhóm, quyết định ĐỘ DÀI thanh: [nhóm][thanh]. */
    private double[][] values;

    /**
     * Chuỗi hiển thị cạnh mỗi thanh: [nhóm][thanh].
     *
     * Tách khỏi {@link #values} vì đơn vị do bên gọi quyết định — biểu đồ này
     * vẽ tốc độ (MB/s) nhưng vẫn muốn ghi kèm thời gian (ms) cho dễ đối chiếu,
     * mà panel thì không nên biết gì về ý nghĩa của con số.
     */
    private String[][] valueTexts;

    /** Màu cho từng phương pháp, dùng nối tiếp qua các nhóm. */
    private static final Color[] COLORS = {
        new Color(220, 53, 69),   // đỏ   - Unbuf byte-by-byte
        new Color(255, 193, 7),   // vàng - Buffered byte-by-byte
        new Color(255, 159, 64),  // cam  - Unbuf chunk 8KB
        new Color(0, 123, 255),   // xanh dương - Buffered 8KB
        new Color(32, 201, 151),  // xanh lá    - Buffered 32KB
        new Color(111, 66, 193),  // tím        - Buffered 64KB
    };

    // ===== Hằng số bố cục =====
    private static final int PAD_LEFT = 170;
    // Đủ rộng cho chuỗi dạng "1232 MB/s · 8.12 ms" nằm bên phải thanh
    private static final int PAD_RIGHT = 165;
    private static final int PAD_TOP = 14;
    private static final int PAD_BOTTOM = 14;
    private static final int GROUP_TITLE_HEIGHT = 24;
    private static final int GROUP_SPACING = 6;
    private static final int BAR_GAP = 6;
    /**
     * Chặn trên chiều cao mỗi thanh (px).
     *
     * Chỉ có 6 thanh nên nếu để chúng chia đều một panel cao ~1000px thì mỗi
     * thanh dày tới 150px, trông thô. Nhưng chặn ở mức thấp (46px như bản trước)
     * lại khiến nội dung chỉ lấp được 1/3 panel, thừa hai mảng trắng trên dưới.
     * 100px là mức cân bằng: thanh vẫn ra hình thanh, mà lấp được phần lớn panel.
     */
    private static final int MAX_BAR_HEIGHT = 100;
    private static final int MIN_BAR_HEIGHT = 8;
    private static final int MIN_BAR_WIDTH = 3;
    private static final double HEADROOM = 1.12; // chừa chỗ cho nhãn giá trị

    /**
     * Nạp dữ liệu biểu đồ.
     * Mọi mảng đều được sao chép để panel không giữ tham chiếu tới mảng bên ngoài.
     *
     * @param groupTitles tiêu đề từng nhóm
     * @param barLabels   nhãn các thanh [nhóm][thanh]
     * @param values      giá trị quyết định độ dài thanh [nhóm][thanh]
     * @param valueTexts  chuỗi hiển thị cạnh thanh, đã kèm đơn vị [nhóm][thanh]
     */
    public void setData(String[] groupTitles, String[][] barLabels,
                        double[][] values, String[][] valueTexts) {
        this.groupTitles = groupTitles.clone();

        // Mảng 2 chiều: clone() chỉ sao chép tầng ngoài → phải sao chép từng hàng
        this.barLabels = new String[barLabels.length][];
        this.values = new double[values.length][];
        this.valueTexts = new String[valueTexts.length][];
        for (int i = 0; i < barLabels.length; i++) {
            this.barLabels[i] = barLabels[i].clone();
            this.values[i] = values[i].clone();
            this.valueTexts[i] = valueTexts[i].clone();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!hasData()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int chartW = getWidth() - PAD_LEFT - PAD_RIGHT;
        int availableH = getHeight() - PAD_TOP - PAD_BOTTOM;
        if (chartW <= 0 || availableH <= 0) {
            return; // Panel quá nhỏ, chưa vẽ được
        }

        g2.setColor(Color.WHITE);
        g2.fillRect(PAD_LEFT, PAD_TOP, chartW, availableH);

        int barHeight = computeBarHeight(availableH);

        // Căn GIỮA theo chiều dọc: chiều cao mỗi thanh bị chặn trên bởi
        // MAX_BAR_HEIGHT nên khi panel cao, nội dung không lấp hết. Nếu vẽ từ mép
        // trên thì phần dư dồn hết xuống dưới thành một mảng trắng lớn.
        int contentH = computeContentHeight(barHeight);
        int y = PAD_TOP + Math.max((availableH - contentH) / 2, 0);

        int colorIndex = 0;

        for (int gIdx = 0; gIdx < groupTitles.length; gIdx++) {
            y = drawGroupTitle(g2, groupTitles[gIdx], y, chartW);

            // Mỗi nhóm có thang đo RIÊNG → không so sánh nhầm giữa 2 kích thước file
            double scaleMax = findMax(values[gIdx]) * HEADROOM;

            for (int bIdx = 0; bIdx < values[gIdx].length; bIdx++) {
                drawBar(g2, barLabels[gIdx][bIdx], values[gIdx][bIdx],
                        valueTexts[gIdx][bIdx], scaleMax,
                        chartW, y, barHeight, COLORS[colorIndex % COLORS.length]);
                y += barHeight + BAR_GAP;
                colorIndex++;
            }

            y += GROUP_SPACING;
        }
    }

    /** Có đủ dữ liệu để vẽ hay chưa. */
    private boolean hasData() {
        return groupTitles != null && barLabels != null && values != null
                && valueTexts != null && groupTitles.length > 0;
    }

    /** Tính chiều cao mỗi thanh sao cho tất cả nhóm vừa khít panel. */
    private int computeBarHeight(int availableH) {
        int totalBars = countBars();
        if (totalBars == 0) {
            return MIN_BAR_HEIGHT;
        }

        int overhead = groupTitles.length * (GROUP_TITLE_HEIGHT + GROUP_SPACING)
                + totalBars * BAR_GAP;
        int height = (availableH - overhead) / totalBars;

        return Math.max(Math.min(height, MAX_BAR_HEIGHT), MIN_BAR_HEIGHT);
    }

    /** Tổng chiều cao thực tế của nội dung, dùng để căn giữa theo chiều dọc. */
    private int computeContentHeight(int barHeight) {
        return groupTitles.length * (GROUP_TITLE_HEIGHT + GROUP_SPACING)
                + countBars() * (barHeight + BAR_GAP);
    }

    /** Đếm tổng số thanh của mọi nhóm. */
    private int countBars() {
        int total = 0;
        for (double[] group : values) {
            total += group.length;
        }
        return total;
    }

    /** Tìm giá trị lớn nhất trong một nhóm (tối thiểu 1 để không chia 0). */
    private double findMax(double[] group) {
        double max = 1;
        for (double v : group) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    /**
     * Vẽ tiêu đề nhóm kèm đường kẻ ngang phân tách.
     *
     * @return toạ độ y cho thanh đầu tiên của nhóm
     */
    private int drawGroupTitle(Graphics2D g2, String title, int y, int chartW) {
        g2.setColor(new Color(52, 58, 64));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString(title, 8, y + 14);

        g2.setColor(new Color(210, 210, 210));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(8, y + 19, PAD_LEFT + chartW, y + 19);

        return y + GROUP_TITLE_HEIGHT;
    }

    /** Vẽ một thanh ngang kèm nhãn trái và giá trị phải. */
    private void drawBar(Graphics2D g2, String label, double value, String valueText,
                         double scaleMax, int chartW, int y, int barHeight, Color color) {
        int barW = Math.max((int) (value * chartW / scaleMax), MIN_BAR_WIDTH);

        g2.setColor(color);
        g2.fillRoundRect(PAD_LEFT + 1, y, barW, barHeight, 5, 5);
        g2.setColor(color.darker());
        g2.drawRoundRect(PAD_LEFT + 1, y, barW, barHeight, 5, 5);

        // Nhãn phương pháp bên trái
        g2.setColor(Color.DARK_GRAY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, PAD_LEFT - fm.stringWidth(label) - 8, y + barHeight / 2 + 4);

        // Giá trị bên phải thanh — chuỗi do bên gọi định dạng sẵn kèm đơn vị
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString(valueText, PAD_LEFT + barW + 6, y + barHeight / 2 + 4);
    }
}
