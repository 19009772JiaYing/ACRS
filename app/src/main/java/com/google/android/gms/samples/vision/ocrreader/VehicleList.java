package com.google.android.gms.samples.vision.ocrreader;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class VehicleList extends ArrayAdapter<Vehicles> {

    private Activity context;
    List<Vehicles> vehicles;

    public VehicleList(Activity context, List<Vehicles> vehicles){

        super(context, R.layout.list_layout_vehicle, vehicles);
        this.context = context;
        this.vehicles = vehicles;

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();

        View listViewItem = inflater.inflate(R.layout.list_layout_vehicle, null, true);

        TextView tvVehLicense = (TextView) listViewItem.findViewById(R.id.TVVehLicense);
        TextView tvVehBrand = (TextView) listViewItem.findViewById(R.id.TVVehBrand);
        TextView tvVehModel = (TextView) listViewItem.findViewById(R.id.TVVehModel);

        Vehicles vehicle = vehicles.get(position);

        tvVehLicense.setText(vehicle.getVehLicense());
        tvVehBrand.setText(vehicle.getVehBrand());
        tvVehModel.setText(vehicle.getVehModel());

        return listViewItem;
    }
}
