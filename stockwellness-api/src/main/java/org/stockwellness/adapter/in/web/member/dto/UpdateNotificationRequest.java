package org.stockwellness.adapter.in.web.member.dto;

import org.stockwellness.application.port.in.member.command.UpdateNotificationCommand;

public record UpdateNotificationRequest(
        Boolean rebalancing,
        Boolean marketAlert,
        Boolean newListing
) {
    public UpdateNotificationCommand toCommand(Long memberId) {
        return new UpdateNotificationCommand(memberId, rebalancing, marketAlert, newListing);
    }
}
