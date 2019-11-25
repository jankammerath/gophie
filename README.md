# Gophie

Gophe is a modern, graphical and cross-platform client, or browser if you like, for "The Internet Gopher" which is defined in RFC 1436. It runs on Microsoft Windows, macOS and Linux. It allows to navigate the Gopherspace as easily as navigating the World Wide Web. You can view Gophermenus, or Gopher pages if you like, read text files, view images and download content provided through the Gopher protocol.

![Gophie on macOS](/screenshot/macos.jpg)
![View all screenshots](/screenshot/)

## Installing Gophie

Gophie is very eas to install as it comes as a portable Java application in a JAR-file. Simply download the file and execute it on your computer. All Gophie requires is a Java Runtime Environment. It was built to work also with older JRE version and currently only requires JVM version 8.

- [Download Gophie for Windows, Mac and Linux (Java JAR-file)](https://github.com/jankammerath/gophie/raw/master/build/Gophie.jar)

Double-click the file on Windows, Mac and Linux will automatically launch Gophie, if you have Java installed. If you do not have Java installed, you can get it from (java.com/download).

## Building Gophie

In order to stay small, reproducable and trustworthy, Gophie was not build with any large-scale IDE, but in plain and simple Java. You can clone this repository and build Gophie with your installed Java SDK yourself. Just run the [make.sh shell script](make.sh) included in this repository.

```
./make.sh
```

The script will create a JAR-file in the build directory for you to run.