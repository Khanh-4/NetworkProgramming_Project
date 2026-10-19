package com.project_final.networkprogramming_project.detai3_threadpoolserver;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Đề tài 3: Server TCP đa luồng dùng ThreadPool — trả về chuỗi đảo ngược.
 * ========================================================================
 *
 * KIẾN TRÚC:
 *
 *                       ┌──────────────── ReverseServer ────────────────┐
 *   Client A ──┐        │                                               │
 *   Client B ──┼─TCP──▶ │  main thread: while(true) accept()            │
 *   Client C ──┘        │        │  mỗi kết nối mới = 1 ClientHandler   │
 *                       │        ▼                                      │
 *                       │  ExecutorService (fixed pool, POOL_SIZE)      │
 *                       │   ┌──────────┬──────────┬──────────┐          │
 *                       │   │ worker-1 │ worker-2 │ worker-3 │ ...      │
 *                       │   │ ↔ A      │ ↔ B      │ ↔ C      │          │
 *                       │   └──────────┴──────────┴──────────┘          │
 *                       │  Task dư (pool đã đầy) → xếp hàng trong queue  │
 *                       └───────────────────────────────────────────────┘
 *
 * VÌ SAO KHÔNG XỬ LÝ CLIENT NGAY TRONG MAIN THREAD?
 *   accept() và readLine() đều là blocking I/O. Nếu main thread tự đọc dữ liệu
 *   của client A thì trong lúc chờ A gõ phím, nó không thể accept() client B
 *   → server chỉ phục vụ được lần lượt từng người (iterative server).
 *
 * VÌ SAO DÙNG THREADPOOL THAY VÌ "new Thread() CHO MỖI CLIENT"?
 *   ┌──────────────────────┬───────────────────────────┬─────────────────────────┐
 *   │                      │ new Thread mỗi client     │ ThreadPool (fixed)      │
 *   ├──────────────────────┼───────────────────────────┼─────────────────────────┤
 *   │ Chi phí tạo thread   │ Mỗi lần kết nối đều tạo   │ Tạo sẵn, dùng lại       │
 *   │ 10.000 client ập tới │ 10.000 thread → hết RAM   │ Chỉ POOL_SIZE thread,   │
 *   │                      │ (mỗi stack ~1 MB)         │ phần còn lại xếp hàng   │
 *   │ Kiểm soát tài nguyên │ Không giới hạn            │ Giới hạn rõ ràng        │
 *   └──────────────────────┴───────────────────────────┴─────────────────────────┘
 *
 * GIỚI HẠN CẦN BIẾT:
 *   Mỗi client giữ một worker suốt thời gian kết nối, kể cả lúc chỉ ngồi im.
 *   Client thứ (POOL_SIZE + 1) vẫn kết nối TCP được (kernel đã bắt tay xong),
 *   nhưng phải chờ trong queue đến khi có worker rảnh mới được trả lời.
 *
 * @author Cao Duy Quốc Khánh
 */
public final class ReverseServer implements AutoCloseable {

    public static final int DEFAULT_PORT = 5000;
    public static final int POOL_SIZE = 10;

    /** Thời gian tối đa chờ các worker làm nốt việc khi tắt server. */
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final ServerSocket serverSocket;
    private final ExecutorService pool;
    private final ConnectionCounter counter = new ConnectionCounter();

    /**
     * Mở cổng lắng nghe ngay trong constructor để lỗi "cổng đã bị chiếm"
     * (BindException) lộ ra sớm, trước khi bắt đầu vòng accept.
     *
     * @param port     cổng TCP; truyền 0 để hệ điều hành tự chọn một cổng trống
     * @param poolSize số worker thread trong pool
     */
    public ReverseServer(int port, int poolSize) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.pool = Executors.newFixedThreadPool(poolSize, namedThreads("worker-"));
    }

    /** Đặt tên "worker-1", "worker-2"... thay cho "pool-1-thread-1" cho log dễ đọc. */
    private static ThreadFactory namedThreads(String prefix) {
        AtomicInteger next = new AtomicInteger(1);
        return task -> new Thread(task, prefix + next.getAndIncrement());
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    /** Số client tối đa từng được phục vụ ĐỒNG THỜI kể từ khi server chạy. */
    public int peakConcurrentClients() {
        return counter.peak();
    }

    public int totalClients() {
        return counter.total();
    }

    /**
     * Vòng lặp chính: chấp nhận kết nối và giao cho pool. Chặn (block) cho đến
     * khi server bị close() từ thread khác.
     */
    public void serve() {
        ConsoleLog.print("Server lang nghe cong " + port() + ", ThreadPool co dinh "
                + POOL_SIZE + " worker");
        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                // execute() trả về ngay; main thread quay lại accept() client tiếp theo
                // trong khi worker xử lý client này.
                pool.execute(new ClientHandler(client, counter));
            } catch (SocketException e) {
                // close() từ thread khác làm accept() ném SocketException → thoát vòng lặp.
                if (!serverSocket.isClosed()) {
                    ConsoleLog.print("Loi socket khi accept: " + e.getMessage());
                }
            } catch (IOException e) {
                ConsoleLog.print("Loi khi accept client: " + e.getMessage());
            }
        }
        ConsoleLog.print("Server ngung nhan ket noi moi");
    }

    /**
     * Đóng cổng lắng nghe rồi cho pool làm nốt các task đang dở.
     * shutdown() không nhận task mới nhưng vẫn chạy hết task cũ;
     * quá thời hạn thì shutdownNow() để ngắt hẳn.
     */
    @Override
    public void close() throws IOException {
        serverSocket.close();
        pool.shutdown();
        try {
            if (!pool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** Điểm vào từ menu chính. Server chạy đến khi nhấn Ctrl+C. */
    public static void run() {
        start(DEFAULT_PORT);
    }

    private static void start(int port) {
        System.out.println("=== DE TAI 3: SERVER TCP DA LUONG (THREADPOOL) ===");
        System.out.println("Mo them it nhat 3 terminal va chay Client o moi terminal.");
        System.out.println("Nhan Ctrl+C de dung server.\n");
        try (ReverseServer server = new ReverseServer(port, POOL_SIZE)) {
            server.serve();
        } catch (BindException e) {
            System.out.println("[X] Cong " + port + " dang bi chuong trinh khac chiem."
                    + " Hay tat server cu hoac chon cong khac.");
        } catch (IOException e) {
            System.out.println("[X] Khong khoi dong duoc server: " + e.getMessage());
        }
    }

    /** Chạy độc lập: java ...ReverseServer [port] */
    public static void main(String[] args) {
        start(args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT);
    }
}
