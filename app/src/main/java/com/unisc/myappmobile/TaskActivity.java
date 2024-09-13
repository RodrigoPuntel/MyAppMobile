package com.unisc.myappmobile;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import cz.msebera.android.httpclient.Header;

public class TaskActivity extends AppCompatActivity {

    private TextView txtEntrada;
    private EditText txtTaskName, txtTaskDescription;
    private SharedPreferences preferences;
    private String userId; // Agora é uma variável de instância String

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        txtEntrada = findViewById(R.id.txtBemvindo);
        txtTaskName = findViewById(R.id.txtTaskName);
        txtTaskDescription = findViewById(R.id.txtTaskDescription);

        // Recebe o Intent e os dados
        Intent intent = getIntent();
        String user = intent.getStringExtra("usuario");
        userId = intent.getStringExtra("userId"); // Recupera userId como String

        if (userId == null || userId.isEmpty()) {
            showMessage("Usuário não está logado.");
            // Lógica para tratar erro, se necessário
        } else {
            txtEntrada.setText("Bem-vindo " + user);
        }

        // Sair do aplicativo com botão de voltar
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        // Inicializa SharedPreferences, se necessário para outros usos
        preferences = getSharedPreferences("Shared", Context.MODE_PRIVATE);
    }

    public void logoutClick(View view) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("user", "");
        editor.putBoolean("session", false);
        editor.apply();
        finishAffinity();
    }

    public void addTaskClick(View view) {
        String taskName = txtTaskName.getText().toString();
        String taskDescription = txtTaskDescription.getText().toString();

        // Verifica se todos os campos foram preenchidos
        if (taskName.isEmpty() || taskDescription.isEmpty()) {
            showMessage("Nome e descrição da tarefa são obrigatórios!");
            return;
        }

        // Verifica se o userId é válido
        if (userId == null || userId.isEmpty()) {
            showMessage("Usuário não está logado.");
            return;
        }

        // Configura os parâmetros da requisição
        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(20000);
        RequestParams params = new RequestParams();
        params.put("taskname", taskName);
        params.put("taskdescription", taskDescription);
        params.put("userId", userId); // Envia userId como String

        // URL do endpoint para adicionar a tarefa
        String url = "http://10.0.2.2:45455/AddTask.aspx";

        // Envia a requisição POST para o servidor
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                showMessage("Tarefa adicionada com sucesso!");
                txtTaskName.setText("");
                txtTaskDescription.setText("");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                showMessage("Falha ao adicionar tarefa: " + error.getMessage());
                Log.d("MOBY", "Erro: " + error.toString());
            }
        });
    }

    public void showTasksClick(View view) {
        Intent intent = new Intent(TaskActivity.this, ShowActivity.class);
        startActivity(intent);
    }

    private void showMessage(String msg) {
        Toast.makeText(TaskActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
}
