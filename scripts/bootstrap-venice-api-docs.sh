#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${VENICE_FYR_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}"
SOURCE_ROOT="${VENICE_API_DOCS_SOURCE_DIR:-$PROJECT_ROOT/.source/venice-api-docs}"
REMOTE="${VENICE_API_DOCS_REMOTE:-https://github.com/veniceai/api-docs.git}"
BRANCH="${VENICE_API_DOCS_BRANCH:-main}"
LOCAL_STATE_DIR="$PROJECT_ROOT/.local"
LOCAL_STATE_FILE="$LOCAL_STATE_DIR/venice-api-docs.env"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

command -v git >/dev/null 2>&1 || fail "git is required"
[ -d "$PROJECT_ROOT" ] || fail "Venice Fyr project directory not found: $PROJECT_ROOT"

mkdir -p "$(dirname "$SOURCE_ROOT")" "$LOCAL_STATE_DIR"

if [ ! -d "$SOURCE_ROOT/.git" ]; then
  if [ -e "$SOURCE_ROOT" ]; then
    fail "Source path exists but is not a Git repository: $SOURCE_ROOT"
  fi
  printf 'Cloning official Venice API docs repository...\n'
  git clone --branch "$BRANCH" --single-branch "$REMOTE" "$SOURCE_ROOT"
else
  actual_origin="$(git -C "$SOURCE_ROOT" remote get-url origin 2>/dev/null || true)"
  # Tolerate either with or without .git suffix
  normalized_actual="${actual_origin%.git}"
  normalized_expected="${REMOTE%.git}"
  [ "$normalized_actual" = "$normalized_expected" ] || fail "Unexpected origin for source mirror: $actual_origin (expected $REMOTE)"

  if [ -n "$(git -C "$SOURCE_ROOT" status --porcelain --untracked-files=normal)" ]; then
    fail "Venice API docs source mirror has local changes. Refusing to overwrite: $SOURCE_ROOT"
  fi

  printf 'Refreshing official Venice API docs source...\n'
  git -C "$SOURCE_ROOT" fetch --prune origin "$BRANCH"
  git -C "$SOURCE_ROOT" checkout "$BRANCH" >/dev/null 2>&1
  if ! git -C "$SOURCE_ROOT" merge --ff-only "origin/$BRANCH" >/dev/null 2>&1; then
    fail "Could not fast-forward Venice API docs source mirror. Manual intervention required: $SOURCE_ROOT"
  fi
fi

head_sha="$(git -C "$SOURCE_ROOT" rev-parse HEAD)"
origin_url="$(git -C "$SOURCE_ROOT" remote get-url origin)"
branch_name="$(git -C "$SOURCE_ROOT" branch --show-current)"

[ -z "$(git -C "$SOURCE_ROOT" status --porcelain --untracked-files=normal)" ] || fail "Source mirror is not clean after bootstrap"

# Extract Swagger info.version safely
swagger_file="$SOURCE_ROOT/swagger.yaml"
swagger_version="unknown"
if [ -f "$swagger_file" ]; then
  # Look for version line under info: section
  swagger_version="$(awk '/^info:/{flag=1; next} /^[^ ]/{flag=0} flag && /^[[:space:]]+version:/{gsub(/^[[:space:]]+version:[[:space:]]*["\x27]?|["\x27]?[[:space:]]*$/, ""); print; exit}' "$swagger_file" || true)"
  [ -n "$swagger_version" ] || swagger_version="unknown"
fi

# Write shell-sourceable state file
printf 'VENICE_API_DOCS_SOURCE=%q\n' "$SOURCE_ROOT" > "$LOCAL_STATE_FILE"
printf 'VENICE_API_DOCS_REMOTE=%q\n' "$origin_url" >> "$LOCAL_STATE_FILE"
printf 'VENICE_API_DOCS_BRANCH=%q\n' "$branch_name" >> "$LOCAL_STATE_FILE"
printf 'VENICE_API_DOCS_HEAD=%q\n' "$head_sha" >> "$LOCAL_STATE_FILE"
printf 'VENICE_API_SWAGGER_VERSION=%q\n' "$swagger_version" >> "$LOCAL_STATE_FILE"

printf '\nVenice API Docs source ready.\n'
printf '  Project root   : %s\n' "$PROJECT_ROOT"
printf '  API docs source: %s\n' "$SOURCE_ROOT"
printf '  Remote         : %s\n' "$origin_url"
printf '  Branch         : %s\n' "$branch_name"
printf '  HEAD           : %s\n' "$head_sha"
printf '  Swagger version: %s\n' "$swagger_version"
printf '  Local state    : %s\n' "$LOCAL_STATE_FILE"
printf '\nTreat the Venice API docs checkout as READ ONLY reference data.\n'
