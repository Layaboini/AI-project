#!/bin/sh
# Append a tamper-evident, hash-chained entry to prompts/prompt-history.md.
#
# POSIX shell: works on macOS and Linux out of the box (and Git Bash / WSL on
# Windows). No Python required. Produces exactly the same hash chain as
# log.py / log.ps1, so the three loggers are interchangeable within one history.
#
# Usage:
#   sh prompts/log.sh "<verbatim prompt>" "<one-line action>"
#   sh prompts/log.sh --verify
#
# Needs a SHA-256 tool: sha256sum (Linux) or shasum (macOS) — both ship by default.

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
HISTORY="$SCRIPT_DIR/prompt-history.md"
GENESIS="GENESIS"
US=$(printf '\037')  # ASCII unit separator, used only as an internal delimiter

sha256() {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum | cut -d' ' -f1
    else
        shasum -a 256 | cut -d' ' -f1
    fi
}

# Collapse all whitespace runs to single spaces and trim
# (matches Python's " ".join(text.split())).
one_line() {
    printf '%s' "$1" | tr -s '[:space:]' ' ' | sed -e 's/^ //' -e 's/ $//'
}

# SHA-256 hex digest of prev\nts\nprompt\naction (LF joins, UTF-8 bytes).
digest() {
    printf '%s\n%s\n%s\n%s' "$1" "$2" "$3" "$4" | sha256
}

last_hash() {
    if [ -f "$HISTORY" ]; then
        h=$(tr -d '\r' < "$HISTORY" | grep -E '^\*\*Hash:\*\* [0-9a-f]{64}$' 2>/dev/null | tail -n 1 | awk '{print $2}')
        if [ -n "${h:-}" ]; then
            printf '%s' "$h"
            return 0
        fi
    fi
    printf '%s' "$GENESIS"
}

append() {
    p=$(one_line "$1")
    a=$(one_line "$2")
    prev=$(last_hash)
    ts=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    dg=$(digest "$prev" "$ts" "$p" "$a")
    block=$(printf '### %s\n**Prompt:** %s\n**Action:** %s\n**Prev:** %s\n**Hash:** %s' "$ts" "$p" "$a" "$prev" "$dg")

    if [ -s "$HISTORY" ]; then
        existing=$(tr -d '\r' < "$HISTORY")   # strip CR and (via $()) trailing newlines
        printf '%s\n\n%s\n\n' "$existing" "$block" > "$HISTORY"
    else
        printf '%s\n\n' "$block" > "$HISTORY"
    fi
    printf 'logged %s (%s...)\n' "$ts" "$(printf '%s' "$dg" | cut -c1-12)"
}

verify() {
    if [ ! -f "$HISTORY" ]; then
        echo "no prompt-history.md found"
        return 1
    fi
    parsed=$(tr -d '\r' < "$HISTORY" | awk '
        /^### / { ts=$0; sub(/^### /,"",ts) }
        /^\*\*Prompt:\*\* / { p=$0; sub(/^\*\*Prompt:\*\* /,"",p) }
        /^\*\*Action:\*\* / { a=$0; sub(/^\*\*Action:\*\* /,"",a) }
        /^\*\*Prev:\*\* / { pv=$0; sub(/^\*\*Prev:\*\* /,"",pv) }
        /^\*\*Hash:\*\* / { h=$0; sub(/^\*\*Hash:\*\* /,"",h);
                            printf "%s\037%s\037%s\037%s\037%s\n", ts, p, a, pv, h }
    ')

    if [ -z "$parsed" ]; then
        echo "no hash-chained entries found"
        return 1
    fi

    tmpf=$(mktemp 2>/dev/null || printf '%s' "${TMPDIR:-/tmp}/loghist.$$")
    printf '%s\n' "$parsed" > "$tmpf"

    prev="$GENESIS"
    last_ts=""
    i=0
    rc=0
    while IFS="$US" read -r ts p a pv h; do
        [ -z "$h" ] && continue
        i=$((i + 1))
        if [ "$pv" != "$prev" ]; then
            echo "BROKEN CHAIN at entry $i: prev mismatch"; rc=1; break
        fi
        want=$(digest "$pv" "$ts" "$(one_line "$p")" "$(one_line "$a")")
        if [ "$want" != "$h" ]; then
            echo "TAMPERED at entry $i: hash mismatch"; rc=1; break
        fi
        if [ -n "$last_ts" ] && awk -v x="$ts" -v y="$last_ts" 'BEGIN { exit !(x < y) }'; then
            echo "NON-MONOTONIC timestamp at entry $i: $ts < $last_ts"; rc=1; break
        fi
        last_ts="$ts"
        prev="$h"
    done < "$tmpf"
    rm -f "$tmpf"

    if [ "$rc" -eq 0 ]; then
        echo "OK: $i entries form a valid chain"
    fi
    return "$rc"
}

case "${1:-}" in
    --verify)
        verify
        ;;
    "")
        echo 'Usage: log.sh "<verbatim prompt>" "<one-line action>"   |   log.sh --verify'
        exit 2
        ;;
    *)
        if [ "$#" -ne 2 ]; then
            echo 'Usage: log.sh "<verbatim prompt>" "<one-line action>"   |   log.sh --verify'
            exit 2
        fi
        append "$1" "$2"
        ;;
esac
