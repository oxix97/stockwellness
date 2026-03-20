package org.stockwellness.application.port.in.member.command;

public record UpdateNotificationCommand(
        Long memberId,
        Boolean rebalancing,
        Boolean marketAlert,
        Boolean newListing
) {}
