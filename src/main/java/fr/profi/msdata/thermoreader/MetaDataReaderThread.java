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

public class MetaDataReaderThread extends Thread {
  private static final Logger LOGGER = LoggerFactory.getLogger(MetaDataReaderThread.class);

  private static MetaDataReaderThread m_singleton;
  private final LinkedList<IMetaDataReaderTask> m_actions;

  private static final int m_serverPort = 8090;

  private boolean serverInitialized = false;

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
    LOGGER.info("Create MetaDateReader Thread");
    setDaemon(false);

    m_actions = new LinkedList<>();

  }

  /**
   * Main loop of the thread
   */
  @Override
  public void run() {
    try {
      LOGGER.debug(" START MetaDate Reader");
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
          LOGGER.debug("  ->  Init MzdbServer ");
          initServer();
          serverInitialized = true;
        }

        if(currentCallback != task.getCallback()) {
          LOGGER.debug("  ->  add new callBack .... ");
          MsDataConsumerMain.getInstance().addCallBack(task.getCallback());
          currentCallback = task.getCallback();
        }

        String rawPathOption;
        String rawPath;
        if(task instanceof MetaDataReaderTask) {
          rawPathOption = "-i";
          rawPath = ((MetaDataReaderTask)task).getInputFile().getAbsolutePath();
        } else { //Is MetaDataReaderListTask
          rawPathOption = "-l";
          List<File> allFiles = ((MetaDataListReaderTask)task).getInputFiles();
          StringBuilder sb = new StringBuilder();
          for(int i= 0; i<allFiles.size(); i++ ){
            if(i>0)
              sb.append(";");
            sb.append(allFiles.get(i).getAbsolutePath());
          }
          rawPath = sb.toString();
        }
        LOGGER.info("\n\n  WILL RUN  ThermoAccess Process with : "+rawPathOption+" ==> "+rawPath+"\n\n");
        //Process next task
        String[]  commandArg = new String[5];
        commandArg[0] = "metadata"; //Read meta data argument
        commandArg[1] = rawPathOption; //Input file
        commandArg[2] =rawPath;
        commandArg[3] ="-p";
        commandArg[4] = Integer.toString(8090);

        //Run ThermoAccess on its own Thread to allow multiple runs
       final SerializationCallback  finalCallBack = currentCallback;
        Thread t = new Thread(() -> {
          LOGGER.info("\n\n  WILL RUN  ThermoAccess Process with : "+rawPathOption+" ==> "+rawPath);
          int errorCode = Thermo2Mzdb.startThermoAccess(commandArg); //Run process
          LOGGER.info("  ->  ThermoAccess Process returned code: "+errorCode);
          if(errorCode != 0){
            finalCallBack.run(rawPath, new ArrayList<>(), false);
          } //if no error, server should call callback with read data
        });
        t.setDaemon(false); // the main thread will wait for the ending of the server thread
        t.start();

      }
    } catch (Throwable t) {
      LOGGER.error("  -> Unexpected exception in main loop of MetaDataReaderThread", t);
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
