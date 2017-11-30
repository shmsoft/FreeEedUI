# FreeEed Search UI

## SOLR dependency

Currently works with apache-solr-4.0
To start the solr server, use the 

<<<<<<< HEAD
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
=======
mvn war:war

## How metadata search works

1. Process the dataset in FreeEed
2. This will create a case in FreeEedUI
3. Edit the case. Load the zip file and the metadata file
4. Do searches, add tags.
5. Export the load file - you will get back the metadata file, but this time with the tags.
>>>>>>> 554e744b4fd4646475da39a6cd7d02be0df789f7
