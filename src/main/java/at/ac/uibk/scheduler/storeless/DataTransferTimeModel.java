package at.ac.uibk.scheduler.storeless;

import at.ac.uibk.metadata.api.model.DataTransfer;
import at.ac.uibk.metadata.api.model.Region;
import at.ac.uibk.metadata.api.model.enums.Provider;
import at.ac.uibk.scheduler.api.MetadataCache;
import at.ac.uibk.scheduler.api.SchedulingException;

import java.util.List;

public class DataTransferTimeModel {

    /**
     * Calculates the download time using the given parameters.
     *
     * @param functionRegionId the id of the function region
     * @param bucketUrls       the urls of the storage bucket to download the data from
     * @param fileAmounts      the amount of files to be downloaded
     * @param fileSizes        the total size of all files to be downloaded
     *
     * @return the calculated download time
     */
    public static double calculateDownloadTime(long functionRegionId, List<String> bucketUrls, List<Integer> fileAmounts, List<Double> fileSizes) {
        double dlTime = 0;
        int idx = 0;
        for (String url : bucketUrls) {
            long storageRegionId = getRegionId(url);
            DataTransfer dataTransfer = getDataTransferEntry(MetadataCache.get().getDataTransfersDownload(), functionRegionId, storageRegionId);
            dlTime += fileAmounts.get(idx) * dataTransfer.getLatency() + ((fileSizes.get(idx) / dataTransfer.getBandwidth()) * 1000);
            idx++;
        }
        return dlTime;
    }

    /**
     * Calculates the upload time using the given parameters.
     *
     * @param dataTransfer the data transfer object to use
     * @param fileAmount   the amount of files to be uploaded
     * @param fileSize     the total size of all files to be uploaded
     *
     * @return the calculated upload time
     */
    public static double calculateUploadTime(DataTransfer dataTransfer, int fileAmount, double fileSize) {
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
        String provider;
        if (bucketUrl.startsWith("s3://") || bucketUrl.startsWith("arn:aws:s3") || bucketUrl.contains(".s3.amazonaws.com")) {
            provider = "AWS";
        } else if (bucketUrl.startsWith("gs://") || bucketUrl.startsWith("https://storage.googleapis.com/")) {
            provider = "Google";
        } else {
            throw new SchedulingException(bucketUrl + " is not an AWS or GCP bucket");
        }

        return MetadataCache.get().getRegions().stream()
                .filter(r -> r.getProvider().equals(Provider.valueOf(provider)) && bucketUrl.contains(r.getRegion()))
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
