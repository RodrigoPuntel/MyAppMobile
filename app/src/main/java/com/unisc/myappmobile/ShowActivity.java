package com.unisc.myappmobile;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import cz.msebera.android.httpclient.Header;

public class ShowActivity extends AppCompatActivity {

    private ListView listViewTasks;
    private ArrayAdapter<String> taskAdapter;
    private ArrayList<String> tasksList;
    private ArrayList<JSONObject> tasksData; // Para armazenar dados JSON das tarefas
    private SharedPreferences preferences;
    private TextView txtBemvindo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        // Inicializando elementos do layout
        listViewTasks = findViewById(R.id.listViewTasks);
        txtBemvindo = findViewById(R.id.txtBemvindo);

        // Obtendo as preferências compartilhadas
        preferences = getSharedPreferences("Shared", Context.MODE_PRIVATE);
        String userIdString = preferences.getString("userId", "");

        // Se não houver userId, exibe uma mensagem de erro
        if (userIdString.isEmpty()) {
            Toast.makeText(this, "ID de usuário não encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Exibe uma saudação personalizada com o userId
        txtBemvindo.setText("Bem-vindo, Usuário " + userIdString);

        // Converte userId para int
        int userId;
        try {
            userId = Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "ID de usuário inválido.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inicializa a lista de tarefas e o adapter
        tasksList = new ArrayList<>();
        tasksData = new ArrayList<>(); // Inicializa o ArrayList para armazenar dados JSON
        taskAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tasksList);
        listViewTasks.setAdapter(taskAdapter);

        // Chama o método para buscar as tarefas do usuário
        fetchTasks(userId);

        // Configura o listener de clique para a ListView
        listViewTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showOptionsDialog(position);
            }
        });
    }

    private void fetchTasks(int userId) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(20000);

        // Configura os parâmetros da requisição
        RequestParams params = new RequestParams();
        params.put("userId", userId);

        // Faz a requisição GET para buscar as tarefas
        client.get("http://10.0.2.2:45455/ApiTask.aspx", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String response = new String(responseBody);
                    JSONArray jsonArray = new JSONArray(response);

                    // Limpa a lista de tarefas antes de atualizar
                    tasksList.clear();
                    tasksData.clear(); // Limpa os dados JSON

                    // Itera sobre o JSON e adiciona cada tarefa à lista
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject taskObject = jsonArray.getJSONObject(i);
                        String taskName = taskObject.getString("TaskName");
                        tasksList.add(taskName);  // Adiciona o nome da tarefa na lista
                        tasksData.add(taskObject); // Adiciona o JSON da tarefa à lista de dados
                    }

                    // Atualiza o adapter para refletir as mudanças
                    taskAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(ShowActivity.this, "Erro ao processar a resposta.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(ShowActivity.this, "Falha ao conectar ao servidor.", Toast.LENGTH_SHORT).show();
                Log.e("ERROR", "Falha na requisição: " + error.getMessage());
            }
        });
    }

    private void showOptionsDialog(int position) {
        // Cria um diálogo para opções de ação
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ação com a Tarefa");
        builder.setItems(new CharSequence[]{"Deletar", "Alterar Descrição"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Deletar
                        deleteTask(position);
                        break;
                    case 1: // Alterar Descrição
                        // Você pode adicionar uma função para alterar a descrição aqui
                        break;
                }
            }
        });
        builder.show();
    }

    private void deleteTask(int position) {
        // Obtém o objeto JSON da tarefa selecionada
        JSONObject taskToDelete = tasksData.get(position);
        int taskId;
        try {
            taskId = taskToDelete.getInt("TaskId"); // Supondo que TaskId seja um campo no JSON
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao obter o ID da tarefa.", Toast.LENGTH_SHORT).show();
            return;
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(20000);

        // Configura os parâmetros da requisição para deletar a tarefa
        RequestParams params = new RequestParams();
        params.put("taskId", taskId);

        // Faz a requisição POST para deletar a tarefa
        client.post("http://10.0.2.2:45455/DeleteTask.aspx", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Toast.makeText(ShowActivity.this, "Tarefa deletada com sucesso.", Toast.LENGTH_SHORT).show();
                // Atualiza a lista de tarefas
                fetchTasks(Integer.parseInt(preferences.getString("userId", "")));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(ShowActivity.this, "Falha ao conectar ao servidor.", Toast.LENGTH_SHORT).show();
                Log.e("ERROR", "Falha na requisição: " + error.getMessage());
            }
        });
    }

    // Método para realizar o logout
    public void logoutClick(View view) {
        // Limpa o SharedPreferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("userId");
        editor.apply();

        // Volta para a MainActivity (tela de login)
        Intent intent = new Intent(ShowActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
