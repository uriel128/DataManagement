package viewmodel;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.UserSession;
import util.ValidationUtils;

public class LoginController {

    @FXML
    private GridPane rootpane;
    @FXML
    private TextField usernameTextField;
    @FXML
    private PasswordField passwordField;

    public void initialize() {
        rootpane.setBackground(new Background(new BackgroundFill(
                Color.web("#f6efe3"),
                CornerRadii.EMPTY,
                javafx.geometry.Insets.EMPTY)));

        rootpane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.2), rootpane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        usernameTextField.setText(UserSession.getLastSignedInUsername());
    }

    @FXML
    public void login(ActionEvent actionEvent) {
        String username = usernameTextField.getText() == null ? "" : usernameTextField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (!ValidationUtils.isValidUsername(username)) {
            showError("Invalid username format.",
                    "Username must be 5-30 chars, start with a letter, and may include '.' or '_' internally.");
            return;
        }

        if (password.isBlank()) {
            showError("Password is required.", "Enter your password to sign in.");
            return;
        }

        String privileges = UserSession.authenticate(username, password);
        if (privileges == null) {
            showError("Sign-in failed.", "Account not found or password is incorrect. Create an account first.");
            return;
        }

        UserSession.createSession(username, password, privileges);

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/db_interface_gui.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            showError("Could not open dashboard.", e.getMessage());
        }
    }

    @FXML
    public void signUp(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/signUp.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            showError("Could not open sign-up page.", e.getMessage());
        }
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Campus Student Registry");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
