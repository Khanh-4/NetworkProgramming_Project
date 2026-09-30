package com.project_final.networkprogramming_project.detai1_bytecharstream;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Đề tài 1: Luồng byte và luồng ký tự trong Java
 * -------------------------------------------------
 * GUI Swing hiển thị kết quả so sánh ByteStream vs CharStream.
 * - Bảng JTable kết quả
 * - Biểu đồ bar chart bằng Java2D
 * - Phần lý thuyết giải thích
 *
 * @author Cao Duy Quốc Khánh
 */
public class StreamComparisonGUI extends JFrame {

    // Các kích thước file test (MB)
    private static final int[] TEST_SIZES_MB = {1, 5, 10, 20};
    private static final String TEST_DIR = "testdata";

    // Components
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private BarChartPanel chartPanel;
    private JButton btnRun;
    private JTextArea txtLog;
    private JLabel lblStatus;

    /** Kết quả benchmark, mỗi phần tử ứng với một kích thước file trong TEST_SIZES_MB. */
    private BenchmarkResult[] results;

    // ===== Tên và màu 4 series của biểu đồ =====
    private static final String[] SERIES_NAMES = {
        "Byte/TEXT", "Char/TEXT", "Byte/BINARY", "Char/BINARY"
    };
    private static final Color[] SERIES_COLORS = {
        new Color(0, 123, 255),    // xanh - ByteStream doc file text
        new Color(255, 159, 64),   // cam  - CharStream doc file text
        new Color(40, 167, 69),    // luc  - ByteStream doc file binary
        new Color(220, 53, 69)     // do   - CharStream doc file binary (te nhat)
    };

    public StreamComparisonGUI() {
        setTitle("De Tai 1: So Sanh Byte Stream vs Char Stream - Cao Duy Quoc Khanh");
        setSize(950, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(245, 245, 250));

        // ========== HEADER ==========
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ========== CENTER: Table + Chart ==========
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setResizeWeight(0.45);

        // Bảng kết quả (bên trái)
        JPanel tablePanel = createTablePanel();
        centerSplit.setLeftComponent(tablePanel);

        // Biểu đồ (bên phải)
        chartPanel = new BarChartPanel();
        JPanel chartWrapper = new JPanel(new BorderLayout());
        chartWrapper.setBorder(BorderFactory.createTitledBorder("Bieu do so sanh (ms)"));
        chartWrapper.add(chartPanel, BorderLayout.CENTER);
        centerSplit.setRightComponent(chartWrapper);

        mainPanel.add(centerSplit, BorderLayout.CENTER);

        // ========== BOTTOM: Log + Lý thuyết ==========
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    /**
     * Tạo header panel với tiêu đề và nút chạy.
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setOpaque(false);

        // Tiêu đề
        JLabel titleLabel = new JLabel("DE TAI 1: SO SANH BYTE STREAM vs CHAR STREAM");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(new Color(33, 37, 41));

        JLabel subtitleLabel = new JLabel("Nguyen ly hoat dong va vi du - FileInputStream vs FileReader");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(108, 117, 125));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);

        // Nút chạy
        btnRun = new JButton("Chay Benchmark");
        btnRun.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnRun.setBackground(new Color(0, 123, 255));
        btnRun.setForeground(Color.WHITE);
        btnRun.setFocusPainted(false);
        btnRun.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRun.setPreferredSize(new Dimension(180, 40));
        btnRun.addActionListener(e -> runBenchmark());

        // Status label
        lblStatus = new JLabel("San sang chay benchmark...");
        lblStatus.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblStatus.setForeground(new Color(108, 117, 125));

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setOpaque(false);
        rightPanel.add(btnRun, BorderLayout.CENTER);
        rightPanel.add(lblStatus, BorderLayout.SOUTH);

        panel.add(titlePanel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * Tạo bảng kết quả.
     */
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Bang ket qua"));

        // 4 cot so lieu = 2 loai stream x 2 loai file, dung yeu cau de bai
        String[] columns = {
            "File", "Byte/TEXT", "Char/TEXT", "Byte/BIN", "Char/BIN", "Char cham hon"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho sửa
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setRowHeight(30);
        resultTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        resultTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        resultTable.getTableHeader().setBackground(new Color(52, 58, 64));
        resultTable.getTableHeader().setForeground(Color.WHITE);

        // Căn giữa các cột
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < columns.length; i++) {
            resultTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(resultTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Tạo panel phía dưới chứa log và lý thuyết.
     */
    private JPanel createBottomPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setPreferredSize(new Dimension(0, 180));

        // Tab Log
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtLog.setBackground(new Color(33, 37, 41));
        txtLog.setForeground(new Color(0, 255, 0));
        tabbedPane.addTab("Log", new JScrollPane(txtLog));

        // Tab Lý thuyết
        JTextArea txtTheory = new JTextArea();
        txtTheory.setEditable(false);
        txtTheory.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtTheory.setLineWrap(true);
        txtTheory.setWrapStyleWord(true);
        txtTheory.setText(
            "LY THUYET: BYTE STREAM vs CHAR STREAM\n"
            + "=".repeat(50) + "\n\n"
            + "1. ByteStream (FileInputStream/FileOutputStream):\n"
            + "   - Doc du lieu dang BYTE (8-bit)\n"
            + "   - Lop goc: InputStream / OutputStream\n"
            + "   - Phu hop: file nhi phan (anh, video, .exe, .zip)\n"
            + "   - KHONG tu xu ly encoding\n\n"
            + "2. CharStream (FileReader/FileWriter):\n"
            + "   - Doc du lieu dang KY TU (char, 16-bit Unicode)\n"
            + "   - Lop goc: Reader / Writer\n"
            + "   - Phu hop: file text (.txt, .csv, .java)\n"
            + "   - TU DONG xu ly encoding (UTF-8, UTF-16...)\n\n"
            + "3. Luong xu ly trong OS:\n"
            + "   ByteStream:  File -> OS Buffer -> byte[] -> Ung dung\n"
            + "   CharStream:  File -> OS Buffer -> byte[] -> Decoder -> char[] -> Ung dung\n"
            + "   => CharStream co them buoc DECODE => cham hon mot chut\n\n"
            + "4. Khi nao dung cai nao?\n"
            + "   - File nhi phan (anh, video, .zip)  => Dung ByteStream\n"
            + "   - File text (.txt, .csv, .java)     => Dung CharStream\n\n"
            + "5. CACH DO DE SO LIEU DANG TIN:\n"
            + "   Van de: file vua ghi xong con nam trong OS page cache (RAM),\n"
            + "   nen lan doc SAU luon duoc loi the => do 1 lan la SAI.\n"
            + "   Xu ly: warm-up 2 lan (bo ket qua) de nap page cache + kich hoat\n"
            + "   JIT compiler, sau do do 3 lan va lay TRUNG VI (median).\n"
            + "   Nho vay ByteStream va CharStream duoc do trong cung dieu kien.\n\n"
            + "6. QUAN TRONG HON CA TOC DO - TINH DUNG DAN:\n"
            + "   CharStream doc file NHI PHAN se lam HONG du lieu: CharsetDecoder\n"
            + "   thay moi byte khong hop le bang U+FFFD ma KHONG nem exception.\n"
            + "   Xem cot 'Char/BIN' va tab 'Bay encoding' de thay bang chung.\n"
        );
        tabbedPane.addTab("Ly thuyet", new JScrollPane(txtTheory));

        // Tab Demo đọc/ghi: phần Demo mà đề bài yêu cầu (đọc VÀ ghi tệp .txt)
        JTextArea txtReadWrite = new JTextArea();
        txtReadWrite.setEditable(false);
        txtReadWrite.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtReadWrite.setText(ReadWriteDemo.buildReport());
        txtReadWrite.setCaretPosition(0); // cuon ve dau, khong de o cuoi
        tabbedPane.addTab("Demo doc/ghi", new JScrollPane(txtReadWrite));

        // Tab Bẫy encoding: chạy ngay khi mở GUI vì rất nhanh (file demo vài chục byte)
        JTextArea txtEncoding = new JTextArea();
        txtEncoding.setEditable(false);
        txtEncoding.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtEncoding.setText(EncodingDemo.buildReport());
        txtEncoding.setCaretPosition(0); // cuon ve dau, khong de o cuoi
        tabbedPane.addTab("Bay encoding", new JScrollPane(txtEncoding));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Chạy benchmark trên background thread (không block GUI).
     */
    private void runBenchmark() {
        btnRun.setEnabled(false);
        lblStatus.setText("Dang chay benchmark...");
        tableModel.setRowCount(0); // Xóa kết quả cũ
        txtLog.setText("");

        // SwingWorker: chạy nặng ở background, cập nhật GUI ở EDT
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws IOException {
                BenchmarkResult[] collected = new BenchmarkResult[TEST_SIZES_MB.length];
                TestFileUtils.ensureTestDir();

                for (int i = 0; i < TEST_SIZES_MB.length; i++) {
                    int sizeMB = TEST_SIZES_MB[i];
                    String textFile = TEST_DIR + "/test_text_" + sizeMB + "MB.txt";
                    String binaryFile = TEST_DIR + "/test_binary_" + sizeMB + "MB.bin";

                    publish("--- Test voi file " + sizeMB + " MB ---");
                    publish("Tao file test (text UTF-8 + binary)...");
                    TestFileUtils.generateTextFile(textFile, sizeMB);
                    TestFileUtils.generateBinaryFile(binaryFile, sizeMB);

                    // Engine tu lo warm-up + do nhieu lan + lay trung vi
                    collected[i] = StreamBenchmark.measure(
                            sizeMB, textFile, binaryFile, this::publish);
                    publish("");
                }

                results = collected;

                publish("Don dep file test...");
                TestFileUtils.cleanupTestFiles();
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
                // get() nem lai exception da xay ra trong doInBackground().
                // KHONG bo qua buoc nay: neu bo, loi doc file se bi im lang
                // va GUI hien bang trong ma khong bao gi ca.
                try {
                    get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // giu lai trang thai interrupt
                    showFailure("Benchmark bi ngat giua chung");
                    return;
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    showFailure("Loi khi chay benchmark: "
                            + (cause != null ? cause.getMessage() : e.getMessage()));
                    return;
                }

                fillResultTable();
                updateChart();

                btnRun.setEnabled(true);
                lblStatus.setText("Benchmark hoan tat!");
            }
        };

        worker.execute();
    }

    /** Báo lỗi ra log + status bar và mở lại nút chạy. */
    private void showFailure(String message) {
        txtLog.append("[LOI] " + message + "\n");
        lblStatus.setText(message);
        btnRun.setEnabled(true);
    }

    /** Đổ kết quả vào bảng, mỗi dòng là một kích thước file. */
    private void fillResultTable() {
        if (results == null) {
            return;
        }
        for (BenchmarkResult r : results) {
            tableModel.addRow(new Object[]{
                r.sizeMB() + " MB",
                String.format("%.2f", r.byteOnTextMs()),
                String.format("%.2f", r.charOnTextMs()),
                String.format("%.2f", r.byteOnBinaryMs()),
                String.format("%.2f", r.charOnBinaryMs()),
                String.format("TEXT %.1fx | BIN %.1fx",
                        r.textSlowdownFactor(), r.binarySlowdownFactor())
            });
        }
    }

    /** Chuyển kết quả sang định dạng 4 series rồi vẽ biểu đồ. */
    private void updateChart() {
        if (results == null) {
            return;
        }

        String[] xLabels = new String[results.length];
        double[][] values = new double[SERIES_NAMES.length][results.length];

        for (int i = 0; i < results.length; i++) {
            BenchmarkResult r = results[i];
            xLabels[i] = r.sizeMB() + " MB";
            values[0][i] = r.byteOnTextMs();
            values[1][i] = r.charOnTextMs();
            values[2][i] = r.byteOnBinaryMs();
            values[3][i] = r.charOnBinaryMs();
        }

        chartPanel.setData(xLabels, SERIES_NAMES, SERIES_COLORS, values);
        chartPanel.repaint();
    }

    /**
     * Khởi chạy GUI.
     */
    public static void run() {
        SwingUtilities.invokeLater(() -> {
            StreamComparisonGUI gui = new StreamComparisonGUI();
            gui.setVisible(true);
        });
    }
}
