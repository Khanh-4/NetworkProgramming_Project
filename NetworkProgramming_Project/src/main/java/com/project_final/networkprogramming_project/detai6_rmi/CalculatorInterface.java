package com.project_final.networkprogramming_project.detai6_rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Đề tài 6: Kiến trúc RMI - Remote Interface
 * ---------------------------------------------
 * Interface khai báo các method có thể gọi từ xa.
 * 
 * TODO (Hiếu):
 * - Khai báo các phương thức tính toán (add, subtract, multiply, divide)
 * - Hoặc các phương thức khác tùy chọn
 * - Phải extends Remote, mỗi method throws RemoteException
 * 
 * @author Nguyễn Đức Hiếu
 */
public interface CalculatorInterface extends Remote {

    // TODO: Khai báo các method từ xa
    // Ví dụ:
    // double add(double a, double b) throws RemoteException;
    // double subtract(double a, double b) throws RemoteException;
}
