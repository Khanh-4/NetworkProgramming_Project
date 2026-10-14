package com.project_final.networkprogramming_project.detai6_rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Đề tài 6: Kiến trúc RMI - Remote Interface
 * ---------------------------------------------
 * Interface khai báo các method có thể gọi từ xa.
 *
 * Đây là "hợp đồng" DUY NHẤT mà cả hai phía cùng biết:
 *   - Server cài đặt (implements) interface này.
 *   - Client chỉ thấy interface này; đối tượng nó cầm trong tay là STUB
 *     (proxy) do RMI sinh ra, cũng implements interface này.
 * Vì vậy file này phải có mặt ở cả máy Client, máy Server và trên classpath
 * của rmiregistry.
 *
 * Quy tắc bắt buộc của RMI:
 *   1. Interface phải extends java.rmi.Remote (interface đánh dấu, không có method).
 *   2. Mọi method phải khai báo throws RemoteException, vì lời gọi đi qua mạng
 *      có thể thất bại bất cứ lúc nào (mất kết nối, server tắt...).
 *   3. Tham số và giá trị trả về phải là kiểu nguyên thủy, Serializable hoặc Remote.
 *
 * @author Nguyễn Đức Hiếu
 */
public interface CalculatorInterface extends Remote {

    double add(double a, double b) throws RemoteException;

    double subtract(double a, double b) throws RemoteException;

    double multiply(double a, double b) throws RemoteException;

    /**
     * Chia a cho b.
     *
     * @throws ArithmeticException khi b = 0. Exception ném ra ở Server được RMI
     *         tuần tự hoá và ném lại ở Client — giống như gọi hàm cục bộ.
     */
    double divide(double a, double b) throws RemoteException;
}
