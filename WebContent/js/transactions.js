/**
 * Order History & Transaction Audit Controller
 */

let transactionsList = [];

async function loadTransactions() {
  const user = await initSession();
  if (!user) return;

  const symbol = document.getElementById('symbolFilterInput').value.trim();
  const type = document.getElementById('typeFilterSelect').value;

  try {
    const params = new URLSearchParams();
    if (symbol) params.append('symbol', symbol);
    if (type && type !== 'ALL') params.append('type', type);
    params.append('limit', '100');

    const res = await apiRequest('/api/transactions/history?' + params.toString());
    if (!res || !res.success || !res.data) return;

    transactionsList = res.data.transactions || [];
    renderTransactionsTable(transactionsList);

    const badge = document.getElementById('txnCountBadge');
    if (badge) {
      badge.textContent = `${transactionsList.length} of ${res.data.totalCount || transactionsList.length} Orders`;
    }

  } catch (err) {
    console.error('Error fetching transactions:', err);
    showToast('Failed to load transaction history: ' + err.message, 'error');
  }
}

function renderTransactionsTable(txns) {
  const tbody = document.getElementById('transactionsHistoryTableBody');
  if (!tbody) return;

  if (txns.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="9" class="empty-state">
          <div class="empty-icon">📜</div>
          <div style="font-size: 1rem; font-weight: 600; margin-bottom: 6px;">No transactions recorded</div>
          <div style="margin-bottom: 16px;">Try adjusting your filters or execute your first trade.</div>
          <a href="stocks.html" class="btn btn-primary btn-sm">Explore Equities →</a>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = txns.map(t => {
    let typeBadge = '';
    if (t.transactionType === 'BUY') {
      typeBadge = '<span class="badge badge-info">BUY</span>';
    } else if (t.transactionType === 'SELL') {
      typeBadge = '<span class="badge badge-success">SELL</span>';
    } else {
      typeBadge = '<span class="badge badge-warning">DEPOSIT</span>';
    }

    return `
      <tr>
        <td><strong>#${t.transactionId}</strong></td>
        <td style="color: var(--text-muted);">${formatDate(t.transactionDate)}</td>
        <td>
          <strong>${t.symbol}</strong>
          <span style="font-size: 0.8rem; color: var(--text-dim);"> • ${t.company}</span>
        </td>
        <td>${typeBadge}</td>
        <td style="font-weight: 600;">${formatNumber(t.quantity)}</td>
        <td>${formatCurrency(t.pricePerShare)}</td>
        <td style="font-weight: 700; font-size: 1.05rem;">${formatCurrency(t.totalAmount)}</td>
        <td><span class="badge badge-success">${t.status}</span></td>
        <td style="text-align: right;">
          <button class="btn btn-outline btn-sm" onclick="viewReceipt(${t.transactionId})">
            📄 Voucher
          </button>
        </td>
      </tr>
    `;
  }).join('');
}

function viewReceipt(transactionId) {
  const txn = transactionsList.find(t => t.transactionId === transactionId);
  if (!txn) return;
  showReceiptModal(txn);
}

window.onAccountUpdated = () => {
  loadTransactions();
};

document.addEventListener('DOMContentLoaded', () => {
  loadTransactions();

  const symInput = document.getElementById('symbolFilterInput');
  if (symInput) {
    let debounce;
    symInput.addEventListener('input', () => {
      clearTimeout(debounce);
      debounce = setTimeout(loadTransactions, 300);
    });
  }

  const typeSelect = document.getElementById('typeFilterSelect');
  if (typeSelect) {
    typeSelect.addEventListener('change', loadTransactions);
  }

  const refreshBtn = document.getElementById('btnRefreshHistory');
  if (refreshBtn) {
    refreshBtn.addEventListener('click', loadTransactions);
  }
});
