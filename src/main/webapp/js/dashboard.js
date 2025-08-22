// Dashboard specific functions

let userAccounts = [];

// Load account summary for dashboard
async function loadAccountSummary() {
    const summaryContainer = document.getElementById('account-summary');
    if (!summaryContainer) return;
    
    showLoading('account-summary', 'Loading your accounts...');
    
    try {
        const user = getCurrentUser();
        if (!user) {
            throw new Error('User not authenticated');
        }
        
        const accounts = await accountAPI.getUserAccounts(user.userId);
        userAccounts = accounts;
        
        if (accounts && accounts.length > 0) {
            displayAccountSummary(accounts);
        } else {
            showEmptyAccountSummary();
        }
        
    } catch (error) {
        console.error('Error loading account summary:', error);
        summaryContainer.innerHTML = `
            <div class="col-12">
                <div class="alert alert-danger">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    Error loading accounts. Please try refreshing the page.
                </div>
            </div>
        `;
    }
}

// Display account summary cards
function displayAccountSummary(accounts) {
    const summaryContainer = document.getElementById('account-summary');
    if (!summaryContainer) return;
    
    let totalBalance = 0;
    let summaryHtml = '';
    
    accounts.forEach(account => {
        totalBalance += parseFloat(account.balance || 0);
        
        summaryHtml += `
            <div class="col-md-4 mb-3">
                <div class="account-summary-card">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <div>
                            <h6 class="card-title mb-1">${account.accountName || 'Account'}</h6>
                            <small class="text-muted">${formatAccountNumber(account.accountNumber)}</small>
                        </div>
                        <span class="badge bg-primary">${account.accountType || 'N/A'}</span>
                    </div>
                    <div class="balance-display">
                        <h4 class="text-success mb-0">${formatCurrency(account.balance || 0)}</h4>
                    </div>
                    <div class="mt-2">
                        <a href="accounts.html" class="btn btn-sm btn-outline-primary">View Details</a>
                    </div>
                </div>
            </div>
        `;
    });
    
    // Add total balance card
    summaryHtml += `
        <div class="col-md-4 mb-3">
            <div class="account-summary-card total-balance-card">
                <div class="text-center">
                    <h6 class="card-title mb-2">Total Balance</h6>
                    <h3 class="text-primary mb-0">${formatCurrency(totalBalance)}</h3>
                    <small class="text-muted">Across ${accounts.length} account${accounts.length !== 1 ? 's' : ''}</small>
                </div>
            </div>
        </div>
    `;
    
    summaryContainer.innerHTML = summaryHtml;
    
    // Animate the balance numbers
    setTimeout(() => {
        const balanceElements = summaryContainer.querySelectorAll('.balance-display h4');
        balanceElements.forEach(element => {
            element.style.opacity = '0';
            element.style.transform = 'translateY(20px)';
            element.style.transition = 'all 0.5s ease';
            
            setTimeout(() => {
                element.style.opacity = '1';
                element.style.transform = 'translateY(0)';
            }, 100);
        });
    }, 100);
}

// Show empty account summary
function showEmptyAccountSummary() {
    const summaryContainer = document.getElementById('account-summary');
    if (!summaryContainer) return;
    
    summaryContainer.innerHTML = `
        <div class="col-12">
            <div class="empty-state-card">
                <div class="text-center">
                    <i class="fas fa-piggy-bank fa-3x text-muted mb-3"></i>
                    <h4>No Accounts Yet</h4>
                    <p class="text-muted">Get started by creating your first bank account.</p>
                    <a href="add-account.html" class="btn btn-primary">
                        <i class="fas fa-plus me-2"></i>Create Account
                    </a>
                </div>
            </div>
        </div>
    `;
}

// Load recent transactions (if API supports it)
async function loadRecentTransactions() {
    const transactionsContainer = document.getElementById('recent-transactions');
    if (!transactionsContainer) return;
    
    try {
        // This would need to be implemented based on the actual API
        // For now, we'll show a placeholder
        transactionsContainer.innerHTML = `
            <div class="text-center py-4">
                <i class="fas fa-clock text-muted mb-2"></i>
                <p class="text-muted">Recent transactions will appear here</p>
            </div>
        `;
    } catch (error) {
        console.error('Error loading recent transactions:', error);
    }
}

// Quick actions handlers
function initializeQuickActions() {
    // Add event listeners for quick action buttons
    const quickTransferBtn = document.querySelector('.quick-action-card a[href="transfer.html"]');
    const addAccountBtn = document.querySelector('.quick-action-card a[href="add-account.html"]');
    
    if (quickTransferBtn) {
        quickTransferBtn.addEventListener('click', function(e) {
            // Check if user has accounts for transfer
            if (userAccounts.length < 2) {
                e.preventDefault();
                showAlert('main-alert', 'You need at least 2 accounts to make a transfer. Please create another account first.', 'warning');
            }
        });
    }
}

// Dashboard metrics calculation
function calculateDashboardMetrics() {
    if (!userAccounts || userAccounts.length === 0) return null;
    
    const totalBalance = userAccounts.reduce((sum, account) => sum + parseFloat(account.balance || 0), 0);
    const accountTypes = [...new Set(userAccounts.map(account => account.accountType))];
    
    return {
        totalBalance,
        accountCount: userAccounts.length,
        accountTypes: accountTypes.length,
        averageBalance: totalBalance / userAccounts.length
    };
}

// Display dashboard metrics
function displayDashboardMetrics() {
    const metrics = calculateDashboardMetrics();
    if (!metrics) return;
    
    // Update any metric displays on the page
    const metricElements = {
        totalBalance: document.getElementById('total-balance'),
        accountCount: document.getElementById('account-count'),
        accountTypes: document.getElementById('account-types')
    };
    
    if (metricElements.totalBalance) {
        metricElements.totalBalance.textContent = formatCurrency(metrics.totalBalance);
    }
    
    if (metricElements.accountCount) {
        metricElements.accountCount.textContent = metrics.accountCount;
    }
    
    if (metricElements.accountTypes) {
        metricElements.accountTypes.textContent = metrics.accountTypes;
    }
}

// Refresh dashboard data
async function refreshDashboard() {
    await loadAccountSummary();
    await loadRecentTransactions();
    displayDashboardMetrics();
}

// Dashboard notifications
function checkDashboardNotifications() {
    const notifications = [];
    
    // Check for low balances
    userAccounts.forEach(account => {
        const balance = parseFloat(account.balance || 0);
        if (balance < 100) {
            notifications.push({
                type: 'warning',
                message: `Low balance in ${account.accountName}: ${formatCurrency(balance)}`
            });
        }
    });
    
    // Display notifications
    if (notifications.length > 0) {
        const notificationContainer = document.getElementById('dashboard-notifications');
        if (notificationContainer) {
            const notificationHtml = notifications.map(notif => `
                <div class="alert alert-${notif.type} alert-dismissible fade show" role="alert">
                    ${notif.message}
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            `).join('');
            
            notificationContainer.innerHTML = notificationHtml;
        }
    }
}

// Initialize dashboard
function initializeDashboard() {
    initializeQuickActions();
    
    // Set up refresh button if it exists
    const refreshBtn = document.getElementById('refresh-dashboard');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', refreshDashboard);
    }
    
    // Load dashboard data
    refreshDashboard().then(() => {
        checkDashboardNotifications();
    });
}

// Dashboard chart initialization (if needed)
function initializeDashboardCharts() {
    // This could be implemented with Chart.js for account balance charts
    // Placeholder for future chart implementations
}

// Export functions for use in other files
window.dashboardFunctions = {
    loadAccountSummary,
    refreshDashboard,
    initializeDashboard
};
