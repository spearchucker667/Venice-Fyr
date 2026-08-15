#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${VENICE_FYR_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}"
ANDROID_ROOT="${VENICE_ANDROID_ROOT:-$PROJECT_ROOT}"
SOURCE_ROOT="${VENICE_FORGE_SOURCE_DIR:-$PROJECT_ROOT/.source/Venice_Forge-desktop}"
REMOTE="${VENICE_FORGE_REMOTE:-https://github.com/spearchucker667/Venice_Forge.git}"
BRANCH="${VENICE_FORGE_BRANCH:-main}"
LOCAL_STATE_DIR="$ANDROID_ROOT/.local"
LOCAL_STATE_FILE="$LOCAL_STATE_DIR/desktop-source.env"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

command -v git >/dev/null 2>&1 || fail "git is required"
[ -d "$ANDROID_ROOT" ] || fail "Android project not found: $ANDROID_ROOT"

mkdir -p "$(dirname "$SOURCE_ROOT")" "$LOCAL_STATE_DIR"

if [ ! -d "$SOURCE_ROOT/.git" ]; then
  if [ -e "$SOURCE_ROOT" ]; then
    fail "Source path exists but is not a Git repository: $SOURCE_ROOT"
  fi
  printf 'Cloning Venice Forge desktop source...\n'
  git clone --branch "$BRANCH" --single-branch "$REMOTE" "$SOURCE_ROOT"
else
  actual_origin="$(git -C "$SOURCE_ROOT" remote get-url origin 2>/dev/null || true)"
  normalized_actual="${actual_origin%.git}"
  normalized_expected="${REMOTE%.git}"
  [ "$normalized_actual" = "$normalized_expected" ] || fail "Unexpected origin for source mirror: $actual_origin (expected $REMOTE)"

  if [ -n "$(git -C "$SOURCE_ROOT" status --porcelain --untracked-files=normal)" ]; then
    fail "Desktop source mirror has local changes. Preserve/review them before refresh: $SOURCE_ROOT"
  fi

  printf 'Refreshing Venice Forge desktop source...\n'
  git -C "$SOURCE_ROOT" fetch --prune origin "$BRANCH"
  git -C "$SOURCE_ROOT" checkout "$BRANCH" >/dev/null 2>&1
  if ! git -C "$SOURCE_ROOT" merge --ff-only "origin/$BRANCH" >/dev/null 2>&1; then
    fail "Could not fast-forward desktop source mirror. Manual intervention required: $SOURCE_ROOT"
  fi
fi

head_sha="$(git -C "$SOURCE_ROOT" rev-parse HEAD)"
origin_url="$(git -C "$SOURCE_ROOT" remote get-url origin)"
branch_name="$(git -C "$SOURCE_ROOT" branch --show-current)"

[ "$origin_url" = "$REMOTE" ] || fail "Origin changed unexpectedly after bootstrap"
[ "$branch_name" = "$BRANCH" ] || fail "Expected branch $BRANCH but found $branch_name"
[ -z "$(git -C "$SOURCE_ROOT" status --porcelain --untracked-files=normal)" ] || fail "Source mirror is not clean after bootstrap"

# Shell-escaped values so the file is safe to source even with spaces in paths.
printf 'VENICE_FORGE_DESKTOP_SOURCE=%q\n' "$SOURCE_ROOT" > "$LOCAL_STATE_FILE"
printf 'VENICE_FORGE_DESKTOP_REMOTE=%q\n' "$origin_url" >> "$LOCAL_STATE_FILE"
printf 'VENICE_FORGE_DESKTOP_BRANCH=%q\n' "$branch_name" >> "$LOCAL_STATE_FILE"
printf 'VENICE_FORGE_DESKTOP_HEAD=%q\n' "$head_sha" >> "$LOCAL_STATE_FILE"

printf '\nDesktop source ready.\n'
printf '  Android target : %s\n' "$ANDROID_ROOT"
printf '  Desktop source : %s\n' "$SOURCE_ROOT"
printf '  Remote         : %s\n' "$origin_url"
printf '  Branch         : %s\n' "$branch_name"
printf '  HEAD           : %s\n' "$head_sha"
printf '  Local state    : %s\n' "$LOCAL_STATE_FILE"
printf '\nTreat the desktop checkout as READ ONLY during Android port work.\n'
