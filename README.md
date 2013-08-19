## This project tests the performance of several OSS Java web clients

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
