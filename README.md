# VoteChain: Secure Blockchain-Based E-Voting Platform 🗳️

**Maharashtra Edition** — A comprehensive, production-ready implementation of a secure online voting system using blockchain technology. Built with a robust Java REST API, MongoDB Atlas cloud storage, and a premium cinematic Web UI tailored for Maharashtra state elections.

---

## 📋 Project Overview

This project implements a decentralized-inspired, blockchain-backed voting system that ensures:
- **One Vote Per Voter**: Guaranteed through strict cryptographic session management and secure MongoDB indexing to prevent duplicate voting.
- **Immutable Votes (Blockchain)**: Once cast, votes are immediately mined into a local blockchain implementation, locked with SHA-256 and previous-block hash linkages.
- **Tamper Detection & Ledger Validation**: The administrative command center features real-time validation to verify that no block in the historical chain has been altered.
- **Transparent, Real-Time Results**: The system autonomously tallies block records to generate exact candidate counts and map demographics.

---

## 🏗️ System Architecture & File Structure

The project follows a robust **layered full-stack architecture** with a clear separation of concerns, evolving from a standard desktop app to a scalable cloud web platform:

```text
MPJ/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/votingsystem/
│       │       ├── api/               # Spark Java REST API server & Routing mappings
│       │       ├── integration/       # Web3 / RPC integration components
│       │       ├── model/             # Document Entities: Block, Voter, Election, etc.
│       │       ├── security/          # BCrypt and SHA-256 Hashing logic
│       │       ├── service/           # Core business logic & Blockchain rules
│       │       ├── storage/           # MongoDB Atlas DAO connections
│       │       ├── ui/                # Legacy JavaFX classes (Desktop Fallback)
│       │       ├── api/ApiOnlyMain.java # Headless API server entry point
│       │       └── Main.java          # Legacy JavaFX fallback entry point
│       └── resources/
│           └── css/styles.css         # Legacy desktop styles
├── assets/                            # Images and visual web resources
├── lib/                               # Third-party required JAR dependencies
├── out/                               # Compiled Java `.class` files (Desktop build)
├── screenshots/                       # Directory containing UI & architectural diagrams
├── target/                            # Maven output directory (Contains fat JAR limit)
├── .env                               # Environment configurations (MONGO_URI, etc.)
├── app.js                             # Web Application Frontend Logic & Fetch Calls
├── build.sh                           # Desktop compilation script
├── insert_screenshots.py              # Script to auto-generate Final DOCX figures
├── pom.xml                            # Maven Dependencies & Build configuration
├── run.sh                             # Shell wrapper execution script
├── voting_index.html                  # Main Web Application Frontend
├── styles.css                         # Web Application Styles (Custom Dashboard)
├── COMPILATION_GUIDE.md               # Advanced build instructions
├── QUICK_START.md                     # Brief boot execution instructions
├── VoteChain_Report_Final.docx        # Final Output College Project Documentation
└── README.md                          # Main Project Documentation File
```

---

## 🔑 Key Features

### Voter Web Interface
- 🛡️ **Secure Authentication**: BCrypt-hashed password validation.
- 🗺️ **Active Constituencies**: District and division mapping ensures voters only vote in their designated areas.
- ⛓️ **Live Mining Feedback**: The frontend simulates network latency and displays live "mining" success immediately after casting a vote.
- 📱 **Responsive UI**: Glassmorphism and premium cinematic design styled organically using Vanilla CSS.

### Election Commission Command Center
- 📊 **Real-Time Analytics**: Division-wise voter turnout, comprehensive candidate tallies, total blocks mined.
- 🧬 **Blockchain Explorer**: Complete visualization of the internal immutable timeline (from Genesis Block to real-time mined blocks), fetching data directly from MongoDB.
- 🌱 **Database Seeding**: Single-button tool to seed the initial active DB with Maharashtra geographical data and standard candidate profiles.
- 🗃️ **Tabular Management**: Dedicated data tables for Constituencies, Live Elections, and Verified Voters.

### Blockchain & Cryptography
- ⛓️ **Hash Chaining**: Each `Block` object links to the `previousHash`, establishing total historical immutability.
- 🔐 **SHA-256 Integrity Engine**: Votes are packed as JSON equivalents and put through strict SHA-256 encryption.
- 🧩 **Proof of Work (Simulated)**: A basic `nonce` iteration process verifies block additions before database commits.
- 🔑 **BCrypt Integration**: Passwords and keys exist exclusively in encoded forms inside the Mongo database.

---

## 🛠️ Technology Stack

- **Backend**: Core Java 25+, Spark Java (Micro-framework for REST APIs), Maven.
- **Frontend**: HTML5, Vanilla JavaScript, CSS3 (Custom Glassmorphism styling, no frameworks).
- **Database**: MongoDB Atlas (Cloud NoSQL), MongoDB Java Driver.
- **Security**: JBCrypt (Password Hashing), Standard `java.security.MessageDigest` (SHA-256).

---

## 🚀 Quick Start / How to Run

### 1. Prerequisites
- Java JDK 11 or higher (Tested on Java 25)
- Maven 3.6+
- A running MongoDB Atlas Cluster (Free tier is perfectly fine)

### 2. Environment Configuration
Create a `.env` file in the root project directory `MPJ/` based on these variables:
```env
MONGO_URI=mongodb+srv://<username>:<password>@<cluster-url>/?retryWrites=true&w=majority
AUTO_SEED=true
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin
```

### 3. Build & Run the Backend
Open your terminal in the target directory and run:

```bash
# Compile and build the fat JAR dependency using Maven
mvn clean package -DskipTests

# Load ENV properties and start the REST Server (Defaults to Port 4567)
set -a && source .env && set +a
java -jar target/MPJ-1.0.jar
```

✅ *When successful, the terminal will log:* `Voting API Server running on port 4567`

### 4. Launch the Web Interface
Simply open `voting_index.html` in any modern web browser. **Note: the backend must be running for authentication arrays to respond.**

---

## 👨‍💻 Default Login Credentials

### Voter Examples (Auto-Seeded)
- `V001` / `voter1` (Pune)
- `V002` / `voter2` (Sambhaji Nagar)
- `V003` / `voter3` (Nagpur)

### Election Commission Administrator
- Username: `admin` (Or whatever configured in `.env`)
- Password: `admin`

---

## 🔐 Cryptographic Implementation Details

### The Vote Structure
When a vote is cast, a JSON payload creates a Block:
```json
{
  "index": 4,
  "voterId": "V001",
  "candidateName": "Hema Salunkhe (BJP)",
  "timestamp": "2024-04-06T12:00:00",
  "previousHash": "0007f0234a9b...",
  "nonce": 34521,
  "hash": "000a3b194f1c..."
}
```
**Hash Calculation Formula:**  
`Hash = SHA-256(Index + VoterID + CandidateName + Timestamp + PreviousHash + Nonce)`

---

## 🔍 Educational Value & Viva FAQ

**Q: How does the blockchain ensure vote security in this project?**  
**A:** Every vote is mathematically sealed in a block spanning the `previousHash`. If a malicious actor alters a record directly in MongoDB, the current `hashCode` will mismatch its origin calculation, and the subsequent block will immediately flag the entire chain as invalid. The Admin Command Center demonstrates this timeline visual validation in real-time.

**Q: Why use MongoDB instead of traditional SQL?**  
**A:** Since block schemas vary (especially when merging block data payloads vs candidate parameters), a NoSQL approach represents a JSON-to-Document translation seamlessly perfectly mirroring typical modern dApp state logic.

**Q: Is the system decentralized?**  
**A:** For the scope of this project, it represents a *Consortium* or private ledger style, managed explicitly by the Central State backend. While it runs logically as a blockchain (using proof-of-work loops, SHA hashing, and chain linkage), the raw data lives on an encrypted cloud cluster rather than distributed peer nodes. 

---
**Developed for Final Year College Project Submission**  
*Online Voting System utilizing Web-based Interface and Java Backend Architecture*
