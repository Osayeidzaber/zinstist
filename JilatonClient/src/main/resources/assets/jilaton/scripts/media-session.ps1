# Jilaton media-session bridge (Windows only).
# Reads the Windows "Global System Media Transport Controls" session that desktop players such as
# Spotify publish for the volume flyout: title, artist, album, position, duration, playback state and
# cover art. It never reads any app's files, tokens or network traffic. One JSON line is printed per
# poll. Commands ("toggle", "next", "previous", "quit") arrive as a one-line command file that is read
# and deleted each poll (Windows PowerShell cannot read redirected stdin without blocking). The bridge
# exits by itself when the parent Minecraft process is gone.
param([string]$Filter = 'Spotify', [string]$ArtDir = $env:TEMP, [int]$IntervalMs = 500,
      [string]$CommandFile = '', [int]$ParentPid = 0)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
function Await($op, [Type]$type) {
    $task = $asTaskGeneric.MakeGenericMethod($type).Invoke($null, @($op))
    if (-not $task.Wait(3000)) { return $null }
    return $task.Result
}
[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime] | Out-Null
[Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime] | Out-Null
$managerType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]
$propsType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties]
$manager = Await ($managerType::RequestAsync()) $managerType
if ($null -eq $manager) { '{"error":"Windows media sessions unavailable"}'; exit 1 }

$lastKey = ''
$artPath = ''
$artSeq = 0

function Select-Session {
    $sessions = @($manager.GetSessions())
    if ($Filter -ne '') {
        foreach ($s in $sessions) { if ($s.SourceAppUserModelId -like "*$Filter*") { return $s } }
        return $null
    }
    $current = $manager.GetCurrentSession()
    if ($null -ne $current) { return $current }
    if ($sessions.Count -gt 0) { return $sessions[0] }
    return $null
}

while ($true) {
    try {
        if ($ParentPid -gt 0 -and $null -eq (Get-Process -Id $ParentPid -ErrorAction SilentlyContinue)) { exit 0 }
        $session = Select-Session
        if ($CommandFile -ne '' -and (Test-Path -LiteralPath $CommandFile)) {
            $command = (Get-Content -LiteralPath $CommandFile -Raw -ErrorAction SilentlyContinue)
            Remove-Item -LiteralPath $CommandFile -Force -ErrorAction SilentlyContinue
            if ($null -ne $command) { $command = $command.Trim() }
            if ($command -eq 'quit') { exit 0 }
            if ($null -ne $session -and $command) {
                switch ($command) {
                    'toggle' { Await ($session.TryTogglePlayPauseAsync()) ([bool]) | Out-Null }
                    'next' { Await ($session.TrySkipNextAsync()) ([bool]) | Out-Null }
                    'previous' { Await ($session.TrySkipPreviousAsync()) ([bool]) | Out-Null }
                }
            }
        }
        if ($null -eq $session) {
            [Console]::Out.WriteLine('{"active":false}')
        } else {
            $props = Await ($session.TryGetMediaPropertiesAsync()) $propsType
            $timeline = $session.GetTimelineProperties()
            $status = $session.GetPlaybackInfo().PlaybackStatus.ToString()
            $position = $timeline.Position.TotalMilliseconds
            if ($status -eq 'Playing') {
                $elapsed = ([DateTimeOffset]::Now - $timeline.LastUpdatedTime).TotalMilliseconds
                if ($elapsed -gt 0 -and $elapsed -lt 86400000) { $position += $elapsed }
            }
            $title = if ($props) { $props.Title } else { '' }
            $artist = if ($props) { $props.Artist } else { '' }
            $album = if ($props) { $props.AlbumTitle } else { '' }
            $key = "$title|$artist|$album"
            if ($key -ne $lastKey) {
                $lastKey = $key
                $artPath = ''
                if ($props -and $null -ne $props.Thumbnail) {
                    try {
                        $stream = Await ($props.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
                        if ($null -ne $stream) {
                            $artSeq++
                            $file = Join-Path $ArtDir ("jilaton-art-{0}.img" -f ($artSeq % 4))
                            $in = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($stream)
                            $out = [System.IO.File]::Create($file)
                            $in.CopyTo($out); $out.Dispose(); $in.Dispose()
                            $artPath = $file
                        }
                    } catch { $artPath = '' }
                }
            }
            $line = @{ active = $true; app = $session.SourceAppUserModelId; title = $title; artist = $artist; album = $album;
                status = $status; positionMs = [long]$position; durationMs = [long]$timeline.EndTime.TotalMilliseconds; art = $artPath }
            [Console]::Out.WriteLine(($line | ConvertTo-Json -Compress))
        }
        [Console]::Out.Flush()
    } catch {
        [Console]::Out.WriteLine('{"active":false,"error":"' + ($_.Exception.Message -replace '[\\"]', "'") + '"}')
        [Console]::Out.Flush()
    }
    Start-Sleep -Milliseconds $IntervalMs
}
