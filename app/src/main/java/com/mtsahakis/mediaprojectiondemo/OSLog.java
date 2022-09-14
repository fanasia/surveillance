package com.mtsahakis.mediaprojectiondemo;

import android.app.Application;

public class OSLog extends Application {
    private String url;
    private String title;
    private String content;
    private String username;
    private String appname;
    private double longitude;
    private double latitude;

    public String getUrl() {
        return url;
    }

    public String getTitle(){
        return title;
    }

    public String getContent(){
        return content;
    }

    public String getUsername() {
        return username;
    }

    public String getAppname(){
        return appname;
    }

    public double getLongitude(){
        return longitude;
    }

    public double getLatitude(){
        return latitude;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "{" +
                "'url':'" + this.url + '\'' +
                ", 'title':'" + this.title + '\'' +
                ", 'content':'" + this.content + '\'' +
                ", 'username':'" + this.username + '\'' +
                ", 'appname':'" + this.appname + '\'' +
                ", 'longitude':" + this.longitude +
                ", 'latitude':" + this.latitude +
                '}';
    }
}
