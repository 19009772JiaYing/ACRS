package com.google.android.gms.samples.vision.ocrreader;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MemAdapter extends RecyclerView.Adapter<MemAdapter.MemViewHolder>
{
    Context context;

    ArrayList<Members> list;

    public MemAdapter(Context context, ArrayList<Members> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public MemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item, parent, false);
        return new MemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MemViewHolder holder, int position) {

        Members members = list.get(position);
        holder.fullname.setText(members.getMemName());
        holder.designation.setText(members.getMemRole());
        holder.license.setText(members.getMemLicense());
        holder.company.setText(members.getMemCompany());

        // Once member is being deleted, it will display a message to notify the Admin
        CharSequence text  = "Member Deleted";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context,text,duration);
        holder.txtOpt.setOnClickListener(new View.OnClickListener() {
            @Override
            //A popup menu will be displayed when the user clicked on the icon
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(context, v);
                popupMenu.getMenuInflater().inflate(R.menu.options_menu,popupMenu.getMenu());
                popupMenu.show();
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId())
                        {
                            case R.id.menu_edit:
                                String uid = list.get(position).getMemId();
                                String contact = list.get(position).getMemContact();
                                String role = list.get(position).getMemRole();
                                String lp = list.get(position).getMemLicense();
                                String company = list.get(position).getMemCompany();

                                //Retrieve the information from the Firebase and store it
                                //Then pass the information to the Update class
                                Intent i = new Intent(context.getApplicationContext(), Update.class);
                                i.putExtra("getid", uid);
                                i.putExtra("getcontact", contact);
                                i.putExtra("getrole", role);
                                i.putExtra("getLP", lp);
                                i.putExtra("getcompany", company);

                                context.startActivity(i);
                                break;

                            case R.id.menu_delete:
                                //Retrieve the user id from the Firebase corresponding to the user that they clicked on
                                uid = list.get(position).getMemId();
                                Intent s = new Intent(context.getApplicationContext(), AddMember.class);
                                DatabaseReference dR = FirebaseDatabase.getInstance().getReference("Members").child(uid);
                                dR.removeValue();
                                DatabaseReference dr = FirebaseDatabase.getInstance().getReference("vehicles").child(uid);
                                dr.removeValue();
                                context.startActivity(s);
                                toast.show();
                                break;


                            case R.id.menu_addVeh:
                                //Retrieve the user id from the Firebase corresponding to the user that they clicked on
                                // Since the vehicle is tagged to the Member ID, it will delete both the Member and Vehicles together
                                uid = list.get(position).getMemId();
                                Intent v = new Intent(context.getApplicationContext(),AddVehicle.class);
                                v.putExtra("getid", uid);
                                context.startActivity(v);
                                break;

                        }
                        return true;
                    }
                });
            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MemViewHolder extends RecyclerView.ViewHolder{

        TextView fullname, contact, designation, license, company, txtOpt;


        public MemViewHolder(View itemView) {
            super(itemView);

            fullname = itemView.findViewById(R.id.tvFullname);
            designation = itemView.findViewById(R.id.tvRole);
            license = itemView.findViewById(R.id.tvLicense);
            company = itemView.findViewById(R.id.tvCom);
            txtOpt = itemView.findViewById(R.id.txtOpt);
        }


    }



}
