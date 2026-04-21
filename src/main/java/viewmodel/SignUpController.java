package viewmodel;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.UserSession;
import util.ValidationUtils;

public class SignUpController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private ComboBox<String> privilegesCombo;

    @FXML
    public void initialize() {
        privilegesCombo.getItems().addAll("STUDENT", "ADMIN", "VIEWER");
        privilegesCombo.getSelectionModel().selectFirst();
    }

    @FXML
    public void createNewAccount(ActionEvent actionEvent) {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();
        String privileges = privilegesCombo.getValue();

        if (!ValidationUtils.isValidUsername(username)) {
            showAlert(Alert.AlertType.ERROR,
                    "Invalid username",
                    "Use 5-30 chars, start with a letter, and keep '.'/'_' away from the ends.");
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            showAlert(Alert.AlertType.ERROR,
                    "Weak password",
                    "Use 10-64 chars with uppercase, lowercase, number, and special symbol.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR,
                    "Password mismatch",
                    "Password and confirm password must match.");
            return;
        }

        if (UserSession.userExists(username)) {
            showAlert(Alert.AlertType.ERROR,
                    "Account exists",
                    "This username already exists. Choose a different username.");
            return;
        }

        boolean created = UserSession.registerUser(username, password, privileges);
        if (!created) {
            showAlert(Alert.AlertType.ERROR,
                    "Sign-up failed",
                    "Could not create account at this time.");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION,
                "Account created",
                "Your account was created successfully. You can sign in now.");
        goBack(actionEvent);
    }

    @FXML
    public void goBack(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("Campus Student Registry");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
