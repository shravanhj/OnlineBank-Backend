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
        const result = await response.json();
        
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
        
        // Redirect to dashboard
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

// Load accounts data
async function loadAccountsData() {
    const container = document.getElementById('accounts-container');
    
    const result = await apiCall('/accounts');
    
    if (result.success) {
        const accounts = result.data.accounts;
        const transactions = result.data.transactions;
        
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
                                <div >Account Number: <b class="balance-amount"> ${account.accountNumber}</b></div>
                                <div>Balance: <b class="balance-amount"> ₹ ${account.balance.toLocaleString('en-IN', {minimumFractionDigits: 2})}</b></div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            `;
            
            if (transactions.length > 0) {
                html += `
<div class="mt-4">
    <h4 class="mb-3">Recent Transactions</h4>
    <div class="row g-2">
        ${transactions.slice(0, 10).map(transaction => `
            <div class="col-sm-6 col-md-4 col-lg-3">
                <div class="p-2 border rounded-full bg-white shadow-sm 
                    ${transaction.transactionType === 'CREDIT' ? 'bg-light border-success' : 'bg-light border-danger'}">
                    
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <strong class="d-block fs-6">${transaction.transactionType}</strong>
                            <small class="text-muted d-block">${transaction.transferMode || 'NEFT'}</small>
                            <small class="text-muted d-block">${new Date(transaction.transactionDate).toLocaleDateString()}</small>
                        </div>
                        <div class="text-end">
                            <strong class="d-block fs-6 text-dark">₹${transaction.amount.toLocaleString('en-IN', {minimumFractionDigits: 2})}</strong>
                            <small class="text-muted">${transaction.status}</small>
                        </div>
                    </div>
                </div>
            </div>
        `).join('')}
    </div>
</div>

                `;
            }
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

// Load transfer accounts for dropdowns
async function loadTransferAccounts() {
    const fromSelect = document.getElementById('fromAccount');
    const toSelect = document.getElementById('toAccount');
    
    const result = await apiCall('/accounts');
    
    if (result.success) {
        userAccounts = result.data.accounts;
        allAccounts = result.data.allAccounts || [];
        
        // Populate from account dropdown (user's accounts)
        fromSelect.innerHTML = '<option value="">Select your account</option>';
        userAccounts.forEach(account => {
            fromSelect.innerHTML += `
                <option value="${account.accountId}">
                    ${account.accountNumber} - ₹${account.balance.toLocaleString('en-IN', {minimumFractionDigits: 2})}
                </option>
            `;
        });
        
        // Populate to account dropdown (all other accounts)
        toSelect.innerHTML = '<option value="">Select Beneficiary Account</option>';
        allAccounts.forEach(account => {
            toSelect.innerHTML += `
                <option value="${account.accountId}">
                    ${account.accountNumber} - ${account.beneficiaryName} (₹${account.balance.toLocaleString('en-IN', {minimumFractionDigits: 2})})
                </option>
            `;
        });
    }
}

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
        
        // Redirect to confirmation page
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

    // Call the OTP verification API
    const result = await apiCall('/verifyOtp', 'POST', data);
    
    if (result.success) {
        // Show success confirmation screen
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
        
        // Clear transfer data
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
        
        // Redirect to accounts page after 3 seconds
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