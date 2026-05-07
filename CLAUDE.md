# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

### Backend (Java 8, Spring Boot 2.6.13, Maven multi-module)

```bash
# Build all modules
cd backendcloud && mvn clean package -DskipTests

# Run each module separately (Spring Boot, order doesn't matter):
# Main server (port 3000) — handles WebSocket, game logic, REST APIs
cd backendcloud/backend && mvn spring-boot:run

# Matching system (port 3001) — matchmaking pool
cd backendcloud/matchingsystem && mvn spring-boot:run

# Bot running system (port 3002) — code sandbox, compiles & executes user bot code
cd backendcloud/botrunningsystem && mvn spring-boot:run
```

### Frontend (Vue 3, Vue CLI)

```bash
cd web
npm install
npm run serve      # Dev server on default Vue CLI port (usually 8080)
npm run build      # Production build
npm run lint       # Lint
```

### Database

MySQL database `algbattle` on `localhost:3306`, user `root`. Tables (`user`, `bot`, `record`) are auto-managed by MyBatis-Plus — no migration scripts. Entity classes are in `backendcloud/backend/src/main/java/org/kob/backend/pojo/`.

## Architecture

```
web (Vue 3)
  │  WebSocket + AJAX
  ▼
backend:3000 ──── REST ────► matchingsystem:3001
  │                              │
  │  REST                        │  REST callback
  ▼                              ▼
botrunningsystem:3002 ◄──────────┘
```

### Port Assignments

| Port | Service | Role |
|------|---------|------|
| 3000 | `backend` | Main server: WebSocket endpoint, game engine, REST APIs, JWT auth |
| 3001 | `matchingsystem` | Matching pool (background thread, 1s polling) |
| 3002 | `botrunningsystem` | Bot code sandbox (dynamic Java compilation via joor) |

### Request Flow: Matching → Game Start → Game Loop

1. Client connects WebSocket to `ws://127.0.0.1:3000/websocket/{token}`, authenticated via JWT
2. Client sends `{"event":"start-matching","bot_id":N}` → backend POSTs to `matchingsystem:3001/player/add/`
3. `MatchingPool` (background thread) matches two players when `|ratingA - ratingB| ≤ min(waitTimeA, waitTimeB) × 10`
4. On match, `matchingsystem` POSTs to `backend:3000/pk/start/game/` with both player IDs
5. Backend creates a `Game` thread and sends `{"event":"start-matching", "role":"A"|"B", "game":{...}}` to each player via WebSocket
6. Game loop: each tick waits for both players' next direction (or calls botrunningsystem for bot players)
7. Backend broadcasts `{"event":"move","a_direction":D,"b_direction":D}` to both clients each tick
8. On game end: backend sends `{"event":"result","loser":"A"|"B"|"all"}` and saves record to DB

### WebSocket Message Protocol

| Direction | Event | Fields | When |
|-----------|-------|--------|------|
| Client → Server | `start-matching` | `bot_id` | Start matchmaking |
| Client → Server | `stop-matching` | — | Cancel matchmaking |
| Client → Server | `move` | `direction` (0=up,1=right,2=down,3=left) | Player keypress (WASD) |
| Server → Client | `start-matching` | `role`, `opponent_username`, `opponent_photo`, `game` | Game starts |
| Server → Client | `move` | `a_direction`, `b_direction` | Each game tick |
| Server → Client | `result` | `loser` | Game ends |

### Game Engine Details

- Map: **13 rows × 14 cols**, walled borders, 20 random inner walls (center-symmetric)
- Player A starts at `(11, 1)` (bottom-left), Player B starts at `(1, 12)` (top-right)
- Snake: first 10 steps grow every tick, then grow every 3rd tick
- Collision: head hits wall, own body, or opponent's body → death
- Rating: winner +5, loser -2
- Bot code must implement `Integer nextMove(String input)` — receives encoded game state, returns direction 0-3

### Security Model

- JWT-based stateless auth (jjwt, `JwtAuthenticationTokenFilter` before `UsernamePasswordAuthenticationFilter`)
- Inter-service endpoints (`/pk/start/game/`, `/pk/receive/bot/move/`) restricted to `127.0.0.1` only
- WebSocket path `/websocket/**` excluded from Spring Security filter chain
- Bot execution timeout: 2000ms (`thread.join(2000)` + interrupt)

### Key Inter-Service Endpoints (localhost only)

- `POST matchingsystem:3001/player/add/` — add player to match pool (params: `user_id`, `rating`, `bot_id`)
- `POST matchingsystem:3001/player/remove/` — remove player from match pool
- `POST backend:3000/pk/start/game/` — start a game (params: `a_id`, `a_bot_id`, `b_id`, `b_bot_id`)
- `POST backend:3000/pk/receive/bot/move/` — receive bot's computed move (params: `user_id`, `direction`)
- `POST botrunningsystem:3002/bot/add/` — submit bot code for execution (params: `user_id`, `bot_code`, `input`)

## Frontend Structure

- **Router**: history mode, `beforeEach` guard checks `store.state.user.is_login`
- **Store modules**: `user` (auth state), `pk` (game state + WebSocket), `record` (replay state)
- **Game rendering**: Canvas-based (`GameMap.js` extends `GameObject` with `requestAnimationFrame` loop). `snakes[0]` = Player A (blue), `snakes[1]` = Player B (red)
- **Replay**: `record.js` stores step arrays; `GameMap.js` auto-plays them via `setInterval` (300ms) when `is_record` is true
- **Bot editor**: Ace Editor in Java mode (`vue3-ace-editor`), content max 10000 chars
- **API calls**: Direct to `http://127.0.0.1:3000` (no proxy configured in `vue.config.js`)
