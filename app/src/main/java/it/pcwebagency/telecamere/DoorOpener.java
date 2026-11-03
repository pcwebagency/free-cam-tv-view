package it.pcwebagency.telecamere;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

/** Conferma e apertura della serratura di un videocitofono, condivisa da griglia e schermo intero. */
public final class DoorOpener {
    private DoorOpener() {}

    public static void ask(Activity activity, Camera cam) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Apri porta — " + cam.label())
                .setPositiveButton("Porta 1", (d, w) -> open(activity, cam, 1))
                .setNeutralButton("Porta 2", (d, w) -> open(activity, cam, 2))
                .setNegativeButton("Annulla", null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
    }

    private static void open(Activity activity, Camera cam, int door) {
        new Thread(() -> {
            String msg;
            try {
                DahuaApi.openDoor(cam, door);
                msg = "Porta " + door + " aperta";
            } catch (Exception ex) {
                msg = "Apertura non riuscita: " + ex.getMessage();
            }
            String text = msg;
            activity.runOnUiThread(() -> Toast.makeText(activity, text, Toast.LENGTH_LONG).show());
        }).start();
    }
}
