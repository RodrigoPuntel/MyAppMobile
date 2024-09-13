package com.unisc.myappmobile;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private EditText txtUsuario, txtSenha;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtUsuario = findViewById(R.id.txtUser);
        txtSenha = findViewById(R.id.txtPwd);

        preferences = getSharedPreferences("Shared", Context.MODE_PRIVATE);
        boolean session = preferences.getBoolean("session", false);

        // Valida login automático
        if (session) {
            String user = preferences.getString("user", "");
            String userId = preferences.getString("userId", ""); // Recupera o userId da sessão

            if (!userId.isEmpty()) { // Verifica se o userId foi recuperado corretamente
                Intent intent = new Intent(MainActivity.this, TaskActivity.class);
                intent.putExtra("usuario", user);
                intent.putExtra("userId", userId); // Passa o userId para TaskActivity
                startActivity(intent);
                finish(); // Encerra a MainActivity após o redirecionamento
            } else {
                showMessage("Falha ao recuperar o ID do usuário.");
            }
        }
    }


    public void loginClick(View view) {

        String user = txtUsuario.getText().toString();
        String pwd = txtSenha.getText().toString();

        if (user.isEmpty() || pwd.isEmpty()) {
            showMessage("Usuário ou senha não preenchidos!");
            return;
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(20000);
        RequestParams params = new RequestParams();
        params.put("username", user);
        params.put("password", pwd);

        client.post("http://10.0.2.2:45455/ApiLogin.aspx", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String response = new String(responseBody);
                    JSONObject jsonResponse = new JSONObject(response);

                    String userId = jsonResponse.getString("userId");

                    // Login automático
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("user", user);
                    editor.putString("userId", userId); // Armazena o userId
                    editor.putBoolean("session", true);
                    editor.apply();

                    // Passagem de parâmetros
                    Intent intent = new Intent(MainActivity.this, TaskActivity.class);
                    intent.putExtra("usuario", user);
                    intent.putExtra("userId", userId);
                    startActivity(intent);

                } catch (JSONException e) {
                    e.printStackTrace();
                    showMessage("Erro ao processar a resposta do servidor.");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (statusCode == 401) {
                    String result = new String(responseBody);
                    showMessage(result);
                } else {
                    showMessage(error.toString());
                }
                Log.d("MOBY", error.toString());
            }
        });
    }

    private void showMessage(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
}
