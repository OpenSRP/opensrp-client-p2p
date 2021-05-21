package org.smartregister.p2p.sample;

import android.app.Application;
import android.os.Build;
import androidx.annotation.NonNull;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.sample.dao.SampleReceiverDao;
import org.smartregister.p2p.sample.dao.SampleSenderDao;
import org.smartregister.p2p.sample.util.FailSafeRecalledID;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class SampleApplication extends Application implements P2PAuthorizationService {

    @Override
    public void onCreate() {
        super.onCreate();
        P2PLibrary.Options options = new P2PLibrary.Options(this
                , "p92ksdicsdj$*Dj"
                , String.format("%s %s", Build.MANUFACTURER, Build.MODEL)
                , this, new SampleReceiverDao(), new SampleSenderDao());

        options.setBatchSize(100);
        options.setRecalledIdentifier(new FailSafeRecalledID());
        P2PLibrary.init(options);
    }

    @Override
    public void authorizeConnection(@NonNull Map<String, Object> authorizationDetails, @NonNull AuthorizationCallback authorizationCallback) {
        Object appVersion = authorizationDetails.get("app-version");
        Object appType = authorizationDetails.get("app-type");

        // Check if appVersion is an int
        if (appVersion != null && appVersion instanceof Double && ((double) appVersion) >= 9d
                && appType != null && appType instanceof String && appType.equals("normal-user")) {
            authorizationCallback.onConnectionAuthorized();
        } else {
            authorizationCallback.onConnectionAuthorizationRejected("App version or app type is incorrect");
        }
    }

    @Override
    public void getAuthorizationDetails(@NonNull OnAuthorizationDetailsProvidedCallback onAuthorizationDetailsProvidedCallback) {
        HashMap<String, Object> authorizationDetails = new HashMap<>();
        authorizationDetails.put("app-version", 9);
        authorizationDetails.put("app-type", "normal-user");

        onAuthorizationDetailsProvidedCallback.onAuthorizationDetailsProvided(authorizationDetails);
    }
}
