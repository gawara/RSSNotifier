/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rssnotifier;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import javax.mail.Message;
import org.codemonkey.simplejavamail.Email;
import org.codemonkey.simplejavamail.Mailer;
import org.codemonkey.simplejavamail.TransportStrategy;

/**
 *
 * @author takeshi
 */
public class RSSNotifier {

    String feedUrl = null;
    XmlReader xmlReader;
    SyndFeed feed;

    String smtpAddress = null;
    int smtpPort = 587;
    String mailUser = null;
    String mailPass = null;

    String tempFilePath = null;
    int checkInterval = 5 * 60 * 1000;

    RSSNotifier(String url) throws Exception {
        this.feedUrl = url;
    }

    public void setMailConfig(String smtp, int port, String user, String pass) throws Exception {
        this.smtpAddress = smtp;
        this.smtpPort = port;
        this.mailUser = user;
        this.mailPass = pass;
    }

    public void setCheckInterval(int sec) throws Exception {
        this.checkInterval = sec;
    }

    public void start() throws Exception {
        loadFeedContents();
        String lastUpdateTime = this.getLastUpdateTime();
        this.createTempFile(lastUpdateTime);
        while (true) {
            lastUpdateTime = this.readTempFile();
            loadFeedContents();
            this.checkNewContentsAndSendMail(lastUpdateTime);
            lastUpdateTime = this.getLastUpdateTime();
            this.createTempFile(lastUpdateTime);
            Thread.sleep(this.checkInterval);
        }
    }

    private void loadFeedContents() throws Exception {
        String currentPath = new File(".").getAbsolutePath();
        currentPath = currentPath.substring(0, currentPath.length() - 1);
        String xmlFile = currentPath + "rss.xml";
        Process p = Runtime.getRuntime().exec("\"" + currentPath + "ext\\curl\" -o \"" + xmlFile + "\" -L " + this.feedUrl);
        int ret = p.waitFor();
        File file = new File(xmlFile);
        SyndFeedInput input = new SyndFeedInput();
        this.feed = input.build(new XmlReader(file));
    }

    private void checkNewContentsAndSendMail(String lastUpdateTime) throws Exception {
        SyndFeed localFeed = this.feed;
        for (Object obj : localFeed.getEntries()) {
            SyndEntry entry = (SyndEntry) obj;
            System.out.println("      lastUpdateTime: " + lastUpdateTime);
            System.out.println("entry lastUpdateTime: " + entry.getUpdatedDate());
            if (lastUpdateTime.trim().equals(entry.getUpdatedDate().toString().trim())) {
                break;
            }
            System.out.println("send mail....");

            String title = entry.getTitle().toString();
            String contents = "Jenkinsでビルドエラーが発生しました。\n"
                    + "エラー内容を確認して、ビルドを正常な状態に修正する必要があります。\n"
                    + "\n"
                    + "エラーが発生したジョブ：\n"
                    + entry.getTitle().toString() + "\n"
                    + "URL：\n"
                    + entry.getLink().toString() + "\n";
            System.out.println(contents);
            this.sendMail(title, contents);
            Thread.sleep(10000);
        }
    }

    private String getLastUpdateTime() throws Exception {
        SyndFeed localFeed = this.feed;
        SyndEntry entry = (SyndEntry) localFeed.getEntries().get(0);
        return entry.getUpdatedDate().toString();
    }

    private void sendMail(String subject, String contents) throws Exception {
        final Email email = new Email();
        email.setFromAddress("fromName", "from_mail@gmail.com");
        email.addRecipient("toName", "to_mail@gmail.com", Message.RecipientType.TO);
        email.setSubject(subject);
        email.setText(contents);
        new Mailer(this.smtpAddress, this.smtpPort, this.mailUser, this.mailPass, TransportStrategy.SMTP_TLS).sendMail(email);
    }

    private void createTempFile(String contents) throws Exception {
        String currentPath = new File(".").getAbsolutePath();
        currentPath = currentPath.substring(0, currentPath.length() - 1);
        this.tempFilePath = currentPath + "lastUpdateTime.tmp";
        File file = new File(this.tempFilePath);
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        pw.println(contents);
        pw.close();
    }

    private String readTempFile() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(this.tempFilePath));
        String lastUpdateTime = br.readLine();
        System.out.println("lastUpdateTime: " + lastUpdateTime);
        br.close();
        return lastUpdateTime;
    }
}
