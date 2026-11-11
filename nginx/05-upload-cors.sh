#!/bin/sh
# Spring과 같은 명시적 origin 목록을 Nginx 자체 업로드 오류에도 적용한다.
set -eu
map_path=${NGINX_CORS_MAP_PATH:-/etc/nginx/conf.d/00-upload-cors.conf}
tmp_path=$(mktemp)
trap 'rm -f "$tmp_path"' EXIT
printf 'map $http_origin $upload_cors_origin {\n    default "";\n' > "$tmp_path"
IFS=,
set -f
for origin in ${CORS_ALLOWED_ORIGINS:-http://localhost:5173}; do
    origin=$(printf '%s' "$origin" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    [ -n "$origin" ] || continue
    if [ "$origin" = '*' ]; then
        printf '    ~. "*";\n' >> "$tmp_path"
        continue
    fi
    # origin만 허용하고 따옴표·변수·설정 구문이 유입되지 않게 한다.
    if ! printf '%s\n' "$origin" | grep -Eq '^(https?://[A-Za-z0-9._:-]+|https?://\[[0-9a-fA-F:]+\](:[0-9]+)?|null)$'; then
        printf '%s\n' 'Invalid CORS_ALLOWED_ORIGINS origin' >&2
        exit 1
    fi
    printf '    "%s" $http_origin;\n' "$origin" >> "$tmp_path"
done
printf '}\n' >> "$tmp_path"
mv "$tmp_path" "$map_path"
