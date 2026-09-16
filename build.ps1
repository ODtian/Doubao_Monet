$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path

if ($env:ANDROID_SDK_ROOT) {
    $Sdk = $env:ANDROID_SDK_ROOT
} elseif ($env:ANDROID_HOME) {
    $Sdk = $env:ANDROID_HOME
} else {
    $Sdk = @(
        (Join-Path $env:LOCALAPPDATA 'Android\Sdk'),
        (Join-Path $env:USERPROFILE 'Android\Sdk')
    ) | Where-Object { Test-Path $_ } | Select-Object -First 1
}

if (-not $Sdk -or !(Test-Path $Sdk)) {
    throw 'Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME.'
}

if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
    $JavaHome = $env:JAVA_HOME
} else {
    $AdoptiumRoot = 'C:\Program Files\Eclipse Adoptium'
    if (!(Test-Path $AdoptiumRoot)) {
        throw 'JAVA_HOME is not set and Eclipse Adoptium was not found.'
    }
    $JavaHome = (Get-ChildItem $AdoptiumRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1).FullName
}
$env:JAVA_HOME = $JavaHome

$Platform = if ($env:ANDROID_PLATFORM) { $env:ANDROID_PLATFORM } else { '35' }
$BuildToolsVersion = if ($env:ANDROID_BUILD_TOOLS) { $env:ANDROID_BUILD_TOOLS } else { '35.0.0' }

$AndroidJar = Join-Path $Sdk "platforms\android-$Platform\android.jar"
$BuildTools = Join-Path $Sdk "build-tools\$BuildToolsVersion"
$Javac = Join-Path $JavaHome 'bin\javac.exe'
$Jar = Join-Path $JavaHome 'bin\jar.exe'
$Keytool = Join-Path $JavaHome 'bin\keytool.exe'
$Aapt2 = Join-Path $BuildTools 'aapt2.exe'
$D8 = Join-Path $BuildTools 'd8.bat'
$Zipalign = Join-Path $BuildTools 'zipalign.exe'
$Apksigner = Join-Path $BuildTools 'apksigner.bat'

foreach ($Required in @($AndroidJar, $Javac, $Jar, $Keytool, $Aapt2, $D8, $Zipalign, $Apksigner)) {
    if (!(Test-Path $Required)) { throw "Missing required tool: $Required" }
}

$Build = Join-Path $Root 'build'
if (Test-Path $Build) { Remove-Item -Recurse -Force $Build }
New-Item -ItemType Directory -Force "$Build\stub_classes", "$Build\classes", "$Build\dex", "$Build\out" | Out-Null

$StubFiles = @(Get-ChildItem "$Root\stubs" -Recurse -Filter '*.java' | Sort-Object FullName | ForEach-Object FullName)
& $Javac -source 8 -target 8 -encoding UTF-8 -d "$Build\stub_classes" $StubFiles
if ($LASTEXITCODE -ne 0) { throw 'stub javac failed' }

$StubJar = "$Build\xposed-stubs.jar"
& $Jar cf $StubJar -C "$Build\stub_classes" .
if ($LASTEXITCODE -ne 0) { throw 'stub jar failed' }

$SourceFiles = @(Get-ChildItem "$Root\src" -Recurse -Filter '*.java' | Sort-Object FullName | ForEach-Object FullName)
& $Javac -source 8 -target 8 -encoding UTF-8 -classpath "$AndroidJar;$StubJar" -d "$Build\classes" $SourceFiles
if ($LASTEXITCODE -ne 0) { throw 'module javac failed' }

$ModuleJar = "$Build\module.jar"
& $Jar cf $ModuleJar -C "$Build\classes" .
if ($LASTEXITCODE -ne 0) { throw 'module jar failed' }

& $D8 --release --lib $AndroidJar --classpath $StubJar --output "$Build\dex" $ModuleJar
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

$Unsigned = "$Build\unsigned.apk"
& $Aapt2 link -o $Unsigned -I $AndroidJar --manifest "$Root\AndroidManifest.xml" -A "$Root\assets" --min-sdk-version 31 --target-sdk-version 35
if ($LASTEXITCODE -ne 0) { throw 'aapt2 failed' }

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$Zip = [System.IO.Compression.ZipFile]::Open($Unsigned, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
        $Zip,
        "$Build\dex\classes.dex",
        'classes.dex',
        [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
} finally {
    $Zip.Dispose()
}

$Aligned = "$Build\aligned.apk"
& $Zipalign -f 4 $Unsigned $Aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

$StorePass = if ($env:KEYSTORE_PASSWORD) { $env:KEYSTORE_PASSWORD } else { 'android' }
$Alias = if ($env:KEY_ALIAS) { $env:KEY_ALIAS } else { 'androiddebugkey' }
$KeyPass = if ($env:KEY_PASSWORD) { $env:KEY_PASSWORD } else { $StorePass }

if ($env:KEYSTORE_PATH) {
    $Ks = $env:KEYSTORE_PATH
} else {
    $Ks = Join-Path $Root 'signing\debug.jks'
}

$KsDir = Split-Path -Parent $Ks
if (!(Test-Path $KsDir)) { New-Item -ItemType Directory -Force $KsDir | Out-Null }

if (!(Test-Path $Ks)) {
    & $Keytool -genkeypair -keystore $Ks -storepass $StorePass -keypass $KeyPass -alias $Alias -dname 'CN=Doubao Monet,O=Local,C=CN' -keyalg RSA -keysize 2048 -validity 3650 -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed' }
}

$Out = "$Build\out\Doubao_Monet.apk"
Copy-Item $Aligned $Out
& $Apksigner sign --ks $Ks --ks-key-alias $Alias --ks-pass "pass:$StorePass" --key-pass "pass:$KeyPass" $Out
if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }
& $Apksigner verify --verbose $Out
if ($LASTEXITCODE -ne 0) { throw 'verification failed' }

Get-Item $Out | Select-Object FullName, Length, LastWriteTime
Get-FileHash $Out -Algorithm SHA256
