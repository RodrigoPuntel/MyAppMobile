package com.unisc.myappmobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import cz.msebera.android.httpclient.Header;

public class RegisterActivity extends AppCompatActivity {

    private EditText txtUser, txtPwd, txtEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        txtUser = findViewById(R.id.txtUser);
        txtPwd = findViewById(R.id.txtPwd);
        txtEmail = findViewById(R.id.txtEmail);
    }

    // Método para registrar um novo usuário
    public void signInClick(View view) {
        String user = txtUser.getText().toString();
        String pwd = txtPwd.getText().toString();
        String email = txtEmail.getText().toString();

        if (user.isEmpty() || pwd.isEmpty() || email.isEmpty()) {
            showMessage("Todos os campos devem ser preenchidos!");
            return;
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(20000);
        RequestParams params = new RequestParams();
        params.put("username", user);
        params.put("password", pwd);
        params.put("emailadress", email);

        client.post("http://10.0.2.2:45455/AddUser.aspx", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (statusCode == 200) {
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    showMessage("Erro ao registrar usuário: " + new String(responseBody));
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (statusCode == 409) {
                    showMessage("Nome de usuário ou e-mail já existente.");
                } else {
                    showMessage("Erro na comunicação com o servidor: " + error.toString());
                }
            }
        });
    }

    // Já está cadastrado?
    public void toMainClick(View view) {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showMessage(String msg) {
        Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
}


