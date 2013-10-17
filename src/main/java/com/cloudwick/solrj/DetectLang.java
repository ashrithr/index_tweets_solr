package com.cloudwick.solrj;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Detects language for input text
 *
 * @author ashrith
 */
public class DetectLang {

  /*
   * Loads the 'profiles' from langdetect.jar and sends it to 'DetectorFactory'
   */
  public void init() throws LangDetectException, IOException {
    String dirName = "profiles/";
    Enumeration<URL> en = Detector.class.getClassLoader().getResources(dirName);
    List<String> profiles = new ArrayList<String>();
    if (en.hasMoreElements()) {
      URL url = en.nextElement();
      // running from packaged jar will generate file path: "jar:file:file_path"
      JarURLConnection urlCon = (JarURLConnection) url.openConnection();
      JarFile jar = urlCon.getJarFile();
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        String entry = entries.nextElement().getName();
        if (entry.startsWith(dirName)) {
          InputStream in = null;
          try {
            in = Detector.class.getClassLoader().getResourceAsStream(entry);
            profiles.add(IOUtils.toString(in));
          } catch (Exception ex) {
            //zzz
          }
        }
      }
    }
    DetectorFactory.loadProfile(profiles);
  }

  /*
   * Detects the language for the input text
   */
  public String detect(String text) throws LangDetectException {
    Detector detector = DetectorFactory.create();
    detector.append(text);
    return detector.detect();
  }

  /*
   * Gives back an array of probable list of detected languages
   */
  public ArrayList<Language> detectLangs(String text) throws LangDetectException {
    Detector detector = DetectorFactory.create();
    detector.append(text);
    return detector.getProbabilities();
  }
}