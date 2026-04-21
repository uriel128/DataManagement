package viewmodel;

import dao.DbConnectivityClass;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Major;
import model.Person;
import service.UserSession;
import util.CsvUtils;
import util.PdfReportUtil;
import util.ValidationUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class DB_GUI_Controller implements Initializable {
    private static final String APP_NAME = "Campus Student Registry";

    @FXML
    private TextField first_name;
    @FXML
    private TextField last_name;
    @FXML
    private TextField department;
    @FXML
    private ComboBox<Major> majorCombo;
    @FXML
    private TextField email;
    @FXML
    private TextField imageURL;
    @FXML
    private ImageView img_view;
    @FXML
    private Label statusLabel;
    @FXML
    private MenuBar menuBar;

    @FXML
    private Button addBtn;
    @FXML
    private Button editBtn;
    @FXML
    private Button deleteBtn;

    @FXML
    private MenuItem editItem;
    @FXML
    private MenuItem deleteItem;
    @FXML
    private MenuItem clearItem;
    @FXML
    private MenuItem importCsvItem;
    @FXML
    private MenuItem exportCsvItem;
    @FXML
    private MenuItem reportMajorItem;

    @FXML
    private TableView<Person> tv;
    @FXML
    private TableColumn<Person, Integer> tv_id;
    @FXML
    private TableColumn<Person, String> tv_fn;
    @FXML
    private TableColumn<Person, String> tv_ln;
    @FXML
    private TableColumn<Person, String> tv_department;
    @FXML
    private TableColumn<Person, Major> tv_major;
    @FXML
    private TableColumn<Person, String> tv_email;

    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        majorCombo.setItems(FXCollections.observableArrayList(Major.values()));

        configureColumns();
        configureStateBindings();
        configureValidationListeners();

        data.setAll(cnUtil.getData());
        addPlaceholderRowIfMissing();
        tv.setItems(data);

        tv.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> populateFormFromSelection(newVal));

        String lastUser = UserSession.getLastSignedInUsername();
        if (!lastUser.isBlank()) {
            setStatus("Welcome " + lastUser + ". Active DB mode: " + cnUtil.getActiveDatabaseMode(), false);
        } else {
            setStatus("Active DB mode: " + cnUtil.getActiveDatabaseMode(), false);
        }
    }

    private void configureColumns() {
        tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
        tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
        tv_major.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getMajor()));
        tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));

        tv.setEditable(true);
        tv_fn.setCellFactory(TextFieldTableCell.forTableColumn());
        tv_ln.setCellFactory(TextFieldTableCell.forTableColumn());
        tv_department.setCellFactory(TextFieldTableCell.forTableColumn());
        tv_email.setCellFactory(TextFieldTableCell.forTableColumn());
        tv_major.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(Major.values())));

        tv_fn.setOnEditCommit(event -> commitNameEdit(event, true));
        tv_ln.setOnEditCommit(event -> commitNameEdit(event, false));
        tv_department.setOnEditCommit(this::commitDepartmentEdit);
        tv_email.setOnEditCommit(this::commitEmailEdit);
        tv_major.setOnEditCommit(this::commitMajorEdit);
    }

    private void configureStateBindings() {
        BooleanBinding noEditableSelection = Bindings.createBooleanBinding(() -> {
            Person selected = tv.getSelectionModel().getSelectedItem();
            return selected == null || selected.isPlaceholderRow();
        }, tv.getSelectionModel().selectedItemProperty());

        editBtn.disableProperty().bind(noEditableSelection);
        deleteBtn.disableProperty().bind(noEditableSelection);
        editItem.disableProperty().bind(noEditableSelection);
        deleteItem.disableProperty().bind(noEditableSelection);

        BooleanBinding formValid = Bindings.createBooleanBinding(this::isFormValid,
                first_name.textProperty(),
                last_name.textProperty(),
                department.textProperty(),
                email.textProperty(),
                imageURL.textProperty(),
                majorCombo.valueProperty());

        addBtn.disableProperty().bind(formValid.not());
    }

    private void configureValidationListeners() {
        first_name.textProperty().addListener((obs, oldText, newText) -> updateValidationStyles());
        last_name.textProperty().addListener((obs, oldText, newText) -> updateValidationStyles());
        department.textProperty().addListener((obs, oldText, newText) -> updateValidationStyles());
        email.textProperty().addListener((obs, oldText, newText) -> updateValidationStyles());
        imageURL.textProperty().addListener((obs, oldText, newText) -> updateValidationStyles());
        majorCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateValidationStyles());
    }

    private void updateValidationStyles() {
        applyValidationStyle(first_name, ValidationUtils.isValidName(first_name.getText()));
        applyValidationStyle(last_name, ValidationUtils.isValidName(last_name.getText()));
        applyValidationStyle(department, ValidationUtils.isValidDepartment(department.getText()));
        applyValidationStyle(email, ValidationUtils.isValidEmail(email.getText()));
        applyValidationStyle(imageURL, ValidationUtils.isValidImageUrl(imageURL.getText()));
        applyValidationStyle(majorCombo, ValidationUtils.isValidMajor(majorCombo.getValue()));
    }

    private void applyValidationStyle(javafx.scene.control.Control control, boolean valid) {
        if (valid) {
            control.setStyle("");
        } else {
            control.setStyle("-fx-border-color: #c62828; -fx-border-width: 2;");
        }
    }

    private boolean isFormValid() {
        return ValidationUtils.isValidName(first_name.getText())
                && ValidationUtils.isValidName(last_name.getText())
                && ValidationUtils.isValidDepartment(department.getText())
                && ValidationUtils.isValidMajor(majorCombo.getValue())
                && ValidationUtils.isValidEmail(email.getText())
                && ValidationUtils.isValidImageUrl(imageURL.getText());
    }

    @FXML
    protected void addNewRecord() {
        if (!isFormValid()) {
            setStatus("Cannot add record. Fix highlighted fields first.", true);
            return;
        }

        Person person = new Person(
                first_name.getText().trim(),
                last_name.getText().trim(),
                department.getText().trim(),
                majorCombo.getValue(),
                email.getText().trim(),
                imageURL.getText().trim()
        );

        int generatedId = cnUtil.insertUser(person);
        if (generatedId > 0) {
            person.setId(generatedId);
            data.add(Math.max(0, data.size() - 1), person);
            clearForm();
            setStatus("Record added successfully.", false);
        } else {
            setStatus("Add failed. Email may already exist or DB is unavailable.", true);
        }
    }

    @FXML
    protected void clearForm() {
        first_name.clear();
        last_name.clear();
        department.clear();
        majorCombo.getSelectionModel().clearSelection();
        email.clear();
        imageURL.clear();
        tv.getSelectionModel().clearSelection();
        updateValidationStyles();
        setStatus("Form cleared.", false);
    }

    @FXML
    protected void logOut(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Log out from current session?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm Log Out");
        Optional<ButtonType> response = alert.showAndWait();
        if (response.isEmpty() || response.get() != ButtonType.YES) {
            return;
        }

        UserSession.clearSession();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            setStatus("Failed to log out: " + e.getMessage(), true);
        }
    }

    @FXML
    protected void closeApplication() {
        System.exit(0);
    }

    @FXML
    protected void displayAbout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/about.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 500);
            stage.setTitle(APP_NAME + " - About");
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            setStatus("Unable to open About window.", true);
        }
    }

    @FXML
    protected void editRecord() {
        Person selected = tv.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isPlaceholderRow()) {
            setStatus("Select a record to edit.", true);
            return;
        }

        if (!isFormValid()) {
            setStatus("Cannot edit record. Fix highlighted fields first.", true);
            return;
        }

        selected.setFirstName(first_name.getText().trim());
        selected.setLastName(last_name.getText().trim());
        selected.setDepartment(department.getText().trim());
        selected.setMajor(majorCombo.getValue());
        selected.setEmail(email.getText().trim());
        selected.setImageURL(imageURL.getText().trim());

        if (cnUtil.editUser(selected.getId(), selected)) {
            tv.refresh();
            setStatus("Record updated successfully.", false);
        } else {
            setStatus("Update failed. Check unique email/server state.", true);
        }
    }

    @FXML
    protected void deleteRecord() {
        Person selected = tv.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isPlaceholderRow()) {
            setStatus("Select a record to delete.", true);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete selected record for " + selected.getFirstName() + " " + selected.getLastName() + "?",
                ButtonType.YES,
                ButtonType.NO);
        alert.setTitle("Confirm Delete");
        Optional<ButtonType> response = alert.showAndWait();

        if (response.isEmpty() || response.get() != ButtonType.YES) {
            return;
        }

        if (cnUtil.deleteRecord(selected)) {
            data.remove(selected);
            addPlaceholderRowIfMissing();
            clearForm();
            setStatus("Record deleted.", false);
        } else {
            setStatus("Delete failed.", true);
        }
    }

    @FXML
    protected void showImage() {
        File file = (new FileChooser()).showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            imageURL.setText(file.toURI().toString());
            img_view.setImage(new Image(file.toURI().toString()));
            updateValidationStyles();
        }
    }

    @FXML
    protected void addRecord() {
        clearForm();
        Person placeholder = getPlaceholderRow();
        if (placeholder != null) {
            tv.getSelectionModel().select(placeholder);
            tv.scrollTo(placeholder);
        }
        setStatus("Enter data in form, or edit the empty table row to add a new record.", false);
    }

    @FXML
    protected void selectedItemTV(MouseEvent mouseEvent) {
        Person selected = tv.getSelectionModel().getSelectedItem();
        populateFormFromSelection(selected);
    }

    @FXML
    protected void importCsvFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showOpenDialog(menuBar.getScene().getWindow());
        if (file == null) {
            return;
        }

        int inserted = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                if (firstLine && line.toLowerCase().contains("first_name")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                Person person = CsvUtils.fromCsvLine(line);
                if (person == null || !isPersonValid(person)) {
                    skipped++;
                    continue;
                }

                int generatedId = cnUtil.insertUser(person);
                if (generatedId > 0) {
                    person.setId(generatedId);
                    data.add(Math.max(0, data.size() - 1), person);
                    inserted++;
                } else {
                    skipped++;
                }
            }
            addPlaceholderRowIfMissing();
            setStatus("CSV import complete. Inserted: " + inserted + ", Skipped: " + skipped, false);
        } catch (IOException e) {
            setStatus("CSV import failed: " + e.getMessage(), true);
        }
    }

    @FXML
    protected void exportCsvFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export CSV");
        chooser.setInitialFileName("student_registry_export.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(menuBar.getScene().getWindow());
        if (file == null) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("first_name,last_name,department,major,email,imageURL");
            writer.newLine();

            for (Person person : data) {
                if (person.isPlaceholderRow()) {
                    continue;
                }
                writer.write(CsvUtils.toCsvLine(person));
                writer.newLine();
            }

            setStatus("CSV exported successfully to " + file.getName(), false);
        } catch (IOException e) {
            setStatus("CSV export failed: " + e.getMessage(), true);
        }
    }

    @FXML
    protected void generateMajorReport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Generate Major Report (PDF)");
        chooser.setInitialFileName("major_report.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(menuBar.getScene().getWindow());
        if (file == null) {
            return;
        }

        Map<Major, Integer> counts = new EnumMap<>(Major.class);
        for (Major major : Major.values()) {
            counts.put(major, 0);
        }

        for (Person person : data) {
            if (!person.isPlaceholderRow() && person.getMajor() != null) {
                counts.put(person.getMajor(), counts.get(person.getMajor()) + 1);
            }
        }

        try {
            PdfReportUtil.writeMajorSummary(file, APP_NAME, counts);
            setStatus("PDF report generated: " + file.getName(), false);
        } catch (IOException e) {
            setStatus("PDF report generation failed: " + e.getMessage(), true);
        }
    }

    public void lightTheme(ActionEvent actionEvent) {
        try {
            Scene scene = menuBar.getScene();
            Stage stage = (Stage) scene.getWindow();
            stage.getScene().getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            setStatus("Could not switch to light theme.", true);
        }
    }

    public void darkTheme(ActionEvent actionEvent) {
        try {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/darkTheme.css").toExternalForm());
        } catch (Exception e) {
            setStatus("Could not switch to dark theme.", true);
        }
    }

    private void populateFormFromSelection(Person person) {
        if (person == null) {
            return;
        }

        if (person.isPlaceholderRow()) {
            first_name.clear();
            last_name.clear();
            department.clear();
            majorCombo.getSelectionModel().clearSelection();
            email.clear();
            imageURL.clear();
            updateValidationStyles();
            setStatus("Editing the empty row enables quick add directly in the table.", false);
            return;
        }

        first_name.setText(person.getFirstName());
        last_name.setText(person.getLastName());
        department.setText(person.getDepartment());
        majorCombo.setValue(person.getMajor());
        email.setText(person.getEmail());
        imageURL.setText(person.getImageURL());

        if (person.getImageURL() != null && !person.getImageURL().isBlank()) {
            try {
                img_view.setImage(new Image(person.getImageURL()));
            } catch (Exception ignored) {
                // keep current image if URL is invalid or unreachable.
            }
        }

        updateValidationStyles();
    }

    private void commitNameEdit(TableColumn.CellEditEvent<Person, String> event, boolean firstNameColumn) {
        Person person = event.getRowValue();
        String oldValue = event.getOldValue();
        String newValue = event.getNewValue() == null ? "" : event.getNewValue().trim();

        if (!ValidationUtils.isValidName(newValue)) {
            restoreEditedValue(person, oldValue, firstNameColumn, false);
            setStatus("Invalid name format.", true);
            tv.refresh();
            return;
        }

        restoreEditedValue(person, newValue, firstNameColumn, false);
        if (!persistEditedRow(person, "Name updated from table.")) {
            restoreEditedValue(person, oldValue, firstNameColumn, false);
            tv.refresh();
        }
    }

    private void commitDepartmentEdit(TableColumn.CellEditEvent<Person, String> event) {
        Person person = event.getRowValue();
        String oldValue = event.getOldValue();
        String newValue = event.getNewValue() == null ? "" : event.getNewValue().trim();

        if (!ValidationUtils.isValidDepartment(newValue)) {
            person.setDepartment(oldValue);
            tv.refresh();
            setStatus("Invalid department format.", true);
            return;
        }

        person.setDepartment(newValue);
        if (!persistEditedRow(person, "Department updated.")) {
            person.setDepartment(oldValue);
            tv.refresh();
        }
    }

    private void commitEmailEdit(TableColumn.CellEditEvent<Person, String> event) {
        Person person = event.getRowValue();
        String oldValue = event.getOldValue();
        String newValue = event.getNewValue() == null ? "" : event.getNewValue().trim();

        if (!ValidationUtils.isValidEmail(newValue)) {
            person.setEmail(oldValue);
            tv.refresh();
            setStatus("Invalid email format.", true);
            return;
        }

        person.setEmail(newValue);
        if (!persistEditedRow(person, "Email updated.")) {
            person.setEmail(oldValue);
            tv.refresh();
        }
    }

    private void commitMajorEdit(TableColumn.CellEditEvent<Person, Major> event) {
        Person person = event.getRowValue();
        Major oldValue = event.getOldValue();
        Major newValue = event.getNewValue();

        if (!ValidationUtils.isValidMajor(newValue)) {
            person.setMajor(oldValue);
            tv.refresh();
            setStatus("Select a valid major.", true);
            return;
        }

        person.setMajor(newValue);
        if (!persistEditedRow(person, "Major updated.")) {
            person.setMajor(oldValue);
            tv.refresh();
        }
    }

    private void restoreEditedValue(Person person, String value, boolean firstName, boolean refreshOnly) {
        if (firstName) {
            person.setFirstName(value);
        } else {
            person.setLastName(value);
        }
        if (refreshOnly) {
            tv.refresh();
        }
    }

    private boolean persistEditedRow(Person person, String successMessage) {
        if (person == null) {
            return false;
        }

        if (person.isPlaceholderRow()) {
            if (isPersonValid(person)) {
                int id = cnUtil.insertUser(person);
                if (id > 0) {
                    person.setId(id);
                    person.setPlaceholderRow(false);
                    addPlaceholderRowIfMissing();
                    setStatus("New row inserted from table edit.", false);
                    return true;
                }
                setStatus("Could not insert table row. Check unique email/data.", true);
                return false;
            }
            setStatus("Continue filling the empty row to insert it.", false);
            return true;
        }

        if (cnUtil.editUser(person.getId(), person)) {
            setStatus(successMessage, false);
            return true;
        }

        setStatus("Could not persist table edit.", true);
        return false;
    }

    private boolean isPersonValid(Person person) {
        return person != null
                && ValidationUtils.isValidName(person.getFirstName())
                && ValidationUtils.isValidName(person.getLastName())
                && ValidationUtils.isValidDepartment(person.getDepartment())
                && ValidationUtils.isValidMajor(person.getMajor())
                && ValidationUtils.isValidEmail(person.getEmail())
                && ValidationUtils.isValidImageUrl(person.getImageURL() == null ? "" : person.getImageURL());
    }

    private void addPlaceholderRowIfMissing() {
        if (getPlaceholderRow() == null) {
            data.add(Person.placeholder());
        }
    }

    private Person getPlaceholderRow() {
        for (Person person : data) {
            if (person.isPlaceholderRow()) {
                return person;
            }
        }
        return null;
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setTextFill(error ? Color.web("#c62828") : Color.web("#1b5e20"));
    }
}
