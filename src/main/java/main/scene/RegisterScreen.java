package main.scene;

import javafx.application.Platform;
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

public class RegisterScreen extends BaseScreen {

    private Button submitButton;
    private TextField loginField;
    private PasswordField passwordField;
    private TextField emailField;
    private TextField ageField;
    private TextField timeField;

    public RegisterScreen(Main main) {
        super(main, "register.fxml");
    }

    @Override
    protected void initViews() {
        submitButton = (Button) scene.lookup("#submit");

        loginField = (TextField) scene.lookup("#login");
        passwordField = (PasswordField) scene.lookup("#password");
        emailField = (TextField) scene.lookup("#email");
        ageField = (TextField) scene.lookup("#age");
        timeField = (TextField) scene.lookup("#time");

        submitButton.setOnAction(event -> {
            String text = loginField.getText();
            String password = passwordField.getText();
            String email = emailField.getText();
            String ageString = ageField.getText();
            String timeString = timeField.getText();

            if (!TextUtils.isEmpty(text) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(ageString) && !TextUtils.isEmpty(timeString)) {
                int age = Integer.parseInt(ageString);
                int time = Integer.parseInt(timeString);
                call(text, password, email, age, time);
            }
        });
    }

    private void call(String login, String password, String email, int age, int time) {
        Call<UserResult> call = restService().createAccount(login, password, email, age, time);
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