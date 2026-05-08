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

## Database Schema

Three tables in MySQL `algbattle`, managed by MyBatis-Plus (no SQL scripts).

### user table
| Field | Type | Key | Description |
|-------|------|-----|-------------|
| id | INT | PK | 自增 |
| username | VARCHAR(100) | | 用户名 |
| password | VARCHAR(255) | | BCrypt加密 |
| photo | VARCHAR(500) | | 头像URL |
| rating | INT | | 天梯积分，默认1500 |

### bot table
| Field | Type | Key | Description |
|-------|------|-----|-------------|
| id | INT | PK | 自增 |
| user_id | INT | FK→user | 所属用户 |
| title | VARCHAR(100) | | Bot标题 |
| description | VARCHAR(100) | | Bot描述 |
| content | TEXT | | Java代码，最长10000字符 |
| createtime | DATETIME | | 创建时间 |
| modifytime | DATETIME | | 修改时间 |

### record table
| Field | Type | Key | Description |
|-------|------|-----|-------------|
| id | INT | PK | 自增 |
| a_id | INT | FK→user | 玩家A用户ID |
| a_sx, a_sy | INT | | A起始坐标 (11,1) |
| b_id | INT | FK→user | 玩家B用户ID |
| b_sx, b_sy | INT | | B起始坐标 (1,12) |
| a_steps | VARCHAR(2000) | | A走法序列，方向值拼接 |
| b_steps | VARCHAR(2000) | | B走法序列 |
| map | TEXT | | 13×14网格，0=空地 1=墙 |
| loser | VARCHAR(10) | | "A"/"B"/"all" |
| createtime | DATETIME | | 对局时间 |

## Core Algorithms

### 1. Dynamic Matching Algorithm
- Formula: `|ratingA - ratingB| ≤ min(waitTimeA, waitTimeB) × 10`
- O(n²) per scan, 1s interval, async thread
- Threshold grows linearly with wait time — balances fairness vs speed

### 2. Map Generation + DFS Connectivity
- 13×14 grid, 20 inner walls with **center-symmetry**: if (r,c) is wall, (12-r, 13-c) must also be wall
- DFS from Player A start (11,1) to verify path to Player B start (1,12)
- O(R×C) time, O(R×C) space
- Regenerate on failure (~11% obstacle density, almost always succeeds first try)

### 3. Collision Detection (per tick)
- 5-step check: boundary → obstacles → own body → opponent body → safe
- O(W + L_A + L_B) ≈ 260 comparisons worst case, << 200ms tick budget
- Snake growth: `Grow(step) = true if step ≤ 10 or step % 3 == 0`

### 4. Game Loop State Machine
- Per tick: get input → compute next position → collision detect → broadcast
- Tick interval: 200ms wait + polling
- Bot execution: 2000ms timeout; bot input string = encoded map + both snakes' positions and paths
- Max 1000 ticks per game

## Key File Reference

### Backend — Game Engine & WebSocket
- `backendcloud/backend/.../consumer/WebSocketServer.java` — WebSocket endpoint, game lifecycle, message routing
- `backendcloud/backend/.../consumer/utils/Game.java` — Game thread: map gen, tick loop, collision, rating update
- `backendcloud/backend/.../consumer/utils/Player.java` — Player state: position, steps, bot info

### Backend — Matching System
- `backendcloud/matchingsystem/.../impl/utils/MatchingPool.java` — Thread-safe matching pool, O(n²) scan
- `backendcloud/matchingsystem/.../impl/utils/Player.java` — Match pool player model

### Backend — Bot Sandbox
- `backendcloud/botrunningsystem/.../impl/utils/BotPool.java` — Task queue with ReentrantLock + Condition
- `backendcloud/botrunningsystem/.../impl/utils/Consumer.java` — jOOR dynamic compile + execute + 2000ms timeout
- `backendcloud/botrunningsystem/.../utils/BotInterface.java` — `Integer nextMove(String input)` interface
- `backendcloud/botrunningsystem/.../utils/Bot.java` — Default greedy bot implementation

### Backend — Security & Config
- `backendcloud/backend/.../config/SecurityConfig.java` — JWT filter, localhost-only inter-service endpoints
- `backendcloud/backend/.../config/filter/JwtAuthenticationTokenFilter.java` — Token validation
- `backendcloud/backend/.../utils/JwtUtil.java` — JWT create/parse

### Frontend — Game Rendering
- `web/src/assets/scripts/GameMap.js` — Canvas game engine, snakes[], walls, requestAnimationFrame loop
- `web/src/assets/scripts/Snake.js` — Snake entity: render, move interpolation, eye direction, growth
- `web/src/assets/scripts/GameObject.js` — Base class with timedelta-based update loop
- `web/src/assets/scripts/Wall.js` — Wall rendering
- `web/src/assets/scripts/SnakeBody.js` — Single body segment model

### Frontend — Vue Components & Store
- `web/src/views/pk/PKIndexView.vue` — Main PK page: WebSocket setup, event routing
- `web/src/store/pk.js` — PK state: status, socket, gameObject, role, loser, opponent info
- `web/src/store/user.js` — Auth state: id, token, is_login
- `web/src/store/record.js` — Replay state: is_record, a_steps, b_steps
- `web/src/components/PlayGround.vue` — Game wrapper with role indicator ("你控制蓝色蛇 左下角")
- `web/src/components/MatchGround.vue` — Match button + bot selector
- `web/src/components/GameMap.vue` — Canvas mount, GameMap initialization
- `web/src/components/ResultBoard.vue` — Win/loss overlay

## Project Context

This is a **graduation thesis project** (2025, 郑州轻工业大学). The thesis itself lives in `thesis/` directory:
- `thesis/宁艳焱毕业设计(论文).docx` — the main thesis document (currently being revised)
- `thesis/数据库表设计图.html` — database table design diagrams for the thesis
- `thesis_rewrite/图表.html` — 12 technical diagrams (architecture, ER, flowcharts, use cases)

## Development Notes

- **Everything runs locally**: IDEA launches 3 Spring Boot microservices, Vue CLI (`npm run serve`) serves frontend, MySQL on localhost:3306. No remote servers involved.
- **User's primary language is C++**; Java chosen for this project due to better web ecosystem (Spring Boot, MyBatis-Plus).
- **Bot code runs in same JVM** — thread-level isolation only, no Docker/process sandbox (noted as limitation in thesis).
- **Role indicator feature**: Players see their snake color and position (e.g. "你控制蓝色蛇 (左下角)") above the game map on match start. The `role` field ("A"/"B") is sent from WebSocketServer and stored in Vuex `pk.role`.
- **Replay uses stored step strings**: `a_steps` and `b_steps` are concatenated direction digits, parsed at 300ms intervals for auto-playback.
- **Pagination**: MyBatis-Plus pagination plugin configured; record list and ranklist both paginated (ranklist: 3 per page).
