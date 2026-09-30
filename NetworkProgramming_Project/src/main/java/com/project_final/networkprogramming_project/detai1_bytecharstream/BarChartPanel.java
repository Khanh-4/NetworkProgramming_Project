package com.project_final.networkprogramming_project.detai1_bytecharstream;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import javax.swing.JPanel;

/**
 * Đề tài 1: Component vẽ biểu đồ Bar Chart nhóm (grouped bar chart).
 * ===================================================================
 *
 * Vẽ được SỐ SERIES BẤT KỲ, không cố định 2 series như bản đầu.
 * Lý do phải tổng quát hoá: đề bài yêu cầu so sánh tốc độ đọc
 * "file nhị phân vs ký tự", nên cần 4 series cùng lúc:
 *
 *   ByteStream/TEXT | CharStream/TEXT | ByteStream/BINARY | CharStream/BINARY
 *
 * Giá trị nhận vào là {@code double} (millisecond) chứ không phải long,
 * vì đọc file 1 MB từ page cache có thể mất < 1 ms → nếu làm tròn về long
 * thì mọi cột đều bằng 0 và biểu đồ vô nghĩa.
 *
 * Bố cục:
 *
 *   ms ┤ ▓          ▓                        ← trục Y tự scale theo max
 *      ┤ ▓ █        ▓ █
 *      ┤ ▓ █ ▒ ░    ▓ █ ▒ ░
 *      └─────────────────────────
 *          1 MB       5 MB        ← mỗi nhóm = 1 kích thước file
 *
 * @author Cao Duy Quốc Khánh
 */
public class BarChartPanel extends JPanel {

    /** Nhãn trục X, mỗi nhãn là một nhóm cột (ví dụ "1 MB"). */
    private String[] xLabels;

    /** Tên từng series, hiển thị ở legend. */
    private String[] seriesNames;

    /** Màu từng series, cùng thứ tự với seriesNames. */
    private Color[] seriesColors;

    /** Dữ liệu [series][nhóm], đơn vị millisecond. */
    private double[][] values;

    // ===== Hằng số bố cục (tránh magic number rải rác trong paintComponent) =====
    private static final int PAD_LEFT = 58;
    private static final int PAD_RIGHT = 18;
    private static final int PAD_TOP = 58;
    private static final int PAD_BOTTOM = 42;
    private static final int GRID_LINES = 4;
    private static final int BAR_GAP = 2;
    private static final int MAX_BAR_WIDTH = 26;
    private static final int MIN_BAR_HEIGHT = 3;
    private static final double HEADROOM = 1.25; // chừa 25% phía trên cho nhãn

    /**
     * Nạp dữ liệu cho biểu đồ.
     *
     * Mọi mảng đều được CLONE trước khi lưu: panel không giữ tham chiếu tới
     * mảng của bên gọi, nên bên gọi có sửa mảng sau đó cũng không làm biểu đồ
     * đổi dữ liệu ngầm (nguyên tắc immutability).
     *
     * @param xLabels      nhãn trục X
     * @param seriesNames  tên các series
     * @param seriesColors màu các series
     * @param values       dữ liệu [series][nhóm], đơn vị ms
     */
    public void setData(String[] xLabels, String[] seriesNames,
                        Color[] seriesColors, double[][] values) {
        this.xLabels = xLabels.clone();
        this.seriesNames = seriesNames.clone();
        this.seriesColors = seriesColors.clone();

        // Mảng 2 chiều: clone() chỉ copy tầng ngoài → phải copy từng hàng
        this.values = new double[values.length][];
        for (int i = 0; i < values.length; i++) {
            this.values[i] = values[i].clone();
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
        int chartH = getHeight() - PAD_TOP - PAD_BOTTOM;
        if (chartW <= 0 || chartH <= 0) {
            return; // Panel quá nhỏ, chưa vẽ được
        }

        // Nền trắng cho vùng chart
        g2.setColor(Color.WHITE);
        g2.fillRect(PAD_LEFT, PAD_TOP, chartW, chartH);

        double maxVal = findMax() * HEADROOM;

        drawGrid(g2, chartW, chartH, maxVal);
        drawAxes(g2, chartW, chartH);
        drawBars(g2, chartW, chartH, maxVal);
        drawLegend(g2);
    }

    /** Có đủ dữ liệu để vẽ hay chưa. */
    private boolean hasData() {
        return xLabels != null && seriesNames != null
                && seriesColors != null && values != null
                && values.length > 0 && xLabels.length > 0;
    }

    /** Tìm giá trị lớn nhất để scale trục Y (tối thiểu 1 để không chia 0). */
    private double findMax() {
        double max = 1;
        for (double[] series : values) {
            for (double v : series) {
                if (v > max) {
                    max = v;
                }
            }
        }
        return max;
    }

    /** Vẽ đường kẻ ngang và nhãn trục Y. */
    private void drawGrid(Graphics2D g2, int chartW, int chartH, double maxVal) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i <= GRID_LINES; i++) {
            int y = PAD_TOP + chartH - (i * chartH / GRID_LINES);

            g2.setColor(new Color(230, 230, 230));
            g2.drawLine(PAD_LEFT, y, PAD_LEFT + chartW, y);

            g2.setColor(Color.GRAY);
            String label = String.format("%.1f", maxVal * i / GRID_LINES);
            g2.drawString(label, PAD_LEFT - fm.stringWidth(label) - 5, y + 4);
        }

        // Đơn vị trục Y
        g2.setColor(Color.DARK_GRAY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("ms", 6, PAD_TOP - 8);
    }

    /** Vẽ trục X và trục Y. */
    private void drawAxes(Graphics2D g2, int chartW, int chartH) {
        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(PAD_LEFT, PAD_TOP + chartH, PAD_LEFT + chartW, PAD_TOP + chartH);
        g2.drawLine(PAD_LEFT, PAD_TOP, PAD_LEFT, PAD_TOP + chartH);
        g2.setStroke(new BasicStroke(1f));
    }

    /** Vẽ các cột theo nhóm, kèm nhãn giá trị dựng đứng. */
    private void drawBars(Graphics2D g2, int chartW, int chartH, double maxVal) {
        int seriesCount = values.length;
        int groupCount = xLabels.length;
        int groupWidth = chartW / groupCount;

        // Chia đều chỗ cho các cột trong nhóm, chừa 20% làm khoảng trống giữa nhóm
        int barWidth = Math.max((int) (groupWidth * 0.8 / seriesCount) - BAR_GAP, 2);
        barWidth = Math.min(barWidth, MAX_BAR_WIDTH);

        int clusterWidth = seriesCount * (barWidth + BAR_GAP) - BAR_GAP;

        for (int gIdx = 0; gIdx < groupCount; gIdx++) {
            int groupStartX = PAD_LEFT + gIdx * groupWidth
                    + (groupWidth - clusterWidth) / 2;

            for (int sIdx = 0; sIdx < seriesCount; sIdx++) {
                double value = values[sIdx][gIdx];
                int barH = Math.max((int) (value * chartH / maxVal), MIN_BAR_HEIGHT);
                int barX = groupStartX + sIdx * (barWidth + BAR_GAP);
                int barY = PAD_TOP + chartH - barH;

                g2.setColor(seriesColors[sIdx]);
                g2.fillRoundRect(barX, barY, barWidth, barH, 3, 3);

                drawRotatedValue(g2, value, barX, barY, barWidth);
            }

            // Nhãn trục X cho cả nhóm
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            String xLabel = xLabels[gIdx];
            int labelW = g2.getFontMetrics().stringWidth(xLabel);
            g2.drawString(xLabel, groupStartX + (clusterWidth - labelW) / 2,
                    PAD_TOP + chartH + 18);
        }
    }

    /**
     * Vẽ nhãn giá trị DỰNG ĐỨNG phía trên cột.
     *
     * Với 4 series, các cột rất sát nhau nên nhãn nằm ngang sẽ đè lên nhau.
     * Quay 90 độ giúp đọc được mọi nhãn mà không cần nới rộng panel.
     */
    private void drawRotatedValue(Graphics2D g2, double value,
                                  int barX, int barY, int barWidth) {
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));

        String label = String.format("%.1f", value);

        // Lưu transform gốc để phục hồi sau khi quay, tránh ảnh hưởng lần vẽ sau
        AffineTransform original = g2.getTransform();
        g2.translate(barX + barWidth / 2.0 + 3, barY - 3);
        g2.rotate(-Math.PI / 2); // quay -90 độ => chữ chạy từ dưới lên
        g2.drawString(label, 0, 0);
        g2.setTransform(original);
    }

    /** Vẽ legend ở góc trên trái, tự xuống dòng khi hết chỗ. */
    private void drawLegend(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();

        final int boxSize = 11;
        final int rowHeight = 15;
        int x = PAD_LEFT + 6;
        int y = 8;
        int maxX = getWidth() - PAD_RIGHT;

        for (int i = 0; i < seriesNames.length; i++) {
            int entryWidth = boxSize + 4 + fm.stringWidth(seriesNames[i]) + 12;

            // Hết chỗ theo chiều ngang => xuống dòng mới
            if (x + entryWidth > maxX) {
                x = PAD_LEFT + 6;
                y += rowHeight;
            }

            g2.setColor(seriesColors[i]);
            g2.fillRoundRect(x, y, boxSize, boxSize, 2, 2);

            g2.setColor(Color.BLACK);
            g2.drawString(seriesNames[i], x + boxSize + 4, y + boxSize - 1);

            x += entryWidth;
        }
    }
}
