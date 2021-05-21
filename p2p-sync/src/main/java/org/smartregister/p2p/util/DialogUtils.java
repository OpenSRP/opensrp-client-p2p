package org.smartregister.p2p.util;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 13/03/2019
 */

public abstract class DialogUtils {

    public static void dismissAllDialogs(@NonNull FragmentManager fragmentManager) {
        List<Fragment> fragments = fragmentManager.getFragments();

        for (Fragment fragment : fragments) {
            if (fragment instanceof DialogFragment && fragment.isAdded()) {
                ((DialogFragment) fragment).dismissAllowingStateLoss();
            }

            FragmentManager childFragmentManager = fragment.getChildFragmentManager();
            dismissAllDialogs(childFragmentManager);
        }
    }
}
