package com.project_final.networkprogramming_project.detai2_bufferedstream;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;

/**
 * Đề tài 2: Ứng dụng kỹ thuật luồng đệm (Buffered Stream)
 * ----------------------------------------------------------
 * GUI Swing hiển thị kết quả so sánh Buffered vs Unbuffered.
 * - Bảng JTable kết quả 6 phương pháp sao chép
 * - Biểu đồ bar chart bằng Java2D
 * - Phần lý thuyết giải thích cơ chế buffer
 *
 * @author Cao Duy Quốc Khánh
 */
public class PerformanceChartGUI extends JFrame {

    private static final String TEST_DIR = "testdata";
    private static final int SMALL_FILE_MB = 1;  // File nhỏ cho byte-by-byte
    private static final int LARGE_FILE_MB = 10;  // File lớn cho so sánh buffer

    // Components
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private HBarChartPanel chartPanel;
    private JButton btnRun;
    private JTextArea txtLog;
    private JLabel lblStatus;
    private JProgressBar progressBar;

    // Kết quả
    private String[] methodNames;
    private long[] times;

    public PerformanceChartGUI() {
        setTitle("De Tai 2: Buffered Stream vs Unbuffered Stream - Cao Duy Quoc Khanh");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(245, 245, 250));

        // ========== HEADER ==========
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        // ========== CENTER: Table + Chart ==========
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setResizeWeight(0.5);

        // Bảng kết quả
        centerSplit.setLeftComponent(createTablePanel());

        // Biểu đồ
        chartPanel = new HBarChartPanel();
        JPanel chartWrapper = new JPanel(new BorderLayout());
        chartWrapper.setBorder(BorderFactory.createTitledBorder("Bieu do toc do I/O (ms)"));
        chartWrapper.add(chartPanel, BorderLayout.CENTER);
        centerSplit.setRightComponent(chartWrapper);

        mainPanel.add(centerSplit, BorderLayout.CENTER);

        // ========== BOTTOM ==========
        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setOpaque(false);

        JLabel title = new JLabel("DE TAI 2: BUFFERED STREAM vs UNBUFFERED STREAM");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(new Color(33, 37, 41));

        JLabel subtitle = new JLabel("Toi uu toc do I/O bang BufferedInputStream / BufferedOutputStream");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(108, 117, 125));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(subtitle);

        // Nút chạy + Progress bar
        btnRun = new JButton("Chay Benchmark");
        btnRun.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnRun.setBackground(new Color(40, 167, 69));
        btnRun.setForeground(Color.WHITE);
        btnRun.setFocusPainted(false);
        btnRun.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRun.setPreferredSize(new Dimension(180, 40));
        btnRun.addActionListener(e -> runBenchmark());

        lblStatus = new JLabel("San sang...");
        lblStatus.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblStatus.setForeground(new Color(108, 117, 125));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(180, 20));

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setOpaque(false);
        rightPanel.add(btnRun, BorderLayout.NORTH);
        rightPanel.add(progressBar, BorderLayout.CENTER);
        rightPanel.add(lblStatus, BorderLayout.SOUTH);

        panel.add(titlePanel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Bang ket qua so sanh"));

        String[] columns = {"Phuong phap", "File Size", "Thoi gian (ms)", "Toc do"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        resultTable = new JTable(tableModel);
        resultTable.setRowHeight(28);
        resultTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        resultTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        resultTable.getTableHeader().setBackground(new Color(52, 58, 64));
        resultTable.getTableHeader().setForeground(Color.WHITE);

        // Cột "Phương pháp" rộng hơn
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(200);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 1; i < columns.length; i++) {
            resultTable.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        panel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBottomPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setPreferredSize(new Dimension(0, 200));

        // Tab Log
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtLog.setBackground(new Color(33, 37, 41));
        txtLog.setForeground(new Color(0, 255, 0));
        tabs.addTab("Log", new JScrollPane(txtLog));

        // Tab Lý thuyết
        JTextArea theory = new JTextArea();
        theory.setEditable(false);
        theory.setFont(new Font("SansSerif", Font.PLAIN, 12));
        theory.setLineWrap(true);
        theory.setWrapStyleWord(true);
        theory.setText(
            "LY THUYET: BUFFERED STREAM\n"
            + "=".repeat(50) + "\n\n"
            + "1. Van de cua Unbuffered Stream:\n"
            + "   - Moi read()/write() = 1 system call den OS Kernel\n"
            + "   - System call ton ~1000 CPU cycles (User -> Kernel mode)\n"
            + "   - File 10MB doc byte-by-byte = 10 TRIEU system calls!\n\n"
            + "2. Giai phap: BufferedStream\n"
            + "   - Tao vung dem (buffer) trong RAM, mac dinh 8KB\n"
            + "   - 1 system call doc 8KB vao buffer\n"
            + "   - Cac lan read() tiep lay tu buffer (cuc nhanh)\n"
            + "   - File 10MB chi can ~1,280 system calls (giam 8000x)\n\n"
            + "3. Minh hoa:\n"
            + "   Khong buffer: App <-> Disk (moi byte)\n"
            + "   Co buffer:    App <-> Buffer(RAM) <-> Disk (moi 8KB)\n\n"
            + "4. BAY THUONG GAP:\n"
            + "   X  Quen flush() voi BufferedOutputStream => mat du lieu cuoi\n"
            + "   X  Quen dong stream => resource leak, file bi lock\n"
            + "   OK Luon dung try-with-resources de tu dong dong stream\n"
        );
        tabs.addTab("Ly thuyet", new JScrollPane(theory));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Chạy benchmark trên background thread.
     */
    private void runBenchmark() {
        btnRun.setEnabled(false);
        lblStatus.setText("Dang chay...");
        progressBar.setValue(0);
        tableModel.setRowCount(0);
        txtLog.setText("");

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws IOException {
                methodNames = new String[6];
                times = new long[6];

                File dir = new File(TEST_DIR);
                if (!dir.exists()) dir.mkdirs();

                String srcSmall = TEST_DIR + "/source_" + SMALL_FILE_MB + "MB.dat";
                String srcLarge = TEST_DIR + "/source_" + LARGE_FILE_MB + "MB.dat";

                // Tạo file test
                publish("Tao file test " + SMALL_FILE_MB + "MB...");
                com.project_final.networkprogramming_project.detai1_bytecharstream.TestFileUtils.generateBinaryFile(srcSmall, SMALL_FILE_MB);
                publish("Tao file test " + LARGE_FILE_MB + "MB...");
                com.project_final.networkprogramming_project.detai1_bytecharstream.TestFileUtils.generateBinaryFile(srcLarge, LARGE_FILE_MB);
                setProgress(10);

                // ① Unbuffered byte-by-byte (file nhỏ)
                publish("\n[1/6] Unbuffered byte-by-byte (" + SMALL_FILE_MB + "MB)...");
                methodNames[0] = "Unbuf byte-by-byte";
                times[0] = copyUnbufferedByteByByte(srcSmall, TEST_DIR + "/c1.dat");
                publish("=> " + times[0] + " ms");
                setProgress(25);

                // ② Buffered byte-by-byte (file nhỏ)
                publish("[2/6] Buffered byte-by-byte (" + SMALL_FILE_MB + "MB)...");
                methodNames[1] = "Buffered byte-by-byte";
                times[1] = copyBufferedByteByByte(srcSmall, TEST_DIR + "/c2.dat");
                publish("=> " + times[1] + " ms");
                setProgress(40);

                // ③ Unbuffered chunk 8KB (file lớn)
                publish("[3/6] Unbuffered chunk 8KB (" + LARGE_FILE_MB + "MB)...");
                methodNames[2] = "Unbuf chunk 8KB";
                times[2] = copyUnbufferedChunk(srcLarge, TEST_DIR + "/c3.dat", 8192);
                publish("=> " + times[2] + " ms");
                setProgress(55);

                // ④ Buffered 8KB (file lớn)
                publish("[4/6] Buffered 8KB (" + LARGE_FILE_MB + "MB)...");
                methodNames[3] = "Buffered 8KB";
                times[3] = copyBufferedChunk(srcLarge, TEST_DIR + "/c4.dat", 8192);
                publish("=> " + times[3] + " ms");
                setProgress(70);

                // ⑤ Buffered 32KB (file lớn)
                publish("[5/6] Buffered 32KB (" + LARGE_FILE_MB + "MB)...");
                methodNames[4] = "Buffered 32KB";
                times[4] = copyBufferedChunk(srcLarge, TEST_DIR + "/c5.dat", 32768);
                publish("=> " + times[4] + " ms");
                setProgress(85);

                // ⑥ Buffered 64KB (file lớn)
                publish("[6/6] Buffered 64KB (" + LARGE_FILE_MB + "MB)...");
                methodNames[5] = "Buffered 64KB";
                times[5] = copyBufferedChunk(srcLarge, TEST_DIR + "/c6.dat", 65536);
                publish("=> " + times[5] + " ms");
                setProgress(95);

                // Dọn dẹp
                publish("\nDon dep file test...");
                com.project_final.networkprogramming_project.detai1_bytecharstream.TestFileUtils.cleanupTestFiles();
                publish("Hoan tat!");
                setProgress(100);

                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String msg : chunks) {
                    txtLog.append(msg + "\n");
                    txtLog.setCaretPosition(txtLog.getDocument().getLength());
                }
                progressBar.setValue(getProgress());
            }

            @Override
            protected void done() {
                // get() nem lai exception da xay ra trong doInBackground().
                // Bo buoc nay thi loi tao/doc file se bi im lang, GUI hien bang rong.
                try {
                    get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // giu lai trang thai interrupt
                    reportFailure("Benchmark bi ngat giua chung");
                    return;
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    reportFailure("Loi khi chay benchmark: "
                            + (cause != null ? cause.getMessage() : e.getMessage()));
                    return;
                }

                progressBar.setValue(100);

                // Tìm thời gian nhanh nhất (bỏ qua 2 test đầu vì file khác kích thước)
                String[] fileSizes = {
                    SMALL_FILE_MB + " MB", SMALL_FILE_MB + " MB",
                    LARGE_FILE_MB + " MB", LARGE_FILE_MB + " MB",
                    LARGE_FILE_MB + " MB", LARGE_FILE_MB + " MB"
                };

                for (int i = 0; i < 6; i++) {
                    String speed;
                    if (i == 0 && times[1] > 0) {
                        speed = String.format("1x (co so)");
                    } else if (i == 1 && times[0] > 0) {
                        speed = String.format("%.1fx", (double) times[0] / Math.max(times[1], 1));
                    } else if (i >= 2) {
                        speed = String.format("%.1f MB/s",
                            (double) LARGE_FILE_MB * 1000 / Math.max(times[i], 1));
                    } else {
                        speed = "-";
                    }

                    tableModel.addRow(new Object[]{
                        methodNames[i], fileSizes[i], times[i], speed
                    });
                }

                // Tô màu hàng nhanh nhất trong nhóm file lớn
                chartPanel.setData(methodNames, times);
                chartPanel.repaint();

                btnRun.setEnabled(true);
                lblStatus.setText("Benchmark hoan tat!");
            }
        };

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                progressBar.setValue((Integer) evt.getNewValue());
            }
        });

        worker.execute();
    }

    /** Báo lỗi ra log + status bar và mở lại nút chạy. */
    private void reportFailure(String message) {
        txtLog.append("[LOI] " + message + "\n");
        lblStatus.setText(message);
        progressBar.setValue(0);
        btnRun.setEnabled(true);
    }

    // ===== CÁC PHƯƠNG PHÁP SAO CHÉP =====

    private long copyUnbufferedByteByByte(String src, String dest) {
        long start = System.nanoTime();
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dest)) {
            int b;
            while ((b = fis.read()) != -1) fos.write(b);
            fos.flush();
        } catch (IOException e) { return -1; }
        return (System.nanoTime() - start) / 1_000_000;
    }

    private long copyBufferedByteByByte(String src, String dest) {
        long start = System.nanoTime();
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dest))) {
            int b;
            while ((b = bis.read()) != -1) bos.write(b);
            bos.flush();
        } catch (IOException e) { return -1; }
        return (System.nanoTime() - start) / 1_000_000;
    }

    private long copyUnbufferedChunk(String src, String dest, int chunkSize) {
        long start = System.nanoTime();
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buf = new byte[chunkSize];
            int n;
            while ((n = fis.read(buf)) != -1) fos.write(buf, 0, n);
            fos.flush();
        } catch (IOException e) { return -1; }
        return (System.nanoTime() - start) / 1_000_000;
    }

    private long copyBufferedChunk(String src, String dest, int bufSize) {
        long start = System.nanoTime();
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src), bufSize);
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dest), bufSize)) {
            byte[] buf = new byte[bufSize];
            int n;
            while ((n = bis.read(buf)) != -1) bos.write(buf, 0, n);
            bos.flush();
        } catch (IOException e) { return -1; }
        return (System.nanoTime() - start) / 1_000_000;
    }

    /**
     * Khởi chạy GUI.
     */
    public static void run() {
        SwingUtilities.invokeLater(() -> {
            PerformanceChartGUI gui = new PerformanceChartGUI();
            gui.setVisible(true);
        });
    }
}

