package org.stockwellness.application.port.in.member.result;

import org.stockwellness.domain.member.Member;

public record NotificationSettingsResult(
        boolean rebalancing,
        boolean marketAlert,
        boolean newListing
) {
    public static NotificationSettingsResult from(Member member) {
        return new NotificationSettingsResult(
                member.isNotificationRebalancing(),
                member.isNotificationMarketAlert(),
                member.isNotificationNewListing()
        );
    }
}
