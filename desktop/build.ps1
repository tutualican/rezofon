$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$csc = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
$out = Join-Path $root "dist"
New-Item -ItemType Directory -Force -Path $out | Out-Null

# Icon: generate a 256px ICO from the launcher vector look if missing.
$ico = Join-Path $root "assets\rezofon.ico"
if (-not (Test-Path $ico)) {
    New-Item -ItemType Directory -Force -Path (Split-Path $ico) | Out-Null
    Add-Type -AssemblyName System.Drawing
    $sizes = @(16, 32, 48, 64, 128, 256)
    $images = @()
    foreach ($s in $sizes) {
        $bmp = New-Object System.Drawing.Bitmap($s, $s)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $g.Clear([System.Drawing.Color]::FromArgb(8,9,11))
        $bars = @(
            @{ x = 0.22; h = 0.30; c = @(200,255,61) },
            @{ x = 0.38; h = 0.52; c = @(200,255,61) },
            @{ x = 0.54; h = 0.70; c = @(255,176,58) },
            @{ x = 0.70; h = 0.40; c = @(200,255,61) }
        )
        foreach ($b in $bars) {
            $bw = [math]::Max(2, $s * 0.10)
            $bh = $s * $b.h
            $bx = $s * $b.x
            $by = ($s - $bh) / 2
            $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb($b.c[0], $b.c[1], $b.c[2]))
            $g.FillRectangle($brush, $bx, $by, $bw, $bh)
            $brush.Dispose()
        }
        $g.Dispose()
        $images += $bmp
    }

    $fs = [System.IO.File]::Create($ico)
    $bw = New-Object System.IO.BinaryWriter($fs)
    $bw.Write([UInt16]0); $bw.Write([UInt16]1); $bw.Write([UInt16]$images.Count)
    $offset = 6 + 16 * $images.Count
    $pngs = @()
    foreach ($img in $images) {
        $ms = New-Object System.IO.MemoryStream
        $img.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
        $pngs += ,$ms.ToArray()
        $ms.Dispose()
    }
    for ($i = 0; $i -lt $images.Count; $i++) {
        $img = $images[$i]
        $dim = if ($img.Width -ge 256) { 0 } else { $img.Width }
        $bw.Write([Byte]$dim); $bw.Write([Byte]$dim)
        $bw.Write([Byte]0); $bw.Write([Byte]0)
        $bw.Write([UInt16]1); $bw.Write([UInt16]32)
        $bw.Write([UInt32]$pngs[$i].Length)
        $bw.Write([UInt32]$offset)
        $offset += $pngs[$i].Length
    }
    foreach ($png in $pngs) { $bw.Write($png) }
    $bw.Close(); $fs.Close()
    foreach ($img in $images) { $img.Dispose() }
}

$sources = Get-ChildItem (Join-Path $root "src") -Filter *.cs | ForEach-Object { $_.FullName }
$exePath = Join-Path $out "Rezofon.exe"

& $csc /nologo /target:winexe /platform:anycpu /optimize+ "/out:$exePath" `
    "/win32icon:$ico" `
    /reference:System.dll `
    /reference:System.Drawing.dll `
    /reference:System.Windows.Forms.dll `
    /reference:System.Management.dll `
    $sources

if ($LASTEXITCODE -ne 0) { throw "Derleme başarısız." }

Copy-Item (Join-Path $root "src\..\fonts") -Destination $out -Recurse -Force -ErrorAction SilentlyContinue
if (Test-Path (Join-Path $root "fonts")) {
    Copy-Item (Join-Path $root "fonts") -Destination $out -Recurse -Force
}
New-Item -ItemType Directory -Force -Path (Join-Path $out "fonts") | Out-Null
Copy-Item (Join-Path $root "..\android\app\src\main\res\font\*.ttf") -Destination (Join-Path $out "fonts") -Force
Write-Host "OK -> $out\Rezofon.exe"
