param(
    [string]$Serial = "",
    [string]$Package = "luzzr.zou",
    [string]$OutputRoot = "scratch/performance",
    [string]$ApkPath = "",
    [switch]$SkipBuild,
    [switch]$SkipInstall
)

$ErrorActionPreference = "Stop"

function Invoke-Native {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    $processInfo = New-Object System.Diagnostics.ProcessStartInfo
    $processInfo.FileName = $FilePath
    $processInfo.UseShellExecute = $false
    $processInfo.RedirectStandardOutput = $true
    $processInfo.RedirectStandardError = $true
    $processInfo.Arguments = ($Arguments | ForEach-Object {
        if ($_ -eq $null) {
            '""'
        } elseif ($_ -match '[\s"]') {
            '"' + ($_ -replace '"', '\"') + '"'
        } else {
            $_
        }
    }) -join " "

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $processInfo
    [void]$process.Start()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    $output = @()
    if (-not [string]::IsNullOrWhiteSpace($stdout)) {
        $output += $stdout -split "`r?`n" | Where-Object { $_ -ne "" }
    }
    if (-not [string]::IsNullOrWhiteSpace($stderr)) {
        $output += $stderr -split "`r?`n" | Where-Object { $_ -ne "" }
    }

    if ($process.ExitCode -ne 0) {
        $joined = $Arguments -join " "
        throw "Command failed ($($process.ExitCode)): $FilePath $joined`n$($output -join "`n")"
    }
    return $output
}

function Get-AdbArguments {
    param([string[]]$Arguments)
    if ([string]::IsNullOrWhiteSpace($script:SelectedSerial)) {
        return $Arguments
    }
    return @("-s", $script:SelectedSerial) + $Arguments
}

function Invoke-Adb {
    param([string[]]$Arguments)
    return Invoke-Native -FilePath "adb" -Arguments (Get-AdbArguments $Arguments)
}

function Invoke-AdbBestEffort {
    param([string[]]$Arguments)
    try {
        Invoke-Adb $Arguments | Out-Null
    } catch {
        Write-Warning $_.Exception.Message
    }
}

function Select-AdbSerial {
    param([string]$RequestedSerial)
    if (-not [string]::IsNullOrWhiteSpace($RequestedSerial)) {
        return $RequestedSerial
    }

    $devices = Invoke-Native -FilePath "adb" -Arguments @("devices")
    $online = @(
        $devices |
            Where-Object { $_ -match "^(\S+)\s+device$" } |
            ForEach-Object { $Matches[1] }
    )
    if ($online.Count -eq 0) {
        throw "No adb device is online."
    }
    if ($online.Count -gt 1) {
        throw "Multiple adb devices are online. Pass -Serial. Devices: $($online -join ', ')"
    }
    return $online[0]
}

function Get-UiXml {
    $raw = Invoke-Adb @("exec-out", "uiautomator", "dump", "/dev/tty")
    $text = $raw -join "`n"
    $match = [regex]::Match($text, "(?s)<hierarchy.*</hierarchy>")
    if ($match.Success) {
        return $match.Value
    }
    return $text
}

function Wait-UiReady {
    param(
        [string]$RequiredLabel = "",
        [int]$TimeoutSeconds = 45
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastXml = ""
    while ((Get-Date) -lt $deadline) {
        $lastXml = Get-UiXml
        $hasBounds = $lastXml -match 'bounds="\[\d+,\d+\]\[\d+,\d+\]"'
        $hasRequiredLabel = [string]::IsNullOrWhiteSpace($RequiredLabel) -or
            ($lastXml -match [regex]::Escape($RequiredLabel))
        if ($hasBounds -and $hasRequiredLabel) {
            return $lastXml
        }
        Start-Sleep -Seconds 1
    }

    Set-Content -LiteralPath (Join-Path $script:RunDir "ui-ready-timeout.xml") -Value $lastXml -Encoding UTF8
    throw "Timed out waiting for app UI."
}

function Get-SafeFileName {
    param([string]$Name)
    return ($Name -replace '[\\/:*?"<>|\s]+', "_").Trim("_")
}

function New-UnicodeLabel {
    param([int[]]$CodePoints)
    return -join ($CodePoints | ForEach-Object { [char]$_ })
}

function Get-RootBounds {
    param([string]$Xml)

    $match = [regex]::Match($Xml, 'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
    if (-not $match.Success) {
        throw "Could not read root bounds from UI XML."
    }
    return [PSCustomObject]@{
        Width = [int]$match.Groups[3].Value - [int]$match.Groups[1].Value
        Height = [int]$match.Groups[4].Value - [int]$match.Groups[2].Value
    }
}

function Get-WmSize {
    $sizeLines = Invoke-Adb @("shell", "wm", "size")
    $sizeText = $sizeLines -join "`n"
    $match = [regex]::Match($sizeText, "Physical size:\s*(\d+)x(\d+)")
    if (-not $match.Success) {
        throw "Could not read wm size. Output: $sizeText"
    }
    return [PSCustomObject]@{
        Width = [int]$match.Groups[1].Value
        Height = [int]$match.Groups[2].Value
    }
}

function New-ScreenPoint {
    param(
        [string]$Label,
        [double]$XRatio,
        [double]$YRatio,
        [Parameter(Mandatory = $true)]$Screen
    )
    return [PSCustomObject]@{
        Label = $Label
        X = [int]($Screen.Width * $XRatio)
        Y = [int]($Screen.Height * $YRatio)
        DumpPath = ""
    }
}

function Get-UiLabelCenter {
    param(
        [string]$Label,
        [int]$MinY = 0,
        [string]$DumpName = ""
    )

    $escaped = [regex]::Escape($Label)
    $pattern = '<node\b(?=[^>]*(?:text="' + $escaped + '"|content-desc="' + $escaped + '"))[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'

    $safe = if ([string]::IsNullOrWhiteSpace($DumpName)) { Get-SafeFileName $Label } else { Get-SafeFileName $DumpName }
    $dumpPath = Join-Path $script:RunDir ("ui-resolve-{0}.xml" -f $safe)

    for ($attempt = 1; $attempt -le 10; $attempt += 1) {
        $xml = Get-UiXml
        Set-Content -LiteralPath $dumpPath -Value $xml -Encoding UTF8
        $matches = [regex]::Matches($xml, $pattern)
        foreach ($candidate in $matches) {
            $x1 = [int]$candidate.Groups[1].Value
            $y1 = [int]$candidate.Groups[2].Value
            $x2 = [int]$candidate.Groups[3].Value
            $y2 = [int]$candidate.Groups[4].Value
            if ($y1 -ge $MinY) {
                return [PSCustomObject]@{
                    Label = $Label
                    X = [int](($x1 + $x2) / 2)
                    Y = [int](($y1 + $y2) / 2)
                    DumpPath = $dumpPath
                }
            }
        }
        Start-Sleep -Milliseconds 500
    }

    throw "Could not find UI label '$Label' at MinY=$MinY. UI dump: $dumpPath"
}

function Invoke-AdbTapPoint {
    param([Parameter(Mandatory = $true)]$Point)
    Invoke-Adb @("shell", "input", "tap", "$($Point.X)", "$($Point.Y)") | Out-Null
    Start-Sleep -Milliseconds 420
}

function Invoke-AdbSwipe {
    param(
        [int]$StartX,
        [int]$EndX,
        [int]$Y,
        [int]$DurationMillis = 650
    )
    Invoke-Adb @("shell", "input", "swipe", "$StartX", "$Y", "$EndX", "$Y", "$DurationMillis") | Out-Null
    Start-Sleep -Milliseconds 520
}

function Tap-UiLabel {
    param([string]$Label)

    $script:StepIndex += 1
    $escaped = [regex]::Escape($Label)
    $pattern = '<node\b(?=[^>]*(?:text="' + $escaped + '"|content-desc="' + $escaped + '"))[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'

    $safe = Get-SafeFileName $Label
    $dumpPath = Join-Path $script:RunDir ("ui-{0:D2}-{1}.xml" -f $script:StepIndex, $safe)
    $match = $null
    $xml = ""
    for ($attempt = 1; $attempt -le 10; $attempt += 1) {
        $xml = Get-UiXml
        Set-Content -LiteralPath $dumpPath -Value $xml -Encoding UTF8
        $match = [regex]::Match($xml, $pattern)
        if ($match.Success) {
            break
        }
        Start-Sleep -Milliseconds 500
    }

    if (-not $match.Success) {
        throw "Could not find UI label '$Label'. UI dump: $dumpPath"
    }

    $x1 = [int]$match.Groups[1].Value
    $y1 = [int]$match.Groups[2].Value
    $x2 = [int]$match.Groups[3].Value
    $y2 = [int]$match.Groups[4].Value
    $x = [int](($x1 + $x2) / 2)
    $y = [int](($y1 + $y2) / 2)

    Invoke-Adb @("shell", "input", "tap", "$x", "$y") | Out-Null
    Start-Sleep -Milliseconds 750
}

function Get-GfxinfoSummary {
    param([string[]]$Lines)

    $summary = [ordered]@{
        package = $Package
        serial = $script:SelectedSerial
        runId = $script:RunId
        totalFrames = $null
        jankyFrames = $null
        jankyPercent = $null
        percentile50Ms = $null
        percentile90Ms = $null
        percentile95Ms = $null
        percentile99Ms = $null
        missedVsync = $null
        slowUiThread = $null
        slowBitmapUploads = $null
        slowDrawCommands = $null
    }

    foreach ($line in $Lines) {
        if ($line -match "Total frames rendered:\s+(\d+)") { $summary.totalFrames = [int]$Matches[1] }
        if ($line -match "Janky frames:\s+(\d+)\s+\(([\d.]+)%\)") {
            $summary.jankyFrames = [int]$Matches[1]
            $summary.jankyPercent = [double]$Matches[2]
        }
        if ($line -match "50th percentile:\s+(\d+)ms") { $summary.percentile50Ms = [int]$Matches[1] }
        if ($line -match "90th percentile:\s+(\d+)ms") { $summary.percentile90Ms = [int]$Matches[1] }
        if ($line -match "95th percentile:\s+(\d+)ms") { $summary.percentile95Ms = [int]$Matches[1] }
        if ($line -match "99th percentile:\s+(\d+)ms") { $summary.percentile99Ms = [int]$Matches[1] }
        if ($line -match "Number Missed Vsync:\s+(\d+)") { $summary.missedVsync = [int]$Matches[1] }
        if ($line -match "Number Slow UI thread:\s+(\d+)") { $summary.slowUiThread = [int]$Matches[1] }
        if ($line -match "Number Slow bitmap uploads:\s+(\d+)") { $summary.slowBitmapUploads = [int]$Matches[1] }
        if ($line -match "Number Slow issue draw commands:\s+(\d+)") { $summary.slowDrawCommands = [int]$Matches[1] }
    }

    return $summary
}

function Save-Diagnostics {
    param([string]$Reason)

    if ([string]::IsNullOrWhiteSpace($script:RunDir)) {
        return
    }

    Set-Content -LiteralPath (Join-Path $script:RunDir "failure.txt") -Value $Reason -Encoding UTF8

    try {
        Invoke-Adb @("logcat", "-d", "-t", "600") |
            Set-Content -LiteralPath (Join-Path $script:RunDir "failure-logcat-tail.txt") -Encoding UTF8
    } catch {
        Set-Content -LiteralPath (Join-Path $script:RunDir "failure-logcat-error.txt") -Value $_.Exception.Message -Encoding UTF8
    }

    try {
        Get-UiXml |
            Set-Content -LiteralPath (Join-Path $script:RunDir "failure-ui.xml") -Encoding UTF8
    } catch {
        Set-Content -LiteralPath (Join-Path $script:RunDir "failure-ui-error.txt") -Value $_.Exception.Message -Encoding UTF8
    }

    try {
        Invoke-Adb @("shell", "dumpsys", "activity", "anr") |
            Set-Content -LiteralPath (Join-Path $script:RunDir "failure-anr.txt") -Encoding UTF8
    } catch {
        Set-Content -LiteralPath (Join-Path $script:RunDir "failure-anr-error.txt") -Value $_.Exception.Message -Encoding UTF8
    }
}

$script:SelectedSerial = Select-AdbSerial $Serial
$script:RunId = Get-Date -Format "yyyyMMdd-HHmmss"
$script:RunDir = Join-Path $OutputRoot $script:RunId
$script:StepIndex = 0
New-Item -ItemType Directory -Path $script:RunDir -Force | Out-Null

try {
if (-not $SkipBuild) {
    Invoke-Native -FilePath ".\gradlew.bat" -Arguments @(":app:assembleDebug", "--console=plain") |
        Set-Content -LiteralPath (Join-Path $script:RunDir "build-debug.log") -Encoding UTF8
}

if (-not $SkipInstall) {
    if ([string]::IsNullOrWhiteSpace($ApkPath)) {
        $abi = ((Invoke-Adb @("shell", "getprop", "ro.product.cpu.abi")) | Select-Object -First 1).Trim()
        $apkName = if ($abi -match "x86_64") { "app-x86_64-debug.apk" } else { "app-arm64-v8a-debug.apk" }
        $ApkPath = Join-Path "app/build/outputs/apk/debug" $apkName
    }

    if (-not (Test-Path -LiteralPath $ApkPath)) {
        throw "Debug APK not found: $ApkPath"
    }
    Invoke-Adb @("install", "-r", "-d", $ApkPath) |
        Set-Content -LiteralPath (Join-Path $script:RunDir "install.log") -Encoding UTF8
    Start-Sleep -Seconds 8
}

Invoke-AdbBestEffort @("logcat", "-c")
Invoke-AdbBestEffort @("shell", "pm", "grant", $Package, "android.permission.POST_NOTIFICATIONS")
Invoke-AdbBestEffort @("shell", "appops", "set", $Package, "SCHEDULE_EXACT_ALARM", "allow")
Invoke-AdbBestEffort @("shell", "dumpsys", "deviceidle", "whitelist", "+$Package")
Invoke-Adb @("shell", "am", "force-stop", $Package) | Out-Null
Invoke-AdbBestEffort @("shell", "am", "force-stop", "com.android.settings")
$resolvedActivity = Invoke-Adb @("shell", "cmd", "package", "resolve-activity", "--brief", $Package)
$component = $resolvedActivity |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ -match "/" } |
    Select-Object -Last 1
if ([string]::IsNullOrWhiteSpace($component)) {
    throw "Could not resolve launcher activity for $Package. Output: $($resolvedActivity -join ' ')"
}
Invoke-Adb @("shell", "am", "start", "-n", $component) |
    Set-Content -LiteralPath (Join-Path $script:RunDir "launch.log") -Encoding UTF8

$labelTasks = New-UnicodeLabel @(0x5F85, 0x529E)
$labelHabits = New-UnicodeLabel @(0x4E60, 0x60EF)
$labelNotes = New-UnicodeLabel @(0x7B14, 0x8BB0)
$labelToday = New-UnicodeLabel @(0x4ECA, 0x65E5)
$labelQuickCreate = New-UnicodeLabel @(0x5C55, 0x5F00, 0x5FEB, 0x901F, 0x521B, 0x5EFA)

$coordinateMode = "ui-tree"
try {
    $readyXml = Wait-UiReady -RequiredLabel $labelToday -TimeoutSeconds 90
    Set-Content -LiteralPath (Join-Path $script:RunDir "ui-ready.xml") -Value $readyXml -Encoding UTF8
    $screen = Get-RootBounds $readyXml
    $navMinY = [int]($screen.Height * 0.78)
    $navToday = Get-UiLabelCenter -Label $labelToday -MinY $navMinY -DumpName "nav-today"
    $navTasks = Get-UiLabelCenter -Label $labelTasks -MinY $navMinY -DumpName "nav-tasks"
    $navHabits = Get-UiLabelCenter -Label $labelHabits -MinY $navMinY -DumpName "nav-habits"
    $navNotes = Get-UiLabelCenter -Label $labelNotes -MinY $navMinY -DumpName "nav-notes"
    $quickCreate = Get-UiLabelCenter -Label $labelQuickCreate -MinY $navMinY -DumpName "quick-create-fab"
} catch {
    $coordinateMode = "screen-fallback"
    Set-Content -LiteralPath (Join-Path $script:RunDir "ui-ready-warning.txt") -Value $_.Exception.Message -Encoding UTF8
    $screen = Get-WmSize
    $navToday = New-ScreenPoint -Label $labelToday -XRatio 0.135 -YRatio 0.94 -Screen $screen
    $navTasks = New-ScreenPoint -Label $labelTasks -XRatio 0.318 -YRatio 0.94 -Screen $screen
    $navHabits = New-ScreenPoint -Label $labelHabits -XRatio 0.682 -YRatio 0.94 -Screen $screen
    $navNotes = New-ScreenPoint -Label $labelNotes -XRatio 0.865 -YRatio 0.94 -Screen $screen
    $quickCreate = New-ScreenPoint -Label $labelQuickCreate -XRatio 0.5 -YRatio 0.94 -Screen $screen
}
$swipeY = [int]($screen.Height * 0.55)
$swipeStartX = [int]($screen.Width * 0.82)
$swipeEndX = [int]($screen.Width * 0.18)

Invoke-Adb @("shell", "dumpsys", "gfxinfo", $Package, "reset") | Out-Null
Start-Sleep -Milliseconds 500

Invoke-AdbTapPoint $navToday
Invoke-AdbTapPoint $quickCreate
Start-Sleep -Milliseconds 700
Invoke-AdbTapPoint $quickCreate
Start-Sleep -Milliseconds 700
Invoke-AdbTapPoint $navTasks
Invoke-AdbTapPoint $navHabits
Invoke-AdbTapPoint $navNotes
Invoke-AdbTapPoint $navToday
Invoke-AdbTapPoint $navTasks
Invoke-AdbSwipe -StartX $swipeStartX -EndX $swipeEndX -Y $swipeY -DurationMillis 720
Invoke-AdbSwipe -StartX $swipeEndX -EndX $swipeStartX -Y $swipeY -DurationMillis 720
Invoke-AdbTapPoint $navToday
Invoke-AdbTapPoint $quickCreate
Start-Sleep -Milliseconds 700
Invoke-AdbTapPoint $quickCreate
Start-Sleep -Seconds 1

$gfxinfo = Invoke-Adb @("shell", "dumpsys", "gfxinfo", $Package)
$framestats = Invoke-Adb @("shell", "dumpsys", "gfxinfo", $Package, "framestats")
$logcat = Invoke-Adb @("logcat", "-d", "-t", "300")
$finalUi = Get-UiXml

$gfxinfoPath = Join-Path $script:RunDir "gfxinfo.txt"
$framestatsPath = Join-Path $script:RunDir "gfxinfo-framestats.txt"
$logcatPath = Join-Path $script:RunDir "logcat-tail.txt"
$summaryJsonPath = Join-Path $script:RunDir "summary.json"
$summaryMdPath = Join-Path $script:RunDir "summary.md"

Set-Content -LiteralPath $gfxinfoPath -Value $gfxinfo -Encoding UTF8
Set-Content -LiteralPath $framestatsPath -Value $framestats -Encoding UTF8
Set-Content -LiteralPath $logcatPath -Value $logcat -Encoding UTF8
Set-Content -LiteralPath (Join-Path $script:RunDir "ui-final.xml") -Value $finalUi -Encoding UTF8

$summary = Get-GfxinfoSummary $gfxinfo
$summary["coordinateMode"] = $coordinateMode
$summary["sampleStatus"] = if ($summary.totalFrames -eq $null) {
    "missing"
} elseif ($summary.totalFrames -lt 60) {
    "too_small"
} else {
    "ok"
}
$summary | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $summaryJsonPath -Encoding UTF8

$markdown = @(
    "# Android gfxinfo smoke summary",
    "",
    "- Package: $Package",
    "- Serial: $script:SelectedSerial",
    "- Run: $script:RunId",
    "- Coordinate mode: $($summary["coordinateMode"])",
    "- Sample status: $($summary["sampleStatus"])",
    "- Total frames: $($summary.totalFrames)",
    "- Janky frames: $($summary.jankyFrames)",
    "- Janky percent: $($summary.jankyPercent)",
    "- 50th percentile: $($summary.percentile50Ms) ms",
    "- 90th percentile: $($summary.percentile90Ms) ms",
    "- 95th percentile: $($summary.percentile95Ms) ms",
    "- 99th percentile: $($summary.percentile99Ms) ms",
    "- Missed vsync: $($summary.missedVsync)",
    "- Slow UI thread: $($summary.slowUiThread)",
    "- Slow bitmap uploads: $($summary.slowBitmapUploads)",
    "- Slow draw commands: $($summary.slowDrawCommands)",
    "",
    "Artifacts:",
    "",
    "- $gfxinfoPath",
    "- $framestatsPath",
    "- $logcatPath"
)
Set-Content -LiteralPath $summaryMdPath -Value $markdown -Encoding UTF8

Write-Host "Performance smoke complete."
Write-Host "Summary: $summaryMdPath"
} catch {
    Save-Diagnostics -Reason $_.Exception.Message
    throw
}
