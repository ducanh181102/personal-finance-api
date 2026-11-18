#!/usr/bin/env bash
# Dừng riêng API trong ít phút để tạo bản sao H2 nhất quán rồi tải lên S3.
set -euo pipefail

: "${BACKUP_BUCKET:?Thiếu BACKUP_BUCKET trong /etc/personal-finance-backup.env}"
: "${AWS_DEFAULT_REGION:?Thiếu AWS_DEFAULT_REGION trong /etc/personal-finance-backup.env}"

readonly project_dir="/opt/personal-finance/personal-finance-api"
readonly compose_file="deploy/compose.yaml"
readonly compose_env="deploy/.env"
readonly database_volume="personal-finance_finance_data"
readonly local_backup_dir="/var/backups/personal-finance"
readonly backup_name="personal-finance-$(date +%Y%m%d-%H%M%S).tar.gz"
readonly backup_path="$local_backup_dir/$backup_name"

api_was_stopped=false

restart_api() {
  if [[ "$api_was_stopped" == true ]]; then
    docker compose --env-file "$compose_env" -f "$compose_file" start api || true
  fi
}

trap restart_api EXIT

mkdir -p "$local_backup_dir"
cd "$project_dir"

# Không sao chép trực tiếp file H2 khi ứng dụng còn đang ghi dữ liệu.
if docker compose --env-file "$compose_env" -f "$compose_file" ps --status running --services | grep -qx api; then
  docker compose --env-file "$compose_env" -f "$compose_file" stop --timeout 45 api
  api_was_stopped=true
fi

# busybox được cài sẵn lúc cấu hình backup; volume được chỉ đọc trong container tạm.
docker run --rm \
  -v "$database_volume:/source:ro" \
  -v "$local_backup_dir:/backup" \
  busybox:1.37 \
  tar -C /source -czf "/backup/$backup_name" .

aws s3 cp "$backup_path" "s3://$BACKUP_BUCKET/$backup_name" --sse AES256 --only-show-errors
aws s3api head-object --bucket "$BACKUP_BUCKET" --key "$backup_name" >/dev/null

# Giữ một tuần trên máy chủ để có phương án khôi phục nhanh; S3 sẽ giữ 30 ngày.
find "$local_backup_dir" -type f -name 'personal-finance-*.tar.gz' -mtime +7 -delete

restart_api
api_was_stopped=false
trap - EXIT

printf 'Sao lưu thành công: s3://%s/%s\n' "$BACKUP_BUCKET" "$backup_name"
