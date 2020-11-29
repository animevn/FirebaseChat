package com.haanhgs.app.firebasechat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.internal.OnConnectionFailedListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.auth.api.signin.GoogleSignIn;

public class MainActivity extends AppCompatActivity
        implements OnConnectionFailedListener {

    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private String username;
    private String photoUrl;

    private Button bnSend;
    private RecyclerView rvMessage;
    private LinearLayoutManager lnMessage;
    private EditText etMessage;

    // Firebase instance variables
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference firebaseRef;
    private FirebaseRecyclerAdapter<Message, Adapter.MessageHolder> firebaseRecyclerAdapter;

    private void initFirebase(){
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseRef = FirebaseDatabase.getInstance().getReference();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        } else {
            username = firebaseUser.getDisplayName();
            if (firebaseUser.getPhotoUrl() != null) {
                photoUrl = firebaseUser.getPhotoUrl().toString();
            }
        }
    }

    private void initGoogleSignin(){
        GoogleSignInOptions signInOptions = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
    }

    private void initRecyclerView(){
        // Initialize ProgressBar and RecyclerView.
        rvMessage = findViewById(R.id.recyclerview_message);
        lnMessage = new LinearLayoutManager(this);
        lnMessage.setStackFromEnd(true);
        rvMessage.setLayoutManager(lnMessage);
    }

    private SnapshotParser<Message> initSnapshotParser(){
        return dataSnapshot -> {
            Message message = dataSnapshot.getValue(Message.class);
            if (message != null) {
                message.setId(dataSnapshot.getKey());
            }else {
                message = new Message();
            }
            return message;
        };
    }

    private FirebaseRecyclerOptions<Message> initOptions(){
        DatabaseReference messagesRef = firebaseRef.child(MESSAGES_CHILD);
        return new FirebaseRecyclerOptions.Builder<Message>()
                        .setQuery(messagesRef, initSnapshotParser())
                        .build();
    }

    private void handleAdapter(){
        firebaseRecyclerAdapter = new Adapter(initOptions());
        firebaseRecyclerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = firebaseRecyclerAdapter.getItemCount();
                int lastVisiblePosition =
                        lnMessage.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    rvMessage.scrollToPosition(positionStart);
                }
            }
        });
        rvMessage.setAdapter(firebaseRecyclerAdapter);
    }

    private void handleEditTextMessage(){
        etMessage = findViewById(R.id.edittext_message);
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    bnSend.setEnabled(true);
                } else {
                    bnSend.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void handleButtonSend(){
        bnSend = findViewById(R.id.button_send);
        bnSend.setOnClickListener(view -> {
            Message message = new Message(etMessage.getText().toString(),
                    username, photoUrl, null);
            firebaseRef.child(MESSAGES_CHILD).push().setValue(message);
            etMessage.setText("");
        });
    }

    private void handleUploadImage(){
        ImageView ivMessage = findViewById(R.id.imageview_add_message);
        ivMessage.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGE);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        initFirebase();
        initGoogleSignin();
        initRecyclerView();
        handleAdapter();
        handleEditTextMessage();
        handleButtonSend();
        handleUploadImage();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    @Override
    public void onPause() {
        firebaseRecyclerAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        firebaseRecyclerAdapter.startListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                googleSignInClient.signOut();
                firebaseAuth.signOut();
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    public void uploadImage(Uri uri, @NonNull DatabaseReference databaseReference){
        String key = databaseReference.getKey();
        if (key != null && uri.getLastPathSegment() != null){
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReference(firebaseUser.getUid())
                    .child(key)
                    .child(uri.getLastPathSegment());
            putImageInStorage(storageReference, uri, key);
        }
    }

    private void prepareUpload(Intent data){
        if (data != null && data.getData() != null) {
            final Uri uri = data.getData();
            Log.d(TAG, "Uri: " + uri.toString());
            Message message = new Message(null, username, photoUrl, LOADING_IMAGE_URL);
            firebaseRef.child(MESSAGES_CHILD).push()
                    .setValue(message, (error, ref) -> {
                        if (error == null) {
                            uploadImage(uri, ref);
                        } else {
                            Log.w(TAG, "data write failed", error.toException());
                        }
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                prepareUpload(data);
            }
        }
    }

    private void uploadTask(Task<UploadTask.TaskSnapshot> task, final String key){
        if (task.isSuccessful() && task.getResult() != null
                && task.getResult().getMetadata() != null
                && task.getResult().getMetadata().getReference() != null){
            task.getResult().getMetadata().getReference().getDownloadUrl()
                    .addOnCompleteListener(MainActivity.this,
                            task1 -> {
                                if (task1.isSuccessful() && task1.getResult() != null) {
                                    Message message = new Message(null, username, photoUrl,
                                                    task1.getResult().toString());
                                    firebaseRef.child(MESSAGES_CHILD).child(key).setValue(message);
                                }
                            });
        }
    }

    private void putImageInStorage(StorageReference storageReference, Uri uri, final String key) {
        storageReference.putFile(uri).addOnCompleteListener(MainActivity.this,
                task -> {
                    if (task.isSuccessful()) {
                        uploadTask(task, key);
                    } else {
                        Log.w(TAG, "Image upload failed.", task.getException());
                    }
                });
    }
}
