package com.example.amapienroll;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.amapienroll.accountsetup.AccountSetupEvents;
import com.example.amapienroll.accountsetup.AccountSetupNotificationReceiver;
import com.google.android.managementapi.accountsetup.AccountSetupClient;
import com.google.android.managementapi.accountsetup.AccountSetupClientFactory;
import com.google.android.managementapi.accountsetup.model.AccountSetupAttempt;
import com.google.android.managementapi.accountsetup.model.LaunchAuthenticationActivityRequest;
import com.google.android.managementapi.accountsetup.model.StartAccountSetupRequest;
import com.google.android.managementapi.common.model.Role;
import com.google.android.managementapi.environment.EnvironmentClient;
import com.google.android.managementapi.environment.EnvironmentClientFactory;
import com.google.android.managementapi.environment.model.PrepareEnvironmentRequest;
import com.google.android.managementapi.environment.model.PrepareEnvironmentResponse;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

/**
 * Single-activity app that starts a managed Google Account enrollment through
 * the AMAPI SDK. It only drives the AccountSetup APIs
 * ({@code startAccountSetup} + {@code launchAuthenticationActivity}) — it does
 * not perform any MDM/device provisioning of its own.
 *
 * <p>This app acts as a custom DPC and must be device owner (or profile owner)
 * before enrolling: {@code launchAuthenticationActivity} throws
 * {@code SecurityException} otherwise. Android Device Policy is installed by
 * {@code prepareEnvironment}, not by hand.
 */
public class MainActivity extends AppCompatActivity
        implements AccountSetupEvents.Listener {

    private EditText tokenInput;
    private AccountSetupClient accountSetupClient;
    private EnvironmentClient environmentClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tokenInput = findViewById(R.id.tokenInput);
        Button enrollButton = findViewById(R.id.enrollButton);

        // Create the AccountSetupClient bound to this activity's result registry,
        // and register its lifecycle observer as required by the SDK.
        accountSetupClient = AccountSetupClientFactory.create(this, getActivityResultRegistry());
        getLifecycle().addObserver(accountSetupClient.getLifecycleObserver());
        environmentClient = EnvironmentClientFactory.create(this);

        enrollButton.setOnClickListener(v -> startEnrollment());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Receive callbacks forwarded from the notification receiver service.
        AccountSetupEvents.setListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (AccountSetupEvents.getListener() == this) {
            AccountSetupEvents.setListener(null);
        }
    }

    private void startEnrollment() {
        String enteredText = tokenInput.getText().toString().trim();
        if (TextUtils.isEmpty(enteredText)) {
            Toast.makeText(this, "Please enter an enrollment token", Toast.LENGTH_SHORT).show();
            return;
        }

        // The AccountSetup API requires this DPC to be device or profile owner.
        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean isOwner = dpm != null
                && (dpm.isDeviceOwnerApp(getPackageName())
                    || dpm.isProfileOwnerApp(getPackageName()));
        if (!isOwner) {
            Toast.makeText(this,
                    "App is not device/profile owner — account setup will not work.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Installs/updates Android Device Policy and Google Play services as
        // needed. It must complete before startAccountSetup, which relies on
        // Android Device Policy being present to delegate to.
        PrepareEnvironmentRequest envRequest = PrepareEnvironmentRequest.builder()
                .setRoles(ImmutableList.of(
                        Role.builder().setRoleType(Role.RoleType.DEVICE_POLICY_CONTROLLER).build()))
                .setAdmin(admin())
                .build();

        Toast.makeText(this, "Preparing environment…", Toast.LENGTH_SHORT).show();
        Futures.addCallback(
                environmentClient.prepareEnvironmentAsync(envRequest, admin()),
                new FutureCallback<PrepareEnvironmentResponse>() {
                    @Override
                    public void onSuccess(PrepareEnvironmentResponse result) {
                        runOnUiThread(() -> startAccountSetup(enteredText));
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                "Environment not ready: " + t.getMessage(),
                                Toast.LENGTH_LONG).show());
                    }
                },
                ContextCompat.getMainExecutor(this));
    }

    private void startAccountSetup(String token) {
        ComponentName notificationReceiver =
                new ComponentName(this, AccountSetupNotificationReceiver.class);

        StartAccountSetupRequest request = StartAccountSetupRequest.builder()
                .setEnrollmentToken(token)
                .setNotificationReceiverServiceComponentName(notificationReceiver)
                .setAdminComponentName(admin())
                .build();

        try {
            accountSetupClient.startAccountSetupFuture(request);
            Toast.makeText(this, "Account setup started…", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start account setup: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private ComponentName admin() {
        return new ComponentName(this, DeviceAdminReceiver.class);
    }

    // ---- AccountSetupEvents.Listener (callbacks forwarded from the service) ----

    @Override
    public void onAccountAdded(String userId, String deviceId, String email) {
        runOnUiThread(() -> Toast.makeText(this,
                "Account added\nuserId: " + userId
                        + "\ndeviceId: " + deviceId
                        + "\nemail: " + email,
                Toast.LENGTH_LONG).show());
    }

    @Override
    public void onAuthenticationActivityRequired(AccountSetupAttempt attempt) {
        runOnUiThread(() -> {
            LaunchAuthenticationActivityRequest request =
                    LaunchAuthenticationActivityRequest.builder()
                            .setAccountSetupAttempt(attempt)
                            .build();
            try {
                // Starts the Google account sign-in activity; control returns
                // here once the user authenticates (or skips).
                accountSetupClient.launchAuthenticationActivityFuture(request);
            } catch (Exception e) {
                Toast.makeText(this,
                        "Failed to launch authentication activity: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onError(String failureReason) {
        runOnUiThread(() -> Toast.makeText(this,
                "Enrollment error: " + failureReason, Toast.LENGTH_LONG).show());
    }
}
