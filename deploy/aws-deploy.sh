#!/usr/bin/env bash
# Đồng bộ source hiện tại lên EC2, build lại API/client và kiểm tra ứng dụng công khai.
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
api_root="$(cd -- "$script_dir/.." && pwd)"
config_path="${PERSONAL_FINANCE_DEPLOY_CONFIG:-$script_dir/aws-deploy.local.env}"
remote_root="/opt/personal-finance"

log() {
  printf '\n==> %s\n' "$*"
}

usage() {
  cat <<'TEXT'
Cách dùng:
  ./deploy/aws-deploy.sh          Đồng bộ source, build và triển khai.
  ./deploy/aws-deploy.sh --check  Chỉ kiểm tra cấu hình và kết nối EC2.

Lần đầu, sao chép deploy/aws-deploy.local.env.example thành
deploy/aws-deploy.local.env, rồi điền đường dẫn khóa SSH và máy chủ.
TEXT
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ ! -f "$config_path" ]]; then
  printf 'Thiếu file cấu hình: %s\n' "$config_path" >&2
  printf 'Hãy tạo từ deploy/aws-deploy.local.env.example.\n' >&2
  exit 1
fi

# File cục bộ này chỉ chứa địa chỉ máy chủ và khóa SSH, không chứa mật khẩu ứng dụng.
# shellcheck disable=SC1090
source "$config_path"

: "${AWS_HOST:?Thiếu AWS_HOST trong file cấu hình.}"
: "${AWS_SSH_KEY:?Thiếu AWS_SSH_KEY trong file cấu hình.}"
AWS_SSH_USER="${AWS_SSH_USER:-ubuntu}"
FRONTEND_ROOT="${FRONTEND_ROOT:-$api_root/../personal-finance-frontend}"

if [[ ! -f "$AWS_SSH_KEY" ]]; then
  printf 'Không tìm thấy khóa SSH: %s\n' "$AWS_SSH_KEY" >&2
  exit 1
fi

if [[ ! -d "$FRONTEND_ROOT" ]]; then
  printf 'Không tìm thấy thư mục frontend: %s\n' "$FRONTEND_ROOT" >&2
  exit 1
fi

if ! command -v rsync >/dev/null 2>&1; then
  printf 'Máy Mac cần có rsync để đồng bộ source.\n' >&2
  exit 1
fi

ssh_options=(
  -i "$AWS_SSH_KEY"
  -o StrictHostKeyChecking=yes
  -o BatchMode=yes
)

if [[ -n "${AWS_KNOWN_HOSTS:-}" ]]; then
  if [[ ! -f "$AWS_KNOWN_HOSTS" ]]; then
    printf 'Không tìm thấy file xác thực máy chủ: %s\n' "$AWS_KNOWN_HOSTS" >&2
    exit 1
  fi
  ssh_options+=( -o "UserKnownHostsFile=$AWS_KNOWN_HOSTS" )
fi

ssh_target="$AWS_SSH_USER@$AWS_HOST"

run_ssh() {
  ssh "${ssh_options[@]}" "$ssh_target" "$@"
}

log "Kiểm tra kết nối với EC2"
run_ssh "test -f '$remote_root/personal-finance-api/deploy/.env' && sudo docker version --format '{{.Server.Version}}'"

if [[ "${1:-}" == "--check" ]]; then
  printf 'Kết nối và cấu hình triển khai đều sẵn sàng.\n'
  exit 0
fi

if [[ -n "${1:-}" ]]; then
  usage >&2
  exit 1
fi

if ! run_ssh 'command -v rsync >/dev/null 2>&1'; then
  log "Cài rsync một lần trên EC2"
  run_ssh 'sudo apt-get update && sudo apt-get install -y rsync'
fi

# Dùng rsync --delete để bản triển khai phản ánh đúng source hiện tại.
# deploy/.env bị loại trừ nên JWT, Gmail SMTP và PUBLIC_HOST trên EC2 được giữ nguyên.
rsync_ssh=(ssh "${ssh_options[@]}")
printf -v rsync_shell '%q ' "${rsync_ssh[@]}"

log "Đồng bộ API"
rsync -az --delete \
  --exclude '.git/' \
  --exclude '.idea/' \
  --exclude 'target/' \
  --exclude 'data/' \
  --exclude 'config/mail-local.properties' \
  --exclude 'deploy/.env' \
  -e "$rsync_shell" \
  "$api_root/" "$ssh_target:$remote_root/personal-finance-api/"

log "Đồng bộ client"
rsync -az --delete \
  --exclude '.git/' \
  --exclude '.idea/' \
  --exclude 'node_modules/' \
  --exclude 'dist/' \
  --exclude '.env' \
  --exclude '.env.local' \
  -e "$rsync_shell" \
  "$FRONTEND_ROOT/" "$ssh_target:$remote_root/personal-finance-frontend/"

log "Build và khởi chạy API + client"
run_ssh 'bash -s' <<'REMOTE'
set -euo pipefail
cd /opt/personal-finance/personal-finance-api
sudo docker compose --env-file deploy/.env -f deploy/compose.yaml up -d --build
sudo docker compose --env-file deploy/.env -f deploy/compose.yaml ps
REMOTE

log "Chờ API sẵn sàng"
# Caddy cấp chứng chỉ cho PUBLIC_HOST, không phải cho IP của EC2.
# Đọc đúng hostname đang dùng từ .env trên máy chủ để kiểm tra TLS và API.
public_host="$(run_ssh "sed -n 's/^PUBLIC_HOST=//p' '$remote_root/personal-finance-api/deploy/.env' | tail -n 1")"
public_url="https://${public_host:-$AWS_HOST}"
for attempt in $(seq 1 30); do
  status="$(curl -sS -o /dev/null -w '%{http_code}' --connect-timeout 5 --max-time 10 "$public_url/api/users/me" || true)"
  if [[ "$status" == "401" ]]; then
    printf 'Triển khai thành công: %s\n' "$public_url"
    exit 0
  fi
  sleep 3
done

printf 'Container đã khởi chạy nhưng API chưa phản hồi đúng sau 90 giây. Kiểm tra log trên EC2.\n' >&2
exit 1
