package com.sososhopping.customer.account;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.sososhopping.customer.R;
import com.sososhopping.customer.account.textValidate.EmailWatcher;
import com.sososhopping.customer.account.textValidate.NameWatcher;
import com.sososhopping.customer.account.textValidate.PasswordDupWatcher;
import com.sososhopping.customer.account.textValidate.PasswordWatcher;
import com.sososhopping.customer.account.textValidate.PhoneWatcher;
import com.sososhopping.customer.databinding.AccountFindPasswordBinding;


public class FindPasswordFragment extends Fragment {
    private NavController navController;
    private AccountFindPasswordBinding binding;
    private Boolean phoneChecked = false;
    public static FindPasswordFragment newInstance() {
        return new FindPasswordFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.account_find_password, container, false);

        binding.editTextFindPassEmail
                .addTextChangedListener(new EmailWatcher(binding.textFieldFindPassEmail, getResources().getString(R.string.signup_error_email)));
        binding.editTextFindPassName
                .addTextChangedListener(new NameWatcher(binding.textFieldFindPassName, getResources().getString(R.string.signup_error_name)));
        binding.textFieldFindPassPhone.setCounterMaxLength(11);
        binding.editTextFindPassPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        binding.editTextFindPassPhone
                .addTextChangedListener(new PhoneWatcher(binding.textFieldFindPassPhone,getResources().getString(R.string.signup_error_phone)));

        binding.textFieldFindPassPassword.getEditText()
                .addTextChangedListener(new PasswordWatcher(binding.textFieldFindPassPassword, getResources().getString(R.string.signup_error_password)));
        binding.textFieldFindPassPasswordDup.getEditText()
                .addTextChangedListener(new PasswordDupWatcher(binding.textFieldFindPassPassword, binding.textFieldFindPassPasswordDup, getResources().getString(R.string.signup_error_passwordDup)));

        binding.textViewPhoneCheck.setVisibility(View.INVISIBLE);
        binding.textViewInfoCheck.setVisibility(View.INVISIBLE);
        binding.buttonPhoneCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.textFieldFindPassPhone.getError() != null) {
                    return;
                }
                //번호인증하기
                phoneChecked = true;

                //번호인증실패시
                if(!phoneChecked){
                    binding.textViewPhoneCheck.setText("인증실패");

                }
                else{
                    blockEditText(binding.editTextFindPassPhone);
                    binding.textViewPhoneCheck.setText("인증완료");
                }
                binding.textViewPhoneCheck.setVisibility(View.VISIBLE);

            }
        });

        binding.buttonInfoCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLayoutEmpty();

                //입력 검증
                if(!phoneChecked ||
                        binding.textFieldFindPassEmail.getError() != null ||
                        binding.textFieldFindPassName.getError() != null ||
                        binding.textFieldFindPassPhone.getError() != null){
                    Toast.makeText(getContext(),getResources().getString(R.string.signup_error_process), Toast.LENGTH_SHORT).show();
                    return;
                }

                //이메일 + 성함 + 휴대전화번호 -> 가입여부 (비밀번호 재설정)
                if(false){
                    binding.textViewInfoCheck.setText(getResources().getString(R.string.find_infoFail));
                }
                else{
                    blockEditText(binding.editTextFindPassName);
                    blockEditText(binding.editTextFindPassEmail);
                    binding.buttonInfoCheck.setClickable(false);
                    binding.textViewInfoCheck.setText(getResources().getString(R.string.find_infoOk));
                }
                binding.textViewInfoCheck.setVisibility(View.VISIBLE);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        binding.buttonToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLayoutPassEmpty();

                if(binding.textFieldFindPassPassword.getError() != null ||
                binding.textFieldFindPassPasswordDup.getError() != null){
                    return;
                }

                //API로 변경된 이메일 + 비밀번호 전송 -> 변경되는지 확인

                //이후 이동
                navController.navigate(R.id.action_findPasswordFragment_to_signUpStartFragment);
            }
        });
    }


    public void blockEditText(EditText editText){
        editText.setFocusable(false);
        editText.setClickable(false);
    }

    public void checkLayoutEmpty(){
        if(TextUtils.isEmpty(binding.editTextFindPassEmail.getText().toString())){
            binding.textFieldFindPassEmail.setError(getResources().getString(R.string.signup_error_email));
        }
        if(TextUtils.isEmpty(binding.editTextFindPassName.getText().toString())){
            binding.textFieldFindPassName.setError(getResources().getString(R.string.signup_error_name));
        }
        if(TextUtils.isEmpty(binding.editTextFindPassPhone.getText().toString())){
            binding.textFieldFindPassPhone.setError(getResources().getString(R.string.signup_error_phone));
        }
    }

    public void checkLayoutPassEmpty(){
        if(TextUtils.isEmpty(binding.editTextFindPassPassword.getText().toString())){
            binding.textFieldFindPassPassword.setError(getResources().getString(R.string.signup_error_phone));
        }
        if(TextUtils.isEmpty(binding.editTextFindPassPasswordDup.getText().toString())){
            binding.textFieldFindPassPasswordDup.setError(getResources().getString(R.string.signup_error_nickname));
        }
    }
}