package com.unisc.myappmobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class TaskAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<JSONObject> tasks;

    public TaskAdapter(Context context, ArrayList<JSONObject> tasks) {
        this.context = context;
        this.tasks = tasks;
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public Object getItem(int position) {
        return tasks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_task, parent, false);
        }

        // Recupera o JSON da tarefa
        JSONObject task = tasks.get(position);

        TextView txtTaskName = convertView.findViewById(R.id.txtTaskName);
        TextView txtTaskDescription = convertView.findViewById(R.id.txtTaskDescription);

        try {
            txtTaskName.setText(task.getString("TaskName"));
            txtTaskDescription.setText(task.getString("TaskDescription"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return convertView;
    }
}
