package org.zywx.wbpalmstar.plugin.uexgaodemap;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.amap.api.maps.AMapException;
import com.amap.api.maps.offlinemap.OfflineMapCity;
import com.amap.api.maps.offlinemap.OfflineMapManager;
import com.amap.api.maps.offlinemap.OfflineMapProvince;
import com.amap.api.maps.offlinemap.OfflineMapStatus;

import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.engine.DataHelper;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.uexgaodemap.vo.AvailableCityVO;
import org.zywx.wbpalmstar.plugin.uexgaodemap.vo.AvailableProvinceVO;
import org.zywx.wbpalmstar.plugin.uexgaodemap.vo.DownloadItemVO;
import org.zywx.wbpalmstar.plugin.uexgaodemap.vo.DownloadResultVO;
import org.zywx.wbpalmstar.plugin.uexgaodemap.vo.DownloadStatusVO;
import org.zywx.wbpalmstar.plugin.uexgaodemap.vo.UpdateResultVO;
import org.zywx.wbpalmstar.plugin.uexgaodemap.util.OnCallBackListener;
import org.zywx.wbpalmstar.plugin.uexgaodemap.util.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GaodeMapOfflineManager implements OfflineMapManager.OfflineMapDownloadListener{
    private static final String TAG = "OfflineManager";
    private Context mContext;
    private OnCallBackListener mListener;
    private OfflineMapManager offlineMapManager;
    private static GaodeMapOfflineManager object;
    private List<DownloadItemVO> mDownloadList;
    private List<DownloadItemVO> mDownloadingList;
    private List<DownloadItemVO> mAvailableList;
    private boolean isDownloading = false;
    private DownloadHandler mHandler;
    private String mDownloadingName = "";

    private HashMap<String,Integer> mUpdateCallbackMap=new HashMap<String,Integer>();
    //为了兼容cb回调,记录检查更新与回调的对应关系,同一个城市不能同时检查两次。

    public static GaodeMapOfflineManager getInstance(Context context, OnCallBackListener listener){
        if (object == null){
            object = new GaodeMapOfflineManager(context, listener);
        }
        return object;
    }

    public GaodeMapOfflineManager(Context mContext, OnCallBackListener mListener) {
        this.mContext = mContext;
        this.mListener = mListener;
        this.mDownloadList = new ArrayList<DownloadItemVO>();
        this.mDownloadingList = new ArrayList<DownloadItemVO>();
        this.mAvailableList = new ArrayList<DownloadItemVO>();
        this.mHandler = new DownloadHandler(Looper.getMainLooper());
        getAvailableList();
        mDownloadingList = SharedPreferencesUtils.getLocalDownloadingList(mContext);
    }

    private void getAvailableList() {
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            if (mAvailableList != null){
                mAvailableList.clear();
            }
            List<OfflineMapProvince> all = offlineMapManager.getOfflineMapProvinceList();
            for (int i = 0; i < all.size(); i++){
                OfflineMapProvince item = all.get(i);
                mAvailableList.add(new DownloadItemVO(item));
                List<OfflineMapCity> cityList = item.getCityList();
                if (cityList != null && cityList.size() > 0){
                    for (int j =0; j < cityList.size(); j++){
                        OfflineMapCity city = cityList.get(j);
                        if (!isInAvailable(city.getCity())){
                            mAvailableList.add(new DownloadItemVO(city));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(mContext, "获取离线地图列表失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private boolean isInAvailable(String city) {
        for (int i = 0; i < mAvailableList.size(); i++){
            if (city.equals(mAvailableList.get(i).getName())){
                return true;
            }
        }
        return false;
    }

    public void download(DownloadItemVO downloadVO, int callbackId, boolean isLast) {
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            DownloadResultVO data = new DownloadResultVO();
            data.setName(downloadVO.getName());
            if(isDownloading(downloadVO.getName()) && !isDownloadingError(downloadVO.getName())){
                data.setErrorCode(JsConst.ERROR_IS_EXIST);
                data.setErrorStr(EUExUtil.getString("plugin_uexgaodemap_is_exist"));
            }else if(isDownload(downloadVO.getName())){
                data.setErrorCode(JsConst.ERROR_IS_DOWNLOAD);
                data.setErrorStr(EUExUtil.getString("plugin_uexgaodemap_is_download"));
            }else if(!isValidCityName(downloadVO.getName())){
                data.setErrorCode(JsConst.ERROR_WRONG_CITY_NAME);
                data.setErrorStr(EUExUtil.getString("plugin_uexgaodemap_wrong_city_name"));
            }else {
                if (isDownloading(downloadVO.getName())){
                    updateDownloadStatus(downloadVO.getName(), OfflineMapStatus.LOADING);
                }else {
                    mDownloadingList.add(downloadVO);
                }

                SharedPreferencesUtils.saveLocalDownloadingList(mContext, mDownloadingList);
                data.setErrorCode(JsConst.SUCCESS);
                if (!isDownloading){
                    sendStartDownloadMsg();
                }
            }
            if (mListener != null){
                mListener.cbDownload(data,callbackId,isLast);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isDownloadingError(String name) {
        boolean result = false;
        if (mDownloadingList != null){
            for (int i = 0; i < mDownloadingList.size(); i++){
                if (name.equals(mDownloadingList.get(i).getName())){
                    if (mDownloadingList.get(i).getStatus() == OfflineMapStatus.ERROR){
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    private boolean isValidCityName(String name) {
        boolean isValid;
        String allCity = DataHelper.gson.toJson(mAvailableList);
        if (TextUtils.isEmpty(allCity)){
            isValid = true;
        }else {
            isValid = allCity.contains(name);
        }
        return isValid;
    }

    @Override
    public void onDownload(int status, int completeCode, String name) {
        DownloadStatusVO statusVO = new DownloadStatusVO(status,completeCode,name);
        if (mListener != null){
            mListener.onDownload(statusVO);
        }
        switch (status){
            case OfflineMapStatus.ERROR:
                sendDownloadErrorMsg(name);
                break;
            case OfflineMapStatus.SUCCESS:
                sendDownloadSuccessMsg(name);
                break;
            case OfflineMapStatus.LOADING:
            case OfflineMapStatus.CHECKUPDATES:
            case OfflineMapStatus.PAUSE:
            case OfflineMapStatus.STOP:
            case OfflineMapStatus.UNZIP:
            case OfflineMapStatus.WAITING:
                break;
            default:
                break;
        }
    }

    @Override
    public void onRemove(boolean success, String name, String describe) {

    }

    public void deleteList(List<String> list, int callbackId) {
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            if (list == null){
                if (isDownloading){
                    offlineMapManager.stop();
                }
                List<String> allList = new ArrayList<String>();
                if (mDownloadingList != null){
                    for (int i = 0; i < mDownloadingList.size(); i++){
                        allList.add(mDownloadingList.get(i).getName());
                    }
                }
                if (mDownloadList != null){
                    for (int i = 0; i < mDownloadList.size(); i++){
                        allList.add(mDownloadList.get(i).getName());
                    }
                }
                list = allList;
            }
            if (list.size() > 0){
                for (int i = 0; i < list.size(); i++){
                    delete(list.get(i),callbackId,i==(list.size()-1));
                }
            }
        } catch (Exception e) {
            // AMap异常处理
            e.printStackTrace();
        }
    }

    private void delete(String city, int callbackId, boolean isLast){
        DownloadResultVO data = new DownloadResultVO();
        data.setName(city);
        boolean isDelete;
        if (isDownloading(city)){
            removeFromDownloadingList(city);
            if (isDownloading && mDownloadingName.equals(city)){
                offlineMapManager.stop();
                sendStartDownloadMsg();
            }
            isDelete = true;
        }else if (isDownload(city)){
            isDelete = true;
        }else{
            isDelete = false;
        }
        if (isDelete){
            offlineMapManager.remove(city);
            data.setErrorCode(JsConst.SUCCESS);
        }else{
            data.setErrorCode(JsConst.FAILED);
            data.setErrorStr(EUExUtil.getString("plugin_uexgaodemap_not_download"));
            getDownload();
        }
        if (mListener != null){
            mListener.cbDelete(data,callbackId,isLast);
        }
    }

    public void getDownloadCityList(final List<DownloadItemVO> resultList) {
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            List<OfflineMapCity> list = offlineMapManager.getDownloadOfflineMapCityList();
            if (list != null){
                for (int i = 0; i < list.size(); i++){
                    OfflineMapCity city = list.get(i);
                    DownloadItemVO item = new DownloadItemVO(city);
                    resultList.add(item);
                }
            }
        } catch (Exception e) {
            // AMap异常处理
            e.printStackTrace();
        }
    }

    public void getDownloadProvinceList(final List<DownloadItemVO> resultList){
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            List<OfflineMapProvince> list = offlineMapManager
                    .getDownloadOfflineMapProvinceList();
            if (list != null){
                for (int i = 0; i < list.size(); i++){
                    OfflineMapProvince data = list.get(i);
                    if (!isDownloadExist(resultList, data.getProvinceName())){
                        DownloadItemVO item = new DownloadItemVO(data);
                        resultList.add(item);
                    }
                }
            }
        } catch (Exception e) {
            // AMap异常处理
            e.printStackTrace();
        }
    }

    private boolean isDownloadExist(List<DownloadItemVO> resultList, String name) {
        boolean result = false;
        for (int i = 0; i < resultList.size(); i++){
            String itemName = resultList.get(i).getName();
            if (itemName.equals(name)){
                result = true;
            }
        }
        return result;
    }

    public void getDownloadingCityList(final List<DownloadItemVO> resultList) {
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            List<OfflineMapCity> list = offlineMapManager.getDownloadingCityList();
            if (list != null){
                for (int i = 0; i < list.size(); i++){
                    OfflineMapCity city = list.get(i);
                    DownloadItemVO item = new DownloadItemVO(city);
                    resultList.add(item);
                }
            }
        } catch (Exception e) {
            // AMap异常处理
            e.printStackTrace();
        }
    }

    public void getDownloadingProvinceList(final List<DownloadItemVO> resultList){
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            List<OfflineMapProvince> list = offlineMapManager
                    .getDownloadingProvinceList();
            if (list != null){
                for (int i = 0; i < list.size(); i++){
                    OfflineMapProvince city = list.get(i);
                    if (!isDownloadExist(resultList, city.getProvinceName())){
                        DownloadItemVO item = new DownloadItemVO(city);
                        resultList.add(item);
                    }
                }
            }
        } catch (Exception e) {
            // AMap异常处理
            e.printStackTrace();
        }
    }

    public void getAvailableCityList(final int callbackId) {
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<AvailableCityVO> resultList = new ArrayList<AvailableCityVO>();
                    try {
                        List<OfflineMapCity> list = offlineMapManager.getOfflineMapCityList();
                        if (list != null){
                            for (int i = 0; i < list.size(); i++){
                                OfflineMapCity city = list.get(i);
                                AvailableCityVO item = new AvailableCityVO(city);
                                resultList.add(item);
                            }
                        }
                        if (mListener != null){
                            mListener.cbGetAvailableCityList(resultList,callbackId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            // AMap异常处理
            e.printStackTrace();
        }
    }

    public void restart(String city) {
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            updateDownloadStatus(city, OfflineMapStatus.LOADING);
            sendStartDownloadMsg();
            //offlineMapManager.restart();
        } catch (Exception e) {
            // AMap异常处理
            e.printStackTrace();
        }
    }

    public void pause(String city) {
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            updateDownloadStatus(city, OfflineMapStatus.PAUSE);
            if (city.equals(mDownloadingName)){
                isDownloading = false;
                offlineMapManager.pause();
            }
            sendStartDownloadMsg();
        } catch (Exception e) {
            // AMap异常处理
            e.printStackTrace();
        }
    }

    public void stop(String city) {
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            offlineMapManager.stop();
        } catch (Exception e) {
            // AMap异常处理
            e.printStackTrace();
        }
    }

    public void getAvailableProvinceList(final int callbackId) {
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<AvailableProvinceVO> resultList = new ArrayList<AvailableProvinceVO>();
                    try {
                        List<OfflineMapProvince> list = offlineMapManager.getOfflineMapProvinceList();
                        if (list != null){
                            for (int i = 0; i < list.size(); i++){
                                OfflineMapProvince province = list.get(i);
                                AvailableProvinceVO item = new AvailableProvinceVO(province);
                                resultList.add(item);
                            }
                        }
                        if (mListener != null){
                            mListener.cbGetAvailableProvinceList(resultList,callbackId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            // AMap异常处理
            e.printStackTrace();
        }
    }

    public List<DownloadItemVO> getDownloadList(int callbackId) {
        List<DownloadItemVO> list = getDownload();
        if (mListener != null){
            mListener.cbGetDownloadList(list,callbackId);
        }
        return list;
    }

    private List<DownloadItemVO> getDownload(){
        mDownloadList.clear();
        List<DownloadItemVO> resultList = new ArrayList<DownloadItemVO>();
        getDownloadCityList(resultList);
        getDownloadProvinceList(resultList);
        if (resultList.size() > 0){
            for (int i = 0; i < resultList.size(); i++){
                DownloadItemVO item = new DownloadItemVO();
                item.setName(resultList.get(i).getName());
                item.setType(resultList.get(i).getType());
                item.setStatus(resultList.get(i).getStatus());
                mDownloadList.add(item);
            }
        }
        return  resultList;
    }

    public List<DownloadItemVO> getDownloadingList(int callbackId) {
        List<DownloadItemVO> list = getDownloading();
        if (mListener != null){
            mListener.cbGetDownloadingList(list,callbackId);
        }
        if (!isDownloading){
            sendStartDownloadMsg();
        }
        return list;
    }

    private List<DownloadItemVO> getDownloading(){
        List<DownloadItemVO> resultList = new ArrayList<DownloadItemVO>();
        //getDownloadingCityList(resultList);
        //getDownloadingProvinceList(resultList);
        if (mDownloadingList != null){
            for (int i = 0; i < mDownloadingList.size(); i++){
                boolean isExist = false;
                String name = mDownloadingList.get(i).getName();
//                for (int j = 0; j < resultList.size(); j++){
//                    String name1 = resultList.get(j).getName();
//                    if (name.equals(name1)){
//                        isExist = true;
//                    }
//                }
                getAvailableList();
                for (int j = 0; j < mAvailableList.size(); j++){
                    final DownloadItemVO item = mAvailableList.get(j);
                    if (name.equals(item.getName())){
                        item.setStatus(mDownloadingList.get(i).getStatus());
                        resultList.add(item);
                    }
                }
            }
        }
        SharedPreferencesUtils.saveLocalDownloadingList(mContext,resultList);
        sendStartDownloadMsg();
        return resultList;
    }

    private boolean isDownloading(String name){
        boolean result = false;
        if (mDownloadingList!= null){
            for (int i = 0; i < mDownloadingList.size(); i++){
                if (name.equals(mDownloadingList.get(i).getName())){
                    result = true;
                }
            }
        }
        return result;
    }

    public boolean isUpdate(DownloadItemVO data, int callbackId) {
        if (mUpdateCallbackMap.containsKey(data.getName())){
            return false;
        }
        mUpdateCallbackMap.put(data.getName(),callbackId);
        isNeedUpdate(data);
        return true;
    }

    @Override
    public void onCheckUpdate(boolean hasNew, String name) {
        BDebug.i("onCheckUpdate:",name,hasNew);
        UpdateResultVO resultVO = new UpdateResultVO(name, hasNew ? 0:1);
        if (mListener != null){
            mListener.cbIsUpdate(resultVO,mUpdateCallbackMap.remove(name));
        }
    }

    private void isNeedUpdate(DownloadItemVO data){
        try {
            if (offlineMapManager == null){
                offlineMapManager = new OfflineMapManager(mContext, this);
            }
            try {
                offlineMapManager.updateOfflineCityByName(data.getName());
            } catch (AMapException e) {
                if (BDebug.DEBUG) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            // AMap异常处理
            e.printStackTrace();
        }
    }

    public void restart(List<String> list) {
        if (list != null && list.size() > 0){
            for (int i = 0; i < list.size(); i++){
                if (!TextUtils.isEmpty(list.get(i))){
                    restart(list.get(i));
                }
            }
        }
    }

    public void pause(List<String> list) {
        if (list != null && list.size() > 0){
            for (int i = 0; i < list.size(); i++){
                if (!TextUtils.isEmpty(list.get(i))){
                    pause(list.get(i));
                }
            }
        }
    }

    private class DownloadHandler extends Handler{

        public DownloadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg == null) return;
            switch (msg.what){
                case JsConst.MSG_DOWNLOAD_INIT_START:
                    if (mDownloadingList == null || mDownloadingList.size() == 0){
                        isDownloading = false;
                        return;
                    }
                    for (int i = 0; i < mDownloadingList.size(); i++){
                        DownloadItemVO data = mDownloadingList.get(i);
                        if (data.getStatus() == OfflineMapStatus.LOADING){
                            startDownloading(data);
                        }
                    }
                    break;
                case JsConst.MSG_DOWNLOAD_ERROR:
                    String errorName = (String) msg.obj;
                    updateDownloadStatus(errorName, OfflineMapStatus.ERROR);
                    sendStartDownloadMsg();
                    break;
                case JsConst.MSG_DOWNLOAD_SUCCESS:
                    String successName = (String) msg.obj;
                    removeFromDownloadingList(successName);
                    getDownload();
                    sendStartDownloadMsg();
                    break;
            }
        }
    }

    private void removeFromDownloadingList(String name) {
        DownloadItemVO downloadItemVO = getItemFromDownloadingList(name);
        if (downloadItemVO != null){
            mDownloadingList.remove(downloadItemVO);
            SharedPreferencesUtils.saveLocalDownloadingList(mContext, mDownloadingList);
        }
    }

    private DownloadItemVO getItemFromDownloadingList(String name) {
        for (int i = 0; i < mDownloadingList.size(); i++){
            if (name.equals(mDownloadingList.get(i).getName())){
                return mDownloadingList.get(i);
            }
        }
        return null;
    }

    private void updateDownloadStatus(String name, int status) {
        DownloadItemVO downloadItemVO = getItemFromDownloadingList(name);
        if (downloadItemVO != null) {
            downloadItemVO.setStatus(status);
            SharedPreferencesUtils.saveLocalDownloadingList(mContext, mDownloadingList);
        }
    }

    private void startDownloading(DownloadItemVO downloadVO) {
        boolean isStart;
        try {
            switch (downloadVO.getType()){
                case JsConst.TYPE_CITY:
                     offlineMapManager.downloadByCityName(downloadVO.getName());
                    break;
                case JsConst.TYPE_PROVINCE:
                    offlineMapManager.downloadByProvinceName(downloadVO.getName());
                    break;
                default:
                    isStart = false;
                    break;
            }
        } catch (AMapException e) {
            isStart = false;
            e.printStackTrace();
        }
        isDownloading = true;
        mDownloadingName = downloadVO.getName();
    }

    private void sendStartDownloadMsg(){
        Message msg = new Message();
        msg.what = JsConst.MSG_DOWNLOAD_INIT_START;
        mHandler.sendMessage(msg);
    }

    private void sendDownloadSuccessMsg(String name) {
        Message msg = new Message();
        msg.what = JsConst.MSG_DOWNLOAD_SUCCESS;
        msg.obj = name;
        mHandler.sendMessage(msg);
    }

    private void sendDownloadErrorMsg(String name) {
        Message msg = new Message();
        msg.what = JsConst.MSG_DOWNLOAD_ERROR;
        msg.obj = name;
        mHandler.sendMessage(msg);
    }

    private boolean isDownload(String name){
        boolean result = false;
        if (mDownloadList != null){
            for (int i = 0; i < mDownloadList.size(); i++){
                if (name.equals(mDownloadList.get(i).getName())){
                    result = true;
                }
            }
        }
        return result;
    }
}
