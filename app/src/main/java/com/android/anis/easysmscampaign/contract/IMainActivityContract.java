package com.android.anis.easysmscampaign.contract;

import android.net.Uri;

import com.airbnb.lottie.LottieAnimationView;
import com.android.anis.easysmscampaign.data.ContactResponse;

import java.util.List;

/**
 * Contract to be implemented by MainActivity (View) and MainActivityViewModel (ViewModel)
 *
 * Created by: Md. Anisuzzaman on 15/04/21.
 */
public interface IMainActivityContract {

    // View
    interface View {
        void initializeViews();
        void setupLottieAnimation(LottieAnimationView animationView, String animationName);
        void setupHandlerThreads();
        void destroyHandlerThreads();
        void onImportContactButtonClicked();
        void onShareButtonClicked();
        void switchVisibility(android.view.View view, int visibility);
        void enableUIComponent(android.view.View componentName);
        void disableUIComponent(android.view.View componentName);
        void setupRecyclerView();
        void displaySnackBar(String message);
        boolean checkPermissionsAtRuntime();
        void requestPermissions();
    }

    // View-Model
    interface ViewModel {
        void initiateImport();
        void initiateExport(List<ContactResponse> dataList);
        void initiateRead();
        Uri initiateSharing();
    }
}
