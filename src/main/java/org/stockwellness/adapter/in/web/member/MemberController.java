package org.stockwellness.adapter.in.web.member;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.adapter.in.web.member.dto.UpdateMemberRequest;
import org.stockwellness.application.port.in.member.MemberUseCase;
import org.stockwellness.application.port.in.member.result.MemberResult;
import org.stockwellness.global.security.CurrentMemberId;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberUseCase memberUseCase;

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<MemberResult> getMember(@CurrentMemberId Long memberId) {
        MemberResult member = memberUseCase.getMember(memberId);
        return ResponseEntity.ok(member);
    }

    /**
     * 내 정보 수정
     */
    @PutMapping("/me")
    public ResponseEntity<Void> updateMember(
            @CurrentMemberId Long memberId,
            @RequestBody @Valid UpdateMemberRequest request
    ) {
        memberUseCase.updateMember(memberId, request.toCommand(memberId));
        return ResponseEntity.ok().build();
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivateMember(@CurrentMemberId Long memberId) {
        memberUseCase.withdrawMember(memberId);
        return ResponseEntity.ok().build();
    }
}
