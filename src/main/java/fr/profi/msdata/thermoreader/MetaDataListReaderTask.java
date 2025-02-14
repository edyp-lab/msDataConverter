package fr.profi.msdata.thermoreader;

import fr.profi.msdata.serialization.SerializationCallback;

import java.io.File;
import java.util.List;

public class MetaDataListReaderTask implements IMetaDataReaderTask {
  private final SerializationCallback callBack;
  private final String taskId;
  private final List<File> rawFiles;

  public MetaDataListReaderTask(String taskId, List<File>  rFiles, SerializationCallback c){
    callBack= c;
    rawFiles = rFiles;
    this.taskId =taskId;
  }

  public SerializationCallback getCallback(){
    return callBack;
  }

  public String getTaskId(){
    return taskId;
  }

  public List<File> getInputFiles(){
    return rawFiles;
  }
}
