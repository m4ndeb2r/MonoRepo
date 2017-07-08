package nl.ou.dpd.gui.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import nl.ou.dpd.gui.model.Model;
import nl.ou.dpd.gui.model.Project;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * A {@link Controller} for the menu of the application.
 *
 * @author Martin de Boer
 */
public class MenuController extends Controller implements Observer {

    @FXML
    private MenuItem newProject;

    @FXML
    private MenuItem openProject;

    @FXML
    private Menu recentProjectsMenu;

    @FXML
    private MenuItem saveProject;

    @FXML
    private MenuItem saveProjectAs;

    @FXML
    private MenuItem closeProject;

    @FXML
    private MenuItem exit;

    @FXML
    private MenuItem help;

    @FXML
    private MenuItem about;

    /**
     * Constructs a {@link MenuController} with the specified {@link Model}.
     *
     * @param model the model of the MVC pattern
     */
    public MenuController(Model model) {
        super(model);
        model.addObserver(this);
        ProjectFileHistory.INSTANCE.restore();
    }

    /**
     * Called to initialize a controller after its root element has been completely processed. It sets some of the
     * menu items' state to disabled (the initial state), because those menu items work on open projects, and initially
     * no project has been opened.
     *
     * @param location  The location used to resolve relative paths for the root object, or
     *                  <tt>null</tt> if the location is not known.
     * @param resources The resources used to localize the root object, or <tt>null</tt>
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    /**
     * Opens a new (blank) project. Prompts the user if the project has any changes that might get lost.
     *
     * @param event is ignored
     */
    @FXML
    protected void newProjectAction(ActionEvent event) {
        if (!getModel().hasOpenProject()
                || getModel().canCloseProjectWithoutDataLoss()
                || canCloseWithoutSaving()) {
            getModel().newProject();
        }
    }

    /**
     * Opens a previously saved project. Prompts the user if the project has any changes that might get lost.
     *
     * @param event is ignored
     */
    @FXML
    protected void openProjectAction(ActionEvent event) {
        if (!getModel().hasOpenProject()
                || getModel().canCloseProjectWithoutDataLoss()
                || canCloseWithoutSaving()) {
            try {
                getModel().openProject();
            } catch (FileNotFoundException fnfe) {
                final Alert alert = new CustomAlert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("The file could not be saved");
                alert.setContentText(fnfe.getMessage());
            }
        }
    }

    /**
     * Closes the currently opened project. Prompts the user if the project has any changes that might get lost.
     *
     * @param event is ignored
     */
    @FXML
    protected void closeProjectAction(ActionEvent event) {
        if (!getModel().hasOpenProject()
                || getModel().canCloseProjectWithoutDataLoss()
                || canCloseWithoutSaving()) {
            getModel().closeProject();
        }
    }

    private boolean canCloseWithoutSaving() {
        // Ask user: okay to close project and lose any changes?
        final Alert alert = new CustomAlert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Note: unsaved changes");
        alert.setHeaderText("The current project will be discarded");
        alert.setContentText("Are you sure you want to close the current project and lose unsaved changes?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Saves the currently open project to a file with the same name it was previously stored in. When the project is
     * successfully saved, a confirmation alert message is shown; when an error occurs, an error alter message is shown.
     * When somewhere along the way the action is cancelled, no messages is shown.
     *
     * @param event is ignored
     */
    @FXML
    protected void saveProjectAction(ActionEvent event) {
        boolean success = true;
        String detailMsg = null;
        try {
            if (!getModel().saveProject()) {
                // Cancelled
                return;
            }
        } catch (Exception ex) {
            success = false;
            detailMsg = ex.getMessage();
        }
        // Show success or error message
        showSaveFileAlert(success, detailMsg);
    }

    /**
     * Saves the currently open project to a file with a different name it was previously stored in. When the project is
     * successfully saved, a confirmation alert message is shown; when an error occurs, an error alter message is shown.
     * When somewhere along the way the action is cancelled, no messages is shown.
     *
     * @param event is ignored
     */
    @FXML
    protected void saveProjectAsAction(ActionEvent event) {
        boolean success = true;
        String detailMsg = null;
        try {
            if (!getModel().saveProjectAs()) {
                // Cancelled
                return;
            }
        } catch (Exception ex) {
            success = false;
            detailMsg = ex.getMessage();
        }
        // Show success or error message
        showSaveFileAlert(success, detailMsg);
    }

    private void showSaveFileAlert(boolean success, String detailMsg) {
        Alert alert;
        if (success) {
            alert = new CustomAlert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("File successfully saved");
        } else {
            alert = new CustomAlert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("The file could not be saved");
            if (detailMsg != null) {
                alert.setContentText(detailMsg);
            }
        }
        alert.showAndWait();
    }

    /**
     * Shows information about tha appliication.
     *
     * @param event is ignored
     */
    @FXML
    protected void aboutAction(ActionEvent event) {
        showNotImplementedAlert("About");
    }

    /**
     * Shows help information.
     *
     * @param event is ignored
     */
    @FXML
    protected void helpAction(ActionEvent event) {
        showNotImplementedAlert("Help");
    }

    private void showNotImplementedAlert(String function) {
        Alert alert = new CustomAlert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText("Not implemented yet: " + function);
        alert.setContentText("This function will be implemented in a future version of the application. Our apologies for the inconvenience.");
        alert.showAndWait();
    }

    /**
     * Handles the file > exit action in the menu.
     *
     * @param event is ignored
     */
    @FXML
    protected void exitAction(ActionEvent event) {
        shutdown();
    }

    /**
     * Ends the application gracefully after user confirmation. This method is called from the
     * {@link #exitAction(ActionEvent)} method as well as from the onClose event from the application window.
     */
    public void shutdown() {
        boolean canExit = false;
        if (getModel().hasOpenProject() && !getModel().canCloseProjectWithoutDataLoss()) {
            if (canCloseWithoutSaving()) {
                canExit = true;
            }
        } else if (okayToExit()) {
            canExit = true;
        }
        if (canExit) {
            ProjectFileHistory.INSTANCE.store();
            Platform.exit();
        }
    }

    private boolean okayToExit() {
        // Ask user: okay to close project and lose any changes?
        final Alert alert = new CustomAlert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit application");
        alert.setHeaderText("The application will stop");
        alert.setContentText("Are you sure you want exit the application?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * This method is called when an {@link Observable} calls the {@code notifyObservers} method. At that moment this
     * {@link MenuController}, being an instance of the {@link Observer} interface, will be updated.
     *
     * @param o   the observable object
     * @param arg a {@link Project} that is opened by the user (or {@code null} if none is currently opened.
     */
    @Override
    public void update(Observable o, Object arg) {
        synchronizeFileMenu((Project) arg);
    }

    /**
     * Synchronize the menu with the state of the currently opened {@link Project} and the {@link ProjectFileHistory}.
     *
     * @param project the currently open {@link Project} or {@code null} if no {@link Project} is currently opened.
     */
    private void synchronizeFileMenu(Project project) {
        this.closeProject.setDisable(project == null);
        this.saveProject.setDisable(project == null);
        this.saveProjectAs.setDisable(project == null);

        if (project != null && project.getProjectFile() != null) {
            ProjectFileHistory.INSTANCE.addProjectFile(project.getProjectFile());
        }

        recentProjectsMenu.getItems().removeAll();
        if (recentProjectsMenu.getItems().size() == 0) {
            for (File projectFile : ProjectFileHistory.INSTANCE.getProjectFiles()) {
                if (projectFile.getPath().equals(getModel().getOpenProjectFilePath())) {
                    continue;
                }
                final String menuText = projectFile.getName();
                final MenuItem menuItem = new MenuItem(menuText);
                menuItem.setOnAction(createOpenRecentProjectFileEventHandler(projectFile));
                recentProjectsMenu.getItems().add(menuItem);
            }
        }

        this.recentProjectsMenu.setDisable(this.recentProjectsMenu.getItems().size() == 0);
    }

    /**
     * Creates a handler for the open-recent-project-file event. Recent project files can only be opened whe no project
     * is currently open, unless it can be closed without data loss, or the user confirms it is okay to close the
     * currently open project file and discard any changes made to it.
     *
     * @param projectFile the project file to open
     * @return the created {@link EventHandler}
     */
    private EventHandler<ActionEvent> createOpenRecentProjectFileEventHandler(File projectFile) {
        return new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    if (!getModel().hasOpenProject()
                            || getModel().canCloseProjectWithoutDataLoss()
                            || canCloseWithoutSaving()) {
                        getModel().openProject(projectFile);
                    }
                } catch (FileNotFoundException ex) {
                    Alert alert = new CustomAlert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Project file not found");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            }
        };
    }
}

