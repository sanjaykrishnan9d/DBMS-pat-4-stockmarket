/**
 * ============================================================================
 * Stock Market Management System - Core Frontend App Helpers
 * Handles API fetch requests, session management, modals, and formatting.
 * ============================================================================
 */

// Dynamically determine the application context path under Apache Tomcat
const CONTEXT_PATH = (function() {
  const path = window.location.pathname;
  const match = path.match(/^(\/[^\/]+)/);
  // If running directly under root context
  if (!match || match[1] === '/WebContent' || path.endsWith('.html') && match[1].includes('.html')) {
    return '';
  }
  return match[1];
})();

/**
 * Universal API fetch wrapper with JSON handling and session check
 */
async function apiRequest(endpoint, options = {}) {
  const url = `${CONTEXT_PATH}${endpoint}`;
  const defaultHeaders = {
    'Accept': 'application/json'
  };

  if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
    defaultHeaders['Content-Type'] = 'application/json';
    options.body = JSON.stringify(options.body);
  }

  options.headers = { ...defaultHeaders, ...options.headers };

  try {
    const res = await fetch(url, options);
    const data = await res.json().catch(() => null);

    if (res.status === 401) {
      // Unauthenticated, redirect to login
      if (!window.location.pathname.endsWith('login.html') && !window.location.pathname.endsWith('register.html') && !window.location.pathname.endsWith('index.html')) {
        window.location.href = `${CONTEXT_PATH}/login.html`;
      }
      throw new Error(data && data.message ? data.message : 'Please log in to continue.');
    }

    if (!res.ok) {
      throw new Error((data && data.message) ? data.message : `Server error (${res.status})`);
    }

    return data;
  } catch (err) {
    console.error(`API request error on ${endpoint}:`, err);
    throw err;
  }
}

/**
 * Validates current session and updates header/sidebar user profile
 */
async function initSession() {
  try {
    const res = await apiRequest('/api/auth/me');
    if (res && res.success && res.data) {
      const user = res.data;
      updateUserProfileUI(user);
      return user;
    }
  } catch (err) {
    // If on a protected page, redirect
    const path = window.location.pathname;
    if (!path.endsWith('login.html') && !path.endsWith('register.html') && !path.endsWith('index.html') && path !== '/') {
      window.location.href = `${CONTEXT_PATH}/login.html`;
    }
  }
  return null;
}

function updateUserProfileUI(user) {
  const nameEl = document.getElementById('sidebarUserName');
  const roleEl = document.getElementById('sidebarUserRole');
  const avatarEl = document.getElementById('sidebarUserAvatar');
  const balanceEl = document.getElementById('headerCashBalance');
  const adminNavLink = document.getElementById('adminNavLink');

  if (nameEl) nameEl.textContent = user.fullName;
  if (roleEl) roleEl.textContent = user.role;
  if (avatarEl) {
    const initials = user.fullName.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
    avatarEl.textContent = initials || 'U';
  }
  if (balanceEl) {
    balanceEl.textContent = formatCurrency(user.cashBalance);
  }
  if (adminNavLink) {
    adminNavLink.style.display = (user.role === 'ADMIN') ? 'flex' : 'none';
  }
}

async function handleLogout() {
  if (!confirm('Are you sure you want to log out of your trading account?')) return;
  try {
    await apiRequest('/api/auth/logout', { method: 'POST' });
  } catch (e) {
    // Continue redirect
  }
  window.location.href = `${CONTEXT_PATH}/login.html`;
}

// ----------------------------------------------------------------------------
// FORMATTING HELPERS
// ----------------------------------------------------------------------------
function formatCurrency(val) {
  if (val === undefined || val === null || isNaN(val)) return '$0.00';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(val);
}

function formatNumber(val) {
  if (val === undefined || val === null || isNaN(val)) return '0';
  return new Intl.NumberFormat('en-US').format(val);
}

function formatDate(timestamp) {
  if (!timestamp) return '-';
  const d = new Date(timestamp);
  return d.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

// ----------------------------------------------------------------------------
// TOAST NOTIFICATIONS
// ----------------------------------------------------------------------------
function showToast(message, type = 'success') {
  let toast = document.getElementById('globalToast');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'globalToast';
    toast.className = 'toast';
    document.body.appendChild(toast);
  }

  toast.className = `toast toast-${type} show`;
  toast.innerHTML = `
    <span style="font-size: 1.2rem;">${type === 'success' ? '✓' : '⚠'}</span>
    <span>${message}</span>
  `;

  setTimeout(() => {
    toast.classList.remove('show');
  }, 4000);
}

// ----------------------------------------------------------------------------
// MODAL CONTROLLERS
// ----------------------------------------------------------------------------
function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.add('show');
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.remove('show');
}

// ----------------------------------------------------------------------------
// GLOBAL SIMULATED DEPOSIT (ADD FUNDS) MODAL
// ----------------------------------------------------------------------------
function setupDepositModal() {
  const btn = document.getElementById('btnAddFunds');
  const modal = document.getElementById('depositModal');
  const form = document.getElementById('depositForm');

  if (btn) {
    btn.addEventListener('click', () => {
      if (form) form.reset();
      openModal('depositModal');
    });
  }

  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const amountInput = document.getElementById('depositAmount');
      const amount = parseFloat(amountInput.value);

      if (!amount || amount <= 0) {
        showToast('Please enter a valid deposit amount greater than $0', 'error');
        return;
      }

      try {
        const res = await apiRequest('/api/account/deposit', {
          method: 'POST',
          body: { amount }
        });
        showToast(res.message || 'Simulated funds deposited successfully!');
        closeModal('depositModal');
        // Refresh balance UI
        initSession();
        // Trigger page refresh if custom hook defined
        if (window.onAccountUpdated) window.onAccountUpdated();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }
}

// ----------------------------------------------------------------------------
// GLOBAL QUICK TRADE MODAL (BUY / SELL)
// ----------------------------------------------------------------------------
let currentTradeStock = null;

function openTradeModal(stock, defaultType = 'BUY') {
  currentTradeStock = stock;
  const modal = document.getElementById('tradeModal');
  if (!modal) return;

  document.getElementById('tradeStockSymbol').textContent = stock.symbol;
  document.getElementById('tradeStockCompany').textContent = stock.company;
  document.getElementById('tradeStockPrice').textContent = formatCurrency(stock.currentPrice);
  document.getElementById('tradeStockId').value = stock.stockId;
  document.getElementById('tradeType').value = defaultType;
  
  const qtyInput = document.getElementById('tradeQuantity');
  qtyInput.value = 1;
  updateTradeCalculation();

  openModal('tradeModal');
}

function updateTradeCalculation() {
  if (!currentTradeStock) return;
  const qty = parseInt(document.getElementById('tradeQuantity').value) || 0;
  const total = qty * currentTradeStock.currentPrice;
  document.getElementById('tradeEstimatedTotal').textContent = formatCurrency(total);
}

function setupTradeModal() {
  const form = document.getElementById('tradeForm');
  const qtyInput = document.getElementById('tradeQuantity');

  if (qtyInput) {
    qtyInput.addEventListener('input', updateTradeCalculation);
  }

  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const stockId = document.getElementById('tradeStockId').value;
      const type = document.getElementById('tradeType').value;
      const quantity = parseInt(document.getElementById('tradeQuantity').value);

      if (!quantity || quantity <= 0) {
        showToast('Please enter a valid quantity of shares', 'error');
        return;
      }

      const submitBtn = form.querySelector('button[type="submit"]');
      if (submitBtn) submitBtn.disabled = true;

      try {
        const res = await apiRequest('/api/transactions', {
          method: 'POST',
          body: { stockId, type, quantity }
        });

        closeModal('tradeModal');
        showToast(res.message || 'Order completed successfully!');
        
        // Show receipt modal
        if (res.data) {
          showReceiptModal(res.data);
        }

        // Refresh live data
        initSession();
        if (window.onAccountUpdated) window.onAccountUpdated();
      } catch (err) {
        showToast(err.message, 'error');
      } finally {
        if (submitBtn) submitBtn.disabled = false;
      }
    });
  }
}

function showReceiptModal(receipt) {
  const modal = document.getElementById('receiptModal');
  if (!modal) return;

  document.getElementById('receiptTxnId').textContent = '#' + receipt.transactionId;
  document.getElementById('receiptDate').textContent = formatDate(receipt.transactionDate);
  document.getElementById('receiptSymbol').textContent = receipt.symbol;
  document.getElementById('receiptType').textContent = receipt.transactionType;
  document.getElementById('receiptQty').textContent = formatNumber(receipt.quantity);
  document.getElementById('receiptPrice').textContent = formatCurrency(receipt.pricePerShare);
  document.getElementById('receiptTotal').textContent = formatCurrency(receipt.totalAmount);
  document.getElementById('receiptStatus').textContent = receipt.status;

  const typeEl = document.getElementById('receiptType');
  if (receipt.transactionType === 'BUY') {
    typeEl.className = 'badge badge-info';
  } else if (receipt.transactionType === 'SELL') {
    typeEl.className = 'badge badge-success';
  }

  openModal('receiptModal');
}

// Global page initializer
document.addEventListener('DOMContentLoaded', () => {
  const logoutBtn = document.getElementById('btnLogout');
  if (logoutBtn) logoutBtn.addEventListener('click', handleLogout);

  setupDepositModal();
  setupTradeModal();
});
