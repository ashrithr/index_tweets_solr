package com.cloudwick.solrj;

import com.cybozu.labs.langdetect.LangDetectException;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Indexes tweets from twitter using `Solrj` and `twitter4j` libraries. To use this create a collection
 * `tweets` as explained in Readme.md
 *
 * @author ashrith
 */
public class IndexTweets {

  static Logger logger = Logger.getLogger(IndexTweets.class);

  public static void main(String[] args) throws IOException, SolrServerException, LangDetectException {
    /*
     * Solr URL & Total Time to run
     */
    String serverURL = (args != null && args.length > 0) ? args[0] : "http://localhost:8983/solr/tweets";
    int runTime = (args !=null && args.length > 1) ? Integer.parseInt(args[1]) : 50; // default to 50 seconds

    /*
     * Connect to the Solr server at the specified URL
     */
    final SolrServer solr = new HttpSolrServer(serverURL);

    /*
     * clean up existing index
     */
    logger.info("Deleting existing documents in the index 'tweets'");
    try {
      solr.deleteByQuery("*:*");
      solr.commit();
    } catch (Exception ex) {
      logger.error("Cannot commit to the solr instance, check if solr is running");
      System.err.println(ex);
      System.exit(1);
    }

    /*
     * configure twitter OAuth params
     */
    ConfigurationBuilder cb = new ConfigurationBuilder();

    /*
     * Initialize lang detection engine
     */
    final DetectLang detectLang = new DetectLang();
    detectLang.init();

    /*
     * Initialize twitter streaming
     */
    TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
    StatusListener listener = new StatusListener() {
      @Override
      public void onStatus(Status status) {
        /*
         * initializes each time a tweet is received
         */
        User user = status.getUser();
        String username = status.getUser().getScreenName();
        long tweetId = status.getId();
        Date timeStamp = status.getCreatedAt();
        String tweet = status.getText();
        int favoritesCount = status.getFavoriteCount();
        URLEntity urlEntities[] = status.getURLEntities();
        ArrayList<String> urls = new ArrayList<String>();
        for (URLEntity urlEntity: urlEntities) {
          urls.add(urlEntity.getText());
        }
        String lang = status.getIsoLanguageCode();
        if (lang == null) {
          try {
            lang = detectLang.detect(tweet);
          } catch (LangDetectException e) {
            // ignore exception and use default lang as english
            lang = "en";
          }
        }
        try {
          addSolrDocument(solr, tweetId, username, tweet, lang, timeStamp, favoritesCount, urls);
        } catch (IOException e) {
          e.printStackTrace();
        } catch (SolrServerException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}

      @Override
      public void onTrackLimitationNotice(int i) {}

      @Override
      public void onScrubGeo(long l, long l2) {}

      @Override
      public void onStallWarning(StallWarning stallWarning) {}

      @Override
      public void onException(Exception e) {}
    };

    FilterQuery fq = new FilterQuery();
    // keywords to filter the stream on
    String keywords[] = {"big data", "nfl", "usa"};
    fq.track(keywords);
    twitterStream.addListener(listener);
    twitterStream.filter(fq);

    try {
      Thread.sleep(runTime * 1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      twitterStream.cleanUp();
      twitterStream.shutdown();
    }

    /*
     * Do a normal or 'hard' commit to make these new docs searchable
     */
    System.out.println();
    logger.info("Committing the documents to index");
    solr.commit(true, true);

    System.out.println();
    logger.info("Querying inserted documents, (showing top 10 hits)");
    for (SolrDocument next : simpleSolrQuery(solr, "*:*", 10)) {
      prettyPrint(System.out, next);
    }
  }

  /**
   * Adds document to solr
   * @param solr instance of SolServer
   * @param id tweet id
   * @param userName username who posted the tweet
   * @param tweet text of the tweet
   * @param lang language in which tweet was posted (best-effort)
   * @param timeStamp time at which user posted tweet
   * @param favoritesCount specifies number of times the tweet has been favorited
   * @param links links present in the tweet
   * @throws IOException
   * @throws SolrServerException
   */
  private static void addSolrDocument(SolrServer solr, long id, String userName, String tweet, String lang,
                                      Date timeStamp, int favoritesCount, ArrayList<String> links)
      throws IOException, SolrServerException {
    /*
     * Send the SolrInputDocument to the Solr update request handler over HTTP
     */
    SolrInputDocument document = new SolrInputDocument();
    document.addField("id", id);
    document.addField("screen_name", userName);
    document.addField("type", "post");
    document.addField("lang", lang);
    document.addField("timestamp", timeStamp);
    document.addField("favourites_count", favoritesCount);
    document.addField("text", tweet);
    for(String link: links) {
      document.addField("link", link);
    }
    logger.info("Indexing Document");
    System.out.println(document);
    if (document != null) {
      solr.add(document);
    }
  }

  /**
   * Performs simple solr query
   * @param solr
   * @param query
   * @param rows
   * @return
   * @throws SolrServerException
   */
  private static SolrDocumentList simpleSolrQuery(SolrServer solr, String query, int rows) throws SolrServerException {
    /*
     * Use a SolrQuery object to construct the match all docs query
     */
    SolrQuery solrQuery = new SolrQuery(query);
    solrQuery.setRows(rows);
    QueryResponse resp = solr.query(solrQuery);
    SolrDocumentList hits = resp.getResults();
    return hits;
  }

  /**
   * Pretty-print each solrDocument in the results to stdout
   * @param out
   * @param doc
   */
  private static void prettyPrint(PrintStream out, SolrDocument doc) {
    List<String> sortedFieldNames = new ArrayList<String>(doc.getFieldNames());
    Collections.sort(sortedFieldNames);
    out.println();
    for (String field : sortedFieldNames) {
      out.println(String.format("\t%s: %s", field, doc.getFieldValue(field)));
    }
    out.println();
  }
}
