package com.wol.updater.application;

import com.wol.updater.domain.UpdateRecord;
import java.util.List;

/**
 * Manages the persistence and retrieval of update history.
 */
public interface HistoryManager {
    
    /**
     * Appends a new update record to the history.
     */
    void addRecord(UpdateRecord record);
    
    /**
     * Retrieves all update records, ordered from newest to oldest.
     */
    List<UpdateRecord> getHistory();
    
    /**
     * Clears all history.
     */
    void clearHistory();
}
