#Indexing tweets using Solr

Indexes the tweets from Twitter Streaming API to Solr using `solrj` java library.

###Creating collection tweets on single solr core

This example requires to create a collection called `tweets` using the following steps:

```
cp -r $SOLR_HOME/example $SOLR_HOME/tweets
cp -r $SOLR_HOME/tweets/solr/collection1 $SOLR_HOME/tweets/solr/tweets
rm -r $SOLR_HOME/tweets/solr/collection1
echo 'name=tweets' > $SOLR_HOME/tweets/solr/tweets/core.properties
```
where,

  * `$SOLR_HOME` is path where solr is installed

**Note**:

  * `schema.xml` from tweets should be replaced with `resources/schema.xml` from project dir
  * Create a new file at `$SOLR_HOME/tweets/solr/tweets/conf/wdfftypes.txt` with contents from `resources/wdfftypes.txt`
  * Create `resources/twitter4j.properties` with your twitter oauth credentials, contents should be as follows:

      ```
      debug=true
      oauth.consumerKey=
      oauth.consumerSecret=
      oauth.accessToken=
      oauth.accessTokenSecret=
      ```

###Running

**Staring a Solr instance**

```
cd $SOLR_HOME/tweets && java -jar start.jar
```

**Packing a jar**

```
mvn install assembly:assembly
```

**Running the indexer**

```
java -cp target/IndexTweets-1.0-SNAPSHOT-jar-with-dependencies.jar com.cloudwick.solrj.IndexTweets
```


**URLS**:

* [admin console](http://localhost:8983/solr/)
* [solarits console](http://localhost:8983/solr/tweets/browse)
