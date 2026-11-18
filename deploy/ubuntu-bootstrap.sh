#!/usr/bin/env bash
# Chạy dưới quyền root trên VM Ubuntu mới để chuẩn bị Docker và vùng nhớ build.
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

# Dùng kho Docker chính thức cho phiên bản Ubuntu và kiến trúc của VM.
. /etc/os-release
cat > /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: ${UBUNTU_CODENAME:-$VERSION_CODENAME}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker

# Swap giúp VM nhỏ hoàn thành bước build Maven/Vite khi thiếu RAM tạm thời.
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  printf '/swapfile none swap sw 0 0\n' >> /etc/fstab
fi
printf 'vm.swappiness=10\n' > /etc/sysctl.d/90-personal-finance.conf
sysctl -p /etc/sysctl.d/90-personal-finance.conf

install -d -m 0750 -o ubuntu -g ubuntu /opt/personal-finance
