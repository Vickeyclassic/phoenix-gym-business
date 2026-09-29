#!/usr/bin/env bash
# Production host only. Credentials come from the container environment, never argv.
set -euo pipefail
umask 077
if [ "$#" -ne 3 ]; then
  echo 'Usage: backup-mysql.sh /absolute/compose.yml /absolute/private.env /absolute/backup-directory' >&2
  exit 2
fi
compose_file=$(realpath "$1")
env_file=$(realpath "$2")
mkdir -p "$3"
backup_dir=$(realpath "$3")
output="$backup_dir/phoenix_business_$(date -u +%Y%m%dT%H%M%SZ)_$$.sql.gz"
partial="$output.partial"
trap 'rm -f -- "$partial"' EXIT
compose=(docker compose --env-file "$env_file" -p phoenix-bg-fitness-prod -f "$compose_file")
"${compose[@]}" exec -T mysql sh -c 'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysqldump -uroot --single-transaction --routines --triggers --no-tablespaces --set-gtid-purged=OFF phoenix_business' | gzip > "$partial"
gzip -t "$partial"
mv -- "$partial" "$output"
sha256sum "$output" > "$output.sha256"
echo "Backup created: $output"
