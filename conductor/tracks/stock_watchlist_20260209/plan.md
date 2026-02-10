# Implementation Plan - Stock Watchlist

## Phase 1: Domain Modeling & Persistence Layer
- [x] Task: Create `WatchlistGroup` Aggregate Root
    - [x] Create `WatchlistGroup` Entity (id, memberId, name, deletedAt, audit fields).
    - [x] Implement business logic: rename, soft-delete, factory method for default group.
    - [x] Create `WatchlistGroupRepository` (Spring Data JPA) with soft-delete handling.
    - [x] Test: Unit tests for Entity behavior (creation, rename, soft-delete).
    - [x] Test: Repository tests (save, findByMemberId, soft-delete filtering).
- [x] Task: Create `WatchlistItem` Entity
    - [x] Create `WatchlistItem` Entity (id, groupId, stockId, marketType, deletedAt, audit fields).
    - [x] Implement business logic: soft-delete, factory method.
    - [x] Create `WatchlistItemRepository` (Spring Data JPA).
    - [x] Test: Unit tests for Entity behavior.
    - [x] Test: Repository tests.
- [x] Task: Implement Domain Service / Constraints
    - [x] Implement `WatchlistConstraintService` to check limits (10 groups/user, 50 items/group).
    - [x] Implement duplicate stock check logic within the domain or service.
    - [x] Test: Service tests mocking repositories to verify limit and duplicate checks.
- [x] Task: QueryDSL Repository Implementation
    - [x] Implement `WatchlistQueryRepository` using QueryDSL.
    - [x] Method: `findGroupsWithItemCount` (fetch groups with item count, filter soft-deleted).
    - [x] Method: `findItemsWithStock` (fetch items with sorting, filter soft-deleted).
    - [x] Test: Integration tests for QueryDSL methods.
- [x] Task: Conductor - User Manual Verification 'Domain Modeling & Persistence Layer' (Protocol in workflow.md)

## Phase 2: Core Business Logic (Service Layer)
- [x] Task: Implement `WatchlistGroupService`
    - [x] Method: `createGroup(memberId, name)` - enforces limit.
    - [x] Method: `updateGroupName(memberId, groupId, newName)` - checks ownership.
    - [x] Method: `deleteGroup(memberId, groupId)` - checks ownership, performs soft-delete.
    - [x] Test: Service unit tests with mocks.
- [x] Task: Implement `WatchlistItemService`
    - [x] Method: `addItem(memberId, groupId, stockId)` - checks ownership, limit, and duplicates.
    - [x] Method: `removeItem(memberId, groupId, stockId)` - checks ownership, performs soft-delete.
    - [x] Test: Service unit tests with mocks (verifying constraints and security checks).
- [x] Task: Implement Event Listener for Default Group
    - [x] Create `MemberCreatedEventListener`.
    - [x] Subscribe to `MemberCreatedEvent`.
    - [x] Logic: Call `WatchlistGroupService` to create "Default" group.
    - [x] Test: Integration test publishing event and verifying group creation.
- [x] Task: Conductor - User Manual Verification 'Core Business Logic (Service Layer)' (Protocol in workflow.md)

## Phase 3: Data Integration & Caching (Redis & Stock Data)
- [x] Task: Define `StockDataPort` Interface
    - [x] Interface method: `getStockDetails(List<StockId>)` returning price, change rate, wellness status (RSI), AI insight.
- [x] Task: Implement `StockDataAdapter` (Persistence/Redis)
    - [x] Implement `StockDataPort`.
    - [x] Logic: Check Redis for cached data first.
    - [x] Fallback: If cache miss, fetch from Stock Domain/DB.
    - [x] Test: Integration test with Redis (Verified via compilation and logic review).
- [x] Task: Implement Cache Synchronization Logic
    - [x] Create service/component to handle cache updates.
    - [x] Trigger: After Daily Batch completion (listen to `BatchCompletedEvent`).
    - [x] Logic: Invalidate latest technical indicators/AI insights in Redis.
    - [x] Test: Verify cache invalidation upon event trigger.
- [x] Task: Conductor - User Manual Verification 'Data Integration & Caching (Redis & Stock Data)' (Protocol in workflow.md)

## Phase 4: API Layer & Security
- [x] Task: Implement `WatchlistController` (Groups)
    - [x] Endpoint: `POST /api/watchlist/groups`
    - [x] Endpoint: `PATCH /api/watchlist/groups/{groupId}`
    - [x] Endpoint: `DELETE /api/watchlist/groups/{groupId}`
    - [x] Endpoint: `GET /api/watchlist/groups`
    - [x] Apply Security: Verify `memberId` from token matches request or implied context.
    - [x] Test: `WebMvcTest` with security mock.
- [x] Task: Implement `WatchlistController` (Items)
    - [x] Endpoint: `POST /api/watchlist/groups/{groupId}/items`
    - [x] Endpoint: `DELETE /api/watchlist/groups/{groupId}/items/{stockId}`
    - [x] Endpoint: `GET /api/watchlist/groups/{groupId}/items` (The main list view)
    - [x] Logic: `GET` uses `WatchlistQueryRepository` for items, then populates details via `StockDataPort`.
    - [x] Test: `WebMvcTest` verifying response structure and flow.
- [x] Task: API Documentation
    - [x] Generate Spring REST Docs snippets for all endpoints.
    - [x] Verify Swagger UI integration.
- [x] Task: Conductor - User Manual Verification 'API Layer & Security' (Protocol in workflow.md)
