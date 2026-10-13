# Stock Market Management System - Installation & Setup Guide

This guide provides step-by-step instructions for installing, configuring, compiling, deploying, and testing the **Stock Market Management System** on Windows using Apache Tomcat 9, Oracle Database, and Java SE.

---

## Prerequisites Overview

| Component | Minimum Version | Recommended / Detected |
|---|---|---|
| **Operating System** | Windows 10 / 11 | Windows 11 (64-bit) |
| **Java Development Kit (JDK)** | JDK 11+ | JDK 11, 17, 21, or 27 |
| **Servlet Container** | Apache Tomcat 9.0.x | Apache Tomcat 9.0.123 |
| **Relational Database** | Oracle Database 11g XE / 19c / 21c | Oracle Database Express Edition (XE) |
| **Database CLI Tool** | Oracle SQL*Plus | SQL*Plus Client |
| **JDBC Driver** | Oracle JDBC Thin Driver | `ojdbc11.jar` / `ojdbc8.jar` / `ojdbc6.jar` |

---

## Step 1: Install a Compatible JDK

1. Download a compatible Java Development Kit (JDK 11, JDK 17, or JDK 21) from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [Eclipse Adoptium Temurin](https://adoptium.net/).
2. Run the Windows installer (for example, to `C:\Program Files\Java\jdk-27` or `C:\Program Files\Java\jdk-11`).

---

## Step 2: Verify Java Installation

Open **Command Prompt** (`cmd.exe`) and verify both the runtime and compiler:

```cmd
java -version
javac -version
```

**Expected output:**
```
java version "11.0.x" (or 17 / 21 / 27)
javac 11.0.x
```

---

## Step 3: Configure `JAVA_HOME` and `PATH` on Windows

1. Press `Windows Key + R`, type `sysdm.cpl`, and press **Enter**.
2. Go to the **Advanced** tab and click **Environment Variables**.
3. Under **System Variables**, click **New**:
   - **Variable name**: `JAVA_HOME`
   - **Variable value**: `C:\Program Files\Java\jdk-27` *(or your actual JDK path)*
4. Under **System Variables**, find `Path`, select it, and click **Edit**:
   - Click **New** and add: `%JAVA_HOME%\bin`
5. Click **OK** to save. Open a fresh Command Prompt and test:
```cmd
echo %JAVA_HOME%
```

---

## Step 4: Download and Extract Apache Tomcat 9

1. Download Apache Tomcat 9 (64-bit Windows zip) from the [official Apache Tomcat 9 site](https://tomcat.apache.org/download-90.cgi).
2. Extract the archive to a standard directory without spaces, e.g.:
   `C:\apache-tomcat-9.0.123` or `C:\apache-tomcat-9.0.123-windows-x64\apache-tomcat-9.0.123`.

---

## Step 5: Verify Tomcat's Java Configuration

Open Command Prompt, navigate to Tomcat's `bin` directory, and run the version utility:

```cmd
cd C:\apache-tomcat-9.0.123-windows-x64\apache-tomcat-9.0.123\bin
catalina.bat version
```

**Expected output:**
```
Server version: Apache Tomcat/9.0.123
JVM Version:    ...
JVM Vendor:     Oracle Corporation
```

---

## Step 6: Setting `JAVA_HOME` for Tomcat Startup

Tomcat requires `JAVA_HOME` or `JRE_HOME`. To ensure Tomcat always finds the correct JDK without relying on system-wide settings, edit `bin\setenv.bat` (create this file if it does not exist) inside your Tomcat directory:

```bat
set "JAVA_HOME=C:\Program Files\Java\jdk-27"
set "JRE_HOME=C:\Program Files\Java\jdk-27"
```

---

## Step 7: Install or Verify Oracle Database and SQL*Plus

1. Verify Oracle Database Windows services are running:
```powershell
Get-Service -Name "*Oracle*"
```
Make sure `OracleServiceXE` and `OracleXETNSListener` are in the **Running** state.
2. If stopped, start them via Windows Services (`services.msc`) or PowerShell:
```powershell
Start-Service OracleServiceXE
Start-Service OracleXETNSListener
```

---

## Step 8: Connect to Oracle Using SQL*Plus

Open Command Prompt and verify you can connect as `SYSTEM` or administrative user:

```cmd
sqlplus SYSTEM/oracle@//localhost:1521/xe
```
*(If your Oracle password differs, replace `oracle` with your actual password).*

Alternatively, connect with local operating system authentication:
```cmd
sqlplus / as sysdba
```

---

## Step 9: Verify Database Service Name (xe vs orcl)

Run the Oracle Listener utility:
```cmd
lsnrctl status
```
Look under `Services Summary`:
- If you see `Service "xe" has 1 instance(s)...`, your service name is **`xe`**.
- If you see `Service "orcl" has 1 instance(s)...`, your service name is **`orcl`**.

Update `src/main/resources/db.properties` accordingly:
```properties
db.url=jdbc:oracle:thin:@//localhost:1521/xe
```

> [!WARNING]
> **Important: Oracle XE Port 8080 Conflict Resolution**
> Oracle XE registers an embedded HTTP service (XDB) on port 8080 by default.
> If Tomcat also runs on port 8080, Tomcat will fail with `java.net.BindException: Address already in use: 8080`.
>
> **Fix Option A (Recommended):** Change Tomcat's HTTP port to `8085`:
> Open `Tomcat\conf\server.xml`, search for `<Connector port="8080" protocol="HTTP/1.1"` and change to:
> ```xml
> <Connector port="8085" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" />
> ```
> Then your application URL will be `http://localhost:8085/StockMarketManagement/`.
>
> **Fix Option B:** Disable Oracle XDB HTTP port via SQL*Plus:
> ```sql
> sqlplus / as sysdba
> EXEC DBMS_XDB.SETHTTPPORT(0);
> COMMIT;
> EXIT;
> ```

---

## Step 10: Execute the `database/stock_market.sql` Script

In Command Prompt, navigate to the project directory:

```cmd
cd C:\Users\sanja\.gemini\antigravity-ide\scratch\StockMarketManagement
sqlplus SYSTEM/oracle@//localhost:1521/xe @database/stock_market.sql
```

The script will:
1. Drop existing objects cleanly if present.
2. Create sequences: `USER_SEQ`, `STOCK_SEQ`, `ACCOUNT_SEQ`, `TXN_SEQ`, `PORTFOLIO_SEQ`.
3. Create tables: `USERS`, `STOCK`, `USER_ACCOUNT`, `STOCK_TRANSACTION`, `PORTFOLIO`.
4. Create triggers for primary key auto-generation (compatible with Oracle 11g, 12c, 19c, 21c).
5. Add foreign keys, unique constraints, and search indexes.
6. Seed sample stocks and demonstration user accounts (`admin` and `student`).

Verify in SQL*Plus:
```sql
SELECT SYMBOL, COMPANY, CURRENT_PRICE FROM STOCK;
```

---

## Step 11 & 12: Oracle JDBC Driver Setup

1. For Oracle 11g XE on your system, the driver `ojdbc6.jar` is located at:
   `C:\oraclexe\app\oracle\product\11.2.0\server\jdbc\lib\ojdbc6.jar`.
2. For JDK 11 / 17 / 21 connecting to modern Oracle instances, download `ojdbc11.jar` from [Oracle Maven Central](https://repo1.maven.org/maven2/com/oracle/database/jdbc/ojdbc11/21.11.0.0/ojdbc11-21.11.0.0.jar).
3. Place this driver JAR in **two** locations:
   - `StockMarketManagement\lib\` (for offline `javac` builds)
   - `StockMarketManagement\WebContent\WEB-INF\lib\` (for Tomcat deployment)

---

## Step 13: BCrypt Password Hashing Dependency

To ensure 100% zero-configuration builds without classpath errors, the project includes a standalone, pure Java implementation of standard Blowfish BCrypt in:
`com.stockmarket.util.BCrypt.java` and `com.stockmarket.util.PasswordUtil.java`.
This complies with standard RFC-compliant BCrypt hashes (`$2a$10$...`) and guarantees password safety without requiring external JAR downloads.

---

## Step 14: Configure Database Credentials Securely

Edit `src/main/resources/db.properties` (or `WebContent/WEB-INF/classes/db.properties`):

```properties
db.driver=oracle.jdbc.OracleDriver
db.url=jdbc:oracle:thin:@//localhost:1521/xe
db.user=SYSTEM
db.password=oracle
```

Alternatively, set system environment variables:
```cmd
set DB_URL=jdbc:oracle:thin:@//localhost:1521/xe
set DB_USER=SYSTEM
set DB_PASSWORD=oracle
```

---

## Step 15 & 16: Compile Java Files and Build the WAR

### Option A: Using the 1-Click `build.bat` Script (No Maven required)

Open Command Prompt, navigate to the project directory, and run:

```cmd
cd C:\Users\sanja\.gemini\antigravity-ide\scratch\StockMarketManagement
build.bat
```

This automated script will:
- Check `JAVA_HOME`.
- Detect `servlet-api.jar` from Tomcat or `lib\`.
- Detect `ojdbc6.jar` or `ojdbc11.jar`.
- Compile all Java sources in `src\main\java\` into `WebContent\WEB-INF\classes`.
- Package the entire `WebContent` directory into `StockMarketManagement.war`.
- Automatically copy `StockMarketManagement.war` into Tomcat's `webapps` folder.

### Option B: Using Maven

If Maven is installed on your command line (`mvn -v`):

```cmd
cd C:\Users\sanja\.gemini\antigravity-ide\scratch\StockMarketManagement
mvn clean package
```
The resulting WAR will be created at `target\StockMarketManagement.war`.

---

## Step 17: Deploy the WAR into Tomcat's `webapps` Directory

If not deployed automatically by `build.bat`, copy the WAR file manually:

```cmd
copy StockMarketManagement.war "C:\apache-tomcat-9.0.123-windows-x64\apache-tomcat-9.0.123\webapps\"
```

---

## Step 18: Start Apache Tomcat

Navigate to Tomcat's `bin` directory and execute `startup.bat`:

```cmd
cd C:\apache-tomcat-9.0.123-windows-x64\apache-tomcat-9.0.123\bin
startup.bat
```

A new Tomcat console window will open. Wait until you see:
`INFO [main] org.apache.catalina.startup.Catalina.start Server startup in [xxxx] milliseconds`.

---

## Step 19: Open the Application in the Browser

Open your web browser (Google Chrome, Edge, or Firefox) and navigate to:

```
http://localhost:8080/StockMarketManagement/
```
*(Or port `8085` if you modified the port in `server.xml` to avoid Oracle XE conflict).*

---

## Step 20: System Verification & Testing Walkthrough

Follow this verification flow:
1. **Landing Page:** Navigate to `http://localhost:8080/StockMarketManagement/index.html`.
2. **Registration:**
   - Click **Create Account** (`register.html`).
   - Register a user: Name `Alice Smith`, email `alice@lab.edu`, phone `555-0188`, username `alice`, password `Secret@123`.
   - Submit and observe immediate redirection to login.
3. **Login:**
   - Enter `alice` and `Secret@123`.
   - Successful login redirects to `dashboard.html`.
4. **Deposit Simulated Funds:**
   - On the top bar, click **+ Add Funds**.
   - Deposit `$10,000`. Observe your cash balance immediately increments.
5. **Market Watch & Buy Equities:**
   - Navigate to **Market Watch** (`stocks.html`).
   - Search for `AAPL` or filter by `Technology`.
   - Click **Trade** on Apple Inc.
   - Enter `10` shares and click **Transmit Order**.
   - An audited **Official Order Receipt Voucher** will pop up with Transaction ID and execution price.
6. **Portfolio Holdings:**
   - Click **My Portfolio** (`portfolio.html`).
   - Verify `AAPL` appears with 10 shares, weighted average buy price, current market valuation, and unrealized profit/loss.
7. **Sell Equities:**
   - On the portfolio page, click **Sell** on `AAPL`.
   - Sell `5` shares. Verify proceeds credit immediately to your cash balance.
8. **Transaction History:**
   - Click **Order History** (`transactions.html`).
   - Verify both the BUY and SELL transactions are recorded chronologically.
   - Click **Voucher** on any transaction to inspect the official receipt.
9. **Financial Reports:**
   - Click **Financial Reports** (`reports.html`).
   - Verify Historical Purchases, Sales Proceeds, Realized P&L, and visual Chart.js charts.
10. **Sign Out:**
    - Click **Sign Out**. The session will be invalidated, preventing unauthenticated access to protected pages.

---

## Step 21: Reading Tomcat Logs & Troubleshooting

Tomcat logs are stored in:
`C:\apache-tomcat-9.0.123-windows-x64\apache-tomcat-9.0.123\logs\`

Key log files:
- `catalina.yyyy-mm-dd.log`: Startup and global server exceptions.
- `localhost.yyyy-mm-dd.log`: Servlet-level exceptions and lifecycle errors.

### Common Errors & Solutions

| Symptom / Error | Root Cause | Solution |
|---|---|---|
| `java.net.BindException: Address already in use: 8080` | Oracle XE XDB HTTP listener is already using port 8080. | In Tomcat `conf\server.xml`, change Connector port to `8085` and access `http://localhost:8085/StockMarketManagement/`. |
| `java.lang.ClassNotFoundException: oracle.jdbc.OracleDriver` | Oracle JDBC driver missing in Tomcat classpath. | Copy `ojdbc6.jar` or `ojdbc11.jar` into `WebContent\WEB-INF\lib\` or Tomcat's `lib\` folder. |
| `ORA-01017: invalid username/password; logon denied` | Wrong database credentials in `db.properties`. | Verify username/password in `db.properties`. Try connecting directly in SQL*Plus to verify. |
| `ORA-12541: TNS:no listener` | Oracle Listener service is stopped. | Run `Start-Service OracleXETNSListener` in PowerShell. |
| `HTTP 401 Unauthorized` | Attempting to access dashboard directly without logging in. | Sign in first at `login.html` to establish an active HTTP session. |
