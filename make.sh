#!/bin/bash
cd src/
javac -g --release 8 -d ../class/ -classpath ".:../res/*.ttf:../res/*.gif:../res/*.png" org/gophie/Gophie.java
cd ../class/
jar cvfe ../build/Gophie.jar org.gophie.Gophie org/gophie/*.class org/gophie/*/*.class org/gophie/*/*/*.class ../res/*.ttf ../res/*.gif ../res/*.png