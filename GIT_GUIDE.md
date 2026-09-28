# 🚀 Hướng dẫn Setup & Git cho thành viên nhóm

## Bước 1: Clone repo về máy
```bash
git clone https://github.com/Khanh-4/NetworkProgramming_Project.git
cd NetworkProgramming_Project
```

## Bước 2: Chuyển sang nhánh của mình

### Khánh (Đề tài 1, 2):
```bash
git checkout detai1-2/khanh
```

### Quân (Đề tài 3, 4):
```bash
git checkout detai3-4/quan
```

### Hiếu (Đề tài 5, 6):
```bash
git checkout detai5-6/hieu
```

## Bước 3: Mở project bằng NetBeans
1. Mở NetBeans
2. File → Open Project
3. Chọn folder `NetworkProgramming_Project/NetworkProgramming_Project` (folder con chứa `pom.xml`)
4. NetBeans sẽ tự nhận diện Maven project

## Bước 4: Code trong package của mình

| Người | Package cần code |
|-------|-----------------|
| Khánh | `detai1_bytecharstream/` và `detai2_bufferedstream/` |
| Quân  | `detai3_ticketselling/` và `detai4_clientserver/` |
| Hiếu  | `detai5_udp/` và `detai6_rmi/` |

> ⚠️ **KHÔNG sửa file trong package của người khác!**

## Bước 5: Commit & Push thường xuyên
```bash
# Kiểm tra file đã thay đổi
git status

# Thêm tất cả file thay đổi
git add -A

# Commit với message rõ ràng
git commit -m "feat: hoàn thành TicketPool class cho đề tài 3"

# Push lên GitHub
git push origin <tên-nhánh-của-bạn>
```

### Quy ước commit message:
```
feat: thêm tính năng        → feat: add detai1 byte stream comparison
fix: sửa lỗi                → fix: detai3 race condition logging
docs: cập nhật tài liệu     → docs: add README instructions
refactor: tái cấu trúc      → refactor: detai2 extract file utils
```

## Bước 6: Khi hoàn thành → Merge vào main

### Cách 1: Tạo Pull Request trên GitHub (khuyến khích)
1. Vào GitHub repo
2. Click "Pull Requests" → "New Pull Request"
3. Chọn nhánh của mình merge vào `main`
4. Mô tả những gì đã làm
5. Cả nhóm review → Merge

### Cách 2: Merge bằng command line
```bash
# Chuyển về main
git checkout main

# Pull code mới nhất
git pull origin main

# Merge nhánh của mình
git merge detai1-2/khanh   # (thay bằng nhánh tương ứng)

# Push main lên
git push origin main
```

## Chạy Project
- **Chạy Main Menu**: Run file `NetworkProgramming_Project.java`
- Chọn số đề tài muốn demo trên console

## ⚠️ Lưu ý quan trọng
1. **Luôn pull trước khi push**: `git pull origin <nhánh>` để tránh conflict
2. **Không commit file build**: `.gitignore` đã cấu hình, nhưng kiểm tra lại trước khi push
3. **Commit thường xuyên**: Đừng để cả đống code rồi mới commit 1 lần
4. **Không sửa file `NetworkProgramming_Project.java`** (Main Menu) nếu không thống nhất trước
