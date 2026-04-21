const topicsList = document.getElementById('topicsList');
const topicsCount = document.getElementById('topicsCount');
const eventsCount = document.getElementById('eventsCount');
const selectedTopicLabel = document.getElementById('selectedTopicLabel');
const eventsTableBody = document.getElementById('eventsTableBody');
const eventDetails = document.getElementById('eventDetails');

const refreshTopicsBtn = document.getElementById('refreshTopicsBtn');
const loadRecentBtn = document.getElementById('loadRecentBtn');
const connectStreamBtn = document.getElementById('connectStreamBtn');
const disconnectStreamBtn = document.getElementById('disconnectStreamBtn');

const channelFilterInput = document.getElementById('channelFilter');
const textFilterInput = document.getElementById('textFilter');

let selectedTopic = null;
let eventSource = null;
let cachedTopics = [];
let cachedEvents = [];

async function fetchJson(url) {
    const response = await fetch(url);
    if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
    }
    return response.json();
}

function buildRecentUrl() {
    const params = new URLSearchParams();
    if (selectedTopic) {
        params.set('channel', selectedTopic);
    }
    const query = params.toString();
    return `/api/events/recent${query ? `?${query}` : ''}`;
}

function buildStreamUrl() {
    const params = new URLSearchParams();

    const channelFilter = channelFilterInput.value.trim();
    const textFilter = textFilterInput.value.trim();

    if (channelFilter) {
        params.set('channelContains', channelFilter);
    } else if (selectedTopic) {
        params.set('channelContains', selectedTopic);
    }

    if (textFilter) {
        params.set('textContains', textFilter);
    }

    const query = params.toString();
    return `/api/stream/events${query ? `?${query}` : ''}`;
}

function renderTopics(topics) {
    cachedTopics = topics;
    topicsList.innerHTML = '';

    const allBtn = document.createElement('button');
    allBtn.className = `topic-chip ${selectedTopic === null ? 'active' : ''}`;
    allBtn.textContent = 'All topics';
    allBtn.onclick = async () => {
        selectedTopic = null;
        selectedTopicLabel.textContent = 'All';
        renderTopics(cachedTopics);
        await loadRecent();
    };
    topicsList.appendChild(allBtn);

    for (const topic of topics) {
        const btn = document.createElement('button');
        btn.className = `topic-chip ${selectedTopic === topic ? 'active' : ''}`;
        btn.textContent = topic;
        btn.onclick = async () => {
            selectedTopic = topic;
            selectedTopicLabel.textContent = topic;
            renderTopics(cachedTopics);
            await loadRecent();
        };
        topicsList.appendChild(btn);
    }
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
    return payload.length > 110 ? payload.slice(0, 110) + '...' : payload;
}

function escapeHtml(text) {
    return String(text)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;');
}

function renderEvents(events) {
    cachedEvents = events;
    eventsCount.textContent = String(events.length);
    eventsTableBody.innerHTML = '';

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
        renderTopics(topics);
    } catch (e) {
        console.error(e);
    }
}

async function loadRecent() {
    try {
        const events = await fetchJson(buildRecentUrl());
        renderEvents(events);
        await loadSummary(); // 🔥 backend-driven stats
    } catch (e) {
        console.error(e);
    }
}

function prependEvent(event) {
    cachedEvents.push(event);

    if (cachedEvents.length > 500) {
        cachedEvents = cachedEvents.slice(cachedEvents.length - 500);
    }

    renderEvents(cachedEvents);
    loadSummary(); // 🔥 keep stats fresh
}

function connectStream() {
    disconnectStream();

    eventSource = new EventSource(buildStreamUrl());

    eventSource.onmessage = (event) => {
        try {
            const parsed = JSON.parse(event.data);
            prependEvent(parsed);
        } catch (e) {
            console.error('Failed parsing SSE event', e);
        }
    };

    eventSource.onerror = (e) => {
        console.error('SSE error', e);
    };
}

function disconnectStream() {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }
}

// 🔥 NEW — backend summary
async function loadSummary() {
    try {
        const summary = await fetchJson('/api/dashboard/summary');

        topicsCount.textContent = String(summary.observedTopicCount ?? 0);
        eventsCount.textContent = String(summary.recentEventCount ?? 0);

    } catch (e) {
        console.error(e);
    }
}

refreshTopicsBtn.addEventListener('click', refreshTopics);
loadRecentBtn.addEventListener('click', loadRecent);
connectStreamBtn.addEventListener('click', connectStream);
disconnectStreamBtn.addEventListener('click', disconnectStream);

(async function init() {
    await refreshTopics();
    await loadRecent();
    await loadSummary(); // 🔥 initial load
    connectStream();
})();