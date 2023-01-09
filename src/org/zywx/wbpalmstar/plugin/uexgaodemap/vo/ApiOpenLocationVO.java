package org.zywx.wbpalmstar.plugin.uexgaodemap.vo;

/**
 * File Description: apiOpenLocation参数VO
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2023/1/9.
 */
public class ApiOpenLocationVO {

    /**
     * 是一次性定位还是持续定位
     */
    private String onceLocation;
    /**
     * 持续定位的间隔
     */
    private String locationInterval;
    /**
     * 是否需要返回逆地理编码信息
     */
    private String needAddress;

    public String getOnceLocation() {
        return onceLocation;
    }

    public void setOnceLocation(String onceLocation) {
        this.onceLocation = onceLocation;
    }

    public String getLocationInterval() {
        return locationInterval;
    }

    public void setLocationInterval(String locationInterval) {
        this.locationInterval = locationInterval;
    }

    public String getNeedAddress() {
        return needAddress;
    }

    public void setNeedAddress(String needAddress) {
        this.needAddress = needAddress;
    }
}
