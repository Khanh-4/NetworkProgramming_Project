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
    // Lấy từ CopyBenchmark để chỉ có MỘT nơi định nghĩa kích thước file test
    private static final int SMALL_FILE_MB = CopyBenchmark.SMALL_FILE_MB;
    private static final int LARGE_FILE_MB = CopyBenchmark.LARGE_FILE_MB;

    // Components
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private HBarChartPanel chartPanel;
    private JButton btnRun;
    private JTextArea txtLog;
    private JLabel lblStatus;
    private JProgressBar progressBar;

    /** Kết quả benchmark, immutable, mỗi phần tử là một phương pháp sao chép. */
    private CopyResult[] results;

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

        // ========== CENTER ==========
        // Bố cục: cột TRÁI = bảng kết quả ở trên + các tab log/lý thuyết ở dưới,
        //         cột PHẢI = biểu đồ chiếm TRỌN chiều cao.
        // Xem StreamComparisonGUI (đề tài 1) để biết lý do bỏ BorderLayout.SOUTH.
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setResizeWeight(0.48);

        // --- Cột trái: bảng ở trên, tab ở dưới ---
        JPanel tablePanel = createTablePanel();
        // Bảng chỉ 6 dòng nên không cần cao; phần dư nhường cho các tab
        tablePanel.setPreferredSize(new Dimension(560, 250));

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSplit.setResizeWeight(0.26); // giãn cửa sổ thì tab nở, bảng giữ nguyên
        leftSplit.setTopComponent(tablePanel);
        leftSplit.setBottomComponent(createBottomPanel());
        centerSplit.setLeftComponent(leftSplit);

        // --- Cột phải: biểu đồ ---
        chartPanel = new HBarChartPanel();
        JPanel chartWrapper = new JPanel(new BorderLayout());
        chartWrapper.setBorder(BorderFactory.createTitledBorder("Bieu do toc do I/O (ms)"));
        chartWrapper.add(chartPanel, BorderLayout.CENTER);
        centerSplit.setRightComponent(chartWrapper);

        mainPanel.add(centerSplit, BorderLayout.CENTER);

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

        // Cot cuoi co DON VI KHAC NHAU tuy nhom (nhom A: boi so, nhom B: MB/s)
        // nen tieu de phai trung tinh, khong duoc ghi cung "Thong luong".
        String[] columns = {"Nhom", "Phuong phap", "File", "Thoi gian (ms)", "So sanh trong nhom"};
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
        // Không đặt preferredSize cố định nữa: các tab nay nằm trong JSplitPane
        // dọc ở cột trái nên tự lấy hết phần chiều cao còn lại.
        JTabbedPane tabs = new JTabbedPane();

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
            + "   OK Luon dung try-with-resources de tu dong dong stream\n\n"
            + "5. CACH DO DE SO LIEU DANG TIN:\n"
            + "   Do 1 lan la SAI: lan chay dau con cache lanh + JIT chua bien dich,\n"
            + "   va o muc 7-11 ms thi rieng sai so lam tron da la +/-14%.\n"
            + "   Nhom B chi chenh nhau vai ms nen rat de dao thu tu neu do au.\n"
            + "   Xu ly: moi phuong phap deu qua MedianTimer (warm-up + trung vi),\n"
            + "   va thoi gian luu bang NANO-giay thay vi long millisecond.\n\n"
            + "6. HAI NHOM KHONG SO CHEO NHAU:\n"
            + "   Nhom A chay file 1MB, nhom B chay file 10MB (byte-by-byte qua cham\n"
            + "   nen khong the dung file lon). Vi vay bieu do ve tach 2 khoi, moi\n"
            + "   khoi mot thang do rieng. So dong cua nhom A voi nhom B la SAI.\n\n"
            + "7. KET QUA PHU THUOC NEN TANG - DUNG KET LUAN VOI:\n"
            + "   Ket qua nhom A (buffer cuu code doc tung byte) rat on dinh,\n"
            + "   lap lai bao nhieu lan cung ra cung ket luan.\n"
            + "   Nhung o nhom B, THU TU xep hang giua 8KB / 32KB / 64KB co the\n"
            + "   DAO NGUOC giua cac lan chay va giua cac he dieu hanh, vi chung\n"
            + "   chi chenh nhau vai ms. Vay nen:\n"
            + "   - DUNG ket luan 'buffer cang lon cang nhanh' tu mot lan chay\n"
            + "   - Chi ket luan duoc rang o nhom B chenh lech DA NHO di nhieu\n"
            + "     so voi nhom A\n"
            + "   - Muon chon co buffer toi uu thi phai DO tren moi truong that\n"
        );
        tabs.addTab("Ly thuyet", new JScrollPane(theory));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Chạy benchmark trên background thread để không đóng băng giao diện.
     */
    private void runBenchmark() {
        btnRun.setEnabled(false);
        lblStatus.setText("Dang chay...");
        progressBar.setValue(0);
        progressBar.setIndeterminate(true); // khong biet truoc bao lau vi co warm-up
        tableModel.setRowCount(0);
        txtLog.setText("");

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws IOException {
                publish("Bat dau benchmark (co warm-up + lay trung vi)...");
                publish("Luu y: moi phuong phap chay nhieu lan nen se lau hon truoc.\n");

                // Toan bo phep do giao cho CopyBenchmark, GUI chi hien thi
                results = CopyBenchmark.runAll(TEST_DIR, this::publish);

                publish("\nDon dep file test...");
                com.project_final.networkprogramming_project.detai1_bytecharstream
                        .TestFileUtils.cleanupTestFiles();
                publish("Hoan tat!");
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String msg : chunks) {
                    txtLog.append(msg + "\n");
                    txtLog.setCaretPosition(txtLog.getDocument().getLength());
                }
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);

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

                fillResultTable();
                updateChart();

                progressBar.setValue(100);
                btnRun.setEnabled(true);
                lblStatus.setText("Benchmark hoan tat!");
            }
        };

        worker.execute();
    }

    /**
     * Đổ kết quả vào bảng.
     *
     * Cột cuối chỉ có nghĩa khi so sánh TRONG CÙNG một nhóm, vì hai nhóm chạy
     * trên hai kích thước file khác nhau. Cột "Nhom" có để người đọc thấy ngay
     * ranh giới đó thay vì so nhầm dòng 1 với dòng 6.
     *
     * Hai nhóm dùng HAI ĐƠN VỊ khác nhau nên đơn vị được ghi thẳng vào từng ô:
     *   - Nhóm A: bội số so với bản không buffer → trả lời "buffer lợi mấy lần?"
     *   - Nhóm B: MB/s → trả lời "đạt được bao nhiêu thông lượng?"
     * Ghi đơn vị vào ô thay vì vào tiêu đề cột, vì một tiêu đề không thể đúng
     * cho cả hai nhóm cùng lúc.
     */
    private void fillResultTable() {
        if (results == null) {
            return;
        }

        CopyResult slowestSmall = results[0]; // Unbuf byte-by-byte, moc so sanh nhom A

        for (CopyResult r : results) {
            boolean isGroupA = r.fileSizeMB() == SMALL_FILE_MB;

            String metric = isGroupA
                    ? String.format("%.1fx nhanh hon", r.speedupOver(slowestSmall))
                    : String.format("%.0f MB/s", r.throughputMBps());

            tableModel.addRow(new Object[]{
                isGroupA ? "A" : "B",
                r.methodName(),
                r.fileSizeMB() + " MB",
                String.format("%.2f", r.ms()),
                metric
            });
        }
    }

    /**
     * Vẽ biểu đồ theo 2 nhóm tách biệt, mỗi nhóm một thang đo riêng.
     */
    private void updateChart() {
        if (results == null) {
            return;
        }

        CopyResult[] groupA = CopyBenchmark.filterBySize(results, SMALL_FILE_MB);
        CopyResult[] groupB = CopyBenchmark.filterBySize(results, LARGE_FILE_MB);

        String[] groupTitles = {
            "Nhom A - doc/ghi tung byte (file " + SMALL_FILE_MB + " MB)",
            "Nhom B - doc theo khoi (file " + LARGE_FILE_MB + " MB)"
        };
        String[][] barLabels = {toLabels(groupA), toLabels(groupB)};
        double[][] values = {toMillis(groupA), toMillis(groupB)};

        chartPanel.setData(groupTitles, barLabels, values);
        chartPanel.repaint();
    }

    /** Trích tên phương pháp để làm nhãn biểu đồ. */
    private static String[] toLabels(CopyResult[] group) {
        String[] labels = new String[group.length];
        for (int i = 0; i < group.length; i++) {
            labels[i] = group[i].methodName();
        }
        return labels;
    }

    /** Trích thời gian (ms) để vẽ biểu đồ. */
    private static double[] toMillis(CopyResult[] group) {
        double[] millis = new double[group.length];
        for (int i = 0; i < group.length; i++) {
            millis[i] = group[i].ms();
        }
        return millis;
    }


    private void reportFailure(String message) {
        txtLog.append("[LOI] " + message + "\n");
        lblStatus.setText(message);
        progressBar.setValue(0);
        btnRun.setEnabled(true);
    }

    // Bốn method sao chép trước đây nằm ở đây đã bị xoá: chúng lặp lại y nguyên
    // logic của UnbufferedCopy và BufferedCopy, lại còn tự đo thời gian 1 lần
    // bằng long millisecond. Nay mọi phép đo đi qua CopyBenchmark + MedianTimer.

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

