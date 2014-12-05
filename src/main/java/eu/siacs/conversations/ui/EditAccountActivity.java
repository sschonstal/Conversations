package eu.siacs.conversations.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import eu.siacs.conversations.R;
import eu.siacs.conversations.cdk.iam.Iam;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.services.XmppConnectionService.OnAccountUpdate;
import eu.siacs.conversations.ui.adapter.KnownHostsAdapter;
import eu.siacs.conversations.utils.CryptoHelper;
import eu.siacs.conversations.utils.UIHelper;
import eu.siacs.conversations.xmpp.XmppConnection.Features;
import eu.siacs.conversations.xmpp.jid.InvalidJidException;
import eu.siacs.conversations.xmpp.jid.Jid;
import eu.siacs.conversations.xmpp.pep.Avatar;

public class EditAccountActivity extends XmppActivity implements OnAccountUpdate {

	private AutoCompleteTextView mAccountName;
	private EditText mPassword;
	private EditText mPasswordConfirm;
	private CheckBox mRegisterNew;
	private Button mCancelButton;
	private Button mSaveButton;

	private LinearLayout mStats;
	private TextView mServerInfoSm;
	private TextView mServerInfoCarbons;
	private TextView mServerInfoPep;
	private TextView mSessionEst;
	private TextView mOtrFingerprint;
	private ImageView mAvatar;
	private RelativeLayout mOtrFingerprintBox;
	private ImageButton mOtrFingerprintToClipboardButton;

	private String usernameToEdit;
	private Account mAccount;

	private boolean mFetchingAvatar = false;

	private OnClickListener mSaveButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
            Iam iam = new Iam();
			if (mAccount != null
					&& mAccount.getStatus() == Account.State.DISABLED) {
				mAccount.setOption(Account.OPTION_DISABLED, false);
				xmppConnectionService.updateAccount(mAccount);
				return;
			}
            String username = mAccountName.getText().toString();
            String password = mPassword.getText().toString();
            String token = iam.login(username, password);

            if (token.length() == 0) {
                mAccountName
                        .setError(getString(R.string.iam_failed_login));
                mAccountName.requestFocus();
                return;
            }
            Jid jid;
            try {
                jid = Jid.fromString(iam.getComminicationEdgeId());
            } catch(InvalidJidException e) {
                mAccountName
                        .setError(getString(R.string.iam_failed_communicationEdgeID));
                mAccountName.requestFocus();
                return;
            }

			if (mAccount != null) {
				mAccount.setPassword(password);
                mAccount.setUsername(username);
                mAccount.setServer(getString(R.string.xmppServerName));
				xmppConnectionService.updateAccount(mAccount);
			} else {

                if (xmppConnectionService.findAccountByName(username) != null) {
                    mAccountName
                            .setError(getString(R.string.account_already_exists));
                    mAccountName.requestFocus();
                    return;
                }

                mAccount = new Account(username, jid.toBareJid(), password);
				mAccount.setOption(Account.OPTION_USETLS, true);
				mAccount.setOption(Account.OPTION_USECOMPRESSION, true);
				mAccount.setOption(Account.OPTION_REGISTER, false);
				xmppConnectionService.createAccount(mAccount);
			}
			if (usernameToEdit != null) {
				finish();
			} else {
				updateSaveButton();
				updateAccountInformation();
			}

		}
	};
	private OnClickListener mCancelButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			finish();
		}
	};
		@Override
		public void onAccountUpdate() {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (mAccount != null
							&& mAccount.getStatus() != Account.State.ONLINE
							&& mFetchingAvatar) {
						startActivity(new Intent(getApplicationContext(),
								ManageAccountActivity.class));
						finish();
					} else if (usernameToEdit == null && mAccount != null
							&& mAccount.getStatus() == Account.State.ONLINE) {
						if (!mFetchingAvatar) {
							mFetchingAvatar = true;
							xmppConnectionService.checkForAvatar(mAccount,
									mAvatarFetchCallback);
						}
					} else {
						updateSaveButton();
					}
					if (mAccount != null) {
						updateAccountInformation();
					}
				}
			});
		}
	private UiCallback<Avatar> mAvatarFetchCallback = new UiCallback<Avatar>() {

		@Override
		public void userInputRequried(PendingIntent pi, Avatar avatar) {
			finishInitialSetup(avatar);
		}

		@Override
		public void success(Avatar avatar) {
			finishInitialSetup(avatar);
		}

		@Override
		public void error(int errorCode, Avatar avatar) {
			finishInitialSetup(avatar);
		}
	};
    private TextWatcher mTextWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
								  int count) {
			updateSaveButton();
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
									  int after) {

		}

		@Override
		public void afterTextChanged(Editable s) {

		}
	};
	private OnClickListener mAvatarClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			if (mAccount!=null) {
				Intent intent = new Intent(getApplicationContext(),
						PublishProfilePictureActivity.class);
				intent.putExtra("account", mAccount.getJid().toBareJid().toString());
				startActivity(intent);
			}
		}
	};

	protected void finishInitialSetup(final Avatar avatar) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Intent intent;
				if (avatar != null) {
					intent = new Intent(getApplicationContext(),
							StartConversationActivity.class);
				} else {
					intent = new Intent(getApplicationContext(),
							PublishProfilePictureActivity.class);
					intent.putExtra("account", mAccount.getJid().toBareJid().toString());
					intent.putExtra("setup", true);
				}
				startActivity(intent);
				finish();
			}
		});
	}

	protected void updateSaveButton() {
		if (mAccount != null
				&& mAccount.getStatus() == Account.State.CONNECTING) {
			this.mSaveButton.setEnabled(false);
			this.mSaveButton.setTextColor(getSecondaryTextColor());
			this.mSaveButton.setText(R.string.account_status_connecting);
		} else if (mAccount != null
				&& mAccount.getStatus() == Account.State.DISABLED) {
			this.mSaveButton.setEnabled(true);
			this.mSaveButton.setTextColor(getPrimaryTextColor());
			this.mSaveButton.setText(R.string.enable);
		} else {
			this.mSaveButton.setEnabled(true);
			this.mSaveButton.setTextColor(getPrimaryTextColor());
			if (usernameToEdit != null) {
				if (mAccount != null
						&& mAccount.getStatus() == Account.State.ONLINE) {
					this.mSaveButton.setText(R.string.save);
					if (!accountInfoEdited()) {
						this.mSaveButton.setEnabled(false);
						this.mSaveButton.setTextColor(getSecondaryTextColor());
					}
				} else {
					this.mSaveButton.setText(R.string.connect);
				}
			} else {
				this.mSaveButton.setText(R.string.next);
			}
		}
	}

	protected boolean accountInfoEdited() {
		return (!this.mAccount.getUsername().equals(this.mAccountName.getText().toString()))
				|| (!this.mAccount.getPassword().equals(this.mPassword.getText().toString()));
	}

	@Override
	protected String getShareableUri() {
		if (mAccount!=null) {
			return mAccount.getShareableUri();
		} else {
			return "";
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_account);
		this.mAccountName = (AutoCompleteTextView) findViewById(R.id.account_name);
		this.mAccountName.addTextChangedListener(this.mTextWatcher);
		this.mPassword = (EditText) findViewById(R.id.account_password);
		this.mPassword.addTextChangedListener(this.mTextWatcher);
		this.mAvatar = (ImageView) findViewById(R.id.avater);
		this.mAvatar.setOnClickListener(this.mAvatarClickListener);
		//this.mRegisterNew = (CheckBox) findViewById(R.id.account_register_new);
		this.mStats = (LinearLayout) findViewById(R.id.stats);
		this.mSessionEst = (TextView) findViewById(R.id.session_est);
		this.mServerInfoCarbons = (TextView) findViewById(R.id.server_info_carbons);
		this.mServerInfoSm = (TextView) findViewById(R.id.server_info_sm);
		this.mServerInfoPep = (TextView) findViewById(R.id.server_info_pep);
		this.mOtrFingerprint = (TextView) findViewById(R.id.otr_fingerprint);
		this.mOtrFingerprintBox = (RelativeLayout) findViewById(R.id.otr_fingerprint_box);
		this.mOtrFingerprintToClipboardButton = (ImageButton) findViewById(R.id.action_copy_to_clipboard);
		this.mSaveButton = (Button) findViewById(R.id.save_button);
		this.mCancelButton = (Button) findViewById(R.id.cancel_button);
		this.mSaveButton.setOnClickListener(this.mSaveButtonClickListener);
		this.mCancelButton.setOnClickListener(this.mCancelButtonClickListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.editaccount, menu);
		MenuItem showQrCode = menu.findItem(R.id.action_show_qr_code);
		if (mAccount == null) {
			showQrCode.setVisible(false);
		}
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (getIntent() != null) {

            this.usernameToEdit = getIntent().getStringExtra("jid");
            if (this.usernameToEdit != null) {
				this.mRegisterNew.setVisibility(View.GONE);
				getActionBar().setTitle(getString(R.string.account_details));
			} else {
				this.mAvatar.setVisibility(View.GONE);
				getActionBar().setTitle(R.string.action_add_account);
			}
		}
	}

	@Override
	protected void onBackendConnected() {
        KnownHostsAdapter mKnownHostsAdapter = new KnownHostsAdapter(this,
                android.R.layout.simple_list_item_1,
                xmppConnectionService.getKnownHosts());
		if (this.usernameToEdit != null) {
			this.mAccount = xmppConnectionService.findAccountByName(usernameToEdit);
			updateAccountInformation();
		} else if (this.xmppConnectionService.getAccounts().size() == 0) {
			getActionBar().setDisplayHomeAsUpEnabled(false);
			getActionBar().setDisplayShowHomeEnabled(false);
			this.mCancelButton.setEnabled(false);
			this.mCancelButton.setTextColor(getSecondaryTextColor());
		}
		this.mAccountName.setAdapter(mKnownHostsAdapter);
		updateSaveButton();
	}

	private void updateAccountInformation() {
		this.mAccountName.setText(this.mAccount.getUsername());
		this.mPassword.setText(this.mAccount.getPassword());
		if (this.usernameToEdit != null) {
			this.mAvatar.setVisibility(View.VISIBLE);
			this.mAvatar.setImageBitmap(avatarService().get(this.mAccount, getPixel(72)));
		}

		if (this.mAccount.getStatus() == Account.State.ONLINE
				&& !this.mFetchingAvatar) {
			this.mStats.setVisibility(View.VISIBLE);
			this.mSessionEst.setText(UIHelper.readableTimeDifferenceFull(
					getApplicationContext(), this.mAccount.getXmppConnection()
							.getLastSessionEstablished()));
			Features features = this.mAccount.getXmppConnection().getFeatures();
			if (features.carbons()) {
				this.mServerInfoCarbons.setText(R.string.server_info_available);
			} else {
				this.mServerInfoCarbons
						.setText(R.string.server_info_unavailable);
			}
			if (features.sm()) {
				this.mServerInfoSm.setText(R.string.server_info_available);
			} else {
				this.mServerInfoSm.setText(R.string.server_info_unavailable);
			}
			if (features.pubsub()) {
				this.mServerInfoPep.setText(R.string.server_info_available);
			} else {
				this.mServerInfoPep.setText(R.string.server_info_unavailable);
			}
			final String fingerprint = this.mAccount.getOtrFingerprint();
			if (fingerprint != null) {
				this.mOtrFingerprintBox.setVisibility(View.VISIBLE);
				this.mOtrFingerprint.setText(CryptoHelper.prettifyFingerprint(fingerprint));
				this.mOtrFingerprintToClipboardButton
						.setVisibility(View.VISIBLE);
				this.mOtrFingerprintToClipboardButton
						.setOnClickListener(new View.OnClickListener() {

							@Override
							public void onClick(View v) {

								if (copyTextToClipboard(fingerprint, R.string.otr_fingerprint)) {
									Toast.makeText(
											EditAccountActivity.this,
											R.string.toast_message_otr_fingerprint,
											Toast.LENGTH_SHORT).show();
								}
							}
						});
			} else {
				this.mOtrFingerprintBox.setVisibility(View.GONE);
			}
		} else {
			if (this.mAccount.errorStatus()) {
				this.mAccountName.setError(getString(this.mAccount.getStatus().getReadableId()));
				this.mAccountName.requestFocus();
			}
			this.mStats.setVisibility(View.GONE);
		}
	}
}
