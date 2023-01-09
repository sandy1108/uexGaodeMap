package org.zywx.wbpalmstar.plugin.uexgaodemap.location;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.engine.DataHelper;
import org.zywx.wbpalmstar.plugin.uexgaodemap.vo.ApiOpenLocationResult;
import org.zywx.wbpalmstar.plugin.uexgaodemap.vo.ApiOpenLocationVO;

import java.util.ArrayList;

public class AMapLocationHandler {

    private static final String TAG = "AMapLocation";

    private static final String SP_GAODE_LOCATION_CACHE = "GaodeLocSdkInfo";
    private static final String LAST_LAT = "last_lat";
    private static final String LAST_LOG = "last_log";
    private static final String LAST_RAD = "last_rad";
    private static final String LAST_LOCATION_ALL = "last_location_all";

    static final long STEP = 1000L * 30;
    static final int INTERVAL = 1000 * 2;

    private static AMapLocationHandler instance;

    //声明mLocationClient对象
    public AMapLocationClient mLocationClient;
    //声明mLocationOption对象
    public AMapLocationClientOption mLocationOption = null;

    private MyLocationListener mListener;
    private final SharedPreferences mLocalInfo;
    private long mLastLocationTime;
    private final ArrayList<LocationCallback> mCallbackList;

    private AMapLocationHandler(Context ctx) throws Exception {
        mLocalInfo = ctx.getSharedPreferences(SP_GAODE_LOCATION_CACHE, Context.MODE_MULTI_PROCESS);
        mCallbackList = new ArrayList<LocationCallback>();
        initLocation(ctx.getApplicationContext());
    }

    public static AMapLocationHandler get(Context ctx) throws Exception {
        if (null == instance) {
            instance = new AMapLocationHandler(ctx);
        }
        return instance;
    }

    public static void agreePrivacy(Context ctx) {
        AMapLocationClient.updatePrivacyShow(ctx,true,true);
        AMapLocationClient.updatePrivacyAgree(ctx,true);
    }

    public void setLocationOptions(ApiOpenLocationVO apiOpenLocationVO) {
        if (mLocationOption == null) {
            return;
        }
        String onceLocation = apiOpenLocationVO.getOnceLocation();
        if (!TextUtils.isEmpty(onceLocation)) {
            mLocationOption.setOnceLocation(onceLocation.equals("1"));
        }
        String locationInterval = apiOpenLocationVO.getLocationInterval();
        if (!TextUtils.isEmpty(locationInterval)) {
            mLocationOption.setInterval(Integer.parseInt(locationInterval));
        }
        String needAddress = apiOpenLocationVO.getNeedAddress();
        if (!TextUtils.isEmpty(needAddress)) {
            mLocationOption.setNeedAddress(needAddress.equals("1"));
        }
        mLocationClient.setLocationOption(mLocationOption);
    }

    public void openLocation(LocationCallback callback) {
        if (!mCallbackList.contains(callback)) {
            mCallbackList.add(callback);
        }
//        if (!mLocationClient.isStarted()) {
//        }
        requestLocation();
    }

    public void closeLocation(LocationCallback callback) {
        if (callback != null) {
            mCallbackList.remove(callback);
        }
        if (0 == mCallbackList.size()) {
            mLocationClient.stopLocation();
        }
    }

    private void initLocation(Context ctx) throws Exception {
        mListener = new MyLocationListener();

        mLocationClient = new AMapLocationClient(ctx);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位监听
        mLocationClient.setLocationListener(mListener);
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(INTERVAL);
        mLocationOption.setOnceLocation(false);
        mLocationOption.setNeedAddress(true);
        mLocationOption.setLocationCacheEnable(true);
        //设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
    }

    private void destroyLocation() {
        if (null != mLocationClient) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
            mLocationClient = null;
            mListener = null;
        }
    }

    private void saveLastLoc(double lat, double log, float radius) {
        Editor edit = mLocalInfo.edit();
        edit.putString(LAST_LAT, Double.toString(lat));
        edit.putString(LAST_LOG, Double.toString(log));
        edit.putString(LAST_RAD, Float.toString(radius));
        edit.commit();
    }

    private void saveLastLocAll(String locationRes) {
        Editor edit = mLocalInfo.edit();
        edit.putString(LAST_LOCATION_ALL, locationRes);
        edit.commit();
    }

    private void requestLocation() {
        long now = System.currentTimeMillis();
        double llat = getLastLat();
        double llog = getLastLog();
        float lrad = getLastRad();
        String lastLocationAll = getLastLocationAll();
        ApiOpenLocationResult lastResult = null;
        if (!TextUtils.isEmpty(lastLocationAll)) {
            lastResult = DataHelper.gson.fromJson(lastLocationAll, ApiOpenLocationResult.class);
        }
        if ((now - mLastLocationTime) < STEP && (llat > 0 && llog > 0)) {
            innerLocCallback(lastResult);
        } else {
            asyLocation();
        }
    }

    private void asyLocation() {
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为1000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        //启动定位
        mLocationClient.startLocation();
    }

    private double getLastLat() {
        double result = 0;
        String lat = mLocalInfo.getString(LAST_LAT, null);
        if (null != lat) {
            result = Double.parseDouble(lat);
        }
        return result;
    }

    private double getLastLog() {
        double result = 0;
        String log = mLocalInfo.getString(LAST_LOG, null);
        if (null != log) {
            result = Double.parseDouble(log);
        }
        return result;
    }

    private float getLastRad() {
        float result = 0;
        String radius = mLocalInfo.getString(LAST_RAD, null);
        if (null != radius) {
            result = Float.parseFloat(radius);
        }
        return result;
    }

    private String getLastLocationAll() {
        String result = "";
        String locationAll = mLocalInfo.getString(LAST_LOCATION_ALL, null);
        if (null != locationAll) {
            result = locationAll;
        }
        return result;
    }

    private void innerLocCallback(ApiOpenLocationResult result) {
        if (result == null) {
            BDebug.w(TAG, "innerLocCallback ApiOpenLocationResult is null");
            return;
        }
        BDebug.i(TAG, "innerLocCallback lat:" + result.getLatitude() + " log:" + result.getLongitude() + " radius:" + result.getRadius());
        for (LocationCallback back : mCallbackList) {
            back.onLocation(result);
        }
    }

    private class MyLocationListener implements AMapLocationListener {

        public MyLocationListener() {
            ;
        }

        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            ApiOpenLocationResult result = null;
            double dLat = 0;
            double dLog = 0;
            float dRadius = 0;
            if (amapLocation != null) {
                if (amapLocation.getErrorCode() == 0) {
                    //定位成功回调信息，设置相关消息
                    amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                    dLat = amapLocation.getLatitude();//获取纬度
                    dLog = amapLocation.getLongitude();//获取经度
                    dRadius = amapLocation.getAccuracy();//获取精度信息
                    //缓存位置信息
                    saveLastLoc(dLat, dLog, dRadius);
                    // 处理逆地理编码信息
                    String address = amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                    String country = amapLocation.getCountry();//国家信息
                    String province = amapLocation.getProvince();//省信息
                    String city = amapLocation.getCity();//城市信息
                    String district = amapLocation.getDistrict();//城区信息
                    String street = amapLocation.getStreet();//街道信息
                    String streetNum = amapLocation.getStreetNum();//街道门牌号信息
                    String cityCode = amapLocation.getCityCode();//城市编码
                    String adCode = amapLocation.getAdCode();//地区编码
                    String aoiName = amapLocation.getAoiName();//获取当前定位点的AOI信息
                    result = new ApiOpenLocationResult(dLat, dLog, dRadius, address, country, province, city, district, street, streetNum, cityCode, adCode, aoiName);
                    String resultStr = DataHelper.gson.toJson(result);
                    saveLastLocAll(resultStr);

                    mLastLocationTime = System.currentTimeMillis();

                } else {
                    //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                    BDebug.e(TAG, "location Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                    result = new ApiOpenLocationResult();
                }
            }
            innerLocCallback(result);
        }
    }
}