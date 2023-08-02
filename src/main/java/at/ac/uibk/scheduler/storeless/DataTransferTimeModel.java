package at.ac.uibk.scheduler.storeless;

import at.ac.uibk.metadata.api.model.DataTransfer;
import at.ac.uibk.metadata.api.model.Region;
import at.ac.uibk.scheduler.api.MetadataCache;
import at.ac.uibk.scheduler.api.SchedulingException;

import java.util.List;

public class DataTransferTimeModel {
    
    /**
     * Calculates the download time using the given parameters.
     *
     * @param functionRegionId the id of the function region
     * @param bucketUrl        the url of the storage bucket to download the data from
     * @param fileAmount       the amount of files to be downloaded
     * @param fileSize         the total size of all files that should be downloaded
     *
     * @return the calculated download time
     */
    public static double calculateDownloadTime(long functionRegionId, String bucketUrl, int fileAmount, double fileSize) {
        long storageRegionId = getRegionId(bucketUrl);
        DataTransfer dataTransfer = getDataTransferEntry(MetadataCache.get().getDataTransfersDownload(), functionRegionId, storageRegionId);

        return fileAmount * dataTransfer.getLatency() + ((fileSize / dataTransfer.getBandwidth()) * 1000);
    }

    /**
     * Gets the id of the region that is contained in the given bucketUrl. If no region is detected in the given string,
     * an exception is thrown.
     *
     * @param bucketUrl to search for the region
     *
     * @return the id of the identified region
     */
    private static Long getRegionId(String bucketUrl) {
        return MetadataCache.get().getRegions().stream()
                .filter(r -> bucketUrl.contains(r.getRegion()))
                .mapToLong(Region::getId)
                .findFirst()
                .orElseThrow(() -> new SchedulingException("Bucket URL '" + bucketUrl + "' does not contain any region-code"));
    }

    /**
     * Gets the entry in the given list with the given function region id and storage region id. If no entry is found,
     * and exception is thrown.
     *
     * @param list             to search for the entry
     * @param functionRegionId the id of the function region
     * @param storageRegionId  the id of the storage region
     *
     * @return the {@link DataTransfer} object with the given function region and storage region
     */
    private static DataTransfer getDataTransferEntry(List<DataTransfer> list, long functionRegionId, long storageRegionId) {
        return list.stream()
                .filter(entry -> entry.getFunctionRegionID() == functionRegionId && entry.getStorageRegionID() == storageRegionId)
                .findFirst()
                .orElseThrow(() -> new SchedulingException(
                        String.format("No Data Transfer entry found for function region %d and storage region %d ", functionRegionId, storageRegionId)
                ));
    }

}
