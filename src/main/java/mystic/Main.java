
package mystic;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Stage;

import mystic.common.Constants;
import mystic.common.GameState;
import mystic.engine.CharacterSelectScreen;
import mystic.engine.CombatScreen;
import mystic.engine.GameEngine;
import mystic.engine.InputManager;
import mystic.engine.MainMenuScreen;
import mystic.engine.ResultScreen;
import mystic.engine.SettingsScreen;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(
                Constants.WIDTH,
                Constants.HEIGHT);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        InputManager input = new InputManager();
        GameEngine engine = new GameEngine(gc, input);

        // Register game screens.
        engine.register(
                GameState.MENU,
                new MainMenuScreen(engine));

        engine.register(
                GameState.CHARACTER_SELECT,
                new CharacterSelectScreen(engine));

        engine.register(
                GameState.COMBAT,
                new CombatScreen(engine));

        engine.register(
                GameState.RESULT,
                new ResultScreen(engine));
        engine.register(
                GameState.SETTINGS,
                new SettingsScreen(engine));        
                
        Scene scene = new Scene(new Group(canvas));

        input.attach(scene);

        // Clear pressed keys when the window loses focus.
        stage.focusedProperty().addListener(
                (obs, wasFocused, isFocused) -> {
                    if (!isFocused) {
                        input.clear();
                    }
                });

        stage.setTitle("Mystic Kombat");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        engine.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
