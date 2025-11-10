# Mobile Client Sync API Documentation

## Overview

This document describes the sync API contract for mobile clients (Android/iOS) to synchronize data with the backend server.

## Base URL

```
Production: https://yourdomain.com/api
Development: http://your-ec2-ip/api
```

## Authentication

Currently, the API does not implement authentication. In production, you should add:
- JWT tokens
- API keys
- OAuth 2.0

## Endpoints

### 1. Push Sync

Push local changes from mobile client to server.

**Endpoint:** `POST /api/sync/push`

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "userId": "user123",
  "entries": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "payload": "{\"title\":\"My Note\",\"content\":\"Note content\"}",
      "updatedAt": "2025-11-10T12:30:00Z",
      "deleted": false
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "payload": "{\"title\":\"Another Note\"}",
      "updatedAt": "2025-11-10T12:35:00Z",
      "deleted": false
    }
  ]
}
```

**Response (200 OK):**
```json
{
  "accepted": 2,
  "conflicts": []
}
```

**Response with Conflicts:**
```json
{
  "accepted": 1,
  "conflicts": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "serverVersion": {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "payload": "{\"title\":\"Server Version\"}",
        "updatedAt": "2025-11-10T12:40:00Z",
        "deleted": false
      },
      "reason": "Server version is newer"
    }
  ]
}
```

### 2. Pull Sync

Fetch changes from server that occurred after a specific timestamp.

**Endpoint:** `GET /api/sync/pull`

**Query Parameters:**
- `userId` (required): User identifier
- `since` (optional): ISO 8601 timestamp. If omitted, returns all entries for the user.

**Example Request:**
```
GET /api/sync/pull?userId=user123&since=2025-11-10T10:00:00Z
```

**Response (200 OK):**
```json
{
  "entries": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "payload": "{\"title\":\"Updated Note\"}",
      "updatedAt": "2025-11-10T12:45:00Z",
      "deleted": false
    }
  ],
  "serverTime": "2025-11-10T13:00:00Z"
}
```

### 3. Health Check

Check if the API is available.

**Endpoint:** `GET /api/health`

**Response (200 OK):**
```
OK
```

## Conflict Resolution Strategy

The API uses **Last-Write-Wins (LWW)** conflict resolution based on the `updatedAt` timestamp:

1. **Client timestamp > Server timestamp**: Server accepts the client version
2. **Client timestamp < Server timestamp**: Server rejects and returns conflict
3. **Client timestamp = Server timestamp**: Server accepts (idempotent operation)

### Handling Conflicts on Mobile Client

When the server returns conflicts:

```kotlin
// Example in Kotlin (Android)
fun handlePushResponse(response: PushResponse) {
    if (response.conflicts.isNotEmpty()) {
        response.conflicts.forEach { conflict ->
            // Option 1: Auto-resolve - accept server version
            updateLocalEntry(conflict.serverVersion)
            
            // Option 2: Prompt user to choose
            showConflictDialog(conflict)
            
            // Option 3: Merge strategies (custom logic)
            val merged = mergeEntries(getLocalEntry(conflict.id), conflict.serverVersion)
            updateLocalEntry(merged)
        }
    }
}
```

## Mobile Client Implementation Guide

### Local Storage Schema

Store entries locally with these fields:

```sql
CREATE TABLE entries (
    id TEXT PRIMARY KEY,
    payload TEXT,
    updated_at INTEGER,  -- Unix timestamp
    deleted INTEGER DEFAULT 0,
    synced INTEGER DEFAULT 0  -- 0 = not synced, 1 = synced
);

CREATE INDEX idx_synced ON entries(synced);
CREATE INDEX idx_updated ON entries(updated_at);
```

### Sync Algorithm

```
1. Check network connectivity
2. If online:
   a. Query local database for unsynced entries (synced = 0)
   b. Convert to API format and POST to /api/sync/push
   c. Handle response:
      - Mark accepted entries as synced (synced = 1)
      - Handle conflicts (see conflict resolution)
   d. Call GET /api/sync/pull with last sync timestamp
   e. Merge server entries into local database
   f. Update last sync timestamp
3. If offline:
   - Continue working with local data
   - Mark changes as unsynced
```

### Example Implementation (Kotlin/Android)

```kotlin
class SyncManager(
    private val api: SyncApi,
    private val db: AppDatabase
) {
    suspend fun sync(userId: String) {
        // Push local changes
        val unsyncedEntries = db.entryDao().getUnsynced()
        if (unsyncedEntries.isNotEmpty()) {
            val pushRequest = PushRequest(
                userId = userId,
                entries = unsyncedEntries.map { it.toDto() }
            )
            val pushResponse = api.push(pushRequest)
            
            // Mark accepted as synced
            pushResponse.accepted.forEach { id ->
                db.entryDao().markSynced(id)
            }
            
            // Handle conflicts
            pushResponse.conflicts.forEach { conflict ->
                handleConflict(conflict)
            }
        }
        
        // Pull server changes
        val lastSyncTime = preferences.getLastSyncTime()
        val pullResponse = api.pull(userId, lastSyncTime)
        
        // Merge server entries
        pullResponse.entries.forEach { entry ->
            db.entryDao().insertOrUpdate(entry.toEntity())
        }
        
        // Update last sync time
        preferences.setLastSyncTime(pullResponse.serverTime)
    }
    
    private fun handleConflict(conflict: ConflictRecord) {
        // Strategy 1: Accept server version
        db.entryDao().update(conflict.serverVersion.toEntity())
        
        // Strategy 2: Show dialog to user
        // showConflictDialog(conflict)
    }
}
```

### Example Implementation (Swift/iOS)

```swift
class SyncManager {
    let api: SyncAPI
    let database: Database
    
    func sync(userId: String) async throws {
        // Push local changes
        let unsyncedEntries = try database.getUnsyncedEntries()
        if !unsyncedEntries.isEmpty {
            let pushRequest = PushRequest(
                userId: userId,
                entries: unsyncedEntries.map { $0.toDTO() }
            )
            let pushResponse = try await api.push(pushRequest)
            
            // Mark accepted as synced
            for id in pushResponse.acceptedIds {
                try database.markSynced(id: id)
            }
            
            // Handle conflicts
            for conflict in pushResponse.conflicts {
                handleConflict(conflict)
            }
        }
        
        // Pull server changes
        let lastSyncTime = UserDefaults.standard.lastSyncTime
        let pullResponse = try await api.pull(userId: userId, since: lastSyncTime)
        
        // Merge server entries
        for entry in pullResponse.entries {
            try database.insertOrUpdate(entry.toEntity())
        }
        
        // Update last sync time
        UserDefaults.standard.lastSyncTime = pullResponse.serverTime
    }
    
    private func handleConflict(_ conflict: ConflictRecord) {
        // Accept server version
        try? database.update(conflict.serverVersion.toEntity())
    }
}
```

## Best Practices

### 1. Sync Frequency
- Sync on app launch
- Sync when app returns to foreground
- Sync after significant user actions
- Periodic background sync (if supported by platform)

### 2. Batch Operations
- Batch multiple changes into a single push request
- Limit batch size to 100-500 entries per request

### 3. Error Handling
- Implement exponential backoff for failed sync attempts
- Queue failed operations for retry
- Handle network timeouts gracefully

### 4. Data Integrity
- Use UUIDs for entry IDs (generated on client)
- Always use UTC timestamps
- Validate data before syncing

### 5. Performance
- Index local database properly
- Use pagination for large datasets
- Compress payloads if they're large

## Error Responses

### 400 Bad Request
```json
{
  "error": "Invalid request",
  "message": "userId is required"
}
```

### 409 Conflict
```json
{
  "error": "Data conflict",
  "message": "The operation could not be completed due to a data conflict"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal server error",
  "message": "An unexpected error occurred. Please try again later."
}
```

## Testing

### Test Push Sync with curl

```bash
curl -X POST http://your-server/api/sync/push \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "entries": [{
      "id": "test-id-1",
      "payload": "{\"test\":\"data\"}",
      "updatedAt": "2025-11-10T12:00:00Z",
      "deleted": false
    }]
  }'
```

### Test Pull Sync with curl

```bash
curl "http://your-server/api/sync/pull?userId=test-user&since=2025-11-10T10:00:00Z"
```

## Recommended Libraries

### Android
- **Networking**: Retrofit + OkHttp
- **Local Storage**: Room Database
- **JSON**: Gson or Moshi
- **Coroutines**: Kotlin Coroutines for async operations

### iOS
- **Networking**: URLSession or Alamofire
- **Local Storage**: Core Data or Realm
- **JSON**: Codable (built-in)
- **Async**: async/await (Swift 5.5+)

### React Native
- **Networking**: Axios or Fetch API
- **Local Storage**: SQLite (react-native-sqlite-storage) or WatermelonDB
- **State Management**: Redux or MobX
