#!/bin/bash
mkdir -p package/macosx
cp build/gophie-logo.icns package/macosx/Gophie.icns 
export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
$JAVA_HOME/bin/javapackager -deploy -native dmg \
        -srcfiles build/Gophie.jar -appclass org.gophie.Gophie -name Gophie \
        -outdir deploy -outfile Gophie -v
rm -r package/
cp deploy/bundles/Gophie-1.0.dmg release/Gophie-1.0.dmg
rm -r deploy/