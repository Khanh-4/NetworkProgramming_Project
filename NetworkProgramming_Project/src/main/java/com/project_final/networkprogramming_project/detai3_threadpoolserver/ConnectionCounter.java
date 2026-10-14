package com.project_final.networkprogramming_project.detai3_threadpoolserver;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Đề tài 3: Đếm số client ĐANG được phục vụ cùng lúc và mức cao nhất từng đạt.
 * -----------------------------------------------------------------------------
 * Con số "đỉnh" (peak) là bằng chứng định lượng cho yêu cầu của đề bài:
 * nếu chạy 3 client mà peak = 3 thì cả 3 kết nối thực sự đã được xử lý
 * ĐỒNG THỜI, chứ không phải lần lượt từng client một.
 *
 * Nhiều worker thread cùng gọi enter()/exit() nên phải dùng AtomicInteger.
 * Nếu dùng int thường, phép "active++" gồm 3 bước (đọc → cộng → ghi) có thể
 * bị hai thread chen ngang nhau → mất một lần đếm (race condition).
 *
 * @author Cao Duy Quốc Khánh
 */
final class ConnectionCounter {

    private final AtomicInteger active = new AtomicInteger();
    private final AtomicInteger peak = new AtomicInteger();
    private final AtomicInteger total = new AtomicInteger();

    /** Gọi khi worker bắt đầu phục vụ một client. Trả về số client đang hoạt động. */
    int enter() {
        total.incrementAndGet();
        int now = active.incrementAndGet();
        // accumulateAndGet thay cho "if (now > peak) peak = now" vì câu if đó
        // không nguyên tử: hai thread có thể cùng đọc peak cũ rồi ghi đè nhau.
        peak.accumulateAndGet(now, Math::max);
        return now;
    }

    /** Gọi khi worker phục vụ xong một client. Trả về số client còn lại. */
    int exit() {
        return active.decrementAndGet();
    }

    int peak() {
        return peak.get();
    }

    int total() {
        return total.get();
    }
}
