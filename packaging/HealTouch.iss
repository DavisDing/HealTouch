; Inno Setup 6 script. The CI invokes this once per architecture with /DArch and /DRuntimeDir.
#ifndef AppVersion
  #define AppVersion "0.0.0"
#endif
#ifndef Arch
  #define Arch "x64"
#endif
#ifndef RuntimeDir
  #define RuntimeDir "runtime\\x64"
#endif

[Setup]
AppId={{4A8F99E7-1135-46BC-A45F-B9E1C25BE00C}
AppName=HealTouch
AppVersion={#AppVersion}
AppPublisher=HealTouch
DefaultDirName={autopf}\HealTouch
DefaultGroupName=HealTouch
DisableProgramGroupPage=yes
OutputDir=..\target\installer
OutputBaseFilename=HealTouch-{#AppVersion}-{#Arch}-Setup
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed={#Arch}
ArchitecturesInstallIn64BitMode=x64
Uninstallable=yes
UninstallDisplayName=HealTouch 推拿门诊管理系统

[Files]
Source: "..\target\healtouch-*-shaded.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\target\HealTouch.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\{#RuntimeDir}\*"; DestDir: "{app}\runtime"; Flags: recursesubdirs createallsubdirs ignoreversion

[Icons]
Name: "{group}\HealTouch"; Filename: "{app}\HealTouch.exe"
Name: "{autodesktop}\HealTouch"; Filename: "{app}\HealTouch.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\HealTouch.exe"; Description: "启动 HealTouch"; Flags: nowait postinstall skipifsilent

[Tasks]
Name: "desktopicon"; Description: "创建桌面快捷方式"; Flags: unchecked

[UninstallDelete]
; Explicitly do not delete the per-user data directory. Business data survives uninstall.
Type: filesandordirs; Name: "{app}\runtime"
