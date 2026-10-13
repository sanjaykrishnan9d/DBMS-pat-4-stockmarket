/**
 * Dashboard Controller - Fetches live data from /api/dashboard and populates metrics & charts
 */

let pricesChartInstance = null;
let allocationChartInstance = null;

async function loadDashboard() {
  try {
    const user = await initSession();
    if (!user) return;

    const res = await apiRequest('/api/dashboard');
    if (!res || !res.success || !res.data) return;

    const d = res.data;

    // 1. Welcome Greeting
    const greetingEl = document.getElementById('welcomeGreeting');
    if (greetingEl) {
      greetingEl.textContent = `Welcome back, ${d.userName}. Real-time Oracle DB trading metrics.`;
    }

    // 2. Metric Cards
    document.getElementById('metricPortfolioValue').textContent = formatCurrency(d.currentPortfolioValue);
    document.getElementById('metricTotalInvested').textContent = formatCurrency(d.totalInvestment);
    
    const plEl = document.getElementById('metricProfitLoss');
    const plPercentEl = document.getElementById('metricProfitLossPercent');
    plEl.textContent = formatCurrency(d.profitLoss);
    
    if (d.profitLoss >= 0) {
      plEl.className = 'metric-value positive';
      plPercentEl.className = 'metric-sub positive';
      plPercentEl.innerHTML = `<span>▲ +${d.profitLossPercent}%</span>`;
    } else {
      plEl.className = 'metric-value negative';
      plPercentEl.className = 'metric-sub negative';
      plPercentEl.innerHTML = `<span>▼ ${d.profitLossPercent}%</span>`;
    }

    document.getElementById('metricCashBalance').textContent = formatCurrency(d.cashBalance);
    document.getElementById('metricAvailableStocks').textContent = formatNumber(d.availableStocksCount);
    document.getElementById('metricTransactionCount').textContent = formatNumber(d.transactionCount);

    // 3. Recent Transactions Table
    renderRecentTransactions(d.recentTransactions || []);

    // 4. Render Charts
    renderCharts(d.holdings || []);

  } catch (err) {
    console.error('Error loading dashboard data:', err);
    showToast('Failed to load dashboard data: ' + err.message, 'error');
  }
}

function renderRecentTransactions(txns) {
  const tbody = document.getElementById('recentTransactionsTableBody');
  if (!tbody) return;

  if (txns.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8" class="empty-state">
          <div class="empty-icon">📂</div>
          <div>No transactions recorded yet. Fund your account or execute a trade!</div>
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
        <td>${formatDate(t.transactionDate)}</td>
        <td><strong>${t.symbol}</strong> <span style="font-size:0.8rem; color:var(--text-muted);">(${t.company})</span></td>
        <td>${typeBadge}</td>
        <td>${formatNumber(t.quantity)}</td>
        <td>${formatCurrency(t.pricePerShare)}</td>
        <td style="font-weight: 600;">${formatCurrency(t.totalAmount)}</td>
        <td><span class="badge badge-success">${t.status}</span></td>
      </tr>
    `;
  }).join('');
}

async function renderCharts(holdings) {
  // 1. Fetch available stocks to display market prices bar chart
  try {
    const stockRes = await apiRequest('/api/stocks');
    if (stockRes && stockRes.success && stockRes.data && stockRes.data.stocks) {
      const stocks = stockRes.data.stocks.slice(0, 8); // Top 8 active stocks
      const labels = stocks.map(s => s.symbol);
      const prices = stocks.map(s => s.currentPrice);

      const ctx1 = document.getElementById('marketPricesChart');
      if (ctx1 && window.Chart) {
        if (pricesChartInstance) pricesChartInstance.destroy();
        pricesChartInstance = new Chart(ctx1, {
          type: 'bar',
          data: {
            labels: labels,
            datasets: [{
              label: 'Share Price (USD)',
              data: prices,
              backgroundColor: 'rgba(6, 182, 212, 0.75)',
              borderColor: '#06b6d4',
              borderWidth: 1,
              borderRadius: 6
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { display: false }
            },
            scales: {
              x: { grid: { color: 'rgba(255, 255, 255, 0.05)' }, ticks: { color: '#94a3b8' } },
              y: { grid: { color: 'rgba(255, 255, 255, 0.05)' }, ticks: { color: '#94a3b8' } }
            }
          }
        });
      }
    }
  } catch (e) {
    console.warn('Could not render market prices chart:', e);
  }

  // 2. Portfolio Asset Allocation Doughnut Chart
  const ctx2 = document.getElementById('portfolioAllocationChart');
  if (ctx2 && window.Chart) {
    if (allocationChartInstance) allocationChartInstance.destroy();

    const hasHoldings = holdings && holdings.length > 0;
    const allocLabels = hasHoldings ? holdings.map(h => h.symbol) : ['Unallocated / Cash'];
    const allocData = hasHoldings ? holdings.map(h => h.currentValue) : [100];
    const allocColors = [
      '#06b6d4', '#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#64748b'
    ];

    allocationChartInstance = new Chart(ctx2, {
      type: 'doughnut',
      data: {
        labels: allocLabels,
        datasets: [{
          data: allocData,
          backgroundColor: allocColors,
          borderWidth: 2,
          borderColor: '#111827'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: '#94a3b8', boxWidth: 12 }
          }
        }
      }
    });
  }
}

// Hook for refresh when transactions or deposits complete
window.onAccountUpdated = () => {
  loadDashboard();
};

document.addEventListener('DOMContentLoaded', () => {
  loadDashboard();
});
