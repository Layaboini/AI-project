#!/usr/bin/env pwsh
# Append a tamper-evident, hash-chained entry to prompts/prompt-history.md.
#
# Cross-platform: Windows PowerShell 5.1 and PowerShell 7+ (Windows, macOS, Linux).
# No Python required. Produces exactly the same hash chain as log.py / log.sh, so
# the three loggers are interchangeable within one history file.
#
# Usage:
#   pwsh -NoProfile -File prompts/log.ps1 "<verbatim prompt>" "<one-line action>"
#   pwsh -NoProfile -File prompts/log.ps1 -Verify
#
# On Windows you may need to allow the script the first time:
#   pwsh -NoProfile -ExecutionPolicy Bypass -File prompts/log.ps1 "..." "..."

[CmdletBinding()]
param(
    [Parameter(Position = 0)][string]$Prompt,
    [Parameter(Position = 1)][string]$Action,
    [switch]$Verify
)

$ErrorActionPreference = 'Stop'
$HistoryPath = Join-Path $PSScriptRoot 'prompt-history.md'
$Genesis = 'GENESIS'
$Utf8 = New-Object System.Text.UTF8Encoding($false)  # UTF-8, no BOM

# Collapse all runs of whitespace to single spaces and trim (matches Python's
# " ".join(text.split())).
function ConvertTo-OneLine([string]$Text) {
    if ([string]::IsNullOrEmpty($Text)) { return '' }
    return [regex]::Replace($Text.Trim(), '\s+', ' ')
}

# SHA-256 hex digest of prev\nts\nprompt\naction (LF joins, UTF-8 bytes).
function Get-Digest([string]$Prev, [string]$Ts, [string]$P, [string]$A) {
    $payload = ($Prev, $Ts, $P, $A) -join "`n"
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = $sha.ComputeHash($Utf8.GetBytes($payload))
    } finally {
        $sha.Dispose()
    }
    return (($bytes | ForEach-Object { $_.ToString('x2') }) -join '')
}

function Read-History {
    if (Test-Path -LiteralPath $HistoryPath) {
        $raw = [System.IO.File]::ReadAllText($HistoryPath, $Utf8)
        # Normalise any CRLF (e.g. written by an older log.py on Windows) to LF so
        # the LF-based regex and hashing below are correct.
        return $raw.Replace("`r`n", "`n").Replace("`r", "`n")
    }
    return ''
}

$EntryRegex = [regex]"(?m)^### (?<ts>.+)\n\*\*Prompt:\*\* (?<prompt>.*)\n\*\*Action:\*\* (?<action>.*)\n\*\*Prev:\*\* (?<prev>.+)\n\*\*Hash:\*\* (?<hash>[0-9a-f]{64})$"

function Get-LastHash([string]$Text) {
    $m = $EntryRegex.Matches($Text)
    if ($m.Count -gt 0) { return $m[$m.Count - 1].Groups['hash'].Value }
    return $Genesis
}

function Invoke-Append([string]$RawPrompt, [string]$RawAction) {
    $p = ConvertTo-OneLine $RawPrompt
    $a = ConvertTo-OneLine $RawAction
    $text = Read-History
    $prev = Get-LastHash $text
    $ts = [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ss') + 'Z'
    $digest = Get-Digest $prev $ts $p $a
    $block = "### $ts`n**Prompt:** $p`n**Action:** $a`n**Prev:** $prev`n**Hash:** $digest`n"

    if ($text -eq '') { $sep = '' }
    elseif ($text.EndsWith("`n`n")) { $sep = '' }
    elseif ($text.EndsWith("`n")) { $sep = "`n" }
    else { $sep = "`n`n" }

    [System.IO.File]::WriteAllText($HistoryPath, ($text + $sep + $block + "`n"), $Utf8)
    Write-Host ("logged {0} ({1}...)" -f $ts, $digest.Substring(0, 12))
    return 0
}

function Invoke-Verify {
    if (-not (Test-Path -LiteralPath $HistoryPath)) {
        Write-Host 'no prompt-history.md found'; return 1
    }
    $entries = $EntryRegex.Matches((Read-History))
    if ($entries.Count -eq 0) {
        Write-Host 'no hash-chained entries found'; return 1
    }
    $prev = $Genesis
    $lastTs = ''
    for ($i = 0; $i -lt $entries.Count; $i++) {
        $e = $entries[$i]
        $ts = $e.Groups['ts'].Value
        $ePrev = $e.Groups['prev'].Value
        $eHash = $e.Groups['hash'].Value
        if ($ePrev -ne $prev) {
            Write-Host ("BROKEN CHAIN at entry {0}: prev mismatch" -f ($i + 1)); return 1
        }
        $want = Get-Digest $ePrev $ts (ConvertTo-OneLine $e.Groups['prompt'].Value) (ConvertTo-OneLine $e.Groups['action'].Value)
        if ($want -ne $eHash) {
            Write-Host ("TAMPERED at entry {0}: hash mismatch" -f ($i + 1)); return 1
        }
        if ([string]::CompareOrdinal($ts, $lastTs) -lt 0) {
            Write-Host ("NON-MONOTONIC timestamp at entry {0}: {1} < {2}" -f ($i + 1), $ts, $lastTs); return 1
        }
        $lastTs = $ts
        $prev = $eHash
    }
    Write-Host ("OK: {0} entries form a valid chain" -f $entries.Count)
    return 0
}

if ($Verify) {
    exit (Invoke-Verify)
}
if ([string]::IsNullOrEmpty($Prompt) -or -not $PSBoundParameters.ContainsKey('Action')) {
    Write-Host 'Usage: log.ps1 "<verbatim prompt>" "<one-line action>"   |   log.ps1 -Verify'
    exit 2
}
exit (Invoke-Append $Prompt $Action)
