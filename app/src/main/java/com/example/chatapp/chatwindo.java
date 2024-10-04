package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;


public class chatwindo extends AppCompatActivity {
    String reciverimg, reciverUid,reciverName,SenderUID;
    CircleImageView profile;
    TextView reciverNName;
    FirebaseDatabase database;
    FirebaseAuth firebaseAuth;
    public  static String senderImg;
    public  static String reciverIImg;
    CardView sendbtn;
    EditText textmsg;
    String senderLang, receiverLang;

    String senderRoom,reciverRoom;
    RecyclerView messageAdpter;
    ArrayList<msgModelclass> messagesArrayList;
    messagesAdpter mmessagesAdpter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatwindo);
        getSupportActionBar().hide();
        database = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        reciverName = getIntent().getStringExtra("nameeee");
        reciverimg = getIntent().getStringExtra("reciverImg");
        reciverUid = getIntent().getStringExtra("uid");

        messagesArrayList = new ArrayList<>();

        sendbtn = findViewById(R.id.sendbtnn);
        textmsg = findViewById(R.id.textmsg);
        reciverNName = findViewById(R.id.recivername);
        profile = findViewById(R.id.profileimgg);
        messageAdpter = findViewById(R.id.msgadpter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageAdpter.setLayoutManager(linearLayoutManager);
        mmessagesAdpter = new messagesAdpter(chatwindo.this,messagesArrayList);
        messageAdpter.setAdapter(mmessagesAdpter);


        Picasso.get().load(reciverimg).into(profile);
        reciverNName.setText(""+reciverName);

        SenderUID =  firebaseAuth.getUid();

        senderRoom = SenderUID+reciverUid;
        reciverRoom = reciverUid+SenderUID;


        DatabaseReference userRef = database.getReference().child("user").child(SenderUID);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                senderLang = snapshot.child("language").getValue().toString();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        DatabaseReference receiverRef = database.getReference().child("user").child(reciverUid);
        receiverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                receiverLang = snapshot.child("language").getValue().toString();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        DatabaseReference  reference = database.getReference().child("user").child(firebaseAuth.getUid());
        DatabaseReference  chatreference = database.getReference().child("chats").child(senderRoom).child("messages");


        chatreference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messagesArrayList.clear();
                for (DataSnapshot dataSnapshot:snapshot.getChildren()){
                    msgModelclass messages = dataSnapshot.getValue(msgModelclass.class);
                    messagesArrayList.add(messages);
                }
                mmessagesAdpter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                senderImg= snapshot.child("profilepic").getValue().toString();
                reciverIImg=reciverimg;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        sendbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = textmsg.getText().toString();
                if (message.isEmpty()){
                    Toast.makeText(chatwindo.this, "Enter The Message First", Toast.LENGTH_SHORT).show();
                    return;
                }
                textmsg.setText("");
                Date date = new Date();
                msgModelclass messagess = new msgModelclass(message,SenderUID,date.getTime());



                translateMessage(message, senderLang, receiverLang);
            }
        });
    }

    private void translateMessage(String message, String sourceLang, String targetLang){
        int sourceLangCode = getFirebaseLanguageCode(sourceLang);
        int targetLangCode = getFirebaseLanguageCode(targetLang);

        FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder()
                .setSourceLanguage(sourceLangCode)
                .setTargetLanguage(targetLangCode)
                .build();

        FirebaseTranslator translator = FirebaseNaturalLanguage.getInstance().getTranslator(options);

        translator.downloadModelIfNeeded().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                translator.translate(message).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedMessage) {
                        saveTranslatedMessage(message, translatedMessage);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(chatwindo.this, "Translation Failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(chatwindo.this, "Model download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTranslatedMessage(String originalMessage, String translatedMessage){
        String fullMessage = originalMessage + "\n\nTranslated Message:\n" + translatedMessage;
        Date date = new Date();
        msgModelclass translatedMsg = new msgModelclass(fullMessage, SenderUID, date.getTime());

        database.getReference().child("chats").child(senderRoom).child("messages").push().setValue(translatedMsg);
        database.getReference().child("chats").child(reciverRoom).child("messages").push().setValue(translatedMsg);
    }
    private int getFirebaseLanguageCode(String language){
        switch (language){
            case "English":
                return FirebaseTranslateLanguage.EN;
            case "Tamil":
                return FirebaseTranslateLanguage.TA;
            case "Hindi":
                return FirebaseTranslateLanguage.HI;
            case "Bengali":
                return FirebaseTranslateLanguage.BN;
            case "Arabic":
                return FirebaseTranslateLanguage.AR;
            case "Telugu":
                return FirebaseTranslateLanguage.TE;
            case "Urdu":
                return FirebaseTranslateLanguage.UR;
            case "Kannada":
                return FirebaseTranslateLanguage.KN;
            case "Marathi":
                return FirebaseTranslateLanguage.MR;
            case "French":
                return FirebaseTranslateLanguage.FR;
            case "Spanish":
                return FirebaseTranslateLanguage.ES;
            default:
                return FirebaseTranslateLanguage.EN;
        }
    }

}