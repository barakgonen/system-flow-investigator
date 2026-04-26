const mqttTopicsList = document.getElementById('mqttTopicsList');
const wsChannelsList = document.getElementById('wsChannelsList');
const topicsCount = document.getElementById('topicsCount');
const eventsCount = document.getElementById('eventsCount');
const selectedTopicLabel = document.getElementById('selectedTopicLabel');
const eventsTableBody = document.getElementById('eventsTableBody');
const eventDetails = document.getElementById('eventDetails');
const streamStatus = document.getElementById('streamStatus');

const refreshTopicsBtn = document.getElementById('refreshTopicsBtn');
const loadRecentBtn = document.getElementById('loadRecentBtn');
const connectStreamBtn = document.getElementById('connectStreamBtn');
const disconnectStreamBtn = document.getElementById('disconnectStreamBtn');
const textFilterInput = document.getElementById('textFilter');

const toggleLiveEventsBtn = document.getElementById('toggleLiveEventsBtn');
const liveEventsBody = document.getElementById('liveEventsBody');
const liveEventsPanel = document.getElementById('liveEventsPanel');

const traceIdInput = document.getElementById('traceIdInput');
const inspectTraceBtn = document.getElementById('inspectTraceBtn');
const traceSummary = document.getElementById('traceSummary');
const traceTimeline = document.getElementById('traceTimeline');
const toggleTraceBtn = document.getElementById('toggleTraceBtn');
const traceBody = document.getElementById('traceBody');
const tracePanel = document.getElementById('tracePanel');
const flowStatus = document.getElementById('flowStatus');
const flowGraph = document.getElementById('flowGraph');

const flowConfigPanel = document.getElementById('flowConfigPanel');
const toggleConfigBtn = document.getElementById('toggleConfigBtn');
const configBody = document.getElementById('configBody');
const loadConfigBtn = document.getElementById('loadConfigBtn');
const saveConfigBtn = document.getElementById('saveConfigBtn');
const exportConfigBtn = document.getElementById('exportConfigBtn');
const importConfigBtn = document.getElementById('importConfigBtn');
const importConfigFile = document.getElementById('importConfigFile');
const configNameInput = document.getElementById('configNameInput');
const configDescriptionInput = document.getElementById('configDescriptionInput');
const maxStepDurationInput = document.getElementById('maxStepDurationInput');
const allowExtraEventsInput = document.getElementById('allowExtraEventsInput');
const addFlowStepBtn = document.getElementById('addFlowStepBtn');
const flowStepsEditor = document.getElementById('flowStepsEditor');
const configMessage = document.getElementById('configMessage');

const SLOW_STEP_THRESHOLD_MS = 50;

let eventSource = null;
let cachedEvents = [];
let cachedMqttTopics = [];
let cachedWsChannels = [];
let selectedChannels = new Set();

let liveEventsCollapsed = false;
let traceCollapsed = false;
let configCollapsed = false;
let currentConfig = null;

function setStreamStatus(state) {
    streamStatus.className = `status-badge ${state}`;

    if (state === 'connected') {
        streamStatus.textContent = 'Connected';
    } else if (state === 'connecting') {
        streamStatus.textContent = 'Connecting';
    } else {
        streamStatus.textContent = 'Disconnected';
    }

    connectStreamBtn.disabled = state === 'connecting' || state === 'connected';
    disconnectStreamBtn.disabled = state === 'disconnected';
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, options);
    if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
    }
    return response.json();
}

function updateSelectionLabel() {
    selectedTopicLabel.textContent = selectedChannels.size === 0
        ? 'All channels'
        : `${selectedChannels.size} selected`;
}

function appendRepeatedParams(params, name, values) {
    for (const value of values) {
        params.append(name, value);
    }
}

function buildRecentUrl() {
    const params = new URLSearchParams();

    appendRepeatedParams(params, 'channel', Array.from(selectedChannels));

    const textFilter = textFilterInput.value.trim();
    if (textFilter) {
        params.set('textContains', textFilter);
    }

    const query = params.toString();
    return `/api/events/recent${query ? `?${query}` : ''}`;
}

function buildStreamUrl() {
    const params = new URLSearchParams();

    appendRepeatedParams(params, 'channel', Array.from(selectedChannels));

    const textFilter = textFilterInput.value.trim();
    if (textFilter) {
        params.set('textContains', textFilter);
    }

    params.set('_ts', String(Date.now()));
    return `/api/stream/events?${params.toString()}`;
}

function escapeHtml(text) {
    return String(text ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;');
}

function formatTime(value) {
    if (!value) return '-';
    try {
        return new Date(value).toLocaleTimeString();
    } catch {
        return value;
    }
}

function formatDateTime(value) {
    if (!value) return '-';
    try {
        return new Date(value).toISOString();
    } catch {
        return value;
    }
}

function formatDuration(value) {
    if (value === null || value === undefined) {
        return '-';
    }
    return `${value} ms`;
}

function payloadPreview(payload) {
    if (!payload) return '';
    return payload.length > 120 ? payload.slice(0, 120) + '...' : payload;
}

function prettyPayload(payload) {
    if (!payload) return '';
    try {
        return JSON.stringify(JSON.parse(payload), null, 2);
    } catch {
        return payload;
    }
}

function eventObservedAt(event) {
    return event.observedAt || event.receivedAt || event.timestamp;
}

function renderEvents(events) {
    cachedEvents = events;
    eventsTableBody.innerHTML = '';
    eventsCount.textContent = String(events.length);

    const ordered = [...events].reverse();

    for (const event of ordered) {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${escapeHtml(formatTime(eventObservedAt(event)))}</td>
            <td>${escapeHtml(event.protocol || '-')}</td>
            <td>${escapeHtml(event.channel || '-')}</td>
            <td>${escapeHtml(event.traceId || '-')}</td>
            <td class="payload-preview">${escapeHtml(payloadPreview(event.payload || ''))}</td>
        `;

        tr.onclick = () => {
            document.querySelectorAll('#eventsTableBody tr')
                .forEach(row => row.classList.remove('selected-row'));

            tr.classList.add('selected-row');
            eventDetails.textContent = JSON.stringify(event, null, 2);

            if (event.traceId) {
                traceIdInput.value = event.traceId;
                inspectTrace();
            }
        };

        eventsTableBody.appendChild(tr);
    }
}

function renderChannelGroup(container, items, label) {
    container.innerHTML = '';

    const allBtn = document.createElement('button');
    allBtn.className = `topic-chip ${selectedChannels.size === 0 ? 'active' : ''}`;
    allBtn.textContent = `All ${label}`;
    allBtn.onclick = async () => {
        selectedChannels.clear();
        renderAllChannels();
        updateSelectionLabel();
        await loadRecent();
        reconnectStreamIfConnected();
    };
    container.appendChild(allBtn);

    for (const item of items) {
        const btn = document.createElement('button');
        btn.className = `topic-chip ${selectedChannels.has(item) ? 'active' : ''}`;
        btn.textContent = item;
        btn.onclick = async () => {
            if (selectedChannels.has(item)) {
                selectedChannels.delete(item);
            } else {
                selectedChannels.add(item);
            }

            renderAllChannels();
            updateSelectionLabel();
            await loadRecent();
            reconnectStreamIfConnected();
        };
        container.appendChild(btn);
    }
}

function renderAllChannels() {
    renderChannelGroup(mqttTopicsList, cachedMqttTopics, 'MQTT');
    renderChannelGroup(wsChannelsList, cachedWsChannels, 'WS');
}

async function refreshChannels() {
    try {
        const [mqttTopics, wsChannels] = await Promise.all([
            fetchJson('/api/events/mqtt/topics'),
            fetchJson('/api/events/ws/channels')
        ]);

        cachedMqttTopics = mqttTopics;
        cachedWsChannels = wsChannels;

        topicsCount.textContent = String(mqttTopics.length);
        renderAllChannels();
        updateSelectionLabel();
    } catch (e) {
        console.error('Failed loading channels', e);
    }
}

async function loadRecent() {
    try {
        const events = await fetchJson(buildRecentUrl());
        renderEvents(events);
        await loadSummary();
    } catch (e) {
        console.error('Failed loading recent events', e);
    }
}

function disconnectStreamInternal() {
    if (eventSource) {
        eventSource.onopen = null;
        eventSource.onmessage = null;
        eventSource.onerror = null;
        eventSource.close();
        eventSource = null;
    }
}

function connectStream() {
    disconnectStreamInternal();

    const url = buildStreamUrl();
    setStreamStatus('connecting');

    const es = new EventSource(url);
    eventSource = es;

    let activated = false;

    es.onopen = () => {
        if (eventSource !== es) return;
        activated = true;
        setStreamStatus('connected');
    };

    es.onmessage = (event) => {
        if (eventSource !== es) return;

        if (!activated) {
            activated = true;
            setStreamStatus('connected');
        }

        if (!event.data) return;

        try {
            const parsed = JSON.parse(event.data);
            cachedEvents.push(parsed);

            if (cachedEvents.length > 500) {
                cachedEvents = cachedEvents.slice(cachedEvents.length - 500);
            }

            renderEvents(cachedEvents);
        } catch (e) {
            console.error('Failed parsing SSE payload', e, event.data);
        }
    };

    es.addEventListener('heartbeat', () => {
        if (eventSource !== es) return;

        if (!activated) {
            activated = true;
            setStreamStatus('connected');
        }
    });

    es.onerror = () => {
        if (eventSource !== es) return;
        disconnectStreamInternal();
        setStreamStatus('disconnected');
    };
}

function disconnectStream() {
    disconnectStreamInternal();
    setStreamStatus('disconnected');
}

function reconnectStreamIfConnected() {
    if (streamStatus.textContent === 'Connected' || streamStatus.textContent === 'Connecting') {
        connectStream();
    }
}

async function loadSummary() {
    try {
        const summary = await fetchJson('/api/dashboard/summary');
        topicsCount.textContent = String(summary.observedTopicCount ?? 0);
        eventsCount.textContent = String(summary.recentEventCount ?? 0);
    } catch (e) {
        console.error('Failed loading summary', e);
    }
}

async function inspectTrace() {
    const traceId = traceIdInput.value.trim();

    if (!traceId) {
        traceSummary.textContent = 'Please enter a traceId.';
        traceTimeline.innerHTML = '';
        flowStatus.classList.add('hidden');
        flowGraph.classList.add('hidden');
        return;
    }

    try {
        const trace = await fetchJson(`/api/correlation/trace/${encodeURIComponent(traceId)}`);
        renderTrace(trace);
    } catch (e) {
        console.error('Failed loading trace', e);
        traceSummary.textContent = 'Failed loading trace.';
        traceTimeline.innerHTML = '';
        flowStatus.classList.add('hidden');
        flowGraph.classList.add('hidden');
    }
}

function renderTrace(trace) {
    traceTimeline.innerHTML = '';

    if (!trace.events || trace.events.length === 0) {
        traceSummary.innerHTML = `<strong>${escapeHtml(trace.traceId || '-')}</strong> | no events found`;

        if (trace.validation) {
            renderFlowStatus(trace.validation);
            renderFlowGraph(trace.validation);
        } else {
            flowStatus.classList.add('hidden');
            flowGraph.classList.add('hidden');
        }

        return;
    }

    traceSummary.innerHTML = `
        <strong>${escapeHtml(trace.traceId || '-')}</strong>
        <span>${trace.eventCount} events</span>
        <span>Source duration: ${formatDuration(trace.totalSourceDurationMs)}</span>
        <span>Observed duration: ${formatDuration(trace.totalObservedDurationMs)}</span>
    `;

    if (trace.validation) {
        renderFlowStatus(trace.validation);
        renderFlowGraph(trace.validation);
    } else {
        flowStatus.classList.add('hidden');
        flowGraph.classList.add('hidden');
    }

    for (const event of trace.events) {
        const item = document.createElement('div');
        item.className = 'trace-step';

        if ((event.deltaFromPreviousSourceMs ?? 0) > SLOW_STEP_THRESHOLD_MS) {
            item.classList.add('slow-step');
        }

        item.innerHTML = `
            <div class="trace-index">${escapeHtml(event.index ?? '-')}</div>
            <div class="trace-content">
                <div class="trace-title">
                    <span class="protocol-pill ${escapeHtml((event.protocol || '').toLowerCase())}">
                        ${escapeHtml(event.protocol || '-')}
                    </span>
                    <strong>${escapeHtml(event.channel || '-')}</strong>
                </div>

                <div class="trace-time">
                    <span>Source sent: ${escapeHtml(formatDateTime(event.sourceSentAt))}</span>
                    <span>Observed: ${escapeHtml(formatDateTime(event.observedAt))}</span>
                </div>

                <div class="trace-delta">
                    <span>Δ source: ${formatDuration(event.deltaFromPreviousSourceMs)}</span>
                    <span>Δ observed: ${formatDuration(event.deltaFromPreviousObservedMs)}</span>
                </div>

                <pre>${escapeHtml(prettyPayload(event.payload))}</pre>
            </div>
        `;

        traceTimeline.appendChild(item);
    }
}

function renderFlowStatus(validation) {
    flowStatus.classList.remove('hidden', 'flow-ok', 'flow-broken', 'flow-warning');

    const status = validation.status || 'UNKNOWN';

    if (status === 'COMPLETE') {
        flowStatus.classList.add('flow-ok');
    } else if (status === 'COMPLETE_WITH_UNEXPECTED_EVENTS') {
        flowStatus.classList.add('flow-warning');
    } else {
        flowStatus.classList.add('flow-broken');
    }

    const missingText = validation.missingChannels && validation.missingChannels.length > 0
        ? `Missing: ${validation.missingChannels.join(', ')}`
        : 'No missing expected steps.';

    const extraText = validation.extraChannels && validation.extraChannels.length > 0
        ? `Extra observed: ${validation.extraChannels.join(', ')}`
        : '';

    const title = status === 'COMPLETE'
        ? '✅ Complete flow'
        : status === 'COMPLETE_WITH_UNEXPECTED_EVENTS'
            ? '⚠️ Complete with unexpected events'
            : status === 'NO_CONFIG'
                ? '⚙️ No flow config'
                : '❌ Broken flow';

    flowStatus.innerHTML = `
        <strong>${escapeHtml(title)}</strong>
        <span>${escapeHtml(validation.message || '')}</span>
        <small>${escapeHtml(missingText)}</small>
        ${extraText ? `<small>${escapeHtml(extraText)}</small>` : ''}
    `;
}

function renderFlowGraph(validation) {
    flowGraph.classList.remove('hidden');
    flowGraph.innerHTML = '';

    const steps = validation.steps || [];

    if (steps.length === 0) {
        flowGraph.innerHTML = `
            <div class="flow-extra">
                <strong>No expected flow configured</strong>
                <span>Configure expected steps to enable flow validation.</span>
            </div>
        `;
        return;
    }

    steps.forEach((step, index) => {
        const node = document.createElement('div');
        node.className = `flow-node ${step.found ? 'found' : 'missing'}`;

        node.innerHTML = `
            <div class="flow-node-label">${escapeHtml(step.label || `Step ${step.index}`)}</div>
            <div class="flow-node-channel">${escapeHtml(step.channel || '-')}</div>
            <div class="flow-node-protocol">${escapeHtml(step.protocol || '-')}</div>
            <div class="flow-node-time">
                ${step.found ? `Δ ${formatDuration(step.deltaFromPreviousSourceMs)}` : 'Missing'}
            </div>
            ${step.found ? `<div class="flow-node-observed">obs Δ ${formatDuration(step.deltaFromPreviousObservedMs)}</div>` : ''}
        `;

        flowGraph.appendChild(node);

        if (index < steps.length - 1) {
            const next = steps[index + 1];
            const arrow = document.createElement('div');
            arrow.className = `flow-arrow ${step.found && next.found ? 'connected' : 'broken'}`;
            arrow.innerHTML = '→';
            flowGraph.appendChild(arrow);
        }
    });

    if (validation.extraChannels && validation.extraChannels.length > 0) {
        const extra = document.createElement('div');
        extra.className = 'flow-extra';
        extra.innerHTML = `
            <strong>Extra observed channels</strong>
            <span>${escapeHtml(validation.extraChannels.join(', '))}</span>
        `;
        flowGraph.appendChild(extra);
    }
}

async function loadConfig() {
    try {
        const config = await fetchJson('/api/investigation/config');
        currentConfig = normalizeConfig(config);
        renderConfigEditor(currentConfig);
        showConfigMessage('Config loaded.', 'ok');
    } catch (e) {
        console.error('Failed loading config', e);
        showConfigMessage('Failed loading config.', 'error');
    }
}

async function saveConfig() {
    try {
        const config = readConfigFromEditor();

        const saved = await fetchJson('/api/investigation/config', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(config)
        });

        currentConfig = normalizeConfig(saved);
        renderConfigEditor(currentConfig);
        showConfigMessage('Config saved.', 'ok');

        if (traceIdInput.value.trim()) {
            inspectTrace();
        }
    } catch (e) {
        console.error('Failed saving config', e);
        showConfigMessage('Failed saving config.', 'error');
    }
}

function normalizeConfig(config) {
    return {
        name: config?.name || '',
        description: config?.description || '',
        steps: Array.isArray(config?.steps) ? config.steps : [],
        rules: {
            maxStepDurationMs: config?.rules?.maxStepDurationMs ?? 50,
            allowExtraEvents: config?.rules?.allowExtraEvents ?? true
        }
    };
}

function renderConfigEditor(config) {
    configNameInput.value = config.name || '';
    configDescriptionInput.value = config.description || '';
    maxStepDurationInput.value = String(config.rules?.maxStepDurationMs ?? 50);
    allowExtraEventsInput.checked = Boolean(config.rules?.allowExtraEvents ?? true);

    flowStepsEditor.innerHTML = '';

    const steps = [...(config.steps || [])]
        .sort((a, b) => Number(a.index ?? 0) - Number(b.index ?? 0));

    steps.forEach((step, idx) => {
        flowStepsEditor.appendChild(createStepEditorRow({
            index: idx + 1,
            protocol: step.protocol || '',
            channel: step.channel || '',
            label: step.label || ''
        }));
    });
}

function createStepEditorRow(step) {
    const row = document.createElement('div');
    row.className = 'flow-step-row';

    row.innerHTML = `
        <div class="step-index">${step.index}</div>

        <select class="step-protocol">
            <option value="MQTT" ${step.protocol === 'MQTT' ? 'selected' : ''}>MQTT</option>
            <option value="WS" ${step.protocol === 'WS' ? 'selected' : ''}>WS</option>
        </select>

        <input class="step-channel" type="text" placeholder="channel/topic" value="${escapeHtml(step.channel)}">
        <input class="step-label" type="text" placeholder="label" value="${escapeHtml(step.label)}">

        <button class="secondary-button remove-step-btn">Remove</button>
    `;

    row.querySelector('.remove-step-btn').onclick = () => {
        row.remove();
        reindexStepRows();
    };

    return row;
}

function reindexStepRows() {
    [...flowStepsEditor.querySelectorAll('.flow-step-row')].forEach((row, index) => {
        row.querySelector('.step-index').textContent = String(index + 1);
    });
}

function addFlowStep() {
    const nextIndex = flowStepsEditor.querySelectorAll('.flow-step-row').length + 1;

    flowStepsEditor.appendChild(createStepEditorRow({
        index: nextIndex,
        protocol: 'MQTT',
        channel: '',
        label: ''
    }));
}

function readConfigFromEditor() {
    const steps = [...flowStepsEditor.querySelectorAll('.flow-step-row')].map((row, index) => ({
        index: index + 1,
        protocol: row.querySelector('.step-protocol').value,
        channel: row.querySelector('.step-channel').value.trim(),
        label: row.querySelector('.step-label').value.trim()
    })).filter(step => step.channel);

    return {
        name: configNameInput.value.trim(),
        description: configDescriptionInput.value.trim(),
        steps,
        rules: {
            maxStepDurationMs: Number(maxStepDurationInput.value || 0),
            allowExtraEvents: allowExtraEventsInput.checked
        }
    };
}

function exportConfig() {
    const config = readConfigFromEditor();
    const blob = new Blob([JSON.stringify(config, null, 2)], {
        type: 'application/json'
    });

    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = url;
    link.download = `${config.name || 'investigation-config'}.json`;
    link.click();

    URL.revokeObjectURL(url);
    showConfigMessage('Config exported.', 'ok');
}

function importConfigFromFile(file) {
    const reader = new FileReader();

    reader.onload = async () => {
        try {
            const parsed = JSON.parse(reader.result);
            currentConfig = normalizeConfig(parsed);
            renderConfigEditor(currentConfig);
            await saveConfig();
            showConfigMessage('Config imported and saved.', 'ok');
        } catch (e) {
            console.error('Failed importing config', e);
            showConfigMessage('Invalid config file.', 'error');
        }
    };

    reader.readAsText(file);
}

function showConfigMessage(message, type) {
    configMessage.textContent = message;
    configMessage.className = `config-message ${type || ''}`;
}

function toggleConfig() {
    configCollapsed = !configCollapsed;

    configBody.classList.toggle('collapsed', configCollapsed);
    flowConfigPanel.classList.toggle('panel-collapsed', configCollapsed);

    toggleConfigBtn.textContent = configCollapsed ? 'Expand' : 'Collapse';
}

function toggleLiveEvents() {
    liveEventsCollapsed = !liveEventsCollapsed;

    liveEventsBody.classList.toggle('collapsed', liveEventsCollapsed);
    liveEventsPanel.classList.toggle('panel-collapsed', liveEventsCollapsed);

    toggleLiveEventsBtn.textContent = liveEventsCollapsed ? 'Expand' : 'Collapse';
}

function toggleTrace() {
    traceCollapsed = !traceCollapsed;

    traceBody.classList.toggle('collapsed', traceCollapsed);
    tracePanel.classList.toggle('panel-collapsed', traceCollapsed);

    toggleTraceBtn.textContent = traceCollapsed ? 'Expand' : 'Collapse';
}

refreshTopicsBtn.addEventListener('click', async () => {
    await refreshChannels();
    await loadRecent();
});

loadRecentBtn.addEventListener('click', async () => {
    await loadRecent();
});

connectStreamBtn.addEventListener('click', () => {
    connectStream();
});

disconnectStreamBtn.addEventListener('click', () => {
    disconnectStream();
});

textFilterInput.addEventListener('change', async () => {
    await loadRecent();
    reconnectStreamIfConnected();
});

inspectTraceBtn.addEventListener('click', () => {
    inspectTrace();
});

traceIdInput.addEventListener('keydown', event => {
    if (event.key === 'Enter') {
        inspectTrace();
    }
});

toggleLiveEventsBtn.addEventListener('click', () => {
    toggleLiveEvents();
});

toggleTraceBtn.addEventListener('click', () => {
    toggleTrace();
});

toggleConfigBtn.addEventListener('click', () => {
    toggleConfig();
});

loadConfigBtn.addEventListener('click', () => {
    loadConfig();
});

saveConfigBtn.addEventListener('click', () => {
    saveConfig();
});

exportConfigBtn.addEventListener('click', () => {
    exportConfig();
});

importConfigBtn.addEventListener('click', () => {
    importConfigFile.click();
});

importConfigFile.addEventListener('change', event => {
    const file = event.target.files?.[0];
    if (file) {
        importConfigFromFile(file);
    }
    importConfigFile.value = '';
});

addFlowStepBtn.addEventListener('click', () => {
    addFlowStep();
});

(async function init() {
    setStreamStatus('disconnected');
    await refreshChannels();
    await loadRecent();
    await loadSummary();
    await loadConfig();
    connectStream();
})();