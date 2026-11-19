package org.stockwellness.support;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.stockwellness.adapter.out.security.jwt.JwtProperties;
import org.stockwellness.adapter.out.security.jwt.JwtProvider;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.MemberRole;

/**
 * Offline test-scope utility that generates an ephemeral HS256 test signing key
 * and issues a single access token for the fixed synthetic seed member (id=9001, USER)
 * using {@link JwtProvider}, without starting Spring or touching DB/Redis/Kafka/network/env.
 */
public final class EodFixtureTokenIssuer {

    static final Long SEEDED_MEMBER_ID = 9001L;
    static final String SEEDED_EMAIL = "eod-fixture@synthetic.test";
    static final String SEEDED_NICKNAME = "eod-fixture-user";
    static final LoginType SEEDED_LOGIN_TYPE = LoginType.KAKAO;
    static final MemberRole SEEDED_ROLE = MemberRole.USER;
    static final LocalDateTime SEEDED_CREATED_AT = LocalDateTime.of(2026, 11, 16, 9, 0);
    static final long ACCESS_TOKEN_EXPIRY_MS = 3_600_000L;
    private static final int SIGNING_KEY_BYTES = 32;

    private static final String SECRET_OUT_FLAG = "--fixture.secret-out=";
    private static final String TOKEN_OUT_FLAG = "--fixture.token-out=";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Set<PosixFilePermission> OWNER_READ_WRITE = PosixFilePermissions.fromString("rw-------");
    private static final Set<PosixFilePermission> OWNER_DIR_PERMS = PosixFilePermissions.fromString("rwx------");

    record OutputPaths(Path secretOut, Path tokenOut) {
    }

    private EodFixtureTokenIssuer() {
    }

    public static void main(String[] args) throws IOException {
        OutputPaths paths = parseArgs(args);
        issueToFiles(paths.secretOut(), paths.tokenOut());
        System.out.printf("Issued offline EOD fixture credentials for memberId=%d (%s) -> secret=%s, token=%s%n",
                SEEDED_MEMBER_ID, SEEDED_ROLE.name(), paths.secretOut(), paths.tokenOut());
    }

    static OutputPaths parseArgs(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException("Arguments must not be null");
        }
        Path secretOut = null;
        Path tokenOut = null;
        for (String arg : args) {
            if (arg == null || arg.isBlank()) {
                throw new IllegalArgumentException("Arguments must not contain blank entries");
            }
            if (arg.startsWith(SECRET_OUT_FLAG)) {
                if (secretOut != null) {
                    throw new IllegalArgumentException("Duplicate --fixture.secret-out flag");
                }
                secretOut = parseOutputPath(arg.substring(SECRET_OUT_FLAG.length()), SECRET_OUT_FLAG);
            } else if (arg.startsWith(TOKEN_OUT_FLAG)) {
                if (tokenOut != null) {
                    throw new IllegalArgumentException("Duplicate --fixture.token-out flag");
                }
                tokenOut = parseOutputPath(arg.substring(TOKEN_OUT_FLAG.length()), TOKEN_OUT_FLAG);
            } else {
                throw new IllegalArgumentException("Unsupported option (fixed seed identity & ephemeral key only): " + arg);
            }
        }
        if (secretOut == null || tokenOut == null) {
            throw new IllegalArgumentException("Both --fixture.secret-out=<path> and --fixture.token-out=<path> are required");
        }
        if (secretOut.equals(tokenOut)) {
            throw new IllegalArgumentException("--fixture.secret-out and --fixture.token-out must be distinct files");
        }
        return new OutputPaths(secretOut, tokenOut);
    }

    private static Path parseOutputPath(String rawPath, String flag) {
        String trimmed = rawPath.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(flag + " must not be empty");
        }
        return Path.of(trimmed).toAbsolutePath().normalize();
    }

    static void issueToFiles(Path secretOut, Path tokenOut) throws IOException {
        Path normalizedSecret = secretOut.toAbsolutePath().normalize();
        Path normalizedToken = tokenOut.toAbsolutePath().normalize();
        if (normalizedSecret.equals(normalizedToken)) {
            throw new IllegalArgumentException("secretOut and tokenOut must be distinct files");
        }

        validateTargetPreconditions(normalizedSecret);
        validateTargetPreconditions(normalizedToken);

        String secretKeyBase64 = generateRandomSecretBase64();
        JwtProvider jwtProvider = new JwtProvider(
                new JwtProperties(secretKeyBase64, ACCESS_TOKEN_EXPIRY_MS, 0L)
        );
        jwtProvider.init();
        String accessToken = jwtProvider.generateAccessToken(
                SEEDED_MEMBER_ID,
                SEEDED_EMAIL,
                SEEDED_NICKNAME,
                SEEDED_LOGIN_TYPE,
                SEEDED_ROLE,
                SEEDED_CREATED_AT
        );

        List<Path> createdPaths = new ArrayList<>();
        try {
            prepareParentDirectory(normalizedSecret.getParent(), createdPaths);
            prepareParentDirectory(normalizedToken.getParent(), createdPaths);
            writeOwnerOnlyNewFile(normalizedSecret, "JWT_SECRET=" + secretKeyBase64 + System.lineSeparator(), createdPaths);
            writeOwnerOnlyNewFile(normalizedToken, "ACCESS_TOKEN=" + accessToken + System.lineSeparator(), createdPaths);
        } catch (IOException | RuntimeException | Error failure) {
            rollbackCreatedPaths(createdPaths, failure);
            throw failure;
        }
    }

    static String generateRandomSecretBase64() {
        byte[] rawKey = new byte[SIGNING_KEY_BYTES];
        SECURE_RANDOM.nextBytes(rawKey);
        try {
            return Base64.getEncoder().encodeToString(rawKey);
        } finally {
            Arrays.fill(rawKey, (byte) 0);
        }
    }

    static void validateTargetPreconditions(Path normalizedTarget) throws IOException {
        if (!normalizedTarget.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            throw new IllegalStateException("POSIX file permissions are required for secret/token output: " + normalizedTarget);
        }
        if (Files.isSymbolicLink(normalizedTarget) || Files.exists(normalizedTarget, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileAlreadyExistsException("Refusing to overwrite existing file or symlink: " + normalizedTarget);
        }
        Path parent = normalizedTarget.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Target path must have a parent directory: " + normalizedTarget);
        }
        Path existingAncestor = findNearestExistingAncestor(parent);
        if (existingAncestor == null) {
            throw new IllegalArgumentException("Target path has no existing ancestor directory: " + normalizedTarget);
        }
        if (existingAncestor.equals(parent)) {
            if (Files.isSymbolicLink(parent) || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException("Refusing output under symlink or non-directory parent: " + parent);
            }
        } else if (!Files.isDirectory(existingAncestor)) {
            throw new FileAlreadyExistsException(
                    existingAncestor.toString(),
                    null,
                    "Parent ancestor exists and is not a directory"
            );
        }
    }

    private static Path findNearestExistingAncestor(Path start) {
        Path cursor = start;
        while (cursor != null && !Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)) {
            cursor = cursor.getParent();
        }
        return cursor;
    }

    private static void prepareParentDirectory(Path parent, List<Path> createdPaths) throws IOException {
        Deque<Path> missingDirs = new ArrayDeque<>();
        Path cursor = parent;
        while (cursor != null && !Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)) {
            missingDirs.push(cursor);
            cursor = cursor.getParent();
        }
        if (cursor == null) {
            throw new IllegalArgumentException("Parent path has no existing ancestor directory: " + parent);
        }
        if (missingDirs.isEmpty()) {
            if (Files.isSymbolicLink(parent) || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException("Refusing output under symlink or non-directory parent: " + parent);
            }
            return;
        }
        if (!Files.isDirectory(cursor)) {
            throw new FileAlreadyExistsException(
                    cursor.toString(),
                    null,
                    "Parent ancestor exists and is not a directory"
            );
        }
        while (!missingDirs.isEmpty()) {
            Path dir = missingDirs.pop();
            Files.createDirectory(dir, PosixFilePermissions.asFileAttribute(OWNER_DIR_PERMS));
            createdPaths.add(dir);
        }
    }

    private static void writeOwnerOnlyNewFile(Path normalized, String content, List<Path> createdPaths) throws IOException {
        try (FileChannel channel = FileChannel.open(
                normalized,
                EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
                PosixFilePermissions.asFileAttribute(OWNER_READ_WRITE)
        )) {
            createdPaths.add(normalized);
            ByteBuffer buffer = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }

        PosixFileAttributeView view = Files.getFileAttributeView(normalized, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        PosixFileAttributes attrs = view == null ? null : view.readAttributes();
        if (attrs == null || !attrs.isRegularFile() || attrs.isSymbolicLink()
                || !OWNER_READ_WRITE.equals(attrs.permissions())) {
            throw new IllegalStateException("Created file does not satisfy strict 0600 regular file check: " + normalized);
        }
    }

    private static void rollbackCreatedPaths(List<Path> createdPaths, Throwable failure) {
        for (int i = createdPaths.size() - 1; i >= 0; i--) {
            Path created = createdPaths.get(i);
            try {
                Files.deleteIfExists(created);
            } catch (IOException | RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
    }
}
