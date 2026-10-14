package com.project_final.networkprogramming_project.detai3_threadpoolserver;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Đề tài 3: In log có kèm thời điểm và TÊN THREAD đang chạy.
 * ------------------------------------------------------------
 * Tên thread là bằng chứng quan trọng nhất của đề tài: nhìn log sẽ thấy
 * các client khác nhau được phục vụ bởi các worker khác nhau trong pool,
 * và các dòng log của chúng xen kẽ nhau theo thời gian (= chạy song song).
 *
 *   [10:15:02.118][worker-1] Client /127.0.0.1:51001 gửi "abc"
 *   [10:15:02.120][worker-2] Client /127.0.0.1:51002 gửi "xyz"
 *
 * System.out.println() bản thân đã synchronized, nên mỗi dòng log
 * in ra nguyên vẹn, không bị trộn ký tự giữa các thread.
 *
 * @author Cao Duy Quốc Khánh
 */
final class ConsoleLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private ConsoleLog() {
    }

    static void print(String message) {
        String time = LocalTime.now().format(TIME);
        String thread = Thread.currentThread().getName();
        System.out.println("[" + time + "][" + thread + "] " + message);
    }
}
