#!/bin/bash
cd src/
javac -g -d ../class/ -classpath ".:../res/*.ttf:../res/*.gif" org/gophie/Gophie.java
cd ../class/
jar cvfe ../build/Gophie.jar org.gophie.Gophie org/gophie/*.class org/gophie/*/*.class org/gophie/*/*/*.class ../res/*.ttf ../res/*.gif