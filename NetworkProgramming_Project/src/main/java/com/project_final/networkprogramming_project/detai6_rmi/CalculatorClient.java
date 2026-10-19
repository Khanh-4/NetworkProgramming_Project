package com.project_final.networkprogramming_project.detai6_rmi;

import java.lang.reflect.Proxy;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Đề tài 6: Kiến trúc RMI - Client
 * ------------------------------------
 * Client tra cứu object từ RMI Registry và gọi hàm từ xa.
 *
 * Client in các bước ① và ④ (phía Stub); cửa sổ Server in các bước ② và ③
 * (phía Skeleton và Server). Đặt hai cửa sổ cạnh nhau sẽ thấy đủ thứ tự:
 *
 *   ① Stub → ② Skeleton → ③ Server → ② Skeleton → ④ Stub
 *
 * Điều đáng chú ý: code gọi calc.add(15, 5) trông y hệt gọi hàm cục bộ.
 * Toàn bộ việc mở kết nối TCP, tuần tự hoá tham số và kết quả do Stub làm.
 *
 * @author Nguyễn Đức Hiếu
 */
public class CalculatorClient {

    private static final String DEFAULT_HOST = "localhost";

    /** Một lời gọi từ xa trả về double; cho phép ném RemoteException. */
    @FunctionalInterface
    private interface RemoteCall {
        double call() throws RemoteException;
    }

    /** Điểm vào từ menu chính; dùng chung Scanner của menu để không tranh System.in. */
    public static void run(Scanner scanner) {
        System.out.println("=== DE TAI 6: RMI CLIENT ===");
        System.out.print("Nhap IP may chay Server (Enter = " + DEFAULT_HOST + "): ");
        String host = scanner.nextLine().trim();
        start(host.isEmpty() ? DEFAULT_HOST : host);
    }

    private static void start(String host) {
        try {
            CalculatorInterface calc = lookup(host);
            invoke("add(15, 5)", () -> calc.add(15, 5));
            invoke("subtract(15, 5)", () -> calc.subtract(15, 5));
            invoke("multiply(15, 5)", () -> calc.multiply(15, 5));
            invoke("divide(15, 5)", () -> calc.divide(15, 5));
            invoke("divide(1, 0)", () -> calc.divide(1, 0));
            System.out.println("[OK] Goi ham tu xa thanh cong. Xem cua so Server de thay buoc (2) va (3).");
        } catch (ConnectException e) {
            System.out.println("[X] Khong ket noi duoc toi " + host + ". Server da chay chua? (menu 6 -> a)");
        } catch (NotBoundException e) {
            System.out.println("[X] Registry khong co ten \"" + CalculatorServer.SERVICE_NAME
                    + "\". Server chua dang ky hoac da tat.");
        } catch (RemoteException e) {
            System.out.println("[X] Loi RMI: " + e.getMessage());
        }
    }

    /** Bước (0): hỏi registry để lấy STUB của đối tượng ở xa. */
    private static CalculatorInterface lookup(String host) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(host, CalculatorServer.REGISTRY_PORT);
        CalculatorInterface calc = (CalculatorInterface) registry.lookup(CalculatorServer.SERVICE_NAME);

        System.out.println("(0) lookup(\"" + CalculatorServer.SERVICE_NAME + "\") tai registry "
                + host + ":" + CalculatorServer.REGISTRY_PORT + " -> nhan ve STUB:");
        System.out.println("    Lop cua stub     : " + calc.getClass().getName()
                + (Proxy.isProxyClass(calc.getClass()) ? "  (dynamic proxy do RMI sinh ra)" : ""));
        if (Proxy.isProxyClass(calc.getClass())) {
            System.out.println("    Xu ly loi goi boi: "
                    + Proxy.getInvocationHandler(calc).getClass().getName());
        }
        // toString() của stub chứa địa chỉ endpoint (IP:cổng) của đối tượng thật bên server.
        System.out.println("    Tro toi          : " + calc + "\n");
        return calc;
    }

    /** Gọi một hàm từ xa và in bước ① (trước khi gửi) và ④ (sau khi nhận). */
    private static void invoke(String expression, RemoteCall call) throws RemoteException {
        System.out.println("(1) [Stub] Goi calc." + expression
                + " -> Stub dong goi tham so, gui qua TCP toi Server");
        try {
            double result = call.call();
            System.out.println("(4) [Stub] Nhan va giai ma ket qua: " + expression + " = " + result + "\n");
        } catch (ArithmeticException e) {
            // Exception ném ở Server được tuần tự hoá, gửi về và ném lại ở đây.
            System.out.println("(4) [Stub] Nhan exception tu Server: " + e.getClass().getSimpleName()
                    + " - " + e.getMessage() + "\n");
        }
    }

    /** Chạy độc lập: java ...CalculatorClient [host] */
    public static void main(String[] args) {
        start(args.length > 0 ? args[0] : DEFAULT_HOST);
    }
}
