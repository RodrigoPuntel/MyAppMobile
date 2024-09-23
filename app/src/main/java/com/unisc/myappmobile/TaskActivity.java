package com.unisc.myappmobile;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class TaskActivity extends AppCompatActivity {

    private EditText txtTaskName, txtTaskDescription;
    private SharedPreferences preferences;
    private String userId;
    private ListView listViewTasks, listViewCompleteTasks;
    private TaskAdapter taskAdapter, completeTaskAdapter;
    private ArrayList<JSONObject> tasksData, completedTasksData;
    private ImageButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        txtTaskName = findViewById(R.id.txtTaskName);
        txtTaskDescription = findViewById(R.id.txtTaskDescription);
        listViewTasks = findViewById(R.id.listViewTasks);
        listViewCompleteTasks = findViewById(R.id.listViewCompleteTasks);
        btnLogout = findViewById(R.id.btnLogout);

        preferences = getSharedPreferences("Shared", Context.MODE_PRIVATE);
        userId = preferences.getString("userId", "");
        boolean session = preferences.getBoolean("session", false);


        if (userId == null || userId.isEmpty()) {
            showMessage("Usuário não está logado.");
            finish();
        }

        if (!session) {
            Intent intent = new Intent(TaskActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        // Inicializa a lista e o adapter
        tasksData = new ArrayList<>();
        completedTasksData = new ArrayList<>();
        taskAdapter = new TaskAdapter(this, tasksData);  // Usando TaskAdapter para tarefas incompletas
        completeTaskAdapter = new TaskAdapter(this, completedTasksData);  // Usando o mesmo adapter para tarefas completas
        listViewTasks.setAdapter(taskAdapter);
        listViewCompleteTasks.setAdapter(completeTaskAdapter);

        fetchTasks();

        // Configura o clique nas tarefas incompletas
        listViewTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showOptionsDialog(position, false);
            }
        });

        // Configura o clique nas tarefas completas
        listViewCompleteTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showOptionsDialog(position, true);
            }
        });

        // Configura o botão de logout
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        // Configura o botão de voltar
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    private void logout() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(TaskActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void addTaskClick(View view) {
        String taskName = txtTaskName.getText().toString();
        String taskDescription = txtTaskDescription.getText().toString();

        if (taskName.isEmpty() || taskDescription.isEmpty()) {
            showMessage("Nome e descrição da tarefa são obrigatórios!");
            return;
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(20000);
        RequestParams params = new RequestParams();
        params.put("taskname", taskName);
        params.put("taskdescription", taskDescription);
        params.put("userId", userId);

        client.post("http://10.0.2.2:45455/AddTask.aspx", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                showMessage("Tarefa adicionada com sucesso!");
                txtTaskName.setText("");
                txtTaskDescription.setText("");
                fetchTasks();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                showMessage("Falha ao adicionar tarefa: " + error.getMessage());
            }
        });
    }

    private void fetchTasks() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(20000);
        RequestParams params = new RequestParams();
        params.put("userId", userId);

        client.get("http://10.0.2.2:45455/ApiTask.aspx", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String response = new String(responseBody);
                    JSONArray jsonArray = new JSONArray(response);
                    tasksData.clear();
                    completedTasksData.clear();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject taskObject = jsonArray.getJSONObject(i);
                        String status = taskObject.getString("Status");

                        if ("Incompleto".equalsIgnoreCase(status)) {
                            tasksData.add(taskObject);
                        } else if ("Completo".equalsIgnoreCase(status)) {
                            completedTasksData.add(taskObject);
                        }
                    }

                    taskAdapter.notifyDataSetChanged();
                    completeTaskAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    showMessage("Erro ao processar as tarefas.");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                showMessage("Falha ao buscar tarefas.");
            }
        });
    }

    private void showOptionsDialog(int position, boolean isCompleted) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ação com a Tarefa");

        if (isCompleted) {
            builder.setItems(new CharSequence[]{"Deletar"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteTask(position, true);
                }
            });
        } else {
            builder.setItems(new CharSequence[]{"Deletar", "Completar Tarefa"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            deleteTask(position, false);
                            break;
                        case 1:
                            completeTask(position);
                            break;
                    }
                }
            });
        }

        builder.show();
    }

    private void deleteTask(int position, boolean isCompleted) {
        ArrayList<JSONObject> taskList = isCompleted ? completedTasksData : tasksData;

        if (position < 0 || position >= taskList.size()) {
            showMessage("Tarefa não encontrada.");
            return;
        }

        JSONObject taskToDelete = taskList.get(position);
        Log.d("DeleteTask", "Tarefa selecionada: " + taskToDelete.toString());

        if (!taskToDelete.has("Id")) {
            showMessage("A tarefa não contém um ID.");
            return;
        }

        int taskId;
        try {
            taskId = taskToDelete.getInt("Id");
        } catch (JSONException e) {
            showMessage("Erro ao obter ID da tarefa: " + e.getMessage());
            return;
        }

        // Criação do Obj Json
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("Id", taskId);
            jsonObject.put("userId", userId);
        } catch (JSONException e) {
            showMessage("Erro ao criar objeto JSON: " + e.getMessage());
            return;
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(20000);
        StringEntity entity = new StringEntity(jsonObject.toString(), "UTF-8");
        client.post(this, "http://10.0.2.2:45455/DeleteTask.aspx", entity, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                showMessage("Tarefa deletada com sucesso!");
                fetchTasks(); // Atualiza a lista de tarefas
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                showMessage("Falha ao deletar tarefa: " + error.getMessage());
            }
        });
    }

    private void completeTask(int position) {
        if (position < 0 || position >= tasksData.size()) {
            showMessage("Tarefa não encontrada.");
            return;
        }

        JSONObject taskToComplete = tasksData.get(position);
        int taskId;
        try {
            taskId = taskToComplete.getInt("Id");
        } catch (JSONException e) {
            showMessage("Erro ao obter ID da tarefa: " + e.getMessage());
            return;
        }

        // Obj JSON para requisição
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("Id", taskId);
            jsonObject.put("userId", userId);
        } catch (JSONException e) {
            showMessage("Erro ao criar objeto JSON: " + e.getMessage());
            return;
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(20000);
        StringEntity entity = new StringEntity(jsonObject.toString(), "UTF-8");

        client.post(this, "http://10.0.2.2:45455/ApiStatus.aspx", entity, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                showMessage("Tarefa marcada como completa!");
                fetchTasks(); // Atualiza a lista de tarefas
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                String responseString = new String(responseBody);
                Log.e("CompleteTask", "Response: " + responseString);
                showMessage("Falha ao completar tarefa: " + error.getMessage());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("TaskActivity", "onPause called");

        // Verifica se a atividade está sendo encerrada
        if (isFinishing()) {
            // Limpar dados de sessão
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("session", false);
            editor.putString("user", "");
            editor.putString("userId", "");
            editor.apply();
        }
    }


    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
