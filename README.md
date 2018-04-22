# FreeEed Review

## ElasticSearch dependency

Currently works with elastic-search 6.2
To start the elastic search server, use the script

    start-review.sh
    
which should be in the same directory where all other projects are, such as

    FreeEed
    FreeEedReview
    elasticsearch-6.2.2

Contents of

    start-review.sh

is

    cd elasticsearch-6.2.2
    ./bin/elasticsearch

## To build the war target:

    mvn war:war
