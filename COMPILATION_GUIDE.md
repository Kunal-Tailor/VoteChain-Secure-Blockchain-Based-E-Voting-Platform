# Compilation and Running Guide

## Quick Start

### Method 1: Using Build Scripts (Recommended)

1. **Build the project:**
   ```bash
   ./build.sh
   ```

2. **Run the application:**
   ```bash
   ./run.sh
   ```

### Method 2: Manual Compilation

#### Step 1: Compile Java Files

**For systems with JavaFX in JDK (JDK 11+):**
```bash
mkdir -p out
javac -d out --add-modules javafx.controls,javafx.fxml \
    -cp src/main/resources \
    src/main/java/com/votingsystem/**/*.java
```

**For systems with separate JavaFX installation:**
```bash
mkdir -p out
javac -d out \
    --module-path /path/to/javafx/lib \
    --add-modules javafx.controls,javafx.fxml \
    -cp src/main/resources \
    src/main/java/com/votingsystem/**/*.java
```

#### Step 2: Copy Resources
```bash
cp -r src/main/resources/* out/
```

#### Step 3: Run the Application

**With JavaFX in JDK:**
```bash
java --add-modules javafx.controls,javafx.fxml \
    -cp out:src/main/resources \
    com.votingsystem.Main
```

**With separate JavaFX:**
```bash
java --module-path /path/to/javafx/lib \
    --add-modules javafx.controls,javafx.fxml \
    -cp out:src/main/resources \
    com.votingsystem.Main
```

### Method 3: Using IDE

#### IntelliJ IDEA

1. **Open Project:**
   - File → Open → Select project folder

2. **Configure JavaFX:**
   - File → Project Structure → Libraries
   - Add JavaFX SDK if not already included
   - Or use JavaFX plugin

3. **Set Run Configuration:**
   - Run → Edit Configurations
   - Main class: `com.votingsystem.Main`
   - VM options: `--module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml`
   - Working directory: Project root

4. **Run:**
   - Right-click `Main.java` → Run 'Main.main()'

#### Eclipse

1. **Import Project:**
   - File → Import → Existing Projects into Workspace

2. **Configure JavaFX:**
   - Install e(fx)clipse plugin
   - Or add JavaFX library to build path

3. **Run:**
   - Right-click `Main.java` → Run As → Java Application

#### VS Code

1. **Install Extensions:**
   - Java Extension Pack
   - JavaFX Extension (if available)

2. **Open Project:**
   - File → Open Folder → Select project folder

3. **Run:**
   - Open `Main.java`
   - Click Run button or press F5

## Troubleshooting

### Issue: "JavaFX runtime components are missing"

**Solution:**
- Download JavaFX SDK from https://openjfx.io/
- Extract and set the path in build/run scripts
- Or use JDK that includes JavaFX (like Amazon Corretto 11+)

### Issue: "Cannot find resource /css/styles.css"

**Solution:**
- Ensure `src/main/resources/css/styles.css` exists
- Include `src/main/resources` in classpath when running
- Copy resources to output directory after compilation

### Issue: "Module not found: javafx.controls"

**Solution:**
- Add `--add-modules javafx.controls,javafx.fxml` to javac and java commands
- Ensure JavaFX is properly installed

### Issue: Compilation errors with wildcards

**Solution:**
- Use IDE compilation instead
- Or compile files individually or by package

## File Structure After Compilation

```
MPJ/
├── out/                    # Compiled classes
│   └── com/
│       └── votingsystem/
│           └── ...
├── src/
│   └── main/
│       ├── java/
│       └── resources/
│           └── css/
│               └── styles.css
├── blockchain.dat          # Created after first run
├── voters.dat              # Created after first run
├── build.sh
├── run.sh
└── README.md
```

## Testing the Application

1. **Start the application**
2. **Test Voter Login:**
   - Click "Voter Login"
   - Use: V001 / voter1
   - Cast a vote
   - Try logging in again (should show already voted)

3. **Test Admin Login:**
   - Click "Admin Login"
   - Use: admin / admin123
   - View results
   - Validate blockchain
   - Detect tampering

4. **Test Blockchain Integrity:**
   - Cast multiple votes
   - View results in admin panel
   - Validate blockchain (should be valid)
   - Manually modify blockchain.dat (if you want to test tampering detection)

## Notes

- Data files (`blockchain.dat`, `voters.dat`) are created in the project root
- To reset the system, delete these files
- Default voters are created automatically on first run
- Admin credentials are hardcoded (can be changed in `AdminService.java`)
