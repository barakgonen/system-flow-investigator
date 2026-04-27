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

const sessionSelect = document.getElementById('sessionSelect');
const importSessionBtn = document.getElementById('importSessionBtn');
const importSessionFile = document.getElementById('importSessionFile');
const downloadSessionBtn = document.getElementById('downloadSessionBtn');

const traceIdInput = document.getElementById('traceIdInput');
const traceFlowSelect = document.getElementById('traceFlowSelect');
const inspectTraceBtn = document.getElementById('inspectTraceBtn');
const traceSummary = document.getElementById('traceSummary');
const traceTimeline = document.getElementById('traceTimeline');
const flowStatus = document.getElementById('flowStatus');
const flowGraph = document.getElementById('flowGraph');

const loadConfigBtn = document.getElementById('loadConfigBtn');
const saveConfigBtn = document.getElementById('saveConfigBtn');
const exportConfigBtn = document.getElementById('exportConfigBtn');
const importConfigBtn = document.getElementById('importConfigBtn');
const importConfigFile = document.getElementById('importConfigFile');

const configNameInput = document.getElementById('configNameInput');
const configDescriptionInput = document.getElementById('configDescriptionInput');
const maxStepDurationInput = document.getElementById('maxStepDurationInput');
const allowExtraEventsInput = document.getElementById('allowExtraEventsInput');

const configFlowSelect = document.getElementById('configFlowSelect');
const addFlowBtn = document.getElementById('addFlowBtn');
const removeFlowBtn = document.getElementById('removeFlowBtn');

const selectedFlowEditor = document.getElementById('selectedFlowEditor');
const flowIdInput = document.getElementById('flowIdInput');
const flowNameInput = document.getElementById('flowNameInput');
const flowDescriptionInput = document.getElementById('flowDescriptionInput');
const addFlowStepBtn = document.getElementById('addFlowStepBtn');
const flowStepsEditor = document.getElementById('flowStepsEditor');
const configMessage = document.getElementById('configMessage');

const SLOW_STEP_THRESHOLD_MS = 50;
const MAX_EVENT_CACHE_SIZE = 2_000;

let eventSource = null;

let allEventsCache = [];
let visibleEvents = [];

let cachedMqttTopics = [];
let cachedWsChannels = [];
let selectedChannels = new Set();

let currentConfig = null;
let selectedConfigFlowId = null;

let currentSession = {
    type: 'LIVE',
    sessionId: null
};

function activateTab(targetId) {
    document.querySelectorAll('.tab').forEach(tab => {
        const active = tab.dataset.tab === targetId;
        tab.classList.toggle('active', active);
        tab.setAttribute('aria-selected', String(active));
    });

    document.querySelectorAll('.tab-panel').forEach(panel => {
        panel.classList.toggle('active-panel', panel.id === targetId);
    });
}

function initTabsAndCollapse() {
    document.querySelectorAll('.tab').forEach(tab => {
        tab.addEventListener('click', () => activateTab(tab.dataset.tab));
    });

    document.querySelectorAll('.collapse-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const body = document.getElementById(btn.dataset.target);
            const panel = document.getElementById(btn.dataset.panel);

            if (!body) return;

            const collapsed = body.classList.toggle('collapsed');

            if (panel) {
                panel.classList.toggle('panel-collapsed', collapsed);
            }

            const chevron = btn.querySelector('.chevron');
            if (chevron) {
                chevron.style.transform = collapsed ? 'rotate(-90deg)' : '';
            }

            const labelNode = [...btn.childNodes].find(node => node.nodeType === Node.TEXT_NODE);
            if (labelNode) {
                labelNode.textContent = collapsed ? ' Expand' : ' Collapse';
            }
        });
    });
}

function setStreamStatus(state) {
    streamStatus.className = `status-badge ${state}`;

    if (state === 'connected') {
        streamStatus.textContent = 'Connected';
    } else if (state === 'connecting') {
        streamStatus.textContent = 'Connecting';
    } else {
        streamStatus.textContent = 'Disconnected';
    }

    connectStreamBtn.disabled = currentSession.type !== 'LIVE' || state === 'connecting' || state === 'connected';
    disconnectStreamBtn.disabled = currentSession.type !== 'LIVE' || state === 'disconnected';
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, options);

    if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
    }

    return response.json();
}

function eventObservedAt(event) {
    return event.observedAt || event.receivedAt || event.timestamp || null;
}

function effectiveEventTime(event) {
    return event.sourceSentAt || eventObservedAt(event) || '';
}

function eventKey(event) {
    return [
        event.traceId || '',
        event.protocol || '',
        event.source || '',
        event.channel || '',
        eventObservedAt(event) || '',
        event.sourceSentAt || '',
        event.payload || ''
    ].join('|');
}

function sortEventsByLogicalTime(events) {
    return [...events].sort((a, b) => {
        const aTime = String(effectiveEventTime(a));
        const bTime = String(effectiveEventTime(b));

        const byTime = aTime.localeCompare(bTime);
        if (byTime !== 0) return byTime;

        return eventKey(a).localeCompare(eventKey(b));
    });
}

function mergeEvents(existing, incoming) {
    const map = new Map();

    for (const event of existing || []) {
        map.set(eventKey(event), event);
    }

    for (const event of incoming || []) {
        map.set(eventKey(event), event);
    }

    const merged = sortEventsByLogicalTime([...map.values()]);

    if (merged.length > MAX_EVENT_CACHE_SIZE) {
        return merged.slice(merged.length - MAX_EVENT_CACHE_SIZE);
    }

    return merged;
}

function resetEventState() {
    allEventsCache = [];
    visibleEvents = [];
    eventsTableBody.innerHTML = '';
    eventsCount.textContent = '0';
}

function updateSelectionLabel() {
    selectedTopicLabel.textContent = selectedChannels.size === 0 ? 'All' : `${selectedChannels.size}`;
}

function appendRepeatedParams(params, name, values) {
    for (const value of values) {
        params.append(name, value);
    }
}

function selectedTraceFlowId() {
    const value = traceFlowSelect.value;
    return value && value.trim() ? value.trim() : null;
}

function buildLiveRecentUrl() {
    return '/api/events/recent';
}

function buildStreamUrl() {
    const params = new URLSearchParams();
    params.set('_ts', String(Date.now()));
    return `/api/stream/events?${params.toString()}`;
}

function buildTraceUrl(traceId) {
    const flowId = selectedTraceFlowId();
    const encodedTraceId = encodeURIComponent(traceId);

    if (currentSession.type === 'IMPORTED') {
        const base = `/api/sessions/imported/${encodeURIComponent(currentSession.sessionId)}/trace/${encodedTraceId}`;
        return flowId ? `${base}?flowId=${encodeURIComponent(flowId)}` : base;
    }

    const base = `/api/correlation/trace/${encodedTraceId}`;
    return flowId ? `${base}?flowId=${encodeURIComponent(flowId)}` : base;
}

function buildSessionDownloadUrl() {
    const params = new URLSearchParams();
    appendRepeatedParams(params, 'channel', Array.from(selectedChannels));

    const query = params.toString();
    return `/api/export/session/download${query ? `?${query}` : ''}`;
}

function escapeHtml(text) {
    return String(text ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;');
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

function eventMatchesUiFilters(event) {
    if (selectedChannels.size > 0 && !selectedChannels.has(event.channel)) {
        return false;
    }

    const textFilter = textFilterInput.value.trim().toLowerCase();

    if (textFilter) {
        return String(event.payload || '').toLowerCase().includes(textFilter)
            || String(event.channel || '').toLowerCase().includes(textFilter)
            || String(event.traceId || '').toLowerCase().includes(textFilter)
            || String(event.protocol || '').toLowerCase().includes(textFilter)
            || String(event.source || '').toLowerCase().includes(textFilter);
    }

    return true;
}

function applyFiltersAndRenderEvents() {
    visibleEvents = allEventsCache.filter(eventMatchesUiFilters);
    renderEventsTable(visibleEvents);
    eventsCount.textContent = String(visibleEvents.length);
}

function renderEventsTable(events) {
    eventsTableBody.innerHTML = '';

    const ordered = [...events].reverse();

    for (const event of ordered) {
        const tr = document.createElement('tr');
        const protocolClass = (event.protocol || '').toLowerCase() === 'ws' ? 'ws' : '';

        tr.innerHTML = `
            <td>${escapeHtml(formatTime(eventObservedAt(event)))}</td>
            <td><span class="protocol-pill ${protocolClass}">${escapeHtml(event.protocol || '-')}</span></td>
            <td>${escapeHtml(event.channel || '-')}</td>
            <td>${escapeHtml(event.traceId || '-')}</td>
            <td class="payload-preview">${escapeHtml(payloadPreview(event.payload || ''))}</td>
        `;

        tr.onclick = () => {
            document.querySelectorAll('#eventsTableBody tr').forEach(row => row.classList.remove('selected-row'));
            tr.classList.add('selected-row');

            eventDetails.textContent = JSON.stringify(event, null, 2);

            if (event.traceId) {
                traceIdInput.value = event.traceId;
                activateTab('tracePanel');
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
    allBtn.onclick = () => {
        selectedChannels.clear();
        renderAllChannels();
        updateSelectionLabel();
        applyFiltersAndRenderEvents();
    };
    container.appendChild(allBtn);

    for (const item of items) {
        const btn = document.createElement('button');
        btn.className = `topic-chip ${selectedChannels.has(item) ? 'active' : ''}`;
        btn.textContent = item;
        btn.onclick = () => {
            if (selectedChannels.has(item)) {
                selectedChannels.delete(item);
            } else {
                selectedChannels.add(item);
            }

            renderAllChannels();
            updateSelectionLabel();
            applyFiltersAndRenderEvents();
        };
        container.appendChild(btn);
    }
}

function renderAllChannels() {
    renderChannelGroup(mqttTopicsList, cachedMqttTopics, 'MQTT');
    renderChannelGroup(wsChannelsList, cachedWsChannels, 'WS');
}

function deriveChannelsFromEvents(events) {
    const mqtt = [];
    const ws = [];

    for (const event of events) {
        if (!event.channel) continue;

        if ((event.protocol || '').toUpperCase() === 'WS') {
            ws.push(event.channel);
        } else {
            mqtt.push(event.channel);
        }
    }

    cachedMqttTopics = [...new Set(mqtt)].sort();
    cachedWsChannels = [...new Set(ws)].sort();
}

async function loadRecent() {
    try {
        if (currentSession.type === 'IMPORTED') {
            const importedEvents = await fetchJson(`/api/sessions/imported/${encodeURIComponent(currentSession.sessionId)}/events`);
            allEventsCache = sortEventsByLogicalTime(importedEvents);
        } else {
            const recentEvents = await fetchJson(buildLiveRecentUrl());
            allEventsCache = mergeEvents(allEventsCache, recentEvents);
        }

        deriveChannelsFromEvents(allEventsCache);
        renderAllChannels();
        updateSelectionLabel();
        applyFiltersAndRenderEvents();
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
    if (currentSession.type !== 'LIVE') {
        disconnectStream();
        return;
    }

    disconnectStreamInternal();

    const es = new EventSource(buildStreamUrl());
    eventSource = es;

    setStreamStatus('connecting');

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

            allEventsCache = mergeEvents(allEventsCache, [parsed]);

            deriveChannelsFromEvents(allEventsCache);
            renderAllChannels();
            updateSelectionLabel();
            applyFiltersAndRenderEvents();
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

async function loadSummary() {
    try {
        if (currentSession.type === 'IMPORTED') {
            topicsCount.textContent = String(cachedMqttTopics.length + cachedWsChannels.length);
            eventsCount.textContent = String(visibleEvents.length);
            return;
        }

        const summary = await fetchJson('/api/dashboard/summary');
        topicsCount.textContent = String(cachedMqttTopics.length + cachedWsChannels.length || summary.observedTopicCount || 0);
        eventsCount.textContent = String(visibleEvents.length || summary.recentEventCount || 0);
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
        const trace = await fetchJson(buildTraceUrl(traceId));
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
            renderFlowGraphFromValidation(trace.validation);
        } else {
            flowStatus.classList.add('hidden');
            renderActualFlowGraph([]);
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
        renderFlowGraphFromValidation(trace.validation);
    } else {
        flowStatus.classList.add('hidden');
        renderActualFlowGraph(trace.events);
    }

    for (const event of trace.events) {
        const item = document.createElement('div');
        item.className = 'trace-step';

        if ((event.deltaFromPreviousSourceMs ?? 0) > SLOW_STEP_THRESHOLD_MS) {
            item.classList.add('slow-step');
        }

        const protocolClass = (event.protocol || '').toLowerCase() === 'ws' ? 'ws' : '';

        item.innerHTML = `
            <div class="trace-index">${escapeHtml(event.index ?? '-')}</div>
            <div class="trace-content">
                <div class="trace-title">
                    <span class="protocol-pill ${protocolClass}">${escapeHtml(event.protocol || '-')}</span>
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

    const missingText = validation.missingChannels?.length
        ? `Missing: ${validation.missingChannels.join(', ')}`
        : 'No missing expected steps.';

    const extraText = validation.extraChannels?.length
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

function renderFlowGraphFromValidation(validation) {
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
            <div class="flow-node-time">${step.found ? `Δ ${formatDuration(step.deltaFromPreviousSourceMs)}` : 'Missing'}</div>
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

    if (validation.extraChannels?.length) {
        const extra = document.createElement('div');
        extra.className = 'flow-extra';
        extra.innerHTML = `
            <strong>Extra observed channels</strong>
            <span>${escapeHtml(validation.extraChannels.join(', '))}</span>
        `;
        flowGraph.appendChild(extra);
    }
}

function renderActualFlowGraph(events) {
    flowGraph.classList.remove('hidden');
    flowGraph.innerHTML = '';

    if (!events || events.length === 0) {
        flowGraph.innerHTML = `
            <div class="flow-extra">
                <strong>No events to graph</strong>
                <span>Select a trace with events to see the actual flow.</span>
            </div>
        `;
        return;
    }

    events.forEach((event, index) => {
        const node = document.createElement('div');
        node.className = 'flow-node found';

        node.innerHTML = `
            <div class="flow-node-label">${escapeHtml(event.protocol || '-')}</div>
            <div class="flow-node-channel">${escapeHtml(event.channel || '-')}</div>
            <div class="flow-node-protocol">Actual event #${escapeHtml(event.index ?? index + 1)}</div>
            <div class="flow-node-time">source Δ ${formatDuration(event.deltaFromPreviousSourceMs)}</div>
            <div class="flow-node-observed">obs Δ ${formatDuration(event.deltaFromPreviousObservedMs)}</div>
        `;

        flowGraph.appendChild(node);

        if (index < events.length - 1) {
            const arrow = document.createElement('div');
            arrow.className = 'flow-arrow connected';
            arrow.innerHTML = '→';
            flowGraph.appendChild(arrow);
        }
    });
}

async function loadConfig() {
    try {
        const config = await fetchJson('/api/investigation/config');
        currentConfig = normalizeConfig(config);

        if (!selectedConfigFlowId && currentConfig.flows.length > 0) {
            selectedConfigFlowId = currentConfig.flows[0].id;
        }

        renderConfigEditor(currentConfig);
        renderTraceFlowSelector(currentConfig);
        showConfigMessage('Config loaded.', 'ok');
    } catch (e) {
        console.error('Failed loading config', e);
        showConfigMessage('Failed loading config.', 'error');
    }
}

async function saveConfig() {
    try {
        persistSelectedFlowEditorIntoConfig();

        const config = readConfigFromEditor();

        const saved = await fetchJson('/api/investigation/config', {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(config)
        });

        currentConfig = normalizeConfig(saved);

        if (!currentConfig.flows.some(flow => flow.id === selectedConfigFlowId)) {
            selectedConfigFlowId = currentConfig.flows[0]?.id || null;
        }

        renderConfigEditor(currentConfig);
        renderTraceFlowSelector(currentConfig);
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
        flows: Array.isArray(config?.flows) ? config.flows.map(normalizeFlow) : [],
        scenarios: Array.isArray(config?.scenarios) ? config.scenarios : [],
        rules: {
            maxStepDurationMs: config?.rules?.maxStepDurationMs ?? 50,
            allowExtraEvents: config?.rules?.allowExtraEvents ?? true
        }
    };
}

function normalizeFlow(flow) {
    return {
        id: flow?.id || createFlowId(flow?.name || 'flow'),
        name: flow?.name || '',
        description: flow?.description || '',
        steps: Array.isArray(flow?.steps) ? flow.steps : []
    };
}

function renderConfigEditor(config) {
    configNameInput.value = config.name || '';
    configDescriptionInput.value = config.description || '';
    maxStepDurationInput.value = String(config.rules?.maxStepDurationMs ?? 50);
    allowExtraEventsInput.checked = Boolean(config.rules?.allowExtraEvents ?? true);

    renderConfigFlowSelector(config);
    renderSelectedFlowEditor();
}

function renderConfigFlowSelector(config) {
    configFlowSelect.innerHTML = '';

    if (!config.flows?.length) {
        selectedConfigFlowId = null;
        configFlowSelect.innerHTML = '<option value="">No flows configured</option>';
        return;
    }

    if (!selectedConfigFlowId || !config.flows.some(flow => flow.id === selectedConfigFlowId)) {
        selectedConfigFlowId = config.flows[0].id;
    }

    for (const flow of config.flows) {
        const option = document.createElement('option');
        option.value = flow.id;
        option.textContent = `${flow.name || flow.id} (${flow.id})`;
        option.selected = flow.id === selectedConfigFlowId;
        configFlowSelect.appendChild(option);
    }
}

function renderTraceFlowSelector(config) {
    const previous = traceFlowSelect.value;
    traceFlowSelect.innerHTML = '';

    const noValidation = document.createElement('option');
    noValidation.value = '';
    noValidation.textContent = 'No validation — show actual graph only';
    traceFlowSelect.appendChild(noValidation);

    for (const flow of config.flows || []) {
        const option = document.createElement('option');
        option.value = flow.id;
        option.textContent = flow.name ? `${flow.name} (${flow.id})` : flow.id;
        traceFlowSelect.appendChild(option);
    }

    if ([...traceFlowSelect.options].some(option => option.value === previous)) {
        traceFlowSelect.value = previous;
    }
}

function renderSelectedFlowEditor() {
    flowStepsEditor.innerHTML = '';

    const flow = selectedFlow();
    const hasFlow = Boolean(flow);

    selectedFlowEditor.classList.toggle('hidden', !hasFlow);
    removeFlowBtn.disabled = !hasFlow;
    addFlowStepBtn.disabled = !hasFlow;

    if (!flow) return;

    flowIdInput.value = flow.id || '';
    flowNameInput.value = flow.name || '';
    flowDescriptionInput.value = flow.description || '';

    [...(flow.steps || [])]
        .sort((a, b) => Number(a.index ?? 0) - Number(b.index ?? 0))
        .forEach((step, idx) => {
            flowStepsEditor.appendChild(createStepEditorRow({
                index: idx + 1,
                protocol: step.protocol || 'MQTT',
                channel: step.channel || '',
                label: step.label || ''
            }));
        });
}

function selectedFlow() {
    if (!currentConfig || !selectedConfigFlowId) return null;
    return currentConfig.flows.find(flow => flow.id === selectedConfigFlowId) || null;
}

function createStepEditorRow(step) {
    const row = document.createElement('div');
    row.className = 'flow-step-row';

    row.innerHTML = `
        <div class="step-index">${escapeHtml(step.index)}</div>
        <select class="step-protocol">
            <option value="MQTT" ${step.protocol === 'MQTT' ? 'selected' : ''}>MQTT</option>
            <option value="WS" ${step.protocol === 'WS' ? 'selected' : ''}>WS</option>
        </select>
        <input class="step-channel" type="text" placeholder="channel/topic" value="${escapeHtml(step.channel)}">
        <input class="step-label" type="text" placeholder="label" value="${escapeHtml(step.label)}">
        <button class="secondary-btn remove-step-btn">Remove</button>
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

function addFlow() {
    persistSelectedFlowEditorIntoConfig();

    const nextNumber = (currentConfig?.flows?.length || 0) + 1;
    const flow = {
        id: `flow-${nextNumber}`,
        name: `Flow ${nextNumber}`,
        description: '',
        steps: []
    };

    currentConfig.flows.push(flow);
    selectedConfigFlowId = flow.id;

    renderConfigEditor(currentConfig);
    renderTraceFlowSelector(currentConfig);
    showConfigMessage('Flow added. Save config to persist.', 'ok');
}

function removeSelectedFlow() {
    if (!currentConfig || !selectedConfigFlowId) return;

    currentConfig.flows = currentConfig.flows.filter(flow => flow.id !== selectedConfigFlowId);
    selectedConfigFlowId = currentConfig.flows[0]?.id || null;

    renderConfigEditor(currentConfig);
    renderTraceFlowSelector(currentConfig);
    showConfigMessage('Flow removed. Save config to persist.', 'ok');
}

function persistSelectedFlowEditorIntoConfig() {
    const flow = selectedFlow();
    if (!flow) return;

    const oldId = flow.id;
    const newId = flowIdInput.value.trim() || oldId || createFlowId(flowNameInput.value || 'flow');

    flow.id = newId;
    flow.name = flowNameInput.value.trim();
    flow.description = flowDescriptionInput.value.trim();
    flow.steps = readStepsFromEditor();

    if (selectedConfigFlowId === oldId) {
        selectedConfigFlowId = newId;
    }
}

function readStepsFromEditor() {
    return [...flowStepsEditor.querySelectorAll('.flow-step-row')]
        .map((row, index) => ({
            index: index + 1,
            protocol: row.querySelector('.step-protocol').value,
            channel: row.querySelector('.step-channel').value.trim(),
            label: row.querySelector('.step-label').value.trim()
        }))
        .filter(step => step.channel);
}

function readConfigFromEditor() {
    return {
        name: configNameInput.value.trim(),
        description: configDescriptionInput.value.trim(),
        flows: currentConfig?.flows || [],
        scenarios: currentConfig?.scenarios || [],
        rules: {
            maxStepDurationMs: Number(maxStepDurationInput.value || 0),
            allowExtraEvents: allowExtraEventsInput.checked
        }
    };
}

function createFlowId(seed) {
    return String(seed || 'flow')
        .trim()
        .toLowerCase()
        .replaceAll(/[^a-z0-9]+/g, '-')
        .replaceAll(/^-+|-+$/g, '') || `flow-${Date.now()}`;
}

function exportConfig() {
    persistSelectedFlowEditorIntoConfig();

    const config = readConfigFromEditor();
    const blob = new Blob([JSON.stringify(config, null, 2)], {type: 'application/json'});
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = url;
    link.download = `${createFlowId(config.name || 'investigation-config')}.json`;
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
            selectedConfigFlowId = currentConfig.flows[0]?.id || null;
            renderConfigEditor(currentConfig);
            renderTraceFlowSelector(currentConfig);
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

async function loadImportedSessions() {
    try {
        const sessions = await fetchJson('/api/sessions/imported');
        const previous = sessionSelect.value;

        sessionSelect.innerHTML = '<option value="LIVE">Live</option>';

        for (const session of sessions) {
            const option = document.createElement('option');
            option.value = session.sessionId;
            option.textContent = `${session.name || 'Imported Session'} (${session.eventCount})`;
            sessionSelect.appendChild(option);
        }

        if ([...sessionSelect.options].some(option => option.value === previous)) {
            sessionSelect.value = previous;
        } else {
            sessionSelect.value = currentSession.type === 'LIVE' ? 'LIVE' : currentSession.sessionId || 'LIVE';
        }
    } catch (e) {
        console.error('Failed loading imported sessions', e);
    }
}

async function importSessionFileToServer(file) {
    if (!file) return;

    const form = new FormData();
    form.append('file', file);

    try {
        const summary = await fetchJson('/api/sessions/import', {
            method: 'POST',
            body: form
        });

        await loadImportedSessions();

        currentSession = {type: 'IMPORTED', sessionId: summary.sessionId};
        sessionSelect.value = summary.sessionId;

        disconnectStream();
        selectedChannels.clear();
        resetEventState();

        traceIdInput.value = '';
        traceSummary.textContent = `Imported session: ${summary.investigationName || summary.sessionId}`;
        traceTimeline.innerHTML = '';
        flowStatus.classList.add('hidden');
        flowGraph.classList.add('hidden');
        eventDetails.textContent = 'Select an event to inspect details.';

        await loadRecent();
        activateTab('liveEventsPanel');
    } catch (e) {
        console.error('Failed importing session', e);
        alert('Failed importing session package.');
    }
}

async function switchSessionFromSelect() {
    const value = sessionSelect.value;

    selectedChannels.clear();
    resetEventState();

    traceIdInput.value = '';
    traceTimeline.innerHTML = '';
    flowStatus.classList.add('hidden');
    flowGraph.classList.add('hidden');
    eventDetails.textContent = 'Select an event to inspect details.';

    if (value === 'LIVE') {
        currentSession = {type: 'LIVE', sessionId: null};
        setStreamStatus('disconnected');
        await loadRecent();
        connectStream();
        return;
    }

    currentSession = {type: 'IMPORTED', sessionId: value};
    disconnectStream();
    setStreamStatus('disconnected');

    traceSummary.textContent = 'Imported session selected.';
    await loadRecent();
}

async function downloadSession() {
    if (currentSession.type !== 'LIVE') {
        alert('Download currently exports the live investigation session. Switch to Live to export.');
        return;
    }

    try {
        const response = await fetch(buildSessionDownloadUrl());

        if (!response.ok) {
            throw new Error(`Download failed: ${response.status}`);
        }

        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');

        link.href = url;
        link.download = 'investigation-session.zip';
        document.body.appendChild(link);
        link.click();

        link.remove();
        URL.revokeObjectURL(url);
    } catch (e) {
        console.error('Failed downloading session', e);
        alert('Failed downloading investigation session.');
    }
}

refreshTopicsBtn.addEventListener('click', async () => {
    await loadRecent();
});

loadRecentBtn.addEventListener('click', async () => {
    await loadRecent();
});

connectStreamBtn.addEventListener('click', () => connectStream());
disconnectStreamBtn.addEventListener('click', () => disconnectStream());

textFilterInput.addEventListener('input', () => {
    applyFiltersAndRenderEvents();
});

inspectTraceBtn.addEventListener('click', () => inspectTrace());

traceIdInput.addEventListener('keydown', event => {
    if (event.key === 'Enter') inspectTrace();
});

traceFlowSelect.addEventListener('change', () => {
    if (traceIdInput.value.trim()) inspectTrace();
});

loadConfigBtn.addEventListener('click', () => loadConfig());
saveConfigBtn.addEventListener('click', () => saveConfig());
exportConfigBtn.addEventListener('click', () => exportConfig());

importConfigBtn.addEventListener('click', () => importConfigFile.click());

importConfigFile.addEventListener('change', event => {
    const file = event.target.files?.[0];
    if (file) importConfigFromFile(file);
    importConfigFile.value = '';
});

addFlowBtn.addEventListener('click', () => addFlow());
removeFlowBtn.addEventListener('click', () => removeSelectedFlow());

configFlowSelect.addEventListener('change', () => {
    persistSelectedFlowEditorIntoConfig();
    selectedConfigFlowId = configFlowSelect.value || null;
    renderSelectedFlowEditor();
});

addFlowStepBtn.addEventListener('click', () => addFlowStep());

sessionSelect.addEventListener('change', () => switchSessionFromSelect());

importSessionBtn.addEventListener('click', () => importSessionFile.click());

importSessionFile.addEventListener('change', event => {
    const file = event.target.files?.[0];
    if (file) importSessionFileToServer(file);
    importSessionFile.value = '';
});

downloadSessionBtn.addEventListener('click', () => downloadSession());

(async function init() {
    initTabsAndCollapse();
    activateTab('liveEventsPanel');

    setStreamStatus('disconnected');

    await loadImportedSessions();
    await loadRecent();
    await loadSummary();
    await loadConfig();

    connectStream();
})();