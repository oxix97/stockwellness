package org.stockwellness.adapter.in.event.watchlist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.watchlist.WatchlistUseCase;
import org.stockwellness.domain.member.event.MemberCreatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberCreatedEventListener {

    private final WatchlistUseCase watchlistUseCase;

    @EventListener
    @Transactional
    public void handleMemberCreatedEvent(MemberCreatedEvent event) {
        log.info("Creating default watchlist group for member: {}", event.getMember().getId());
        watchlistUseCase.createDefaultGroup(event.getMember());
    }
}
