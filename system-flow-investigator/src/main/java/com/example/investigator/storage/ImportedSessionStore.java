package com.example.investigator.storage;

import com.example.investigator.domain.session.ImportedSession;
import com.example.investigator.domain.session.ImportedSessionSummary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ImportedSessionStore {

    private final ConcurrentMap<String, ImportedSession> sessions = new ConcurrentHashMap<>();

    public ImportedSession save(ImportedSession session) {
        sessions.put(session.sessionId(), session);
        return session;
    }

    public Optional<ImportedSession> get(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public List<ImportedSessionSummary> summaries() {
        return sessions.values()
                .stream()
                .map(session -> new ImportedSessionSummary(
                        session.sessionId(),
                        session.name(),
                        session.importedAt(),
                        session.events().size()
                ))
                .sorted((a, b) -> b.importedAt().compareTo(a.importedAt()))
                .toList();
    }

    public List<ImportedSession> all() {
        return new ArrayList<>(sessions.values());
    }
}