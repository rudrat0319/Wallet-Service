-- ===============================
-- USERS
-- ===============================
INSERT INTO users (id, email, created_at)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'alice@example.com', NOW()),
  ('22222222-2222-2222-2222-222222222222', 'bob@example.com', NOW());

-- ===============================
-- SYSTEM USER (TREASURY)
-- ===============================
INSERT INTO users (id, email, created_at)
VALUES
  ('00000000-0000-0000-0000-000000000000', 'system@wallet.internal', NOW());

-- ===============================
-- WALLETS
-- One wallet per (user, asset)
-- ===============================

-- Alice wallets
INSERT INTO wallets (id, user_id, asset_type, balance, created_at, updated_at)
VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
   '11111111-1111-1111-1111-111111111111',
   'COIN',
   100.00,
   NOW(),
   NOW()),

  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
   '11111111-1111-1111-1111-111111111111',
   'GEM',
   10.00,
   NOW(),
   NOW());

-- Bob wallets
INSERT INTO wallets (id, user_id, asset_type, balance, created_at, updated_at)
VALUES
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
   '22222222-2222-2222-2222-222222222222',
   'COIN',
   50.00,
   NOW(),
   NOW());

-- System (Treasury) wallets
INSERT INTO wallets (id, user_id, asset_type, balance, created_at, updated_at)
VALUES
  ('cccccccc-cccc-cccc-cccc-ccccccccccc1',
   '00000000-0000-0000-0000-000000000000',
   'COIN',
   1000000.00,
   NOW(),
   NOW()),

  ('cccccccc-cccc-cccc-cccc-ccccccccccc2',
   '00000000-0000-0000-0000-000000000000',
   'GEM',
   50000.00,
   NOW(),
   NOW());

-- ===============================
-- OPTIONAL: INITIAL LEDGER ENTRIES
-- (to match opening balances)
-- ===============================

INSERT INTO ledger_entries (id, wallet_id, transaction_type, amount, reference_id, created_at)
VALUES
  (gen_random_uuid(),
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
   'CREDIT',
   100.00,
   'INITIAL_BALANCE',
   NOW()),

  (gen_random_uuid(),
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
   'CREDIT',
   50.00,
   'INITIAL_BALANCE',
   NOW()),

  (gen_random_uuid(),
   'cccccccc-cccc-cccc-cccc-ccccccccccc1',
   'CREDIT',
   1000000.00,
   'SYSTEM_FUNDING',
   NOW());
