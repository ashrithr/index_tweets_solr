#!/bin/sh

if [ -z $1 ]
then
  echo "Please provide at least one file to index."
  exit 1
fi

FILES=$*
COLLECTION=tweets
URL=http://localhost:8983/solr/${COLLECTION}/update
#URL=http://localhost:8983/solr/update

for f in $FILES; do
  echo Posting file $f to $URL
  curl $URL --data-binary @$f -H 'Content-type:application/xml'
  echo
done

curl $URL --data-binary '<commit/>' -H 'Content-type:application/xml'
