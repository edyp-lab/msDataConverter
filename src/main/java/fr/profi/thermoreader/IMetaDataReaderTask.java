package fr.profi.thermoreader;

import fr.profi.mzdb.serialization.SerializationCallback;

public interface IMetaDataReaderTask {
  SerializationCallback getCallback();

  String getTaskId();

}
