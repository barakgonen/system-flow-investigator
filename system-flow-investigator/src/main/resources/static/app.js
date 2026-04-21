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

let eventSource = null;
let cachedEvents = [];
let cachedMqttTopics = [];
let cachedWsChannels = [];
let selectedChannels = new Set();

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
    if (selectedChannels.size === 0) {
        selectedTopicLabel.textContent = 'All channels';
    } else {
        selectedTopicLabel.textContent = `${selectedChannels.size} selected`;
    }
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
    return String(text)
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

function payloadPreview(payload) {
    if (!payload) return '';
    return payload.length > 120 ? payload.slice(0, 120) + '...' : payload;
}

function renderEvents(events) {
    cachedEvents = events;
    eventsTableBody.innerHTML = '';
    eventsCount.textContent = String(events.length);

    const ordered = [...events].reverse();

    for (const event of ordered) {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${escapeHtml(formatTime(event.receivedAt))}</td>
            <td>${escapeHtml(event.protocol || '-')}</td>
            <td>${escapeHtml(event.channel || '-')}</td>
            <td>${escapeHtml(event.traceId || '-')}</td>
            <td class="payload-preview">${escapeHtml(payloadPreview(event.payload || ''))}</td>
        `;

        tr.onclick = () => {
            eventDetails.textContent = JSON.stringify(event, null, 2);
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
        if (eventSource !== es) {
            return;
        }
        activated = true;
        setStreamStatus('connected');
    };

    es.onmessage = (event) => {
        if (eventSource !== es) {
            return;
        }

        if (!activated) {
            activated = true;
            setStreamStatus('connected');
        }

        if (!event.data) {
            return;
        }

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
        if (eventSource !== es) {
            return;
        }

        if (!activated) {
            activated = true;
            setStreamStatus('connected');
        }
    });

    es.onerror = () => {
        if (eventSource !== es) {
            return;
        }
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

(async function init() {
    setStreamStatus('disconnected');
    await refreshChannels();
    await loadRecent();
    await loadSummary();
    connectStream();
})();