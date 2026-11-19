param(
    [string]$Path = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Path)) {
    $candidate = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot "..\dist") -Filter "Hifz-Sniper-Desktop-*-portable.exe" -File |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
    if ($null -eq $candidate) {
        throw "Aucun executable portable a verifier dans desktop\dist."
    }
    $Path = $candidate.FullName
}

$resolved = (Resolve-Path -LiteralPath $Path).Path
$signature = Get-AuthenticodeSignature -LiteralPath $resolved
if ($signature.Status -ne "Valid") {
    throw "Signature Authenticode invalide pour '$resolved' : $($signature.Status) - $($signature.StatusMessage)"
}

Write-Output "Signature Authenticode valide : $resolved"
Write-Output "Signataire : $($signature.SignerCertificate.Subject)"
Write-Output "Empreinte : $($signature.SignerCertificate.Thumbprint)"
