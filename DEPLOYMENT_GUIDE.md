# Free Cloud Hosting Deployment Guide
## VNR VJIET Canteen Management Web & Mobile Application

This guide explains how to deploy the **VNR VJIET Canteen App** for **FREE** online so that students and canteen staff can access it from their phones across the campus.

---

## 🚀 Option 1: Deploy on Render.com (Recommended - 100% Free)

[Render.com](https://render.com) offers free web service hosting with custom domains, automatic SSL, and zero configuration via Docker.

### Step 1: Push Code to GitHub
1. Create a free repository on [GitHub](https://github.com) (e.g. `vnr-canteen-app`).
2. Push your project from your terminal:
   ```bash
   cd /Users/srishanthmulupoju/Downloads/Canteennproject
   git init
   git add .
   git commit -m "Initial commit of VNR VJIET Canteen app"
   git branch -M main
   git remote add origin https://github.com/<your-username>/vnr-canteen-app.git
   git push -u origin main
   ```

### Step 2: Deploy on Render
1. Go to [https://dashboard.render.com](https://dashboard.render.com) and sign in with GitHub.
2. Click **New +** ➔ **Web Service**.
3. Select your repository `vnr-canteen-app`.
4. Configure settings:
   - **Name**: `vnr-vjiet-canteen`
   - **Environment**: `Docker` (Render will automatically detect the included `Dockerfile`)
   - **Region**: Singapore or Oregon
   - **Instance Type**: `Free`
5. Click **Create Web Service**.
6. Render will build and launch your container. In 2-3 minutes, you'll receive a live HTTPS URL:
   ```
   https://vnr-vjiet-canteen.onrender.com/student
   ```

---

## ⚡ Option 2: Deploy on Railway.app / Koyeb (Free Tier)

### Koyeb:
1. Sign up at [https://koyeb.com](https://koyeb.com).
2. Click **Create App** ➔ **GitHub**.
3. Select your repository. Koyeb reads the `Dockerfile` automatically.
4. Set Port to `8080` and click **Deploy**.

---

## 🏛️ Option 3: Oracle Cloud Free Tier (Always Free)

Oracle Cloud provides **Always Free** resources with:
- 2 Free Autonomous Oracle Databases (20 GB storage each).
- 4 Ampere A1 Compute Cores and 24 GB of RAM.

### Steps:
1. Create a free account at [https://cloud.oracle.com](https://cloud.oracle.com).
2. Create an **Always Free Autonomous Transaction Processing (ATP)** Oracle Database.
3. Download the database wallet zip file.
4. Launch an **Always Free Compute Instance** (Ubuntu 22.04 / Oracle Linux).
5. SSH into your VM and install Java 21:
   ```bash
   sudo apt update && sudo apt install -y openjdk-21-jre-headless
   ```
6. Copy `canteen-management-system-1.0.0.jar` to the server and run:
   ```bash
   java -jar canteen-management-system-1.0.0.jar --spring.profiles.active=oracle
   ```

---

## 💳 Razorpay & UPI Payment Gateway Setup

The application supports **Razorpay Payment Gateway SDK** with automated cryptographic HMAC-SHA256 signature verification and Swiggy-style confirmation:
- Set environment variables `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` in your hosting dashboard (e.g., Render Environment tab).
- Test keys are configured by default for instant local & staging payments.
- Supports all UPI apps (PhonePe, Google Pay, Paytm, Cred, BHIM), debit/credit cards, and NetBanking.
- Also supports direct UPI payments to payee ID: `BHARATPE2L0U0H4C3E50129@unitype`.

### Optional: Android WebView Wrapper Setup
The source code is located in:
```
/Users/srishanthmulupoju/Downloads/Canteennproject/android-app/
```
1. Open **Android Studio** ➔ **Open Project** ➔ Select `/Users/srishanthmulupoju/Downloads/Canteennproject/android-app`.
2. In `MainActivity.java`, update `CANTEEN_URL` to your production domain:
   ```java
   public static final String CANTEEN_URL = "https://<your-app>.onrender.com/student";
   ```
3. Click **Build** ➔ **Build Bundle(s) / APK(s)** ➔ **Build APK(s)**.
4. The generated APK will be in `android-app/app/build/outputs/apk/debug/app-debug.apk`.
