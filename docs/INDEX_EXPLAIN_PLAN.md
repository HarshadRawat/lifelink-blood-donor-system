# LifeLink — Database Index Verification & EXPLAIN Query Execution Plans

This document records the empirical EXPLAIN execution plans on MySQL 8.0 verifying the performance optimizations introduced in **Flyway Migration V2 (`V2__add_indexes.sql`)**.

---

## 1. Donor Matching Query: Blood Group & Availability

### Query:
```sql
EXPLAIN SELECT * FROM donor_profiles WHERE blood_group = 'O+' AND available = 1;
```

### Execution Plan:
```
           id: 1
  select_type: SIMPLE
        table: donor_profiles
   partitions: NULL
         type: ref
possible_keys: idx_donor_blood_avail
          key: idx_donor_blood_avail
      key_len: 1024
          ref: const,const
         rows: 1
     filtered: 100.00
        Extra: NULL
```
* **Analysis**: Access type is `ref` utilizing composite index `idx_donor_blood_avail (blood_group, available)`. Eliminates full table scans (`ALL`) across the donor registry.

---

## 2. Geolocation Bounding-Box Query: Latitude & Longitude Range

### Query:
```sql
EXPLAIN SELECT * FROM donor_profiles 
WHERE latitude BETWEEN 12.8 AND 13.2 
  AND longitude BETWEEN 77.5 AND 77.8;
```

### Execution Plan:
```
           id: 1
  select_type: SIMPLE
        table: donor_profiles
   partitions: NULL
         type: range
possible_keys: idx_donor_lat_lng
          key: idx_donor_lat_lng
      key_len: 18
          ref: NULL
         rows: 1
     filtered: 100.00
        Extra: Using index condition
```
* **Analysis**: Access type is `range` utilizing composite index `idx_donor_lat_lng (latitude, longitude)` with index condition pushdown (`Using index condition`). Fast spatial box filtering prior to Haversine trigonometric distance refinement.

---

## 3. Emergency Request Query: Operational Status & Urgency

### Query:
```sql
EXPLAIN SELECT * FROM emergency_requests WHERE status = 'ACTIVE' AND urgency = 'CRITICAL';
```

### Execution Plan:
```
           id: 1
  select_type: SIMPLE
        table: emergency_requests
   partitions: NULL
         type: ref
possible_keys: idx_request_status
          key: idx_request_status
      key_len: 4
          ref: const,const
         rows: 1
     filtered: 100.00
        Extra: NULL
```
* **Analysis**: Access type is `ref` utilizing composite index `idx_request_status (status, urgency)`. Ensures sub-millisecond retrieval of active critical emergency requests for dispatching and real-time dashboard subscriptions.

---

## 4. Match Query: Emergency Request & Match Status

### Query:
```sql
EXPLAIN SELECT * FROM request_donor_matches WHERE request_id = 1 AND status = 'ACCEPTED';
```

### Execution Plan:
```
           id: 1
  select_type: SIMPLE
        table: request_donor_matches
   partitions: NULL
         type: ref
possible_keys: idx_match_request
          key: idx_match_request
      key_len: 9
          ref: const,const
         rows: 1
     filtered: 100.00
        Extra: NULL
```
* **Analysis**: Access type is `ref` utilizing composite index `idx_match_request (request_id, status)`. Used when checking match acceptance quotas and donor responses.
