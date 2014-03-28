CP = `find lib -name "*.jar" -printf %p:`
JAVA_BUILD_OPTS = -cp .:$(CP)
CP_SPACE = `ls lib/*.jar`


.PHONY: build
build: compile  iotester 

.PHONY: compile
compile:
	mkdir -p build
	javac $(JAVA_BUILD_OPTS) -d build `find src/ -name \*.java`

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

.PHONY: unit_tests 
unit_tests:
	echo "Manifest-Version: 1.0" > jar.manifest
	echo "Name: IOTester" >> jar.manifest
	echo "Build-Date: " `date` >> jar.manifest
	echo "Class-Path: " $(CP_SPACE)>> jar.manifest
	echo "Specification-Title: iotester" >> jar.manifest
	echo "Specification-Version: 1" >> jar.manifest
	echo "Specification-Vendor: theBoddy" >> jar.manifest 	
	echo "Implementation-Version: 1.01" >> jar.manifest
	echo "Main-Class: im.boddy.iotester.unit_tests.AllTests" >> jar.manifest
	echo "" >> jar.manifest

	jar -cfm IOTesterUnitTests.jar jar.manifest  -C build  .
	rm -f jar.manifest

.PHONY: clean
clean:
	rm -Rf build
	rm -f IOTester.jar
