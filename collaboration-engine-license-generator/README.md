# collaboration-engine-license-generator

This module provides a generator of license files in JSON format.
It is invoked by the main method from the CLI, e.g.

```
$ mvn package
$ java -jar target/collaboration-engine-license-generator-X.Y-SNAPSHOT-jar-with-dependencies.jar 'Acme Corporation' 2000 2022-12-31
```

Change `X.Y-SNAPSHOT` with the version you want to use, e.g. the current one in the POM.

The produced license will contain a unique key and a checksum of its content.

Note: You have to call the jar-with-dependencies jar, not the default module-only one. 
