package com.wol.updater.application;

import com.wol.updater.domain.UpdatePlan;
import com.wol.updater.domain.VersionSignature;

import java.util.Optional;

public interface UpdateSource {
    
    /**
     * Fetches metadata from the source and generates an UpdatePlan based on the local version signature.
     * 
     * @param localSignature The version signature detected on the local machine
     * @return An UpdatePlan containing the target version and required download packages, 
     *         or Optional.empty() if the signature is unknown or metadata cannot be fetched.
     */
    Optional<UpdatePlan> getUpdatePlan(VersionSignature localSignature);
}
