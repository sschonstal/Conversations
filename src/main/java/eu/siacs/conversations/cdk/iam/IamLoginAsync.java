package eu.siacs.conversations.cdk.iam;

import android.os.AsyncTask;

public class IamLoginAsync extends AsyncTask<String, String, Boolean> {

    private Exception exception;

    @Override
    public Boolean doInBackground(String... params) {
        try {
            Iam iam = Iam.getInstance();
            iam.login(params[0], params[1]);
        } catch (Exception e) {
            exception= e;
            return false;
        }
        return true;
    }

    public Exception getException() {
        return exception;
    }
}
