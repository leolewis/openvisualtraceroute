; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "Open Visual Traceroute"
#define MyAppVersion "1.8.0"
#define MyAppPublisher "Leo Lewis"
#define MyAppURL "https://sourceforge.net/projects/openvisualtrace/"
#define MyAppExeName "ovtr.exe"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{74E0B6B8-9214-46A4-A0D7-6373A659A643}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={pf}\{#MyAppName}
DefaultGroupName={#MyAppName}
LicenseFile=D:\workspace\git\openvisualtraceroute\org.leo.traceroute\product\License.txt
OutputDir=D:\workspace\git\openvisualtraceroute\org.leo.traceroute\released
OutputBaseFilename=OpenVisualTraceroute1.8.0
SetupIconFile=D:\workspace\git\openvisualtraceroute\org.leo.traceroute\product\resources\icon.ico
Compression=lzma
SolidCompression=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "quicklaunchicon"; Description: "{cm:CreateQuickLaunchIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked; OnlyBelowVersion: 0,6.1

[Files]
Source: "D:\workspace\git\openvisualtraceroute\org.leo.traceroute\product\ovtr.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "D:\workspace\git\openvisualtraceroute\org.leo.traceroute\product\org.leo.traceroute.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "D:\workspace\git\openvisualtraceroute\org.leo.traceroute\build\win\WinPcap_4_1_3.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "D:\workspace\git\openvisualtraceroute\org.leo.traceroute\product\lib\*"; DestDir: "{app}\lib"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "D:\workspace\git\openvisualtraceroute\org.leo.traceroute\product\native\win\*"; DestDir: "{app}\native\win"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "D:\workspace\git\openvisualtraceroute\org.leo.traceroute\product\resources\*"; DestDir: "{app}\resources"; Flags: ignoreversion recursesubdirs createallsubdirs
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:ProgramOnTheWeb,{#MyAppName}}"; Filename: "{#MyAppURL}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon
Name: "{userappdata}\Microsoft\Internet Explorer\Quick Launch\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: quicklaunchicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Code]
procedure ExitProcess(exitCode:integer);
  external 'ExitProcess@kernel32.dll stdcall';

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  ResultCode:   Integer;
begin
   ExtractTemporaryFile('Win10Pcap-v10.2-5002.msi');
   if Exec(ExpandConstant('MsiExec.exe /i {tmp}/\Win10Pcap-v10.2-5002.msi ALLUSERS=1 /qn'), '/S', '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
   begin
       if ResultCode = 1 then
       begin
         msgbox('Failed to install Win10Pcap, the installer will close.', mbInformation, MB_OK);
         MainForm.Close;
         ExitProcess(0);
       end
   end
   else begin
     msgbox('Failed to install Win10Pcap, the installer will close.', mbInformation, MB_OK);
     MainForm.Close;
     ExitProcess(0);
   end;
end;