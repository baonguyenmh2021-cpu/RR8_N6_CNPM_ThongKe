
# RR8 – Home Rent Statistics Module

Hệ thống quản lý và thống kê hoạt động cho thuê phòng, tập trung vào **doanh thu, báo cáo, khuyến mãi, chiến lược Marketing và kế hoạch điều chỉnh giá phòng**.

> **Trạng thái dự án:** Đã xây dựng Backend và Frontend cho các luồng nghiệp vụ US-01 đến US-15 trong môi trường demo. Giai đoạn tiếp theo là bổ sung dữ liệu mẫu và kiểm thử toàn bộ hệ thống.

---

## 1. Giới thiệu dự án

**RR8 – Home Rent Statistics Module** là module thống kê trong hệ thống quản lý cho thuê phòng.

Hệ thống hỗ trợ người quản lý và quản trị viên theo dõi tình hình kinh doanh, phân tích doanh thu, đánh giá hiệu quả Marketing và quản lý các kế hoạch điều chỉnh giá phòng.

### Mục tiêu

- Thống kê và phân tích doanh thu cho thuê phòng.
- Hỗ trợ theo dõi dữ liệu đặt phòng.
- Trực quan hóa số liệu thông qua giao diện web.
- Quản lý chiến lược Marketing và đánh giá hiệu quả.
- Tạo, phê duyệt và thực thi kế hoạch điều chỉnh giá.
- Hỗ trợ các vai trò Manager và Admin trong môi trường demo.

---

## 2. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 17 |
| Framework | Spring Boot 4.1.2 |
| Build Tool | Maven Wrapper |
| Database | Microsoft SQL Server |
| Frontend | React |
| Ngôn ngữ Frontend | TypeScript |
| Build Tool Frontend | Vite |
| UI Library | Mantine UI |
| API Client | Axios |
| Routing | React Router |
| IDE | Visual Studio Code |
| Database Tool | SQL Server Management Studio (SSMS) |
| Version Control | Git / GitHub |

### Kiến trúc hệ thống

```text
                 NGƯỜI DÙNG
                     |
                     v
             React + TypeScript
                (Frontend)
                     |
                     | HTTP / REST API
                     v
               Spring Boot
                (Backend)
                     |
                     | JDBC
                     v
               SQL Server
                (Database)
```

Frontend sử dụng Axios để gửi request đến các REST API của Spring Boot.

Backend đảm nhiệm xử lý nghiệp vụ, kiểm tra dữ liệu và truy vấn SQL Server.

---

## 3. Chức năng chính

### 3.1. Nhóm thống kê và báo cáo

**US-01 đến US-08**

Nhóm chức năng phục vụ việc thống kê, phân tích và báo cáo hoạt động kinh doanh của hệ thống cho thuê phòng.

Các chức năng liên quan đến dữ liệu doanh thu, đặt phòng và các báo cáo nghiệp vụ.

Chi tiết từng Use Case được thể hiện trong tài liệu phân tích nghiệp vụ và mã nguồn tương ứng.

### 3.2. Nhóm kế hoạch điều chỉnh giá

| Use Case | Chức năng | Vai trò |
|---|---|---|
| US-09 | Tạo kế hoạch điều chỉnh giá | Manager |
| US-10 | Phê duyệt kế hoạch điều chỉnh giá | Admin |
| US-15 | Áp dụng và kết thúc kế hoạch giá | Admin |

Quy trình:

```text
Manager tạo kế hoạch (US-09)
              |
              v
Admin xem xét (US-10)
              |
              v
          Approved
              |
              v
Admin áp dụng (US-15)
              |
              v
           Applied
              |
              v
Admin kết thúc (US-15)
              |
              v
            Ended
```

Khi áp dụng, Backend thay đổi giá thực tế trong bảng `rooms`.

Khi kết thúc, Backend khôi phục giá phòng về giá ban đầu.

### 3.3. Nhóm chiến lược Marketing

| Use Case | Chức năng | Vai trò |
|---|---|---|
| US-11 | Tạo chiến lược Marketing | Manager |
| US-12 | Đánh giá hiệu quả Marketing | Theo quyền giao diện |
| US-13 | Xem và lọc chiến lược | Manager |
| US-14 | Cập nhật và chuyển trạng thái chiến lược | Manager |

Vòng đời chiến lược:

```text
             Planned
             /     \
            v       v
         Running  Cancelled
          /   \
         v     v
     Finished Cancelled
```

Các trạng thái hỗ trợ:

- `Planned`: Đã lập kế hoạch.
- `Running`: Đang triển khai.
- `Finished`: Đã hoàn thành.
- `Cancelled`: Đã hủy.

Chiến lược ở trạng thái `Finished` hoặc `Cancelled` không được tiếp tục cập nhật hay chuyển trạng thái.

---

## 4. Cấu trúc thư mục dự án

Cấu trúc các thành phần chính:

```text
demo/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/demo/
│   │   │       └── modules/
│   │   │           ├── marketing/
│   │   │           ├── priceplan/
│   │   │           └── ...
│   │   │
│   │   └── resources/
│   │
│   └── test/
│
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── app/
│   │   │   └── router.tsx
│   │   │
│   │   └── features/
│   │       ├── auth/
│   │       ├── marketing/
│   │       ├── priceplans/
│   │       ├── revenue/
│   │       └── ...
│   │
│   ├── package.json
│   └── vite.config.ts
│
├── native/
│
├── mvnw.cmd
├── pom.xml
└── README.md
```

**Lưu ý:** Đây là cấu trúc rút gọn, không thể hiện toàn bộ file và module.

Backend tổ chức code theo các lớp:

```text
Controller
    |
    v
Service
    |
    v
Repository
    |
    v
SQL Server
```

Frontend tổ chức theo từng tính năng để dễ phát triển và bảo trì.

---

## 5. Yêu cầu hệ thống

Trước khi chạy dự án, cần cài đặt:

| Phần mềm | Yêu cầu |
|---|---|
| Java JDK | 17 |
| Node.js | Phiên bản tương thích với Vite |
| npm | Đi kèm Node.js |
| Microsoft SQL Server | Đang hoạt động |
| Git | Để clone repository |
| VS Code | Khuyến nghị |
| SSMS | Khuyến nghị |

### Kiểm tra Java

```powershell
java -version
```

### Kiểm tra Node.js

```powershell
node -v
```

### Kiểm tra npm

```powershell
npm -v
```

Nếu Terminal nhận diện được các công cụ trên thì có thể tiếp tục.

---

## 6. Hướng dẫn clone dự án

Mở Terminal và chạy:

```bash
git clone <URL_REPOSITORY_CUA_NHOM>
```

Sau đó di chuyển đến thư mục dự án:

```bash
cd <TEN_THU_MUC_REPOSITORY>
```

Nếu repository chứa thư mục `demo`, tiếp tục:

```bash
cd demo
```

Đây là thư mục chứa:

```text
pom.xml
mvnw.cmd
src/
frontend/
```

---

## 7. Cấu hình Database

Dự án sử dụng **Microsoft SQL Server**.

### Bước 1. Khởi động SQL Server

Đảm bảo dịch vụ SQL Server đang chạy.

Có thể sử dụng SQL Server Management Studio để kiểm tra kết nối.

### Bước 2. Chuẩn bị Database

Tạo hoặc khôi phục database từ file SQL/backup do nhóm cung cấp.

Database cần có cấu trúc bảng và dữ liệu phù hợp với các module Backend.

Một số bảng được sử dụng trong dự án:

```text
users
categories
rooms
bookings
discounts
marketing_strategies
price_adjustment_plans
revenue_reports
```

Đây là các bảng tiêu biểu, không phải danh sách đầy đủ của database.

### Bước 3. Cấu hình Spring Boot

Kiểm tra file cấu hình trong:

```text
src/main/resources/
```

Ví dụ:

```text
application.properties
```

Hoặc file cấu hình tương ứng với profile đang sử dụng.

Các thông tin cần kiểm tra:

- JDBC URL
- Tên database
- SQL Server instance
- Cổng kết nối
- Phương thức xác thực

**Không đưa mật khẩu database hoặc thông tin bí mật lên GitHub.**

### Bước 4. SQL Server Windows Authentication

Nếu cấu hình JDBC sử dụng Windows Integrated Authentication, cần có thư viện DLL phù hợp với JDBC Driver.

Thư mục dự kiến:

```text
demo/native/
```

Lưu ý: DLL cần tương thích với phiên bản JDBC Driver và kiến trúc Windows đang sử dụng.

---

## 8. Hướng dẫn chạy Backend

### 8.1. Chạy bằng Windows CMD

Mở **Command Prompt (CMD)** tại thư mục `demo`.

Chạy lần lượt:

```cmd
set "RR8_DEMO_MANAGER_ID=2"
set "RR8_DEMO_ADMIN_ID=1"
set "PATH=%CD%\native;%PATH%"

mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local-demo"
```

### Giải thích

| Lệnh | Ý nghĩa |
|---|---|
| `RR8_DEMO_MANAGER_ID=2` | ID tài khoản Manager demo |
| `RR8_DEMO_ADMIN_ID=1` | ID tài khoản Admin demo |
| `PATH` | Bổ sung đường dẫn DLL SQL Server |
| `spring-boot:run` | Khởi động Backend |
| `local-demo` | Kích hoạt profile demo |

**Lưu ý:** ID `1` và `2` là giá trị sử dụng khi phát triển. Phải đảm bảo các ID này tồn tại và có đúng role trong bảng `dbo.users`.

### 8.2. Chạy bằng PowerShell

Nếu sử dụng PowerShell thay vì CMD:

```powershell
$env:RR8_DEMO_MANAGER_ID = '2'
$env:RR8_DEMO_ADMIN_ID = '1'
$env:PATH = "$PWD\native;$env:PATH"

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local-demo"
```

### 8.3. Kiểm tra Backend

Khi Spring Boot khởi động thành công, truy cập:

```text
http://localhost:8080
```

Một số API có thể được kiểm tra tại:

```text
http://localhost:8080/api/marketing-strategies
```

Backend mặc định chạy cổng `8080` trong môi trường phát triển đã sử dụng.

Để dừng Backend:

```text
Ctrl + C
```

---

## 9. Hướng dẫn chạy Frontend

Mở Terminal mới, giữ nguyên Terminal đang chạy Backend.

Di chuyển vào thư mục Frontend:

```powershell
cd frontend
```

Nếu đang đứng ở thư mục khác, hãy đảm bảo đường dẫn hiện tại là:

```text
demo/frontend
```

### Bước 1. Cài đặt dependencies

```bash
npm install
```

### Bước 2. Khởi động Frontend

```bash
npm run dev
```

### Bước 3. Mở trình duyệt

Truy cập:

```text
http://localhost:5173
```

Nếu Vite sử dụng cổng khác thì truy cập địa chỉ được in trên Terminal.

### Bước 4. Kiểm tra build

```bash
npm run build
```

Nếu build thành công, Frontend đã vượt qua bước kiểm tra build của dự án.

---

## 10. Vai trò người dùng

Bản demo hỗ trợ chuyển đổi giao diện giữa hai vai trò chính.

### Manager

Các chức năng tiêu biểu:

- Theo dõi các thống kê được phân quyền.
- Tạo kế hoạch điều chỉnh giá.
- Tạo chiến lược Marketing.
- Xem danh sách chiến lược.
- Đánh giá hiệu quả Marketing.
- Cập nhật thông tin chiến lược.
- Thay đổi trạng thái chiến lược.

### Admin

Các chức năng tiêu biểu:

- Xem các báo cáo được phân quyền.
- Xem xét kế hoạch điều chỉnh giá.
- Phê duyệt kế hoạch giá.
- Áp dụng kế hoạch đã được phê duyệt.
- Kết thúc kế hoạch giá và khôi phục giá phòng.

> Chức năng chuyển đổi Manager/Admin là cơ chế phục vụ demo. Không được xem đây là một hệ thống Authentication/Authorization hoàn chỉnh cho môi trường production.

---

## 11. REST API tiêu biểu

Các endpoint dưới đây là những API đã được sử dụng trong các luồng Marketing và kế hoạch giá.

### Marketing

| Method | Endpoint | Chức năng |
|---|---|---|
| POST | `/api/marketing-strategies` | Tạo chiến lược |
| GET | `/api/marketing-strategies` | Xem danh sách |
| GET | `/api/marketing-strategies?status=Running` | Lọc chiến lược |
| GET | `/api/marketing-strategies/{strategyId}/effectiveness` | Đánh giá hiệu quả |
| PUT | `/api/marketing-strategies/{strategyId}` | Cập nhật |
| PATCH | `/api/marketing-strategies/{strategyId}/status` | Chuyển trạng thái |

Ví dụ request chuyển trạng thái:

```http
PATCH /api/marketing-strategies/1/status
Content-Type: application/json
```

```json
{
  "status": "Running"
}
```

### Kế hoạch điều chỉnh giá

| Method | Endpoint | Chức năng |
|---|---|---|
| POST | `/api/price-plans/{planId}/apply` | Áp dụng kế hoạch |
| POST | `/api/price-plans/{planId}/end` | Kết thúc kế hoạch |

Ví dụ:

```http
POST /api/price-plans/10/apply
```

Hai API `apply` và `end` không yêu cầu JSON request body.

### Lưu ý khi thực thi kế hoạch giá

Backend chỉ cho phép áp dụng khi:

1. Kế hoạch có trạng thái `Approved`.
2. Đã đến ngày hiệu lực và chưa quá ngày kết thúc.
3. Loại phòng tồn tại.
4. Có phòng thuộc loại phòng tương ứng.
5. Các phòng trong cùng loại có giá đồng nhất.
6. Giá hiện tại khớp với giá cũ trong kế hoạch.
7. Không có kế hoạch `Applied` khác đang xung đột.

Khi kết thúc, Backend kiểm tra kế hoạch đang `Applied` và giá hiện tại khớp `new_price` trước khi khôi phục.

Các thay đổi được thực hiện trong transaction.

---

## 12. Kiểm thử hệ thống

### 12.1. Kiểm thử Backend

Chạy tại thư mục `demo`:

```powershell
.\mvnw.cmd test
```

### 12.2. Kiểm tra Frontend

Chạy tại thư mục `demo/frontend`:

```powershell
npm run build
```

### 12.3. Kiểm thử nghiệp vụ

Các nhóm kiểm thử cần thực hiện:

- Kiểm tra kết quả thống kê doanh thu.
- Đối chiếu dữ liệu với SQL Server.
- Kiểm tra bộ lọc và danh sách dữ liệu.
- Kiểm tra thao tác tạo chiến lược Marketing.
- Kiểm tra hiệu quả chiến lược Marketing.
- Kiểm tra cập nhật và chuyển trạng thái.
- Kiểm tra quy trình tạo và phê duyệt kế hoạch giá.
- Kiểm tra áp dụng và kết thúc kế hoạch giá.
- Kiểm tra điều kiện lỗi và dữ liệu không hợp lệ.

### 12.4. Kiểm tra dữ liệu SQL Server

Đếm chiến lược theo trạng thái:

```sql
SELECT
    status,
    COUNT(*) AS total
FROM dbo.marketing_strategies
GROUP BY status;
```

Đếm kế hoạch giá theo trạng thái:

```sql
SELECT
    status,
    COUNT(*) AS total
FROM dbo.price_adjustment_plans
GROUP BY status;
```

Kiểm tra giá phòng:

```sql
SELECT
    category_id,
    COUNT(*) AS room_count,
    MIN(price) AS min_price,
    MAX(price) AS max_price
FROM dbo.rooms
GROUP BY category_id;
```

**Cảnh báo:** US-15 cập nhật giá trực tiếp trong bảng `dbo.rooms`. Cần sao lưu database trước khi kiểm thử thao tác thay đổi giá.

---

## 13. Các lỗi thường gặp

### Backend không kết nối SQL Server

Kiểm tra:

- Dịch vụ SQL Server.
- JDBC URL.
- Tên database.
- Cổng kết nối.
- Cấu hình xác thực.
- DLL SQL Server nếu sử dụng Windows Authentication.

### Frontend không gọi được API

Kiểm tra:

- Backend có đang chạy không.
- Backend có sử dụng cổng `8080` không.
- Vite proxy có chuyển `/api` đến đúng Backend không.
- Tab Network trong Developer Tools.

### API trả về HTTP 403

Kiểm tra cấu hình tài khoản demo và role của người dùng.

### API trả về HTTP 404

Kiểm tra:

- Đường dẫn API.
- ID bản ghi.
- Profile `local-demo`.
- Route của React Router.

### API trả về HTTP 409

Đây thường là lỗi xung đột nghiệp vụ.

Ví dụ:

- Chuyển trạng thái Marketing không hợp lệ.
- Kế hoạch giá chưa được phê duyệt.
- Kế hoạch chưa đến ngày hiệu lực.
- Giá hiện tại không khớp với kế hoạch.
- Có kế hoạch khác đang được áp dụng.

---

## 14. Công việc tiếp theo

- [x] Xây dựng Backend Spring Boot.
- [x] Xây dựng Frontend React.
- [x] Kết nối Frontend và Backend.
- [x] Triển khai giao diện các Use Case đến US-15.
- [x] Tích hợp SQL Server trong môi trường demo.
- [ ] Bổ sung dữ liệu mẫu đủ lớn.
- [ ] Kiểm thử tích hợp US-01 đến US-15.
- [ ] Đối chiếu kết quả thống kê với SQL Server.
- [ ] Kiểm thử các trường hợp lỗi.
- [ ] Chụp ảnh minh chứng kiểm thử.
- [ ] Hoàn thiện báo cáo đồ án.

---

## 15. Lưu ý bảo mật

Dự án hiện được xây dựng cho mục đích học tập và demo.

Profile `local-demo` hỗ trợ sử dụng các tài khoản demo được cấu hình qua biến môi trường.

Không sử dụng cơ chế chuyển đổi vai trò demo làm hệ thống xác thực thực tế.

Nếu triển khai production, cần bổ sung:

- Authentication.
- Authorization.
- Quản lý phiên đăng nhập.
- Bảo vệ API.
- Quản lý thông tin bí mật.
- Cấu hình database an toàn.
- Ghi log và giám sát hệ thống.

---

## 16. Thông tin dự án

**Tên dự án:** RR8 – Home Rent Statistics Module

**Loại dự án:** Đồ án Công nghệ phần mềm

**Mục tiêu:** Xây dựng module thống kê và hỗ trợ quản lý hoạt động kinh doanh cho thuê phòng.

**Backend:** Java Spring Boot

**Frontend:** React + TypeScript

**Database:** Microsoft SQL Server

---

**RR8 – Home Rent Statistics Module**

*Tài liệu README phục vụ hướng dẫn cài đặt, chạy và phát triển hệ thống trong môi trường demo.*
