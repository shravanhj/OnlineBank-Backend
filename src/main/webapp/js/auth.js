// Authentication related functions

// Global current user variable
let currentUser = null;

// Check authentication status
function checkAuthStatus() {
    const userData = getSessionStorageItem('userData');
    if (userData) {
        currentUser = userData;
        return true;
    } else {
        currentUser = null;
        return false;
    }
}

// Check authentication and redirect if not logged in
function checkAuthAndRedirect() {
    if (!checkAuthStatus()) {
        window.location.href = 'login.html';
        return false;
    }
    return true;
}

// Get current user
function getCurrentUser() {
    if (!currentUser) {
        checkAuthStatus();
    }
    return currentUser;
}

// Set current user
function setCurrentUser(userData) {
    currentUser = userData;
    setSessionStorageItem('userData', userData);
    updateNavbarForLoggedInUser();
}

// Clear current user
function clearCurrentUser() {
    currentUser = null;
    removeSessionStorageItem('userData');
    updateNavbarForLoggedOutUser();
}

// Handle login form submission
async function handleLogin(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const phoneNumber = formData.get('phoneNumber');
    const password = formData.get('password');
    
    // Validate form
    const errors = validateForm(formData, {
        phoneNumber: {
            required: true,
            label: 'Phone Number'
        },
        password: {
            required: true,
            label: 'Password'
        }
    });
    
    if (errors.length > 0) {
        showAlert('login-alert', errors.join('<br>'), 'danger');
        return;
    }
    
    const submitBtn = event.target.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Logging in...';
    
    try {
        const response = await authAPI.login(phoneNumber, password);
        
        // Store user data
        setCurrentUser(response);
        
        // Show success message
        showAlert('login-alert', 'Login successful! Redirecting...', 'success');
        
        // Redirect to dashboard
        setTimeout(() => {
            window.location.href = 'dashboard.html';
        }, 1500);
        
    } catch (error) {
        handleApiError(error, 'login-alert');
        submitBtn.disabled = false;
        submitBtn.innerHTML = 'Login';
    }
}

// Handle registration form submission
async function handleRegister(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const password = formData.get('password');
    const confirmPassword = formData.get('confirmPassword');
    
    // Validate form
    const errors = validateForm(formData, {
        name: {
            required: true,
            minLength: 2,
            label: 'Full Name'
        },
        phoneNumber: {
            required: true,
            pattern: /^[\d\-\+\(\)\s]+$/,
            message: 'Please enter a valid phone number',
            label: 'Phone Number'
        },
        password: {
            required: true,
            label: 'Password'
        },
        confirmPassword: {
            required: true,
            label: 'Confirm Password'
        }
    });
    
    // Check password match
    if (password !== confirmPassword) {
        errors.push('Passwords do not match');
    }
    
    if (errors.length > 0) {
        showAlert('register-alert', errors.join('<br>'), 'danger');
        return;
    }
    
    const submitBtn = event.target.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Creating Account...';
    
    try {
        const userData = {
            userName: formData.get('name'),
            phoneNumber: formData.get('phoneNumber'),
            password: password
        };
        
        const response = await authAPI.register(userData);
        
        // Show success message
        showAlert('register-alert', 'Account created successfully! Please log in.', 'success');
        
        // Redirect to login page
        setTimeout(() => {
            window.location.href = 'login.html';
        }, 2000);
        
    } catch (error) {
        handleApiError(error, 'register-alert');
        submitBtn.disabled = false;
        submitBtn.innerHTML = 'Register';
    }
}

// Handle logout
async function logout() {
    try {
        // Call logout API if available
        if (currentUser) {
            await authAPI.logout();
        }
    } catch (error) {
        console.error('Logout API error:', error);
        // Continue with logout even if API call fails
    }
    
    // Clear user data
    clearCurrentUser();
    
    // Redirect to home page
    window.location.href = 'index.html';
}

// Update navbar for logged in user
function updateNavbarForLoggedInUser() {
    const authButtons = document.getElementById('auth-buttons');
    const userMenu = document.getElementById('user-menu');
    const userNameSpan = document.getElementById('user-name');
    
    if (authButtons) authButtons.classList.add('d-none');
    if (userMenu) userMenu.classList.remove('d-none');
    if (userNameSpan && currentUser) {
        userNameSpan.textContent = currentUser.userName;
    }
    
    // Update navbar brand for authenticated users
    const navbarBrand = document.querySelector('.navbar-brand');
    if (navbarBrand && window.location.pathname !== '/index.html') {
        navbarBrand.textContent = 'Dashboard';
        navbarBrand.href = 'dashboard.html';
    }
}

// Update navbar for logged out user
function updateNavbarForLoggedOutUser() {
    const authButtons = document.getElementById('auth-buttons');
    const userMenu = document.getElementById('user-menu');
    
    if (authButtons) authButtons.classList.remove('d-none');
    if (userMenu) userMenu.classList.add('d-none');
    
    // Reset navbar brand
    const navbarBrand = document.querySelector('.navbar-brand');
    if (navbarBrand) {
        navbarBrand.textContent = 'Online Bank';
        navbarBrand.href = 'index.html';
    }
}

// Session timeout handling
function startSessionTimeout() {
    const TIMEOUT_DURATION = 30 * 60 * 1000; // 30 minutes
    let timeoutId;
    
    function resetTimeout() {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => {
            showAlert('main-alert', 'Your session has expired. Please log in again.', 'warning');
            setTimeout(logout, 3000);
        }, TIMEOUT_DURATION);
    }
    
    // Reset timeout on user activity
    ['click', 'keypress', 'scroll', 'mousemove'].forEach(event => {
        document.addEventListener(event, resetTimeout, true);
    });
    
    resetTimeout();
}

// Check if user should remain logged in (remember me functionality)
function checkRememberMe() {
    const rememberData = getLocalStorageItem('rememberUser');
    if (rememberData && rememberData.expiry > Date.now()) {
        setCurrentUser(rememberData.userData);
        return true;
    } else {
        removeLocalStorageItem('rememberUser');
        return false;
    }
}

// Set remember me data
function setRememberMe(userData, days = 7) {
    const expiry = Date.now() + (days * 24 * 60 * 60 * 1000);
    setLocalStorageItem('rememberUser', {
        userData,
        expiry
    });
}

// Password strength checker
function checkPasswordStrength(password) {
    const strength = {
        score: 0,
        suggestions: []
    };
    
    if (password.length >= 8) strength.score++;
    else strength.suggestions.push('Use at least 8 characters');
    
    if (/[a-z]/.test(password)) strength.score++;
    else strength.suggestions.push('Include lowercase letters');
    
    if (/[A-Z]/.test(password)) strength.score++;
    else strength.suggestions.push('Include uppercase letters');
    
    if (/\d/.test(password)) strength.score++;
    else strength.suggestions.push('Include numbers');
    
    if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) strength.score++;
    else strength.suggestions.push('Include special characters');
    
    // Determine strength level
    if (strength.score < 2) strength.level = 'weak';
    else if (strength.score < 4) strength.level = 'medium';
    else strength.level = 'strong';
    
    return strength;
}

// Initialize authentication on page load
document.addEventListener('DOMContentLoaded', function() {
    // Check authentication status and update navbar
    if (checkAuthStatus() || checkRememberMe()) {
        updateNavbarForLoggedInUser();
        
        // Start session timeout for authenticated users
        if (currentUser) {
            startSessionTimeout();
        }
    } else {
        updateNavbarForLoggedOutUser();
    }
});
