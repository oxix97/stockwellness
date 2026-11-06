package org.stockwellness.adapter.in.web.actual;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import java.util.Date;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.stockwellness.adapter.out.security.jwt.JwtProperties;
import org.stockwellness.application.port.in.actual.ActualAccountUseCase;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.MemberRole;
import org.stockwellness.global.error.exception.BusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.springframework.restdocs.payload.JsonFieldType;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.stockwellness.adapter.out.persistence.actual.ActualAccountAdapter;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.GlobalException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.stockwellness.adapter.out.security.jwt.JwtProvider;
import org.stockwellness.application.service.auth.CustomOAuth2UserService;
import org.stockwellness.config.SecurityConfig;
import org.stockwellness.global.alert.SlackAlertService;
import org.stockwellness.global.error.GlobalExceptionHandler;
import org.stockwellness.global.security.CustomUserDetailsService;
import org.stockwellness.global.security.MemberPrincipal;
import org.stockwellness.global.security.handler.OAuth2LoginFailureHandler;
import org.stockwellness.global.security.handler.OAuth2LoginSuccessHandler;
import org.stockwellness.global.security.jwt.JwtAccessDeniedHandler;
import org.stockwellness.global.security.jwt.JwtAuthenticationEntryPoint;
import org.stockwellness.global.security.jwt.JwtAuthenticationFilter;
import org.stockwellness.global.security.jwt.JwtExceptionFilter;
import org.stockwellness.support.RestDocsSupport;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"test", "actual-contract-test"})
@SpringBootTest(classes = ActualAccountControllerTest.ActualTestApplication.class)
class ActualAccountControllerTest extends RestDocsSupport {
    @Autowired JwtProvider jwtProvider;
    @Autowired JwtProperties jwtProperties;
    @Autowired ActualAccountUseCase accounts;
    @MockitoSpyBean ActualAccountAdapter accountAdapter;
    @MockitoBean CustomUserDetailsService userDetails;
    @MockitoBean CustomOAuth2UserService oauthUsers;
    @MockitoBean OAuth2LoginSuccessHandler oauthSuccess;
    @MockitoBean OAuth2LoginFailureHandler oauthFailure;
    @MockitoBean SlackAlertService slackAlertService;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void actualSecuritySetup(WebApplicationContext context, RestDocumentationContextProvider docs) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(ActualNoStoreFilter.class))
                .apply(springSecurity()).apply(documentationConfiguration(docs)).build();
        jdbc.update("delete from actual_mutation_receipt");
        jdbc.update("delete from actual_account");
        jdbc.update("delete from member");
        for (long id : List.of(7L, 8L)) {
            jdbc.update("insert into member(id,created_at,nickname,role,status,notification_rebalancing,notification_market_alert,notification_new_listing) values (?,CURRENT_TIMESTAMP,?,'USER','ACTIVE',true,false,false)", id, "member" + id);
            given(userDetails.loadUserByUsername(Long.toString(id))).willReturn(MemberPrincipal.fromToken(id, "owner@example.test", "USER"));
        }
    }

    @Test
    void empty_owned_list_returns_200() throws Exception {
        mockMvc.perform(get("/api/v1/investment-accounts")
                        .with(user(MemberPrincipal.fromToken(7L, "owner@example.test", "USER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void unauthenticated_actual_route_is_rejected_by_real_filter() throws Exception {
        mockMvc.perform(get("/api/v1/investment-accounts"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A001"));
    }

    private static final String CREATE_KEY = "create-account-0001";
    private static final String RENAME_KEY = "rename-account-0001";
    private static final String BODY = "{\"name\":\"장부\",\"baseCurrency\":\"KRW\"}";

    private RequestPostProcessor owner(long id) {
        return user(MemberPrincipal.fromToken(id, "owner@example.test", "USER"));
    }

    private JsonNode createAccount() throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/v1/investment-accounts").with(owner(7))
                .contentType("application/json").header("Idempotency-Key", CREATE_KEY).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("S001")).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().doesNotExist("ETag"))
                .andReturn().getResponse().getContentAsString());
    }

    @Test void create_multiple_accounts_and_list_only_owner() throws Exception {
        JsonNode first = createAccount();
        assertThat(first.path("data").path("accountVersion").asLong()).isZero();
        assertThat(first.path("data").has("brokerLabel")).isTrue();
        assertThat(first.path("data").get("brokerLabel").isNull()).isTrue();
        mockMvc.perform(post("/api/v1/investment-accounts").with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", "create-account-0002").content(BODY)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/investment-accounts").with(owner(8)).contentType("application/json")
                .header("Idempotency-Key", CREATE_KEY).content(BODY)).andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/investment-accounts").with(owner(7)))
                .andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(get("/api/v1/investment-accounts").with(owner(8)))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test void original_create_envelope_survives_rename_and_canonical_replay() throws Exception {
        JsonNode first = createAccount();
        long id = first.path("data").path("accountId").asLong();
        mockMvc.perform(patch("/api/v1/investment-accounts/" + id).with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", RENAME_KEY).header("If-Match", "\"0\"").content("{\"name\":\"새이름\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accountVersion").value(1))
                .andExpect(jsonPath("$.data.ledgerVersion").value(0));
        String repeated = mockMvc.perform(post("/api/v1/investment-accounts").with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", CREATE_KEY)
                .content("{\"brokerLabel\":null,\"baseCurrency\":\"KRW\",\"name\":\"  장부  \"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(repeated)).isEqualTo(first);
        assertThat(jdbc.queryForObject("select count(*) from actual_account", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from actual_mutation_receipt", Integer.class)).isEqualTo(2);
    }

    @Test void rename_replay_precedes_current_version_and_noop_increments() throws Exception {
        long id = createAccount().path("data").path("accountId").asLong();
        String first = mockMvc.perform(patch("/api/v1/investment-accounts/" + id).with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", RENAME_KEY).header("If-Match", "\"0\"").content("{\"name\":\"장부\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accountVersion").value(1)).andReturn().getResponse().getContentAsString();
        String repeated = mockMvc.perform(patch("/api/v1/investment-accounts/" + id).with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", RENAME_KEY).header("If-Match", "\"0\"").content("{\"name\":\"장부\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(repeated).isEqualTo(first);
        mockMvc.perform(patch("/api/v1/investment-accounts/" + id).with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", RENAME_KEY).header("If-Match", "\"1\"").content("{\"name\":\"장부\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("I002"));
        mockMvc.perform(patch("/api/v1/investment-accounts/" + id).with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", "rename-account-0002").header("If-Match", "\"0\"").content("{\"name\":\"장부\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("I001"));
        assertThat(accounts.get(7, id).accountVersion()).isEqualTo(1);
    }

    @Test void changed_create_body_conflicts_and_failed_key_is_reusable() throws Exception {
        createAccount();
        mockMvc.perform(post("/api/v1/investment-accounts").with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", CREATE_KEY).content("{\"name\":\"different\",\"baseCurrency\":\"KRW\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("I002"));
        String key = "create-account-0002";
        mockMvc.perform(post("/api/v1/investment-accounts").with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", key).content("{\"name\":\"\",\"baseCurrency\":\"KRW\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/investment-accounts").with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", key).content(BODY)).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select count(*) from actual_account", Integer.class)).isEqualTo(2);
    }

    @Test void another_owner_and_missing_id_have_same_404_and_body_does_not_leak() throws Exception {
        long id = createAccount().path("data").path("accountId").asLong();
        for (long target : List.of(id, 9_000_000L)) {
            mockMvc.perform(get("/api/v1/investment-accounts/" + target).with(owner(8)))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("G003"))
                    .andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."))
                    .andExpect(jsonPath("$.data").doesNotExist());
            mockMvc.perform(patch("/api/v1/investment-accounts/" + target).with(owner(8)).contentType("application/json")
                    .header("Idempotency-Key", RENAME_KEY).header("If-Match", "broken").content("{duplicate broken"))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("G003"));
        }
        mockMvc.perform(delete("/api/v1/investment-accounts/" + id).with(owner(7)))
                .andExpect(status().isMethodNotAllowed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"name\":1,\"baseCurrency\":\"KRW\"}", "{\"name\":null,\"baseCurrency\":\"KRW\"}",
        "{\"name\":\"x\",\"baseCurrency\":\"USD\"}", "{\"name\":\"x\",\"baseCurrency\":\"KRW\",\"memberId\":7}",
        "{\"name\":\"a\",\"name\":\"b\",\"baseCurrency\":\"KRW\"}", "{\"name\":\"x\",\"baseCurrency\":\"KRW\"} {}",
        "[]", "null", "{}", "{\"name\":\"x\",\"baseCurrency\":\"KRW\",\"brokerLabel\":5}"})
    void strict_json_rejects_coercion_unknown_duplicate_trailing_and_invalid_fields(String body) throws Exception {
        String result = mockMvc.perform(post("/api/v1/investment-accounts").with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", CREATE_KEY).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("G001"))
                .andExpect(jsonPath("$.errors").isEmpty()).andReturn().getResponse().getContentAsString();
        assertThat(result).doesNotContain("stackTrace", "Exception", "rejectedValue");
        assertThat(jdbc.queryForObject("select count(*) from actual_account", Integer.class)).isZero();
    }

    @Test void unicode_code_points_trim_and_broker_null_match_contract() throws Exception {
        String name = "😀".repeat(50);
        String response = mockMvc.perform(post("/api/v1/investment-accounts").with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", CREATE_KEY).content(objectMapper.writeValueAsString(Map.of(
                        "name", "\u0085" + name + "\u00a0", "baseCurrency", "KRW", "brokerLabel", "\u3000"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(response).path("data");
        assertThat(data.path("name").asText()).isEqualTo(name);
        assertThat(data.get("brokerLabel").isNull()).isTrue();
        for (String invalidName : List.of("\u0085\u00a0", "😀".repeat(51))) {
            mockMvc.perform(post("/api/v1/investment-accounts").with(owner(7)).contentType("application/json")
                    .header("Idempotency-Key", "create-account-0002").content(objectMapper.writeValueAsString(Map.of(
                            "name", invalidName, "baseCurrency", "KRW"))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("G001"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "W/\"0\"", "*", "\"00\"", "\"-1\"", "\"9007199254740992\"", "\"0\",\"1\"", "\"99999999999999999\""})
    void invalid_if_match_is_safe_400_without_mutation(String ifMatch) throws Exception {
        long id = createAccount().path("data").path("accountId").asLong();
        mockMvc.perform(patch("/api/v1/investment-accounts/" + id).with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", RENAME_KEY).header("If-Match", ifMatch).content("{\"name\":\"new\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("G001"));
        assertThat(accounts.get(7, id).accountVersion()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from actual_mutation_receipt", Integer.class)).isEqualTo(1);
    }

    @Test void missing_headers_and_invalid_paths_are_400() throws Exception {
        mockMvc.perform(post("/api/v1/investment-accounts").with(owner(7)).contentType("application/json").content(BODY))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("G001"));
        long id = createAccount().path("data").path("accountId").asLong();
        mockMvc.perform(patch("/api/v1/investment-accounts/" + id).with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", RENAME_KEY).content("{\"name\":\"new\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("G001"));
        for (String path : List.of("0", "-1", "9007199254740992", "bad"))
            mockMvc.perform(get("/api/v1/investment-accounts/" + path).with(owner(7)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("G001"));
    }

    @Test void real_jwt_valid_invalid_and_expired_paths() throws Exception {
        String token = jwtProvider.generateAccessToken(7L, "owner@example.test", "owner", LoginType.KAKAO, MemberRole.USER, null);
        mockMvc.perform(get("/api/v1/investment-accounts").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/investment-accounts").header("Authorization", "Bearer invalid.fixture"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A004"));
        String expired = Jwts.builder().subject("7").expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secretKey()))).compact();
        mockMvc.perform(get("/api/v1/investment-accounts").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A003"));
        mockMvc.perform(get("/api/v1/portfolios"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A008"));
        mockMvc.perform(get("/api/v1/comparison-targets"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A001"));
    }

    @Test void version_overflow_rolls_back_and_does_not_store_receipt() throws Exception {
        long id = createAccount().path("data").path("accountId").asLong();
        jdbc.update("update actual_account set account_version = 9007199254740991 where id = ?", id);
        mockMvc.perform(patch("/api/v1/investment-accounts/" + id).with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", RENAME_KEY).header("If-Match", "\"9007199254740991\"").content("{\"name\":\"new\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("I008"));
        assertThat(accounts.get(7, id).name()).isEqualTo("장부");
        assertThat(jdbc.queryForObject("select count(*) from actual_mutation_receipt", Integer.class)).isEqualTo(1);
    }

    private List<String> race(Supplier<ActualAccountUseCase.Mutation> first, Supplier<ActualAccountUseCase.Mutation> second) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(3);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = List.of(first, second).stream().map(action -> executor.submit(() -> {
                barrier.await();
                try { return "OK:" + action.get().data().accountId(); }
                catch (BusinessException e) { return e.getErrorCode().getCode(); }
            })).toList();
            barrier.await();
            return List.of(tasks.get(0).get(10, TimeUnit.SECONDS), tasks.get(1).get(10, TimeUnit.SECONDS));
        }
    }

    @Test void same_create_key_race_commits_one_account_and_receipt_in_h2() throws Exception {
        var command = new ActualAccountUseCase.Create(7, "장부", "KRW", null, CREATE_KEY);
        List<String> result = race(() -> accounts.create(command), () -> accounts.create(command));
        assertThat(result.get(0)).startsWith("OK:").isEqualTo(result.get(1));
        assertThat(jdbc.queryForObject("select count(*) from actual_account", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from actual_mutation_receipt", Integer.class)).isEqualTo(1);
    }

    @Test void different_rename_keys_same_version_race_commits_one_in_h2() throws Exception {
        long id = createAccount().path("data").path("accountId").asLong();
        List<String> result = race(
                () -> accounts.rename(new ActualAccountUseCase.Rename(7, id, "first", "\"0\"", RENAME_KEY)),
                () -> accounts.rename(new ActualAccountUseCase.Rename(7, id, "second", "\"0\"", "rename-account-0002")));
        assertThat(result).contains("I001");
        assertThat(result.stream().filter(value -> value.startsWith("OK:")).count()).isEqualTo(1);
        assertThat(accounts.get(7, id).accountVersion()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from actual_mutation_receipt", Integer.class)).isEqualTo(2);
    }

    @Test void failed_receipt_write_rolls_back_account_and_allows_same_key_retry() throws Exception {
        doThrow(new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR)).when(accountAdapter).saveReceipt(any());
        mockMvc.perform(post("/api/v1/investment-accounts").with(owner(7)).contentType("application/json")
                .header("Idempotency-Key", CREATE_KEY).content(BODY))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("G004"));
        assertThat(jdbc.queryForObject("select count(*) from actual_account", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from actual_mutation_receipt", Integer.class)).isZero();
        doCallRealMethod().when(accountAdapter).saveReceipt(any());
        createAccount();
    }

    @Test void same_rename_key_race_replays_one_success_in_h2() throws Exception {
        long id = createAccount().path("data").path("accountId").asLong();
        var command = new ActualAccountUseCase.Rename(7, id, "new", "\"0\"", RENAME_KEY);
        List<String> result = race(() -> accounts.rename(command), () -> accounts.rename(command));
        assertThat(result.get(0)).startsWith("OK:").isEqualTo(result.get(1));
        assertThat(accounts.get(7,id).accountVersion()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from actual_mutation_receipt", Integer.class)).isEqualTo(2);
    }

    @Test void personal_success_and_errors_are_no_store_and_ignore_cache_validators() throws Exception {
        long id = createAccount().path("data").path("accountId").asLong();
        mockMvc.perform(get("/api/v1/investment-accounts/" + id).with(owner(7)).header("If-None-Match", "\"0\""))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().doesNotExist("ETag"));
        mockMvc.perform(get("/api/v1/investment-accounts/" + id).with(owner(8)))
                .andExpect(status().isNotFound()).andExpect(header().string("Cache-Control", "no-store"));
        mockMvc.perform(get("/api/v1/investment-accounts"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test void account_endpoints_generate_rest_docs() throws Exception {
        String token = jwtProvider.generateAccessToken(7L, "owner@example.test", "owner", LoginType.KAKAO, MemberRole.USER, null);
        var fields = new ArrayList<>(commonResponseFields());
        fields.add(subsectionWithPath("data").description("contract 1.0.0 Account, brokerLabel explicit nullable"));
        String created = mockMvc.perform(post("/api/v1/investment-accounts").header("Authorization", "Bearer " + token)
                .contentType("application/json").header("Idempotency-Key", CREATE_KEY).content(BODY))
                .andExpect(status().isCreated()).andDo(document("actual-account-create", resource(ResourceSnippetParameters.builder()
                        .tag("Actual Accounts").summary("실제 KRW 계정 생성").responseSchema(Schema.schema("AccountEnvelope"))
                        .responseFields(fields).build())))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).path("data").path("accountId").asLong();
        mockMvc.perform(get("/api/v1/investment-accounts/{accountId}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andDo(document("actual-account-get", resource(ResourceSnippetParameters.builder()
                        .tag("Actual Accounts").summary("내 실제 계정 조회").responseSchema(Schema.schema("AccountEnvelope")).responseFields(fields).build())));
        var listFields = new ArrayList<>(commonResponseFields());
        listFields.add(subsectionWithPath("data").type(JsonFieldType.ARRAY).description("contract 1.0.0 Account[]"));
        mockMvc.perform(get("/api/v1/investment-accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andDo(document("actual-account-list", resource(ResourceSnippetParameters.builder()
                        .tag("Actual Accounts").summary("내 실제 계정 목록").responseSchema(Schema.schema("AccountListEnvelope")).responseFields(listFields).build())));
        mockMvc.perform(patch("/api/v1/investment-accounts/{accountId}", id).header("Authorization", "Bearer " + token)
                .contentType("application/json").header("Idempotency-Key", RENAME_KEY).header("If-Match", "\"0\"").content("{\"name\":\"새장부\"}"))
                .andExpect(status().isOk()).andDo(document("actual-account-rename", resource(ResourceSnippetParameters.builder()
                        .tag("Actual Accounts").summary("내 실제 계정 이름 변경").responseSchema(Schema.schema("AccountEnvelope")).responseFields(fields).build())));
    }

    @Configuration
    @Profile("actual-contract-test")
    @EnableAutoConfiguration
    @EnableConfigurationProperties(JwtProperties.class)
    @EntityScan(basePackages = {"org.stockwellness.domain.actual", "org.stockwellness.domain.member"})
    @EnableJpaRepositories(basePackages = "org.stockwellness.adapter.out.persistence.actual")
    @ComponentScan(basePackages = {"org.stockwellness.adapter.in.web.actual",
            "org.stockwellness.application.service.actual", "org.stockwellness.adapter.out.persistence.actual"})
    @Import({JwtProvider.class, SecurityConfig.class, JwtAuthenticationFilter.class, JwtExceptionFilter.class,
            JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
    static class ActualTestApplication { }
}
