# Specification: Kakao OAuth2 Login Implementation

## 1. Overview
Implement Kakao OAuth2 authentication to allow users to sign up and log in to Stockwellness using their Kakao accounts. This will follow the standard Spring Security OAuth2 server-side flow, integrating with the existing JWT-based authentication system.

## 2. Functional Requirements

### 2.1 OAuth2 Flow (Server-Side)
- **Initiation:** The client (frontend) initiates login by navigating to the backend endpoint (e.g., `/oauth2/authorization/kakao`).
- **Handshake:** Spring Security handles the redirect to Kakao's authorization server and the subsequent code-to-token exchange.
- **User Info Fetching:** Upon successful authentication, the backend fetches user profile data (specifically email and nickname) from Kakao's User Info API.

### 2.2 User Registration & Mapping
- **Identity Resolution:** Use the `email` provided by Kakao as the unique identifier.
- **Member Creation:** 
    - If a `Member` with the given email does not exist, create a new record.
    - **Nickname:** Auto-generate a unique nickname (e.g., `KakaoUser_{8-digit-random-id}`).
    - **LoginType:** Set to `KAKAO`.
    - **Role:** Default to `USER`.
    - **Status:** Set to `ACTIVE`.
- **Existing User:** If the user already exists, update their last login status (if tracked) and proceed to token issuance.

### 2.3 Success Handling & Token Issuance
- **JWT Generation:** Generate Access Token and Refresh Token using the existing `AuthService` logic.
- **Client Delivery:** Redirect the user to the frontend callback URL (configured via properties) with tokens as query parameters:
  `{FRONTEND_URL}/oauth/callback?accessToken={JWT}&refreshToken={JWT}`

### 2.4 Error Handling
- Handle cases where the user denies authorization.
- Handle communication failures with Kakao API.
- Redirect to a frontend error page with appropriate error codes if authentication fails.

## 3. Non-Functional Requirements
- **Security:** Ensure OAuth2 client secrets are managed via environment variables/secrets (not hardcoded).
- **Maintainability:** Adhere to Hexagonal Architecture by placing OAuth2 logic in the appropriate `adapter/in/web` or `adapter/out/security` packages.

## 4. Acceptance Criteria
- [ ] Users can successfully log in via Kakao and be redirected to the frontend with valid JWTs.
- [ ] New users are automatically registered in the database with the `KAKAO` login type.
- [ ] The nickname generation ensures uniqueness and fits within the 20-character limit.
- [ ] Integration tests verify the OAuth2 success/failure scenarios (mocking Kakao API where possible).

## 5. Out of Scope
- Implementation of other OAuth2 providers (Naver, Google).
- Advanced profile syncing (e.g., syncing Kakao profile image updates after initial registration).
