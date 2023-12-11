package fr.profi.mzdbconverter;

import fr.edyp.mzdb.server.MzdbController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;


/**
 *
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] argv) {

        // look for a specific port
        int port = 8090;
        int portSpecifiedIndex = -1;
        for (int i=0;i<argv.length-1;i++) {
            String param = argv[i];
            if (param.equals("-p") || param.equals("--port")) {
                portSpecifiedIndex = i+1;
                port = Integer.parseInt(argv[portSpecifiedIndex]);

            }
        }


        // --- look for an available port between port=8090 (or specified port as argument) and port+100
        int portMax = port+100;
        boolean available = false;
        for (;port<=portMax;port++) {
            available = isPortAvailable(port);
            if (available) {
                break;
            }
        }

        if (! available) {
            LOGGER.error("No port available between"+port+"and "+portMax);
            return;
        }



        // --- mzdb server start

        String[] args = { Integer.toString(port)};
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                fr.edyp.mzdb.server.Main.main(args);
            }
        });
        t.setDaemon(false); // the main thread will wait for the ending of the server thread
        t.start();

        // --- start ThermoAccess

        // prepare command with parameters

        // check localisation of the ThermoAccess.exe
        File pathFile = new File(".\\target\\unzip-dependencies\\ThermoAccess_1.0.2.1-win-x64\\"); // path for debugging with IDE.
        if (! pathFile.exists()) {
            pathFile = new File(".\\ThermoAccess_win-x64_1.0.1\\");
        }


        ArrayList<String> cmds = new ArrayList(3+argv.length);
        cmds.add(pathFile.getAbsolutePath()+"\\ThermoAccess.exe");

        if (portSpecifiedIndex != -1) {
            argv[portSpecifiedIndex] = String.valueOf(port);
        } else {
            cmds.add("-p");
            cmds.add(String.valueOf(port));
        }
        cmds.addAll(Arrays.asList(argv));

        // Prepare process builder, we redirect error stream to ouput stream to be able to read both
        ProcessBuilder pb = new ProcessBuilder(cmds);
        pb.redirectErrorStream(true);

        pb.directory(pathFile);



        try {
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                LOGGER.info(line);
            }

            int exitCode = p.waitFor();
            if (exitCode != 0) {

                LOGGER.error(cmds+" abnormally finished with exitCode "+exitCode);
                LOGGER.info("Interrupt socket server");
                fr.edyp.mzdb.server.Main.interrupt();
                System.exit(exitCode);
            }

        } catch (Exception e ) {
            LOGGER.error(cmds+" abnormally finished");
            LOGGER.error(e.getMessage(), e);
            LOGGER.info("Interrupt socket server");
            fr.edyp.mzdb.server.Main.interrupt();
            System.exit(-1);
        }


    }

    private static boolean isPortAvailable(int port) {

        ServerSocket serverSocket = null;
        DatagramSocket datagramSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            datagramSocket = new DatagramSocket(port);
            datagramSocket.setReuseAddress(true);

            return true;

        } catch (IOException e) {
        } finally {
            if (datagramSocket != null) {
                datagramSocket.close();
            }

            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                }
            }
        }

        return false;
    }

}
