package com.example.testoauth.ui.login;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testoauth.R;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.truecaller.android.sdk.common.TrueException;
import com.truecaller.android.sdk.common.VerificationCallback;
import com.truecaller.android.sdk.common.VerificationDataBundle;
import com.truecaller.android.sdk.common.callVerification.RequestPermissionHandler;
import com.truecaller.android.sdk.common.models.TrueProfile;
import com.truecaller.android.sdk.oAuth.CodeVerifierUtil;
import com.truecaller.android.sdk.oAuth.TcOAuthCallback;
import com.truecaller.android.sdk.oAuth.TcOAuthData;
import com.truecaller.android.sdk.oAuth.TcOAuthError;
import com.truecaller.android.sdk.oAuth.TcSdk;
import com.truecaller.android.sdk.oAuth.TcSdkOptions;

import org.shadow.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";

    //constants for layouts
    private static final int LANDING_LAYOUT  = 1;
    private static final int PROFILE_LAYOUT  = 2;
    private static final int LOADER_LAYOUT   = 3;
    private static final int FORM_LAYOUT     = 4;
    private static final int SETTINGS_LAYOUT = 5;

    private RadioGroup titleSelector;
    private RadioGroup additionalFooterSelector;
    private int        verificationCallbackType;
    private Spinner    ctaTextSpinner, prefixSpinner;
    private Spinner colorSpinner, colorTextSpinner;
    private AppCompatTextView timerTextViewMissedCall;
    private MaterialCheckBox  phoneCheckbox, profileCheckbox, openIdCheckbox, offlineAccessCheckbox;
    private CountDownTimer           timer;
    private String                   state;
    private RequestPermissionHandler permissionHandler;

    private String codeVerifier;

    private final TcOAuthCallback sdkCallback = new TcOAuthCallback() {
        @Override
        public void onSuccess(@NonNull final TcOAuthData oAuthData) {
            Log.i(TAG, "\ncode: " + oAuthData.getAuthorizationCode() + "\nverifier: " + codeVerifier);
            Toast.makeText(SignInActivity.this,
                    "onSuccess : state = " + oAuthData.getState(),
                    Toast.LENGTH_SHORT).show();
            showLayout(LANDING_LAYOUT);
            Intent intent = new Intent(SignInActivity.this, SignedInActivity.class);
            intent.putExtra("data", oAuthData);
            intent.putExtra("state", state);
            intent.putExtra("cv", codeVerifier);
            startActivity(intent);
        }

        @Override
        public void onFailure(@NonNull final TcOAuthError oAuthError) {
            Toast.makeText(SignInActivity.this, "onFailure: " + oAuthError.getErrorCode() +
                    ": " + oAuthError.getErrorMessage(), Toast.LENGTH_SHORT).show();
            showLayout(LANDING_LAYOUT);
        }

        @Override
        public void onVerificationRequired(@Nullable final TcOAuthError tcOAuthError) {
            if (tcOAuthError != null) {
                Toast.makeText(SignInActivity.this,
                        "Verification Required : " + tcOAuthError.getErrorMessage(),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SignInActivity.this,
                        "Verification Required",
                        Toast.LENGTH_SHORT).show();
            }
            showLayout(FORM_LAYOUT);
            findViewById(R.id.btnProceed).setOnClickListener(proceedClickListener);
        }
    };

    private final VerificationCallback apiCallback = new VerificationCallback() {

        @Override
        public void onRequestSuccess(final int requestCode, @Nullable VerificationDataBundle bundle) {
            if (requestCode == VerificationCallback.TYPE_MISSED_CALL_INITIATED) {
                verificationCallbackType = VerificationCallback.TYPE_MISSED_CALL_INITIATED;
                String ttl = bundle.getString(VerificationDataBundle.KEY_TTL);
                if (ttl != null) {
                    Toast.makeText(SignInActivity.this,
                            "Missed call initiated with TTL : " + ttl,
                            Toast.LENGTH_SHORT).show();
                    Toast.makeText(SignInActivity.this,
                            "Req Nonce : " + bundle.getString(VerificationDataBundle.KEY_REQUEST_NONCE),
                            Toast.LENGTH_SHORT).show();
                    showCountDownTimer(Double.parseDouble(ttl) * 1000);
                }
                showLoader("Waiting for call");
            } else if (requestCode == VerificationCallback.TYPE_MISSED_CALL_RECEIVED) {
                Toast.makeText(SignInActivity.this,
                        "Missed call received",
                        Toast.LENGTH_SHORT).show();
                showLayout(PROFILE_LAYOUT);
                findViewById(R.id.btnVerify).setOnClickListener(verifyClickListener);
            } else if (requestCode == VerificationCallback.TYPE_PROFILE_VERIFIED_BEFORE) {
                Toast.makeText(SignInActivity.this,
                        "Profile verified for your app before: " + bundle.getProfile().firstName
                                + " and access token: " + bundle.getProfile().accessToken,
                        Toast.LENGTH_SHORT).show();
                Toast.makeText(SignInActivity.this,
                        "Req Nonce : " + bundle.getProfile().requestNonce,
                        Toast.LENGTH_SHORT).show();
                showLayout(LANDING_LAYOUT);
                onSuccessfullManualVerification(bundle.getProfile().firstName);
            } else if (requestCode == VerificationCallback.TYPE_VERIFICATION_COMPLETE) {
                dismissCountDownTimer();
                Toast.makeText(SignInActivity.this,
                        "Success: Verified with " + bundle.getString(VerificationDataBundle.KEY_ACCESS_TOKEN),
                        Toast.LENGTH_SHORT).show();
                Toast.makeText(SignInActivity.this,
                        "Req Nonce : " + bundle.getString(VerificationDataBundle.KEY_REQUEST_NONCE),
                        Toast.LENGTH_SHORT).show();
                showLayout(LANDING_LAYOUT);
                onSuccessfullManualVerification(((EditText) findViewById(R.id.edtFirstName)).getText().toString());
            }
        }

        @Override
        public void onRequestFailure(final int requestCode, @NonNull final TrueException e) {
            Toast.makeText(
                            SignInActivity.this,
                            "OnFailureApiCallback: " + e.getExceptionType() + "\n" + e.getExceptionMessage(),
                            Toast.LENGTH_SHORT)
                    .show();
            showLayout(FORM_LAYOUT);
        }
    };

    private void onSuccessfullManualVerification(String name) {
        Intent intent = new Intent(this, SignedInSuccessfulActivity.class);
        intent.putExtra("name", name);
        startActivity(intent);
    }

    //**********Click listeners  *************//
    private final View.OnClickListener verifyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            final String firstName = ((EditText) findViewById(R.id.edtFirstName)).getText().toString();
            final String lastName = ((EditText) findViewById(R.id.edtLastName)).getText().toString();
            final TrueProfile profile = new TrueProfile.Builder(firstName, lastName).build();

            TcSdk.getInstance().verifyMissedCall(profile, apiCallback);
        }
    };

    private final View.OnClickListener startClickListener = view -> {
        try {
            if (TcSdk.getInstance().isOAuthFlowUsable()) {
                EditText localeEt = findViewById(R.id.localeEt);
                String locale = null;
                if (!TextUtils.isEmpty(localeEt.getText())) {
                    locale = localeEt.getText().toString();
                }
                if (!StringUtils.isEmpty(locale)) {
                    TcSdk.getInstance().setLocale(new Locale(locale));
                }
                codeVerifier = CodeVerifierUtil.Companion.generateRandomCodeVerifier();
                String codeChallenge = CodeVerifierUtil.Companion.getCodeChallenge(codeVerifier);
                if (codeChallenge != null) {
                    TcSdk.getInstance().setCodeChallenge(codeChallenge);
                } else {
                    Toast.makeText(this, "code challenge is required", Toast.LENGTH_SHORT).show();
                }
                state = UUID.randomUUID().toString();
                TcSdk.getInstance().setOAuthState(state);
                TcSdk.getInstance().setOAuthScopes(getRequestedScopes());
                TcSdk.getInstance().getAuthorizationCode(SignInActivity.this);
            } else {
                Toast.makeText(this, "OAuth flow not usable", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // If you receive an exception "Please call init() on TcSdk first", it means either you haven't initialized the SDK
            // or you have but since it has been initialized in a background thread, so you need to wait. In ideal scenario this shouldn't happen
            // since you would be initializing the SDK well in advance before calling its methods.
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };

    private String[] getRequestedScopes() {
        List<String> scopes = new ArrayList<>();
        if (phoneCheckbox.isChecked()) {
            scopes.add(phoneCheckbox.getTag().toString());
        }
        if (profileCheckbox.isChecked()) {
            scopes.add(profileCheckbox.getTag().toString());
        }
        if (openIdCheckbox.isChecked()) {
            scopes.add(openIdCheckbox.getTag().toString());
        }
        if (offlineAccessCheckbox.isChecked()) {
            scopes.add(offlineAccessCheckbox.getTag().toString());
        }
        return scopes.toArray(new String[0]);
    }

    private final View.OnClickListener proceedClickListener = view -> checkAndRequestPermissions();
    private       EditText             mPhoneField;

    @SuppressLint("NewApi")
    private final View.OnClickListener btnGoClickListner = v -> {
        initTruecallerSDK();
        showLayout(LANDING_LAYOUT);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mPhoneField = findViewById(R.id.edtPhone);

        findViewById(R.id.btnStart).setOnClickListener(startClickListener);
        findViewById(R.id.buttonGo).setOnClickListener(btnGoClickListner);
        findViewById(R.id.buttonGo).setBackgroundColor(ContextCompat.getColor(this, R.color.white));
        titleSelector = findViewById(R.id.sdkTitleOptions);
        additionalFooterSelector = findViewById(R.id.additionalFooters);

        colorSpinner = findViewById(R.id.color_spinner);
        colorTextSpinner = findViewById(R.id.color_text_spinner);
        ctaTextSpinner = findViewById(R.id.cta_prefix_spinner);
        prefixSpinner = findViewById(R.id.prefix_spinner);

        findViewById(R.id.scopes_layout).setVisibility(View.VISIBLE);
        phoneCheckbox = findViewById(R.id.phone_scope);
        profileCheckbox = findViewById(R.id.profile_scope);
        openIdCheckbox = findViewById(R.id.openId_scope);
        offlineAccessCheckbox = findViewById(R.id.offline_access_scope);

        timerTextViewMissedCall = findViewById(R.id.timerTextProgress);
        setSpinnerAdapters();

        //        initTruecallerSDK();
        showLayout(SETTINGS_LAYOUT);
    }

    private void setSpinnerAdapters() {
        ArrayAdapter<CharSequence> adapterP =
                ArrayAdapter.createFromResource(this,
                        R.array.SdkPartnerLoginPrefixOptionsArray,
                        android.R.layout.simple_spinner_item);
        adapterP.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prefixSpinner.setAdapter(adapterP);

        ArrayAdapter<CharSequence> adapterCP =
                ArrayAdapter.createFromResource(this,
                        R.array.SdkPartnerCTAOptionsArray,
                        android.R.layout.simple_spinner_item);
        adapterCP.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ctaTextSpinner.setAdapter(adapterCP);

        ArrayAdapter<CharSequence> adapterColor =
                ArrayAdapter.createFromResource(this,
                        R.array.SdkPartnerSampleColors,
                        android.R.layout.simple_spinner_item);
        adapterColor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(adapterColor);
        colorTextSpinner.setAdapter(adapterColor);

        prefixSpinner.setSelection(1);
        ctaTextSpinner.setSelection(0);
        colorSpinner.setSelection(0);
        colorTextSpinner.setSelection(1);
    }

    private void initTruecallerSDK() {
        // clear any existing instance before initializing a new one
        TcSdk.clear();
        TcSdkOptions trueScope = new TcSdkOptions.Builder(this, sdkCallback)
                .buttonColor(Color.parseColor(colorSpinner.getSelectedItem().toString())) //default TC blue
                .buttonTextColor(Color.parseColor(colorTextSpinner.getSelectedItem().toString())) //default white
                .loginTextPrefix(prefixSpinner.getSelectedItemPosition()) //default 0
                .ctaText(ctaTextSpinner.getSelectedItemPosition()) //default 0
                .buttonShapeOptions(((SwitchCompat) findViewById(R.id.shapeOptions)).isChecked() ?
                        TcSdkOptions.BUTTON_SHAPE_RECTANGLE
                        : TcSdkOptions.BUTTON_SHAPE_ROUNDED) //default ROUNDED
                .footerType(additionalFooterSelector.getCheckedRadioButtonId() == ListView.INVALID_POSITION
                        ? TcSdkOptions.FOOTER_TYPE_SKIP
                        : resolveAdditionalFooter(additionalFooterSelector.getCheckedRadioButtonId()))
                .consentTitleOption(titleSelector.getCheckedRadioButtonId() == ListView.INVALID_POSITION
                        ? TcSdkOptions.SDK_CONSENT_HEADING_LOG_IN_TO
                        : resolveSelectedPosition(titleSelector.getCheckedRadioButtonId()))
                .sdkOptions(((SwitchCompat) findViewById(R.id.sdkOptions)).isChecked() ?
                        TcSdkOptions.OPTION_VERIFY_ALL_USERS :
                        TcSdkOptions.OPTION_VERIFY_ONLY_TC_USERS)
                .build();

        new Thread(() -> TcSdk.init(trueScope)).start();
    }

    private int resolveAdditionalFooter(final int checkedRadioButtonId) {
        if (checkedRadioButtonId == R.id.uan) {
            return TcSdkOptions.FOOTER_TYPE_ANOTHER_MOBILE_NO;
        } else if (checkedRadioButtonId == R.id.uam) {
            return TcSdkOptions.FOOTER_TYPE_ANOTHER_METHOD;
        } else if (checkedRadioButtonId == R.id.edm) {
            return TcSdkOptions.FOOTER_TYPE_MANUALLY;
        } else if (checkedRadioButtonId == R.id.idl) {
            return TcSdkOptions.FOOTER_TYPE_LATER;
        }
        return TcSdkOptions.FOOTER_TYPE_SKIP;
    }

    private int resolveSelectedPosition(final int checkedRadioButtonId) {
        int pos;
        if (checkedRadioButtonId == R.id.zero) {
            pos = 0;
        } else if (checkedRadioButtonId == R.id.one) {
            pos = 1;
        } else if (checkedRadioButtonId == R.id.two) {
            pos = 2;
        } else if (checkedRadioButtonId == R.id.three) {
            pos = 3;
        } else if (checkedRadioButtonId == R.id.four) {
            pos = 4;
        } else if (checkedRadioButtonId == R.id.five) {
            pos = 5;
        } else {
            pos = 0;
        }
        return pos;
    }

    public void requestVerification() {
        final String phone = mPhoneField.getText().toString().trim();
        if (!TextUtils.isEmpty(phone)) {
            showLoader("Trying to verify your number...");
            try {
                TcSdk.getInstance().requestVerification("IN", phone, apiCallback, this);
            } catch (RuntimeException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkAndRequestPermissions() {
        permissionHandler = new RequestPermissionHandler(this, new RequestPermissionHandler.Listener() {
            @Override
            public boolean onShowSettingRationale(@NonNull final Set<String> set) {
                return false;
            }

            @Override
            public boolean onShowPermissionRationale(@NonNull final Set<String> set) {
                new AlertDialog.Builder(SignInActivity.this)
                        .setMessage("For verifying your number, we need Calls and Phone permission")
                        .setCancelable(false)
                        .setPositiveButton("OK", (dialogInterface, i) -> permissionHandler.retryRequestDeniedPermission())
                        .setNegativeButton("Cancel", (dialogInterface, i) -> {
                            permissionHandler.cancel();
                            dialogInterface.dismiss();
                        })
                        .show();
                return true;
            }

            @Override
            public void onComplete(@NonNull final Set<String> grantedPermissions, @NonNull final Set<String> deniedPermissions) {
                if (deniedPermissions.isEmpty()) {
                    requestVerification();
                } else {
                    Toast.makeText(SignInActivity.this, "Cannot proceed ahead unless permissions are granted", Toast.LENGTH_SHORT).show();
                }
            }
        });
        permissionHandler.requestPermission();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            TcSdk.getInstance().onActivityResultObtained(this, requestCode, resultCode, data);
        } catch (RuntimeException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void showLoader(String message) {
        showLayout(LOADER_LAYOUT);
        ((TextView) findViewById(R.id.txtLoader)).setText(message);
    }

    private void showCountDownTimer(Double ttl) {
        if (verificationCallbackType == VerificationCallback.TYPE_MISSED_CALL_INITIATED) {
            timerTextViewMissedCall.setVisibility(View.VISIBLE);
        }
        timer = new CountDownTimer(ttl.longValue(), 1000) {
            @Override
            public void onTick(final long millisUntilFinished) {
                if (verificationCallbackType == VerificationCallback.TYPE_MISSED_CALL_INITIATED) {
                    timerTextViewMissedCall.setPaintFlags(timerTextViewMissedCall.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
                    timerTextViewMissedCall.setText(String.format(getString(R.string.retry_timer), millisUntilFinished / 1000));
                }
            }

            @Override
            public void onFinish() {
                if (verificationCallbackType == VerificationCallback.TYPE_MISSED_CALL_INITIATED) {
                    timerTextViewMissedCall.setPaintFlags(timerTextViewMissedCall.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    timerTextViewMissedCall.setText(getString(R.string.retry_now));
                    timerTextViewMissedCall.setOnClickListener(v -> {
                        showLayout(FORM_LAYOUT);
                    });
                }
            }
        };
        timer.start();
    }

    private void dismissCountDownTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (verificationCallbackType == VerificationCallback.TYPE_MISSED_CALL_INITIATED) {
            timerTextViewMissedCall.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.optionsMenu).getVisibility() != View.VISIBLE) {
            showLayout(SETTINGS_LAYOUT);
        } else {
            finishAfterTransition();
        }
    }

    public void showLayout(int id) {
        findViewById(R.id.landingLayout).setVisibility(id == LANDING_LAYOUT ? View.VISIBLE : View.GONE);
        findViewById(R.id.profileLayout).setVisibility(id == PROFILE_LAYOUT ? View.VISIBLE : View.GONE);
        findViewById(R.id.loaderLayout).setVisibility(id == LOADER_LAYOUT ? View.VISIBLE : View.GONE);
        findViewById(R.id.formLayout).setVisibility(id == FORM_LAYOUT ? View.VISIBLE : View.GONE);
        findViewById(R.id.optionsMenu).setVisibility(id == SETTINGS_LAYOUT ? View.VISIBLE : View.GONE);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(findViewById(R.id.landingLayout).getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissCountDownTimer();
        TcSdk.clear();
        permissionHandler = null;
    }
}