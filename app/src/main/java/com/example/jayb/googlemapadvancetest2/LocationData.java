package com.example.jayb.googlemapadvancetest2;

/**
 * Created by JayB on 9/30/16.
 */
public class LocationData {

    String addr1;
    String addr2;
    String contentid;
    String dist; //거리 (현재위치로부터의 거리)
    String mapx; //좌표x
    String mapy; //좌표y
    String tel;
    String title;
    String firstimage;

    public LocationData(){

    }

    public LocationData(String addr1, String addr2, String contentid, String dist, String mapx, String mapy, String tel, String title, String firstimage){

        this.addr1 = addr1;
        this.addr2 = addr2;
        this.contentid = contentid;
        this.dist = dist;
        this.mapx = mapx;
        this.mapy = mapy;
        this.tel = tel;
        this.title = title;
        this.firstimage = firstimage;

    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddr1() {
        return addr1;
    }

    public void setAddr1(String addr1) {
        this.addr1 = addr1;
    }

    public String getAddr2() {
        return addr2;
    }

    public void setAddr2(String addr2) {
        this.addr2 = addr2;
    }

    public String getContentid() {
        return contentid;
    }

    public void setContentid(String contentid) {
        this.contentid = contentid;
    }

    public String getDist() {
        return dist;
    }

    public void setDist(String dist) {
        this.dist = dist;
    }

    public String getMapx() {
        return mapx;
    }

    public void setMapx(String mapx) {
        this.mapx = mapx;
    }

    public String getMapy() {
        return mapy;
    }

    public void setMapy(String mapy) {
        this.mapy = mapy;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getFirstimage() {
        return firstimage;
    }

    public void setFirstimage(String firstimage) {
        this.firstimage = firstimage;
    }
}
