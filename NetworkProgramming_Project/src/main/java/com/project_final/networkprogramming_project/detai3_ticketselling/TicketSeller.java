package com.project_final.networkprogramming_project.detai3_ticketselling;

/**
 * Đề tài 3: Ứng dụng bán vé giữa nhiều Thread
 * -----------------------------------------------
 * Class đại diện cho 1 quầy bán vé (1 Thread).
 * Implements Runnable để chạy như 1 thread.
 * 
 * TODO (Quân):
 * - Nhận TicketPool qua constructor
 * - Trong run(): lặp bán vé cho đến khi hết
 * - Log: "Quầy X bán vé số Y"
 * 
 * @author Trần Gia Quân
 */
public class TicketSeller implements Runnable {

    // TODO: Implement ticket seller thread
    
    public TicketSeller(String sellerName, TicketPool pool) {
        System.out.println("[TODO] TicketSeller chưa implement - Quân sẽ code phần này");
    }

    @Override
    public void run() {
        // TODO: Implement selling logic
    }
}
