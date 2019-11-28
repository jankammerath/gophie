# Gophie

Gophe is a modern, graphical and cross-platform client, or browser if you like, for "The Internet Gopher" which is defined in RFC 1436. It runs on Microsoft Windows, macOS and Linux. It allows to navigate the Gopherspace as easily as navigating the World Wide Web. You can view Gophermenus, or Gopher pages if you like, read text files, view images and download content provided through the Gopher protocol.

![Gophie on macOS](/screenshot/macos.png)
[View all screenshots](/screenshot/)

## Installing Gophie

Gophie is very eas to install as it comes as a portable Java application in a JAR-file. Simply download the file and execute it on your computer. **All Gophie requires is a Java Runtime Environment**. It was built to work also with older JRE version and currently only requires JVM version 8.

- [Download Gophie for Windows, Mac and Linux (Java JAR-file)](https://github.com/jankammerath/gophie/raw/master/build/Gophie.jar)

Double-click the file on Windows, Mac and Linux will automatically launch Gophie, if you have Java installed. If you do not have Java installed, you can get it from [java.com/download](java.com/download).

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