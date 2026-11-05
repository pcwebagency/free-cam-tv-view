package it.pcwebagency.telecamere;

import android.app.Activity;

import android.widget.Toast;

/** Conferma e apertura della serratura di un videocitofono, condivisa da griglia e schermo intero. */
public final class DoorOpener {
    private DoorOpener() {}

    public static void ask(Activity activity, Camera cam) {
        new ModernDialog(activity)
                .setTitle(activity.getString(R.string.door_title, cam.label()))
                .setPositiveButton(activity.getString(R.string.door_1), (d, w) -> open(activity, cam, 1))
                .setNeutralButton(activity.getString(R.string.door_2), (d, w) -> open(activity, cam, 2))
                .setNegativeButton(activity.getString(R.string.cancel), null)
                .show();
    }

    private static void open(Activity activity, Camera cam, int door) {
        new Thread(() -> {
            String msg;
            try {
                DahuaApi.openDoor(cam, door);
                msg = activity.getString(R.string.door_opened, door);
            } catch (Exception ex) {
                msg = activity.getString(R.string.door_failed, ex.getMessage());
            }
            String text = msg;
            activity.runOnUiThread(() -> Toast.makeText(activity, text, Toast.LENGTH_LONG).show());
        }).start();
    }
}
