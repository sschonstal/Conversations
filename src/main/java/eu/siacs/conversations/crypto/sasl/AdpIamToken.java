package eu.siacs.conversations.crypto.sasl;

import android.util.Base64;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

import eu.siacs.conversations.cdk.iam.Iam;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.xml.TagWriter;

public class AdpIamToken extends SaslMechanism {
    public AdpIamToken(final TagWriter tagWriter, final Account account) {
        super(tagWriter, account, null);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getMechanism() {
        return "ADP-IAM-TOKEN";
    }

    @Override
    public String getClientFirstMessage() {

        Iam iam = Iam.getInstance();
        if(iam.getToken() == null) {
            iam.login(account.getCdkUser(), account.getPassword());
        }
        final String sasl = '\u0000' + iam.getJid() + '\u0000' + iam.getToken();
        return Base64.encodeToString(sasl.getBytes(Charset.defaultCharset()), Base64.NO_WRAP);
    }
}