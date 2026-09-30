package com.project_final.networkprogramming_project.detai2_bufferedstream;

import java.awt.*;
import javax.swing.*;

/**
 * Đề tài 2: Component vẽ biểu đồ Horizontal Bar Chart so sánh các phương pháp I/O.
 * Tách riêng khỏi GUI chính.
 */
public class HBarChartPanel extends JPanel {
    private String[] names;
    private long[] data;

    // Màu cho từng phương pháp
    private static final Color[] COLORS = {
        new Color(220, 53, 69),   // Đỏ  - Unbuf byte
        new Color(255, 193, 7),   // Vàng - Buf byte
        new Color(255, 159, 64),  // Cam  - Unbuf chunk
        new Color(0, 123, 255),   // Xanh dương - Buf 8K
        new Color(32, 201, 151),  // Xanh lá - Buf 32K
        new Color(111, 66, 193),  // Tím  - Buf 64K
    };

    public void setData(String[] names, long[] data) {
        this.names = names;
        this.data = data;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (names == null || data == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int padLeft = 160;  // Đủ rộng cho tên phương pháp
        int padRight = 70;  // Đủ rộng cho giá trị ms
        int padTop = 35;
        int padBottom = 20;
        int chartW = w - padLeft - padRight;
        int chartH = h - padTop - padBottom;

        // Nền trắng
        g2.setColor(Color.WHITE);
        g2.fillRect(padLeft, padTop, chartW, chartH);

        // Tiêu đề
        g2.setColor(new Color(52, 58, 64));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString("Thoi gian (ms) - Cang thap cang tot", padLeft, padTop - 10);

        // Tìm max
        long maxVal = 1;
        for (long v : data) maxVal = Math.max(maxVal, v);
        maxVal = (long)(maxVal * 1.1); // 10% padding

        // Vẽ gridlines dọc
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        for (int i = 0; i <= 5; i++) {
            int x = padLeft + (i * chartW / 5);
            g2.setColor(new Color(235, 235, 235));
            g2.drawLine(x, padTop, x, padTop + chartH);
            g2.setColor(Color.GRAY);
            long val = maxVal * i / 5;
            String valStr = val + "";
            g2.drawString(valStr, x - g2.getFontMetrics().stringWidth(valStr) / 2,
                    padTop + chartH + 15);
        }

        // Vẽ trục
        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(padLeft, padTop, padLeft, padTop + chartH);
        g2.drawLine(padLeft, padTop + chartH, padLeft + chartW, padTop + chartH);
        g2.setStroke(new BasicStroke(1f));

        // Vẽ horizontal bars
        int barCount = data.length;
        int totalGap = (barCount + 1) * 6;
        int barHeight = Math.min((chartH - totalGap) / barCount, 35);
        int startY = padTop + (chartH - barCount * barHeight - (barCount - 1) * 6) / 2;

        for (int i = 0; i < barCount; i++) {
            int y = startY + i * (barHeight + 6);

            // Chiều dài bar
            int barW = Math.max((int) (data[i] * chartW / maxVal), 4);

            // Vẽ bar với bo góc
            g2.setColor(COLORS[i % COLORS.length]);
            g2.fillRoundRect(padLeft + 1, y, barW, barHeight, 5, 5);

            // Viền nhẹ
            g2.setColor(COLORS[i % COLORS.length].darker());
            g2.drawRoundRect(padLeft + 1, y, barW, barHeight, 5, 5);

            // Label bên trái (tên phương pháp)
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            String label = names[i];
            int labelW = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, padLeft - labelW - 8, y + barHeight / 2 + 4);

            // Giá trị bên phải bar
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            String valStr = data[i] + " ms";
            g2.drawString(valStr, padLeft + barW + 6, y + barHeight / 2 + 4);
        }
    }
}
