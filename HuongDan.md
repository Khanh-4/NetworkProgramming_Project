# HƯỚNG DẪN TEST 6 ĐỀ TÀI & CÁCH ĐỌC SỐ LIỆU

> Project cuối kỳ môn **Lập trình mạng máy tính** — Nhóm Khánh · Quân · Hiếu
>
> File này hướng dẫn cho **cả 6 đề tài (1, 2, 3, 4, 5, 6)**: cách chạy, cách test, cách đọc và giải thích
> các con số / log in ra, lỗi hay gặp. Mọi output mẫu bên dưới là **output thật** khi chạy code trong repo
> (máy test: Linux/WSL, JDK 25). Số đo thời gian trên máy bạn **sẽ khác**, nhưng **xu hướng**
> (cái nào nhanh hơn, chậm hơn bao nhiêu lần) phải giống.

---

## Mục lục

0. [Chuẩn bị & cách chạy](#0-chuẩn-bị--cách-chạy)
1. [Đề tài 1 — Luồng Byte vs Ký tự](#1-đề-tài-1--luồng-byte-vs-luồng-ký-tự-khánh)
2. [Đề tài 2 — Buffered Stream](#2-đề-tài-2--buffered-stream-khánh)
3. [Đề tài 3 — Server TCP đa luồng (ThreadPool)](#3-đề-tài-3--server-tcp-đa-luồng-threadpool-quân)
4. [Đề tài 4 — TCP Client-Server](#4-đề-tài-4--tcp-client-server-quân)
5. [Đề tài 5 — UDP Ping-Pong](#5-đề-tài-5--udp-ping-pong-hiếu)
6. [Đề tài 6 — Java RMI](#6-đề-tài-6--java-rmi-hiếu)
7. [Test trên 2 máy (mạng LAN)](#7-test-trên-2-máy-mạng-lan)
8. [Bảng lỗi thường gặp](#8-bảng-lỗi-thường-gặp)
9. [Checklist trước khi demo](#9-checklist-trước-khi-demo)

---

## 0. Chuẩn bị & cách chạy

### 0.1. Yêu cầu

| Thành phần | Yêu cầu |
|---|---|
| JDK | Khớp với `maven.compiler.release` trong `NetworkProgramming_Project/pom.xml` (hiện là **26**). Nếu máy chỉ có JDK thấp hơn, sửa số này bằng phiên bản JDK đang cài (tối thiểu 17 vì code dùng `record`). |
| IDE | NetBeans (project Maven) — hoặc chạy bằng terminal |
| Cổng mạng cần trống | **5000** (đề 3), **12345** (đề 4), **9876/UDP** (đề 5), **1099** (đề 6 – RMI Registry) |
| Dung lượng đĩa trống | ~250 MB (đề 2 tạo file 100 MB + bản copy; tự xoá khi chạy xong) |

### 0.2. Chạy bằng NetBeans

1. `File → Open Project` → chọn thư mục `NetworkProgramming_Project/NetworkProgramming_Project` (thư mục có `pom.xml`).
2. Nhấn **F6 (Run Project)** → console hiện **menu chính**:

```
+==============================================================+
|          PROJECT CUOI KY - LAP TRINH MANG MAY TINH           |
|                  Nhom: Khanh - Quan - Hieu                   |
+==============================================================+
|  [1] De tai 1: Luong Byte vs Ky tu        (Khanh)            |
|  [2] De tai 2: Buffered Stream            (Khanh)            |
|  [3] De tai 3: Server da luong TCP        (Quan)             |
|  [4] De tai 4: TCP Client-Server          (Quan)             |
|  [5] De tai 5: UDP Ping-Pong              (Hieu)             |
|  [6] De tai 6: Java RMI                   (Hieu)             |
|  [0] Thoat                                                   |
+==============================================================+
-> Chon de tai (0 de thoat):
```

3. **Đề 3, 4, 5, 6 cần ≥ 2 tiến trình** (1 server + 1 hay nhiều client). Trong NetBeans chỉ cần
   **nhấn F6 thêm lần nữa**: mỗi lần chạy mở một tab Output mới (`Run #2`, `Run #3`...).
   Tab 1 chọn Server, các tab sau chọn Client.

> Chữ in ra console được viết **không dấu** có chủ đích, vì Output của NetBeans trên Windows
> hay hiển thị sai tiếng Việt có dấu.

### 0.3. Chạy bằng terminal (tiện khi cần nhiều client)

Build một lần (NetBeans: **F11 – Build**, hoặc `mvn package`), class sẽ nằm trong `target/classes`:

```bash
cd NetworkProgramming_Project
# Menu chính
java -cp target/classes com.project_final.networkprogramming_project.NetworkProgramming_Project
```

Mỗi server/client còn có `main()` riêng, chạy thẳng không qua menu:

```bash
P=com.project_final.networkprogramming_project       # Windows: gõ tên đầy đủ thay cho $P
java -cp target/classes $P.detai3_threadpoolserver.ReverseServer [port]
java -cp target/classes $P.detai3_threadpoolserver.ReverseClient [host] [port]
java -cp target/classes $P.detai3_threadpoolserver.ConcurrentClientsDemo     # exit code 0 = ĐẠT
java -cp target/classes $P.detai4_clientserver.TCPServer [port]
java -cp target/classes $P.detai4_clientserver.TCPClient [host] [port]
java -cp target/classes $P.detai5_udp.UDPServer [port]
java -cp target/classes $P.detai5_udp.UDPClient [host] [port]
java -cp target/classes $P.detai6_rmi.CalculatorServer [IP-may-server]
java -cp target/classes $P.detai6_rmi.CalculatorClient [host]
```

**Luôn chạy SERVER trước, CLIENT sau.** Dừng server bằng **Ctrl+C** (NetBeans: nút ■ đỏ bên trái tab Output).

---

## 1. Đề tài 1 — Luồng Byte vs Luồng Ký tự (Khánh)

**Mục tiêu:** so sánh `FileInputStream` (ByteStream) với `FileReader` (CharStream) khi đọc file
**text** và file **nhị phân**; demo đọc/ghi `.txt`; chỉ ra bẫy encoding.

### 1.1. Cách test

1. Menu → `1` → mở cửa sổ GUI.
2. Nhấn **"Chay Benchmark"**. Chương trình tự:
   - tạo file test trong thư mục `testdata/` cho các mốc **5, 10, 20, 40 MB**
     (file `.txt` UTF-8 có dấu tiếng Việt + file `.bin` chứa byte `0x00..0xFF` lặp lại);
   - đo 4 tổ hợp **(ByteStream / CharStream) × (TEXT / BIN)**;
   - vẽ biểu đồ cột, đổ bảng, rồi **xoá** file test.
3. Xem lần lượt 4 tab bên dưới: **Log**, **Ly thuyet**, **Demo doc/ghi**, **Bay encoding**.
   (2 tab cuối chạy ngay khi mở GUI, không cần nhấn nút.)

### 1.2. Cách đo (để trả lời khi thầy hỏi "số này có tin được không?")

Mọi phép đo (đề 1 và đề 2) đi qua `BenchmarkUtils.medianNanos()`:

```
1 lần chạy thử (probe) ──► nếu > 1 giây: đo thêm 1 lần, lấy luôn
                       └─► nếu nhanh  : warm-up thêm 2 lần (bỏ kết quả)
                                         rồi đo 5 lần → lấy TRUNG VỊ (median)
```

- **Warm-up** để nạp OS page cache và cho JIT biên dịch vòng lặp → các phương pháp được đo trong
  **cùng điều kiện**, không cái nào ăn sẵn cache.
- **Trung vị** thay vì trung bình: một lần bị antivirus chen ngang không kéo lệch kết quả.
- Đo bằng `System.nanoTime()` (đồng hồ đơn điệu), lưu **nano giây**, hiển thị **ms** 2 số lẻ.

### 1.3. Đọc bảng kết quả

| Cột | Ý nghĩa |
|---|---|
| `File` | Kích thước file test |
| `Byte/TEXT` | ms — ByteStream đọc file `.txt` |
| `Char/TEXT` | ms — CharStream đọc file `.txt` |
| `Byte/BIN` | ms — ByteStream đọc file `.bin` |
| `Char/BIN` | ms — CharStream đọc file `.bin` |
| `Char cham hon` | `TEXT a.x` = Char/TEXT ÷ Byte/TEXT; `BIN b.x` = Char/BIN ÷ Byte/BIN |

**Số liệu mẫu (máy test):**

| File | Byte/TEXT | Char/TEXT | Byte/BIN | Char/BIN | Char chậm hơn |
|---|---|---|---|---|---|
| 5 MB  | 1.08  | 11.73 | 0.65 | 66.44  | TEXT 10.9x \| BIN 102.9x |
| 10 MB | 1.72  | 18.99 | 1.40 | 95.32  | TEXT 11.0x \| BIN 68.2x |
| 20 MB | 5.03  | 40.71 | 3.63 | 240.35 | TEXT 8.1x \| BIN 66.3x |
| 40 MB | 10.60 | 79.87 | 9.95 | 516.27 | TEXT 7.5x \| BIN 51.9x |

**Cách đọc / kết luận:**

1. **Thời gian tăng gần tuyến tính theo kích thước file** (5→40 MB: dữ liệu ×8, thời gian ≈ ×7–10)
   → phép đo ổn định, không bị nhiễu.
2. **CharStream luôn chậm hơn ByteStream** (ở đây ~7–11 lần với file text) vì có thêm bước
   **decode**: `đĩa → byte[] → CharsetDecoder → char[]`. ByteStream chỉ copy byte, không diễn giải.
3. **CharStream đọc file nhị phân còn chậm hơn nữa** (50–100 lần) vì gặp byte không hợp lệ UTF-8
   liên tục → decoder phải xử lý lỗi cho từng byte.
4. **Quan trọng nhất – CharStream LÀM HỎNG file nhị phân.** Log có dòng:
   ```
   [5MB] CharStream doc file .bin -> 2621440/5242880 ky tu BI HONG
   ```
   Tức **50%** ký tự bị thay bằng `U+FFFD (�)`. Vì sao đúng 50%? File chứa byte 0x00..0xFF lặp lại:
   128 byte `0x00–0x7F` là ASCII hợp lệ, 128 byte `0x80–0xFF` đứng lẻ không phải UTF-8 hợp lệ
   → mỗi byte đó bị thay bằng `�`. **Không có exception nào** → dữ liệu hỏng âm thầm.
5. Con số "chậm hơn bao nhiêu lần" thay đổi theo máy/JDK/ổ đĩa — **không học thuộc con số**, chỉ cần
   nắm: *Byte nhanh hơn; Char có bước decode; Char + file nhị phân = sai dữ liệu*.

### 1.4. Đọc tab "Demo doc/ghi"

- **Phần 1 – Ghi file:** ghi cùng 20.000 dòng tiếng Việt bằng 3 cách
  (`FileOutputStream` + `getBytes(UTF_8)`, `FileWriter(UTF_8)`, `OutputStreamWriter(UTF_8)`), kèm thời gian.
  Điểm cần nói: với ByteStream **lập trình viên tự encode**, với CharStream **thư viện encode hộ**.
- **Phần 2 – Kiểm chứng:** 3 file có **cùng số byte** → `Giống nhau? : CÓ`.
- **Phần 3 – Đọc lại:** ByteStream đếm ra **số BYTE**, CharStream đếm ra **số KÝ TỰ** — hai số **khác nhau**
  và đó **không phải lỗi**: ký tự có dấu chiếm 2–3 byte trong UTF-8.
- **Phần 4:** khi nào dùng `FileWriter`, `OutputStreamWriter`, `FileOutputStream`.

### 1.5. Đọc tab "Bay encoding"

- **Mục 1:** phiên bản Java, `defaultCharset` (từ JDK 18 luôn là UTF-8), `native.encoding` (charset thật của OS).
- **Mục 2:** chuỗi gốc có dấu → `Số KÝ TỰ` < `Số BYTE trên đĩa`.
- **Mục 3:** cùng một file đọc lại bằng nhiều charset. UTF-8 → `Giống gốc? : CÓ`;
  charset sai (vd. windows-1252) → ra chữ rác kiểu `áº¿` (**mojibake**), `Giống gốc? : KHÔNG`.
- **Kết luận:** *luôn truyền charset tường minh*, đừng tin giá trị mặc định.

---

## 2. Đề tài 2 — Buffered Stream (Khánh)

**Mục tiêu:** chứng minh `BufferedInputStream/BufferedOutputStream` giảm số system call, đo ảnh
hưởng của **kích thước buffer** khi sao chép file lớn và vẽ biểu đồ **tốc độ I/O**.

### 2.1. Cách test

1. Menu → `2` → GUI → nhấn nút chạy. Thanh tiến trình chạy liên tục (không biết trước thời gian vì có warm-up).
   Mất khoảng **10–30 giây** tuỳ máy.
2. Đợi `Benchmark hoan tat!` rồi đọc **bảng** + **biểu đồ ngang 2 khối**.

### 2.2. Hai nhóm phép đo — KHÔNG ĐƯỢC SO CHÉO

| Nhóm | File | Phương pháp | Câu hỏi trả lời |
|---|---|---|---|
| **A** | **1 MB** | `Unbuf byte-by-byte`, `Buffered byte-by-byte` | Code đọc/ghi lắt nhắt từng byte thì buffer giúp được bao nhiêu? |
| **B** | **100 MB** | `Unbuf chunk 8KB`, `Buffered 8KB`, `Buffered 32KB`, `Buffered 64KB` | Khi đã đọc theo khối rồi thì cỡ buffer còn quan trọng không? |

Nhóm A phải dùng file nhỏ vì đọc từng byte **không buffer** cực chậm (1 MB ≈ 1–3 giây; 100 MB sẽ mất vài phút).
Vì 2 nhóm chạy trên **2 kích thước file khác nhau** → biểu đồ tách 2 khối, mỗi khối một thang đo riêng.
**So dòng của nhóm A với dòng của nhóm B là sai.**

### 2.3. Đọc bảng kết quả

| Cột | Ý nghĩa |
|---|---|
| `Nhom` | A hoặc B |
| `Phuong phap` | Tên phương pháp sao chép |
| `File` | Kích thước file nguồn |
| `Thoi gian (ms)` | Thời gian trung vị (đo như đề 1) |
| `So sanh trong nhom` | **Nhóm A:** `x.x nhanh hon` = nhanh hơn `Unbuf byte-by-byte` bao nhiêu lần. **Nhóm B:** `MB/s` = thông lượng |

**Thông lượng (MB/s)** = kích thước file nguồn ÷ thời gian (giây). Ví dụ 100 MB trong 0.171 s → ~585 MB/s.
(Thực tế đĩa chịu gấp đôi vì vừa đọc vừa ghi; con số tính trên dữ liệu nguồn.)
Biểu đồ vẽ **độ dài thanh = MB/s**, nhãn cạnh thanh ghi `MB/s · ms`.

**Số liệu mẫu (máy test):**

| Nhóm | Phương pháp | File | Thời gian (ms) | So sánh trong nhóm |
|---|---|---|---|---|
| A | Unbuf byte-by-byte    | 1 MB   | 975.94 | 1.0x (mốc) — ~1 MB/s |
| A | Buffered byte-by-byte | 1 MB   | 44.64  | **21.9x nhanh hơn** — ~22 MB/s |
| B | Unbuf chunk 8KB       | 100 MB | 214.42 | 466 MB/s |
| B | Buffered 8KB          | 100 MB | 170.78 | 586 MB/s |
| B | Buffered 32KB         | 100 MB | 158.99 | 629 MB/s |
| B | Buffered 64KB         | 100 MB | 184.93 | 541 MB/s |

### 2.4. Cách đọc / kết luận

1. **Nhóm A là kết quả chắc chắn nhất:** buffer nhanh hơn **từ hàng chục đến hàng trăm lần** (tuỳ máy: SSD trên
   Windows thường >100x, WSL/ổ ảo thì thấp hơn). Lý do: không buffer thì **mỗi `read()`/`write()` 1 byte = 1 system call**
   (chuyển User mode → Kernel mode). 1 MB = ~1 triệu lần đọc + 1 triệu lần ghi. Có buffer 8 KB: 1 system call
   phục vụ 8192 byte → số system call giảm ~8000 lần.
2. **Nhóm B chênh lệch nhỏ:** khi code **đã tự đọc theo khối 8 KB**, bọc thêm `BufferedStream` không bớt được
   nhiều system call → các phương pháp chỉ chênh nhau vài chục %. Buffer lớn hơn (32/64 KB) thường nhanh hơn 8 KB
   một chút.
3. **Điều KHÔNG kết luận được:** buffer nào "tối ưu" (32 KB hay 64 KB). Chúng chênh nhau vài ms và thứ tự
   **đảo qua đảo lại giữa các lần chạy, ngay trên cùng một máy** (ví dụ mẫu ở trên, 64 KB lại chậm hơn 8 KB).
   Muốn chọn buffer tối ưu phải đo nhiều lần trên chính môi trường chạy thật.
4. Nếu số của bạn khác bảng mẫu thì **bình thường**. Chỉ cần đúng xu hướng:
   *Nhóm A: Buffered ≫ Unbuf. Nhóm B: chênh nhỏ, không có người thắng tuyệt đối.*

---

## 3. Đề tài 3 — Server TCP đa luồng (ThreadPool) (Quân)

**Mục tiêu:** server TCP dùng `ExecutorService` (fixed pool **10 worker**), nhận chuỗi và **trả về chuỗi đảo ngược**;
phục vụ **≥ 3 client đồng thời**.

| Thông số | Giá trị |
|---|---|
| Cổng | **5000** |
| Pool | 10 worker (`worker-1`…`worker-10`) |
| Giao thức | Mỗi dòng văn bản UTF-8 → server trả 1 dòng đảo ngược |
| Client timeout đọc | 10 giây |

### 3.1. Cách test A — Tự động (nhanh nhất, nên dùng khi demo)

Menu → `3` → `c`. Chương trình tự bật server ở **cổng ngẫu nhiên** (không đụng cổng 5000), tạo **5 client
cùng xuất phát** (CountDownLatch), mỗi client gửi 3 câu mang tên riêng và tự kiểm tra câu trả lời.

**Output thật (phần cuối):**

```
+------------+--------------+----------+
| Client     | Dung / Gui   | Ket qua  |
+------------+--------------+----------+
| Client-1   |     3 / 3    | DAT      |
| Client-2   |     3 / 3    | DAT      |
| Client-3   |     3 / 3    | DAT      |
| Client-4   |     3 / 3    | DAT      |
| Client-5   |     3 / 3    | DAT      |
+------------+--------------+----------+
Tong so client server da phuc vu : 5
So client duoc phuc vu DONG THOI : 5 (ThreadPool 10 worker)

[OK] KET LUAN: server phuc vu dong thoi nhieu client va tra loi dung cho tung client.
```

**Đọc số liệu:**

- `Dung / Gui = 3 / 3`: cả 3 câu trả lời của client đó đều đúng bằng chuỗi đảo ngược mong đợi.
  Mỗi client gửi chuỗi **có tên riêng** (`Client-2 gui cau 3`) → nếu server lẫn dữ liệu giữa các
  kết nối, client sẽ nhận câu của người khác và bị `LOI`.
- `Tong so client server da phuc vu`: tổng số kết nối đã accept.
- **`So client duoc phuc vu DONG THOI` (peak)** là con số chứng minh **song song**: 5 nghĩa là có thời điểm
  cả 5 client cùng được 5 worker phục vụ. Nếu server xử lý tuần tự, con số này sẽ là 1.
- Điều kiện **ĐẠT**: mọi client `DAT` **và** peak ≥ 3. Chạy bằng terminal thì exit code `0` = ĐẠT, `1` = không đạt.

**Đọc log (phía trên bảng):**

```
[03:27:09.488][worker-1] Client /127.0.0.1:51096 da ngat ket noi
[03:27:09.489][worker-4] Client /127.0.0.1:51122 da ngat ket noi
[03:27:09.489][worker-1] Worker ranh, tra ve pool (con phuc vu: 4)
```

- `[giờ:phút:giây.mili]` là thời điểm; `[worker-N]` là **tên thread** đang xử lý.
- Các client khác nhau do **các worker khác nhau** xử lý, và dòng log của chúng **xen kẽ nhau theo thời gian**
  (cùng mốc mili giây) → bằng chứng chạy song song.
- `/127.0.0.1:51096` là IP và **cổng tạm (ephemeral port)** của client do OS cấp; mỗi client một cổng khác.
- `(dang phuc vu: N)` / `(con phuc vu: N)` là số client đang được phục vụ tại thời điểm đó.

### 3.2. Cách test B — Bằng tay (≥ 3 client)

| Cửa sổ | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Menu `3` → `a` (Server) | `Server lang nghe cong 5000, ThreadPool co dinh 10 worker` |
| 2 | Menu `3` → `b`, Enter (localhost), gõ `abc` | `Server tra < cba` |
| 3 | Menu `3` → `b`, gõ `xyz` | `Server tra < zyx` |
| 4 | Menu `3` → `b`, gõ `Xin chào` | `Server tra < oàhc niX` |

Giữ **cả 3 client mở cùng lúc**. Ở cửa sổ server phải thấy 3 worker khác nhau và số `dang phuc vu` lên tới **3**.
Gõ `quit` ở client để thoát → server in `da ngat ket noi` + `Worker ranh, tra ve pool`.

**Test thêm (nếu muốn chứng minh hiểu sâu):**
- Đóng ngang một cửa sổ client → server in `Loi voi client ...: Connection reset`, **các client khác vẫn chạy bình thường**.
- Pool có 10 worker: client thứ 11 vẫn kết nối được (kernel bắt tay xong) nhưng **phải xếp hàng** đến khi có
  worker rảnh; nếu chờ quá 10 giây, client in `Co the pool da day va client nay dang phai xep hang.`

---

## 4. Đề tài 4 — TCP Client-Server (Quân)

**Mục tiêu:** minh hoạ kiến trúc Client/Server và vai trò của `ServerSocket` so với `Socket`.
Client gửi chuỗi → server in ra console và trả `Received OK`.

| Thông số | Giá trị |
|---|---|
| Cổng | **12345** |
| Kiểu server | **Tuần tự (iterative)**: phục vụ xong client này mới tới client sau |
| Phản hồi | Luôn là `Received OK` |

### 4.1. Cách test

1. Cửa sổ 1: menu `4` → `a` (Server).
2. Cửa sổ 2: menu `4` → `b` (Client), Enter để dùng `localhost`, gõ vài câu, cuối cùng gõ `quit`.

**Output thật – Client:**
```
[OK] Da ket noi server 127.0.0.1:12345 tu cong cuc bo 36140
Go chuoi roi Enter de gui. Go 'quit' de thoat.

Gui  > Xin chao
Nhan < Received OK
Gui  > abc
Nhan < Received OK
Gui  > quit
Da ngat ket noi.
```

**Output thật – Server:**
```
Server lang nghe tren cong 12345. ...
 Dang cho client ket noi (accept)...
[OK] Client ket noi: 127.0.0.1:36140  ->  server 127.0.0.1:12345
[127.0.0.1:36140] Xin chao
[127.0.0.1:36140] abc
Client 127.0.0.1:36140 da ngat ket noi.

 Dang cho client ket noi (accept)...
```

### 4.2. Cách đọc

- `127.0.0.1:36140 -> server 127.0.0.1:12345` chính là **bộ 4 định danh kết nối TCP**
  (IP client, cổng client, IP server, cổng server). Cổng client (36140) do OS cấp ngẫu nhiên và **đổi mỗi lần chạy**;
  cổng server cố định là 12345.
- `Dang cho client ket noi (accept)...` → `ServerSocket.accept()` đang **block**. Khi in ra `[OK] Client ket noi`
  nghĩa là bắt tay 3 bước (SYN → SYN-ACK → ACK) đã xong.
- `[ip:port] <nội dung>` → server nhận đúng từng dòng client gửi.
- `da ngat ket noi` → client đóng socket, `readLine()` trả `null` (nhận FIN), server quay lại `accept()`.

### 4.3. Test để so sánh với đề 3

Mở **2 client cùng lúc**: client thứ 2 kết nối được nhưng gõ gì cũng **không nhận phản hồi** cho tới khi
client 1 gõ `quit` (hoặc sau 10 giây báo `server khong phan hoi`). Đó là đặc điểm của **server tuần tự**;
đề 3 giải quyết vấn đề này bằng ThreadPool.

---

## 5. Đề tài 5 — UDP Ping-Pong (Hiếu)

**Mục tiêu:** minh hoạ UDP không kết nối: client gửi `Ping`, server trả `Pong`; đo **RTT** và **tỉ lệ mất gói**.

| Thông số | Giá trị |
|---|---|
| Cổng server | **9876/UDP** |
| Số gói | 4, cách nhau 1 giây |
| Timeout mỗi gói | 2000 ms |
| Buffer nhận | 1024 byte |

### 5.1. Cách test

1. Cửa sổ 1: menu `5` → `a` (Server). Server in danh sách **IP của máy** (dùng khi test 2 máy) và lệnh `netstat`.
2. Cửa sổ 2: menu `5` → `b` (Client), Enter = `localhost`.

**Output thật – Client:**
```
Gui 4 goi "Ping" toi 127.0.0.1:9876 tu cong cuc bo 49253

[OK] Goi #1: nhan "Pong" tu 127.0.0.1:9876 - RTT = 4.23 ms
[OK] Goi #2: nhan "Pong" tu 127.0.0.1:9876 - RTT = 1.34 ms
[OK] Goi #3: nhan "Pong" tu 127.0.0.1:9876 - RTT = 0.97 ms
[OK] Goi #4: nhan "Pong" tu 127.0.0.1:9876 - RTT = 1.14 ms

--- Thong ke ---
Da gui = 4, Nhan duoc = 4, Mat = 0 (0% mat goi)
```

**Output thật – Server:**
```
Server UDP lang nghe cong 9876.
Dia chi IP cua may nay (nhap o Client tren may khac):
   * 172.26.39.246  (eth0)
Nhan "Ping" tu 127.0.0.1:49253  ->  tra "Pong"
...
```

### 5.2. Cách đọc số liệu

- **RTT (Round-Trip Time)** là thời gian từ lúc `send()` gói Ping tới lúc `receive()` được Pong, đo bằng `nanoTime`.
  - Gói **#1 thường chậm nhất** (ở đây 4.23 ms) vì là lần đầu: JIT chưa biên dịch, cache chưa nóng. Các gói sau ổn định ~1 ms.
  - Với `localhost`, RTT < 5 ms là bình thường. **Qua LAN/Wi-Fi** thì từ vài ms đến vài chục ms.
- **Thống kê:** `Mat = gửi − nhận`, `% mat goi = Mat ÷ 4 × 100`, giống lệnh `ping` của hệ điều hành.
- Server trả lời đúng `IP:cổng` lấy **từ chính gói vừa nhận** (`127.0.0.1:49253`). Vì UDP **không có kết nối**,
  server chỉ biết "trả cho ai" nhờ địa chỉ người gửi nằm trong gói.
- **Một `DatagramSocket` phục vụ mọi client** (khác TCP: mỗi client một `Socket`).

### 5.3. Test mất gói (chứng minh "UDP không đảm bảo")

Chạy **client khi KHÔNG bật server** (hoặc tắt server giữa chừng):

```
[X] Goi #1: qua 2000 ms khong co phan hoi (mat goi hoac server chua chay)
...
Da gui = 4, Nhan duoc = 0, Mat = 4 (100% mat goi)
```

Client **không báo lỗi kết nối** như TCP (vì UDP không bắt tay), chỉ có thể phát hiện bằng **timeout**
(`setSoTimeout`). Trên một số OS sẽ thấy `cong ... khong co chuong trinh nao nghe` (ICMP Port Unreachable).

Nếu gửi nội dung khác `Ping` (vd. bằng `nc -u localhost 9876`), server trả `Khong hieu "..." - hay gui "Ping"`.

### 5.4. Kiểm tra cổng bằng netstat

Trong khi server đang chạy:

```bash
# Windows
netstat -an -p UDP | findstr 9876
#   UDP    0.0.0.0:9876    *:*
# Linux / macOS
ss -uan | grep 9876        # hoặc: netstat -anu | grep 9876
```

Cách đọc: `0.0.0.0:9876` = server gắn vào **mọi card mạng** ở cổng 9876; `*:*` = không có địa chỉ đầu bên kia
(**không có trạng thái LISTEN/ESTABLISHED như TCP** vì UDP không kết nối).

---

## 6. Đề tài 6 — Java RMI (Hiếu)

**Mục tiêu:** gọi hàm từ xa qua RMI theo đường Client → **Stub** → (mạng) → **Skeleton** → **Server**, và minh hoạ thứ tự 4 bước.

| Thông số | Giá trị |
|---|---|
| RMI Registry | cổng **1099** |
| Tên dịch vụ | `CalculatorService` |
| Interface | `CalculatorInterface` (add, subtract, multiply, divide) |

### 6.1. Cách test

1. *(Tuỳ chọn, đúng yêu cầu "chạy qua rmiregistry")* mở terminal ở `target/classes` và chạy:
   ```bash
   cd NetworkProgramming_Project/target/classes
   rmiregistry 1099
   ```
   Nếu **không** chạy bước này, server tự tạo registry trong JVM của nó và demo vẫn chạy.
2. Cửa sổ server: menu `6` → `a`.
   - Có registry sẵn: `Da ket noi rmiregistry co san o cong 1099`
   - Không có: `Chua co rmiregistry -> tu tao registry o cong 1099`
   - Sau đó: `[OK] Da dang ky "CalculatorService" vao registry. Cac ten hien co: CalculatorService`
3. Cửa sổ client: menu `6` → `b`, Enter = `localhost`. Client tự gọi 5 phép tính.
4. **Đặt 2 cửa sổ cạnh nhau** để thấy đủ thứ tự ① → ② → ③ → ② → ④.

### 6.2. Output thật & cách đọc

**Client – bước (0) lookup:**
```
(0) lookup("CalculatorService") tai registry localhost:1099 -> nhan ve STUB:
    Lop cua stub     : jdk.proxy1.$Proxy0  (dynamic proxy do RMI sinh ra)
    Xu ly loi goi boi: java.rmi.server.RemoteObjectInvocationHandler
    Tro toi          : Proxy[...UnicastRef [liveRef: [endpoint:[127.0.1.1:34393](remote),objID:[...]]]]
```
- Thứ client nhận về **không phải đối tượng Calculator thật** mà là **Stub**: một dynamic proxy
  (`$Proxy0`) cũng implements `CalculatorInterface`.
- `endpoint:[127.0.1.1:34393]` là **địa chỉ + cổng thật** của đối tượng bên server (cổng ngẫu nhiên do
  `UnicastRemoteObject` export, **khác** cổng 1099 của registry). Registry chỉ đóng vai "danh bạ".

**Client – bước ① và ④:**
```
(1) [Stub] Goi calc.add(15, 5) -> Stub dong goi tham so, gui qua TCP toi Server
(4) [Stub] Nhan va giai ma ket qua: add(15, 5) = 20.0
...
(1) [Stub] Goi calc.divide(1, 0) -> Stub dong goi tham so, gui qua TCP toi Server
(4) [Stub] Nhan exception tu Server: ArithmeticException - Khong the chia cho 0
```

**Server – bước ② và ③:**
```
(2) [Skeleton] RMI runtime nhan loi goi add(15.0, 5.0) tu client 127.0.0.1 tren thread 'RMI TCP Connection(2)-127.0.0.1', giai ma tham so roi chuyen cho Server
(3) [Server]   Thuc thi add(15.0, 5.0) = 20.0 -> Skeleton dong goi ket qua gui ve Stub
...
(3) [Server]   divide(1.0, 0.0) loi: Khong the chia cho 0 -> exception duoc gui ve Client
```

| Thấy gì | Ý nghĩa |
|---|---|
| Kết quả `20.0, 10.0, 75.0, 3.0` | 15+5, 15−5, 15×5, 15÷5, tính **ở server** rồi trả về client |
| Client ghi `15`, server in `15.0` | Tham số được **marshal** (tuần tự hoá) thành `double` rồi gửi qua mạng |
| `tu client 127.0.0.1` | `RemoteServer.getClientHost()`: IP của bên gọi |
| Thread `RMI TCP Connection(2)-...` | Hàm chạy trên **thread của RMI runtime**, không phải `main` → bằng chứng lời gọi tới từ mạng |
| `divide(1, 0)` → `ArithmeticException` ở client | Exception ném ở server được **tuần tự hoá và ném lại** ở client, như gọi hàm cục bộ |

**Ghi chú lý thuyết:** từ Java 1.2 không còn lớp Skeleton riêng (do `rmic` sinh); vai trò Skeleton do RMI runtime
đảm nhiệm. Từ Java 5, Stub là dynamic proxy. Nhãn `[Skeleton]` trong log được in ngay khi runtime vừa giao lời gọi
cho method của server.

---

## 7. Test trên 2 máy (mạng LAN)

Áp dụng cho đề 3, 4, 5, 6. Hai máy phải **cùng mạng** (cùng Wi-Fi / LAN).

1. **Máy A (server):** chạy server, rồi lấy IP của máy:
   - Server đề 5 tự in IP (`* 192.168.x.x (Wi-Fi)`).
   - Hoặc: Windows dùng `ipconfig` (dòng *IPv4 Address*); Linux/macOS dùng `ip a` / `ifconfig`.
2. **Mở firewall** trên máy A cho cổng tương ứng (5000/TCP, 12345/TCP, 9876/UDP, 1099/TCP + cổng ngẫu nhiên
   của RMI). Cách nhanh khi demo: bấm cho phép **Java(TM) Platform SE binary** khi Windows hỏi, hoặc tạm tắt firewall cho mạng Private.
3. **Máy B (client):** chạy client; khi được hỏi địa chỉ server thì nhập **IP máy A** thay vì nhấn Enter.
4. **Riêng đề 6 (RMI):** server phải chạy với hostname là IP thật. Nếu không, stub sẽ chứa `127.0.1.1`
   và client ở máy khác **không gọi được**:
   ```bash
   java -cp target/classes com.project_final.networkprogramming_project.detai6_rmi.CalculatorServer 192.168.1.10
   # hoặc thêm VM option: -Djava.rmi.server.hostname=192.168.1.10
   ```
   (Trong NetBeans: Project Properties → Run → VM Options.)
5. Khi chạy qua LAN, log server sẽ hiện **IP máy B** thay cho `127.0.0.1`, và RTT của đề 5 lớn hơn (từ vài đến vài chục ms).

---

## 8. Bảng lỗi thường gặp

| Thông báo | Nguyên nhân | Cách xử lý |
|---|---|---|
| `[X] Cong 5000/12345 dang bi chuong trinh khac chiem` | Server cũ chưa tắt (tab NetBeans cũ vẫn chạy) | Tắt tab Output cũ (nút ■), hoặc chạy server với cổng khác |
| `[X] Cong UDP 9876 dang bi chuong trinh khac chiem` | Như trên, nhưng với UDP | Như trên |
| `[X] Khong ket noi duoc localhost:...  Server da chay chua?` | Chưa chạy server / sai IP / firewall chặn | Chạy server trước; kiểm tra IP và firewall |
| `[X] Qua 10 giay server khong tra loi` (đề 3) | Pool 10 worker đã đầy, client đang xếp hàng | Đóng bớt client |
| `[X] Qua 10 giay server khong phan hoi` (đề 4) | Server tuần tự đang bận với client khác | Cho client trước gõ `quit` |
| `[X] Goi #n: qua 2000 ms khong co phan hoi` (đề 5) | Server chưa chạy, sai IP, firewall chặn UDP, hoặc mất gói thật | Kiểm tra server và firewall |
| `[X] Registry khong co ten "CalculatorService"` (đề 6) | Server RMI chưa đăng ký hoặc đã tắt | Chạy lại server |
| Client máy khác báo `Connection refused to host: 127.0.1.1` (đề 6) | Thiếu `java.rmi.server.hostname` | Xem mục 7, bước 4 |
| `Loi khi chay benchmark: No space left on device` (đề 1/2) | Thiếu dung lượng đĩa | Giải phóng ~250 MB |
| `error: release version 26 not supported` khi build | JDK đang cài thấp hơn 26 | Cài JDK 26 hoặc sửa `maven.compiler.release` trong `pom.xml` |
| Chữ tiếng Việt trong GUI/tab hiển thị lỗi font | Font hệ thống thiếu glyph | Không ảnh hưởng số liệu; console đã dùng chữ không dấu |

---

## 9. Checklist trước khi demo

- [ ] Build thành công (F11), menu chính hiện ra.
- [ ] **Đề 1:** chạy benchmark, bảng có 4 dòng (5/10/20/40 MB), cột `Char/BIN` lớn nhất, log có dòng `BI HONG` (~50%).
- [ ] **Đề 2:** ở nhóm A, `Buffered byte-by-byte` nhanh hơn rõ rệt; nhóm B chênh nhau ít; biểu đồ tách 2 khối.
- [ ] **Đề 3:** menu `3 → c` ra 5/5 `DAT`, **peak = 5**, có dòng `[OK] KET LUAN`. Demo tay thêm 3 client nếu được yêu cầu.
- [ ] **Đề 4:** client nhận `Received OK`, server in đúng nội dung kèm `IP:cổng` của client.
- [ ] **Đề 5:** 4/4 gói `Pong`, `0% mat goi`; tắt server để thấy `100% mat goi`; chạy `netstat` thấy cổng 9876.
- [ ] **Đề 6:** client in (0)(1)(4), server in (2)(3); `divide(1,0)` ra `ArithmeticException` ở client.
- [ ] Tắt hết server cũ trước khi demo để tránh lỗi "cổng bị chiếm".
- [ ] (Nếu demo 2 máy) đã mở firewall, biết IP máy server, server RMI chạy với `java.rmi.server.hostname`.
