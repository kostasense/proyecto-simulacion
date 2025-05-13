import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

public class AutoSimuladorGUI extends Application {

    private List<Auto> inventario = new ArrayList<>();
    private Random rand = new Random();
    private int semanaActual = 0;
    private int semanasSimulacion = 52;
    private double gananciaTotal = 0;
    private double capitalActual = 0;

    private TextArea log;
    private XYChart.Series<Number, Number> gananciaSeries;
    private XYChart.Series<Number, Number> capitalSeries;

    private Slider capitalInicialSlider;
    private Slider costoMaxSlider;
    private Slider gananciaSlider;
    //private Slider autosMaxInventarioSlider;
    private Label capitalInicialLabel;
    private Label costoMaxLabel;
    private Label gananciaLabel;
    //private Label autosMaxInventariosLabel;

    static class Auto {
        int semanaCompra;
        double costo;
        boolean intentadoDirecto;
        boolean vendido;
        boolean reparado;
        int semanasEnInventario;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Simulador de Compra-Venta de Autos");

        BorderPane root = new BorderPane();

        log = new TextArea();
        log.setEditable(false);
        log.setPrefHeight(350);

        capitalInicialSlider = createSlider(0, 500000, 250000, true);
        costoMaxSlider = createSlider(0, 100000, 50000, true);
        gananciaSlider = createSlider(0, 100, 50, true);
        //autosMaxInventarioSlider = createSlider(0, 20, 10, true);

        capitalInicialLabel = new Label();
        costoMaxLabel = new Label();
        gananciaLabel = new Label();
        //autosMaxInventariosLabel = new Label();

        capitalInicialSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            capitalInicialLabel.setText("Capital Inicial: $" + newVal.intValue());
            capitalActual = newVal.doubleValue();
        });
        costoMaxSlider.valueProperty().addListener((obs, oldVal, newVal) ->
            costoMaxLabel.setText("Costo Máximo de Compra: $" + newVal.intValue()));
        gananciaSlider.valueProperty().addListener((obs, oldVal, newVal) ->
            gananciaLabel.setText("Porcentaje de Ganancia: " + newVal.intValue() + "%"));
        //autosMaxInventarioSlider.valueProperty().addListener((obs, oldVal, newVal) ->
            //autosMaxInventariosLabel.setText("Autos maximos en inventario: " + newVal.intValue()));

        capitalInicialLabel.setText("Capital Inicial: $" + (int) capitalInicialSlider.getValue());
        costoMaxLabel.setText("Costo Máximo de Compra: $" + (int) costoMaxSlider.getValue());
        gananciaLabel.setText("Porcentaje de Ganancia: " + (int) gananciaSlider.getValue() + "%");
        //autosMaxInventariosLabel.setText("Autos maximos en inventario: " + (int) autosMaxInventarioSlider.getValue());

        Button simularBtn = new Button("Simular Año");
        simularBtn.setOnAction(e -> {
            resetSimulacion();
            simularAnio();
        });

        VBox controls = new VBox(15, new Label("Configuración Inicial"),
            new VBox(5, capitalInicialLabel, capitalInicialSlider),
            new VBox(5, costoMaxLabel, costoMaxSlider),
            new VBox(5, gananciaLabel, gananciaSlider),
            //new VBox(5, autosMaxInventariosLabel, autosMaxInventarioSlider),
            simularBtn);
        controls.setPrefWidth(250);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Semana");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Ganancia / Capital");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Relación Ganancia / Capital");
        gananciaSeries = new XYChart.Series<>();
        capitalSeries = new XYChart.Series<>();
        gananciaSeries.setName("Ganancia");
        capitalSeries.setName("Capital");
        lineChart.getData().add(gananciaSeries);
        lineChart.getData().add(capitalSeries);

        HBox mainLayout = new HBox(log, controls);
        mainLayout.setSpacing(10);
        root.setCenter(mainLayout);
        root.setBottom(lineChart);

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Slider createSlider(double min, double max, double initial, boolean showTicks) {
        Slider slider = new Slider(min, max, initial);
        slider.setBlockIncrement((max - min) / 10);
        slider.setMajorTickUnit((max - min) / 5);
        slider.setMinorTickCount(4);
        slider.setShowTickLabels(showTicks);
        slider.setShowTickMarks(showTicks);
        slider.setSnapToTicks(true);
        slider.setPrefWidth(200);
        return slider;
    }

    private void resetSimulacion() {
        semanaActual = 0;
        gananciaTotal = 0;
        capitalActual = capitalInicialSlider.getValue();
        inventario.clear();
        gananciaSeries.getData().clear();
        capitalSeries.getData().clear();
        log.clear();
    }

    private void simularAnio() {
        for (semanaActual = 1; semanaActual <= semanasSimulacion; semanaActual++) {
            int autosAComprar = rand.nextInt(2) + 1;
            for (int i = 0; i < autosAComprar; i++) {
                Auto auto = new Auto();
                auto.semanaCompra = semanaActual;
                auto.costo = 40000 + rand.nextInt(20001);

                if (auto.costo <= costoMaxSlider.getValue() && capitalActual >= auto.costo) {
                    capitalActual -= auto.costo;
                    inventario.add(auto);
                    log.appendText("Semana " + semanaActual + ": Compra de auto por $" + auto.costo + "\n");
                }
            }

            for (Auto auto : inventario) {
                if (auto.vendido) continue;
                auto.semanasEnInventario++;

                if (auto.intentadoDirecto && !auto.reparado && auto.semanasEnInventario <= 2) {
                    if (rand.nextDouble() < 0.6) {
                        double gananciaBase = 10000 + rand.nextInt(3001);
                        double gananciaFinal = gananciaBase * (gananciaSlider.getValue() / 100);
                        double precioVenta = auto.costo + gananciaBase;

                        capitalActual += auto.costo + (gananciaBase - gananciaFinal);
                        gananciaTotal += gananciaFinal;
                        auto.vendido = true;

                        log.appendText("Semana " + semanaActual + ": Venta de auto por $" + precioVenta +
                            " | Ganancia Final: $" + gananciaFinal + "\n");
                        continue;
                    }
                }

                if (!auto.reparado && auto.semanasEnInventario > 2 && capitalActual >= 25000) {
                    auto.reparado = true;
                    auto.costo += 25000;
                    capitalActual -= 25000;
                    log.appendText("Semana " + semanaActual + ": Reparación de auto por $25000\n");
                }

                if (auto.reparado && auto.semanasEnInventario > 6) {
                    if (rand.nextDouble() < 0.8) {
                        double gananciaBase = 15000 + rand.nextInt(5001);
                        double gananciaFinal = gananciaBase * (gananciaSlider.getValue() / 100);
                        double precioVenta = auto.costo + gananciaBase;

                        capitalActual += auto.costo + (gananciaBase - gananciaFinal);
                        gananciaTotal += gananciaFinal;
                        auto.vendido = true;

                        log.appendText("Semana " + semanaActual + ": Venta de auto reparado por $" + precioVenta +
                            " | Ganancia Final: $" + gananciaFinal + "\n");
                    }
                }
            }

            gananciaSeries.getData().add(new XYChart.Data<>(semanaActual, gananciaTotal));
            capitalSeries.getData().add(new XYChart.Data<>(semanaActual, capitalActual));
        }

        log.appendText("=== SIMULACIÓN COMPLETA ===\n");
        log.appendText("Ganancia neta final: $" + gananciaTotal + "\n");
        log.appendText("Capital final disponible: $" + capitalActual + "\n");
    }
}