# Android Screenshot Tool - PowerShell Version
# Professional UI with enhanced formatting

function Show-Header {
    Write-Host ""
    Write-Host "    +==========================================================+" -ForegroundColor Cyan
    Write-Host "    |                                                          |" -ForegroundColor Cyan
    Write-Host "    |        [ Android Screenshot Tool ]                       |" -ForegroundColor Cyan
    Write-Host "    |                                                          |" -ForegroundColor Cyan
    Write-Host "    +==========================================================+" -ForegroundColor Cyan
    Write-Host ""
}

function Show-DeviceInfo {
    param(
        [string]$Suffix,
        [string]$Filename
    )
    
    Write-Host "    +----------------------------------------------------------+"
    Write-Host "    |  Suffix : $Suffix" -ForegroundColor Green
    Write-Host "    |  Filename: $Filename" -ForegroundColor Green
    Write-Host "    +----------------------------------------------------------+"
    Write-Host ""
}

function Show-DeviceSelection {
    param(
        [string[]]$Devices
    )
    
    Write-Host ""
    Write-Host "    +----------------------------------------------------------+"
    Write-Host "    |  Multiple devices detected. Please select:               |" -ForegroundColor Yellow
    Write-Host "    +----------------------------------------------------------+"
    
    for ($i = 0; $i -lt $Devices.Count; $i++) {
        Write-Host "    |  [$($i + 1)] $($Devices[$i])" -ForegroundColor White
    }
    
    Write-Host "    +----------------------------------------------------------+"
    Write-Host ""
}

function Show-Error {
    param(
        [string]$Message
    )
    
    Write-Host ""
    Write-Host "    +==========================================================+" -ForegroundColor Red
    Write-Host "    |  [ ERROR ] $Message" -ForegroundColor Red
    Write-Host "    +==========================================================+" -ForegroundColor Red
}

function Show-Success {
    param(
        [string]$Location
    )
    
    Write-Host ""
    Write-Host "    +==========================================================+" -ForegroundColor Green
    Write-Host "    |  [ SUCCESS ] Screenshot saved!                           |" -ForegroundColor Green
    Write-Host "    |                                                          |" -ForegroundColor Green
    Write-Host "    |  Location: $Location" -ForegroundColor Green
    Write-Host "    +==========================================================+" -ForegroundColor Green
    Write-Host ""
}

:start
while ($true) {
    Clear-Host
    Show-Header
    
    # Prompt for filename suffix
    $suffix = Read-Host "    Enter filename suffix (default: Home)"
    if ([string]::IsNullOrWhiteSpace($suffix)) {
        $suffix = "Home"
    }
    
    $fullname = "Screenshot-$suffix.png"
    
    Show-DeviceInfo -Suffix $suffix -Filename $fullname
    
    # Create screenshots directory
    if (-not (Test-Path "screenshots")) {
        New-Item -ItemType Directory -Path "screenshots" | Out-Null
    }
    
    # Detect connected devices
    Write-Host "    [ Detecting connected devices... ]" -ForegroundColor Cyan
    
    $devicesOutput = & adb devices 2>&1
    $devices = @()
    
    # Parse device list
    $lines = $devicesOutput -split "`n"
    for ($i = 1; $i -lt $lines.Count; $i++) {
        $line = $lines[$i].Trim()
        if (-not [string]::IsNullOrWhiteSpace($line) -and $line -notmatch "^List") {
            $parts = $line -split "\s+"
            if ($parts.Count -ge 1) {
                $devices += $parts[0]
            }
        }
    }
    
    # Check if any device connected
    if ($devices.Count -eq 0) {
        Show-Error -Message "No devices detected!"
        Write-Host "       Please ensure your device is connected and USB debugging is enabled." -ForegroundColor Yellow
        Write-Host ""
        Read-Host "    Press Enter to exit"
        exit 1
    }
    
    # Select device
    $selectedDevice = $null
    if ($devices.Count -eq 1) {
        Write-Host "    [ OK ] Found 1 device" -ForegroundColor Green
        $selectedDevice = $devices[0]
    } else {
        Show-DeviceSelection -Devices $devices
        $choice = Read-Host "    Enter device number (1-$($devices.Count))"
        
        # Validate input
        $validChoice = $false
        $choiceNum = 0
        if ([int]::TryParse($choice, [ref]$choiceNum)) {
            if ($choiceNum -ge 1 -and $choiceNum -le $devices.Count) {
                $selectedDevice = $devices[$choiceNum - 1]
                $validChoice = $true
            }
        }
        
        if (-not $validChoice) {
            Show-Error -Message "Invalid selection!"
            Read-Host "    Press Enter to exit"
            exit 1
        }
    }
    
    Write-Host ""
    Write-Host "    [ OK ] Selected device: $selectedDevice" -ForegroundColor Green
    Write-Host ""
    
    # Capture screenshot
    Write-Host "    [ Capturing screen... ]" -ForegroundColor Cyan
    $screencapResult = & adb -s $selectedDevice shell screencap -p /sdcard/screenshot_temp.png 2>&1
    if ($LASTEXITCODE -ne 0) {
        Show-Error -Message "Failed to capture screenshot!"
        Write-Host "       Please check device connection and permissions." -ForegroundColor Yellow
        Read-Host "    Press Enter to exit"
        exit 1
    }
    
    # Pull screenshot
    $pullResult = & adb -s $selectedDevice pull /sdcard/screenshot_temp.png "screenshots\$fullname" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Show-Error -Message "Failed to save screenshot!"
        Read-Host "    Press Enter to exit"
        exit 1
    }
    
    Show-Success -Location "screenshots\$fullname"
    
    # Ask to continue
    $continue = Read-Host "    Continue capturing? (Y/N, default: N)"
    if ($continue -ne "Y" -and $continue -ne "y") {
        Write-Host ""
        Write-Host "    Thank you for using Android Screenshot Tool. Goodbye!" -ForegroundColor Cyan
        Write-Host ""
        Read-Host "    Press Enter to exit"
        break
    }
    
    Write-Host ""
    Write-Host "    ----------------------------------------------------------"
    Write-Host "    Restarting..." -ForegroundColor Cyan
    Write-Host "    ----------------------------------------------------------"
    Write-Host ""
    Start-Sleep -Seconds 1
}
