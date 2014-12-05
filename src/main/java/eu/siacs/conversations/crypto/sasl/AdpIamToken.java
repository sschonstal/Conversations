package eu.siacs.conversations.crypto.sasl;

import android.util.Base64;

import java.nio.charset.Charset;

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
		return "AdpIamToken";
	}

	@Override
	public String getClientFirstMessage() {

        Iam iam = new Iam();
        String jid;
        String token = iam.login(account.getUsername(), account.getPassword());
        if(account.getJid().toString().length() >0) {
            jid = account.getJid().toString();
        } else {
            jid = iam.getComminicationEdgeId();
        }
        final String sasl = '\u0000' + jid + '\u0000' + token;
		return Base64.encodeToString(sasl.getBytes(Charset.defaultCharset()), Base64.NO_WRAP);
	}
}
