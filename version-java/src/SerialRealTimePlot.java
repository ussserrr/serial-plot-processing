import processing.core.*;
import processing.serial.*;

import java.nio.*;
import java.util.List;
import java.util.ArrayList;

// Processing plotting library
import grafica.*;
// Processing GUI library
import controlP5.*;


public class SerialRealTimePlot extends PApplet {

    public void settings() {
        size(plotWidth * nChannels, plotHeight + pauseBtnHeight);
    }

    static public void main(String[] passedArgs) {
        String[] appletArgs = new String[] { "SerialRealTimePlot" };
        if (passedArgs != null) {
            PApplet.main(concat(appletArgs, passedArgs));
        } else {
            PApplet.main(appletArgs);
        }
    }

    private final int nChannels = 2;  // also number of plots
    private final int intSize = 4;  // bytes

    private final int plotWidth = 450;
    private final int plotHeight = 300;

    private final int pauseBtnWidth = plotWidth * nChannels;
    private final int pauseBtnHeight = 40;

    // Array of points for 2 channels
    private final List<Integer>[] channels = new List[nChannels];
    // Store last 2 integers (1 per each channel)
    private final byte[] currentBuffer = new byte[nChannels * intSize];
    // Use this class to interpret bytes as integers
    private ByteBuffer byteBuffer;

    private final int nPoints = 250;  // num of points in each plot (generally affects resolution and speed)
    private int pointsCnt = 0;  // count each new point
    private final float scatterCoef = 5.0f;
    private final GPlot[] plots = new GPlot[nChannels];
    private boolean isPaused = false;

    // For x-ticks
    private long startTime;
    private long currentTime;
    private long currentTimePrev;

    // For benchmarking
    private final boolean runBench = false;
    private int cnt;
    private final int skipNFirstPoints = 100;
    private int skipNFirstPointsCnt = 0;

    /*
     *  Use Processing GUI framework to add play/pause button. This is a pretty rich library
     *  so you can put some other useful elements
     */
    private ControlP5 cp5;


    public void setup() {

        for (int i = 0; i < channels.length; i++) {
            channels[i] = new ArrayList<>();
        }

        for (int i = 0, posX = 0; i < plots.length; i++, posX += plotWidth) {
            plots[i] = new GPlot(this);
            plots[i].getMainLayer().setPointSize(3.5f);
            plots[i].setPos(posX, 0);
            plots[i].setYLim(0, 4095);
            plots[i].defaultDraw();
        }

        cp5 = new ControlP5(this);
        cp5.addButton("pauseBtn")
            .setPosition(0, plotHeight)
            .setSize(pauseBtnWidth, pauseBtnHeight);

        String portName = "/dev/cu.wchusbserial14220";
        Serial ser = new Serial(this, portName, 115200);
        ser.buffer(nChannels * intSize);

        startTime = System.nanoTime();
        currentTimePrev = startTime;
    }


    public void draw() {
        currentTime = System.nanoTime();

        // Benchmark - how many points in 1 second
        if ( runBench ) {
            cnt += channels[0].size();
            if (currentTime - startTime >= 1e9) {
                println(cnt);
                cnt = 0;
                startTime = currentTime;
            }
            // Controlling whether values are correct when benchmarking
            if ( ++skipNFirstPointsCnt == skipNFirstPoints ) {
                System.out.printf("A: %4d\tB: %4d\n", channels[0].get(0), channels[1].get(0));
            }
        }
        else {

            /*
             *  No need to redraw during the pause. But we continue to stamp points in the background
             *  to provide an instant resuming
             */
            if (!isPaused) {
                for (GPlot plot : plots) {
                    plot.beginDraw();
                    plot.drawBackground();
                    plot.drawBox();
                    plot.drawXAxis();
                    plot.drawYAxis();
                    plot.drawTopAxis();
                    plot.drawRightAxis();
                    plot.drawTitle();
                    plot.getMainLayer().drawPoints();
                    plot.endDraw();
                }
            }

            /*
             *  Append all points accumulated between 2 consecutive screen updates (see 'serialEvent' note).
             *  Instead of putting all these accumulated points at the one x-tick we evenly scatter them
             *  a little bit with a 'scatterCoef' to avoid gaps between points.
             */
            for (int i = 0; i < channels[0].size(); i++, pointsCnt++) {
                for (int j = 0; j < plots.length; j++) {
                    plots[j].addPoint((currentTimePrev
                                          + ((currentTime - currentTimePrev) * scatterCoef * i / channels[j].size())
                                          - startTime)
                                          / 1e9f,
                            channels[j].get(i));
                }
                if (pointsCnt > nPoints) {
                    for (GPlot plot : plots) {
                        plot.removePoint(0);
                    }
                }
            }
            currentTimePrev = currentTime;
        }

        // Free dynamic buffers
        for (List<Integer> channel : channels) {
            channel.clear();
        }
    }


    /*
     *  We use this separate event to read bytes from the serial port. 'currentBuffer' is used to store raw
     *  bytes and 'byteBuffer' to convert them into 2 4-byte integers (little endian format). As 'serialEvent'
     *  triggers more frequent than screen update event we need to store several values between 2 updates.
     *  So we use dynamic arrays for this purpose.
     *
     *  Also there are always a chance to get in during a 'wrong' byte: not a first one of a whole integer or
     *  just channels are swapped. In this case simply restart the sketch or implement some sort of syncing
     *  mechanism (e.g. check decoded values).
     */
    public void serialEvent(Serial s) {
        s.readBytes(currentBuffer);
        byteBuffer = ByteBuffer.wrap(currentBuffer).order(ByteOrder.LITTLE_ENDIAN);
        for (List<Integer> channel : channels) {
            channel.add(byteBuffer.getInt());
        }
    }

    /*
     *  Our button automatically binds itself to the function with matching name
     */
    public void pauseBtn() {
        isPaused = !isPaused;
    }

}
