
package mystic;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(1280, 720);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.rgb(12, 12, 24));
        gc.fillRect(0, 0, 1280, 720);

        gc.setFill(Color.CRIMSON);
        gc.fillRect(200, 504, 64, 96);

        gc.setFill(Color.DODGERBLUE);
        gc.fillRect(1016, 504, 64, 96);

        stage.setTitle("Mystic Kombat");
        stage.setScene(new Scene(new Group(canvas), 1280, 720));
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
