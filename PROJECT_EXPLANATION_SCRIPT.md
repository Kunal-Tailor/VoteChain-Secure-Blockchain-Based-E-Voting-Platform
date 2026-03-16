# Project Explanation Script (End-to-End)
## Online Voting System using Blockchain Technology (Java + JavaFX + SHA-256 + GetBlock)

Use this file as your **presentation / viva script**. You can read it line-by-line while showing the application.

---

## 1) What is the project?

My project is an **Online Voting System using Blockchain Technology** built in **Core Java** with **JavaFX UI**.  
The key idea is: **each vote is stored as a block**, and blocks are connected using **SHA-256 hashes**.  
This makes votes **immutable** and helps detect **tampering**.

---

## 2) Why did I build it? (Problem + Motivation)

Traditional voting systems have challenges like:
- Duplicate voting attempts
- Lack of transparency
- Possible tampering with stored vote data
- Slow result counting

Blockchain concepts solve this because:
- Each block contains a hash of the previous block (chain-linking)
- If any block is modified, the hashes mismatch and integrity fails
- Results can be computed directly from the chain

---

## 3) What technologies did I use?

- **Java (Core Java)**: OOP, collections, file I/O
- **JavaFX**: Frontend UI screens and navigation
- **SHA-256**: Hashing algorithm for block integrity
- **File Handling (Serialization)**: Persistence without database
- **GetBlock + JSON-RPC**: Demonstrates live connectivity to an external blockchain node using `eth_blockNumber`

---

## 4) Project architecture (Layered)

I used **layered architecture** for clean separation:

- **UI Layer (`ui/`)**
  - JavaFX screens: home, login, dashboard, vote casting, results, admin dashboard
- **Service Layer (`service/`)**
  - Business logic: voter operations, admin operations, blockchain operations
- **Model Layer (`model/`)**
  - Core blockchain classes: `Block`, `Blockchain`
- **Security Layer (`security/`)**
  - SHA-256 hashing: `HashUtil`
- **Storage Layer (`storage/`)**
  - File persistence: `FileStorage`
- **Integration Layer (`integration/`)**
  - GetBlock client for external blockchain connectivity: `GetBlockClient`

This structure makes the code easy to explain and maintain.

---

## 5) Core blockchain concept (How vote becomes a block)

### 5.1 Block structure (`Block.java`)
Each vote block contains:
- **Index**
- **Voter ID**
- **Candidate Name**
- **Timestamp**
- **Previous Hash**
- **Current Hash (SHA-256)**

Hash is calculated using:
\[
Hash = SHA-256(index + voterId + candidateName + timestamp + previousHash)
\]

### 5.2 Genesis block
The blockchain starts with a **Genesis Block** (index 0) which acts as the base of the chain.

### 5.3 One vote per voter
In `Blockchain.java`, I track voters in a map (`votedVoters`).  
If the same voter tries to vote again, the system blocks it.

### 5.4 Chain validation (tamper detection)
The admin can validate blockchain integrity:
- Recalculate each block’s hash and compare to stored hash
- Check that each block’s `previousHash` equals the previous block’s `currentHash`

If a block is modified, validation fails → tampering detected.

---

## 6) Persistence (File handling)

I used file storage so the data stays even after closing the app:

- `blockchain.dat`: stores the blockchain (serialized)
- `voters.dat`: stores default voters and their passwords

On startup:
- If files exist → load data
- If not → create default data

---

## 7) UI screens (what the user sees)

### 7.1 Home Screen
- Two options: **Voter Login** or **Admin Login**

### 7.2 Voter Flow (End-to-end)
1. **Voter Login**
   - Enter Voter ID and password
2. **Voter Dashboard**
   - Options: Cast Vote / View Results / Logout
3. **Vote Casting Screen**
   - Select candidate (radio button)
   - Confirm vote (cannot change later)
4. **Vote Confirmation Screen**
   - Shows success message + selected candidate
5. **Results Screen**
   - Shows totals + percentages (computed by traversing blockchain)

### 7.3 Admin Flow (End-to-end)
1. **Admin Login**
2. **Admin Dashboard**
   - Shows results (charts + timeline)
   - Shows blockchain security status
   - Admin can refresh data

---

## 8) Admin Dashboard (What makes it advanced)

Admin dashboard displays:
- **Blockchain secure/tampered status**
- **Pie chart**: vote distribution
- **Bar chart**: vote comparison
- **Blockchain timeline**: list of blocks (scrollable)

This is useful for demonstrating transparency and integrity.

---

## 9) GetBlock integration (External blockchain connectivity)

To showcase real blockchain connectivity, I integrated **GetBlock**:

- In the Admin Dashboard, I added a button: **“🌐 Test GetBlock Connection”**
- When clicked, the project calls Ethereum JSON-RPC method:
  - `eth_blockNumber`
- The popup displays the **latest on-chain block number**, proving the system is connected to a live blockchain node.

Where it is implemented:
- `integration/GetBlockClient.java`: HTTP JSON-RPC client
- `service/AdminService.java`: exposes methods for UI
- `ui/AdminDashboardScreen.java`: button + popup

---

## 10) How to demo the project (Suggested live demo)

### Demo Part A: Voting + Blockchain
1. Login as voter (V001 / voter1)
2. Cast vote for a candidate
3. Try voting again with same voter → show “already voted”
4. View results (results update)

### Demo Part B: Admin validation
1. Login as admin (admin / admin123)
2. Show charts + timeline
3. Explain block linking + integrity check

### Demo Part C: GetBlock connectivity (wow factor)
1. Click **Test GetBlock Connection**
2. Show the latest block number from GetBlock
3. Explain: “This proves we are connected to an external blockchain node.”

---

## 11) Key points to say in viva (short answers)

- **Why blockchain?** To ensure immutability and tamper detection using hash linking.
- **Why SHA-256?** Secure hashing algorithm; small data change changes the hash completely.
- **How duplicate voting is prevented?** By tracking voted voter IDs and rejecting repeat votes.
- **How results are calculated?** By traversing blocks and counting candidate votes.
- **How tampering is detected?** Hash mismatch or broken previousHash link invalidates chain.
- **What is GetBlock doing?** It provides a real blockchain node endpoint; we call `eth_blockNumber` to prove connectivity.

---

## 12) Conclusion (final statement)

This project is a complete end-to-end implementation of:
- Secure voting using blockchain concepts
- SHA-256 hashing for integrity
- File persistence
- Professional JavaFX UI
- Admin validation and visualization
- Live blockchain connectivity using GetBlock

It is suitable for college submission and can be further enhanced with smart contracts or distributed networks.

