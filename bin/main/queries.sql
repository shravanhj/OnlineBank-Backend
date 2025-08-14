-- User Registration
INSERT INTO users (name, phone_number, password) VALUES (?, ?, ?);

-- User Login
SELECT user_id, name, phone_number FROM users WHERE phone_number = ? AND password = ?;

-- Add New Account
INSERT INTO accounts (user_id, account_number, balance) VALUES (?, ?, ?);

-- Get User Accounts
SELECT a.account_id, a.account_number, a.balance 
FROM accounts a 
WHERE a.user_id = ?;

-- Get Account Details
SELECT a.account_id, a.account_number, a.balance, u.name as account_holder
FROM accounts a
JOIN users u ON a.user_id = u.user_id
WHERE a.account_number = ?;

-- Check Account Balance
SELECT balance FROM accounts WHERE account_id = ?;

-- Create Transaction
INSERT INTO transactions (from_account_id, to_account_id, amount, transaction_type, otp, otp_expiry)
VALUES (?, ?, ?, 'TRANSFER', ?, DATE_ADD(NOW(), INTERVAL 5 MINUTE));

-- Update Transaction Status
UPDATE transactions 
SET status = ?, 
    transaction_date = CURRENT_TIMESTAMP 
WHERE transaction_id = ?;

-- Get Transaction Details
SELECT t.transaction_id, 
       t.amount,
       t.status,
       t.transaction_date,
       t.otp,
       t.otp_expiry,
       a1.account_number as from_account,
       a2.account_number as to_account,
       u1.name as from_name,
       u2.name as to_name
FROM transactions t
JOIN accounts a1 ON t.from_account_id = a1.account_id
JOIN accounts a2 ON t.to_account_id = a2.account_id
JOIN users u1 ON a1.user_id = u1.user_id
JOIN users u2 ON a2.user_id = u2.user_id
WHERE t.transaction_id = ?;

-- Get User Transactions
SELECT t.transaction_id,
       t.amount,
       t.status,
       t.transaction_date,
       a1.account_number as from_account,
       a2.account_number as to_account,
       u1.name as from_name,
       u2.name as to_name
FROM transactions t
JOIN accounts a1 ON t.from_account_id = a1.account_id
JOIN accounts a2 ON t.to_account_id = a2.account_id
JOIN users u1 ON a1.user_id = u1.user_id
JOIN users u2 ON a2.user_id = u2.user_id
WHERE a1.user_id = ? OR a2.user_id = ?
ORDER BY t.transaction_date DESC;

-- Update Account Balance
UPDATE accounts 
SET balance = balance + ? 
WHERE account_id = ?;

-- Verify OTP
SELECT transaction_id 
FROM transactions 
WHERE transaction_id = ? 
AND otp = ? 
AND otp_expiry > NOW() 
AND status = 'PENDING';

-- Get All Beneficiary Accounts
SELECT a.account_id, a.account_number, a.balance, u.name as account_holder
FROM accounts a
JOIN users u ON a.user_id = u.user_id
WHERE a.account_number != ?; 