# Campus Canteen Food Ordering & Management Web Application

An end-to-end Web Application built with **Java (Spring Boot 3)**, **Oracle Database** architecture, **UPI Payments integration**, and **Role-Based Access Separation** (Student / Customer Portal and Canteen Kitchen / Staff Portal).

Designed specifically to eliminate crowded waiting lines during lunch breaks and lecture intervals by allowing students to pre-order food, pay instantly via UPI, and monitor their order's live status until their token number is called at the pickup counter.

---

## 🏗️ System Architecture & Workflow

Based on the role-separated lifecycle model:

```
[CUSTOMER / STUDENT]                                      [CANTEEN ADMIN / KITCHEN]
1. Browse Menu (Bootstrap UI)                                     │
   ├── Filter Categories, Veg/Non-veg                             │
   └── Search Dishes                                              │
         │                                                        │
2. Add to Cart & Review Quantity                                  │
         │                                                        │
3. Place Order & UPI QR Payment                                   │
   ├── Dynamic NPCI UPI QR Code                                   │
   └── GPay / PhonePe / Paytm Deep Link                           │
         │                                                        │
         ├──> [DATABASE: Oracle DB] <─────────────────────────────┤
         │    • Status: "ORDERED"                                 │
         │    • Generated Token # (e.g. #104)                     │
         │    • Payment Record Stored                             │
         │                                                        │
4. Real-Time Status Tracker                               6. Live Order Alert in Kitchen
   • Auto-updates every 3 seconds                            • Audible Audio Chime on New Order
   • [ORDERED] ─────────────> (Cancel Option?)               • Dynamic Badge Counter
         │                                                        │
         │                                          7. Admin Accepts & Prepares Order
         ├── [PREPARING] <─────────────────────────── Kitchen Updates Status to "PREPARING"
         │                                                        │
         │                                          8. Updates Status to "READY TO COLLECT"
         ├── [READY TO COLLECT] <──────────────────── Student Screen Alerts with Pickup Chime!
         │                                                        │
         │                                          9. Marks Order as "DELIVERED"
         └── [DELIVERED] <─────────────────────────── Transaction Completed & Closed
```

---

## 🌟 Key Features

### 1. Student / Customer Portal (`/student`)
- **Interactive Food Menu**: Filter by categories (Breakfast, Meals & Rice, Snacks & Chaat, Beverages), search dishes, and filter "Veg Only".
- **Real-Time Stock Reflection**: Unavailable dishes are marked "Sold Out" so students never order out-of-stock items.
- **Cart Management**: Add dishes, change quantities (+/-), and review order totals.
- **UPI QR Code Payment Integration**:
  - Dynamic QR Code generation on the fly (via ZXing).
  - NPCI standard URI (`upi://pay?pa=...&pn=...&am=...&cu=INR&tn=...`).
  - Compatible with Google Pay, PhonePe, Paytm, and BHIM.
  - Direct deep-link button for mobile devices.
- **Real-Time Customer Status Tracker (`/student/track/{orderNumber}`)**:
  - Prominent Token Display Card (e.g. `TOKEN #104`).
  - Animated 4-step progress stepper: `ORDERED` ➔ `PREPARING` ➔ `READY TO COLLECT` ➔ `DELIVERED`.
  - Live polling without page reload.
  - Celebratory audio chime and pickup banner when food status becomes **Ready to Collect**.
  - **Cancel Order Option**: Enabled while in `ORDERED` status; disabled automatically once cooking begins.
- **My Orders (`/student/my-orders`)**: Track active and past orders by College Roll Number.

### 2. Canteen Kitchen & Admin Portal (`/canteen/admin`)
- **Live Kitchen Order Board (`/canteen/admin/dashboard`)**:
  - Live order cards categorized with student roll number, items, and special instructions.
  - Web Audio API synthetic bell alert on every new incoming order.
  - One-click workflow buttons:
    - **Step 7**: *Accept & Prepare* (moves order to `PREPARING`).
    - **Step 8**: *Ready to Collect* (moves order to `READY_TO_COLLECT`).
    - **Step 9**: *Mark Delivered & Close* (moves order to `DELIVERED`).
- **Menu & Stock Management (`/canteen/admin/menu`)**:
  - Instant toggle switch for stock: mark dishes "In Stock" or "Out of Stock" during peak rushes.
  - Add new dishes modal (Name, Category, Price in ₹, Veg/Non-Veg, Prep Time, Description, Image URL).
  - Delete dishes.
- **Sales & Audit Reports (`/canteen/admin/reports`)**:
  - Total revenue calculation (verified UPI orders).
  - Daily orders count and fulfilled orders count.
  - Complete printable audit table.

---

## 🗄️ Oracle Database Integration

The application includes full native support for **Oracle Database (12c, 19c, 21c, 23ai, and Oracle XE)**.

### Oracle DDL Schema File:
The schema script is located at:
```
src/main/resources/schema-oracle.sql
```
It defines:
- `USERS`: College students and canteen staff.
- `CATEGORIES`: Food categories.
- `MENU_ITEMS`: Food dishes with prices, veg flags, prep times.
- `ORDERS`: Orders with token sequences, timestamps, roll numbers, status.
- `ORDER_ITEMS`: Individual order lines and quantities.
- `PAYMENTS`: UPI transaction records, UTR references, amounts, and QR representations.

### Connecting to your Oracle Database Instance:
Edit `src/main/resources/application-oracle.properties` with your Oracle DB connection details:
```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1521/XEPDB1
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.username=CANTEEN_USER
spring.datasource.password=canteen123
spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
```

Run with the `oracle` Spring profile:
```bash
java -jar target/canteen-management-system-1.0.0.jar --spring.profiles.active=oracle
```

*(Note: For immediate out-of-the-box evaluation without a running Oracle instance, the default `dev` profile uses an in-memory database in Oracle compatibility mode with sample data pre-seeded.)*

---

## 🚀 How to Run the Application

### Prerequisites:
- Java 21+ (`java -version`)
- Maven 3.8+ (Installed at `/opt/homebrew/bin/mvn`)

### 1. Build & Package:
```bash
cd /Users/srishanthmulupoju/Downloads/Canteennproject
mvn clean package -DskipTests
```

### 2. Start the Server:
```bash
java -jar target/canteen-management-system-1.0.0.jar
```
Or via Maven:
```bash
mvn spring-boot:run
```

### 3. Open in Browser:
- **Role Portal Selector**: [http://localhost:8080/portal](http://localhost:8080/portal)
- **Student Food Menu**: [http://localhost:8080/student](http://localhost:8080/student)
- **Student Order Tracking**: [http://localhost:8080/student/my-orders](http://localhost:8080/student/my-orders)
- **Canteen Kitchen Dashboard**: [http://localhost:8080/canteen/admin/dashboard](http://localhost:8080/canteen/admin/dashboard)
- **Menu & Stock Management**: [http://localhost:8080/canteen/admin/menu](http://localhost:8080/canteen/admin/menu)
- **Daily Sales Reports**: [http://localhost:8080/canteen/admin/reports](http://localhost:8080/canteen/admin/reports)
- **H2 Database Console** *(dev mode)*: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:canteendb`, user: `sa`, password: empty)

---

## 👥 Default Demo Credentials & Seed Data

| Role | Name | Email / Roll No | Access |
|---|---|---|---|
| **Student** | Rahul Sharma | Roll: `21BD1A0501` / `rahul@student.edu` | Place orders, UPI pay, track live status |
| **Canteen Admin** | Canteen Manager | ID: `STAFF-01` / `admin@canteen.edu` | Kitchen dashboard, prepare orders, stock toggle |

Pre-seeded live orders for immediate testing:
- **Token #101** (`ORD-20261011-101`) - Status: `ORDERED` (New incoming order)
- **Token #102** (`ORD-20261011-102`) - Status: `PREPARING` (In the kitchen)
- **Token #103** (`ORD-20261011-103`) - Status: `READY_TO_COLLECT` (Ready for pickup with audio chime)
