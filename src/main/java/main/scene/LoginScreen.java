package main.scene;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import main.Main;
import main.TextUtils;
import main.User;
import main.UserResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginScreen extends BaseScreen {

    private Button loginButton;
    private TextField loginField;
    private PasswordField passwordField;
    private Button createAccountButton;

    public LoginScreen(Main main) {
        super(main, "login.fxml");
    }

    @Override
    protected void initViews() {
        loginButton = (Button) scene.lookup("#loginButton");
        loginField = (TextField) scene.lookup("#login");
        passwordField = (PasswordField) scene.lookup("#password");
        createAccountButton = (Button) scene.lookup("#createAccountButton");

        loginButton.setOnAction(event -> {
            String text = loginField.getText();
            String password = passwordField.getText();

            if (!TextUtils.isEmpty(text) && !TextUtils.isEmpty(password)) {
                loginCall(text, password);
            }
        });

        createAccountButton.setOnAction(event -> {
            main.toRegister();
        });
    }

    private void loginCall(String login, String password) {
        Call<UserResult> call = restService().login(login, password);
        call.enqueue(new Callback<UserResult>() {
            @Override
            public void onResponse(Call<UserResult> call, Response<UserResult> response) {
                if (response != null) {
                    UserResult result = response.body();
                    if (result != null) {
                        if (result.invalid()) {
                            toast(result.error);
                            return;
                        }
                        User.save(result);
                        main.toBluetooth();
                    }
                }
            }

            @Override
            public void onFailure(Call<UserResult> call, Throwable throwable) {
                throwable.printStackTrace();
                toast(throwable.getMessage());
            }
        });
    }
}