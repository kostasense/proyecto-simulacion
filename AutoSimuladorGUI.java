import javafx.application.Application;
import javafx.geometry.Insets;
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
    private Queue<Auto> colaTaller = new LinkedList<>();
    private List<Auto> taller = new ArrayList<>();

    private Random rand = new Random();
    private int semanaActual = 0;
    private int semanasSimulacion = 52;
    private double gananciaTotal = 0;
    private double capitalActual = 0;

    private TextArea log;
    private XYChart.Series<Number, Number> gananciaSeries;
    private XYChart.Series<Number, Number> capitalSeries;
    private XYChart.Series<Number, Number> entradaTallerSeries;
    private XYChart.Series<Number, Number> salidaTallerSeries;
    private XYChart.Series<Number, Number> entradaColaSeries;
    private XYChart.Series<Number, Number> salidaColaSeries;

    private Slider capitalInicialSlider, costoMaxSlider, gananciaSlider, autosMaxInventarioSlider;
    private Label capitalInicialLabel, costoMaxLabel, gananciaLabel, autosMaxInventarioLabel;

    static class Auto {
        int semanaCompra;
        double costo;
        boolean vendido = false;
        boolean intentadoDirecto = false;
        boolean enTaller = false;
        boolean reparado = false;
        int semanasEnInventario = 0;
        int semanasEnTaller = 0;
        int semanasParaReparacion = 0;
        int semanasPostReparado = 0;
        boolean esperandoTaller = false;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Simulador de Compra-Venta de Autos");

        // Cuadrante 1: Logs
        log = new TextArea();
        log.setEditable(false);

        // Cuadrante 2: Configuraciones
        capitalInicialSlider = createSlider(0, 500000, 250000, true);
        costoMaxSlider = createSlider(0, 100000, 50000, true);
        gananciaSlider = createSlider(0, 100, 50, true);
        autosMaxInventarioSlider = createSlider(0, 50, 20, true);

        capitalInicialLabel = new Label();
        costoMaxLabel = new Label();
        gananciaLabel = new Label();
        autosMaxInventarioLabel = new Label();

        capitalInicialSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            capitalInicialLabel.setText("Capital Inicial: $" + newVal.intValue());
            capitalActual = newVal.doubleValue();
        });

        costoMaxSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                costoMaxLabel.setText("Costo Máximo de Compra: $" + newVal.intValue()));

        gananciaSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                gananciaLabel.setText("Porcentaje de Ganancia: " + newVal.intValue() + "%"));

        autosMaxInventarioSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                autosMaxInventarioLabel.setText("Autos máximos en inventario: " + newVal.intValue()));

        capitalInicialLabel.setText("Capital Inicial: $" + (int) capitalInicialSlider.getValue());
        costoMaxLabel.setText("Costo Máximo de Compra: $" + (int) costoMaxSlider.getValue());
        gananciaLabel.setText("Porcentaje de Ganancia: " + (int) gananciaSlider.getValue() + "%");
        autosMaxInventarioLabel.setText("Autos máximos en inventario: " + (int) autosMaxInventarioSlider.getValue());

        Button simularBtn = new Button("Simular Año");
        simularBtn.setOnAction(e -> {
            resetSimulacion();
            simularAnio();
        });

        VBox controls = new VBox(15,
                new Label("Configuración Inicial"),
                new VBox(5, capitalInicialLabel, capitalInicialSlider),
                new VBox(5, costoMaxLabel, costoMaxSlider),
                new VBox(5, gananciaLabel, gananciaSlider),
                new VBox(5, autosMaxInventarioLabel, autosMaxInventarioSlider),
                simularBtn
        );
        controls.setPadding(new Insets(10));

        // Cuadrante 3: Gráfica de Evolución Financiera
        NumberAxis xAxis1 = new NumberAxis();
        xAxis1.setLabel("Semana");
        NumberAxis yAxis1 = new NumberAxis();
        yAxis1.setLabel("Ganancia / Capital");
        LineChart<Number, Number> lineChartFinanzas = new LineChart<>(xAxis1, yAxis1);
        gananciaSeries = new XYChart.Series<>();
        capitalSeries = new XYChart.Series<>();
        gananciaSeries.setName("Ganancia");
        capitalSeries.setName("Capital");
        lineChartFinanzas.getData().addAll(gananciaSeries, capitalSeries);
        lineChartFinanzas.setTitle("Evolución Financiera");

        // Cuadrante 4: Gráfica de Flujo de Autos
        NumberAxis xAxis2 = new NumberAxis();
        xAxis2.setLabel("Semana");
        NumberAxis yAxis2 = new NumberAxis();
        yAxis2.setLabel("Cantidad de Autos");
        LineChart<Number, Number> lineChartTallerCola = new LineChart<>(xAxis2, yAxis2);
        entradaTallerSeries = new XYChart.Series<>();
        salidaTallerSeries = new XYChart.Series<>();
        entradaColaSeries = new XYChart.Series<>();
        salidaColaSeries = new XYChart.Series<>();
        entradaTallerSeries.setName("Entradas a Taller");
        salidaTallerSeries.setName("Salidas de Taller");
        entradaColaSeries.setName("Entradas a Cola");
        salidaColaSeries.setName("Salidas de Cola");
        lineChartTallerCola.getData().addAll(entradaTallerSeries, salidaTallerSeries, entradaColaSeries, salidaColaSeries);
        lineChartTallerCola.setTitle("Flujo de Autos: Taller y Cola");

        GridPane gridPane = new GridPane();
        gridPane.add(log, 0, 0);
        gridPane.add(controls, 1, 0);
        gridPane.add(lineChartFinanzas, 0, 1);
        gridPane.add(lineChartTallerCola, 1, 1);

        // Configurar restricciones de filas y columnas para que los cuadrantes se expandan por igual
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        gridPane.getColumnConstraints().addAll(col1, col2);

        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(50);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(50);
        gridPane.getRowConstraints().addAll(row1, row2);

        Scene scene = new Scene(gridPane, 1200, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Slider createSlider(double min, double max, double initial, boolean showTicks) {
        Slider slider = new Slider(min, max, initial);
        slider.setBlockIncrement(1);
        slider.setMajorTickUnit((max - min) / 5);
        slider.setMinorTickCount(4);
        slider.setSnapToTicks(true);
        slider.setShowTickLabels(showTicks);
        slider.setShowTickMarks(showTicks);
        return slider;
    }

    private void resetSimulacion() {
        semanaActual = 0;
        gananciaTotal = 0;
        capitalActual = capitalInicialSlider.getValue();
        inventario.clear();
        taller.clear();
        colaTaller.clear();
        gananciaSeries.getData().clear();
        capitalSeries.getData().clear();
        entradaTallerSeries.getData().clear();
        salidaTallerSeries.getData().clear();
        entradaColaSeries.getData().clear();
        salidaColaSeries.getData().clear();
        log.clear();
    }

    private void simularAnio() {
        for (semanaActual = 1; semanaActual <= semanasSimulacion; semanaActual++) {
            int entradasTaller = 0, salidasTaller = 0, entradasCola = 0, salidasCola = 0;

            // Comprar autos
            int autosAComprar = rand.nextInt(2) + 1;
            for (int i = 0; i < autosAComprar; i++) {
                if (inventario.size() >= autosMaxInventarioSlider.getValue()) break;
                Auto auto = new Auto();
                auto.semanaCompra = semanaActual;
                auto.costo = 40000 + rand.nextInt(20001);

                if (auto.costo <= costoMaxSlider.getValue() && capitalActual >= auto.costo) {
                    capitalActual -= auto.costo;
                    inventario.add(auto);
                    log.appendText("Semana " + semanaActual + ": Comprado auto por $" + auto.costo + "\n");

                    if (rand.nextDouble() < 0.2) {
                        double gananciaBase = 10000 + rand.nextInt(3001);
                        double porcentajeGanancia = gananciaSlider.getValue() / 100.0;
                        double ganancia = gananciaBase * porcentajeGanancia;
                        double restante = gananciaBase - ganancia;
                        gananciaTotal += ganancia;
                        capitalActual += restante;
                        auto.vendido = true;
                        log.appendText("→ Vendido directamente (sin reparar). Ganancia base: $" + gananciaBase +
                                    " | Ganancia total: $" + ganancia + " | Capital añadido: $" + restante + "\n");
                    } else {
                        auto.intentadoDirecto = true;
                    }
                }
            }

            // Procesar taller
            Iterator<Auto> it = taller.iterator();
            while (it.hasNext()) {
                Auto auto = it.next();
                auto.semanasEnTaller++;
                if (auto.semanasEnTaller >= auto.semanasParaReparacion) {
                    auto.reparado = true;
                    auto.enTaller = false;
                    auto.semanasPostReparado = rand.nextInt(2) + 1;
                    log.appendText("Semana " + semanaActual + ": Auto reparado\n");
                    it.remove();
                    salidasTaller++;
                }
            }

            // Agregar autos de la cola al taller
            while (taller.size() < 4 && !colaTaller.isEmpty()) {
                Auto siguiente = colaTaller.poll();
                salidasCola++;
                if (rand.nextDouble() < 0.10) {
                    double gananciaBase = 10000 + rand.nextInt(3001);
                    double porcentajeGanancia = gananciaSlider.getValue() / 100.0;
                    double ganancia = gananciaBase * porcentajeGanancia;
                    double restante = gananciaBase - ganancia;
                    gananciaTotal += ganancia;
                    capitalActual += restante;
                    siguiente.vendido = true;
                    log.appendText("→ Vendido directamente (sin reparar) desde cola. Ganancia base: $" + gananciaBase +
                                " | Ganancia total: $" + ganancia + " | Capital añadido: $" + restante + "\n");
                } else {
                    int reparacion = 20000 + rand.nextInt(5001);
                    if (capitalActual >= reparacion) {
                        capitalActual -= reparacion;
                        siguiente.costo += reparacion;
                        siguiente.enTaller = true;
                        siguiente.semanasParaReparacion = 3 + rand.nextInt(2);
                        taller.add(siguiente);
                        entradasTaller++;
                        log.appendText("→ Entró a taller por $" + reparacion + "\n");
                    } else {
                        log.appendText("→ Sin dinero para reparar auto\n");
                    }
                }
            }

            // Vender autos reparados
            for (Auto auto : inventario) {
                if (auto.vendido) continue;
                auto.semanasEnInventario++;

                if (auto.reparado && auto.semanasPostReparado-- <= 0) {
                    double gananciaBase = 15000 + rand.nextInt(5001);
                    double porcentajeGanancia = gananciaSlider.getValue() / 100.0;
                    double ganancia = gananciaBase * porcentajeGanancia;
                    double restante = gananciaBase - ganancia;
                    gananciaTotal += ganancia;
                    capitalActual += restante;
                    auto.vendido = true;
                    log.appendText("→ Auto reparado vendido. Ganancia base: $" + gananciaBase +
                                " | Ganancia total: $" + ganancia + " | Capital añadido: $" + restante + "\n");
                } else if (auto.intentadoDirecto && !auto.reparado && !auto.enTaller && !auto.esperandoTaller) {
                    auto.esperandoTaller = true;
                    colaTaller.add(auto);
                    entradasCola++;
                    log.appendText("→ Auto enviado a cola de taller\n");
                }
            }

            gananciaSeries.getData().add(new XYChart.Data<>(semanaActual, gananciaTotal));
            capitalSeries.getData().add(new XYChart.Data<>(semanaActual, capitalActual));

            entradaTallerSeries.getData().add(new XYChart.Data<>(semanaActual, entradasTaller));
            salidaTallerSeries.getData().add(new XYChart.Data<>(semanaActual, salidasTaller));
            entradaColaSeries.getData().add(new XYChart.Data<>(semanaActual, entradasCola));
            salidaColaSeries.getData().add(new XYChart.Data<>(semanaActual, salidasCola));
        }

        log.appendText("\n=== SIMULACIÓN COMPLETA ===\n");
        log.appendText("Ganancia neta final: $" + gananciaTotal + "\n");
        log.appendText("Capital final disponible: $" + capitalActual + "\n");
    }
}