package com.example.amapienroll.accountsetup;

import androidx.annotation.NonNull;

import com.google.android.managementapi.accountsetup.AccountSetupListener;
import com.google.android.managementapi.notification.NotificationReceiverService;
import com.google.android.managementapi.accountsetup.model.AccountSetupAttempt;

/**
 * Receives account-setup status updates from the AMAPI SDK / Android Device
 * Policy. It forwards the interesting states to the foreground activity through
 * {@link AccountSetupEvents}, because launching the Google authentication
 * activity and showing UI must happen on the activity that owns the
 * {@code AccountSetupClient}.
 *
 * <p>Registered as an exported {@code <service>} in AndroidManifest.xml.
 */
public class AccountSetupNotificationReceiver extends NotificationReceiverService
        implements AccountSetupListener {

    @NonNull
    @Override
    protected AccountSetupListener getAccountSetupListener() {
        return this;
    }

    @Override
    public void onAccountSetupChanged(AccountSetupAttempt accountSetupAttempt) {
        AccountSetupEvents.Listener listener = AccountSetupEvents.getListener();

        switch (accountSetupAttempt.getState().getKind()) {
            case ADDED_ACCOUNT: {
                AccountSetupAttempt.EnterpriseAccount account =
                        accountSetupAttempt.getState().addedAccount();
                String userId = account.getUserId();
                String deviceId = account.getDeviceId();
                String email = account.getEmailAddress();
                if (listener != null) {
                    listener.onAccountAdded(userId, deviceId, email);
                }
                break;
            }
            case AUTHENTICATION_ACTIVITY_LAUNCH_REQUIRED_INFORMATION: {
                if (listener != null) {
                    listener.onAuthenticationActivityRequired(accountSetupAttempt);
                }
                break;
            }
            case ACCOUNT_SETUP_ERROR: {
                String failureReason = String.valueOf(
                        accountSetupAttempt.getState().accountSetupError().getFailureReason());
                if (listener != null) {
                    listener.onError(failureReason);
                }
                break;
            }
            default:
                // Unknown / intermediate state; nothing to surface.
                break;
        }
    }
}
