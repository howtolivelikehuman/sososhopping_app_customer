package com.sososhopping.customer.search.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapOptions;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.MarkerIcons;
import com.sososhopping.customer.HomeActivity;
import com.sososhopping.customer.R;
import com.sososhopping.customer.common.StringFormatMethod;
import com.sososhopping.customer.common.types.Location;
import com.sososhopping.customer.databinding.SearchShopMapBinding;
import com.sososhopping.customer.search.HomeViewModel;
import com.sososhopping.customer.search.dto.PageableShopListDto;
import com.sososhopping.customer.search.model.ShopInfoShortModel;
import com.sososhopping.customer.shop.view.ShopMainFragment;

import java.util.ArrayList;
import java.util.concurrent.Executor;

public class ShopMapFragment extends Fragment implements OnMapReadyCallback {
    private NavController navController;
    private SearchShopMapBinding binding;
    private HomeViewModel homeViewModel;
    private int navigateFrom;

    //네이버 지도 GPS
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private MapFragment mapFragment;
    private NaverMap naverMap;

    MutableLiveData<ShopInfoShortModel> focusedShop;
    ArrayList<Marker> markers = new ArrayList<>();

    public static ShopMainFragment newInstance() {return new ShopMainFragment();}

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        homeViewModel = new ViewModelProvider(getActivity()).get(HomeViewModel.class);

        //현재위치
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        FragmentManager fm = getChildFragmentManager();
        mapFragment = (MapFragment)fm.findFragmentById(R.id.map);

        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this::onMapReady);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        super.onCreateOptionsMenu(menu,inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_top_search, menu);
        menu.findItem(R.id.menu_list).setVisible(true);
        menu.findItem(R.id.menu_map).setVisible(false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        binding = SearchShopMapBinding.inflate(inflater,container,false);

        focusedShop = new MutableLiveData<>();
        focusedShop.observe(this, new Observer<ShopInfoShortModel>() {
            @Override
            public void onChanged(ShopInfoShortModel shopInfoShortModel) {
                if(shopInfoShortModel == null){
                    binding.itemSearchMap.getRoot().setVisibility(View.GONE);
                }else{
                    bindShopItem(shopInfoShortModel);
                }
            }
        });

        homeViewModel.getShopList().observe(this, new Observer<ArrayList<ShopInfoShortModel>>() {
            @Override
            public void onChanged(ArrayList<ShopInfoShortModel> shopInfoShortModels) {
                if(naverMap != null){
                    //마커 재설정
                    addMarkers(naverMap);
                }
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        binding.buttonPager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(homeViewModel.getNumberOfElement() > 0){
                    homeViewModel.search(
                            ((HomeActivity)getActivity()).getLoginToken(),
                            homeViewModel.getLocation(getContext()),
                            null,
                            null,
                            null,
                            ShopMapFragment.this::onSearchSuccess,
                            ShopMapFragment.this::onNetworkError);
                }
            }
        });
    }


    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        navigateFrom = ShopMapFragmentArgs.fromBundle(getArguments()).getNavigateFrom();
        this.naverMap = naverMap;

        //트래킹 모드On
        naverMap.setLocationSource(locationSource);
        if(!locationSource.isActivated()){
            naverMap.setLocationTrackingMode(LocationTrackingMode.None);
        }
        else{
            naverMap.setLocationTrackingMode(LocationTrackingMode.NoFollow);
        }

        Location location =  homeViewModel.getLocation(getContext());
        LatLng currentLocation = new LatLng(
                location.getLat(),
                location.getLng()
        );

        //위치 설정
        LocationOverlay locationOverlay = naverMap.getLocationOverlay();
        locationOverlay.setPosition(currentLocation);
        locationOverlay.setVisible(true);


        //버튼 클릭시 이동
        binding.buttonGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Location location =  homeViewModel.getLocation(getContext());
                LatLng currentLocation = new LatLng(
                        location.getLat(),
                        location.getLng()
                );

                //좌표설정
                naverMap.moveCamera(CameraUpdate.scrollTo(currentLocation));
                naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        });

        //줌 + 한국으로 제한
        naverMap.setMinZoom(5.0);
        naverMap.setMaxZoom(18.0);
        naverMap.setExtent(new LatLngBounds(new LatLng(31.43, 122.37),
                new LatLng(44.35, 132)));

        //초기 카메라 위치
        switch (navigateFrom){
            //홈
            //리스트에서 옴 -> 검색내용 토대로 구성
            //검색결과로 구성
            case R.id.home2 :
            case R.id.shopListFragment:
            case R.id.searchDialogFragment:
            default:
                naverMap.moveCamera(CameraUpdate.scrollTo(currentLocation));
                break;

            //상점에서 옴
            case R.id.mysosoPointDetailFragment:
            case R.id.shopMainFragment:
                double shopLat = ShopMapFragmentArgs.fromBundle(getArguments()).getLat();
                double shopLng = ShopMapFragmentArgs.fromBundle(getArguments()).getLng();

                //상점으로 이동
                naverMap.moveCamera(CameraUpdate.scrollTo(new LatLng(shopLat, shopLng)));
                break;
        }
        naverMap.moveCamera(CameraUpdate.zoomTo(16));

        //TODO: 클릭시 INFO 추가
        naverMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                focusedShop.postValue(null);
            }
        });

        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(false);
        uiSettings.setCompassEnabled(false);

        //마커 추가
        addMarkers(naverMap);
    }

    @Override
    public void onResume() {
        //앱바 설정
        ((HomeActivity) getActivity()).hideBottomNavigation();
        ((HomeActivity) getActivity()).showTopAppBar();
        setAppBar(homeViewModel);
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void addMarkers(NaverMap naverMap){

        //기존 마커 해제
        int size = markers.size();
        for(int i=0; i<size; i++){
            markers.get(0).setMap(null);
            markers.remove(0);
        }

        //백그라운드 스레드로 마커 생성
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                new Thread(command).start();
            }
        };

        executor.execute(new Runnable() {
            @Override
            public void run() {
                createMarkers(0);
            }
        });

        //메인에서 마커 추가
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
           for(Marker m : markers){
               m.setMap(naverMap);
           }
        });
    }

    //추가 된 경우
    public void addMarkersPage(NaverMap naverMap, int index){
        //백그라운드 스레드로 마커 생성
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                new Thread(command).start();
            }
        };

        executor.execute(new Runnable() {
            @Override
            public void run() {
                createMarkers(index);
            }
        });

        //메인에서 마커 추가
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            for(Marker m : markers){
                m.setMap(naverMap);
            }
        });
    }

    public void createMarkers(int startIdx){
        for(int i= startIdx; i<homeViewModel.getShopList().getValue().size(); i++){
            ShopInfoShortModel s = homeViewModel.getShopList().getValue().get(i);

            Marker m = new Marker();
            m.setPosition(new LatLng(s.getLocation().getLat(), s.getLocation().getLng()));
            m.setIcon(MarkerIcons.GREEN);
            m.setWidth(72);
            m.setHeight(108);
            m.setTag(i);

            m.setOnClickListener(new Overlay.OnClickListener() {
                @Override
                public boolean onClick(@NonNull Overlay overlay) {
                    Marker marker = (Marker) overlay;
                    focusedShop.postValue(homeViewModel.getShopList().getValue().get((Integer)marker.getTag()));
                    return true;
                }
            });
            markers.add(m);
        }
        Log.e("생성 후 마커 (1)", markers.size()+"");
    }


    public void bindShopItem(ShopInfoShortModel s){
        binding.itemSearchMap.textViewShopName.setText(s.getName());

        //특징설정
        if(!s.isLocalCurrencyStatus()){
            binding.itemSearchMap.layoutLocalPay.setVisibility(View.GONE);
        }
        if(!s.isDeliveryStatus()){
            binding.itemSearchMap.layoutDelivery.setVisibility(View.GONE);
        }

        //별점설정
        binding.itemSearchMap.textViewRating.setText(StringFormatMethod.getRating(s.getScore()));
        binding.itemSearchMap.buttonMove.setText("매장 둘러보기 (" + StringFormatMethod.getDistance(s.getDistance())+")");


        binding.itemSearchMap.buttonMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //이동
                navController.navigate(ShopMapFragmentDirections.actionShopMapFragmentToShopGraph(s.getStoreId()));
            }
        });

        binding.itemSearchMap.buttonShopChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //채팅
            }
        });

        binding.itemSearchMap.buttonShopCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(s.getPhone() == null){
                    Snackbar.make(binding.getRoot(), "등록된 전화번호가 없습니다", Snackbar.LENGTH_SHORT).show();
                }
                else{
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+ s.getPhone())));
                }
            }
        });
        binding.itemSearchMap.getRoot().setVisibility(View.VISIBLE);
    }
    public void setAppBar(HomeViewModel homeViewModel){
        HomeActivity activity = (HomeActivity) getActivity();
        activity.getBinding().topAppBar.setTitle(homeViewModel.getSearchContent().getValue());
        activity.getBinding().topAppBar.setTitleCentered(false);
        activity.getBinding().topAppBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //검색 dialog 띄우기
                navController.navigate(ShopMapFragmentDirections.actionShopMapFragmentToSearchDialogFragment(R.id.shopMapFragment));
                return;
            }
        });
        activity.getBinding().topAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.menu_search :{

                        //검색 dialog 띄우기
                        navController.navigate(ShopMapFragmentDirections.actionShopMapFragmentToSearchDialogFragment(R.id.shopMapFragment));
                        break;
                    }
                    case R.id.menu_map:{
                        //지도로 navigate
                        break;
                    }

                    case R.id.menu_list:{
                        navController.navigate(ShopMapFragmentDirections.actionShopMapFragmentToShopListFragment());
                        break;
                    }
                }
                return false;
            }
        });
        activity.invalidateOptionsMenu();
    }

    private void onSearchSuccess(PageableShopListDto success, Integer navigate){
        if(success.getNumberOfElements() > 0){
            homeViewModel.getShopList().getValue().addAll(success.getContent());
            //추가된거 밑에
            addMarkersPage(naverMap, homeViewModel.getOffset());
        }

        //몇개 추가되었는지 갱신
        homeViewModel.setNumberOfElement(success.getNumberOfElements());
        homeViewModel.setOffset(success.getPageable().getOffset() + success.getNumberOfElements());
    }

    private void onNetworkError(){
        Snackbar.make(binding.getRoot(), "상점 정보를 더 불러오는데 실패했습니다", Snackbar.LENGTH_SHORT).show();
    }
}