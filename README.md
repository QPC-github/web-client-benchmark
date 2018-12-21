## Archived Project
Please note that this project is archived and read-only. 

## This project tests the performance of several OSS Java web clients
New features in this version of the benchmark suite include:
* Upgraded the benchmark suite to use Ning AHC 2.0.0 (snapshot).
* There are now specific benchmark tests for Ning AHC 2.0.0 (snapshot), one for each back end HTTP provider: Netty and Grizzly.
* There are now specific benchmark tests for the Jetty 9 client as well as the Jetty 8 client.
* There is now a benchmark test for the Apache HTTPComponents 4 client.
* All test code run as JUnit tests that run from Maven, instead of plain Java applications that you must invoke from the command line.
* The pom.xml is able to detect different JDK brands and versions that the user has installed on their machine, and is able to switch between them using Maven command line arguments.  Sun JDK and IBM JDK are both supported, and Java 1.6 and 1.7 are both supported.
* The pom.xml contains selectable Maven profiles for Sun HotSpot 1.6, Sun HotSpot 1.7, IBM J9 1.6, and IBM J9 1.7.
* The tests build, then run on the specified JVM brand and version, regardless of what the default Java installation is set to.
* The server base URL is specifiable on the Maven command line.

NOTE: Before running any benchmark, make sure that you set your shell user's
file descriptor limit to at least 4096:

	ulimit -n 4096

You must set that on both the client side and the server side in the shell
before running the tests.

You must also run a server, either local or remote.

To specify a server base URL:

	mvn -DserverBaseUrl=http://machine4:8080

## Selecting JVMs

To specify testing one specific JVM brand and version (the installation
filesystem location is autodetected by the Maven build's pom.xml code):

	mvn -Photspot16
	mvn -Photspot17
	mvn -Pibm16
	mvn -Pibm17

To specify a specific JAVA_HOME path for a JVM, in case it isn't
autodetected:

	mvn -Photspot16 -Dhotspot16Home=/usr/java/jdk1.6.0_38

The code to detect the installation directory of the specified JVM is
multiplatform, so it will work on any OS.  Once the JVM's installation
location is set, the remainder of the build's configuration isolates
the compilation and runtime to use only that JVM installation, so you
get a proper build specifically for that JVM, and the benchmark runs
on the same JVM installation that compiled the code.

## Specifying the web client test to benchmark

To specify benchmarking only a specific client test class:

	mvn -Dtest=NingAhcBenchmarkTest

Or, to specify it down to the very test you want to run:

	mvn -Dtest=NingAhcBenchmarkTest\#testAsyncRequests

## Putting it all together

	mvn -Photspot17 -DserverBaseUrl=http://machine4:8080 -Dtest=NingAhcBenchmarkTest\#testAsyncRequests

-- OR --

	mvn -Photspot16 -Dhotspot16Home=/usr/lib/jvm/java-6-openjdk-amd64 -DserverBaseUrl=http://10.9.252.99:8081 -Dtest=NingAhcBenchmarkTest

-- OR --

	mvn -Pibm16 -Dibm16Home=$JAVA_HOME -DserverBaseUrl=http://machine4:8080 -Dtest=Jetty8BenchmarkTest
