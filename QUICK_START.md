# Quick Start Guide

## 🚀 Running the Application (Fastest Method)

### Option 1: Using Scripts (macOS/Linux)
```bash
./build.sh    # Compile
./run.sh      # Run
```

### Option 2: Using IDE
1. Open project in IntelliJ IDEA / Eclipse / VS Code
2. Run `Main.java`

## 👤 Login Credentials

### Voters
- **V001** / **voter1**
- **V002** / **voter2**
- **V003** / **voter3**
- **V004** / **voter4**
- **V005** / **voter5**

### Admin
- **admin** / **admin123**

## 📋 Quick Test Flow

1. **Start Application** → Home Screen appears
2. **Voter Login** → Use V001/voter1
3. **Cast Vote** → Select a candidate
4. **Confirm Vote** → Vote is recorded
5. **View Results** → See election results
6. **Admin Login** → Use admin/admin123
7. **Validate Blockchain** → Check integrity
8. **View Results** → See all votes

## 🎯 Key Features to Demonstrate

✅ **One Vote Per Voter** - Try voting twice with same ID
✅ **Blockchain Integrity** - Admin validates chain
✅ **Tamper Detection** - Modify blockchain.dat to test
✅ **Results Calculation** - Cast multiple votes and see updates
✅ **File Persistence** - Close and reopen app, data persists

## 📁 Important Files

- `Main.java` - Entry point
- `Blockchain.java` - Core blockchain logic
- `Block.java` - Individual block structure
- `HashUtil.java` - SHA-256 hashing
- `blockchain.dat` - Persistent blockchain data
- `voters.dat` - Voter credentials

## 🔧 Troubleshooting

**Can't find JavaFX?**
- Use JDK 11+ (includes JavaFX)
- Or download from https://openjfx.io/

**CSS not loading?**
- Ensure `src/main/resources` is in classpath
- Check `styles.css` exists at `src/main/resources/css/styles.css`

**Compilation errors?**
- Use IDE instead of command line
- Ensure Java 11+ is installed

## 📚 Documentation

- **README.md** - Full project documentation
- **COMPILATION_GUIDE.md** - Detailed compilation instructions
- **PROJECT_SUMMARY.md** - Project overview and statistics

---

**Ready to go!** 🎉
