package com.wol.updater.domain;

public record VersionSignature(
    String stringTableHash,
    String techTreeHash,
    String protoHash
) {}
