package fr.profi.mzdbconverter;

import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;


public class Main {

    public static void main(String[] argv) {

        // --- look for an available port between 8090 and 8190

        int port = 8090;
        int portMax = port+100;
        boolean available = false;
        for (;port<=portMax;port++) {
            available = isPortAvailable(port);
            if (available) {
                break;
            }
        }

        if (! available) {
            System.err.println("No port available between"+port+"and "+portMax);
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
        t.setDaemon(false); // oblige the main thread to wait for the ending of the server thread
        t.start();

        // --- start ThermoAccess

        // prepare command with parameters

        // check localisation of the ThermoAccess.exe
        File pathFile = new File(".\\target\\unzip-dependencies\\ThermoAccess_win-x64_1.0.1\\"); // path for debugging with IDE.
        if (! pathFile.exists()) {
            pathFile = new File(".\\ThermoAccess_win-x64_1.0.1\\");
        }


        ArrayList<String> cmds = new ArrayList(3+argv.length);
        cmds.add(pathFile.getAbsolutePath()+"\\ThermoAccess.exe");
        cmds.add("-p");
        cmds.add(String.valueOf(port));
        cmds.addAll(Arrays.asList(argv));

        // Prepare process builder, we redirect error stream to ouput stream to be able to read both
        ProcessBuilder pb = new ProcessBuilder(cmds);
        pb.redirectErrorStream(true);

        pb.directory(pathFile);

        System.out.println(pathFile.getAbsolutePath()); //JPM.TODO put in comment


        try {
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = p.waitFor();
            if (exitCode != 0) {
                System.err.println(cmds+" abnormally finished with exitCode "+exitCode);
                System.out.println("Interrupt socket server");
                fr.edyp.mzdb.server.Main.interrupt();
                System.exit(exitCode);
            }

        } catch (Exception e ) {
            System.err.println(cmds+" abnormally finished");
            e.printStackTrace();
            System.out.println("Interrupt socket server");
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
