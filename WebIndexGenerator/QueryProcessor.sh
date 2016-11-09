export MAVEN_OPTS="-Xms5120M -Xmx6144M" # Increase the heap-memory for url-table and lexicon
mvn exec:java -Dexec.mainClass="websearch.queryprocessor.QueryProcessor"