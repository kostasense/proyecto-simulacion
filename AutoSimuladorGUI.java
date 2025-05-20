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
    private Queue<Auto> colaVentaReparados = new LinkedList<>();

    private Random rand = new Random();
    private int semanaActual = 0;
    private int semanasSimulacion = 52;
    private double gananciaTotal = 0;
    private double capitalActual = 0;

    private TextArea log;
    private XYChart.Series<Number, Number> gananciaSeries;
    private XYChart.Series<Number, Number> capitalSeries;

    // Series para las nuevas gráficas
    private XYChart.Series<Number, Number> entradaTallerSeries;
    private XYChart.Series<Number, Number> salidaTallerSeries;
    private XYChart.Series<Number, Number> entradaColaTallerSeries;
    private XYChart.Series<Number, Number> salidaColaTallerSeries;
    private XYChart.Series<Number, Number> entradaVentaReparadosSeries;
    private XYChart.Series<Number, Number> salidaVentaReparadosSeries;

    private Slider capitalInicialSlider, costoMaxSlider, gananciaSlider, autosMaxInventarioSlider, capacidadTallerSlider;
    private Label capitalInicialLabel, costoMaxLabel, gananciaLabel, autosMaxInventarioLabel, capacidadTallerLabel;

    private Label autosEnColaTallerLabel;
    private Label autosEnTallerLabel;
    private Label autosEnVentaReparadosLabel;

    // Componentes para el slide de gráficas
    private StackPane chartStackPane;
    private List<LineChart<Number, Number>> flowCharts;
    private int currentChartIndex = 0;

    static class Auto {
        int semanaCompra;
        double costo;
        boolean vendido = false;
        boolean intentadoDirecto = false;
        boolean enTaller = false;
        boolean reparado = false;
        boolean comprometido = false;
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
        capacidadTallerSlider = createSlider(1, 10, 4, true);

        capitalInicialLabel = new Label("Capital Inicial:");
        costoMaxLabel = new Label("Costo Máximo de Compra:");
        gananciaLabel = new Label("Porcentaje de Ganancia:");
        autosMaxInventarioLabel = new Label("Autos máximos en inventario:");
        capacidadTallerLabel = new Label("Capacidad del Taller:");

        autosEnColaTallerLabel = new Label("Autos en cola de taller: 0");
        autosEnTallerLabel = new Label("Autos en taller: 0");
        autosEnVentaReparadosLabel = new Label("Autos en venta (reparados): 0");


        capitalInicialSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            capitalInicialLabel.setText("Capital Inicial: $" + newVal.intValue());
        });

        costoMaxSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                costoMaxLabel.setText("Costo Máximo de Compra: $" + newVal.intValue()));

        gananciaSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                gananciaLabel.setText("Porcentaje de Ganancia: " + newVal.intValue() + "%"));

        autosMaxInventarioSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                autosMaxInventarioLabel.setText("Autos máximos en inventario: " + newVal.intValue()));

        capacidadTallerSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                capacidadTallerLabel.setText("Capacidad del Taller: " + newVal.intValue()));

        capitalInicialLabel.setText("Capital Inicial: $" + (int) capitalInicialSlider.getValue());
        costoMaxLabel.setText("Costo Máximo de Compra: $" + (int) costoMaxSlider.getValue());
        gananciaLabel.setText("Porcentaje de Ganancia: " + (int) gananciaSlider.getValue() + "%");
        autosMaxInventarioLabel.setText("Autos máximos en inventario: " + (int) autosMaxInventarioSlider.getValue());
        capacidadTallerLabel.setText("Capacidad del Taller: " + (int) capacidadTallerSlider.getValue());

        Button simularBtn = new Button("Simular Año");
        simularBtn.setOnAction(e -> {
            resetSimulacion();
            actualizarContadoresGUI();
            simularAnio();
        });
        simularBtn.setStyle("-fx-font-size: 10px;");

        GridPane configPane = new GridPane();
        configPane.setPadding(new Insets(10));
        configPane.setVgap(5);
        configPane.setHgap(10);

        configPane.add(capitalInicialLabel, 0, 0);
        configPane.add(capitalInicialSlider, 1, 0);
        configPane.add(costoMaxLabel, 0, 1);
        configPane.add(costoMaxSlider, 1, 1);
        configPane.add(gananciaLabel, 0, 2);
        configPane.add(gananciaSlider, 1, 2);
        configPane.add(autosMaxInventarioLabel, 0, 3);
        configPane.add(autosMaxInventarioSlider, 1, 3);
        configPane.add(capacidadTallerLabel, 0, 4);
        configPane.add(capacidadTallerSlider, 1, 4);

        configPane.add(autosEnColaTallerLabel, 0, 6, 2, 1);
        configPane.add(autosEnTallerLabel, 0, 7, 2, 1);
        configPane.add(autosEnVentaReparadosLabel, 0, 8, 2, 1);

        configPane.add(simularBtn, 0, 9, 2, 1);

        TitledPane configTitledPane = new TitledPane("Configuración Inicial", configPane);
        configTitledPane.setCollapsible(false);

        VBox controls = new VBox(15, configTitledPane);
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

        // --- Cuadrante 4: Slide de Gráficas de Flujo de Autos ---
        // Gráfica 1: Entradas y Salidas a Taller
        NumberAxis xAxisTaller = new NumberAxis();
        xAxisTaller.setLabel("Semana");
        NumberAxis yAxisTaller = new NumberAxis();
        yAxisTaller.setLabel("Cantidad de Autos");
        LineChart<Number, Number> chartTaller = new LineChart<>(xAxisTaller, yAxisTaller);
        entradaTallerSeries = new XYChart.Series<>();
        salidaTallerSeries = new XYChart.Series<>();
        entradaTallerSeries.setName("Entradas a Taller");
        salidaTallerSeries.setName("Salidas de Taller");
        chartTaller.getData().addAll(entradaTallerSeries, salidaTallerSeries);
        chartTaller.setTitle("Flujo de Autos: Taller");

        // Gráfica 2: Entradas y Salidas a Cola de Taller
        NumberAxis xAxisColaTaller = new NumberAxis();
        xAxisColaTaller.setLabel("Semana");
        NumberAxis yAxisColaTaller = new NumberAxis();
        yAxisColaTaller.setLabel("Cantidad de Autos");
        LineChart<Number, Number> chartColaTaller = new LineChart<>(xAxisColaTaller, yAxisColaTaller);
        entradaColaTallerSeries = new XYChart.Series<>();
        salidaColaTallerSeries = new XYChart.Series<>();
        entradaColaTallerSeries.setName("Entradas a Cola Taller");
        salidaColaTallerSeries.setName("Salidas de Cola Taller");
        chartColaTaller.getData().addAll(entradaColaTallerSeries, salidaColaTallerSeries);
        chartColaTaller.setTitle("Flujo de Autos: Cola de Taller");

        // Gráfica 3: Entradas a Venta de Reparados y Ventas de Reparados
        NumberAxis xAxisVentaReparados = new NumberAxis();
        xAxisVentaReparados.setLabel("Semana");
        NumberAxis yAxisVentaReparados = new NumberAxis();
        yAxisVentaReparados.setLabel("Cantidad de Autos");
        LineChart<Number, Number> chartVentaReparados = new LineChart<>(xAxisVentaReparados, yAxisVentaReparados);
        entradaVentaReparadosSeries = new XYChart.Series<>();
        salidaVentaReparadosSeries = new XYChart.Series<>();
        entradaVentaReparadosSeries.setName("Autos en Venta (Reparados)"); // Representa el tamaño de la cola
        salidaVentaReparadosSeries.setName("Ventas de Reparados");
        chartVentaReparados.getData().addAll(entradaVentaReparadosSeries, salidaVentaReparadosSeries);
        chartVentaReparados.setTitle("Flujo de Autos: Venta de Reparados");

        flowCharts = Arrays.asList(chartTaller, chartColaTaller, chartVentaReparados);
        chartStackPane = new StackPane();
        chartStackPane.getChildren().add(flowCharts.get(currentChartIndex));

        Button prevChartBtn = new Button("<");
        prevChartBtn.setOnAction(e -> showPreviousChart());
        Button nextChartBtn = new Button(">");
        nextChartBtn.setOnAction(e -> showNextChart());

        HBox chartNavigation = new HBox(10, prevChartBtn, nextChartBtn);
        chartNavigation.setPadding(new Insets(5));
        chartNavigation.setAlignment(javafx.geometry.Pos.CENTER);

        VBox flowChartContainer = new VBox();
        flowChartContainer.getChildren().addAll(chartStackPane, chartNavigation);
        VBox.setVgrow(chartStackPane, Priority.ALWAYS); // Make the chart expand

        GridPane gridPane = new GridPane();
        gridPane.add(log, 0, 0);
        gridPane.add(controls, 1, 0);
        gridPane.add(lineChartFinanzas, 0, 1);
        gridPane.add(flowChartContainer, 1, 1); // Add the container with chart and navigation

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

    private void actualizarContadoresGUI() {
        autosEnColaTallerLabel.setText("Autos en cola de taller: " + colaTaller.size());
        autosEnTallerLabel.setText("Autos en taller: " + taller.size());
        autosEnVentaReparadosLabel.setText("Autos en venta (reparados): " + colaVentaReparados.size());
    }

    private void showNextChart() {
        chartStackPane.getChildren().remove(flowCharts.get(currentChartIndex));
        currentChartIndex = (currentChartIndex + 1) % flowCharts.size();
        chartStackPane.getChildren().add(flowCharts.get(currentChartIndex));
    }

    private void showPreviousChart() {
        chartStackPane.getChildren().remove(flowCharts.get(currentChartIndex));
        currentChartIndex = (currentChartIndex - 1 + flowCharts.size()) % flowCharts.size();
        chartStackPane.getChildren().add(flowCharts.get(currentChartIndex));
    }

    private void resetSimulacion() {
        semanaActual = 0;
        gananciaTotal = 0;
        capitalActual = capitalInicialSlider.getValue();
        inventario.clear();
        taller.clear();
        colaTaller.clear();
        colaVentaReparados.clear();
        gananciaSeries.getData().clear();
        capitalSeries.getData().clear();

        // Limpiar todas las series de las gráficas de flujo de autos
        entradaTallerSeries.getData().clear();
        salidaTallerSeries.getData().clear();
        entradaColaTallerSeries.getData().clear();
        salidaColaTallerSeries.getData().clear();
        entradaVentaReparadosSeries.getData().clear();
        salidaVentaReparadosSeries.getData().clear();
        log.clear();
    }

    private void simularAnio() {
        for (semanaActual = 1; semanaActual <= semanasSimulacion; semanaActual++) {
            int entradasTaller = 0, salidasTaller = 0, entradasColaTaller = 0, salidasColaTaller = 0, entradasVentaReparados = 0, salidasVentaReparados = 0;

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
                        capitalActual += (restante + auto.costo);
                        auto.vendido = true;
                        log.appendText("→ Vendido directamente (sin reparar) en " + (auto.costo + gananciaBase) + " | Ganancia base: $" + gananciaBase +
                                " | Ganancia total: $" + ganancia + " | Capital añadido: $" + restante + "\n");
                        inventario.remove(auto);
                    } else {
                        auto.intentadoDirecto = true;
                        auto.esperandoTaller = true;
                        colaTaller.add(auto);
                        entradasColaTaller++;
                        log.appendText("→ Auto enviado a cola de taller\n");
                    }
                }
            }

            // Intentar vender autos en cola de taller
            Iterator<Auto> itCola = colaTaller.iterator();
            while (itCola.hasNext()) {
                Auto autoEnCola = itCola.next();
                if (!autoEnCola.vendido && rand.nextDouble() < 0.10) {
                    double gananciaBase = 8000 + rand.nextInt(2001);
                    double porcentajeGanancia = gananciaSlider.getValue() / 100.0;
                    double ganancia = gananciaBase * porcentajeGanancia;
                    double restante = gananciaBase - ganancia;
                    gananciaTotal += ganancia;
                    capitalActual += (restante + autoEnCola.costo);
                    autoEnCola.vendido = true;
                    itCola.remove();
                    salidasColaTaller++;
                    log.appendText("Semana " + semanaActual + ": Auto vendido directamente desde la cola de taller en " + (autoEnCola.costo + gananciaBase) + " | Ganancia base: $" + gananciaBase +
                            " | Ganancia total: $" + ganancia + " | Capital añadido: $" + restante + "\n");
                    inventario.remove(autoEnCola);
                }
            }

            // Procesar taller
            Iterator<Auto> itTaller = taller.iterator();
            while (itTaller.hasNext()) {
                Auto autoEnTaller = itTaller.next();
                autoEnTaller.semanasEnTaller++;

                // Intentar comprometer auto en taller
                if (!autoEnTaller.comprometido && rand.nextDouble() < 0.05) {
                    autoEnTaller.comprometido = true;
                    log.appendText("Semana " + semanaActual + ": Auto en taller comprometido para venta.\n");
                }

                if (autoEnTaller.semanasEnTaller >= autoEnTaller.semanasParaReparacion) {
                    autoEnTaller.reparado = true;
                    autoEnTaller.enTaller = false;
                    autoEnTaller.semanasPostReparado = rand.nextInt(2) + 1;
                    log.appendText("Semana " + semanaActual + ": Auto reparado\n");
                    itTaller.remove();
                    salidasTaller++;
                    if (autoEnTaller.comprometido) {
                        double gananciaBase = 15000 + rand.nextInt(5001);
                        double porcentajeGanancia = gananciaSlider.getValue() / 100.0;
                        double ganancia = gananciaBase * porcentajeGanancia;
                        double restante = gananciaBase - ganancia;
                        gananciaTotal += ganancia;
                        capitalActual += (restante + autoEnTaller.costo);
                        autoEnTaller.vendido = true;
                        log.appendText("→ Auto comprometido vendido al salir del taller en " + (autoEnTaller.costo + gananciaBase) +" | Ganancia base: $" + gananciaBase +
                                " | Ganancia total: $" + ganancia + " | Capital añadido: $" + restante + "\n");
                        salidasVentaReparados++;
                        inventario.remove(autoEnTaller);
                    } else {
                        colaVentaReparados.add(autoEnTaller);
                        entradasVentaReparados++;
                        log.appendText("→ Auto reparado, esperando venta.\n");
                    }
                }
            }

            // Agregar autos de la cola al taller
            while (taller.size() < capacidadTallerSlider.getValue() && !colaTaller.isEmpty()) {
                Auto siguiente = colaTaller.poll();
                salidasColaTaller++;
                int reparacion = 20000 + rand.nextInt(5001);
                if (capitalActual >= reparacion) {
                    capitalActual -= reparacion;
                    siguiente.costo += reparacion;
                    siguiente.enTaller = true;
                    siguiente.semanasParaReparacion = 3 + rand.nextInt(2);
                    taller.add(siguiente);
                    entradasTaller++;
                    log.appendText("→ Auto de la cola entró a taller por $" + reparacion + "\n");
                } else {
                    colaTaller.offer(siguiente);
                    log.appendText("→ Sin dinero para reparar auto de la cola, regresa a la cola.\n");
                    break;
                }
            }

            // Vender autos reparados
            Iterator<Auto> itVentaReparados = colaVentaReparados.iterator();
            while (itVentaReparados.hasNext()) {
                Auto autoReparado = itVentaReparados.next();
                if (!autoReparado.vendido) {
                    autoReparado.semanasEnInventario++;
                    if (rand.nextDouble() < 0.7) {
                        double gananciaBase = 15000 + rand.nextInt(5001);
                        double porcentajeGanancia = gananciaSlider.getValue() / 100.0;
                        double ganancia = gananciaBase * porcentajeGanancia;
                        double restante = gananciaBase - ganancia;
                        gananciaTotal += ganancia;
                        capitalActual += (restante + autoReparado.costo);
                        autoReparado.vendido = true;
                        itVentaReparados.remove();
                        salidasVentaReparados++;
                        log.appendText("Semana " + semanaActual + ": Auto reparado vendido en " + (autoReparado.costo + gananciaBase) + " | Ganancia base: $" + gananciaBase +
                                " | Ganancia total: $" + ganancia + " | Capital añadido: $" + restante + "\n");
                        inventario.remove(autoReparado);
                    }
                }
            }

            actualizarContadoresGUI();

            gananciaSeries.getData().add(new XYChart.Data<>(semanaActual, gananciaTotal));
            capitalSeries.getData().add(new XYChart.Data<>(semanaActual, capitalActual));

            entradaTallerSeries.getData().add(new XYChart.Data<>(semanaActual, entradasTaller));
            salidaTallerSeries.getData().add(new XYChart.Data<>(semanaActual, salidasTaller));
            entradaColaTallerSeries.getData().add(new XYChart.Data<>(semanaActual, entradasColaTaller));
            salidaColaTallerSeries.getData().add(new XYChart.Data<>(semanaActual, salidasColaTaller));
            // For the "Entradas a Venta Reparados" series, we are showing the current size of the queue
            entradaVentaReparadosSeries.getData().add(new XYChart.Data<>(semanaActual, colaVentaReparados.size()));
            salidaVentaReparadosSeries.getData().add(new XYChart.Data<>(semanaActual, salidasVentaReparados));
        }

        log.appendText("\n=== SIMULACIÓN COMPLETA ===\n");
        log.appendText("Ganancia neta final: $" + gananciaTotal + "\n");
        log.appendText("Capital final disponible: $" + capitalActual + "\n");
    }
}