#!/usr/bin/env bash
# Tạo S3 riêng tư + quyền tối thiểu cho EC2, rồi cài lịch sao lưu hằng ngày trên VM.
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
config_path="${PERSONAL_FINANCE_DEPLOY_CONFIG:-$script_dir/aws-deploy.local.env}"

log() {
  printf '\n==> %s\n' "$*"
}

if [[ ! -f "$config_path" ]]; then
  printf 'Thiếu file cấu hình: %s\n' "$config_path" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "$config_path"

: "${AWS_HOST:?Thiếu AWS_HOST trong file cấu hình.}"
: "${AWS_SSH_KEY:?Thiếu AWS_SSH_KEY trong file cấu hình.}"
: "${AWS_INSTANCE_ID:?Thiếu AWS_INSTANCE_ID trong file cấu hình.}"
AWS_SSH_USER="${AWS_SSH_USER:-ubuntu}"
AWS_REGION="${AWS_REGION:-ap-southeast-2}"
AWS_PROFILE="${AWS_PROFILE:-personal-finance}"

if [[ ! -f "$AWS_SSH_KEY" ]]; then
  printf 'Không tìm thấy khóa SSH: %s\n' "$AWS_SSH_KEY" >&2
  exit 1
fi

aws_cli="${AWS_CLI_PATH:-aws}"
if ! command -v "$aws_cli" >/dev/null 2>&1; then
  printf 'Không tìm thấy AWS CLI: %s\n' "$aws_cli" >&2
  exit 1
fi

aws_cmd=("$aws_cli" --profile "$AWS_PROFILE" --region "$AWS_REGION" --no-cli-pager)
account_id="$("${aws_cmd[@]}" sts get-caller-identity --query Account --output text)"
bucket="personal-finance-backups-$account_id-$AWS_REGION"
role_name="personal-finance-backup-ec2"
policy_name="personal-finance-backup-s3"

ssh_options=(
  -i "$AWS_SSH_KEY"
  -o StrictHostKeyChecking=yes
  -o BatchMode=yes
)
if [[ -n "${AWS_KNOWN_HOSTS:-}" ]]; then
  ssh_options+=( -o "UserKnownHostsFile=$AWS_KNOWN_HOSTS" )
fi
ssh_target="$AWS_SSH_USER@$AWS_HOST"

run_ssh() {
  ssh "${ssh_options[@]}" "$ssh_target" "$@"
}

log "Tạo kho backup S3 riêng tư"
if ! "${aws_cmd[@]}" s3api head-bucket --bucket "$bucket" 2>/dev/null; then
  "${aws_cmd[@]}" s3api create-bucket \
    --bucket "$bucket" \
    --create-bucket-configuration "LocationConstraint=$AWS_REGION"
fi
"${aws_cmd[@]}" s3api put-public-access-block \
  --bucket "$bucket" \
  --public-access-block-configuration 'BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true'
"${aws_cmd[@]}" s3api put-bucket-encryption \
  --bucket "$bucket" \
  --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"},"BucketKeyEnabled":false}]}'
"${aws_cmd[@]}" s3api put-bucket-lifecycle-configuration \
  --bucket "$bucket" \
  --lifecycle-configuration '{"Rules":[{"ID":"expire-backups-after-30-days","Status":"Enabled","Filter":{"Prefix":""},"Expiration":{"Days":30},"AbortIncompleteMultipartUpload":{"DaysAfterInitiation":1}}]}'

log "Cấp quyền tối thiểu cho máy chủ"
trust_policy='{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}'
if ! "${aws_cmd[@]}" iam get-role --role-name "$role_name" >/dev/null 2>&1; then
  "${aws_cmd[@]}" iam create-role --role-name "$role_name" --assume-role-policy-document "$trust_policy" >/dev/null
fi

s3_policy="{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"s3:ListBucket\"],\"Resource\":\"arn:aws:s3:::$bucket\"},{\"Effect\":\"Allow\",\"Action\":[\"s3:GetObject\",\"s3:PutObject\"],\"Resource\":\"arn:aws:s3:::$bucket/*\"}]}"
"${aws_cmd[@]}" iam put-role-policy \
  --role-name "$role_name" \
  --policy-name "$policy_name" \
  --policy-document "$s3_policy"

if ! "${aws_cmd[@]}" iam get-instance-profile --instance-profile-name "$role_name" >/dev/null 2>&1; then
  "${aws_cmd[@]}" iam create-instance-profile --instance-profile-name "$role_name" >/dev/null
fi

attached_roles="$("${aws_cmd[@]}" iam get-instance-profile --instance-profile-name "$role_name" --query 'InstanceProfile.Roles[].RoleName' --output text)"
if [[ "$attached_roles" != *"$role_name"* ]]; then
  "${aws_cmd[@]}" iam add-role-to-instance-profile --instance-profile-name "$role_name" --role-name "$role_name"
fi

# IAM cần một khoảng ngắn để đồng bộ profile mới trước khi EC2 có thể nhận diện nó.
instance_profile_arn="None"
for attempt in $(seq 1 12); do
  instance_profile_arn="$("${aws_cmd[@]}" iam get-instance-profile --instance-profile-name "$role_name" --query 'InstanceProfile.Arn' --output text)"
  if [[ "$instance_profile_arn" != "None" ]]; then
    break
  fi
  sleep 3
done
if [[ "$instance_profile_arn" == "None" ]]; then
  printf 'AWS chưa tạo xong IAM instance profile. Hãy chạy lại script sau ít phút.\n' >&2
  exit 1
fi

current_profile="$("${aws_cmd[@]}" ec2 describe-instances --instance-ids "$AWS_INSTANCE_ID" --query 'Reservations[0].Instances[0].IamInstanceProfile.Arn' --output text)"
if [[ "$current_profile" == "None" ]]; then
  "${aws_cmd[@]}" ec2 associate-iam-instance-profile \
    --instance-id "$AWS_INSTANCE_ID" \
    --iam-instance-profile "Arn=$instance_profile_arn" >/dev/null
elif [[ "$current_profile" != "$instance_profile_arn" ]]; then
  printf 'EC2 đang có IAM profile khác (%s); dừng để tránh ghi đè quyền hiện hữu.\n' "$current_profile" >&2
  exit 1
fi

log "Cài công cụ sao lưu trên EC2"
run_ssh 'bash -s' <<'REMOTE'
set -euo pipefail
sudo timedatectl set-timezone Asia/Ho_Chi_Minh

# Ubuntu ARM trong region này không cung cấp awscli qua apt, nên cài bản chính thức của AWS.
if ! command -v aws >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y unzip
  case "$(uname -m)" in
    aarch64|arm64) aws_arch="aarch64" ;;
    x86_64) aws_arch="x86_64" ;;
    *) printf 'Không hỗ trợ kiến trúc EC2 này: %s\n' "$(uname -m)" >&2; exit 1 ;;
  esac
  temp_dir="$(mktemp -d)"
  trap 'rm -rf "$temp_dir"' EXIT
  curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-$aws_arch.zip" -o "$temp_dir/awscliv2.zip"
  unzip -q "$temp_dir/awscliv2.zip" -d "$temp_dir"
  sudo "$temp_dir/aws/install" --update
fi

sudo docker pull busybox:1.37
REMOTE

scp "${ssh_options[@]}" \
  "$script_dir/backup/personal-finance-backup.sh" \
  "$script_dir/backup/personal-finance-backup.service" \
  "$script_dir/backup/personal-finance-backup.timer" \
  "$ssh_target:/tmp/"

run_ssh "sudo install -m 0750 /tmp/personal-finance-backup.sh /usr/local/sbin/personal-finance-backup && sudo install -m 0644 /tmp/personal-finance-backup.service /etc/systemd/system/personal-finance-backup.service && sudo install -m 0644 /tmp/personal-finance-backup.timer /etc/systemd/system/personal-finance-backup.timer && printf 'BACKUP_BUCKET=%s\\nAWS_DEFAULT_REGION=%s\\n' '$bucket' '$AWS_REGION' | sudo tee /etc/personal-finance-backup.env >/dev/null && sudo chmod 600 /etc/personal-finance-backup.env && sudo systemctl daemon-reload && sudo systemctl enable --now personal-finance-backup.timer && sudo systemctl list-timers --all personal-finance-backup.timer"

log "Kiểm tra EC2 nhận quyền S3"
for attempt in $(seq 1 12); do
  if run_ssh 'aws sts get-caller-identity >/dev/null 2>&1'; then
    printf 'Backup đã sẵn sàng. Bucket: s3://%s\n' "$bucket"
    exit 0
  fi
  sleep 5
done

printf 'IAM role đã gắn nhưng EC2 chưa nhận credential sau 60 giây. Chạy lại script sau ít phút.\n' >&2
exit 1
