# Sparklin - Kotlin Language Support for Apache Spark
Welcome to Sparklin project. This is a proof-of-concept software includes experimental 
new Kotlin REPL (aka `KShell`) which supports extensions and custom commands via plugin system. 
Spark supported by *separate plugins* for versions 1.x and 2.x.

# Installation
Current version of the project is 0.1 and this version should be checked out.
```bash
git clone https://github.com/khud/sparklin
git checkout 0.1
```
Sparkling can be built as usual:
```bash
mvn clean install
```
One can run `KShell` out of box by following command from sparklin home directory in the terminal:
```bash
bin/kshell.sh
```
# Spark 1.x
By default `KShell` don't need a configuration file, but if you want to try Spark you need to enable plugin.
It can be done by passing system property `config.path` to JVM. Example of Spark 1.x plugin configuration you may
find in `$SPARKLIN_HOME/conf/spark1x.properties`. Of course you could create `.kshell` file in your home directory but
using separate files for various environments is highly recommended. Please set appropriate `SPARK_HOME` variable in 
the file `bin/sparklin-spark1x.sh`.

`JavaSparkContext` is available as `sc`, `SQLContext` is available as `sqlContext`. 
 
# Spark 2.x
The configuration is completely the same as in the case of Spark 1.x but you need `$SPARKLIN_HOME/conf/spark2x.properties`
and `bin/sparklin-spark2x.sh` instead. Do not forget set `SPARK_HOME` to your Spark 2.x installation.

`SparkSession` is available as `spark`, `JavaSparkContext` is available as `sc`.
At the moment Sparklin does not have additional Spark runtime support for Kotlin for both Spark versions. 
But it has planned to be in version 0.2.

# Environment Configuration
Sparklin version 0.1 is tested in following environment:

* Mac OS X 10.11.6
* Java 1.8.0_152
* Maven 3.3.3
* Kotlin 1.2.20
* Spark 1.6.1
* Spark 2.2.0 

