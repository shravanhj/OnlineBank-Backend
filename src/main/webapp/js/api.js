// API Configuration
const API_BASE_URL = 'http://localhost:8080/OnlineBank/api';

// API call wrapper with error handling
async function makeApiCall(endpoint, method = 'GET', data = null, headers = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    
    const config = {
        method,
        headers: {
            'Content-Type': 'application/json',
            ...headers
        }
    };
    
    if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
        config.body = JSON.stringify(data);
    }
    
    try {
        const response = await fetch(url, config);
        
        // Handle different response types
        let responseData;
        const contentType = response.headers.get('content-type');
        
        if (contentType && contentType.includes('application/json')) {
            responseData = await response.json();
        } else {
            responseData = await response.text();
        }
        
        if (!response.ok) {
            // Handle API errors
            const errorMessage = responseData.message || responseData.error || responseData || 'An error occurred';
            throw new Error(errorMessage);
        }
        
        return responseData;
    } catch (error) {
        // Handle network errors
        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            throw new Error('Network error. Please check your connection and try again.');
        }
        throw error;
    }
}

// Authentication API calls
const authAPI = {
    login: (phoneNumber, password) => {
        return makeApiCall('/auth/login', 'POST', { phoneNumber, password });
    },
    
    register: (userData) => {
        return makeApiCall('/auth/register', 'POST', userData);
    },
    
    logout: () => {
        return makeApiCall('/auth/logout', 'POST');
    },
    
    getCurrentUser: () => {
        return makeApiCall('/auth/user');
    }
};

// Account API calls
const accountAPI = {
    getUserAccounts: (userId) => {
        return makeApiCall(`/accounts/user/${userId}`);
    },
    
    getAllAccounts: () => {
        return makeApiCall('/accounts/all');
    },
    
    getAccountById: (accountId) => {
        return makeApiCall(`/accounts/${accountId}`);
    },
    
    createAccount: (accountData) => {
        return makeApiCall('/accounts/create', 'POST', accountData);
    },
    
    updateAccount: (accountId, accountData) => {
        return makeApiCall(`/accounts/${accountId}`, 'PUT', accountData);
    },
    
    deleteAccount: (accountId) => {
        return makeApiCall(`/accounts/${accountId}`, 'DELETE');
    }
};

// Transfer API calls
const transferAPI = {
    transfer: (transferData) => {
        return makeApiCall('/transfers/transfer', 'POST', transferData);
    },
    
    getTransferHistory: (accountId) => {
        return makeApiCall(`/transfers/history/${accountId}`);
    },
    
    getAllTransfers: () => {
        return makeApiCall('/transfers/all');
    },
    
    getTransferById: (transferId) => {
        return makeApiCall(`/transfers/${transferId}`);
    }
};

// User API calls
const userAPI = {
    getAllUsers: () => {
        return makeApiCall('/users/all');
    },
    
    getUserById: (userId) => {
        return makeApiCall(`/users/${userId}`);
    },
    
    updateUser: (userId, userData) => {
        return makeApiCall(`/users/${userId}`, 'PUT', userData);
    },
    
    deleteUser: (userId) => {
        return makeApiCall(`/users/${userId}`, 'DELETE');
    }
};

// Utility function to handle API errors globally
function handleApiError(error, alertId = null) {
    console.error('API Error:', error);
    
    let message = 'An unexpected error occurred. Please try again.';
    
    if (error.message) {
        message = error.message;
    }
    
    // Handle specific error cases
    if (error.message && error.message.includes('Network error')) {
        message = 'Unable to connect to the server. Please check your internet connection.';
    } else if (error.message && error.message.includes('401')) {
        message = 'Your session has expired. Please log in again.';
        // Redirect to login page
        logout();
        return;
    } else if (error.message && error.message.includes('403')) {
        message = 'You do not have permission to perform this action.';
    } else if (error.message && error.message.includes('404')) {
        message = 'The requested resource was not found.';
    } else if (error.message && error.message.includes('500')) {
        message = 'Server error. Please try again later.';
    }
    
    if (alertId) {
        showAlert(alertId, message, 'danger');
    }
    
    return message;
}

// Request interceptor to add authorization header if user is logged in
function addAuthHeader(headers = {}) {
    const user = getCurrentUser();
    if (user && user.token) {
        headers['Authorization'] = `Bearer ${user.token}`;
    }
    return headers;
}

// Enhanced API call with automatic auth header
async function makeAuthenticatedApiCall(endpoint, method = 'GET', data = null, headers = {}) {
    const authHeaders = addAuthHeader(headers);
    return makeApiCall(endpoint, method, data, authHeaders);
}

// API health check
async function checkApiHealth() {
    try {
        const response = await fetch(`${API_BASE_URL}/health`);
        return response.ok;
    } catch (error) {
        return false;
    }
}

// Retry API call with exponential backoff
async function retryApiCall(apiFunction, maxRetries = 3, initialDelay = 1000) {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            return await apiFunction();
        } catch (error) {
            if (attempt === maxRetries) {
                throw error;
            }
            
            // Exponential backoff delay
            const delay = initialDelay * Math.pow(2, attempt - 1);
            await new Promise(resolve => setTimeout(resolve, delay));
        }
    }
}

// Batch API calls
async function batchApiCalls(apiCalls) {
    try {
        const results = await Promise.allSettled(apiCalls);
        return results.map(result => {
            if (result.status === 'fulfilled') {
                return { success: true, data: result.value };
            } else {
                return { success: false, error: result.reason };
            }
        });
    } catch (error) {
        throw new Error('Batch API call failed');
    }
}
