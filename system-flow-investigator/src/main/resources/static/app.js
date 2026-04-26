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

const traceIdInput = document.getElementById('traceIdInput');
const inspectTraceBtn = document.getElementById('inspectTraceBtn');
const traceSummary = document.getElementById('traceSummary');
const traceTimeline = document.getElementById('traceTimeline');
const toggleTraceBtn = document.getElementById('toggleTraceBtn');
const traceBody = document.getElementById('traceBody');
const liveEventsPanel = document.getElementById('liveEventsPanel');
const tracePanel = document.getElementById('tracePanel');

let eventSource = null;
let cachedEvents = [];
let cachedMqttTopics = [];
let cachedWsChannels = [];
let selectedChannels = new Set();

let liveEventsCollapsed = false;
let traceCollapsed = false;

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

async function fetchJson(url) {
    const response = await fetch(url);
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
        return;
    }

    try {
        const trace = await fetchJson(`/api/correlation/trace/${encodeURIComponent(traceId)}`);
        renderTrace(trace);
    } catch (e) {
        console.error('Failed loading trace', e);
        traceSummary.textContent = 'Failed loading trace.';
        traceTimeline.innerHTML = '';
    }
}

function renderTrace(trace) {
    traceTimeline.innerHTML = '';

    if (!trace.events || trace.events.length === 0) {
        traceSummary.innerHTML = `<strong>${escapeHtml(trace.traceId || '-')}</strong> | no events found`;
        return;
    }

    traceSummary.innerHTML = `
        <strong>${escapeHtml(trace.traceId || '-')}</strong>
        <span>${trace.eventCount} events</span>
        <span>Source duration: ${formatDuration(trace.totalSourceDurationMs)}</span>
        <span>Observed duration: ${formatDuration(trace.totalObservedDurationMs)}</span>
    `;

    for (const event of trace.events) {
        const item = document.createElement('div');
        item.className = 'trace-step';

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

(async function init() {
    setStreamStatus('disconnected');
    await refreshChannels();
    await loadRecent();
    await loadSummary();
    connectStream();
})();