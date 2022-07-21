package at.ac.uibk.metadata.api.model.enums;

/**
 * This file was originally part of the bachelor thesis <a href="https://github.com/sashkoristov/AFCLDeployer">AFCLDeployer</a>
 * supervised by Sasko Ristov, written by
 *
 * @author Christoph Abenthung and Caroline Haller
 * <p>
 * The file was moved and refactored to a new project in order to extract a common API by Philipp Gritsch as part of the
 * 'Tracing and Simulation Framework for AFCL' bachelor thesis
 */
public enum LimitType {

    MIN_MEMORY,
    MAX_MEMORY,
    DEFAULT_MEMORY_SIZE,
    MEMORY_STEP_SIZE,

    MIN_TIMEOUT,
    MAX_TIMEOUT,
    DEFAULT_TIMEOUT,

    MAX_FILE_SIZE,

    MIN_VOLUME_SIZE,
    MAX_VOLUME_SIZE,
    DEFAULT_VOLUME_SIZE,

    MIN_DISKS,
    MAX_DISKS,
    MIN_IOPS,
    MAX_IOPS,
    MIN_CAPACITY,
    MAX_CAPACITY,

    BUCKET_CREATION_DELAY,


}
