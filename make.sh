#!/bin/bash
cd src/
javac -d ../class/ org/gophie/Gophie.java
cd ../class/
# jar cvfe ../build/Gophie.jar Gophie org/gophie/*.class org/gophie/*/*.class org/gophie/*/*/*.class