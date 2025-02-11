package fr.profi.thermoreader;

import fr.profi.mzdb.serialization.SerializationCallback;


import java.io.File;

public class MetaDataReaderTask implements IMetaDataReaderTask {
  private final SerializationCallback callBack;
  private final String taskId;
  private final File rawFile;

  public MetaDataReaderTask(String taskId, File rFile, SerializationCallback c){
    callBack= c;
    rawFile = rFile;
    this.taskId =taskId;
  }

  public SerializationCallback getCallback(){
    return callBack;
  }

  public String getTaskId(){
    return taskId;
  }

  public File getInputFile(){
    return rawFile;
  }

}
