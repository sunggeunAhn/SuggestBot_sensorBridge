package com.project.pinetree408.sbspeechrecognition;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;


/**
 * A simple dialog with a message.
 *
 * <p>The calling {@link android.app.Activity} needs to implement {@link
 * MessageDialogFragment.Listener}.</p>
 */
public class MessageDialogFragment extends AppCompatDialogFragment {

    public interface Listener {
        /**
         * Called when the dialog is dismissed.
         */
        void onMessageDialogDismissed();
    }

    private static final String ARG_MESSAGE = "message";

    /**
     * Creates a new instance of {@link MessageDialogFragment}.
     *
     * @param message The message to be shown on the dialog.
     * @return A newly created dialog fragment.
     */
    public static MessageDialogFragment newInstance(String message) {
        final MessageDialogFragment fragment = new MessageDialogFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setMessage(getArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> ((Listener) getActivity()).onMessageDialogDismissed())
                .setOnDismissListener(
                        (dialogInterface) -> ((Listener) getActivity()).onMessageDialogDismissed())
                .create();
    }

}
