package org.stockwellness.adapter.in.web.member;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.adapter.in.web.member.dto.UpdateMemberRequest;
import org.stockwellness.adapter.in.web.member.dto.UpdateNotificationRequest;
import org.stockwellness.application.port.in.member.MemberUseCase;
import org.stockwellness.application.port.in.member.result.MemberResult;
import org.stockwellness.application.port.in.member.result.NotificationSettingsResult;
import org.stockwellness.global.common.response.ApiResponse;
import org.stockwellness.global.security.MemberPrincipal;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberUseCase memberUseCase;

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    public ApiResponse<MemberResult> getMember(@AuthenticationPrincipal MemberPrincipal memberPrincipal) {
        MemberResult member = memberUseCase.getMember(memberPrincipal.id());
        return ApiResponse.success(member);
    }

    /**
     * 내 정보 수정
     */
    @PutMapping("/me")
    public ApiResponse<Void> updateMember(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @RequestBody @Valid UpdateMemberRequest request
    ) {
        memberUseCase.updateMember(memberPrincipal.id(), request.toCommand(memberPrincipal.id()));
        return ApiResponse.success();
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/me")
    public ApiResponse<Void> deactivateMember(@AuthenticationPrincipal MemberPrincipal memberPrincipal) {
        memberUseCase.withdrawMember(memberPrincipal.id());
        return ApiResponse.success();
    }

    /**
     * 알림 설정 조회
     */
    @GetMapping("/me/notifications")
    public ApiResponse<NotificationSettingsResult> getNotificationSettings(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal) {
        return ApiResponse.success(memberUseCase.getNotificationSettings(memberPrincipal.id()));
    }

    /**
     * 알림 설정 변경
     */
    @PutMapping("/me/notifications")
    public ApiResponse<Void> updateNotificationSettings(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @RequestBody UpdateNotificationRequest request) {
        memberUseCase.updateNotificationSettings(memberPrincipal.id(), request.toCommand(memberPrincipal.id()));
        return ApiResponse.success();
    }
}
