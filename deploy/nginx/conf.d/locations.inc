# ================================================================
# 공통 location 블록 — app.conf의 80/443 server 블록에서 include
# ================================================================

# gzip 압축
gzip on;
gzip_types text/plain text/css application/javascript application/json image/svg+xml;
gzip_min_length 1024;
gzip_comp_level 6;

proxy_set_header Host              $host;
proxy_set_header X-Real-IP         $remote_addr;
proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $real_scheme;
proxy_set_header X-Forwarded-Port  $server_port;
proxy_set_header X-Forwarded-Host  $host;

# 보안 헤더
add_header X-Frame-Options        "SAMEORIGIN"            always;
add_header X-Content-Type-Options "nosniff"               always;
add_header X-XSS-Protection       "1; mode=block"         always;
add_header Referrer-Policy        "strict-origin-when-cross-origin" always;

# 로드밸런서 헬스체크
location /health {
    return 200 "OK\n";
}

# Actuator 외부 접근 차단
location ~ ^/api/actuator {
    return 403;
}

# API 라우팅
location /api/ {
    proxy_pass http://$api_upstream;
}

# OAuth2 시작 경로
location /oauth2 {
    proxy_pass http://$api_upstream;
}

# OAuth2 콜백 경로
location /login {
    proxy_pass http://$api_upstream;
}

# Service Worker — 정적 파일로 서빙 (캐시 무효화)
location = /sw.js {
    root /usr/share/nginx/html;
    add_header Cache-Control "no-cache, no-store, must-revalidate";
}

# Front-End SPA 라우팅
location / {
    proxy_pass http://$front_upstream;
    proxy_intercept_errors on;
    error_page 404 = /index.html;
}

error_page 502 /502.html;
location = /502.html {
    root /usr/share/nginx/html;
    internal;
}
