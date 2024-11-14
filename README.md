# 🎮 Game bắn súng Battle Royale
Game bắn súng 2D nhiều người chơi, hỗ trợ chơi online qua mạng LAN.

## ✨ Tính năng chính

### 🏃‍♂️ Gameplay
- **Chế độ chơi:**
  - Nhiều người chơi
- **Luật chơi:**
  - Người chơi bắn nhau trong map 2D
  - Mỗi người chơi có thanh máu
  - Người sống sót cuối cùng là người chiến thắng

### 🌟 Tính năng online
- **Kết nối đa nền tảng:**
  - Chơi qua mạng

### 🎮 Điều khiển
- W-A-S-D: Di chuyển nhân vật
- Space: Bắn
- ESC: Menu game

### 🔧 Mô tả hệ thống

- **Kiến trúc:**
  - Server xử lý logic game và đồng bộ dữ liệu
  - Client render đồ họa và gửi input người chơi

- **Giao thức sử dụng:**
  - TCP/IP cho kết nối mạng LAN
  - Đảm bảo tính đồng bộ và độ tin cậy của dữ liệu

- **Luồng hoạt động:**
  1. Server khởi động và lắng nghe kết nối
  2. Client kết nối vào server
  3. Server tạo phòng chờ cho người chơi
  4. Server xử lý:
     - Vị trí người chơi
     - Xử lý va chạm
     - Tính toán damage
     - Đồng bộ trạng thái game
  5. Client:
     - Gửi input người chơi
     - Render game theo dữ liệu từ server
     - Hiển thị UI/effects
     
### 🔨 Công nghệ sử dụng
- Java
- JavaFX cho GUI
- Socket Programming
