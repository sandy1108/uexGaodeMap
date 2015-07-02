package org.zywx.wbpalmstar.plugin.uexgaodemap;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.InfoWindowAdapter;
import com.amap.api.maps.AMap.OnInfoWindowClickListener;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import org.zywx.wbpalmstar.base.cache.ImageLoadTask;
import org.zywx.wbpalmstar.base.cache.ImageLoadTask$ImageLoadTaskCallback;
import org.zywx.wbpalmstar.base.cache.ImageLoaderManager;
import org.zywx.wbpalmstar.plugin.uexgaodemap.bean.MarkerBean;
import org.zywx.wbpalmstar.plugin.uexgaodemap.util.GaodeUtils;
import org.zywx.wbpalmstar.plugin.uexgaodemap.util.OnCallBackListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class GaodeMapMarkerMgr extends GaodeMapBaseMgr implements OnMarkerClickListener, OnInfoWindowClickListener, InfoWindowAdapter {

    private static final String TAG = "GaodeMapMarkerMgr";
    private static HashMap<String, Marker> mMarkers = new HashMap<String, Marker>();
    private ImageLoaderManager manager;
    private List<LatLng> mOverlays;

    public GaodeMapMarkerMgr(Context cxt, AMap map,
                             OnCallBackListener listener, List<LatLng> markers) {
        super(cxt, map, listener);
        manager = ImageLoaderManager.initImageLoaderManager(mContext);
        this.mOverlays = markers;
    }

    public void addMarkers(List<MarkerBean> list){
        if (list == null || list.size() < 1){
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            final MarkerBean bean = list.get(i);
            final MarkerOptions option = new MarkerOptions();
            option.position(bean.getPosition());
            if(bean.isHasBubble()){
                if (!TextUtils.isEmpty(bean.getTitle())){
                    option.title(bean.getTitle());
                }
                if (!TextUtils.isEmpty(bean.getSubTitle())){
                    option.snippet(bean.getSubTitle());
                }
            }
            if (!TextUtils.isEmpty(bean.getIcon())){
                manager.asyncLoad(new ImageLoadTask(bean.getIcon()) {
                    @Override
                    protected Bitmap doInBackground() {
                        return GaodeUtils.getImage(mContext, filePath);
                    }
                }.addCallback(new ImageLoadTask$ImageLoadTaskCallback() {
                    public void onImageLoaded(ImageLoadTask task, Bitmap bitmap) {
                        if (bitmap != null) {
                            BitmapDescriptor bd = BitmapDescriptorFactory.fromBitmap(bitmap);
                            option.icon(bd);
                        }
                        addMark(bean.getId(), option);
                    }
                }));
            }else{
                addMark(bean.getId(), option);
            }
        }
    }

    private void addMark(String id, MarkerOptions option) {
        if (option != null && map != null){
            Marker marker = map.addMarker(option);
            mMarkers.put(id, marker);
            addToBoundsList(marker.getPosition());
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.i(TAG,"onMarkerClick->isShowBubble = " + marker.isInfoWindowShown());
        String id = getMarkerId(marker.getId());
        if (mListener != null){
            mListener.onMarkerClicked(id);
        }
        return false;
    }

    private String getMarkerId(String markerID){
        String id = null;
        Iterator it = mMarkers.keySet().iterator();
        while (it.hasNext()){
            final String key = it.next().toString();
            Marker marker = mMarkers.get(key);
            if (markerID.equals(marker.getId())){
                id = key;
                break;
            }
        }
        return id;
    }

    public void updateMarker(MarkerBean bean) {
        final Marker marker = mMarkers.get(bean.getId());
        final boolean isShowInfoWindow = marker.isInfoWindowShown();
        if (!TextUtils.isEmpty(bean.getIcon())){
            manager.asyncLoad(new ImageLoadTask(bean.getIcon()) {
                @Override
                protected Bitmap doInBackground() {
                    return GaodeUtils.getImage(mContext, filePath);
                }
            }.addCallback(new ImageLoadTask$ImageLoadTaskCallback() {
                public void onImageLoaded(ImageLoadTask task, Bitmap bitmap) {
                    if (bitmap != null) {
                        BitmapDescriptor bd = BitmapDescriptorFactory.fromBitmap(bitmap);
                        marker.setIcon(bd);
                    }
                }
            }));
        }
        if (bean.getPosition() != null){
            marker.setPosition(bean.getPosition());
        }
        if(bean.isHasBubble()){
            if (isShowInfoWindow){
                marker.hideInfoWindow();
            }
            if (!TextUtils.isEmpty(bean.getTitle())){
                marker.setTitle(bean.getTitle());
            }
            if (!TextUtils.isEmpty(bean.getSubTitle())){
                marker.setSnippet(bean.getSubTitle());
            }
            if (isShowInfoWindow){
                marker.showInfoWindow();
            }
        }
    }

    public void showBubble(String id){
        Marker marker = mMarkers.get(id);
        if (marker != null){
            marker.showInfoWindow();
        }
    }

    public void hideBubble(){
        Iterator it = mMarkers.keySet().iterator();
        while (it.hasNext()){
            final String key = it.next().toString();
            Marker marker = mMarkers.get(key);
            if (marker != null){
                marker.hideInfoWindow();
            }
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        String id = getMarkerId(marker.getId());
        if (mListener != null){
            mListener.onBubbleClicked(id);
        }
    }



    public void removeMarker(String id) {
        if (TextUtils.isEmpty(id)){
            removeAllMarker();
        }else{
            remove(id);
        }
    }

    private void remove(String id){
        Marker marker = mMarkers.get(id);
        if (marker != null){
            marker.remove();
        }
        removeMarkerFromList(id);
        removeFromBoundsList(marker.getPosition());
    }

    private void addToBoundsList(LatLng latLng){
        if (latLng != null && !mOverlays.contains(latLng)){
            mOverlays.add(latLng);
        }
    }

    private void removeFromBoundsList(LatLng latLng){
        if (latLng != null && mOverlays.contains(latLng)){
            mOverlays.remove(latLng);
        }
    }

    private void removeMarkerFromList(String id) {
        Iterator<String> iterator = mMarkers.keySet().iterator();
        while (iterator.hasNext()){
            final String item = iterator.next();
            if (id.equals(item)){
                iterator.remove();
            }
        }
    }

    private void removeAllMarker(){
        List<String> list = new ArrayList<String>();
        Iterator<String> iterator = mMarkers.keySet().iterator();
        while (iterator.hasNext()){
            String id = iterator.next();
            list.add(id);
        }
        for (int i = 0; i < list.size(); i ++){
            remove(list.get(i));
        }
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
