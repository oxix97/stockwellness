package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.member.LoadMemberPort;
import org.stockwellness.application.port.out.member.SaveMemberPort;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.shared.Email;
import org.stockwellness.global.security.MemberPrincipal;
import org.stockwellness.global.security.userinfo.OAuth2UserInfo;
import org.stockwellness.global.security.userinfo.OAuth2UserInfoFactory;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 서비스로 사용자 정보 로드
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 2. 서비스 구분 (kakao 등)
        LoginType loginType = LoginType.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());

        // 3. 속성 가져오기
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 4. Factory를 통한 UserInfo 객체 생성
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(loginType, attributes);
        log.debug("OAuth2 UserInfo: {}", oAuth2UserInfo);

        // 5. 회원 가입 및 조회 로직
        String email = oAuth2UserInfo.email();
        String nickname = oAuth2UserInfo.nickname();
        log.debug("Attempting to load/register member with email: {} and loginType: {}", email, loginType);

        Member member = loadMemberPort.loadMemberByEmailAndLoginType(new Email(email), loginType)
                .orElseGet(() -> {
                    log.info("New member registration for email: {}", email);
                    Member newMember = Member.register(email, nickname, loginType);
                    return saveMemberPort.saveMember(newMember);
                });

        log.info("Successfully loaded member: ID={}", member.getId());
        // 6. MemberPrincipal 반환
        return MemberPrincipal.of(member, attributes);
    }
}
