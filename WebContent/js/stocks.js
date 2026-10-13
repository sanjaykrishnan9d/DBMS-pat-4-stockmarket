/**
 * Market Watch & Stock Directory Controller
 */

let allStocks = [];
let currentUser = null;

async function loadStocks() {
  currentUser = await initSession();
  if (!currentUser) return;

  const urlParams = new URLSearchParams(window.location.search);
  const isAdminTab = urlParams.get('tab') === 'admin' && currentUser.role === 'ADMIN';

  // Toggle admin UI controls
  const adminBtn = document.getElementById('btnAdminAddStock');
  if (adminBtn && currentUser.role === 'ADMIN') {
    adminBtn.style.display = 'inline-flex';
  }

  const query = document.getElementById('searchInput').value.trim();
  const sector = document.getElementById('sectorFilter').value;

  try {
    let endpoint = '/api/stocks';
    if (isAdminTab) {
      endpoint = '/api/admin/stocks';
    } else {
      const params = new URLSearchParams();
      if (query) params.append('search', query);
      if (sector && sector !== 'ALL') params.append('sector', sector);
      if (params.toString()) endpoint += '?' + params.toString();
    }

    const res = await apiRequest(endpoint);
    if (!res || !res.success) return;

    if (isAdminTab) {
      allStocks = res.data || [];
    } else {
      allStocks = res.data.stocks || [];
      populateSectors(res.data.sectors || []);
    }

    renderStocksTable(allStocks, isAdminTab);
    document.getElementById('stockCountBadge').textContent = `${allStocks.length} Stocks`;

  } catch (err) {
    console.error('Error fetching stocks:', err);
    showToast('Failed to load stocks: ' + err.message, 'error');
  }
}

function populateSectors(sectors) {
  const select = document.getElementById('sectorFilter');
  if (!select || select.children.length > 1) return; // already populated

  sectors.forEach(sec => {
    const opt = document.createElement('option');
    opt.value = sec;
    opt.textContent = sec;
    select.appendChild(opt);
  });
}

function renderStocksTable(stocks, isAdminView = false) {
  const tbody = document.getElementById('stocksTableBody');
  if (!tbody) return;

  if (stocks.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="7" class="empty-state">
          <div class="empty-icon">🔍</div>
          <div>No stocks found matching your search criteria.</div>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = stocks.map(s => {
    const isInactive = s.activeStatus === 'INACTIVE';
    const statusBadge = isInactive 
      ? '<span class="badge badge-danger">INACTIVE</span>' 
      : '<span class="badge badge-success">ACTIVE</span>';

    let actionButtons = '';
    if (isAdminView || (currentUser && currentUser.role === 'ADMIN')) {
      actionButtons += `
        <button class="btn btn-outline btn-sm" onclick="openAdminEditModal(${s.stockId})">✏️ Edit</button>
        <button class="btn btn-outline btn-sm" onclick="toggleStockStatus(${s.stockId}, '${s.activeStatus}')">
          ${isInactive ? 'Activate' : 'Deactivate'}
        </button>
      `;
    }

    actionButtons += `
      <button class="btn btn-outline btn-sm" onclick="viewStockDetails(${s.stockId})">ℹ️ Details</button>
      <button class="btn btn-primary btn-sm" ${isInactive ? 'disabled title="Trading suspended"' : ''} onclick="triggerBuy(${s.stockId})">
        Trade
      </button>
    `;

    return `
      <tr style="${isInactive ? 'opacity: 0.65;' : ''}">
        <td><strong>#${s.stockId}</strong></td>
        <td><strong style="color: var(--accent-cyan); font-size: 1.05rem;">${s.symbol}</strong></td>
        <td>
          <div style="font-weight: 600;">${s.company}</div>
          <div style="font-size: 0.8rem; color: var(--text-muted);">${s.stockName}</div>
        </td>
        <td><span class="badge badge-info">${s.sector}</span></td>
        <td style="font-weight: 700; font-size: 1.05rem;">${formatCurrency(s.currentPrice)}</td>
        <td>${statusBadge}</td>
        <td style="text-align: right; white-space: nowrap;">
          <div style="display: inline-flex; gap: 6px;">
            ${actionButtons}
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function viewStockDetails(stockId) {
  const stock = allStocks.find(s => s.stockId === stockId);
  if (!stock) return;

  document.getElementById('detailSymbol').textContent = stock.symbol;
  document.getElementById('detailStockName').textContent = stock.stockName;
  document.getElementById('detailCompany').textContent = stock.company;
  document.getElementById('detailSector').textContent = stock.sector;
  document.getElementById('detailPrice').textContent = formatCurrency(stock.currentPrice);
  document.getElementById('detailStatus').textContent = stock.activeStatus;
  document.getElementById('detailUpdatedAt').textContent = formatDate(stock.updatedAt);

  const tradeBtn = document.getElementById('btnDetailTrade');
  tradeBtn.onclick = () => {
    closeModal('detailsModal');
    openTradeModal(stock, 'BUY');
  };

  openModal('detailsModal');
}

function triggerBuy(stockId) {
  const stock = allStocks.find(s => s.stockId === stockId);
  if (stock) {
    openTradeModal(stock, 'BUY');
  }
}

// ----------------------------------------------------------------------------
// ADMIN STOCK OPERATIONS
// ----------------------------------------------------------------------------
function openAdminCreateModal() {
  document.getElementById('adminStockModalTitle').textContent = 'Add New Stock (Administrator)';
  document.getElementById('adminStockAction').value = 'create';
  document.getElementById('adminStockId').value = '';
  document.getElementById('adminSymbol').readOnly = false;
  document.getElementById('adminStockForm').reset();
  document.getElementById('adminStatusGroup').style.display = 'none';
  openModal('adminStockModal');
}

function openAdminEditModal(stockId) {
  const stock = allStocks.find(s => s.stockId === stockId);
  if (!stock) return;

  document.getElementById('adminStockModalTitle').textContent = `Edit Stock #${stock.stockId} (${stock.symbol})`;
  document.getElementById('adminStockAction').value = 'update';
  document.getElementById('adminStockId').value = stock.stockId;
  document.getElementById('adminSymbol').value = stock.symbol;
  document.getElementById('adminSymbol').readOnly = true;
  document.getElementById('adminPrice').value = stock.currentPrice;
  document.getElementById('adminStockName').value = stock.stockName;
  document.getElementById('adminCompany').value = stock.company;
  document.getElementById('adminSector').value = stock.sector;
  document.getElementById('adminActiveStatus').value = stock.activeStatus;
  document.getElementById('adminStatusGroup').style.display = 'block';

  openModal('adminStockModal');
}

async function toggleStockStatus(stockId, currentStatus) {
  const newStatus = (currentStatus === 'ACTIVE') ? 'INACTIVE' : 'ACTIVE';
  if (!confirm(`Are you sure you want to change status to ${newStatus}?`)) return;

  try {
    const res = await apiRequest('/api/admin/stocks', {
      method: 'POST',
      body: {
        action: 'toggleStatus',
        stockId: stockId,
        status: newStatus
      }
    });
    showToast(res.message || 'Status updated successfully!');
    loadStocks();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function setupAdminForm() {
  const form = document.getElementById('adminStockForm');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const action = document.getElementById('adminStockAction').value;
    const stockId = document.getElementById('adminStockId').value;
    const symbol = document.getElementById('adminSymbol').value.trim();
    const currentPrice = document.getElementById('adminPrice').value;
    const stockName = document.getElementById('adminStockName').value.trim();
    const company = document.getElementById('adminCompany').value.trim();
    const sector = document.getElementById('adminSector').value.trim();
    const activeStatus = document.getElementById('adminActiveStatus').value;

    const payload = {
      action,
      stockId,
      symbol,
      currentPrice,
      stockName,
      company,
      sector,
      activeStatus
    };

    try {
      const res = await apiRequest('/api/admin/stocks', {
        method: 'POST',
        body: payload
      });

      closeModal('adminStockModal');
      showToast(res.message || 'Stock successfully saved to database!');
      loadStocks();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });
}

window.onAccountUpdated = () => {
  loadStocks();
};

document.addEventListener('DOMContentLoaded', () => {
  loadStocks();
  setupAdminForm();

  const searchInput = document.getElementById('searchInput');
  if (searchInput) {
    let debounce;
    searchInput.addEventListener('input', () => {
      clearTimeout(debounce);
      debounce = setTimeout(loadStocks, 300);
    });
  }

  const sectorFilter = document.getElementById('sectorFilter');
  if (sectorFilter) {
    sectorFilter.addEventListener('change', loadStocks);
  }

  const refreshBtn = document.getElementById('btnRefreshStocks');
  if (refreshBtn) {
    refreshBtn.addEventListener('click', loadStocks);
  }
});
