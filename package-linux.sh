#!/bin/bash
mkdir -p release/Linux
cat build/linux-stub.sh build/Gophie.jar > release/Linux/Gophie && chmod +x release/Linux/Gophie 
cd release/Linux/
tar -czvf Gophie.tar.gz Gophie
cd ../../ 