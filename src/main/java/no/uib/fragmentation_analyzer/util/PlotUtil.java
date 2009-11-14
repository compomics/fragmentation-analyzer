package no.uib.fragmentation_analyzer.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

/**
 * Includes help methods that are used when plotting.
 *
 * @author  Harald Barsnes
 */
public class PlotUtil {

    public static final float LINE_WIDTH = 4;

    /**
     * Returns a line plot based on the provided data.
     *
     * @param dataSet
     * @param xAxisLabel
     * @param yAxisLabel
     * @return
     */
    public static JFreeChart getBarPlot(HashMap<String, Integer> data, int numberOfSpectra,
            String xAxisLabel, String yAxisLabel) {

        // sort the keys
        ArrayList<String> sortedKeys = new ArrayList<String>();

        Iterator<String> iterator = data.keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();
            sortedKeys.add(key);
        }

        java.util.Collections.sort(sortedKeys);


        // add the data to the plot
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (int i = 0; i < sortedKeys.size(); i++) {
            String key = sortedKeys.get(i);
            dataset.addValue(data.get(key).doubleValue() / numberOfSpectra, "1", key);
        }


        // create the chart
        JFreeChart chart = ChartFactory.createBarChart(
                null, // title
                xAxisLabel, // xAxisLabel
                yAxisLabel, // yAxisLabel
                dataset, // XYZDataset
                PlotOrientation.VERTICAL, // orientation
                true, // legend
                true, // tooltips
                false); // urls

        chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
        chart.getLegend().setPosition(RectangleEdge.BOTTOM);
        chart.setBackgroundPaint(new Color(225, 225, 225));
        chart.removeLegend();

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        // set the range to only include valid percatage values (and leave some padding at the top)
        plot.getRangeAxis().setRange(0, 1.04);

        // label direction
        plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);

        // make sure that tooltip is generated
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());

        return chart;
    }

    /**
     * Returns a line plot based on the provided data.
     *
     * @param dataSet
     * @param xAxisLabel
     * @param yAxisLabel
     * @return
     */
    public static JFreeChart getAverageLinePlot(HashMap<String, double[][]> data, int[] totalNumberOfSpectraOfGivenLength,
            String xAxisLabel, String yAxisLabel, Properties properties) {
        
        // sort the keys
        ArrayList<String> sortedKeys = new ArrayList<String>();

        Iterator<String> iterator = data.keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();
            sortedKeys.add(key);
        }

        java.util.Collections.sort(sortedKeys);


        // add the data to the plot
        YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();

        for (int i = 0; i < sortedKeys.size(); i++) {

            String key = sortedKeys.get(i);

            double[][] tempArray = data.get(key);
           
            YIntervalSeries tempDataSeries = new YIntervalSeries(key);
 
            for (int j = 1; j < tempArray.length; j++) {

                double averageValue = 0.0;
                double max = Double.MIN_VALUE;
                double min = Double.MAX_VALUE;

                for(int k=0; k<tempArray[j].length; k++){
                    if(!new Double(tempArray[j][k]).isNaN()){

                        if(tempArray[j][k] > max){
                            max = tempArray[j][k];
                        }

                        if(tempArray[j][k] < min){
                            min = tempArray[j][k];
                        }

                        averageValue += tempArray[j][k];
                    }
                }

                averageValue /= totalNumberOfSpectraOfGivenLength[j];
                tempDataSeries.add(j, averageValue, min, max);
            }

            dataset.addSeries(tempDataSeries);
        }


        // create the chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                null, // title
                xAxisLabel, // xAxisLabel
                yAxisLabel, // yAxisLabel
                dataset, // XYZDataset
                PlotOrientation.VERTICAL, // orientation
                true, // legend
                true, // tooltips
                false); // urls

        chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
        chart.getLegend().setPosition(RectangleEdge.BOTTOM);
        chart.setBackgroundPaint(new Color(225, 225, 225));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        NumberAxis rangeAxis = (NumberAxis) plot.getDomainAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        // set the range to only include valid percatage values (and leave some padding at the top)
        plot.getRangeAxis().setRange(0, 1.04);

        // make sure that tooltip is generated
        XYErrorRenderer renderer = new XYErrorRenderer();
        renderer.setBaseLinesVisible(true);
        renderer.setBaseShapesVisible(false);

        if(properties.showMaxMin()) {
            renderer.setErrorStroke(new BasicStroke(LINE_WIDTH/2));
        } else {
            renderer.setErrorStroke(new BasicStroke(0));
        }
        
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

        // set the data series colors
        for (int i = 0; i < sortedKeys.size(); i++) {
            renderer.setSeriesPaint(i, Util.determineColorOfLine(sortedKeys.get(i)));
        }

        // increase the width of all lines and use dotted lines for the neutral loss and doubly charged ions
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            if (sortedKeys.get(i).lastIndexOf("++") != -1 ||
                    sortedKeys.get(i).lastIndexOf("H2O") != -1 ||
                    sortedKeys.get(i).lastIndexOf("H20") != -1 ||
                    sortedKeys.get(i).lastIndexOf("NH3") != -1) {
                renderer.setSeriesStroke(i, new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1.0f, new float[]{6.0f}, 0f));
            } else {
                renderer.setSeriesStroke(i, new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
        }

        plot.setRenderer(renderer);

        return chart;
    }

    /**
     * Returns a line plot based on the provided data.
     *
     * @param dataSet
     * @param xAxisLabel
     * @param yAxisLabel
     * @return
     */
    public static JFreeChart getLinePlot(HashMap<String, int[]> data, int[] totalNumberOfSpectraOfGivenLength,
            String xAxisLabel, String yAxisLabel) {

        // sort the keys
        ArrayList<String> sortedKeys = new ArrayList<String>();

        Iterator<String> iterator = data.keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();
            sortedKeys.add(key);
        }

        java.util.Collections.sort(sortedKeys);


        // add the data to the plot
        XYSeriesCollection dataset = new XYSeriesCollection();

        for (int i = 0; i < sortedKeys.size(); i++) {

            String key = sortedKeys.get(i);

            int[] tempArray = data.get(key);

            XYSeries tempDataSeries = new XYSeries(key);

            // note that the last fragment ion is ignored due to this being the same as the precursor
            // to include the ion remove the "-1" from the for-loop
            for (int j = 1; j < tempArray.length - 1; j++) {
                tempDataSeries.add(j, ((double) tempArray[j]) / totalNumberOfSpectraOfGivenLength[j]);
            }

            dataset.addSeries(tempDataSeries);
        }


        // create the chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                null, // title
                xAxisLabel, // xAxisLabel
                yAxisLabel, // yAxisLabel
                dataset, // XYZDataset
                PlotOrientation.VERTICAL, // orientation
                true, // legend
                true, // tooltips
                false); // urls

        chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
        chart.getLegend().setPosition(RectangleEdge.BOTTOM);
        chart.setBackgroundPaint(new Color(225, 225, 225));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        NumberAxis rangeAxis = (NumberAxis) plot.getDomainAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        // set the range to only include valid percatage values (and leave some padding at the top)
        plot.getRangeAxis().setRange(0, 1.04);

        // make sure that tooltip is generated
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

        // set the data series colors
        for (int i = 0; i < sortedKeys.size(); i++) {
            renderer.setSeriesPaint(i, Util.determineColorOfLine(sortedKeys.get(i)));
        }

        // increase the width of all lines and use dotted lines for the neutral loss and doubly charged ions
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            if (sortedKeys.get(i).lastIndexOf("++") != -1 ||
                    sortedKeys.get(i).lastIndexOf("H2O") != -1 ||
                    sortedKeys.get(i).lastIndexOf("H20") != -1 ||
                    sortedKeys.get(i).lastIndexOf("NH3") != -1) {
                renderer.setSeriesStroke(i, new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1.0f, new float[]{6.0f}, 0f));
            } else {
                renderer.setSeriesStroke(i, new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
        }

        plot.setRenderer(renderer);

        return chart;
    }

    /**
     * Adds the average mass error line to the plot.
     *
     * @param averageValues the map of average values (mz-value, average mass error)
     * @param chart the chart to add the mass error line to
     */
    public static void addAverageMassErrorLine(HashMap<Double, Double> averageValues, JFreeChart chart,
            boolean showAverageMassError) {

        XYSeries averageError = new XYSeries("Average Mass Error");

        Iterator<Double> averageIterator = averageValues.keySet().iterator();

        // add the mass errors to the data series
        while (averageIterator.hasNext()) {
            Double currentMzValue = averageIterator.next();
            Double currentAverage = averageValues.get(currentMzValue);
            averageError.add(currentMzValue, currentAverage);
        }

        // add the data series to the data set
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(averageError);

        // create the mass error line renderer
        XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, Color.BLACK);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

        // add the data set to the plot
        if (chart.getPlot() instanceof XYPlot) {
            ((XYPlot) chart.getPlot()).setDataset(1, dataset);
            ((XYPlot) chart.getPlot()).setRenderer(1, renderer);
        }

        // make the mass error line visble or not
        ((XYPlot) chart.getPlot()).getRenderer(1).setSeriesVisible(0, showAverageMassError);
        ((XYPlot) chart.getPlot()).getRenderer(1).setSeriesStroke(0,
                new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // makes sure that the average mass error line is rendered last, i.e., to the front
        ((XYPlot) chart.getPlot()).setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    }

    /**
     * Adds interval markers for all the fragment ion types.
     *
     * @param data the data to get the interval markers from
     * @param chart the chart to add the markers to
     */
    public static void addFragmentIonTypeMarkers(HashMap<String, ArrayList<XYZDataPoint>> data, JFreeChart chart,
            boolean showMarkers, Properties properties) {

        int horizontalFontPadding = 13;

        Iterator<String> iterator = data.keySet().iterator();

        // iterate the data and add one interval marker for each fragment ion type
        while (iterator.hasNext()) {

            String fragmentIonType = iterator.next();

            // get the mz value of the current fragment ion type
            ArrayList<XYZDataPoint> dataPoints = data.get(fragmentIonType);
            XYZDataPoint currentDataPoint = dataPoints.get(0);
            double currentXValue = currentDataPoint.getX();

            // create the interval marker
            IntervalMarker intervalMarker = new IntervalMarker(currentXValue - 5, currentXValue + 5, properties.getDefaultMarkerColor());
            intervalMarker.setLabel(fragmentIonType);
            intervalMarker.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
            intervalMarker.setLabelPaint(Color.GRAY);
            intervalMarker.setLabelTextAnchor(TextAnchor.TOP_LEFT);

            // set the fragment ion marker color
            if (fragmentIonType.startsWith("b")) {
                intervalMarker.setPaint(properties.getbFragmentIonColor());
            } else if (fragmentIonType.startsWith("y")) {
                intervalMarker.setPaint(properties.getyFragmentIonColor());
            } else {
                intervalMarker.setPaint(properties.getOtherFragmentIonColor());
            }

            // make the marker visible or not
            if (showMarkers) {
                intervalMarker.setAlpha(Properties.DEFAULT_VISIBLE_MARKER_ALPHA);
            } else {
                intervalMarker.setAlpha(Properties.DEFAULT_NON_VISIBLE_MARKER_ALPHA);
            }

            // set the horizontal location of the markers label
            // this is need so that not all labels appear on top of each other
            if (fragmentIonType.lastIndexOf("H2O") != -1) {
                intervalMarker.setLabelOffset(new RectangleInsets(horizontalFontPadding, 0, horizontalFontPadding, 0));
            }

            if (fragmentIonType.lastIndexOf("NH3") != -1) {
                intervalMarker.setLabelOffset(new RectangleInsets(horizontalFontPadding * 2, 0, horizontalFontPadding * 2, 0));
            }

            if (fragmentIonType.lastIndexOf("Prec") != -1) {
                intervalMarker.setLabelOffset(new RectangleInsets(horizontalFontPadding * 3, 0, horizontalFontPadding * 3, 0));

                if (fragmentIonType.lastIndexOf("H2O") != -1) {
                    intervalMarker.setLabelOffset(new RectangleInsets(horizontalFontPadding * 4, 0, horizontalFontPadding * 4, 0));
                }

                if (fragmentIonType.lastIndexOf("NH3") != -1) {
                    intervalMarker.setLabelOffset(new RectangleInsets(horizontalFontPadding * 5, 0, horizontalFontPadding * 5, 0));
                }
            }

            if (fragmentIonType.startsWith("i")) {
                intervalMarker.setLabelOffset(new RectangleInsets(horizontalFontPadding * 4, 0, horizontalFontPadding * 4, 0));
            }

            if (fragmentIonType.lastIndexOf("++") != -1) {
                intervalMarker.setLabelOffset(new RectangleInsets(horizontalFontPadding * 7, 0, horizontalFontPadding * 7, 0));

                if (fragmentIonType.lastIndexOf("H2O") != -1) {
                    intervalMarker.setLabelOffset(new RectangleInsets(horizontalFontPadding * 8, 0, horizontalFontPadding * 8, 0));
                }

                if (fragmentIonType.lastIndexOf("NH3") != -1) {
                    intervalMarker.setLabelOffset(new RectangleInsets(horizontalFontPadding * 9, 0, horizontalFontPadding * 9, 0));
                }
            }

            // add the interval marker to the plot
            ((XYPlot) chart.getPlot()).addDomainMarker(intervalMarker, Layer.BACKGROUND);
        }
    }

    /**
     * Adds a marker in the plot highlighting the modified residue.
     *
     * @param modifiedSequence
     * @param plot
     */
    public static void addModificationMarker(String modifiedSequence, CategoryPlot plot, boolean showMarkers,
            Properties properties) {

        // find the label to use for the modification marker
        String tempModifiedSequence = modifiedSequence;

        // remove the n-terminal
        if (tempModifiedSequence.startsWith("#")) {
            tempModifiedSequence = tempModifiedSequence.substring(tempModifiedSequence.indexOf("#", 1) + 2);
        } else {
            tempModifiedSequence = tempModifiedSequence.substring(tempModifiedSequence.indexOf("-") + 1);
        }

        int modificationIndex = tempModifiedSequence.indexOf('<') - 1;

        String modificationCategory = "" + tempModifiedSequence.charAt(modificationIndex) + (modificationIndex + 1);

        // create the marker and add it to the plot
        CategoryMarker marker = new CategoryMarker(
                modificationCategory, properties.getDefaultMarkerColor(), new BasicStroke(1.0f));
        marker.setDrawAsLine(false);
        marker.setLabelOffset(new RectangleInsets(2, 5, 2, 5));
        plot.addDomainMarker(marker, Layer.BACKGROUND);

        // make the marker visible or not visible
        if (showMarkers) {
            marker.setAlpha(Properties.DEFAULT_VISIBLE_MARKER_ALPHA);
        } else {
            marker.setAlpha(Properties.DEFAULT_NON_VISIBLE_MARKER_ALPHA);
        }
    }

    /**
     * Returns a category plot based on the provided data.
     *
     * @param dataSet
     * @param xAxisLabel
     * @param yAxisLabel
     * @return
     */
    public static CategoryPlot getCategoryPlot(CategoryDataset dataSet, String xAxisLabel,
            String yAxisLabel) {
        CategoryAxis xAxis = new CategoryAxis(xAxisLabel);
        xAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        NumberAxis yAxis = new NumberAxis(yAxisLabel);
        yAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        yAxis.setAutoRangeIncludesZero(false);

        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setFillBox(true);
        renderer.setBaseToolTipGenerator(new BoxAndWhiskerToolTipGenerator());

        return new CategoryPlot(dataSet, xAxis, yAxis, renderer);
    }

    /**
     * Returns a scatter plot if tbe provided data set.
     *
     * @param dataSet
     * @param usePpm if true ppm is used when plotting, otherwise Dalton is used
     * @param addLegend if true the legend is visible
     * @return the created chart
     */
    public static JFreeChart getScatterPlotChart(DefaultXYDataset dataSet, boolean usePpm, boolean addLegend,
            Properties properies) {

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        xAxis.setAutoRangeIncludesZero(true);
        xAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        xAxis.setLabel("m/z-value");

        String yAxisLabel = "Mass Error (Da)";

        if (usePpm) {
            yAxisLabel = "Mass Error (ppm)";
        }

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        yAxis.setAutoRangeIncludesZero(true);
        yAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        yAxis.setLabel(yAxisLabel);

        DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
        renderer.setBaseLinesVisible(false);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

        // set the data series colors if fragment ion label type is currently used
        if (properies.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_TYPE) {
            for (int i = 0; i < dataSet.getSeriesCount(); i++) {
                renderer.setSeriesPaint(i, Util.determineColorOfLine(dataSet.getSeriesKey(i).toString()));
            }
        }

        XYPlot plot = new XYPlot(dataSet, xAxis, yAxis, renderer);
        plot.setForegroundAlpha(0.5f);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        JFreeChart chart = new JFreeChart(plot);

        chart.setBackgroundPaint(new Color(225, 225, 225));
        chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));

        if (!addLegend) {
            chart.removeLegend();
        }

        return chart;
    }

    /**
     * Add a set of values to a box plot.
     *
     * @param dataSet the data set to add the values to
     * @param values the values to add
     * @param categoryLabel the label used for the category
     * @param dataSeriesLabel the label used for the data series
     * @return
     */
    public static double[] addValuesToBoxPlot(DefaultBoxAndWhiskerCategoryDataset dataSet, ArrayList<Double> values,
            String categoryLabel, String dataSeriesLabel) {

        ArrayList<Double> listValues = new ArrayList();

        double[] sample1 = new double[values.size()];

        for (int t = 0; t < values.size(); t++) {
            sample1[t] = values.get(t).doubleValue();
            listValues.add(new Double(values.get(t).doubleValue()));
        }

        dataSet.add(listValues, categoryLabel, dataSeriesLabel);

        return sample1;
    }

    /**
     * Retrieve the non-null b fragments.
     *
     * @param nonNullBValues the list to store the fragments in
     * @param bIntensities the list of intensities for the b fragments
     * @param index the index in the b fragment list to search
     * @return averge intensity of the non-null b fragments
     */
    public static double getNonNullBFragments(ArrayList<Double> nonNullBValues, double[][] bIntensities, int index) {

        float averageBValue = 0.0f;
        int averageBValues_numberOfNonNullValues = 0;

        for (int n = 0; n < bIntensities[index].length; n++) {

            if (bIntensities[index][n] > 0) {
                averageBValue += bIntensities[index][n];
                averageBValues_numberOfNonNullValues++;
                nonNullBValues.add(bIntensities[index][n]);
            }
        }

        return averageBValue /= averageBValues_numberOfNonNullValues;
    }

    /**
     * Retrieve the non-null y fragments.
     *
     * @param nonNullYValues the list to store the fragments in
     * @param yIntensities the list of intensities for the y fragments
     * @param index the index in the y fragment list to search
     * @return averge intensity of the non-null y fragments
     */
    public static double getNonNullYFragments(ArrayList<Double> nonNullYValues, double[][] yIntensities, int index) {

        float averageYValue = 0.0f;
        int averageYValues_numberOfNonNullValues = 0;

        for (int n = 0; n < yIntensities[index].length; n++) {
            if (yIntensities[yIntensities.length - index - 1][n] > 0) {
                averageYValue += yIntensities[yIntensities.length - index - 1][n];
                averageYValues_numberOfNonNullValues++;
                nonNullYValues.add(yIntensities[yIntensities.length - index - 1][n]);
            }
        }

        return averageYValue /= averageYValues_numberOfNonNullValues;
    }

    /**
     * Adds the provided data series to an XYZ data set.
     *
     * @param data the data to add
     * @param average a hash map to store the average fragment ion mass errors
     * @return the created data set
     */
    public static DefaultXYZDataset addXYZDataSeries(HashMap<String, ArrayList<XYZDataPoint>> data,
            HashMap<Double, Double> average, Properties properties) {

        // sort the keys
        ArrayList<String> sortedKeys = new ArrayList<String>();

        Iterator<String> iterator = data.keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();
            sortedKeys.add(key);
        }

        java.util.Collections.sort(sortedKeys);

        DefaultXYZDataset dataset = new DefaultXYZDataset();

        HashMap<Double, ArrayList<Double>> xAndYValues = new HashMap<Double, ArrayList<Double>>();

        for (int j = 0; j < sortedKeys.size(); j++) {

            String key = sortedKeys.get(j);

            ArrayList<XYZDataPoint> currentData = data.get(key);

            double[][] tempXYZData = new double[3][currentData.size()];

            double averageYValue = 0.0;

            for (int i = 0; i < currentData.size(); i++) {
                tempXYZData[0][i] = currentData.get(i).getX();
                tempXYZData[1][i] = currentData.get(i).getY();
                tempXYZData[2][i] = currentData.get(i).getZ();

                if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_TYPE) {
                    averageYValue += tempXYZData[1][i];
                } else if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_INSTRUMENT ||
                        properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_SCORING_TYPE ||
                        properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_THRESHOLD ||
                        properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_IDENTIFICATION_ID) {
                    if (xAndYValues.containsKey(tempXYZData[0][i])) {
                        ArrayList<Double> tempYValues = xAndYValues.get(tempXYZData[0][i]);
                        tempYValues.add(tempXYZData[1][i]);
                        xAndYValues.put(tempXYZData[0][i], tempYValues);
                    } else {
                        ArrayList<Double> tempYValues = new ArrayList<Double>();
                        tempYValues.add(tempXYZData[1][i]);
                        xAndYValues.put(tempXYZData[0][i], tempYValues);
                    }
                }
            }

            if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_TYPE) {
                average.put(tempXYZData[0][0], (averageYValue / currentData.size()));
            } else if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_INSTRUMENT ||
                    properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_SCORING_TYPE ||
                    properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_THRESHOLD ||
                    properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_IDENTIFICATION_ID) {

                Iterator<Double> xValuesIterator = xAndYValues.keySet().iterator();

                while (xValuesIterator.hasNext()) {
                    Double currentXValue = xValuesIterator.next();

                    ArrayList<Double> currentYValues = xAndYValues.get(currentXValue);

                    double currentAverageYValue = 0.0;

                    for (int i = 0; i < currentYValues.size(); i++) {
                        currentAverageYValue += currentYValues.get(i);
                    }

                    average.put(currentXValue, (currentAverageYValue / currentYValues.size()));
                }
            }

            dataset.addSeries(key, tempXYZData);
        }

        return dataset;
    }

    /**
     * Adds the provided data series to an XY data set.
     *
     * @param data the data to add
     * @param average a hash map to store the average fragment ion mass errors
     * @return the created data set
     */
    public static DefaultXYDataset addXYDataSeries(HashMap<String, ArrayList<XYZDataPoint>> data,
            HashMap<Double, Double> average, Properties properties) {

        // sort the keys
        ArrayList<String> sortedKeys = new ArrayList<String>();

        Iterator<String> iterator = data.keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();
            sortedKeys.add(key);
        }

        java.util.Collections.sort(sortedKeys);

        DefaultXYDataset dataset = new DefaultXYDataset();

        HashMap<Double, ArrayList<Double>> xAndZValues = new HashMap<Double, ArrayList<Double>>();

        for (int j = 0; j < sortedKeys.size(); j++) {

            String key = sortedKeys.get(j);

            ArrayList<XYZDataPoint> currentData = data.get(key);

            double[][] tempXYData = new double[2][currentData.size()];

            double averageZValue = 0.0;

            for (int i = 0; i < currentData.size(); i++) {
                tempXYData[0][i] = currentData.get(i).getX();
                tempXYData[1][i] = currentData.get(i).getZ();

                if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_TYPE) {
                    averageZValue += tempXYData[1][i];
                } else if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_INSTRUMENT ||
                        properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_SCORING_TYPE ||
                        properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_THRESHOLD ||
                        properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_IDENTIFICATION_ID) {
                    if (xAndZValues.containsKey(tempXYData[0][i])) {
                        ArrayList<Double> tempZValues = xAndZValues.get(tempXYData[0][i]);
                        tempZValues.add(tempXYData[1][i]);
                        xAndZValues.put(tempXYData[0][i], tempZValues);
                    } else {
                        ArrayList<Double> tempZValues = new ArrayList<Double>();
                        tempZValues.add(tempXYData[1][i]);
                        xAndZValues.put(tempXYData[0][i], tempZValues);
                    }
                }
            }

            if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_TYPE) {
                average.put(tempXYData[0][0], (averageZValue / currentData.size()));
            } else if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_INSTRUMENT ||
                    properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_SCORING_TYPE ||
                    properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_THRESHOLD ||
                    properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_IDENTIFICATION_ID) {

                Iterator<Double> xValuesIterator = xAndZValues.keySet().iterator();

                while (xValuesIterator.hasNext()) {
                    Double currentXValue = xValuesIterator.next();

                    ArrayList<Double> currentZValues = xAndZValues.get(currentXValue);

                    double currentAverageZValue = 0.0;

                    for (int i = 0; i < currentZValues.size(); i++) {
                        currentAverageZValue += currentZValues.get(i);
                    }

                    average.put(currentXValue, (currentAverageZValue / currentZValues.size()));
                }
            }

            dataset.addSeries(key, tempXYData);
        }

        return dataset;
    }

    /**
     * Returns a bubble chart of the provided data set.
     *
     * @param dataset the data set to plot
     * @param usePpm if true ppm is used when plotting, otherwise Dalton is used
     * @param addLegend if true the legend is visible
     * @return the created chart
     */
    public static JFreeChart getBubbleChart(DefaultXYZDataset dataSet, boolean usePpm, boolean addLegend,
            Properties properties) {

        String yAxisLabel = "Mass Error (Da)";

        if (usePpm) {
            yAxisLabel = "Mass Error (ppm)";
        }

        JFreeChart chart = ChartFactory.createBubbleChart(
                null, // title
                "m/z-value", // xAxisLabel
                yAxisLabel, // yAxisLabel
                dataSet, // XYZDataset
                PlotOrientation.VERTICAL, // orientation
                true, // legend
                true, // tooltips
                false); // urls

        XYPlot plot = chart.getXYPlot();
        plot.setForegroundAlpha(0.5f);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        xAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        yAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        chart.setBackgroundPaint(new Color(225, 225, 225));
        chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));

        if (!addLegend) {
            chart.removeLegend();
        }

        // set the data series colors if fragment ion label type is currently used
        if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_TYPE) {
            for (int i = 0; i < dataSet.getSeriesCount(); i++) {
                plot.getRenderer().setSeriesPaint(i, Util.determineColorOfLine(dataSet.getSeriesKey(i).toString()));
            }
        }

        return chart;
    }
}
