package com.android.anis.easysmscampaign.viewModel;

import android.app.Application;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.anis.easysmscampaign.common.Constants;
import com.android.anis.easysmscampaign.common.ExcelUtils;
import com.android.anis.easysmscampaign.contract.IMainActivityContract;
import com.android.anis.easysmscampaign.data.ContactResponse;
import com.android.anis.easysmscampaign.data.response.BooleanResponse;
import com.android.anis.easysmscampaign.data.response.DataResponse;
import com.android.anis.easysmscampaign.data.response.ErrorData;
import com.android.anis.easysmscampaign.data.response.StateDefinition;

import java.util.ArrayList;
import java.util.List;

public class MainActivityViewModel extends AndroidViewModel
        implements IMainActivityContract.ViewModel {
    private static final String TAG = MainActivityViewModel.class.getSimpleName();

    private final List<ContactResponse> contactResponseList;
    private List<ContactResponse> parsedExcelDataList;


    // Constructor
    public MainActivityViewModel(@NonNull Application application) {
        super(application);

        contactResponseList = new ArrayList<>();
        parsedExcelDataList = new ArrayList<>();

    }

}
