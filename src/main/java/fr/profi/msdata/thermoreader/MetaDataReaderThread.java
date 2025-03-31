package fr.profi.msdata.thermoreader;

import fr.profi.msdata.serialization.SerializationCallback;
import fr.profi.msdata.consumer.MsDataConsumerMain;
import fr.profi.msdata.mzdbconverter.Thermo2Mzdb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MetaDataReaderThread extends Thread {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetaDataReaderThread.class);

  private static MetaDataReaderThread m_singleton;
  private final LinkedList<IMetaDataReaderTask> m_actions;

  private static final int m_serverPort = 8090;

  private boolean serverInitialized = false;

  private final ExecutorService m_thermoAccessThreadPool = Executors.newCachedThreadPool();


  public static MetaDataReaderThread getInstance() {
    if (m_singleton == null)
      m_singleton = new MetaDataReaderThread();
    return m_singleton;
  }

  public void addTask(IMetaDataReaderTask task) {
    synchronized (this) {
      m_actions.add(task);
      notifyAll();
    }
  }

  private MetaDataReaderThread() {
    super("MetaDataReader-Thread"); // useful for debugging
    setDaemon(false);

    m_actions = new LinkedList<>();

  }

  /**
   * Main loop of the thread
   */
  @Override
  public void run() {
    try {
      LOGGER.info(" START MetaDate Reader ...");
      SerializationCallback currentCallback = null;
      while (true) {

        IMetaDataReaderTask task;

        synchronized (this) {
          while (true) {
            // look for a task to be done
            if (!m_actions.isEmpty()) {
              task = m_actions.poll();
              break;
            }
            wait();
          }
          notifyAll();
        }

        if(!serverInitialized) {
          LOGGER.trace("  ->  Init MzdbServer .... ");
          initServer();
          serverInitialized = true;
        }

        if(currentCallback != task.getCallback()) {
          LOGGER.trace("  ->  add new callBack .... ");
          MsDataConsumerMain.getInstance().addCallBack(task.getCallback());
          currentCallback = task.getCallback();
        }

        String rawPathOption;
        String rawPath;
        final List<String> filesToProcess = new ArrayList<>();
        if(task instanceof MetaDataReaderTask) {
          rawPathOption = "-i";
          rawPath = ((MetaDataReaderTask)task).getInputFile().getAbsolutePath();
          filesToProcess.add(rawPath);
        } else { //Is MetaDataReaderListTask
          rawPathOption = "-l";
          List<File> allFiles = ((MetaDataListReaderTask)task).getInputFiles();
          StringBuilder sb = new StringBuilder();
          for(int i= 0; i<allFiles.size(); i++ ){
            if(i>0)
              sb.append(";");
            String nextFilePath = allFiles.get(i).getAbsolutePath();
            sb.append(nextFilePath);
            filesToProcess.add(nextFilePath);
          }
          rawPath = sb.toString();
        }

        //Process next task
        String[]  commandArg = new String[5];
        commandArg[0] = "metadata"; //Read meta data argument
        commandArg[1] = rawPathOption; //Input file
        commandArg[2] =rawPath;
        commandArg[3] ="-p";
        commandArg[4] = Integer.toString(m_serverPort);

        //Run ThermoAccess on its own Thread to allow multiple runs
       final SerializationCallback  finalCallBack = currentCallback;
        m_thermoAccessThreadPool.submit(() -> {
          LOGGER.debug("\n\n  WILL RUN  ThermoAccess Process with : {} ==> {}", rawPathOption, rawPath);
          int errorCode = Thermo2Mzdb.startThermoAccess(commandArg); //Run process
          LOGGER.debug("  ->  ThermoAccess Process returned code: {}", errorCode);
          if(errorCode == -1 || errorCode == 1 || errorCode == 2 || errorCode == 10 ) {
            filesToProcess.forEach(filePath -> finalCallBack.run(filePath, new ArrayList<>(), false));
          } //in other cases, server should have called callback with read data

        });
      }
    } catch (Throwable t) {
      LOGGER.error("  -> Unexpected exception in main loop of MetaDataReaderThread", t);
      m_thermoAccessThreadPool.shutdown();
      try {
        // Wait for all tasks to complete
        if (!m_thermoAccessThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
          m_thermoAccessThreadPool.shutdownNow();
        }
      } catch (InterruptedException e) {
        m_thermoAccessThreadPool.shutdownNow();
      }
      interrupt();
      m_singleton = null; // reset thread
    }

  }

  @Override
  public void interrupt() {
    LOGGER.info("Interrupt MetaDateReader Thread");
    MsDataConsumerMain.getInstance().interrupt(false);
    super.interrupt();
  }

  private void initServer() {
    // Init MzdbServer
    String[] args = { "-p", Integer.toString(m_serverPort), "-t"};
    MsDataConsumerMain.getInstance().initServer(args);

    //Run MzdbServer on its own Thread
    Thread t = new Thread(() -> MsDataConsumerMain.getInstance().start(), "MainConsumer Thread");
    t.setDaemon(false); // the main thread will wait for the ending of the server thread
    t.start();

  }

}
