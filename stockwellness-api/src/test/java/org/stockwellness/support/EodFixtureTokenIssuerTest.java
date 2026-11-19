package org.stockwellness.support;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stockwellness.adapter.out.security.jwt.JwtProperties;
import org.stockwellness.adapter.out.security.jwt.JwtProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EodFixtureTokenIssuer 오프라인 발급, JwtProvider 파싱 및 파일 보안 테스트")
class EodFixtureTokenIssuerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("오프라인에서 1회성 무작위 키와 access token을 0600으로 생성하고 실제 JwtProvider로 9001/USER가 검증되며 stdout에 비밀값이 노출되지 않는다")
    void issues_ephemeral_key_and_access_token_verifiable_by_jwt_provider_without_stdout_leak() throws IOException {
        Path secretFile = tempDir.resolve("nested/eod.secret");
        Path tokenFile = tempDir.resolve("nested/eod.token");

        PrintStream origOut = System.out;
        PrintStream origErr = System.err;
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(outBuf, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(errBuf, true, StandardCharsets.UTF_8));
            EodFixtureTokenIssuer.main(new String[] {
                    "--fixture.secret-out=" + secretFile,
                    "--fixture.token-out=" + tokenFile
            });
        } finally {
            System.setOut(origOut);
            System.setErr(origErr);
        }

        List<String> secretLines = Files.readAllLines(secretFile);
        List<String> tokenLines = Files.readAllLines(tokenFile);
        assertThat(secretLines).hasSize(1);
        assertThat(tokenLines).hasSize(1);
        assertThat(secretLines.get(0)).startsWith("JWT_SECRET=");
        assertThat(tokenLines.get(0)).startsWith("ACCESS_TOKEN=");

        String secretBase64 = secretLines.get(0).substring("JWT_SECRET=".length());
        String accessToken = tokenLines.get(0).substring("ACCESS_TOKEN=".length());
        assertThat(Decoders.BASE64.decode(secretBase64)).hasSize(32);

        // Verify exact 0600 permissions on both files
        assertThat(Files.getPosixFilePermissions(secretFile, LinkOption.NOFOLLOW_LINKS))
                .containsExactlyInAnyOrderElementsOf(PosixFilePermissions.fromString("rw-------"));
        assertThat(Files.getPosixFilePermissions(tokenFile, LinkOption.NOFOLLOW_LINKS))
                .containsExactlyInAnyOrderElementsOf(PosixFilePermissions.fromString("rw-------"));

        // Ensure neither secret nor token was printed to stdout/stderr
        String stdout = outBuf.toString(StandardCharsets.UTF_8);
        String stderr = errBuf.toString(StandardCharsets.UTF_8);
        assertThat(stdout).doesNotContain(secretBase64).doesNotContain(accessToken);
        assertThat(stderr).doesNotContain(secretBase64).doesNotContain(accessToken);

        // Parse with actual JwtProvider (exactly as isolated API JwtAuthenticationFilter does)
        JwtProvider verifier = new JwtProvider(new JwtProperties(secretBase64, 3_600_000L, 0L));
        verifier.init();
        assertThat(verifier.validateAndGetId(accessToken)).isEqualTo(9001L);

        // Verify claims match fixed seeded memberId=9001, USER, KAKAO, eod-fixture@synthetic.test
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64)))
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();
        assertThat(claims.getSubject()).isEqualTo("9001");
        assertThat(claims.get("email", String.class)).isEqualTo("eod-fixture@synthetic.test");
        assertThat(claims.get("nickname", String.class)).isEqualTo("eod-fixture-user");
        assertThat(claims.get("loginType", String.class)).isEqualTo("KAKAO");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(EodFixtureTokenIssuer.ACCESS_TOKEN_EXPIRY_MS);
    }

    @Test
    @DisplayName("호출마다 서로 다른 무작위 서명 키가 생성되어 이전 키의 토큰을 검증할 수 없다")
    void generates_fresh_random_signing_key_on_each_invocation() throws IOException {
        Path secret1 = tempDir.resolve("run1/secret.env");
        Path token1 = tempDir.resolve("run1/token.env");
        Path secret2 = tempDir.resolve("run2/secret.env");
        Path token2 = tempDir.resolve("run2/token.env");

        EodFixtureTokenIssuer.issueToFiles(secret1, token1);
        EodFixtureTokenIssuer.issueToFiles(secret2, token2);

        String key1 = Files.readString(secret1).trim().substring("JWT_SECRET=".length());
        String key2 = Files.readString(secret2).trim().substring("JWT_SECRET=".length());
        String tok1 = Files.readString(token1).trim().substring("ACCESS_TOKEN=".length());

        assertThat(key1).isNotEqualTo(key2);

        JwtProvider providerWithKey2 = new JwtProvider(new JwtProperties(key2, 3_600_000L, 0L));
        providerWithKey2.init();
        assertThat(providerWithKey2.isTokenValid(tok1)).isFalse();
    }

    @Test
    @DisplayName("임의 memberId/role/외부 키/Spring/환경 인자 및 누락·중복·동일 경로를 거부한다")
    void rejects_arbitrary_identity_role_secret_spring_and_invalid_cli_options() {
        Path secret = tempDir.resolve("s.env");
        Path token = tempDir.resolve("t.env");

        assertThatThrownBy(() -> EodFixtureTokenIssuer.parseArgs(new String[] {
                "--fixture.secret-out=" + secret,
                "--fixture.token-out=" + token,
                "--fixture.member-id=1"
        })).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> EodFixtureTokenIssuer.parseArgs(new String[] {
                "--fixture.secret-out=" + secret,
                "--fixture.token-out=" + token,
                "--fixture.role=ADMIN"
        })).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> EodFixtureTokenIssuer.parseArgs(new String[] {
                "--fixture.secret-out=" + secret,
                "--fixture.token-out=" + token,
                "--jwt.secret-key=dGhpcy1pcy1hbi1leHRlcm5hbC1rZXktbm90LWFsbG93ZWQ="
        })).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> EodFixtureTokenIssuer.parseArgs(new String[] {
                "--fixture.secret-out=" + secret,
                "--fixture.token-out=" + token,
                "--spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/mydatabase"
        })).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> EodFixtureTokenIssuer.parseArgs(new String[] {
                "--fixture.token-out=" + token
        })).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> EodFixtureTokenIssuer.parseArgs(new String[] {
                "--fixture.secret-out=" + secret,
                "--fixture.token-out=" + secret
        })).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("기존 파일 및 심볼릭 링크 덮어쓰기를 거부하며 실패 시 부분 산출물을 남기지 않는다")
    void rejects_existing_files_and_symlinks_without_leaving_partial_output() throws IOException {
        Path existingToken = tempDir.resolve("existing.token");
        Files.writeString(existingToken, "ACCESS_TOKEN=keep-me\n");
        Path newSecret = tempDir.resolve("should-not-exist.secret");

        // Precondition check prevents creating secret if token already exists
        assertThatThrownBy(() -> EodFixtureTokenIssuer.issueToFiles(newSecret, existingToken))
                .isInstanceOf(FileAlreadyExistsException.class);
        assertThat(Files.exists(newSecret, LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(Files.readString(existingToken)).isEqualTo("ACCESS_TOKEN=keep-me\n");

        // Dangling symlink target must fail
        Path danglingLink = tempDir.resolve("dangling.token");
        Files.createSymbolicLink(danglingLink, tempDir.resolve("nonexistent"));
        assertThatThrownBy(() -> EodFixtureTokenIssuer.issueToFiles(newSecret, danglingLink))
                .isInstanceOf(FileAlreadyExistsException.class);
        assertThat(Files.exists(newSecret, LinkOption.NOFOLLOW_LINKS)).isFalse();

        // Symlink immediate parent directory must fail
        Path realDir = tempDir.resolve("real-dir");
        Files.createDirectory(realDir);
        Path linkDir = tempDir.resolve("link-dir");
        Files.createSymbolicLink(linkDir, realDir);
        assertThatThrownBy(() -> EodFixtureTokenIssuer.issueToFiles(newSecret, linkDir.resolve("token.env")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("symlink");
        assertThat(Files.exists(newSecret, LinkOption.NOFOLLOW_LINKS)).isFalse();
    }

    @Test
    @DisplayName("오프라인 발급은 DB·Redis·Kafka·외부 환경변수나 .env에 전혀 의존하지 않는다")
    void works_offline_with_zero_dependency_on_spring_db_redis_kafka_or_env() throws IOException {
        Path isolatedDir = tempDir.resolve("clean-cwd");
        Files.createDirectories(isolatedDir);
        Path secret = isolatedDir.resolve("secret.env");
        Path token = isolatedDir.resolve("token.env");

        EodFixtureTokenIssuer.issueToFiles(secret, token);

        String secretBase64 = Files.readString(secret).trim().substring("JWT_SECRET=".length());
        String accessToken = Files.readString(token).trim().substring("ACCESS_TOKEN=".length());
        JwtProvider provider = new JwtProvider(new JwtProperties(secretBase64, 3_600_000L, 0L));
        provider.init();
        assertThat(provider.validateAndGetId(accessToken)).isEqualTo(9001L);
    }

    @Test
    @DisplayName("tokenOut 상위 조상 경로에 일반 파일이 있으면 실패 시 secretOut과 새 디렉터리를 남기지 않고 기존 파일도 보존한다")
    void cleans_up_or_prevents_secret_when_token_ancestor_is_regular_file() throws IOException {
        Path secretDir = tempDir.resolve("fresh-secret-dir");
        Path secretFile = secretDir.resolve("eod.secret");
        Path regularBlocker = tempDir.resolve("blocker-file");
        Files.writeString(regularBlocker, "preserve-existing-content\n");
        Path tokenUnderFile = regularBlocker.resolve("nested/eod.token");

        assertThatThrownBy(() -> EodFixtureTokenIssuer.issueToFiles(secretFile, tokenUnderFile))
                .isInstanceOf(IOException.class);

        assertThat(Files.exists(secretFile, LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(Files.exists(secretDir, LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(Files.readString(regularBlocker)).isEqualTo("preserve-existing-content\n");

        Path tokenDir = tempDir.resolve("fresh-token-dir");
        Path tokenFile = tokenDir.resolve("eod.token");
        Path secretUnderFile = regularBlocker.resolve("nested/eod.secret");
        assertThatThrownBy(() -> EodFixtureTokenIssuer.issueToFiles(secretUnderFile, tokenFile))
                .isInstanceOf(IOException.class);
        assertThat(Files.exists(tokenFile, LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(Files.exists(tokenDir, LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(Files.readString(regularBlocker)).isEqualTo("preserve-existing-content\n");
    }

    @Test
    @DisplayName("두 번째 파일 생성 중 쓰기 권한 오류 등으로 실패하면 이번 호출에서 새로 만든 secret과 디렉터리만 정리하고 기존 디렉터리는 보존한다")
    void rolls_back_newly_created_secret_and_dirs_when_second_file_creation_fails() throws IOException {
        Path secretParent = tempDir.resolve("new-secret-parent/deep");
        Path secretFile = secretParent.resolve("eod.secret");
        Path readOnlyTokenDir = tempDir.resolve("ro-token-dir");
        Files.createDirectory(readOnlyTokenDir);
        Path preExistingSibling = readOnlyTokenDir.resolve("keep.txt");
        Files.writeString(preExistingSibling, "keep-me\n");
        Files.setPosixFilePermissions(readOnlyTokenDir, PosixFilePermissions.fromString("r-xr-xr-x"));

        try {
            Path tokenFile = readOnlyTokenDir.resolve("eod.token");
            assertThatThrownBy(() -> EodFixtureTokenIssuer.issueToFiles(secretFile, tokenFile))
                    .isInstanceOf(IOException.class);

            assertThat(Files.exists(secretFile, LinkOption.NOFOLLOW_LINKS)).isFalse();
            assertThat(Files.exists(secretParent, LinkOption.NOFOLLOW_LINKS)).isFalse();
            assertThat(Files.exists(tempDir.resolve("new-secret-parent"), LinkOption.NOFOLLOW_LINKS)).isFalse();
            assertThat(Files.exists(readOnlyTokenDir, LinkOption.NOFOLLOW_LINKS)).isTrue();
            assertThat(Files.readString(preExistingSibling)).isEqualTo("keep-me\n");
        } finally {
            Files.setPosixFilePermissions(readOnlyTokenDir, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
    }

    @Test
    @DisplayName("macOS /tmp 및 /var 기반 mktemp -d 통상 경로의 하위 디렉터리 발급은 정상 동작한다")
    void preserves_issuance_under_macos_tmp_and_var_paths() throws IOException {
        for (Path root : List.of(Path.of("/tmp"), Path.of("/var/tmp"))) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            Path runDir = Files.createTempDirectory(root, "sw-t337-test-");
            Path nestedOut = runDir.resolve("sub");
            Path secretFile = nestedOut.resolve("eod.secret");
            Path tokenFile = nestedOut.resolve("eod.token");
            try {
                EodFixtureTokenIssuer.issueToFiles(secretFile, tokenFile);
                assertThat(Files.getPosixFilePermissions(secretFile, LinkOption.NOFOLLOW_LINKS))
                        .containsExactlyInAnyOrderElementsOf(PosixFilePermissions.fromString("rw-------"));
                assertThat(Files.getPosixFilePermissions(tokenFile, LinkOption.NOFOLLOW_LINKS))
                        .containsExactlyInAnyOrderElementsOf(PosixFilePermissions.fromString("rw-------"));
            } finally {
                Files.deleteIfExists(secretFile);
                Files.deleteIfExists(tokenFile);
                Files.deleteIfExists(nestedOut);
                Files.deleteIfExists(runDir);
            }
        }
    }
}
