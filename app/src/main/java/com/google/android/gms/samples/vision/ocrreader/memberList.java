package com.google.android.gms.samples.vision.ocrreader;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class memberList extends AppCompatActivity {
    RecyclerView recyclerView;
    DatabaseReference database;
    MemAdapter memAdapter;
    ArrayList<Members> list;
    LinearLayoutManager linearLayoutManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.member_list);

        //Layout to display the user information
        recyclerView = findViewById(R.id.memList);
        database = FirebaseDatabase.getInstance().getReference("Members");
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(memberList.this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);

        list = new ArrayList<>();
        memAdapter = new MemAdapter(this, list);
        recyclerView.setAdapter(memAdapter);


        //Retrieved information from the database and add it into a list
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    Members members = dataSnapshot.getValue(Members.class);
                    list.add(members);
                }
                memAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

    }
}
