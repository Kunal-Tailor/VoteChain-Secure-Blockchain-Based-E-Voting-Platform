# Online Voting System using Blockchain Technology

A complete, college-level end-to-end project implementing a secure online voting system using blockchain concepts in Java with JavaFX UI.

## 📋 Project Overview

This project implements a blockchain-based voting system that ensures:
- **One vote per voter** - Prevents duplicate voting
- **Immutable votes** - Once cast, votes cannot be modified
- **Tamper detection** - Any modification to the blockchain is detected
- **Transparent results** - Results calculated by traversing the blockchain

## 🏗️ Architecture

The project follows a **layered architecture** with clear separation of concerns:

```
src/main/java/com/votingsystem/
├── ui/           → JavaFX frontend screens
├── service/      → Business logic layer
├── model/        → Blockchain core (Block, Blockchain)
├── security/     → SHA-256 hashing utilities
├── storage/      → File persistence layer
├── integration/  → External blockchain connectivity (GetBlock JSON-RPC)
└── Main.java     → Application entry point
```

## 🔑 Key Features

### Voter Module
- ✅ Voter login using Voter ID and password
- ✅ One vote per voter (no duplicate voting)
- ✅ Select candidate and cast vote
- ✅ Vote once → cannot modify
- ✅ View election results

### Admin Module
- ✅ Admin login
- ✅ View live election results
- ✅ Validate blockchain integrity
- ✅ Detect tampering attempts
- ✅ Test live connection to external blockchain node (GetBlock)

### Blockchain & Security
- ✅ Each vote stored as a block
- ✅ SHA-256 hashing for block integrity
- ✅ Genesis block implementation
- ✅ Chain validation (previous hash linking)
- ✅ Tamper detection mechanism
- ✅ External node connectivity check using Ethereum JSON-RPC (`eth_blockNumber`) via GetBlock

## 🛠️ Technology Stack

- **Java** (Core Java)
- **JavaFX** (UI Framework)
- **SHA-256** (Cryptographic Hashing)
- **File I/O** (Persistence)
- **HTTP + JSON-RPC** (External node integration via GetBlock)

## 📦 Project Structure

```
MPJ/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── votingsystem/
│   │   │           ├── ui/              # JavaFX UI Screens
│   │   │           │   ├── HomeScreen.java
│   │   │           │   ├── VoterLoginScreen.java
│   │   │           │   ├── VoterDashboardScreen.java
│   │   │           │   ├── VoteCastingScreen.java
│   │   │           │   ├── VoteConfirmationScreen.java
│   │   │           │   ├── VoterResultsScreen.java
│   │   │           │   ├── AdminLoginScreen.java
│   │   │           │   └── AdminDashboardScreen.java
│   │   │           ├── service/         # Business Logic
│   │   │           │   ├── BlockchainService.java
│   │   │           │   ├── VoterService.java
│   │   │           │   └── AdminService.java
│   │   │           ├── model/           # Blockchain Core
│   │   │           │   ├── Block.java
│   │   │           │   └── Blockchain.java
│   │   │           ├── security/        # Hashing
│   │   │           │   └── HashUtil.java
│   │   │           ├── storage/         # File Handling
│   │   │           │   └── FileStorage.java
│   │   │           ├── integration/     # External Blockchain Integration
│   │   │           │   └── GetBlockClient.java
│   │   │           └── Main.java
│   │   └── resources/
│   │       └── css/
│   │           └── styles.css
└── README.md
```

## 🚀 How to Run

### Prerequisites
- Java JDK 11 or higher
- JavaFX SDK (JDK 11+ does NOT always include JavaFX; on many systems you must install JavaFX separately)

### Running the Application

1. **Compile the project (with JavaFX SDK):**
   ```bash
   cd /Users/kunaltailor/Desktop/Kunal/MPJ
   javac -d out \
     --module-path /path/to/javafx/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp src/main/resources \
     src/main/java/com/votingsystem/**/*.java
   ```

2. **Run the application (with JavaFX SDK):**
   ```bash
   java \
     --module-path /path/to/javafx/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp out:src/main/resources \
     com.votingsystem.Main
   ```

   **For macOS/Linux (if JavaFX is in JDK):**
   ```bash
   java -cp out com.votingsystem.Main
   ```

   **For Windows:**
   ```bash
   java -cp out com.votingsystem.Main
   ```

### Using an IDE (Recommended)

1. **IntelliJ IDEA / Eclipse:**
   - Import the project
   - Ensure JavaFX is configured
   - Run `Main.java`

2. **VS Code:**
   - Install Java Extension Pack
   - Install JavaFX Extension
   - Run `Main.java`

## 👤 Default Credentials

### Voters
- **Voter ID:** V001, **Password:** voter1
- **Voter ID:** V002, **Password:** voter2
- **Voter ID:** V003, **Password:** voter3
- **Voter ID:** V004, **Password:** voter4
- **Voter ID:** V005, **Password:** voter5

### Admin
- **Username:** admin
- **Password:** admin123

## 🔐 Blockchain Implementation Details

### Block Structure
Each block contains:
- **Index:** Position in the chain
- **Voter ID:** Who cast the vote
- **Candidate Name:** Selected candidate
- **Timestamp:** When the vote was cast
- **Previous Hash:** Hash of the previous block
- **Current Hash:** SHA-256 hash of this block

### Hash Calculation
```
Hash = SHA-256(Index + VoterID + CandidateName + Timestamp + PreviousHash)
```

### Chain Validation
The system validates:
1. Each block's hash matches its calculated hash
2. Each block's previousHash matches the previous block's currentHash
3. Genesis block is present and valid

## 📊 Features Explained

### 1. One Vote Per Voter
- System tracks all voted Voter IDs
- Attempting to vote twice throws an exception
- Voters who already voted are redirected to results

### 2. Immutable Votes
- Once a vote is added to the blockchain, it cannot be modified
- Any modification breaks the chain integrity
- Admin can detect tampering through validation

### 3. Results Calculation
- Results are calculated by traversing the blockchain
- Each vote block is counted
- Percentages are calculated dynamically

### 4. File Persistence
- Blockchain is saved to `blockchain.dat`
- Voter credentials saved to `voters.dat`
- Data persists between application runs

## 🎨 UI Screens

1. **Home Screen** - Choose Voter or Admin login
2. **Voter Login** - Authenticate with Voter ID and password
3. **Voter Dashboard** - Cast vote or view results
4. **Vote Casting** - Select candidate using radio buttons
5. **Vote Confirmation** - Confirm successful vote
6. **Results Display** - View election results
7. **Admin Login** - Authenticate admin
8. **Admin Dashboard** - View results (charts), blockchain timeline, validate chain, detect tampering, test GetBlock connection

## 🌐 GetBlock Integration (External Blockchain Connectivity)

This project includes a lightweight integration with **GetBlock** to demonstrate live blockchain connectivity.

### What it does
- The Admin Dashboard includes a button: **“🌐 Test GetBlock Connection”**
- When clicked, the system calls Ethereum JSON-RPC method `eth_blockNumber` via your configured GetBlock endpoint.
- It displays the latest on-chain block number in a popup.

### Where it is implemented
- **Client**: `src/main/java/com/votingsystem/integration/GetBlockClient.java`
- **Service**: `src/main/java/com/votingsystem/service/AdminService.java`
- **UI Button**: `src/main/java/com/votingsystem/ui/AdminDashboardScreen.java`

### How to test (demo steps)
1. Run the application
2. Login as Admin (`admin` / `admin123`)
3. Click **“🌐 Test GetBlock Connection”**
4. You should see a popup showing the latest block number from the external blockchain node

### Note
- The endpoint must be an **Ethereum-compatible HTTP JSON-RPC** endpoint. The code calls `eth_blockNumber`.
- Treat your GetBlock URL as sensitive (don’t publish it publicly).

## 🔍 Viva Questions & Answers

### Q: How does blockchain ensure vote security?
**A:** Each block contains a hash of the previous block. If any block is modified, its hash changes, breaking the chain. The system validates the entire chain to detect tampering.

### Q: How is SHA-256 used?
**A:** SHA-256 calculates a unique hash for each block based on its data. This hash is used to link blocks and detect modifications.

### Q: How do you prevent duplicate voting?
**A:** The system maintains a map of voted Voter IDs. Before adding a vote, it checks if the Voter ID already exists in the map.

### Q: What is the Genesis Block?
**A:** The first block in the chain (index 0) with no previous block. It serves as the starting point of the blockchain.

### Q: How are results calculated?
**A:** The system traverses all blocks (excluding genesis), counts votes for each candidate, and calculates percentages.

## 📝 Notes

- This is a **simulated blockchain** (not cryptocurrency-based)
- Data is stored locally in files
- Suitable for educational/demonstration purposes
- Can be extended with network features for distributed voting

## 🎓 Educational Value

This project demonstrates:
- Object-Oriented Programming (OOP)
- Layered Architecture
- Data Structures (Lists, Maps)
- File I/O Operations
- Cryptographic Hashing
- GUI Development (JavaFX)
- Software Design Patterns

## 📄 License

This project is created for educational purposes.

---

**Developed for College Project Submission**
**Blockchain-Based Online Voting System**
