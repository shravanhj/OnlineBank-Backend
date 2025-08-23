// Common utility functions

// Show alert message
function showAlert(containerId, message, type = 'info') {
    const container = document.getElementById(containerId);
    if (!container) return;
    
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    container.innerHTML = '';
    container.appendChild(alertDiv);
    
    // Auto-hide success messages after 5 seconds
    if (type === 'success') {
        setTimeout(() => {
            if (alertDiv.parentNode) {
                alertDiv.remove();
            }
        }, 5000);
    }
}

// Format currency
function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(amount);
}

// Format date
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

// Format account number for display
function formatAccountNumber(accountNumber) {
    if (!accountNumber) return '';
    const str = accountNumber.toString();
    return str.length > 4 ? `****${str.slice(-4)}` : str;
}

// Validate form data
function validateForm(formData, rules) {
    const errors = [];
    
    for (const [field, rule] of Object.entries(rules)) {
        const value = formData.get(field);
        
        if (rule.required && (!value || value.trim() === '')) {
            errors.push(`${rule.label || field} is required`);
            continue;
        }
        
        if (value && rule.minLength && value.length < rule.minLength) {
            errors.push(`${rule.label || field} must be at least ${rule.minLength} characters`);
        }
        
        if (value && rule.maxLength && value.length > rule.maxLength) {
            errors.push(`${rule.label || field} must be less than ${rule.maxLength} characters`);
        }
        
        if (value && rule.pattern && !rule.pattern.test(value)) {
            errors.push(rule.message || `${rule.label || field} format is invalid`);
        }
        
        if (value && rule.min && parseFloat(value) < rule.min) {
            errors.push(`${rule.label || field} must be at least ${rule.min}`);
        }
        
        if (value && rule.max && parseFloat(value) > rule.max) {
            errors.push(`${rule.label || field} must be less than ${rule.max}`);
        }
    }
    
    return errors;
}

// Debounce function for search/input delays
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Show loading state
function showLoading(elementId, message = 'Loading...') {
    const element = document.getElementById(elementId);
    if (!element) return;
    
    element.innerHTML = `
        <div class="text-center py-5">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <p class="mt-2">${message}</p>
        </div>
    `;
}

// Show empty state
function showEmptyState(elementId, title, message, actionText = null, actionLink = null) {
    const element = document.getElementById(elementId);
    if (!element) return;
    
    const actionButton = actionText && actionLink ? 
        `<a href="${actionLink}" class="btn btn-primary">${actionText}</a>` : '';
    
    element.innerHTML = `
        <div class="text-center py-5">
            <i class="fas fa-inbox fa-3x text-muted mb-3"></i>
            <h4>${title}</h4>
            <p class="text-muted">${message}</p>
            ${actionButton}
        </div>
    `;
}

// Smooth scroll to element
function scrollToElement(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.scrollIntoView({ behavior: 'smooth' });
    }
}

// Copy to clipboard
function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        // Could show a small toast notification here
        console.log('Copied to clipboard');
    }).catch(err => {
        console.error('Failed to copy: ', err);
    });
}

// Generate random ID
function generateId() {
    return Math.random().toString(36).substr(2, 9);
}

// Check if mobile device
function isMobile() {
    return window.innerWidth <= 768;
}

// Animate number counting
function animateNumber(element, start, end, duration = 1000) {
    const range = end - start;
    const increment = range / (duration / 16);
    let current = start;
    
    const timer = setInterval(() => {
        current += increment;
        if ((increment > 0 && current >= end) || (increment < 0 && current <= end)) {
            current = end;
            clearInterval(timer);
        }
        element.textContent = Math.round(current);
    }, 16);
}

// Local storage helpers
function setLocalStorageItem(key, value) {
    try {
        localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
        console.error('Error saving to localStorage:', error);
    }
}

function getLocalStorageItem(key) {
    try {
        const item = localStorage.getItem(key);
        return item ? JSON.parse(item) : null;
    } catch (error) {
        console.error('Error reading from localStorage:', error);
        return null;
    }
}

function removeLocalStorageItem(key) {
    try {
        localStorage.removeItem(key);
    } catch (error) {
        console.error('Error removing from localStorage:', error);
    }
}

// Session storage helpers
function setSessionStorageItem(key, value) {
    try {
        sessionStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
        console.error('Error saving to sessionStorage:', error);
    }
}

function getSessionStorageItem(key) {
    try {
        const item = sessionStorage.getItem(key);
        return item ? JSON.parse(item) : null;
    } catch (error) {
        console.error('Error reading from sessionStorage:', error);
        return null;
    }
}

function removeSessionStorageItem(key) {
    try {
        sessionStorage.removeItem(key);
    } catch (error) {
        console.error('Error removing from sessionStorage:', error);
    }
}
