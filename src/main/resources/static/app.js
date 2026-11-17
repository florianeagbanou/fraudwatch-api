// Small demo client for the FraudWatch API. No framework, no build step.
// Security notes:
//  - the JWT is kept in memory only (not localStorage), so a malicious script cannot read it later;
//  - every value coming from the API is inserted with textContent, never innerHTML, to prevent XSS.

const state = { token: null, user: null, role: null, accounts: [], selectedAccountId: null };

const $ = (id) => document.getElementById(id);

// ---------- HTTP helper ----------

async function api(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    const fieldErrors = data?.errors ? Object.entries(data.errors).map(([f, m]) => `${f} ${m}`).join(', ') : '';
    throw new Error(fieldErrors || data?.detail || `Request failed (${response.status})`);
  }
  return data;
}

// ---------- UI helpers ----------

function showMessage(text, kind = 'error') {
  const box = $('message');
  box.textContent = text;
  box.className = kind === 'info' ? 'info' : '';
  box.hidden = false;
}

function clearMessage() {
  $('message').hidden = true;
}

function cell(text, className) {
  const td = document.createElement('td');
  td.textContent = text;
  if (className) td.className = className;
  return td;
}

function badge(text) {
  const span = document.createElement('span');
  span.className = `badge ${text}`;
  span.textContent = text;
  return span;
}

const money = (value) => Number(value).toLocaleString('en-CA', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function showView(name) {
  $('auth-view').hidden = name !== 'auth';
  $('customer-view').hidden = name !== 'customer';
  $('analyst-view').hidden = name !== 'analyst';
  $('session').hidden = name === 'auth';
}

// ---------- Authentication ----------

async function login(username, password) {
  const result = await api('POST', '/api/auth/login', { username, password });
  state.token = result.accessToken;
  state.user = result.username;
  state.role = result.role;
  $('session-user').textContent = `${result.username} (${result.role})`;
  clearMessage();
  if (result.role === 'USER') {
    showView('customer');
    await loadAccounts();
  } else {
    showView('analyst');
    await loadAlerts();
  }
}

$('auth-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    await login($('username').value.trim(), $('password').value);
  } catch (error) {
    showMessage(error.message);
  }
});

$('register-btn').addEventListener('click', async () => {
  const username = $('username').value.trim();
  const password = $('password').value;
  try {
    await api('POST', '/api/auth/register', { username, password });
    await login(username, password);
    showMessage('Account created. Welcome!', 'info');
  } catch (error) {
    showMessage(error.message);
  }
});

$('logout').addEventListener('click', () => {
  Object.assign(state, { token: null, user: null, role: null, accounts: [], selectedAccountId: null });
  $('password').value = '';
  $('tx-result').hidden = true;
  clearMessage();
  showView('auth');
});

// ---------- Customer: accounts ----------

async function loadAccounts() {
  state.accounts = await api('GET', '/api/accounts');
  if (!state.selectedAccountId && state.accounts.length > 0) state.selectedAccountId = state.accounts[0].id;
  renderAccounts();
  await loadHistory();
}

function renderAccounts() {
  const list = $('accounts');
  list.replaceChildren();
  if (state.accounts.length === 0) {
    const li = document.createElement('li');
    li.textContent = 'No account yet. Open one to start.';
    list.append(li);
    return;
  }
  for (const account of state.accounts) {
    const li = document.createElement('li');
    li.textContent = `${account.number} · ${money(account.balance)} ${account.currency} · ${account.homeCountry}`;
    li.dataset.testid = 'account';
    li.tabIndex = 0;
    if (account.id === state.selectedAccountId) li.classList.add('selected');
    const select = async () => {
      state.selectedAccountId = account.id;
      renderAccounts();
      await loadHistory();
    };
    li.addEventListener('click', select);
    li.addEventListener('keydown', (e) => { if (e.key === 'Enter') select(); });
    list.append(li);
  }
}

$('account-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    const account = await api('POST', '/api/accounts', { homeCountry: $('home-country').value, currency: 'CAD' });
    state.selectedAccountId = account.id;
    await loadAccounts();
  } catch (error) {
    showMessage(error.message);
  }
});

// ---------- Customer: transactions ----------

async function loadHistory() {
  const body = $('tx-history');
  body.replaceChildren();
  if (!state.selectedAccountId) return;
  const history = await api('GET', `/api/accounts/${state.selectedAccountId}/transactions`);
  for (const tx of history) {
    const tr = document.createElement('tr');
    const status = document.createElement('td');
    status.append(badge(tx.status));
    tr.append(
      cell(new Date(tx.createdAt).toLocaleString('en-CA')),
      cell(tx.type),
      cell(money(tx.amount)),
      cell(tx.country),
      status,
      cell(String(tx.riskScore)),
    );
    body.append(tr);
  }
}

$('tx-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!state.selectedAccountId) {
    showMessage('Open an account first.');
    return;
  }
  try {
    const tx = await api('POST', `/api/accounts/${state.selectedAccountId}/transactions`, {
      type: $('tx-type').value,
      amount: $('tx-amount').value,
      merchant: $('tx-merchant').value || null,
      country: $('tx-country').value.trim().toUpperCase(),
    });
    clearMessage();
    renderResult(tx);
    await loadAccounts();
  } catch (error) {
    showMessage(error.message);
  }
});

function renderResult(tx) {
  const box = $('tx-result');
  box.replaceChildren();
  const title = document.createElement('p');
  title.append('Result: ', badge(tx.status), ` risk score ${tx.riskScore}/100`);
  box.append(title);
  if (tx.triggeredRules.length > 0) {
    const ul = document.createElement('ul');
    for (const rule of tx.triggeredRules) {
      const li = document.createElement('li');
      li.textContent = `${rule.code} (+${rule.points}): ${rule.reason}`;
      ul.append(li);
    }
    box.append(ul);
  }
  box.hidden = false;
}

// ---------- Analyst: alerts ----------

async function loadAlerts() {
  const status = $('alert-filter').value;
  const alerts = await api('GET', `/api/alerts${status ? `?status=${status}` : ''}`);
  const body = $('alerts');
  body.replaceChildren();
  if (alerts.length === 0) {
    const tr = document.createElement('tr');
    const td = cell('No alerts.');
    td.colSpan = 8;
    tr.append(td);
    body.append(tr);
    return;
  }
  for (const alert of alerts) {
    const tr = document.createElement('tr');
    tr.dataset.testid = 'alert-row';
    const statusCell = document.createElement('td');
    statusCell.append(badge(alert.status));
    const actions = document.createElement('td');
    if (alert.status === 'OPEN') {
      actions.append(
        actionButton('Confirm fraud', 'danger', () => review(alert.id, 'CONFIRMED_FRAUD')),
        ' ',
        actionButton('False positive', 'secondary', () => review(alert.id, 'FALSE_POSITIVE')),
      );
    } else {
      actions.textContent = `by ${alert.reviewedBy}`;
    }
    tr.append(
      cell(String(alert.id)),
      cell(alert.accountNumber),
      cell(money(alert.amount)),
      cell(alert.country),
      cell(String(alert.riskScore)),
      cell(alert.triggeredRules.map((r) => r.code).join(', ')),
      statusCell,
      actions,
    );
    body.append(tr);
  }
}

function actionButton(label, className, onClick) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = className;
  button.textContent = label;
  button.addEventListener('click', onClick);
  return button;
}

async function review(alertId, resolution) {
  try {
    await api('PATCH', `/api/alerts/${alertId}`, { resolution, note: 'Reviewed from the dashboard' });
    showMessage(`Alert ${alertId} closed as ${resolution}.`, 'info');
    await loadAlerts();
  } catch (error) {
    showMessage(error.message);
  }
}

$('alert-filter').addEventListener('change', () => loadAlerts().catch((e) => showMessage(e.message)));

showView('auth');
