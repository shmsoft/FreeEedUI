# FreeEed Search UI

## SOLR dependency

Currently works with apache-solr-4.0
To start the solr server, use the script 

    start-solr.sh
    
which should be in the same directory where all other projects are, such as

    FreeEed
    FreeEedUI
    freeeed-solr
    
Contents of

    start-solr.sh
       
is

    cd freeeed-solr/example
    java -Xmx1024M -jar start.jar &
         
## To build the war target:

    mvn war:war
