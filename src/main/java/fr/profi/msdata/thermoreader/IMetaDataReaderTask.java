package fr.profi.msdata.thermoreader;

import fr.profi.msdata.serialization.SerializationCallback;

public interface IMetaDataReaderTask {
  SerializationCallback getCallback();

  String getTaskId();

}
