/**
 * Financial Reports Controller
 */

let reportAssetChartInstance = null;
let reportCashFlowChartInstance = null;

async function loadReports() {
  const user = await initSession();
  if (!user) return;

  try {
    const res = await apiRequest('/api/reports');
    if (!res || !res.success || !res.data) return;

    const d = res.data;

    // 1. Metrics Cards
    document.getElementById('repPurchases').textContent = formatCurrency(d.totalPurchases);
    document.getElementById('repBuyCount').textContent = `${d.buyCount || 0} Buy Orders`;

    document.getElementById('repSales').textContent = formatCurrency(d.totalSales);
    document.getElementById('repSellCount').textContent = `${d.sellCount || 0} Sell Orders`;

    document.getElementById('repCost').textContent = formatCurrency(d.currentPortfolioCost);

    const realEl = document.getElementById('repRealizedPL');
    realEl.textContent = (d.realizedProfitLoss >= 0 ? '+' : '') + formatCurrency(d.realizedProfitLoss);
    realEl.className = 'metric-value ' + (d.realizedProfitLoss >= 0 ? 'positive' : 'negative');

    const unrealEl = document.getElementById('repUnrealizedPL');
    unrealEl.textContent = (d.unrealizedProfitLoss >= 0 ? '+' : '') + formatCurrency(d.unrealizedProfitLoss);
    unrealEl.className = 'metric-value ' + (d.unrealizedProfitLoss >= 0 ? 'positive' : 'negative');

    const netEl = document.getElementById('repNetPL');
    netEl.textContent = (d.netCombinedProfitLoss >= 0 ? '+' : '') + formatCurrency(d.netCombinedProfitLoss);
    netEl.className = 'metric-value ' + (d.netCombinedProfitLoss >= 0 ? 'positive' : 'negative');

    // 2. Holdings Table
    renderReportHoldings(d.holdings || []);

    // 3. Render Visual Charts
    renderReportCharts(d);

  } catch (err) {
    console.error('Error loading reports:', err);
    showToast('Failed to load reports: ' + err.message, 'error');
  }
}

function renderReportHoldings(holdings) {
  const tbody = document.getElementById('reportHoldingsTableBody');
  if (!tbody) return;

  if (holdings.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="9" class="empty-state">
          <div class="empty-icon">📊</div>
          <div>No active equity holdings in your account.</div>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = holdings.map(h => {
    const isGain = h.unrealizedProfitLoss >= 0;
    const plClass = isGain ? 'positive' : 'negative';
    const plSign = isGain ? '+' : '';

    return `
      <tr>
        <td><strong style="color: var(--accent-cyan);">${h.symbol}</strong></td>
        <td>${h.company}</td>
        <td><span class="badge badge-info">${h.sector}</span></td>
        <td style="font-weight: 600;">${formatNumber(h.quantity)}</td>
        <td>${formatCurrency(h.averageBuyPrice)}</td>
        <td style="font-weight: 600;">${formatCurrency(h.currentPrice)}</td>
        <td>${formatCurrency(h.totalCost)}</td>
        <td style="font-weight: 700;">${formatCurrency(h.currentValue)}</td>
        <td class="${plClass}" style="font-weight: 700;">
          ${plSign}${formatCurrency(h.unrealizedProfitLoss)} (${plSign}${h.profitLossPercent}%)
        </td>
      </tr>
    `;
  }).join('');
}

function renderReportCharts(data) {
  const holdings = data.holdings || [];

  // 1. Asset Value Chart
  const ctx1 = document.getElementById('reportAssetChart');
  if (ctx1 && window.Chart) {
    if (reportAssetChartInstance) reportAssetChartInstance.destroy();

    const hasHoldings = holdings.length > 0;
    const labels = hasHoldings ? holdings.map(h => h.symbol) : ['No Holdings'];
    const values = hasHoldings ? holdings.map(h => h.currentValue) : [1];
    const colors = ['#06b6d4', '#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#64748b'];

    reportAssetChartInstance = new Chart(ctx1, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: values,
          backgroundColor: colors,
          borderColor: '#111827',
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { color: '#94a3b8' } }
        }
      }
    });
  }

  // 2. Cash Flow Comparison Bar Chart
  const ctx2 = document.getElementById('reportCashFlowChart');
  if (ctx2 && window.Chart) {
    if (reportCashFlowChartInstance) reportCashFlowChartInstance.destroy();

    reportCashFlowChartInstance = new Chart(ctx2, {
      type: 'bar',
      data: {
        labels: ['Purchases', 'Sales Proceeds', 'Active Cost Basis', 'Net Return'],
        datasets: [{
          data: [data.totalPurchases, data.totalSales, data.currentPortfolioCost, data.netCombinedProfitLoss],
          backgroundColor: [
            'rgba(59, 130, 246, 0.75)',
            'rgba(16, 185, 129, 0.75)',
            'rgba(245, 158, 11, 0.75)',
            data.netCombinedProfitLoss >= 0 ? 'rgba(6, 182, 212, 0.75)' : 'rgba(244, 63, 94, 0.75)'
          ],
          borderColor: ['#3b82f6', '#10b981', '#f59e0b', data.netCombinedProfitLoss >= 0 ? '#06b6d4' : '#f43f5e'],
          borderWidth: 1,
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { color: 'rgba(255, 255, 255, 0.05)' }, ticks: { color: '#94a3b8' } },
          y: { grid: { color: 'rgba(255, 255, 255, 0.05)' }, ticks: { color: '#94a3b8' } }
        }
      }
    });
  }
}

window.onAccountUpdated = () => {
  loadReports();
};

document.addEventListener('DOMContentLoaded', () => {
  loadReports();

  const refreshBtn = document.getElementById('btnRefreshReports');
  if (refreshBtn) {
    refreshBtn.addEventListener('click', loadReports);
  }
});
