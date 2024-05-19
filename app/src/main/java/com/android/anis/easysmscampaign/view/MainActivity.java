package com.android.anis.easysmscampaign.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.android.anis.easysmscampaign.R;
import com.android.anis.easysmscampaign.common.Constants;
import com.android.anis.easysmscampaign.common.ExcelUtils;
import com.android.anis.easysmscampaign.data.ContactResponse;
import com.android.anis.easysmscampaign.data.response.BooleanResponse;
import com.android.anis.easysmscampaign.data.response.DataResponse;
import com.android.anis.easysmscampaign.data.response.StateDefinition;
import com.android.anis.easysmscampaign.databinding.ActivityMainBinding;
import com.android.anis.easysmscampaign.contract.IMainActivityContract;
import com.android.anis.easysmscampaign.view.adapter.ContactsAdapter;
import com.android.anis.easysmscampaign.viewModel.MainActivityViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created By: Md. Anisuzzaman on 15/04/2021
 */
public class MainActivity extends AppCompatActivity implements IMainActivityContract.View {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_DOC = 1;

    private ActivityMainBinding mBinding;
    private MainActivityViewModel mViewModel;

    private HandlerThread importContactsHandlerThread;
    private Handler contactsHandler;
    private HandlerThread generateExcelHandlerThread;

    private HandlerThread readExcelDataHandlerThread;


    private Button importContactsButton;

    private FloatingActionButton shareButton;
    private RecyclerView contactsRecyclerView;
    private ConstraintLayout constraintLayout;
    private LottieAnimationView lottieAnimationView;
    private LottieAnimationView importLottieView;
    private LottieAnimationView exportLottieView;
    private LottieAnimationView readLottieView;
    private EditText smsText;
    private RadioGroup simRadioGroup;

    private final String NO_DATA_ANIMATION = "no_data.json";
    private final String LOADING_ANIMATION = "loading.json";
    private final String ERROR_ANIMATION = "error.json";
    private final String DONE_ANIMATION = "done.json";
    private final String CANCEL_ANIMATION = "cancel.json";

    private final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS
    };

    private List<ContactResponse> contactsList;
    private List<ContactResponse> importedExcelContactsList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        contactsList = new ArrayList<>();
        importedExcelContactsList = new ArrayList<>();

        mViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);

        initializeViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean isPermissionGranted = checkPermissionsAtRuntime();

        if (isPermissionGranted) {
            importContactsButton.setOnClickListener(view -> onImportContactButtonClicked());
        }

        shareButton.setOnClickListener(view -> onSendSMSButtonClicked());
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void initializeViews() {
        Log.e(TAG, "initializeViews: ");
        importContactsButton = mBinding.importContactButton;
        smsText = mBinding.editDescription;
        simRadioGroup = mBinding.simRadioGroup;
        shareButton = mBinding.shareExcelFloatingButton;
        contactsRecyclerView = mBinding.displayContactsRecyclerView;
        constraintLayout = mBinding.constraintLayout;
        lottieAnimationView = mBinding.lottieAnimationView;
        importLottieView = mBinding.importContactLottie;


        setupLottieAnimation(lottieAnimationView, NO_DATA_ANIMATION);
    }

    @Override
    public void setupLottieAnimation(LottieAnimationView lottieView, String animationName) {
        if (lottieView.isAnimating()) {
            lottieView.cancelAnimation();
        }
        lottieView.setAnimation(animationName);
        lottieView.playAnimation();
    }


    @Override
    public void onImportContactButtonClicked() {
        Log.e(TAG, "onImportContactButtonClicked: ");
        //contactsHandler.post(importContactsRunnable);
        browseDocument();
    }


    @Override
    public void onSendSMSButtonClicked() {
        Log.e(TAG, "onSendSMSButtonClicked: ");

        if (smsText == null || smsText.getText().toString().matches("")) {
            displaySnackBar("SMS text is empty");
        } else if (importedExcelContactsList.isEmpty()) {
            displaySnackBar("No recipients");
        } else {
            sendSMS(smsText.getText().toString());
        }
    }

    @Override
    public void switchVisibility(View view, int visibility) {
        view.setVisibility(visibility);
    }

    @Override
    public void enableUIComponent(View componentName) {
        componentName.setClickable(true);
        componentName.setAlpha(1);
    }

    @Override
    public void disableUIComponent(View componentName) {
        componentName.setClickable(false);
        componentName.setAlpha((float) 0.4);
    }

    @Override
    public void setupRecyclerView() {
        Log.e(TAG, "setupRecyclerView: ");

        switchVisibility(lottieAnimationView, View.GONE);
        switchVisibility(contactsRecyclerView, View.VISIBLE);

        ContactsAdapter mAdapter = new ContactsAdapter(importedExcelContactsList);
        contactsRecyclerView.setHasFixedSize(true);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactsRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void displaySnackBar(String message) {
        Snackbar.make(constraintLayout, message, BaseTransientBottomBar.LENGTH_SHORT)
                .show();
    }

    @Override
    public boolean checkPermissionsAtRuntime() {
        Log.e(TAG, "checkPermissionsAtRuntime: ");
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void requestPermissions() {
        Log.e(TAG, "requestPermissions: ");
        ActivityCompat.requestPermissions(this, PERMISSIONS, Constants.REQUEST_PERMISSION_ALL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean isAlertDialogInflated = false;
        boolean isUIDisabled = false;

        if (requestCode == Constants.REQUEST_PERMISSION_ALL) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    boolean showRationale = shouldShowRequestPermissionRationale(permission);
                    if (!showRationale) {
                        // Called when user selects 'NEVER ASK AGAIN'
                        isAlertDialogInflated = true;

                    } else {
                        // Called when user selects 'DENY'
                        displaySnackBar("Enable all permissions");
                        isUIDisabled = true;

                        disableUIComponent(importContactsButton);

                    }
                } else if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // Called when user selects 'ALLOW'
                    if (!isUIDisabled) {
                        enableUIComponent(importContactsButton);
                    }

                }
            }

            inflateAlertDialog(isAlertDialogInflated);
        }

    }

    /**
     * Method: Show Alert Dialog when User denies permission permanently
     */
    private void inflateAlertDialog(boolean isTrue) {
        if (isTrue) {
            // Inflate Alert Dialog
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Permissions Mandatory")
                    .setMessage("Kindly enable all permissions through Settings")
                    .setPositiveButton("OKAY", (dialogInterface, i) -> {
                        launchAppSettings();
                        dialogInterface.dismiss();
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    /**
     * Method: Launch App-Settings Screen
     */
    private void launchAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, Constants.REQUEST_PERMISSION_SETTING);
    }

    /**
     * Methods: File open copy read excel
     */

    private void sendSMS(String messageText) {
        int simIndex = 0;
        int selectedId = simRadioGroup.getCheckedRadioButtonId();
        // find the radiobutton by returned id
        RadioButton selectedRadioButton = findViewById(selectedId);

        if(selectedRadioButton.getText() == "SIM 2")
        {
            simIndex = 1;
        }

        final ArrayList<Integer> simCardList = new ArrayList<>();
        SubscriptionManager subscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            checkPermissionsAtRuntime();
            return;
        }
        final List<SubscriptionInfo> subscriptionInfoList = subscriptionManager
                .getActiveSubscriptionInfoList();
        for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
            int subscriptionId = subscriptionInfo.getSubscriptionId();
            simCardList.add(subscriptionId);
        }

        if(!simCardList.isEmpty())
        {

            int smsToSendFrom = simCardList.get(simIndex); //assign your desired sim to send sms, or user selected choice

            for(int i=0;i< importedExcelContactsList.size(); i++) {
                String phoneNumber = importedExcelContactsList.get(i).getPhoneNumberList().get(0).getNumber();

                if(messageText.length() > 160)
                {
                    ArrayList<String> parts = SmsManager.getSmsManagerForSubscriptionId(smsToSendFrom).divideMessage(messageText);
                    SmsManager.getSmsManagerForSubscriptionId(smsToSendFrom)
                            .sendMultipartTextMessage(phoneNumber, null, parts, null, null);
                }
                else {

                    SmsManager.getSmsManagerForSubscriptionId(smsToSendFrom)
                            .sendTextMessage(phoneNumber, null, messageText, null, null);
                }

            }
        }
        else
        {
            SmsManager smsManager = SmsManager.getDefault();
            for(int i=0;i< importedExcelContactsList.size(); i++) {
                String phoneNumber = importedExcelContactsList.get(i).getPhoneNumberList().get(0).getNumber();
                if(messageText.length()>160)
                {
                    ArrayList<String> parts = smsManager.divideMessage(messageText);
                    smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
                }
                else {
                    smsManager.sendTextMessage(phoneNumber, null, messageText, null, null);
                }
            }
        }


        displaySnackBar("Sending SMS Completed");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DOC && resultCode == Activity.RESULT_OK) {
            Log.e(TAG, "onActivityResult success: ");
            Uri uri = data.getData();
            String filePath = getPath(uri);

            assert uri != null;
            String filename = "";
            int cut = filePath.lastIndexOf('/');
            if (cut != -1) {
                filename = filePath.substring(cut + 1);
            }

            // File Input Stream gets me file data
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                File file = new File(this.getExternalFilesDir(null), filename);
                copyInputStreamToFile(inputStream,file);
                if(filename.toLowerCase().contains(".xlsx"))
                {

                    importedExcelContactsList = ExcelUtils.getExcelDataFromFileXLSX(file);
                }
                else if(filename.toLowerCase().contains(".xls")) {

                    importedExcelContactsList = ExcelUtils.getExcelDataFromFile(file);
                }
                else {
                    //displaySnackBar("Not a valid excel file["+filename+"].");
                    importedExcelContactsList = ExcelUtils.getExcelDataFromFileXLSX(file);
                }
                Log.e(TAG,"Your Contact Count:"+importedExcelContactsList.size());

                //setupLottieAnimation(readLottieView, DONE_ANIMATION);
                setupRecyclerView();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    // Copy an InputStream to a File.
//
    private void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                if ( out != null ) {
                    out.close();
                }

                // If you want to close the "in" InputStream yourself then remove this
                // from here but ensure that you close it yourself eventually.
                in.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    private void browseDocument(){


        String[] mimeTypes =
                {
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        // .xls & .xlsx
                };

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes);
        startActivityForResult(Intent.createChooser(intent,"Choose File"), REQUEST_CODE_DOC);

    }
    public String getPath(Uri uri) {

        String path = null;
        String[] projection = { MediaStore.Files.FileColumns.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if(cursor == null){
            path = uri.getPath();
        }
        else{
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            path = cursor.getString(column_index);
            cursor.close();
        }

        return ((path == null || path.isEmpty()) ? (uri.getPath()) : path);
    }
    private void copy(File source, File destination) {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(destination).getChannel();
        }
        catch (FileNotFoundException  fileNotFoundException)
        {
            Toast.makeText(this,"File Not Found Exception",Toast.LENGTH_LONG).show();
        }


        try {
            if(in != null) {
                in.transferTo(0, in.size(), out);
            }
        } catch(Exception exception){
            Toast.makeText(this,"An IO Exception occurred",Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
            }
            catch (IOException ioException)
            {
                Toast.makeText(this,"IO Exception",Toast.LENGTH_LONG).show();
            }

        }
    }
}




