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

const channelFilterInput = document.getElementById('channelFilter');
const textFilterInput = document.getElementById('textFilter');

let selectedChannel = null;
let selectedChannelType = null;
let eventSource = null;
let cachedEvents = [];
let cachedMqttTopics = [];
let streamConnected = false;

async function fetchJson(url) {
    const response = await fetch(url);
    if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
    }
    return response.json();
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

    connectStreamBtn.disabled = state === 'connecting' || state === 'connected';
    disconnectStreamBtn.disabled = state === 'disconnected';
    streamConnected = state === 'connected' || state === 'connecting';
}

function buildRecentUrl() {
    const params = new URLSearchParams();

    if (selectedChannel) {
        params.set('channel', selectedChannel);
    }

    const query = params.toString();
    return `/api/events/recent${query ? `?${query}` : ''}`;
}

function buildStreamUrl() {
    const params = new URLSearchParams();

    const manualChannelFilter = channelFilterInput.value.trim();
    const textFilter = textFilterInput.value.trim();

    if (manualChannelFilter) {
        params.set('channelContains', manualChannelFilter);
    } else if (selectedChannel) {
        params.set('channelContains', selectedChannel);
    }

    if (textFilter) {
        params.set('textContains', textFilter);
    }

    params.set('_ts', String(Date.now()));

    return `/api/stream/events?${params.toString()}`;
}

function renderMqttTopics(topics) {
    cachedMqttTopics = topics;
    mqttTopicsList.innerHTML = '';

    const allBtn = document.createElement('button');
    allBtn.className = `topic-chip ${selectedChannel === null ? 'active' : ''}`;
    allBtn.textContent = 'All topics';
    allBtn.onclick = async () => {
        selectedChannel = null;
        selectedChannelType = null;
        selectedTopicLabel.textContent = 'All';
        renderMqttTopics(cachedMqttTopics);
        await loadRecent();
        if (streamConnected) {
            reconnectStream();
        }
    };
    mqttTopicsList.appendChild(allBtn);

    for (const topic of topics) {
        const btn = document.createElement('button');
        btn.className = `topic-chip ${(selectedChannel === topic && selectedChannelType === 'MQTT') ? 'active' : ''}`;
        btn.textContent = topic;
        btn.onclick = async () => {
            selectedChannel = topic;
            selectedChannelType = 'MQTT';
            selectedTopicLabel.textContent = topic;
            renderMqttTopics(cachedMqttTopics);
            await loadRecent();
            if (streamConnected) {
                reconnectStream();
            }
        };
        mqttTopicsList.appendChild(btn);
    }

    topicsCount.textContent = String(topics.length);
}

function renderWsPlaceholder() {
    wsChannelsList.innerHTML = '';
    const item = document.createElement('div');
    item.className = 'topic-chip';
    item.textContent = 'WebSocket paused for now';
    wsChannelsList.appendChild(item);
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

function escapeHtml(text) {
    return String(text)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;');
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

async function refreshTopics() {
    try {
        const topics = await fetchJson('/api/events/mqtt/topics');
        renderMqttTopics(topics);
    } catch (e) {
        console.error('Failed loading topics', e);
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

function shouldDisplayEvent(event) {
    const manualChannelFilter = channelFilterInput.value.trim();
    const textFilter = textFilterInput.value.trim();

    const effectiveChannel = manualChannelFilter || selectedChannel;
    const matchesChannel = !effectiveChannel || (event.channel || '').includes(effectiveChannel);
    const matchesText = !textFilter || (event.payload || '').includes(textFilter);

    return matchesChannel && matchesText;
}

function prependEvent(event) {
    if (!shouldDisplayEvent(event)) {
        return;
    }

    cachedEvents.push(event);

    if (cachedEvents.length > 500) {
        cachedEvents = cachedEvents.slice(cachedEvents.length - 500);
    }

    renderEvents(cachedEvents);
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
    console.log('Connecting SSE:', url);
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
            prependEvent(parsed);
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

function reconnectStream() {
    connectStream();
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
    await refreshTopics();
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

channelFilterInput.addEventListener('change', async () => {
    await loadRecent();
    if (streamConnected) {
        reconnectStream();
    }
});

textFilterInput.addEventListener('change', async () => {
    await loadRecent();
    if (streamConnected) {
        reconnectStream();
    }
});

(async function init() {
    setStreamStatus('disconnected');
    renderWsPlaceholder();
    await refreshTopics();
    await loadRecent();
    await loadSummary();

    // prod-like behavior: auto-start live stream on load
    connectStream();
})();