# VoteChain (MPJ)

VoteChain is a blockchain-inspired e-voting demo built for a final-year college project. It includes:

- A Web UI (`voting_index.html` + `app.js`) that talks to a Java/Spark REST API backed by MongoDB.
- A JavaFX desktop app that stores votes locally in `blockchain.dat` and voters in `voters.dat`.

This is an educational prototype, not a production election system.

## Installation

You can run this project in two ways:

- Web UI + API server (recommended): Java 17 + Maven + MongoDB connection string.
- JavaFX desktop app: Java 17 + JavaFX available on your machine.

### Prerequisites

- Java 17+ (project is configured for Java 17 in `pom.xml`)
- Maven (for building the API fat JAR)

API/Web mode also needs:

- MongoDB connection string in `MONGO_URI` (Atlas is fine)

Desktop mode also needs:

- JavaFX (if JavaFX modules are missing, follow `COMPILATION_GUIDE.md`)

### Setup

1) Create `.env` from `.env.example` (used by `run.sh` and the API server):

```bash
cp .env.example .env
```

2) Put your MongoDB connection string into `.env` as `MONGO_URI`.

Notes:

- `AUTO_SEED=true` will seed demo Maharashtra elections/constituencies if missing.
- Admin credentials for API/Web mode come from `ADMIN_USERNAME` / `ADMIN_PASSWORD`.

## What This Project Demonstrates

- One-vote-per-voter enforcement (both modes)
- Tamper detection via hash chaining (SHA-256) and proof-of-work style mining (difficulty `3`)
- Password hashing with BCrypt in the MongoDB-backed API mode
- RSA digital signatures on vote blocks in the desktop blockchain model
- An admin dashboard in the Web UI for results and chain validation

## Tech Stack

- Java 17, Maven
- Spark Java (REST API)
- MongoDB (Atlas or any MongoDB connection string)
- JavaFX (desktop UI)
- jBCrypt (password hashing)

## Repo Layout (High Level)

```text
MPJ/
├── src/main/java/com/votingsystem/
│   ├── api/                 # Spark REST API + MongoDB services
│   ├── model/               # Block + Blockchain (desktop mode)
│   ├── security/            # SHA-256 + RSA signature helpers
│   ├── service/             # Desktop service layer
│   ├── storage/             # Desktop file persistence (blockchain.dat, voters.dat)
│   └── ui/                  # JavaFX screens
├── voting_index.html        # Web UI entry page
├── app.js                   # Web UI logic (calls the REST API)
├── styles.css               # Web UI styles
├── pom.xml                  # Maven build (fat JAR runs API-only main)
├── Dockerfile               # Container build for API server
├── build.sh                 # Compile desktop app to ./out
├── run.sh                   # Run desktop app
└── screenshots/             # Diagrams and UI screenshots
```

## Quick Start (Web UI + API Server)

Prerequisites:

- Java 17+
- Maven
- A MongoDB connection string (`MONGO_URI`)

1) Build the API fat JAR:

```bash
mvn clean package -DskipTests
```

2) Run the API server (port `4567`):

```bash
set -a && source .env && set +a
java -jar target/MPJ-1.0.jar
```

3) Open the Web UI:

- Open `voting_index.html` in your browser.
- If your API is not on `http://localhost:4567`, open with `?api=...`, for example:
  - `voting_index.html?api=http://localhost:4567`

### Default Credentials (API/Web Mode)

- Voters (auto-created in MongoDB): `V001`/`voter1`, `V002`/`voter2`, `V003`/`voter3`, `V004`/`voter4`, `V005`/`voter5`
- Admin: `ADMIN_USERNAME` / `ADMIN_PASSWORD` from `.env` (defaults to `admin` / `admin123`)

## Quick Start (JavaFX Desktop App)

Prerequisites:

- Java 17+
- JavaFX available on your machine (see `COMPILATION_GUIDE.md` if JavaFX modules are missing)

Build and run:

```bash
./build.sh
./run.sh
```

### Default Credentials (Desktop Mode)

- Voters (stored in `voters.dat`): `V001`/`voter1` ... `V005`/`voter5`
- Admin (hardcoded in code): `admin` / `admin123`

### Desktop Persistence Files

- `blockchain.dat`: serialized blockchain (votes)
- `voters.dat`: serialized voter credentials

To reset the desktop demo, delete those two files and re-run.

## Environment Variables (API Mode)

These are read by `src/main/java/com/votingsystem/api/VotingApiServer.java`:

- `MONGO_URI` (required): MongoDB connection string
- `AUTO_SEED` (optional, default `false`): set `true` to seed Maharashtra demo elections/constituencies if missing
- `ADMIN_USERNAME` (optional, default `admin`)
- `ADMIN_PASSWORD` (optional, default `admin123`)

## API Notes

- Default base URL: `http://localhost:4567`
- Main routes used by the Web UI include:
  - `POST /api/voter/login`
  - `POST /api/voter/cast-vote` and `POST /api/voter/cast-vote-election`
  - `GET /api/results` (and election/constituency breakdown endpoints)
  - `POST /api/admin/login`
  - `GET /api/admin/blockchain`, `GET /api/admin/voters`, `POST /api/admin/seed-maharashtra`

For the full list, see `src/main/java/com/votingsystem/api/VotingApiServer.java`.

## Docker (API Server)

Build:

```bash
docker build -t votechain-api .
```

Run (provide `MONGO_URI`):

```bash
docker run --rm -p 4567:4567 -e MONGO_URI="..." -e AUTO_SEED=true votechain-api
```

## Screenshots

Some screenshots and diagrams are in `screenshots/`, for example `screenshots/fig01_architecture.png`.

## Troubleshooting

- API fails immediately with "Missing Mongo configuration": set `MONGO_URI` (see `.env.example`)
- Web UI shows "Server offline": start the API server and ensure it is reachable at `http://localhost:4567` (or pass `?api=...`)
- JavaFX errors ("module not found: javafx.controls"): follow `COMPILATION_GUIDE.md` for JavaFX setup

## More Docs

- `QUICK_START.md` (fast run steps)
- `COMPILATION_GUIDE.md` (desktop build + JavaFX troubleshooting)
- `PROJECT_SUMMARY.md` (component overview)
