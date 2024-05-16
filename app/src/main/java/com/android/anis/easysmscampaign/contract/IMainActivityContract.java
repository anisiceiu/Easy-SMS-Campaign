package com.android.anis.easysmscampaign.contract;

import com.airbnb.lottie.LottieAnimationView;

/**
 * Created by: Md. Anisuzzaman on 15/04/24.
 */
public interface IMainActivityContract {

    // View
    interface View {
        void initializeViews();
        void setupLottieAnimation(LottieAnimationView animationView, String animationName);
        void onImportContactButtonClicked();
        void onSendSMSButtonClicked();
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

    }
}
