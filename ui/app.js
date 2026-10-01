const state = {
  auth: '',
  username: '',
  leads: [],
  selectedLeadId: null,
  dashboard: null,
  calls: [],
  campaigns: [],
  playbooks: [],
  suppressions: [],
  filters: { search: '', status: 'ALL', priority: 'ALL', source: 'ALL', followUp: 'ALL' }
};

const apiBase = `${window.LEAD_API_BASE_URL || defaultApiOrigin()}/api/v1`;

function defaultApiOrigin() {
  const protocol = window.location.protocol;
  const host = window.location.host;
  if (protocol.startsWith('http') && host && !host.startsWith('127.0.0.1:5500') && !host.startsWith('localhost:5500')) {
    return window.location.origin;
  }
  return 'http://localhost:8080';
}

const elements = {
  loginScreen: document.getElementById('loginScreen'),
  appScreen: document.getElementById('appScreen'),
  authForm: document.getElementById('authForm'),
  username: document.getElementById('username'),
  password: document.getElementById('password'),
  loginAlert: document.getElementById('loginAlert'),
  userLabel: document.getElementById('userLabel'),
  logoutBtn: document.getElementById('logoutBtn'),
  pageTitle: document.getElementById('pageTitle'),
  menuToggle: document.getElementById('menuToggle'),
  navItems: [...document.querySelectorAll('.nav-item[data-view]')],
  appViews: [...document.querySelectorAll('.app-view')],
  exportExcelBtn: document.getElementById('exportExcelBtn'),
  exportPdfBtn: document.getElementById('exportPdfBtn'),
  dashboardStats: document.getElementById('dashboardStats'),
  searchInput: document.getElementById('searchInput'),
  statusFilter: document.getElementById('statusFilter'),
  priorityFilter: document.getElementById('priorityFilter'),
  sourceFilter: document.getElementById('sourceFilter'),
  followUpFilter: document.getElementById('followUpFilter'),
  leadTableBody: document.getElementById('leadTableBody'),
  selectedLeadName: document.getElementById('selectedLeadName'),
  leadMeta: document.getElementById('leadMeta'),
  keyGrid: document.getElementById('keyGrid'),
  qualifyLeadForm: document.getElementById('qualifyLeadForm'),
  qualifyMessage: document.getElementById('qualifyMessage'),
  qualIntent: document.getElementById('qualIntent'),
  qualPropertyType: document.getElementById('qualPropertyType'),
  qualBudget: document.getElementById('qualBudget'),
  qualLocation: document.getElementById('qualLocation'),
  qualTimeline: document.getElementById('qualTimeline'),
  qualDecisionMaker: document.getElementById('qualDecisionMaker'),
  qualCallback: document.getElementById('qualCallback'),
  qualNotes: document.getElementById('qualNotes'),
  callType: document.getElementById('callType'),
  startCallBtn: document.getElementById('startCallBtn'),
  leadNotes: document.getElementById('leadNotes'),
  followUpAt: document.getElementById('followUpAt'),
  followUpStatus: document.getElementById('followUpStatus'),
  saveActivityBtn: document.getElementById('saveActivityBtn'),
  activityMessage: document.getElementById('activityMessage'),
  callList: document.getElementById('callList'),
  briefText: document.getElementById('briefText'),
  briefOutput: document.getElementById('briefOutput'),
  generateBriefBtn: document.getElementById('generateBriefBtn'),
  exportBriefPdfBtn: document.getElementById('exportBriefPdfBtn'),
  createLeadForm: document.getElementById('createLeadForm'),
  createLeadMessage: document.getElementById('createLeadMessage'),
  importLeadsForm: document.getElementById('importLeadsForm'),
  leadImportFile: document.getElementById('leadImportFile'),
  importLeadsMessage: document.getElementById('importLeadsMessage'),
  callSummaryStats: document.getElementById('callSummaryStats'),
  callFlowForm: document.getElementById('callFlowForm'),
  callFlowLead: document.getElementById('callFlowLead'),
  callFlowType: document.getElementById('callFlowType'),
  callFlowMessage: document.getElementById('callFlowMessage'),
  callPlanForm: document.getElementById('callPlanForm'),
  planName: document.getElementById('planName'),
  planPhone: document.getElementById('planPhone'),
  planSource: document.getElementById('planSource'),
  callPlanResult: document.getElementById('callPlanResult'),
  scenarioForm: document.getElementById('scenarioForm'),
  scenarioLead: document.getElementById('scenarioLead'),
  scenarioPlaybook: document.getElementById('scenarioPlaybook'),
  scenarioTranscript: document.getElementById('scenarioTranscript'),
  scenarioSummary: document.getElementById('scenarioSummary'),
  scenarioResult: document.getElementById('scenarioResult'),
  transcriptForm: document.getElementById('transcriptForm'),
  transcriptCallSelect: document.getElementById('transcriptCallSelect'),
  transcriptText: document.getElementById('transcriptText'),
  transcriptSummary: document.getElementById('transcriptSummary'),
  transcriptRecordingUrl: document.getElementById('transcriptRecordingUrl'),
  transcriptStatus: document.getElementById('transcriptStatus'),
  transcriptMessage: document.getElementById('transcriptMessage'),
  allCallsTable: document.getElementById('allCallsTable'),
  refreshCallsBtn: document.getElementById('refreshCallsBtn'),
  campaignForm: document.getElementById('campaignForm'),
  campaignName: document.getElementById('campaignName'),
  campaignPlaybookVersion: document.getElementById('campaignPlaybookVersion'),
  campaignStart: document.getElementById('campaignStart'),
  campaignEnd: document.getElementById('campaignEnd'),
  campaignAttempts: document.getElementById('campaignAttempts'),
  campaignMessage: document.getElementById('campaignMessage'),
  campaignList: document.getElementById('campaignList'),
  refreshCampaignsBtn: document.getElementById('refreshCampaignsBtn'),
  playbookForm: document.getElementById('playbookForm'),
  playbookName: document.getElementById('playbookName'),
  playbookIndustry: document.getElementById('playbookIndustry'),
  playbookStatus: document.getElementById('playbookStatus'),
  playbookVersion: document.getElementById('playbookVersion'),
  playbookConfiguration: document.getElementById('playbookConfiguration'),
  playbookApproval: document.getElementById('playbookApproval'),
  playbookMessage: document.getElementById('playbookMessage'),
  playbookList: document.getElementById('playbookList'),
  refreshPlaybooksBtn: document.getElementById('refreshPlaybooksBtn'),
  suppressionForm: document.getElementById('suppressionForm'),
  suppressionPhone: document.getElementById('suppressionPhone'),
  suppressionReason: document.getElementById('suppressionReason'),
  suppressionScope: document.getElementById('suppressionScope'),
  suppressionSource: document.getElementById('suppressionSource'),
  suppressionMessage: document.getElementById('suppressionMessage'),
  suppressionResults: document.getElementById('suppressionResults'),
  refreshComplianceBtn: document.getElementById('refreshComplianceBtn'),
  complianceCallLog: document.getElementById('complianceCallLog'),
  briefLeadSelect: document.getElementById('briefLeadSelect'),
  briefSummary: document.getElementById('briefSummary'),
  salesBriefForm: document.getElementById('salesBriefForm'),
  lookupLeadForm: document.getElementById('lookupLeadForm'),
  lookupPhone: document.getElementById('lookupPhone'),
  lookupLeadMessage: document.getElementById('lookupLeadMessage'),
  assignLeadForm: document.getElementById('assignLeadForm'),
  assignOwnerId: document.getElementById('assignOwnerId'),
  assignReason: document.getElementById('assignReason'),
  assignMessage: document.getElementById('assignMessage')
};

function setAuth(username, password) {
  state.username = username;
  state.auth = 'Basic ' + btoa(`${username}:${password}`);
}

function showAlert(message, isError = true) {
  elements.loginAlert.textContent = message;
  elements.loginAlert.classList.add('show');
  elements.loginAlert.style.background = isError ? '#fff3f3' : '#edf9f3';
  elements.loginAlert.style.color = isError ? '#a42a2a' : '#105c3d';
  elements.loginAlert.style.borderColor = isError ? 'rgba(164, 42, 42, 0.18)' : 'rgba(16, 92, 61, 0.18)';
}

function hideAlert() {
  elements.loginAlert.classList.remove('show');
}

function showApp() {
  elements.loginScreen.classList.add('hidden');
  elements.appScreen.classList.remove('hidden');
  elements.userLabel.textContent = `Signed in as ${state.username}`;
}

function showLogin() {
  elements.appScreen.classList.add('hidden');
  elements.loginScreen.classList.remove('hidden');
}

async function login() {
  const username = elements.username.value.trim();
  const password = elements.password.value.trim();

  if (!username || !password) {
    showAlert('Please enter both username and password.');
    return;
  }

  setAuth(username, password);

  try {
    const response = await fetch(`${apiBase}/auth/login`, {
      method: 'POST',
      headers: {
        Authorization: state.auth,
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error('Authentication failed');
    }

    hideAlert();
    showApp();
    await loadDashboard();
  } catch (error) {
    showAlert('Login failed. Check the username and password configured in .env.');
  }
}

function logout() {
  state.auth = '';
  state.username = '';
  elements.username.value = '';
  elements.password.value = '';
  showLogin();
}

async function fetchJson(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      Authorization: state.auth,
      'ngrok-skip-browser-warning': 'true',
      ...(options.headers || {})
    }
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || 'Request failed');
  }

  return response.json();
}

async function loadDashboard() {
  try {
    const [dashboard, leads] = await Promise.all([
      fetchJson(`${apiBase}/dashboard`),
      fetchJson(`${apiBase}/leads`)
    ]);

    state.dashboard = dashboard;
    state.leads = leads || [];
    populateLeadSelectors();

    const metrics = [
      { label: 'Queued', value: dashboard.queued ?? state.leads.length },
      { label: 'Called Today', value: dashboard.calledToday ?? 0 },
      { label: 'Qualified', value: dashboard.qualified ?? 0 },
      { label: 'Hot Leads', value: dashboard.hotLeads ?? 0 }
    ];

    elements.dashboardStats.innerHTML = metrics.map(item => `
      <div class="metric-card">
        <div class="metric-label">${item.label}</div>
        <div class="metric-value">${item.value}</div>
      </div>
    `).join('');

    renderLeadTable();
    if (!state.selectedLeadId && state.leads.length) {
      selectLead(state.leads[0]);
    } else if (state.selectedLeadId) {
      const existing = state.leads.find(lead => Number(lead.id) === Number(state.selectedLeadId));
      if (existing) {
        selectLead(existing);
      } else if (state.leads.length) {
        selectLead(state.leads[0]);
      }
    }
  } catch (error) {
    showAlert(error.message || 'Unable to load dashboard data.');
  }
}

function getFilteredLeads() {
  const search = state.filters.search.trim().toLowerCase();

  return state.leads.filter(lead => {
    const matchesSearch = !search || [
      lead.name,
      lead.phone,
      lead.email,
      lead.source,
      lead.status,
      lead.summary,
      lead.preferredLocation,
      lead.propertyType
    ].join(' ').toLowerCase().includes(search);

    const matchesStatus = state.filters.status === 'ALL' || (lead.status || 'NEW') === state.filters.status;
    const matchesPriority = state.filters.priority === 'ALL' || (lead.scoreBand || 'COLD') === state.filters.priority;
    const matchesSource = state.filters.source === 'ALL' || (lead.source || '').toLowerCase() === state.filters.source.toLowerCase();
    const followUpStatus = lead.followUpStatus || 'NONE';
    const followUpDate = lead.followUpAt ? new Date(lead.followUpAt) : null;
    const matchesFollowUp = state.filters.followUp === 'ALL'
      || (state.filters.followUp === 'DUE' && followUpStatus === 'SCHEDULED' && followUpDate && followUpDate <= new Date())
      || followUpStatus === state.filters.followUp;

    return matchesSearch && matchesStatus && matchesPriority && matchesSource && matchesFollowUp;
  });
}

function renderLeadTable() {
  const filteredLeads = getFilteredLeads();

  if (!filteredLeads.length) {
    elements.leadTableBody.innerHTML = `
      <tr>
        <td colspan="9" class="muted">No leads match the current filters.</td>
      </tr>
    `;
    return;
  }

  elements.leadTableBody.innerHTML = filteredLeads.map(lead => {
    const status = lead.status || 'NEW';
    const scoreBand = (lead.scoreBand || 'COLD').toLowerCase();
    const isSelected = Number(lead.id) === Number(state.selectedLeadId);

    return `
      <tr class="${isSelected ? 'selected' : ''}" data-id="${lead.id}">
        <td>${lead.name || 'Unknown'}</td>
        <td>${lead.phone || '—'}</td>
        <td>${lead.source || 'website_form'}</td>
        <td><span class="badge ${status.toLowerCase().replace(/\s+/g, '-')}">${status}</span></td>
        <td><span class="badge ${scoreBand}">${lead.scoreBand || 'COLD'}</span></td>
        <td>${lead.leadIntent || lead.propertyType || '—'}</td>
        <td>${lead.preferredLocation || '—'}</td>
        <td>${lead.followUpAt ? `${escapeHtml(formatDateTime(lead.followUpAt))}<small class="follow-up-state">${escapeHtml(lead.followUpStatus || 'SCHEDULED')}</small>` : '—'}</td>
        <td>${lead.summary || 'Awaiting qualification call'}</td>
      </tr>
    `;
  }).join('');

  elements.leadTableBody.querySelectorAll('tr[data-id]').forEach(row => {
    row.addEventListener('click', () => {
      const id = row.dataset.id;
      const lead = state.leads.find(item => Number(item.id) === Number(id));
      if (lead) selectLead(lead);
    });
  });
}

async function selectLead(lead) {
  const requestedLeadId = Number(lead.id);
  state.selectedLeadId = requestedLeadId;
  try {
    const latestLead = await fetchJson(`${apiBase}/leads/${requestedLeadId}`);
    state.leads = state.leads.map(item => Number(item.id) === requestedLeadId ? latestLead : item);
    lead = latestLead;
  } catch (error) {
    lead = state.leads.find(item => Number(item.id) === requestedLeadId) || lead;
  }
  if (Number(state.selectedLeadId) !== requestedLeadId) return;
  renderLeadTable();
  [elements.callFlowLead, elements.scenarioLead, elements.briefLeadSelect].forEach(select => {
    if (!select.value) select.value = String(requestedLeadId);
  });

  elements.selectedLeadName.textContent = lead.name || 'Unknown lead';
  elements.leadMeta.textContent = `${lead.phone || '—'} • ${lead.source || 'website_form'} • ${lead.status || 'NEW'}`;

  const keyItems = [
    ['Intent', lead.leadIntent || 'Not captured'],
    ['Property', lead.propertyType || 'Not captured'],
    ['Budget', lead.budgetRange || 'Not captured'],
    ['Location', lead.preferredLocation || 'Not captured'],
    ['Timeline', lead.timeline || 'Not captured'],
    ['Decision maker', lead.decisionMaker || 'Not captured'],
    ['Callback', lead.preferredCallbackTime || 'Not captured'],
    ['Status', lead.status || 'NEW']
  ];

  elements.keyGrid.innerHTML = keyItems.map(([label, value]) => `
    <div class="key-item">
      <small>${label}</small>
      <strong>${value}</strong>
    </div>
  `).join('');
  elements.leadNotes.value = lead.qualificationNotes || '';
  elements.qualIntent.value = lead.leadIntent || '';
  elements.qualPropertyType.value = lead.propertyType || '';
  elements.qualBudget.value = lead.budgetRange || '';
  elements.qualLocation.value = lead.preferredLocation || '';
  elements.qualTimeline.value = lead.timeline || '';
  elements.qualDecisionMaker.value = lead.decisionMaker || '';
  elements.qualCallback.value = lead.preferredCallbackTime || '';
  elements.qualNotes.value = lead.qualificationNotes || '';
  elements.followUpAt.value = lead.followUpAt ? String(lead.followUpAt).slice(0, 16) : '';
  elements.followUpStatus.value = lead.followUpStatus || 'NONE';
  elements.activityMessage.textContent = '';
  elements.activityMessage.className = 'activity-message';

  try {
    const history = await fetchJson(`${apiBase}/calls/lead/${lead.id}`);
    renderCallHistory(history || []);
  } catch (error) {
    elements.callList.innerHTML = '<div class="muted">No call history available.</div>';
  }

  const leadBrief = lead.summary || 'Lead details are still being collected. The sales brief will appear here once the call is completed.';
  elements.briefText.value = leadBrief;
  elements.briefOutput.textContent = leadBrief;
}

function renderCallHistory(history) {
  if (!history.length) {
    elements.callList.innerHTML = '<div class="muted">No call history exists for this lead yet.</div>';
    return;
  }

  elements.callList.innerHTML = history.map(call => `
    <div class="call-item">
      <div class="call-item-header">
        <strong>Call #${escapeHtml(call.id)}</strong>
        <span class="badge ${String(call.status || 'PLACED').toLowerCase().replace(/\s+/g, '-')}\">${escapeHtml(call.status || 'PLACED')}</span>
      </div>
      <div class="muted">${escapeHtml(call.phone || '—')} • ${call.createdAt ? new Date(call.createdAt).toLocaleString() : '—'}</div>
      <div class="call-duration">Duration: ${call.durationSeconds == null ? 'Pending' : escapeHtml(formatDuration(call.durationSeconds))}</div>
      <div class="call-summary">${escapeHtml((call.summary || 'No summary captured yet.').substring(0, 220))}</div>
      ${call.recordingUrl ? `<a class="recording-link" href="${escapeHtml(call.recordingUrl)}" target="_blank" rel="noopener noreferrer">Open recording</a>` : ''}
      ${renderConversation(call.conversation || call.transcript)}
    </div>
  `).join('');
}

const questionLabels = {
  1: 'Buying, renting, selling, or investing',
  2: 'Area and property type',
  3: 'Budget range',
  4: 'Purchase timeline',
  5: 'Decision maker',
  6: 'Preferred callback time'
};

function renderConversation(conversation) {
  if (!conversation) {
    return '<div class="answers-empty">Answers will appear here as the call progresses.</div>';
  }

  const answers = conversation.split(/\r?\n/).map(entry => {
    const match = entry.match(/^Q(\d+):\s*(.*)$/s);
    if (match) {
      return { speaker: questionLabels[match[1]] || `Question ${match[1]}`, text: match[2] };
    }
    const turn = entry.match(/^(USER|ASSISTANT):\s*(.*)$/s);
    return turn ? { speaker: turn[1] === 'USER' ? 'Lead' : 'Agent', text: turn[2] } : { speaker: 'Conversation', text: entry };
  });

  return `
    <div class="call-answers">
      <div class="answers-heading">Conversation</div>
      ${answers.map(item => `
        <div class="answer-row">
          <span class="answer-question">${escapeHtml(item.speaker)}</span>
          <span class="answer-value">${escapeHtml(item.text)}</span>
        </div>
      `).join('')}
    </div>
  `;
}

function formatDuration(durationSeconds) {
  const seconds = Math.max(0, Number(durationSeconds) || 0);
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return minutes ? `${minutes}m ${remainder}s` : `${remainder}s`;
}

function formatDateTime(value) {
  return new Date(value).toLocaleString();
}

function showActivityMessage(message, isError = false) {
  elements.activityMessage.textContent = message;
  elements.activityMessage.className = `activity-message ${isError ? 'error' : 'success'}`;
}

async function startCall() {
  const lead = state.leads.find(item => Number(item.id) === Number(state.selectedLeadId));
  if (!lead) {
    showActivityMessage('Select a lead before starting a call.', true);
    return;
  }

  elements.startCallBtn.disabled = true;
  showActivityMessage('Starting call...');
  try {
    const result = await fetchJson(`${apiBase}/leads/call`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        phone: lead.phone,
        leadName: lead.name,
        leadId: lead.id,
        type: elements.callType.value
      })
    });
    showActivityMessage(`Call ${result.status || 'started'} (${result.provider || 'provider'}).`);
    const history = await fetchJson(`${apiBase}/calls/lead/${lead.id}`);
    renderCallHistory(history || []);
  } catch (error) {
    showActivityMessage(error.message || 'Unable to start call.', true);
  } finally {
    elements.startCallBtn.disabled = false;
  }
}

async function saveLeadActivity() {
  const lead = state.leads.find(item => Number(item.id) === Number(state.selectedLeadId));
  if (!lead) {
    showActivityMessage('Select a lead before saving activity.', true);
    return;
  }
  if (elements.followUpStatus.value === 'SCHEDULED' && !elements.followUpAt.value) {
    showActivityMessage('Choose a date and time for the scheduled follow-up.', true);
    return;
  }

  elements.saveActivityBtn.disabled = true;
  try {
    const updatedLead = await fetchJson(`${apiBase}/leads/${lead.id}/activity`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        qualificationNotes: elements.leadNotes.value,
        followUpAt: elements.followUpAt.value || null,
        followUpStatus: elements.followUpStatus.value
      })
    });
    state.leads = state.leads.map(item => Number(item.id) === Number(updatedLead.id) ? updatedLead : item);
    renderLeadTable();
    showActivityMessage('Notes and follow-up saved.');
  } catch (error) {
    showActivityMessage(error.message || 'Unable to save notes and follow-up.', true);
  } finally {
    elements.saveActivityBtn.disabled = false;
  }
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

async function generateBrief() {
  const lead = state.leads.find(item => Number(item.id) === Number(elements.briefLeadSelect.value));
  if (!lead) {
    elements.briefOutput.textContent = 'Select a lead before generating a sales brief.';
    return;
  }

  try {
    const payload = {
      transcript: elements.briefText.value || '',
      summary: elements.briefSummary.value || lead.summary || ''
    };

    const result = await fetchJson(`${apiBase}/leads/${lead.id}/sales-brief`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const text = result?.brief || result?.salesBrief || JSON.stringify(result, null, 2);
    elements.briefOutput.textContent = text;
    elements.briefOutput.dataset.brief = text;
  } catch (error) {
    elements.briefOutput.textContent = error.message || 'Unable to generate sales brief.';
  }
}

function exportLeadTableToExcel() {
  const rows = state.leads.map(lead => ({
    ID: lead.id,
    Name: lead.name,
    Phone: lead.phone,
    Email: lead.email || '',
    Source: lead.source || '',
    Status: lead.status || 'NEW',
    Score: lead.scoreBand || 'COLD',
    Intent: lead.leadIntent || '',
    Property: lead.propertyType || '',
    Budget: lead.budgetRange || '',
    Location: lead.preferredLocation || '',
    Timeline: lead.timeline || '',
    Summary: lead.summary || ''
  }));

  const worksheet = XLSX.utils.json_to_sheet(rows);
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Leads');
  XLSX.writeFile(workbook, 'lead-report.xlsx');
}

function exportBriefPdf() {
  const { jsPDF } = window.jspdf;
  const doc = new jsPDF();
  const lead = state.leads.find(item => Number(item.id) === Number(elements.briefLeadSelect.value));
  const text = elements.briefOutput.textContent || 'No sales brief available.';

  doc.setFontSize(18);
  doc.text('Sales Brief', 14, 18);
  doc.setFontSize(11);
  doc.text(`Lead: ${lead ? lead.name : 'N/A'}`, 14, 30);
  doc.text(`Phone: ${lead ? lead.phone : 'N/A'}`, 14, 38);
  doc.text(`Status: ${lead ? (lead.status || 'NEW') : 'N/A'}`, 14, 46);

  const lines = doc.splitTextToSize(text, 180);
  doc.text(lines, 14, 58);
  doc.save('sales-brief.pdf');
}

function updateFilters() {
  state.filters.search = elements.searchInput.value;
  state.filters.status = elements.statusFilter.value;
  state.filters.priority = elements.priorityFilter.value;
  state.filters.source = elements.sourceFilter.value;
  state.filters.followUp = elements.followUpFilter.value;
  renderLeadTable();
}

function populateLeadSelectors() {
  const selectors = [elements.callFlowLead, elements.scenarioLead, elements.briefLeadSelect];
  selectors.forEach(select => {
    if (!select) return;
    const previousValue = select.value || String(state.selectedLeadId || '');
    select.innerHTML = '<option value="">Select a lead</option>' + state.leads.map(lead =>
      `<option value="${escapeHtml(lead.id)}">${escapeHtml(lead.name || 'Unknown')} · ${escapeHtml(lead.phone || '')}</option>`
    ).join('');
    if (state.leads.some(lead => String(lead.id) === previousValue)) {
      select.value = previousValue;
    }
  });
}

function showView(viewId) {
  const activeButton = elements.navItems.find(button => button.dataset.view === viewId);
  if (!activeButton) return;
  elements.navItems.forEach(button => button.classList.toggle('active', button === activeButton));
  elements.appViews.forEach(view => {
    const isActive = view.id === viewId;
    view.hidden = !isActive;
    view.classList.toggle('active', isActive);
  });
  elements.pageTitle.textContent = activeButton.textContent.trim();

  if (viewId === 'callCenterView') loadCallCenter();
  if (viewId === 'campaignsView') {
    loadCampaigns();
    loadPlaybooks();
  }
  if (viewId === 'playbooksView') loadPlaybooks();
  if (viewId === 'complianceView') {
    loadComplianceCallLog();
    loadSuppressions();
  }
}

function setFormMessage(element, message, isError = false) {
  element.textContent = message;
  element.className = `form-message ${isError ? 'error' : 'success'}`;
}

async function createLead(event) {
  event.preventDefault();
  const formData = new FormData(elements.createLeadForm);
  const payload = {
    name: formData.get('name').trim(),
    phone: formData.get('phone').trim(),
    email: formData.get('email').trim() || null,
    industry: formData.get('industry').trim(),
    source: formData.get('source').trim(),
    consent: {
      lawfulBasis: 'consent',
      capturedAt: new Date().toISOString(),
      evidenceReference: 'Dashboard operator confirmed consent'
    }
  };

  try {
    const lead = await fetchJson(`${apiBase}/leads`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    state.selectedLeadId = Number(lead.id);
    elements.createLeadForm.reset();
    elements.createLeadForm.elements.source.value = 'website_form';
    elements.createLeadForm.elements.industry.value = 'REAL_ESTATE';
    setFormMessage(elements.createLeadMessage, `Created lead ${lead.name}.`);
    await loadDashboard();
  } catch (error) {
    setFormMessage(elements.createLeadMessage, error.message || 'Unable to create lead.', true);
  }
}

async function importLeads(event) {
  event.preventDefault();
  const file = elements.leadImportFile.files[0];
  if (!file) return;
  const form = new FormData();
  form.append('file', file);
  try {
    const response = await fetch(`${apiBase}/leads/import`, {
      method: 'POST',
      headers: { Authorization: state.auth, 'ngrok-skip-browser-warning': 'true' },
      body: form
    });
    if (!response.ok) throw new Error((await response.text()) || 'Import failed');
    const result = await response.json();
    elements.importLeadsForm.reset();
    setFormMessage(elements.importLeadsMessage,
      `Imported ${result.imported || 0}; skipped ${result.skipped || 0}.`);
    await loadDashboard();
  } catch (error) {
    setFormMessage(elements.importLeadsMessage, error.message || 'Unable to import workbook.', true);
  }
}

async function qualifySelectedLead(event) {
  event.preventDefault();
  const leadId = state.selectedLeadId;
  if (!leadId) {
    setFormMessage(elements.qualifyMessage, 'Select a lead first.', true);
    return;
  }
  const payload = {
    intent: elements.qualIntent.value,
    property_type: elements.qualPropertyType.value,
    budget_range: elements.qualBudget.value,
    preferred_location: elements.qualLocation.value,
    timeline: elements.qualTimeline.value,
    decision_maker: elements.qualDecisionMaker.value,
    preferred_callback_time: elements.qualCallback.value,
    qualification_notes: elements.qualNotes.value
  };
  try {
    const updated = await fetchJson(`${apiBase}/leads/${leadId}/qualify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    state.leads = state.leads.map(lead => Number(lead.id) === Number(updated.id) ? updated : lead);
    renderLeadTable();
    await selectLead(updated);
    setFormMessage(elements.qualifyMessage, 'Qualification saved.');
  } catch (error) {
    setFormMessage(elements.qualifyMessage, error.message || 'Unable to save qualification.', true);
  }
}

async function lookupLead(event) {
  event.preventDefault();
  try {
    const phone = encodeURIComponent(elements.lookupPhone.value.trim());
    const lead = await fetchJson(`${apiBase}/leads/phone/${phone}`);
    if (!state.leads.some(item => Number(item.id) === Number(lead.id))) state.leads.unshift(lead);
    state.selectedLeadId = Number(lead.id);
    renderLeadTable();
    await selectLead(lead);
    setFormMessage(elements.lookupLeadMessage, `Found ${lead.name}.`);
  } catch (error) {
    setFormMessage(elements.lookupLeadMessage, error.message || 'Lead was not found.', true);
  }
}

async function assignSelectedLead(event) {
  event.preventDefault();
  if (!state.selectedLeadId) return setFormMessage(elements.assignMessage, 'Select a lead first.', true);
  try {
    await fetchJson(`${apiBase}/leads/${state.selectedLeadId}/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ user_id: Number(elements.assignOwnerId.value), reason: elements.assignReason.value })
    });
    const updated = await fetchJson(`${apiBase}/leads/${state.selectedLeadId}`);
    state.leads = state.leads.map(lead => Number(lead.id) === Number(updated.id) ? updated : lead);
    renderLeadTable();
    await selectLead(updated);
    setFormMessage(elements.assignMessage, `Assigned to user ${updated.ownerId}.`);
  } catch (error) {
    setFormMessage(elements.assignMessage, error.message || 'Unable to assign lead.', true);
  }
}

async function loadCallCenter() {
  elements.callSummaryStats.innerHTML = '<div class="muted">Loading call summary...</div>';
  try {
    const summary = await fetchJson(`${apiBase}/calls/summary`);
    const metrics = [
      ['Total calls', summary.totalCalls ?? 0],
      ['Completed', summary.completedCalls ?? 0],
      ['Failed', summary.failedCalls ?? 0],
      ['Hot leads', summary.hotLeads ?? 0]
    ];
    elements.callSummaryStats.innerHTML = metrics.map(([label, value]) => `
      <div class="metric-card"><div class="metric-label">${escapeHtml(label)}</div><div class="metric-value">${escapeHtml(value)}</div></div>
    `).join('');
  } catch (error) {
    elements.callSummaryStats.textContent = error.message || 'Unable to load call summary.';
  }
  await loadAllCallHistory();
}

async function loadAllCallHistory() {
  const histories = await Promise.allSettled(state.leads.map(lead =>
    fetchJson(`${apiBase}/calls/lead/${lead.id}`).then(calls => (calls || []).map(call => ({ ...call, leadName: lead.name })))
  ));
  state.calls = histories.flatMap(result => result.status === 'fulfilled' ? result.value : []);
  state.calls.sort((first, second) => new Date(second.createdAt || 0) - new Date(first.createdAt || 0));
  renderAllCalls();
}

function renderAllCalls() {
  if (!state.calls.length) {
    elements.allCallsTable.innerHTML = '<tr><td colspan="6" class="muted">No calls found for the current leads.</td></tr>';
    elements.transcriptCallSelect.innerHTML = '<option value="">No calls available</option>';
    return;
  }
  elements.allCallsTable.innerHTML = state.calls.map(call => `
    <tr data-call-id="${escapeHtml(call.id)}">
      <td>${escapeHtml(call.leadName || call.leadId || 'Lead')}</td>
      <td>${escapeHtml(call.phone || '—')}</td>
      <td>${escapeHtml(call.status || '—')}</td>
      <td>${call.durationSeconds == null ? 'Pending' : escapeHtml(formatDuration(call.durationSeconds))}</td>
      <td>${call.createdAt ? escapeHtml(new Date(call.createdAt).toLocaleString()) : '—'}</td>
      <td>${escapeHtml(call.summary || '—')}</td>
    </tr>
  `).join('');
  elements.transcriptCallSelect.innerHTML = state.calls.map(call =>
    `<option value="${escapeHtml(call.id)}">${escapeHtml(call.leadName || 'Lead')} · #${escapeHtml(call.id)} · ${escapeHtml(call.status || '—')}</option>`
  ).join('');
  fillTranscriptForm();
  elements.allCallsTable.querySelectorAll('tr[data-call-id]').forEach(row => {
    row.addEventListener('click', () => {
      elements.transcriptCallSelect.value = row.dataset.callId;
      fillTranscriptForm();
      elements.transcriptText.focus();
    });
  });
}

function fillTranscriptForm() {
  const call = state.calls.find(item => String(item.id) === elements.transcriptCallSelect.value);
  if (!call) return;
  elements.transcriptText.value = call.conversation || call.transcript || '';
  elements.transcriptSummary.value = call.summary || '';
  elements.transcriptRecordingUrl.value = call.recordingUrl || '';
  const status = String(call.status || 'COMPLETED').toUpperCase().replace(/-/g, '_');
  const supportedStatus = [...elements.transcriptStatus.options].some(option => option.value === status);
  elements.transcriptStatus.value = supportedStatus ? status : 'COMPLETED';
}

async function startCallFlow(event) {
  event.preventDefault();
  const lead = state.leads.find(item => String(item.id) === elements.callFlowLead.value);
  if (!lead) return setFormMessage(elements.callFlowMessage, 'Select a lead.', true);
  if (!window.confirm(`Place a call to ${lead.name} at ${lead.phone}?`)) return;
  try {
    const result = await fetchJson(`${apiBase}/call-flow/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: lead.phone, leadName: lead.name, leadId: lead.id, type: elements.callFlowType.value })
    });
    setFormMessage(elements.callFlowMessage, `Call created: ${result.status || 'QUEUED'} · ${result.callSid || result.provider || ''}`);
    await loadAllCallHistory();
  } catch (error) {
    setFormMessage(elements.callFlowMessage, error.message || 'Unable to start call.', true);
  }
}

async function buildCallPlan(event) {
  event.preventDefault();
  try {
    const result = await fetchJson(`${apiBase}/leads/call-plan`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: elements.planPhone.value, leadName: elements.planName.value, source: elements.planSource.value })
    });
    elements.callPlanResult.textContent = JSON.stringify(result, null, 2);
  } catch (error) {
    elements.callPlanResult.textContent = error.message || 'Unable to create call plan.';
  }
}

async function runScenario(event) {
  event.preventDefault();
  const lead = state.leads.find(item => String(item.id) === elements.scenarioLead.value);
  if (!lead) {
    elements.scenarioResult.textContent = 'Select a lead first.';
    return;
  }
  if (!window.confirm(`Run the scenario and place an outbound call to ${lead.name}?`)) return;
  try {
    const result = await fetchJson(`${apiBase}/call-flow/scenario`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        leadId: lead.id,
        phone: lead.phone,
        leadName: lead.name,
        transcript: elements.scenarioTranscript.value,
        playbookName: elements.scenarioPlaybook.value,
        userId: 0,
        salesSummary: elements.scenarioSummary.value
      })
    });
    elements.scenarioResult.textContent = JSON.stringify(result, null, 2);
    await loadDashboard();
    await loadAllCallHistory();
  } catch (error) {
    elements.scenarioResult.textContent = error.message || 'Unable to run scenario.';
  }
}

async function saveTranscript(event) {
  event.preventDefault();
  const callId = elements.transcriptCallSelect.value;
  if (!callId) return setFormMessage(elements.transcriptMessage, 'Choose a call first.', true);
  try {
    await fetchJson(`${apiBase}/calls/${callId}/transcript`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        transcript: elements.transcriptText.value,
        summary: elements.transcriptSummary.value,
        recordingUrl: elements.transcriptRecordingUrl.value,
        status: elements.transcriptStatus.value
      })
    });
    setFormMessage(elements.transcriptMessage, 'Transcript saved.');
    await loadAllCallHistory();
  } catch (error) {
    setFormMessage(elements.transcriptMessage, error.message || 'Unable to save transcript.', true);
  }
}

async function loadCampaigns() {
  try {
    state.campaigns = await fetchJson(`${apiBase}/campaigns`) || [];
    elements.campaignList.innerHTML = state.campaigns.length ? state.campaigns.map(campaign => `
      <div class="record-item"><strong>${escapeHtml(campaign.name || 'Campaign')}</strong>
        <span class="record-meta">${escapeHtml(campaign.status || 'DRAFT')} · Playbook version ${escapeHtml(campaign.playbookVersionId || '—')}</span>
        <span class="record-meta">Call window ${escapeHtml(campaign.callWindow || '—')} · ${escapeHtml(campaign.retryPolicy || '—')}</span>
      </div>
    `).join('') : '<div class="muted">No campaigns created.</div>';
  } catch (error) {
    elements.campaignList.textContent = error.message || 'Unable to load campaigns.';
  }
}

async function createCampaign(event) {
  event.preventDefault();
  try {
    await fetchJson(`${apiBase}/campaigns`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: elements.campaignName.value,
        playbookVersionId: Number(elements.campaignPlaybookVersion.value),
        callSchedule: { timezone: 'Asia/Dubai', days: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], start: elements.campaignStart.value, end: elements.campaignEnd.value },
        retryPolicy: { maxAttempts: Number(elements.campaignAttempts.value), minimumHoursBetweenAttempts: 24 }
      })
    });
    elements.campaignForm.reset();
    elements.campaignStart.value = '09:00';
    elements.campaignEnd.value = '18:00';
    elements.campaignAttempts.value = '2';
    setFormMessage(elements.campaignMessage, 'Campaign draft created.');
    await loadCampaigns();
  } catch (error) {
    setFormMessage(elements.campaignMessage, error.message || 'Unable to create campaign.', true);
  }
}

async function loadPlaybooks() {
  try {
    state.playbooks = await fetchJson(`${apiBase}/playbooks`) || [];
    const versions = state.playbooks.flatMap(playbook => (playbook.versions || []).map(version => ({
      id: version.id,
      label: `${playbook.name} · ${version.version || `v${version.id}`}`
    })));
    elements.campaignPlaybookVersion.innerHTML = versions.length
      ? versions.map(version => `<option value="${escapeHtml(version.id)}">${escapeHtml(version.label)}</option>`).join('')
      : '<option value="">Create a playbook version first</option>';
    elements.playbookList.innerHTML = state.playbooks.length ? state.playbooks.map(playbook => `
      <div class="record-item"><strong>${escapeHtml(playbook.name || 'Playbook')}</strong>
        <span class="record-meta">${escapeHtml(playbook.industry || '—')} · ${escapeHtml(playbook.status || 'DRAFT')}</span>
        ${Array.isArray(playbook.versions) ? playbook.versions.map(version => `<span class="record-meta">Version ${escapeHtml(version.version || version.id)} · ${escapeHtml(version.approvalState || 'DRAFT')}</span>`).join('') : ''}
      </div>
    `).join('') : '<div class="muted">No playbooks created.</div>';
  } catch (error) {
    elements.playbookList.textContent = error.message || 'Unable to load playbooks.';
  }
}

async function createPlaybook(event) {
  event.preventDefault();
  try {
    await fetchJson(`${apiBase}/playbooks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: elements.playbookName.value,
        industry: elements.playbookIndustry.value,
        status: elements.playbookStatus.value,
        version: elements.playbookVersion.value,
        configurationJson: elements.playbookConfiguration.value || null,
        approvalState: elements.playbookApproval.value
      })
    });
    elements.playbookForm.reset();
    elements.playbookIndustry.value = 'REAL_ESTATE';
    setFormMessage(elements.playbookMessage, 'Playbook created.');
    await loadPlaybooks();
  } catch (error) {
    setFormMessage(elements.playbookMessage, error.message || 'Unable to create playbook.', true);
  }
}

async function addSuppression(event) {
  event.preventDefault();
  try {
    const result = await fetchJson(`${apiBase}/suppressions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: elements.suppressionPhone.value, reason: elements.suppressionReason.value, scope: elements.suppressionScope.value, source: elements.suppressionSource.value })
    });
    state.suppressions.unshift(result);
    renderSuppressions();
    elements.suppressionForm.reset();
    elements.suppressionScope.value = 'ALL_CAMPAIGNS';
    elements.suppressionSource.value = 'dashboard';
    setFormMessage(elements.suppressionMessage, 'Number added to suppression list.');
    await loadSuppressions();
  } catch (error) {
    setFormMessage(elements.suppressionMessage, error.message || 'Unable to add suppression.', true);
  }
}

async function loadSuppressions() {
  try {
    state.suppressions = await fetchJson(`${apiBase}/suppressions`) || [];
    renderSuppressions();
  } catch (error) {
    elements.suppressionResults.textContent = error.message || 'Unable to load suppressions.';
  }
}

function renderSuppressions() {
  elements.suppressionResults.innerHTML = state.suppressions.map(entry => `
    <div class="record-item"><strong>${escapeHtml(entry.phone)}</strong>
      <span class="record-meta">${escapeHtml(entry.scope)} · ${escapeHtml(entry.reason)}</span>
    </div>
  `).join('');
}

async function loadComplianceCallLog() {
  elements.complianceCallLog.textContent = 'Loading call-log report...';
  try {
    const report = await fetchJson(`${apiBase}/compliance/call-log`);
    elements.complianceCallLog.textContent = JSON.stringify(report, null, 2);
  } catch (error) {
    elements.complianceCallLog.textContent = error.message || 'Unable to load call-log report.';
  }
}

async function selectBriefLead() {
  const lead = state.leads.find(item => String(item.id) === elements.briefLeadSelect.value);
  if (!lead) return;
  elements.briefSummary.value = lead.summary || '';
  elements.briefOutput.textContent = 'No brief generated yet.';
  try {
    const calls = await fetchJson(`${apiBase}/calls/lead/${lead.id}`);
    const latestCall = calls && calls.length ? calls[calls.length - 1] : null;
    elements.briefText.value = latestCall?.conversation || latestCall?.transcript || lead.summary || '';
  } catch (error) {
    elements.briefText.value = lead.summary || '';
  }
}

function bindEvents() {
  elements.authForm.addEventListener('submit', event => {
    event.preventDefault();
    login();
  });

  elements.logoutBtn.addEventListener('click', logout);
  elements.navItems.forEach(button => button.addEventListener('click', () => showView(button.dataset.view)));
  elements.menuToggle.addEventListener('click', () => document.getElementById('appSidebar').classList.toggle('menu-open'));
  elements.createLeadForm.addEventListener('submit', createLead);
  elements.importLeadsForm.addEventListener('submit', importLeads);
  elements.lookupLeadForm.addEventListener('submit', lookupLead);
  elements.qualifyLeadForm.addEventListener('submit', qualifySelectedLead);
  elements.assignLeadForm.addEventListener('submit', assignSelectedLead);
  elements.callFlowForm.addEventListener('submit', startCallFlow);
  elements.callPlanForm.addEventListener('submit', buildCallPlan);
  elements.scenarioForm.addEventListener('submit', runScenario);
  elements.transcriptForm.addEventListener('submit', saveTranscript);
  elements.transcriptCallSelect.addEventListener('change', fillTranscriptForm);
  elements.refreshCallsBtn.addEventListener('click', loadAllCallHistory);
  elements.campaignForm.addEventListener('submit', createCampaign);
  elements.refreshCampaignsBtn.addEventListener('click', loadCampaigns);
  elements.playbookForm.addEventListener('submit', createPlaybook);
  elements.refreshPlaybooksBtn.addEventListener('click', loadPlaybooks);
  elements.suppressionForm.addEventListener('submit', addSuppression);
  elements.refreshComplianceBtn.addEventListener('click', loadComplianceCallLog);
  elements.briefLeadSelect.addEventListener('change', selectBriefLead);
  elements.salesBriefForm.addEventListener('submit', event => {
    event.preventDefault();
    generateBrief();
  });
  elements.exportExcelBtn.addEventListener('click', exportLeadTableToExcel);
  elements.exportPdfBtn.addEventListener('click', () => {
    const lead = state.leads.find(item => Number(item.id) === Number(state.selectedLeadId));
    if (!lead) {
      showAlert('Select a lead before exporting PDF.');
      return;
    }

    const { jsPDF } = window.jspdf;
    const doc = new jsPDF();
    doc.setFontSize(18);
    doc.text('AI Lead Agent Lead Summary', 14, 18);
    doc.setFontSize(11);
    doc.text(`Name: ${lead.name || 'N/A'}`, 14, 32);
    doc.text(`Phone: ${lead.phone || 'N/A'}`, 14, 40);
    doc.text(`Source: ${lead.source || 'N/A'}`, 14, 48);
    doc.text(`Status: ${lead.status || 'NEW'}`, 14, 56);
    doc.text(`Score: ${lead.scoreBand || 'COLD'}`, 14, 64);
    doc.text(`Summary: ${lead.summary || '—'}`, 14, 72, { maxWidth: 180 });
    doc.save(`${(lead.name || 'lead').replace(/\s+/g, '-').toLowerCase()}.pdf`);
  });

  elements.searchInput.addEventListener('input', updateFilters);
  elements.statusFilter.addEventListener('change', updateFilters);
  elements.priorityFilter.addEventListener('change', updateFilters);
  elements.sourceFilter.addEventListener('change', updateFilters);
  elements.followUpFilter.addEventListener('change', updateFilters);
  elements.exportBriefPdfBtn.addEventListener('click', exportBriefPdf);
  elements.startCallBtn.addEventListener('click', startCall);
  elements.saveActivityBtn.addEventListener('click', saveLeadActivity);
}

bindEvents();
showLogin();
