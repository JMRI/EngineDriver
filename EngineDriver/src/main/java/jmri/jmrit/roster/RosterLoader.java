package jmri.jmrit.roster;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import jmri.enginedriver.threaded_application;

public class RosterLoader {
    static final String activityName = "roster";
    final URL rosterUrl;

    public RosterLoader(String Url) {
        try {
            this.rosterUrl = new URL(Url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    protected InputStream getInputStream() {
        try {
            return rosterUrl.openConnection().getInputStream();
        } catch (IOException e) {
            Log.e(threaded_application.applicationName, activityName + ": InputStream(): Error retrieving roster xml: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, RosterEntry> parse() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        HashMap<String, RosterEntry> roster = new HashMap<String, RosterEntry>();
        try {
            InputStream rosterStream = this.getInputStream();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dom = builder.parse(rosterStream);
            Element root = dom.getDocumentElement();
            NodeList items = root.getElementsByTagName("locomotive");
            for (int i = 0; i < items.getLength(); i++) {
                RosterEntry entry = new RosterEntry(items.item(i));
                entry.setHostURLs(rosterUrl.getHost() + ":" + rosterUrl.getPort());
                roster.put(entry.getId(), entry);
            }
            rosterStream.close();
        } catch (Exception e) {
            Log.e(threaded_application.applicationName, activityName + ": parse(): Error building hashmap of Roster Entries: " + e.getMessage());
            return null;
        }
        return roster;
    }
}
