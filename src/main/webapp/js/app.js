// API Configuration
const API_BASE_URL = 'http://localhost:8080/OnlineBank/api';

// Global variables
let currentUser = null;
let userAccounts = [];
let allAccounts = [];

// API Functions
async function apiCall(endpoint, method = 'GET', data = null) {
    try {
        const options = {
            method: method,
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include' // Include cookies for session management
        };

        if (data && method === 'POST') {
            const formData = new URLSearchParams();
            for (const [key, value] of Object.entries(data)) {
                formData.append(key, value);
            }
            options.body = formData;
        }

        const response = await fetch(`${API_BASE_URL}${endpoint}`, options);

        // If server returns non-JSON (e.g., HTML on auth failure), handle safely
        const text = await response.text();
        let result;
        try {
            result = JSON.parse(text);
        } catch {
            return { success: false, error: 'Unexpected server response' };
        }

        return result;
    } catch (error) {
        console.error('API Error:', error);
        return { success: false, error: 'Network error occurred' };
    }
}

// Handle login
async function handleLogin(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const data = Object.fromEntries(formData.entries());
    
    const alertDiv = document.getElementById('login-alert');
    alertDiv.innerHTML = `
        <div class="alert alert-info">
            <div class="text-center">
                <div class="spinner-border spinner-border-sm" role="status"></div>
                Logging in...
            </div>
        </div>
    `;

    const result = await apiCall('/login', 'POST', data);
    
    if (result.success) {
        currentUser = result.data;
        sessionStorage.setItem('userData', JSON.stringify(currentUser));
        
        alertDiv.innerHTML = `
            <div class="alert alert-success">
                Login successful! Redirecting to dashboard...
            </div>
        `;
        
        setTimeout(() => {
            window.location.href = 'dashboard.html';
        }, 1500);
    } else {
        alertDiv.innerHTML = `
            <div class="alert alert-danger">
                ${result.error || 'Login failed'}
            </div>
        `;
    }
}

// Handle register
async function handleRegister(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const data = Object.fromEntries(formData.entries());
    
    if (data.password !== data.confirmPassword) {
        document.getElementById('register-alert').innerHTML = `
            <div class="alert alert-danger">
                Passwords do not match
            </div>
        `;
        return;
    }
    
    const alertDiv = document.getElementById('register-alert');
    alertDiv.innerHTML = `
        <div class="alert alert-info">
            <div class="text-center">
                <div class="spinner-border spinner-border-sm" role="status"></div>
                Creating account...
            </div>
        </div>
    `;

    const result = await apiCall('/register', 'POST', data);
    
    if (result.success) {
        alertDiv.innerHTML = `
            <div class="alert alert-success">
                Account created successfully! Please login.
            </div>
        `;
        setTimeout(() => {
            window.location.href = 'login.html';
        }, 2000);
    } else {
        alertDiv.innerHTML = `
            <div class="alert alert-danger">
                ${result.error || 'Registration failed'}
            </div>
        `;
    }
}

// -------- Accounts + Transactions (split APIs) --------

// Load accounts data (render accounts immediately, fetch transactions separately)
async function loadAccountsData() {
    const container = document.getElementById('accounts-container');

    // Show spinner (in case the page didn't already)
    container.innerHTML = `
        <div class="text-center">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
        </div>
    `;
    
    const result = await apiCall('/accounts');
    
    if (result.success) {
        const accounts = Array.isArray(result.data?.accounts) ? result.data.accounts : [];
        userAccounts = accounts; // keep globals updated if needed elsewhere
        allAccounts = Array.isArray(result.data?.allAccounts) ? result.data.allAccounts : [];

        // Render accounts NOW (do not wait for transactions)
        let html = '';
        if (accounts.length === 0) {
            html = `
                <div class="alert alert-info">
                    No accounts found. Please contact support to create an account.
                </div>
            `;
        } else {
            html = `
                <div class="row">
                    ${accounts.map(account => `
                        <div class="col-md-3 mb-3">
                            <div class="balance-card">
                                <div>Account Number: <b class="balance-amount">${account.accountNumber}</b></div>
                                <div>Balance: <b class="balance-amount">₹ ${Number(account.balance || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</b></div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            `;
        }
        container.innerHTML = html;
    } else {
        container.innerHTML = `
            <div class="alert alert-danger">
                ${result.error || 'Failed to load accounts'}
            </div>
        `;
    }
}

// Load transfer accounts for dropdowns (unchanged)
async function loadTransferAccounts() {
    const fromSelect = document.getElementById('fromAccount');
    const toSelect = document.getElementById('toAccount');

    let page = 0;
    const pageSize = 100;
    let userAccounts = [];
    let allAccounts = [];

    while (true) {
        const result = await apiCall(`/accounts?page=${page}&size=${pageSize}`);
        if (!result.success) {
            console.error('Failed to load accounts:', result.error);
            break;
        }

        userAccounts.push(...(result.data.accounts || []));
        allAccounts.push(...(result.data.allAccounts || []));

        if (!result.data.nextPage) break; // no more pages
        page = result.data.nextPage;
    }

    // Populate user's accounts
    if (fromSelect) {
        fromSelect.innerHTML = '<option value="">Select your account</option>';
        userAccounts.forEach(account => {
            fromSelect.innerHTML += `
                <option value="${account.accountId}">
                    ${account.accountNumber} - ₹${Number(account.balance || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </option>
            `;
        });
    }

    // Populate beneficiary accounts
    if (toSelect) {
        toSelect.innerHTML = '<option value="">Select Beneficiary Account</option>';
        allAccounts.forEach(account => {
            toSelect.innerHTML += `
                <option value="${account.accountId}">
                    ${account.accountNumber} - ${account.beneficiaryName} (₹${Number(account.balance || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })})
                </option>
            `;
        });
    }
}

// Fetch and render transactions by date (fills #transactions-container)
async function loadTransactionsByDate(startDate, endDate) {
    const container = document.getElementById('transactions-container');
    if (!container) return;

    container.innerHTML = `
        <div class="text-center">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
        </div>
    `;

    const txResult = await apiCall(`/transactions?startDate=${startDate}&endDate=${endDate}`);

    if (txResult.success) {
        const transactions = Array.isArray(txResult.data?.transactions) ? txResult.data.transactions : [];
        if (transactions.length === 0) {
            container.innerHTML = `<div class="alert alert-info">No transactions found for this period.</div>`;
        } else {
            container.innerHTML = `
                <div class="row g-2">
                    ${transactions.map(transaction => `
                        <div class="col-sm-6 col-md-4 col-lg-3">
                            <div class="p-2 border rounded bg-white shadow-sm ${transaction.transactionType === 'CREDIT' ? 'border-success' : 'border-danger'}">
                                <div class="d-flex justify-content-between align-items-center">
                                    <div>
                                        <strong class="d-block fs-6">${transaction.transactionType}</strong>
                                        <small class="text-muted d-block">${transaction.transferMode || 'NEFT'}</small>
                                        <small class="text-muted d-block">${new Date(transaction.transactionDate).toLocaleDateString()}</small>
                                    </div>
                                    <div class="text-end">
                                        <strong class="d-block fs-6 text-dark">₹${Number(transaction.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</strong>
                                        <small class="text-muted">${transaction.status}</small>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            `;
        }
    } else {
        container.innerHTML = `<div class="alert alert-danger">${txResult.error || 'Failed to load transactions'}</div>`;
    }
}

// Wire up transactions filter form (if present)
document.addEventListener('DOMContentLoaded', () => {
    const txForm = document.getElementById('transactionFilterForm');
    if (txForm) {
        txForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const startDate = document.getElementById('startDate').value;
            const endDate = document.getElementById('endDate').value;
            if (!startDate || !endDate) return;
            loadTransactionsByDate(startDate, endDate);
        });
    }
});

// Handle transfer
async function handleTransfer(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const data = Object.fromEntries(formData.entries());
    
    const alertDiv = document.getElementById('transfer-alert');
    alertDiv.innerHTML = `
        <div class="alert alert-info">
            <div class="text-center">
                <div class="spinner-border spinner-border-sm" role="status"></div>
                Processing transfer...
            </div>
        </div>
    `;

    const result = await apiCall('/transfer', 'POST', data);
    
    if (result.success) {
        // Store transfer data for confirmation page
        const transferData = {
            fromAccount: userAccounts.find(acc => acc.accountId == data.fromAccountId)?.accountNumber,
            toAccount: allAccounts.find(acc => acc.accountId == data.toAccountId)?.accountNumber,
            beneficiaryName: allAccounts.find(acc => acc.accountId == data.toAccountId)?.beneficiaryName,
            transferMode: data.transferMode,
            amount: data.amount,
            otp: result.data.otp
        };
        sessionStorage.setItem('transferData', JSON.stringify(transferData));
        
        window.location.href = 'transfer-confirm.html';
    } else {
        alertDiv.innerHTML = `
            <div class="alert alert-danger">
                ${result.error || 'Transfer failed'}
            </div>
        `;
    }
}

// Handle transfer confirmation
async function handleTransferConfirm(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const data = Object.fromEntries(formData.entries());
    
    const alertDiv = document.getElementById('confirm-alert');
    alertDiv.innerHTML = `
        <div class="alert alert-info">
            <div class="text-center">
                <div class="spinner-border spinner-border-sm" role="status"></div>
                Verifying OTP and completing transfer...
            </div>
        </div>
    `;

    const result = await apiCall('/verifyOtp', 'POST', data);
    
    if (result.success) {
        const mainContent = document.querySelector('main');
        mainContent.innerHTML = `
            <div class="container py-4">
                <div class="row">
                    <div class="col-12">
                        <div class="text-center">
                            <div class="mb-4">
                                <i class="fas fa-check-circle text-success" style="font-size: 4rem;"></i>
                            </div>
                            <h2 class="text-success mb-3">Transfer Completed Successfully!</h2>
                            <div class="alert alert-success">
                                <h5>Transaction Details</h5>
                                <p><strong>Amount:</strong> ₹${result.data.amount ? parseFloat(result.data.amount).toLocaleString('en-IN', {minimumFractionDigits: 2}) : '0.00'}</p>
                                <p><strong>Transfer Mode:</strong> ${result.data.transferMode || 'NEFT'}</p>
                                <p><strong>Status:</strong> Completed</p>
                                <p class="mb-0">Your money has been transferred successfully. You will receive a confirmation shortly.</p>
                            </div>
                            <div class="mt-4">
                                <a href="dashboard.html" class="btn btn-primary me-2">Go to Dashboard</a>
                                <a href="accounts.html" class="btn btn-outline-primary">View Accounts</a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        sessionStorage.removeItem('transferData');
    } else {
        alertDiv.innerHTML = `
            <div class="alert alert-danger">
                ${result.error || 'OTP verification failed'}
            </div>
        `;
    }
}

// Handle add account
async function handleAddAccount(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const data = Object.fromEntries(formData.entries());
    
    const alertDiv = document.getElementById('add-account-alert');
    alertDiv.innerHTML = `
        <div class="alert alert-info">
            <div class="text-center">
                <div class="spinner-border spinner-border-sm" role="status"></div>
                Adding account...
            </div>
        </div>
    `;

    const result = await apiCall('/addAccount', 'POST', data);
    
    if (result.success) {
        alertDiv.innerHTML = `
            <div class="alert alert-success">
                <h5>Account Added Successfully!</h5>
                <p>Your new account has been created with account number: ${data.accountNumber}</p>
            </div>
        `;
        
        setTimeout(() => {
            window.location.href = 'accounts.html';
        }, 3000);
    } else {
        alertDiv.innerHTML = `
            <div class="alert alert-danger">
                ${result.error || 'Failed to add account'}
            </div>
        `;
    }
}

// Logout function
async function logout() {
    await apiCall('/logout', 'POST');
    sessionStorage.removeItem('userData');
    sessionStorage.removeItem('transferData');
    currentUser = null;
    window.location.href = 'index.html';
}
