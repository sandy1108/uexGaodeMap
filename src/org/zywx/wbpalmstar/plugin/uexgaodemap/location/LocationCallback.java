package org.zywx.wbpalmstar.plugin.uexgaodemap.location;

import org.zywx.wbpalmstar.plugin.uexgaodemap.vo.ApiOpenLocationResult;

public interface LocationCallback {

    public void onLocation(ApiOpenLocationResult result);

}