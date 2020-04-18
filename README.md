# Gophie

Gophe is a modern, graphical and cross-platform client, or browser if you like, for "The Internet Gopher" which is defined in RFC 1436. It runs on Microsoft Windows, macOS and Linux. It allows to navigate the Gopherspace as easily as navigating the World Wide Web. You can view Gophermenus, or Gopher pages if you like, read text files, view images and download content provided through the Gopher protocol.

![Gophie on macOS](/screenshot/macos.png)
[View all screenshots](/screenshot/)

## Installing Gophie

Gophie is very eas to install as it comes as a portable Java application in a JAR-file. Simply download the file and execute it on your computer. **All Gophie requires is a Java Runtime Environment**. It was built to work also with older JRE version and currently only requires JVM version 8.

- [Download the Windows, Mac or Linux release](https://github.com/jankammerath/gophie/releases/)
- [Download the latest Java JAR-file](https://github.com/jankammerath/gophie/raw/master/build/Gophie.jar)

### Install on Windows

The Windows package is a ZIP-file that includes the EXE-file for Windows with Gophie. You can extract the ZIP-file and put it somewhere in a folder, on a USB stick, network drive or wherever you would like to store the EXE-file. Note that you still need the Java JRE from [java.com/download](java.com/download) to run the Gophie.exe file.

### Install on Mac

The Mac package for Gophie is a DMG-file. You can download the DMG-file, open it and drag the Gophie icon into the application folder. Afterwards Gophie will be available in your applications and you can launch it from wherever you wish.

### Install on Linux

The Linux package is simply a tar.gz-archive with an executable. The executable is a bash-file with the JAR-file attached. You can either double-click the executable named "Gophie" or launch it from the terminal of your choice. If you wish to have Gophie present wherever you need it in your system, just copy the executable file into your "/usr/bin"-directory.

### Using the JAR-file itself

Double-click the Java JAR-file on Windows, Mac and Linux will automatically launch Gophie, if you have Java installed. If you do not have Java installed, you can get it from [java.com/download](java.com/download).

## Building Gophie

In order to stay small, reproducable and trustworthy, Gophie was not build with any large-scale IDE, but in plain and simple Java. You can clone this repository and build Gophie with your installed Java SDK yourself. Just run the [make.sh shell script](make.sh) included in this repository.

```
./make.sh
```

The script will create a JAR-file in the build directory for you to run. If you wish to launch Gophie's jar file from the command line, simply use the java cli.

```
java -jar Gophie.jar
```

If you're having trouble with Gophie, it is quite verbose when it comes to errors and exceptions. It is recommended that you run Gophie through Java on the command line to get the exception and error message output it might report.

## Configuration file

A configuration file named "config.ini" resides in a directory named "Gophie" in the user's home directory. The configuration file [config.ini](https://github.com/jankammerath/gophie/blob/master/config/config.ini) allows to configure the GopherHome, default charset, various colors and the font for the page view or content area. The following table outlines the main configuration options. For a complete list, please have a look into the [config.ini](https://github.com/jankammerath/gophie/blob/master/config/config.ini) itself.

| Section       | Setting             | Default value        | Description                              |
| :-------------|:--------------------|:---------------------|:-----------------------------------------|
| Navigation    | GOPHERHOME          | gopher.floodgap.com  | The GopherHome also known as "Homepage"  |
| Network       | DEFAULT_CHARSET     | UTF-8                | Charset to use for text encoding         |
| Appearance    | PAGE_FONT           | Inconsolata (Custom) | Font for the text in the page view       |
| Appearance    | PAGE_FONT_SIZE      | 17                   | Font size for the text in the page view  |
| Appearance    | PAGE_ICON_FONT_SIZE | 10                   | Size of the icon font in the page view   |

All configuration settings are part of a corresponding configuration section. When changing the configuration file, please ensure to put the settings into the section the setting belongs to.

## Gopher Protocol Support

Gophie aims to support the Gopher protocol from RFC 1436 entirely so that users have an unlimited graphical Gopher experience. The following provides an overview of which gopher items and part of the gopher protocol are supported. HTML files are currently not being rendered, but redirects with *URL:* in the Gopher item selector are supported and the user is prompted to open his system's WWW browser.

| Item Type          | Code | Support             |Handling                                               |
| :------------------|:-----|:--------------------|:------------------------------------------------------|
| Text File          | 0    | Fully supported     | Displayed inside Gophie                               |
| Gopher Menu        | 1    | Fully supported     | Rendered inside Gophie                                |
| CCSO Nameserver    | 2    | Only with terminal  | Works like Telnet, user needs to operate CCSO         |
| Error Code         | 3    | Fully supported     | Rendered inside Gophie                                |
| BinHex File        | 4    | Fully supported     | Allows user to download this file type                |
| DOS File           | 5    | Fully supported     | Allows user to download this file type                |
| UUEncoded File     | 6    | Fully supported     | Allows user to download this file type                |
| Full Text-Search   | 7    | Fully supported     | Rendered inside Gophie                                |
| Telnet             | 8    | Fully supported     | Executes "telnet://"-URI on the system                |
| Binary File        | 9    | Fully supported     | Allows user to download this file type                |
| GIF File           | g    | Fully supported     | Displayed inside Gophie                               |
| Image File         | I    | Fully supported     | Displayed inside Gophie                               |
| Telnet 3270        | T    | Depends on OS       | Treated the same as Telnet item                       |
| HTML file          | h    | Partially supported | Opens WWW with browser, HTML displayed as text        |
| Information        | i    | Fully supported     | Rendered inside Gophie                                |
| Sound File         | s    | Fully supported     | Allows user to download this file type                |

Protocol extensions are recommended extensions to RFC 1436 like Gopher+ or Gopher II are not supported by Gophie. The development of features focuses on implementing RFC 1436 and providing a seamsless experience for the user without sacrificing the proper implementation of the protocol specification.

## Help and FAQs

If you need help with Gophie you can of course open an issue here on GitHib in case you really found a bug. You can also find me on freenode as @derjanni. If you have trouble using Gophie, kindly check this README file first before asking any questions. I will track all the questions and update the README file accordingly.