package com.project_final.networkprogramming_project.detai6_rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CountDownLatch;
import java.util.function.DoubleBinaryOperator;

/**
 * Đề tài 6: Kiến trúc RMI - Server Implementation
 * ---------------------------------------------------
 * Server triển khai CalculatorInterface, đăng ký vào RMI Registry.
 *
 * KIẾN TRÚC RMI VÀ THỨ TỰ MỘT LỜI GỌI:
 *
 *        MÁY CLIENT                                   MÁY SERVER
 *   ┌──────────────────┐                      ┌──────────────────────────┐
 *   │ CalculatorClient │                      │ rmiregistry (cổng 1099)  │
 *   │   lookup(name) ──┼──── (0) tra cứu ────▶│  "CalculatorService" →   │
 *   │   ◀── nhận STUB ─┼──────────────────────┼─ stub của Server         │
 *   │                  │                      └──────────────────────────┘
 *   │ calc.add(5, 3)   │                      ┌──────────────────────────┐
 *   │      │           │                      │ RMI runtime (Skeleton)   │
 *   │  ① STUB          │  ─── TCP ──────────▶ │  ② nhận request, giải mã │
 *   │  marshal tham số │                      │     tham số, gọi method  │
 *   │                  │                      │           │              │
 *   │                  │                      │  ③ SERVER (lớp này)      │
 *   │                  │                      │     add(5, 3) = 8        │
 *   │  ④ STUB          │ ◀── TCP ──────────── │  ② đóng gói kết quả 8    │
 *   │  unmarshal → 8   │                      │                          │
 *   └──────────────────┘                      └──────────────────────────┘
 *
 * VỀ SKELETON TRONG JAVA HIỆN ĐẠI:
 *   Ở Java 1.1, mỗi lớp server có một lớp Skeleton riêng do công cụ rmic sinh ra.
 *   Từ Java 1.2, Skeleton được thay bằng bộ điều phối chung bên trong RMI runtime
 *   (vẫn làm đúng vai trò: nhận request, unmarshal, gọi method, marshal kết quả);
 *   từ Java 5, Stub được sinh tự động bằng dynamic proxy nên không cần rmic nữa.
 *   Log "② [Skeleton]" bên dưới được in khi RMI runtime vừa giao lời gọi tới
 *   method của Server, tức là ngay sau khi vai trò Skeleton hoàn tất.
 *
 * @author Nguyễn Đức Hiếu
 */
public class CalculatorServer extends UnicastRemoteObject implements CalculatorInterface {

    private static final long serialVersionUID = 1L;

    public static final int REGISTRY_PORT = 1099;
    public static final String SERVICE_NAME = "CalculatorService";

    /**
     * Giữ tham chiếu tới registry tự tạo để nó không bị thu gom rác (GC)
     * và ngừng hoạt động khi không còn biến nào trỏ tới.
     */
    private static Registry ownedRegistry;

    /**
     * Constructor của UnicastRemoteObject "export" đối tượng: mở một cổng TCP
     * (cổng ngẫu nhiên) và bắt đầu chờ lời gọi từ xa tới đối tượng này.
     */
    public CalculatorServer() throws RemoteException {
        super();
    }

    @Override
    public double add(double a, double b) {
        return execute("add", a, b, Double::sum);
    }

    @Override
    public double subtract(double a, double b) {
        return execute("subtract", a, b, (x, y) -> x - y);
    }

    @Override
    public double multiply(double a, double b) {
        return execute("multiply", a, b, (x, y) -> x * y);
    }

    @Override
    public double divide(double a, double b) {
        return execute("divide", a, b, (x, y) -> {
            if (y == 0) {
                throw new ArithmeticException("Khong the chia cho 0");
            }
            return x / y;
        });
    }

    /**
     * Mọi lời gọi từ xa đều đi qua đây để in log theo đúng thứ tự ② → ③.
     * Method chạy trên thread "RMI TCP Connection(...)" của RMI runtime chứ
     * không phải thread main — bằng chứng lời gọi tới từ mạng.
     */
    private double execute(String operation, double a, double b, DoubleBinaryOperator formula) {
        String call = operation + "(" + a + ", " + b + ")";
        System.out.println("(2) [Skeleton] RMI runtime nhan loi goi " + call + " tu client "
                + clientHost() + " tren thread '" + Thread.currentThread().getName()
                + "', giai ma tham so roi chuyen cho Server");
        try {
            double result = formula.applyAsDouble(a, b);
            System.out.println("(3) [Server]   Thuc thi " + call + " = " + result
                    + " -> Skeleton dong goi ket qua gui ve Stub\n");
            return result;
        } catch (ArithmeticException e) {
            System.out.println("(3) [Server]   " + call + " loi: " + e.getMessage()
                    + " -> exception duoc gui ve Client\n");
            throw e;
        }
    }

    private static String clientHost() {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            return "(goi cuc bo)";
        }
    }

    /**
     * Ưu tiên dùng rmiregistry đang chạy sẵn (đúng yêu cầu "chạy qua rmiregistry");
     * nếu chưa có thì tự tạo registry trong JVM này để demo vẫn chạy được.
     */
    private static Registry obtainRegistry() throws RemoteException {
        Registry existing = LocateRegistry.getRegistry(REGISTRY_PORT);
        try {
            existing.list(); // Gọi thử: ném RemoteException nếu không có registry nào ở cổng này
            System.out.println("Da ket noi rmiregistry co san o cong " + REGISTRY_PORT);
            return existing;
        } catch (RemoteException e) {
            ownedRegistry = LocateRegistry.createRegistry(REGISTRY_PORT);
            System.out.println("Chua co rmiregistry -> tu tao registry o cong " + REGISTRY_PORT);
            return ownedRegistry;
        }
    }

    /** Điểm vào từ menu chính. Server chạy đến khi nhấn Ctrl+C. */
    public static void run() {
        start();
    }

    private static void start() {
        System.out.println("=== DE TAI 6: RMI SERVER ===");
        try {
            Registry registry = obtainRegistry();
            CalculatorServer server = new CalculatorServer();
            // rebind: ghi đè nếu tên đã tồn tại (ví dụ lần chạy server trước chưa dọn).
            registry.rebind(SERVICE_NAME, server);
            System.out.println("[OK] Da dang ky \"" + SERVICE_NAME + "\" vao registry. Cac ten hien co: "
                    + String.join(", ", registry.list()));
            System.out.println("Mo cua so khac va chay Client (menu 6 -> b). Nhan Ctrl+C de dung.");
            System.out.println("Client o may khac: chay server voi -Djava.rmi.server.hostname=<IP may nay>\n");
            waitForever();
        } catch (RemoteException e) {
            System.out.println("[X] Khong khoi dong duoc RMI server: " + e.getMessage());
        }
    }

    /**
     * Các thread của RMI runtime đã giữ JVM sống, nhưng khi chạy từ menu thì phải
     * chặn thread main lại, nếu không menu sẽ hiện lên chồng với log của server.
     */
    private static void waitForever() {
        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Chạy độc lập: java ...CalculatorServer [IP máy server]
     * Truyền IP khi client ở máy khác, để stub gửi cho client chứa đúng địa chỉ
     * (mặc định Java có thể ghi 127.0.1.1 vào stub và client sẽ không gọi tới được).
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            System.setProperty("java.rmi.server.hostname", args[0]);
        }
        start();
    }
}
