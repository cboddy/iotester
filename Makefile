
.PHONY: build
build: compile  iotester 

.PHONY: compile
compile:
	mkdir -p build
	javac -d build `find src/ -name \*.java`

.PHONY: iotester 
iotester:
	echo "Manifest-Version: 1.0" > jar.manifest
	echo "Name: IOTester" >> jar.manifest
	echo "Build-Date: " `date` >> jar.manifest
	echo "Specification-Title: iotester" >> jar.manifest
	echo "Specification-Version: 1" >> jar.manifest
	echo "Specification-Vendor: theBoddy" >> jar.manifest 	
	echo "Implementation-Version: 1.01" >> jar.manifest
	echo "Main-Class: im.boddy.iotester.IOTester" >> jar.manifest
	echo "" >> jar.manifest

	jar -cfm IOTester.jar jar.manifest  -C build  .
	rm -f jar.manifest

.PHONY: clean
clean:
	rm -Rf build
	rm -f IOTester.jar
