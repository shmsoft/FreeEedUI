# FreeEed Search UI

## SOLR dependency

Currently works with apache-solr-7.2
To start the solr server, use the script 

    start-solr.sh
    
which should be in the same directory where all other projects are, such as

    FreeEed
    FreeEedReview
    freeeed-solr

Contents of

    start-solr.sh

is

    cd freeeed-solr
    bin/solr -e schemaless &
         
## To build the war target:

    mvn war:war
