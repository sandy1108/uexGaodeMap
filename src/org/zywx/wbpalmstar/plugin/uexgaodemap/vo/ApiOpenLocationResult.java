package org.zywx.wbpalmstar.plugin.uexgaodemap.vo;

/**
 * File Description: 定位返回结果
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2023/1/10.
 */
public class ApiOpenLocationResult {
    /**
     *                     String address = amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
     *                     String country = amapLocation.getCountry();//国家信息
     *                     String province = amapLocation.getProvince();//省信息
     *                     String city = amapLocation.getCity();//城市信息
     *                     String district = amapLocation.getDistrict();//城区信息
     *                     String street = amapLocation.getStreet();//街道信息
     *                     String streetNum = amapLocation.getStreetNum();//街道门牌号信息
     *                     String cityCode = amapLocation.getCityCode();//城市编码
     *                     String adCode = amapLocation.getAdCode();//地区编码
     *                     String aoiName = amapLocation.getAoiName();//获取当前定位点的AOI信息
     */

    private double latitude;
    private double longitude;
    private float radius;

    private String address;
    private String country;
    private String province;
    private String city;
    private String district;
    private String street;
    private String streetNum;
    private String cityCode;
    private String adCode;
    private String aoiName;

    public ApiOpenLocationResult() {
    }

    public ApiOpenLocationResult(double latitude, double longitude, float radius, String address, String country, String province, String city, String district, String street, String streetNum, String cityCode, String adCode, String aoiName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.address = address;
        this.country = country;
        this.province = province;
        this.city = city;
        this.district = district;
        this.street = street;
        this.streetNum = streetNum;
        this.cityCode = cityCode;
        this.adCode = adCode;
        this.aoiName = aoiName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getStreetNum() {
        return streetNum;
    }

    public void setStreetNum(String streetNum) {
        this.streetNum = streetNum;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getAdCode() {
        return adCode;
    }

    public void setAdCode(String adCode) {
        this.adCode = adCode;
    }

    public String getAoiName() {
        return aoiName;
    }

    public void setAoiName(String aoiName) {
        this.aoiName = aoiName;
    }
}
