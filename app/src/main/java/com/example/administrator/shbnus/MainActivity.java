package com.example.administrator.shbnus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;
import com.baidu.mapapi.search.sug.SuggestionSearch;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public int accuracyCircleFillColor = 0xAAFFFF88;
    public int accuracyCircleStrokeColor = 0xAA00FF00;
    private TextureMapView mMapView;
    private BaiduMap mBaiduMap;
    public LocationClient mLocationClient = null;
    private MyLocationListener myListener = new MyLocationListener();
    private BitmapDescriptor bitmap;
    private float mCurrentX;
    private double mLatitude; // 记录最新经纬度
    private double mLongtitude;
    private MyOrientationListener mOrientationListener;
    private SuggestionSearch suggestionSearch;
    private PoiSearch poiSearch;
    private BitmapDescriptor descriptor;
    private View viwe;
    private TextView viewById;

    //BDAbstractLocationListener为7.2版本新增的Abstract类型的监听接口
     //原有BDLocationListener接口暂时同步保留。具体介绍请参考后文中的说明
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("TAG","aaaaa--");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            // 只要有一个权限没有被授予, 则直接返回 false
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_WIFI_STATE
                    },
                    1
            );
        }
        mMapView = (TextureMapView) findViewById(R.id.mTexturemap);
        mBaiduMap = mMapView.getMap();


        //-----------------------------------------------------------

        //热词搜索
        queryHotWord();
        final EditText content = findViewById(R.id.content);
        Button edit = findViewById(R.id.edit);

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = content.getText().toString();

             /*   poiSearch.searchInCity((new PoiCitySearchOption())
                        .city("北京")
                        .keyword(s)
                        .pageNum(10)
                );*/
                LatLng latLng = new LatLng(mLatitude, mLongtitude);
                //周边搜索
                poiSearch.searchNearby(new PoiNearbySearchOption()
                        .keyword(s)
                        .sortType(PoiSortType.distance_from_near_to_far)
                        .location(latLng)
                        .radius(10000)
                        .pageNum(10));
            }
        });


        //-----------------------------------------------------------
        bitmap = BitmapDescriptorFactory.fromResource(R.drawable.location);
       //初始化定位
        myLocad();
       //定位到我的位置
      centerToMyLocat();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        suggestionSearch.destroy();
    }
    @Override
    protected void onStart() {
        super.onStart();
        mOrientationListener.start();

    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
       mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

     //初始化定位
    public  void myLocad(){
        mLocationClient = new LocationClient(getApplicationContext());
        //声明LocationClient类
        mLocationClient.registerLocationListener(myListener);
        //注册监听函数
        LocationClientOption option = new LocationClientOption();
      //可选，设置定位模式，默认高精度
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
     //可选，设置返回经纬度坐标类型，默认gcj02
        option.setCoorType("bd09ll");
     //可选，设置发起定位请求的间隔，int类型，单位ms
       // option.setScanSpan(1000);
     //使用高精度和仅用设备两种定位模式的，参数必须设置为true  GPS 定位 和  网络定位
        option.setOpenGps(true);
     //可选，设置是否当GPS有效时按照1S/1次频率输出GPS结果，默认false
        option.setLocationNotify(true);
        //可选，定位SDK内部是一个service，并放到了独立进程。//设置是否在stop的时候杀死这个进程，默认（建议）不杀死，即setIgnoreKillProcess(true)
        option.setIgnoreKillProcess(false);
        //可选，设置是否收集Crash信息，默认收集，即参数为false
        option.SetIgnoreCacheException(false);
       //如果设置了该接口，首次启动定位时，会先判断当前WiFi是否超出有效期，若超出有效期，会先重新扫描WiFi，然后定位
        option.setWifiCacheTimeOut(5*60*1000);
        //可选，设置是否需要过滤GPS仿真结果，默认需要，即参数为false
        option.setEnableSimulateGps(false);

        //可选，是否需要地址信息，默认为不需要，即参数为false
        option.setIsNeedAddress(true);
        //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        mLocationClient.setLocOption(option);
        //启动定位
        mLocationClient.start();
         //初始化系统方向器类
        mOrientationListener = new MyOrientationListener(this);
        //获取方向传的纬度
        mOrientationListener
                .setmOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
                    public void onOrientationChanged(float x) {
                        mCurrentX = x;
                    }
                });
    }
   //  定位回调的监听
   class MyLocationListener extends BDAbstractLocationListener {
       @Override
       public void onReceiveLocation(BDLocation location) {
           // 更新位置
           mLatitude = location.getLatitude();
           mLongtitude = location.getLongitude();
           // 开启定位图层
           mBaiduMap.setMyLocationEnabled(true);

// 构造定位数据
           Toast.makeText(getApplication(), mCurrentX+"--", Toast.LENGTH_SHORT).show();
           MyLocationData locData = new MyLocationData.Builder()
                   .accuracy(location.getRadius())
                   // 此处设置开发者获取到的方向信息，顺时针0-360
                   .direction(mCurrentX).latitude(location.getLatitude())
                   .longitude(location.getLongitude()).build();

// 设置定位数据
           mBaiduMap.setMyLocationData(locData);
// 设置定位图层的配置（定位模式，是否允许方向信息，用户自定义定位图标）
           MyLocationConfiguration config = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.FOLLOWING, true, bitmap);
           mBaiduMap.setMyLocationConfiguration(config);
       }
   }

    // 定位到我的位置
    private void centerToMyLocat() {
        LatLng latLng = new LatLng(mLatitude, mLongtitude);
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.animateMapStatus(msu);
    }

//热词搜索
    public  void  queryHotWord(){
        poiSearch = PoiSearch.newInstance();
        poiSearch.setOnGetPoiSearchResultListener(poiListener);
    }
    List<OverlayOptions> options = new ArrayList<OverlayOptions>();

    OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener(){

        private LatLng location;

        @Override
        public void onGetPoiResult(PoiResult poiResult) {
            if (poiResult != null && poiResult.error == PoiResult.ERRORNO.NO_ERROR) {
                List<PoiInfo> allPoi = poiResult.getAllPoi();
                poiResult.getSuggestCityList();
                for (PoiInfo all: allPoi){
                    BeanInfo beanInfo = new BeanInfo(all.name);
                    descriptor = BitmapDescriptorFactory.fromView(viwe);
                    MarkerOptions option1 =  new MarkerOptions()
                            .position(all.location)
                            .title(all.name) // 这里title 不会在图上显示，实在Marker的点击事件中显示， marker中的点击事件的marker对象，这里参数不设置，是调不到信息的
                            .icon(bitmap) //把View对象转换成bitmap对象 显示文字与图片
                            ;
                    options.add(option1);

                    Marker  marker = (Marker) mBaiduMap.addOverlay(option1);
                    //使用marker携带info信息，当点击事件的时候可以通过marker获得info信息
                    Bundle bundle = new Bundle();
                    //info必须实现序列化接口
                   bundle.putSerializable("info",  beanInfo);
                    marker.setExtraInfo(bundle);
                    location = all.location;
                }
                //将地图显示在最后一个marker的位置
                MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(location);
                mBaiduMap.setMapStatus(msu);
                //List<Overlay> overlays = mBaiduMap.addOverlays(options);

                mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        Bundle extraInfo = marker.getExtraInfo();
                        BeanInfo info = (BeanInfo) extraInfo.getSerializable("info");
                        Toast.makeText(MainActivity.this,info.getName()+"--",Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });

            /*  for (PoiInfo all: allPoi){
                  Button button = new Button(getApplicationContext());
                  button.setBackgroundResource(R.drawable.ic_launcher);
                  button.setText(all.name);
                  InfoWindow mInfoWindow = new InfoWindow(button, all.location, -47);
                  mBaiduMap.showInfoWindow(mInfoWindow);
                }*/
//显示InfoWindow

            } else {
                Toast.makeText(getApplication(), "搜索不到你需要的信息！", Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

        }
    };
}
