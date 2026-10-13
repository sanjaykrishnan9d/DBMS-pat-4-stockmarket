/**
 * Portfolio Controller
 */

let portfolioHoldings = [];

async function loadPortfolio() {
  const user = await initSession();
  if (!user) return;

  try {
    const res = await apiRequest('/api/portfolio');
    if (!res || !res.success || !res.data) return;

    const d = res.data;
    const summary = d.summary || {};
    portfolioHoldings = d.holdings || [];

    // 1. Populate summary cards
    document.getElementById('portCurrentVal').textContent = formatCurrency(summary.currentTotalValue);
    document.getElementById('portTotalCost').textContent = formatCurrency(summary.totalInvestment);
    
    const plEl = document.getElementById('portProfitLoss');
    const plPercentEl = document.getElementById('portProfitLossPercent');
    const pl = summary.unrealizedProfitLoss || 0;
    const plPct = summary.profitLossPercent || 0;

    plEl.textContent = formatCurrency(pl);
    if (pl >= 0) {
      plEl.className = 'metric-value positive';
      plPercentEl.className = 'metric-sub positive';
      plPercentEl.innerHTML = `<span>▲ +${plPct}%</span>`;
    } else {
      plEl.className = 'metric-value negative';
      plPercentEl.className = 'metric-sub negative';
      plPercentEl.innerHTML = `<span>▼ ${plPct}%</span>`;
    }

    document.getElementById('portTotalShares').textContent = formatNumber(summary.totalShares || 0);

    // 2. Render holdings table
    renderHoldingsTable(portfolioHoldings);

  } catch (err) {
    console.error('Error loading portfolio:', err);
    showToast('Failed to load portfolio: ' + err.message, 'error');
  }
}

function renderHoldingsTable(holdings) {
  const tbody = document.getElementById('portfolioTableBody');
  if (!tbody) return;

  if (holdings.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="10" class="empty-state">
          <div class="empty-icon">💼</div>
          <div style="font-size: 1rem; font-weight: 600; margin-bottom: 6px;">No stock holdings in portfolio</div>
          <div style="margin-bottom: 16px;">You haven't purchased any company shares yet.</div>
          <a href="stocks.html" class="btn btn-primary btn-sm">Explore Market Watch →</a>
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
        <td>
          <strong style="color: var(--accent-cyan); font-size: 1.05rem;">${h.symbol}</strong>
          <div style="font-size: 0.8rem; color: var(--text-muted);">${h.company}</div>
        </td>
        <td><span class="badge badge-info">${h.sector}</span></td>
        <td style="font-weight: 700; font-size: 1.05rem;">${formatNumber(h.quantity)}</td>
        <td>${formatCurrency(h.averageBuyPrice)}</td>
        <td style="font-weight: 600;">${formatCurrency(h.currentPrice)}</td>
        <td>${formatCurrency(h.totalCost)}</td>
        <td style="font-weight: 700;">${formatCurrency(h.currentValue)}</td>
        <td class="${plClass}" style="font-weight: 700;">
          ${plSign}${formatCurrency(h.unrealizedProfitLoss)}
        </td>
        <td class="${plClass}" style="font-weight: 700;">
          ${plSign}${h.profitLossPercent}%
        </td>
        <td style="text-align: right; white-space: nowrap;">
          <div style="display: inline-flex; gap: 6px;">
            <button class="btn btn-danger btn-sm" onclick="triggerSell(${h.stockId})">
              Sell
            </button>
            <button class="btn btn-outline btn-sm" onclick="triggerBuyMore(${h.stockId})">
              Buy More
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function triggerSell(stockId) {
  const holding = portfolioHoldings.find(h => h.stockId === stockId);
  if (!holding) return;

  const stock = {
    stockId: holding.stockId,
    symbol: holding.symbol,
    company: holding.company,
    currentPrice: holding.currentPrice
  };
  openTradeModal(stock, 'SELL');
}

function triggerBuyMore(stockId) {
  const holding = portfolioHoldings.find(h => h.stockId === stockId);
  if (!holding) return;

  const stock = {
    stockId: holding.stockId,
    symbol: holding.symbol,
    company: holding.company,
    currentPrice: holding.currentPrice
  };
  openTradeModal(stock, 'BUY');
}

window.onAccountUpdated = () => {
  loadPortfolio();
};

document.addEventListener('DOMContentLoaded', () => {
  loadPortfolio();

  const refreshBtn = document.getElementById('btnRefreshPortfolio');
  if (refreshBtn) {
    refreshBtn.addEventListener('click', loadPortfolio);
  }
});
