/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rssnotifier;


/**
 *
 * @author takeshi
 */
public class App {

    public static void main(String[] args) throws Exception {

        try {
            
            RSSNotifier rssNotifier = new RSSNotifier("http://localhost:8080/rssFailed");
            rssNotifier.setMailConfig("smtp.gmail.com", 587, "******@gmail.com", "*****");
            rssNotifier.setCheckInterval(10000);
            rssNotifier.start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
