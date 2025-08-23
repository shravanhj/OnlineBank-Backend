// Transfer page specific functions

let userAccounts = [];
let allAccounts = [];
let selectedTransferData = null;

// Initialize transfer page
async function initializeTransferPage() {
    showLoading('accounts-list', 'Loading your accounts...');
    try {
        await loadAccountsForTransfer();
        setupTransferForm();
        
        // Check if there's a pre-selected account from accounts page
        const preSelectedAccount = getSessionStorageItem('selectedFromAccount');
        if (preSelectedAccount) {
            const fromAccountSelect = document.getElementById('fromAccount');
            if (fromAccountSelect) {
                fromAccountSelect.value = preSelectedAccount;
            }
            // Clear the stored selection
            removeSessionStorageItem('selectedFromAccount');
        }
        
    } catch (error) {
        console.error('Error initializing transfer page:', error);
        handleApiError(error, 'transfer-alert');
    }
}

// Load accounts for transfer dropdowns
async function loadAccountsForTransfer() {
    try {
        const user = getCurrentUser();
        if (!user) {
            throw new Error('User not authenticated');
        }
        
        // Load user's accounts for "from" dropdown
        const userAccountsResponse = await accountAPI.getUserAccounts(user.userId);
        userAccounts = userAccountsResponse || [];
        
        // Load all accounts for "to" dropdown
        const allAccountsResponse = await accountAPI.getAllAccounts();
        allAccounts = allAccountsResponse || [];
        
        populateAccountDropdowns();
        
    } catch (error) {
        console.error('Error loading accounts:', error);
        throw error;
    }
}

// Populate account dropdown menus
function populateAccountDropdowns() {
    const fromAccountSelect = document.getElementById('fromAccount');
    const toAccountSelect = document.getElementById('toAccount');
    
    if (!fromAccountSelect || !toAccountSelect) return;
    
    // Clear existing options (except the first placeholder)
    fromAccountSelect.innerHTML = '<option value="">Select account...</option>';
    toAccountSelect.innerHTML = '<option value="">Select account...</option>';
    
    // Populate "from" account dropdown with user's accounts
    userAccounts.forEach(account => {
        const option = document.createElement('option');
        option.value = account.accountId;
        option.textContent = `${account.accountName} - ${formatAccountNumber(account.accountNumber)} (${formatCurrency(account.balance)})`;
        option.dataset.balance = account.balance;
        option.dataset.accountName = account.accountName;
        fromAccountSelect.appendChild(option);
    });
    
    // Populate "to" account dropdown with all accounts
    allAccounts.forEach(account => {
        const option = document.createElement('option');
        option.value = account.accountId;
        option.textContent = `${account.accountName} - ${formatAccountNumber(account.accountNumber)}`;
        option.dataset.accountName = account.accountName;
        toAccountSelect.appendChild(option);
    });
}

// Setup transfer form handlers
function setupTransferForm() {
    const transferForm = document.getElementById('transfer-form');
    if (!transferForm) return;
    
    transferForm.addEventListener('submit', handleTransferSubmit);
    
    // Add real-time validation
    const fromAccountSelect = document.getElementById('fromAccount');
    const toAccountSelect = document.getElementById('toAccount');
    const amountInput = document.getElementById('amount');
    
    if (fromAccountSelect) {
        fromAccountSelect.addEventListener('change', validateTransferForm);
    }
    
    if (toAccountSelect) {
        toAccountSelect.addEventListener('change', validateTransferForm);
    }
    
    if (amountInput) {
        amountInput.addEventListener('input', debounce(validateTransferForm, 300));
    }
}

// Handle transfer form submission
async function handleTransferSubmit(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const transferData = {
        fromAccount: formData.get('fromAccount'),
        toAccount: formData.get('toAccount'),
        amount: parseFloat(formData.get('amount')),
        description: formData.get('description') || ''
    };
    
    // Validate transfer data
    const validationErrors = validateTransferData(transferData);
    if (validationErrors.length > 0) {
        showAlert('transfer-alert', validationErrors.join('<br>'), 'danger');
        return;
    }
    
    // Get account names for confirmation
    const fromAccountOption = document.querySelector(`#fromAccount option[value="${transferData.fromAccount}"]`);
    const toAccountOption = document.querySelector(`#toAccount option[value="${transferData.toAccount}"]`);
    
    transferData.fromAccountName = fromAccountOption ? fromAccountOption.dataset.accountName : '';
    transferData.toAccountName = toAccountOption ? toAccountOption.dataset.accountName : '';
    
    // Store transfer data for confirmation page
    setSessionStorageItem('transferData', transferData);
    
    // Redirect to confirmation page
    window.location.href = 'transfer-confirm.html';
}

// Validate transfer data
function validateTransferData(transferData) {
    const errors = [];
    
    // Check required fields
    if (!transferData.fromAccount) {
        errors.push('Please select a source account');
    }
    
    if (!transferData.toAccount) {
        errors.push('Please select a destination account');
    }
    
    if (!transferData.amount || transferData.amount <= 0) {
        errors.push('Please enter a valid amount');
    }
    
    // Check if accounts are different
    if (transferData.fromAccount && transferData.toAccount && 
        transferData.fromAccount === transferData.toAccount) {
        errors.push('Source and destination accounts must be different');
    }
    
    // Check sufficient balance
    if (transferData.fromAccount && transferData.amount) {
        const fromAccount = userAccounts.find(acc => acc.accountId === transferData.fromAccount);
        if (fromAccount && parseFloat(fromAccount.balance) < transferData.amount) {
            errors.push(`Insufficient balance. Available: ${formatCurrency(fromAccount.balance)}`);
        }
    }
    
    // Check minimum transfer amount
    if (transferData.amount && transferData.amount < 0.01) {
        errors.push('Minimum transfer amount is $0.01');
    }
    
    // Check maximum transfer amount (example limit)
    if (transferData.amount && transferData.amount > 10000) {
        errors.push('Maximum transfer amount is $10,000');
    }
    
    return errors;
}

// Real-time form validation
function validateTransferForm() {
    const fromAccount = document.getElementById('fromAccount').value;
    const toAccount = document.getElementById('toAccount').value;
    const amount = parseFloat(document.getElementById('amount').value) || 0;
    
    const transferData = { fromAccount, toAccount, amount };
    const errors = validateTransferData(transferData);
    
    // Update form UI based on validation
    const submitBtn = document.querySelector('#transfer-form button[type="submit"]');
    if (submitBtn) {
        submitBtn.disabled = errors.length > 0;
    }
    
    // Show inline validation messages
    updateFieldValidation('fromAccount', !fromAccount ? 'Please select an account' : '');
    updateFieldValidation('toAccount', !toAccount ? 'Please select an account' : '');
    updateFieldValidation('amount', amount <= 0 ? 'Please enter a valid amount' : '');
    
    // Show balance information
    if (fromAccount) {
        showAccountBalance(fromAccount);
    }
}

// Update field validation display
function updateFieldValidation(fieldId, errorMessage) {
    const field = document.getElementById(fieldId);
    if (!field) return;
    
    let feedbackDiv = field.parentNode.querySelector('.invalid-feedback');
    
    if (errorMessage) {
        field.classList.add('is-invalid');
        field.classList.remove('is-valid');
        
        if (!feedbackDiv) {
            feedbackDiv = document.createElement('div');
            feedbackDiv.className = 'invalid-feedback';
            field.parentNode.appendChild(feedbackDiv);
        }
        feedbackDiv.textContent = errorMessage;
    } else {
        field.classList.remove('is-invalid');
        field.classList.add('is-valid');
        
        if (feedbackDiv) {
            feedbackDiv.remove();
        }
    }
}

// Show account balance information
function showAccountBalance(accountId) {
    const account = userAccounts.find(acc => acc.accountId === accountId);
    if (!account) return;
    
    let balanceDiv = document.getElementById('account-balance-info');
    if (!balanceDiv) {
        balanceDiv = document.createElement('div');
        balanceDiv.id = 'account-balance-info';
        balanceDiv.className = 'alert alert-info mt-2';
        
        const fromAccountSelect = document.getElementById('fromAccount');
        fromAccountSelect.parentNode.appendChild(balanceDiv);
    }
    
    balanceDiv.innerHTML = `
        <i class="fas fa-info-circle me-2"></i>
        Available Balance: <strong>${formatCurrency(account.balance)}</strong>
    `;
}

// Quick transfer amounts
function setQuickAmount(amount) {
    const amountInput = document.getElementById('amount');
    if (amountInput) {
        amountInput.value = amount;
        validateTransferForm();
    }
}

// Calculate transfer fee (if applicable)
function calculateTransferFee(amount, fromAccountType, toAccountType) {
    // Example fee calculation - modify based on business rules
    let fee = 0;
    
    // Different account types might have different fees
    if (fromAccountType !== toAccountType) {
        fee = Math.min(amount * 0.001, 5); // 0.1% with max $5
    }
    
    // Free transfers under $100
    if (amount < 100) {
        fee = 0;
    }
    
    return fee;
}

// Show transfer preview
function showTransferPreview() {
    const fromAccount = document.getElementById('fromAccount').value;
    const toAccount = document.getElementById('toAccount').value;
    const amount = parseFloat(document.getElementById('amount').value) || 0;
    
    if (!fromAccount || !toAccount || !amount) return;
    
    const fromAccountData = userAccounts.find(acc => acc.accountId === fromAccount);
    const toAccountData = allAccounts.find(acc => acc.accountId === toAccount);
    
    if (!fromAccountData || !toAccountData) return;
    
    const fee = calculateTransferFee(amount, fromAccountData.accountType, toAccountData.accountType);
    const total = amount + fee;
    
    let previewDiv = document.getElementById('transfer-preview');
    if (!previewDiv) {
        previewDiv = document.createElement('div');
        previewDiv.id = 'transfer-preview';
        previewDiv.className = 'card mt-3';
        
        const form = document.getElementById('transfer-form');
        form.appendChild(previewDiv);
    }
    
    previewDiv.innerHTML = `
        <div class="card-header">
            <h6 class="mb-0">Transfer Preview</h6>
        </div>
        <div class="card-body">
            <div class="row">
                <div class="col-6">
                    <small class="text-muted">From:</small>
                    <p class="mb-1">${fromAccountData.accountName}</p>
                </div>
                <div class="col-6">
                    <small class="text-muted">To:</small>
                    <p class="mb-1">${toAccountData.accountName}</p>
                </div>
            </div>
            <hr>
            <div class="d-flex justify-content-between">
                <span>Transfer Amount:</span>
                <span>${formatCurrency(amount)}</span>
            </div>
            ${fee > 0 ? `
                <div class="d-flex justify-content-between">
                    <span>Transfer Fee:</span>
                    <span>${formatCurrency(fee)}</span>
                </div>
                <hr>
                <div class="d-flex justify-content-between fw-bold">
                    <span>Total:</span>
                    <span>${formatCurrency(total)}</span>
                </div>
            ` : ''}
        </div>
    `;
}

// Initialize transfer page on DOM load
document.addEventListener('DOMContentLoaded', function() {
    if (window.location.pathname.includes('transfer.html')) {
        if (!checkAuthAndRedirect()) return;
        
        const user = getCurrentUser();
        if (user) {
            document.getElementById('user-name').textContent = user.userName;
            initializeTransferPage();
        }
    }
});

// Export functions for use in other files
window.transferFunctions = {
    initializeTransferPage,
    loadAccountsForTransfer,
    handleTransferSubmit,
    validateTransferData,
    setQuickAmount,
    showTransferPreview
};
