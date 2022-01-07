~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
~~~~~~~ Open Visual Trace Route ~~~~~~~ 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Version 2.0.0
Starting version 2.0.0, I decided to deprecate the sniffer feature and refocus the tool on its original purpose: a visual traceroute.
Reasoning around the decision were ultimately time to maintain the application while the various libraries it depends on were being upgraded to non compatible APIs or plainly deprecated.
This decision will also make the application runnable without admin privilege and external software required to be installed (except Java), which was one of the pain point in the installation of versions 1.7 and prior.

Full code source is as always available on Github https://github.com/leolewis/openvisualtraceroute and anyone is free to branch the version.

I could successfully make this version on multiple systems, but my tests do not cover everything, so if you find a bug, a typo in some label feel free to post a bug report and/or fix
- on Sourceforge http://sourceforge.net/projects/openvisualtrace/
- or by email at leo.lewis.software@gmail.com
- on Facebook https://www.facebook.com/openvisualtraceroute/
Please specify your OS, logs (under $USER_HOME/ovtr/ovtr.log), and the steps to reproduce the issue on the bug report.
I cannot guaranty to fix issues, (this software is free and open source) but I can promise to take a look at them when I have a moment.

Enjoy and share,
Leo Lewis

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Installation and execution procedure :

See http://visualtraceroute.net/installation

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
OS/Arch notice :
Tested on the following systems :
- Windows 11 64 bits
- Ubuntu 20.04 64 bits
- OpenSUSE Leap 15.3 64 bits
- MacOSX
- FreeBSD 13.0 Xfce 

With Java versions
- 11~17

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Limitations, Known issues :
- Systems that don't have a graphic card/drivers that supports required OpenGL features will not be able to use the 3D map

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Wish list : 
- traceroute server mode
- display active connections
- export and import traceroute data
- clean up WW cache data when uninstalling the app (worldwind26.arc.nasa.gov)

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Release Note
2.0.0
This release
- Library: Upgrade to worldwind 2.2.0
- Library: upgrade to Java 11+
- Feature: UI Dark mode
- Deprecate embedded trace route mode and sniffer mode

Release Note 
1.7.1
- Library: Upgrade to geoip v2

1.7.0
- Feature: DNS loc records feature
- Feature: Use system proxy when available, allow to specify user and password
- Feature: Anti-aliasing fonts
- Library: Upgrade world wind version
- Library: Upgrade required Java version to 1.8
- Fix    : Fix hanging network interface detection
- Fix    : Increase geoip db download timeout to avoid issue on first startup

1.6.5
- Fix Fatal error IOException: Failed to download geoip database
- UI : Fix status bar tooltip
- Traceroute : Fix consecutive unknown locations diplayed in the (0, 0)

1.6.4
- Sniffer : Add feature to select a list and or range of ports.
- Sniffer : Add feature to stop the capture after a given amount of time
- Sniffer : Fix filter on port and packet length greater than 999 issue
- Traceroute : Fix traceroute timeout greater than 999 issue
- UI : German translation (thanks to Centauri39) 
- UI : Japanese translation
- UI : Save table column width in preferences
- UI : Save window location
- UI : Add font chooser in the preference window
- UI : Fix sorting on row number
- Map : Choose line thickness in preference window
- Map : display unknown ips at the previous known location and no longer at (0, 0) 
- Map : Fix 2D map not repainting in sniffer mode  
- Windows : change whois provider
- Linux Mint : fix launcher
- Linux : add rpm packages

1.6.3
- Add max number of hops for traceroute
- Add hostname filter for sniffer
- Improve embedded traceroute accuracy
- Add log windows to show application logs
- Add settings to hide non selected labels from the map
- Dynamic config to be able to update external content (ip address provider, geoip db location) without having to release a new version of the app
- Expire the geo ip db every month and download the latest version to avoid having the db getting outdated when the application is not updated.
- Add startup information to the splashscreen, refactor the startup flow
- Fix a bunch of UI bugs
- Fix a bunch of labels.

1.6.2
- Add whois feature in both traceroute and sniffer
- Fix duplicated route points during the traceroute
- Fix command+Q failed to close the application on MacOSX
- Fix bug on sorting on tables
- Update Geoip database
- Update public ip address provider

1.6.1
- Improve Maps visualization by displaying lines with color and elevation corresponding to latency for traceroute and number of packets for sniffer
- Add export route and network capture to a file in CSV format
- Add "take a screenshot" feature
- Persist traceroute history and provide it as autocomplete suggestions
- Add a preference to hide the current public IP address from the UI (useful when taking snapshots of the application but don't want to show your IP address)
- For the 3D map, allow user to specify the animation speed and the replay speed for traceroute
- Rotating selection for overlapping points on the maps
- Update geo ip database
- Clear geo ip cached database when installing a new version to make sure the data is updated from the new installer
- Add a preference to hide the splashscreen
- Fix couple of issues.

1.6.0
- Add Traceroute support for MacOS X
- Improve traceroute mechanism to use the OS traceroute instead of the embedded traceroute (that will be used by default in case the embedded network library couldn't be initialized for whatever reason)
- Add installer for windows, dmg for mac and deb for linux packagings for easier installation and uninstallation
- Improve error handling, disable 3D map for system that don't support required OpenGL features.
- Fix error when using the timeout spinner
- Fix error when first starting the application if it is located in a directory where the user does not have right access
- Improve toolbar layout for low resolution screens
- Add a Settings dialog
- Add support for French language

1.5.1
- Fix JVM crash when capturing packets for a long time
- Check version at startup and propose the user to go to the download page
- Improve the packet details view
- Allow to choose network interface for sniffer
- Update ip->geo location database. Ship it compressed with the application to reduce package size

1.5.0
- Add packet sniffer mode
- Add timeout for traceroute
- 2D map component using Openmap
- Upgrade to WorldWind 2.0
- Refactoring of code
- Use proper logger inside the code

1.4.0
- Allow to choose the network interface to use for trace route
- Optimize startup of traceroute
- Dramatically improve DNS lookup performance
- Add route length and distance between route points
- Display Country flag in the table and in the map
- Fix the overlay of labels on the map when several route points have the same coordinates
- Redesign gantt chart view to make it easier to see points
- Implement selection synchronization map<->table<->gantt chart
- Change window layout
- Change look and feel for windows os
- Refactor code
- Fix an issue when replaying a route with only one point
- Fix Linux start script
- Fix bug when saving the size of the window

1.3.0 Beta
- Upgrade to Worldwind 1.5.0, change layer panel, add graticule
- Add Gantt view of the route
- Add Replay function of the traceroute
- Implement bi-directional selection synchronization map<->table
- Focus on the last point of the route during tracing
- Update visual of the Route (3d shape)
- Update labels of the points (cities) of the route
- Save application window size and split location when exiting the application
- Highlight current route point during tracing (both map and table)
- Fix an error when clearing the selection of the table
- Fix an error that crashed the application when starting from a directory that has space characters inside its path
- Fix a memory leak when tracing the route

1.2.1 Beta
- Remove the Applet version (JNLP files than contains JOGL native libs are no longer maintained by Oracle/JOGL)
- Refactor installer, remove the local dir, simplify native library management
- Add executable files for Windows and Linux
- Better error management

1.2.0 Beta
- Add support for Windows 64 bits
- Update geoip data
- Add copy to clipboard feature
- Set text of buttons as tooltips
- Display latency 0ms as <1ms
- Add support for mac OSX 32 bits
- Fix a couple of bugs on the install, and on the JLNP version
- Fix first IP point (my ip)

1.1.1 Beta
- Update the traceroute algo to dramatically decrease the traceroute execution time
- Add an option to see DNS associated with network node IP
- Add traceroute latency and DNS lookup time informations
- Increase map movement speed
- Add tooltips on the route table, adjust font size and column size.

1.1.0 Beta
- Add Linux 32/64 bits support
- Fix some issues during the browsing of devices
- Add proxy support 
- Change public IP address mechanism
- Sturdiness when start the application in command line (no need to be in the jar folder)

1.0.8 Beta
- Fatal error dialog when launching on OS which arch is not x86

1.0.7 Beta
- 3308703 : Remaining reference to the JDK 7 that was installed on the development environment (and used by ant compilation task).
- Minor corrections on Manifest and build, clean unused jar entries
- Update header license
- License file not included in the build
- Update build to zip the product and add the README

1.0.6 Beta
- 3308404 : Stand alone execution available
- The Applet version can be deployed by copying the content of the folder into the target Internet location. The JNLP and html files need to be updated with the given location.
- The Stand alone version can be launch by double-click on the org.leo.traceroute.jar 
- If the default memory is insufficient for the stand alone version, launching the jar in command line 
java -Xmx512m -jar org.leo.traceroute.jar
- Update of the Install of the application in both execution cases
- Update of the build.xml

1.0.5 Beta
- First released on Sourceforge
- 3306739 : Correction of a UnsatisfiedLinkError while loading the Jpcap dll

1.0.4 Beta
- Bug in the trace route algo, the last point is not added to the route
- Add the license dialog
- Several minor evolutions and bug corrections

1.0.3 Beta
- Implements the GlassPane class to display messages
- Update the list of layers of WW
- Several minor evolutions and bug corrections

1.0.2 Beta
- Update of the renderer of the Table
- Use of Nimbus Look & Feel

1.0.1 Beta
- First point of the route is the public IP of the client, not the local Lan IP
- Trouble of selection in the Table component 
- Add a progress bar to show the trace route is running

1.0.0 Beta
- First build
- Integration of WWJ and Jpcap OK

