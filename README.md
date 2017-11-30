# FreeEed Search UI

Currently need apache-solr-4.0

To build a war file:

mvn war:war

## How metadata search works

1. Process the dataset in FreeEed
2. This will create a case in FreeEedUI
3. Edit the case. Load the zip file and the metadata file
4. Do searches, add tags.
5. Export the load file - you will get back the metadata file, but this time with the tags.