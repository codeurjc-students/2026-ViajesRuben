package com.ruben.backend.image.dto;

/**
 * The stored picture of an owner's image sub-resource (avatar, trip cover), and whether the owner
 * had none before (201) or it replaced one (200).
 */
public record ImageResult(ImageDTO image, boolean created) {}
