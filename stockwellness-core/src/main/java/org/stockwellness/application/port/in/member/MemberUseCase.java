package org.stockwellness.application.port.in.member;

import org.stockwellness.application.port.in.member.command.UpdateMemberCommand;
import org.stockwellness.application.port.in.member.command.UpdateNotificationCommand;
import org.stockwellness.application.port.in.member.result.MemberResult;
import org.stockwellness.application.port.in.member.result.NotificationSettingsResult;

public interface MemberUseCase {
    MemberResult getMember(Long memberId);

    void updateMember(Long memberId, UpdateMemberCommand command);

    void withdrawMember(Long memberId);

    NotificationSettingsResult getNotificationSettings(Long memberId);

    void updateNotificationSettings(Long memberId, UpdateNotificationCommand command);
}
