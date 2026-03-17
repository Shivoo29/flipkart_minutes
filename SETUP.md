# Setup Guide — Flipkart Minutes

This guide walks you through running the Flipkart Minutes project from scratch on any machine. No prior Java or Maven experience required.

---

## What You Need

| Requirement | Version | Notes |
|---|---|---|
| Java (JDK) | 17 or higher | Java 21 also works |
| Apache Maven | 3.6 or higher | Used to build and run |
| Internet | — | First run downloads dependencies (~50 MB) |

---

## Step 1 — Install Java

### Check if Java is already installed
Open a terminal and run:
```bash
java -version
```
If you see `openjdk version "17..."` or higher, skip to Step 2.

---

### Install Java on Linux (Ubuntu / Debian)
```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
java -version
```

### Install Java on Linux (Arch / Manjaro)
```bash
sudo pacman -S jdk17-openjdk
java -version
```

### Install Java on macOS
```bash
# Using Homebrew
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version
```

### Install Java on Windows
1. Download the JDK 17 installer from: https://adoptium.net/temurin/releases/?version=17
2. Run the `.msi` installer — it sets up everything automatically
3. Open Command Prompt and verify: `java -version`

---

## Step 2 — Install Maven

### Check if Maven is already installed
```bash
mvn -version
```
If you see `Apache Maven 3.x.x`, skip to Step 3.

---

### Install Maven on Linux (Ubuntu / Debian)
```bash
sudo apt update
sudo apt install -y maven
mvn -version
```

### Install Maven on Linux (Arch / Manjaro)
```bash
sudo pacman -S maven
mvn -version
```

### Install Maven on macOS
```bash
brew install maven
mvn -version
```

### Install Maven on Windows
1. Download the binary zip from: https://maven.apache.org/download.cgi
   (pick `apache-maven-3.x.x-bin.zip`)
2. Extract to a folder, e.g. `C:\maven`
3. Add `C:\maven\bin` to your System Environment Variables → `PATH`
4. Open a new Command Prompt and verify: `mvn -version`

### Quick install (Linux/macOS — no sudo needed)
```bash
wget https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
tar -xzf apache-maven-3.9.6-bin.tar.gz -C ~/
echo 'export PATH="$HOME/apache-maven-3.9.6/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
mvn -version
```

---

## Step 3 — Get the Project

### If you have the folder already
```bash
cd /path/to/flipkart_minutes
```

### If you need to copy it
```bash
# Example: project is at /home/codemaster29/Documents/project/flipkart_minutes
cd /home/codemaster29/Documents/project/flipkart_minutes
```

Confirm you are in the right directory — you should see `pom.xml`:
```bash
ls pom.xml
# Expected output: pom.xml
```

---

## Step 4 — Run the Application

```bash
mvn spring-boot:run
```

**That's it.** Maven will:
1. Download all dependencies on the first run (~50 MB, cached after that)
2. Compile all source files
3. Start the application
4. Run all 9 demo scenarios automatically
5. Print the final dashboard and exit

---

## Expected Output

You will see structured output like this:

```
╔══════════════════════════════════════════════════════════╗
║       FLIPKART MINUTES — QUICK COMMERCE SYSTEM           ║
║            Standalone Demo / Evaluator Driver            ║
╚══════════════════════════════════════════════════════════╝

──────────────────────────────────────────────────────────────
  Scenario 1 — Basic Order Lifecycle
──────────────────────────────────────────────────────────────
  → onboard-customer C001 Alice
  → create-order C001 'Milk' qty=2
  Order ORD-1 | Customer: C001 | Item: Milk x2 | Status: ASSIGNED | Partner: P001
  → pick-up-order P001 ORD-1
  → complete-order P001 ORD-1
  Order ORD-1 | ... | Status: DELIVERED | Partner: P001
  ✓ Scenario 1 passed
  ...

═══════════════════════════════════════════════════════════════
  ALL DEMO SCENARIOS COMPLETED SUCCESSFULLY
═══════════════════════════════════════════════════════════════
```

---

## Build Without Running (Optional)

To compile only (no execution):
```bash
mvn compile
```

To build a standalone JAR:
```bash
mvn package -DskipTests
java -jar target/flipkart-minutes-1.0.0.jar
```

---

## Configuration

You can tweak settings in [src/main/resources/application.properties](src/main/resources/application.properties):

```properties
# How long before an undelivered order is auto-cancelled (default: 30 minutes)
flipkart.minutes.auto-cancel-minutes=30
```

To test auto-cancel faster, change `30` to `1` (1 minute) and re-run.

---

## Troubleshooting

### `bash: mvn: command not found`
Maven is not on your PATH. Use the full path:
```bash
/path/to/apache-maven-3.9.6/bin/mvn spring-boot:run
```

Or set PATH permanently:
```bash
echo 'export PATH="/path/to/apache-maven-3.9.6/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

---

### `java: command not found` or wrong version
Set JAVA_HOME explicitly:
```bash
# Find where Java is installed
find /usr /opt /home -name "java" -type f 2>/dev/null | grep bin/java | head -5

# Set it (replace the path with your actual path)
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

---

### First run is slow
Normal — Maven is downloading dependencies from the internet. Subsequent runs are fast (everything is cached in `~/.m2/`).

---

### `Error: LinkageError` or `UnsupportedClassVersionError`
Your Java version is too old. Run `java -version` and make sure it shows 17 or higher.

---

### Port conflicts / web errors
None expected — this is a **standalone application with no web server**. It runs, prints output, and exits.

---

## Project Layout (Quick Reference)

```
flipkart_minutes/
├── pom.xml                          ← Maven build config
├── README.md                        ← Full feature documentation
├── SETUP.md                         ← This file
└── src/
    └── main/
        ├── java/com/flipkart/minutes/
        │   ├── FlipkartMinutesApplication.java   ← Main class
        │   ├── model/               ← Customer, Order, Item, DeliveryPartner
        │   ├── repository/          ← In-memory data stores
        │   ├── strategy/            ← Assignment strategies (4 implementations)
        │   ├── service/             ← Business logic
        │   ├── exception/           ← FlipkartMinutesException
        │   └── demo/
        │       └── DemoRunner.java  ← 9 test scenarios (entry point for demo)
        └── resources/
            └── application.properties
```

---

## One-liner for Arch Linux (as used during development)

```bash
JAVA_HOME="/home/codemaster29/.antigravity/extensions/redhat.java-1.52.0-linux-x64/jre/21.0.9-linux-x86_64" /tmp/apache-maven-3.9.6/bin/mvn spring-boot:run
```
