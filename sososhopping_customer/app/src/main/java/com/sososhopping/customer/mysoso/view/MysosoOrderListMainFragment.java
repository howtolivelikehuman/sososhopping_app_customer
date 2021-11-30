package com.sososhopping.customer.mysoso.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.sososhopping.customer.MainActivity;
import com.sososhopping.customer.NavGraphDirections;
import com.sososhopping.customer.R;
import com.sososhopping.customer.common.types.enumType.OrderStatus;
import com.sososhopping.customer.databinding.MysosoOrderListBinding;
import com.sososhopping.customer.mysoso.dto.OrderListDto;
import com.sososhopping.customer.mysoso.model.OrderRecordShortModel;
import com.sososhopping.customer.mysoso.viemodel.OrderListViewModel;
import com.sososhopping.customer.mysoso.view.adapter.MysosoOrderListAdapter;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class MysosoOrderListMainFragment extends Fragment {

    private MysosoOrderListBinding binding;
    private NavController navController;
    private OrderListViewModel orderListViewModel;

    MysosoOrderListAdapter orderListAdapter;

    private final OrderStatus INITIAL_ORDERSTAT = OrderStatus.PENDING;
    private final OrderStatus[] searchTypeApprove = {OrderStatus.APPROVE_ALL, OrderStatus.APPROVE, OrderStatus.READY};
    private final OrderStatus[] searchTypeCancel = {OrderStatus.CANCEL_ALL, OrderStatus.REJECT, OrderStatus.CANCEL};

    int beforeSpinnerClick = -1;

    public static MysosoOrderListMainFragment newInstance() {return new MysosoOrderListMainFragment();}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //메뉴 변경 확인
        setHasOptionsMenu(true);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        super.onCreateOptionsMenu(menu,inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_top_none, menu);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        //binding 설정
        binding = MysosoOrderListBinding.inflate(inflater, container, false);

        //viewmodel 설정
        orderListViewModel = new ViewModelProvider(this).get(OrderListViewModel.class);
        orderListAdapter = new MysosoOrderListAdapter(orderListViewModel.getOrderList().getValue());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(orderListAdapter);

        orderListViewModel.getOrderList().observe(this, new Observer<ArrayList<OrderRecordShortModel>>() {
            @Override
            public void onChanged(ArrayList<OrderRecordShortModel> orderRecordShortModels) {
                orderListAdapter.setItems(orderRecordShortModels);
                orderListAdapter.notifyDataSetChanged();
            }
        });

        //재검색
        orderListViewModel.requestMyOrderLists(
                ((MainActivity)getActivity()).getLoginToken(),
                INITIAL_ORDERSTAT,
                MysosoOrderListMainFragment.this::onSuccess,
                MysosoOrderListMainFragment.this::onFailedLogIn,
                MysosoOrderListMainFragment.this::onFailed,
                MysosoOrderListMainFragment.this::onNetworkError
        );
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        //상단바
        ((MainActivity)getActivity()).showTopAppBar();
        ((MainActivity)getActivity()).setTopAppBarTitle("주문내역");
        //하단바
        ((MainActivity)getActivity()).hideBottomNavigation();
        super.onResume();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        binding.spinnerSearchType.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                binding.textViewSearchType.setText(adapterView.getSelectedItem().toString());

                if(beforeSpinnerClick == i){
                    //재검색
                    getListCall((OrderStatus) adapterView.getSelectedItem());
                }
                beforeSpinnerClick = i;
            }
        });

        binding.taplayoutPurchaseStatus.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()){
                    case 0:{
                        getListCall(OrderStatus.PENDING);
                        binding.linearLayoutSpinner.setVisibility(View.GONE);
                        break;
                    }
                    case 1:{
                        //스피너 설정 필요
                        ArrayAdapter<OrderStatus> adapter = new ArrayAdapter<OrderStatus>(getContext(), android.R.layout.simple_spinner_item, searchTypeApprove);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        binding.spinnerSearchType.setAdapter(adapter);

                        //초기 설정
                        beforeSpinnerClick = 0;
                        binding.spinnerSearchType.setSelection(0);
                        binding.linearLayoutSpinner.setVisibility(View.VISIBLE);
                        break;
                    }
                    case 2:{
                        //스피너 설정 필요
                        ArrayAdapter<OrderStatus> adapter = new ArrayAdapter<OrderStatus>(getContext(), android.R.layout.simple_spinner_item, searchTypeCancel);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        binding.spinnerSearchType.setAdapter(adapter);

                        //초기 설정
                        beforeSpinnerClick = 0;
                        binding.spinnerSearchType.setSelection(0);
                        binding.linearLayoutSpinner.setVisibility(View.VISIBLE);
                        break;
                    }
                    case 3:{
                        getListCall(OrderStatus.DONE);
                        binding.linearLayoutSpinner.setVisibility(View.GONE);
                        break;
                    }
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                switch (tab.getPosition()){
                    case 0:{
                        //새로고침되게
                        getListCall(OrderStatus.PENDING);
                        break;
                    }
                    case 1:
                    case 2:
                        binding.spinnerSearchType.setSelection(0);
                        break;
                    case 3:{
                        getListCall(OrderStatus.DONE);
                        break;
                    }
                }
            }
        });
        orderListAdapter.setOnItemClickListener(new MysosoOrderListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(long orderId) {
                //세부정보로 이동
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void getListCall(OrderStatus orderStatus){
        //재검색
        orderListViewModel.requestMyOrderLists(
                ((MainActivity)getActivity()).getLoginToken(),
                orderStatus,
                MysosoOrderListMainFragment.this::onSuccess,
                MysosoOrderListMainFragment.this::onFailedLogIn,
                MysosoOrderListMainFragment.this::onFailed,
                MysosoOrderListMainFragment.this::onNetworkError
        );
    }

    //추후에 페이징시 연결하는 메소드 추가해야함
    public void onSuccess(OrderListDto orderListDto){
        orderListViewModel.getOrderList().getValue().clear();
        orderListViewModel.getOrderList().setValue(orderListDto.getResults());
    }

    private void onFailedLogIn(){
        NavHostFragment.findNavController(this)
                .navigate(NavGraphDirections.actionGlobalLogInRequiredDialog().setErrorMsgId(R.string.login_error_token));
    }

    private void onFailed() {
        Toast.makeText(getContext(),getResources().getString(R.string.mysoso_myRating_delte_error), Toast.LENGTH_LONG).show();
    }

    private void onNetworkError() {
        NavHostFragment.findNavController(this).navigate(R.id.action_global_networkErrorDialog);
        getActivity().onBackPressed();
    }


}
