package at.ac.uibk.scheduler.api;

import at.ac.uibk.metadata.api.daos.se.provider.MetadataProvider;
import at.ac.uibk.metadata.api.model.*;
import at.ac.uibk.metadata.api.model.enums.DataTransferType;
import at.ac.uibk.metadata.api.model.enums.Provider;
import at.ac.uibk.metadata.api.model.functions.FunctionDeployment;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MetadataCache {

    private static MetadataCache INSTANCE;

    public static synchronized MetadataCache get() {
        if (MetadataCache.INSTANCE == null) {
            MetadataCache.INSTANCE = new MetadataCache();
        }
        return MetadataCache.INSTANCE;
    }

    private final List<FunctionType> functionTypes;
    private final List<FunctionImplementation> functionImplementations;
    private final List<FunctionDeployment> functionDeployments;
    private final List<Region> regions;
    private final List<DetailedProvider> detailedProviders;
    private final List<DataTransfer> dataTransfers;
    private final List<DataTransfer> dataTransfersDownload;
    private final List<DataTransfer> dataTransfersUpload;

    private final Map<Long, FunctionImplementation> functionImplementationsById;
    private final Map<Long, FunctionType> functionTypesById;
    private final Map<Long, FunctionDeployment> deploymentsById;
    private final Map<FunctionType, Set<FunctionDeployment>> deploymentsByFunctionType;

    private final Map<String, FunctionType> functionTypesByName;

    private final Map<FunctionDeployment, Long> maxConcurrencyByDeployment;

    private final Map<Integer, Region> regionsById;
    private final Map<String, Region> regionsByName;

    private final Map<Integer, DetailedProvider> detailedProvidersById;

    private final Map<Long, DataTransfer> dataTransfersById;

    private MetadataCache() {
        final Map<Long, Long> concurrencyByFdId = new HashMap<>();

        try (final MetadataProvider metadata = MetadataProvider.get()) {

            this.functionTypes = metadata.functionTypeDao().getAll();
            this.functionImplementations = metadata.functionImplementationDao().getAll();
            this.functionDeployments = metadata.functionDeploymentDao().getAll()
                    .stream().filter(fd -> fd.getAvgRTT() != null && fd.getKmsArn() != null).collect(Collectors.toList());

            this.regions = metadata.regionDao().getAll();
            this.detailedProviders = metadata.detailedProviderDao().getAll();
            this.dataTransfers = metadata.dataTransferDao().getAll();

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        this.detailedProvidersById = this.detailedProviders.stream()
                .collect(Collectors.toMap(DetailedProvider::getId, f -> f));

        this.regionsById = this.regions.stream()
                .collect(Collectors.toMap(Region::getId, f -> f));

        this.regionsByName = this.regions.stream()
                .collect(Collectors.toMap(Region::getRegion, f -> f));

        this.deploymentsById = this.functionDeployments.stream()
                .collect(Collectors.toMap(FunctionDeployment::getId, f -> f));

        this.functionImplementationsById = this.functionImplementations.stream()
                .collect(Collectors.toMap(FunctionImplementation::getId, f -> f));

        this.functionTypesById = this.functionTypes.stream()
                .collect(Collectors.toMap(FunctionType::getId, f -> f));

        this.dataTransfersById = this.dataTransfers.stream()
                .collect(Collectors.toMap(DataTransfer::getId, f -> f));

        this.dataTransfersDownload = this.dataTransfers.stream()
                .filter(d -> d.getType() == DataTransferType.download)
                .collect(Collectors.toList());

        this.dataTransfersUpload = this.dataTransfers.stream()
                .filter(d -> d.getType() == DataTransferType.upload)
                .collect(Collectors.toList());

        this.deploymentsByFunctionType =
                this.functionDeployments.stream()
                        .map(deployment -> Pair.of(this.functionImplementationsById.get(deployment.getFunctionImplementationId()),
                                deployment))
                        .map(pair -> Pair.of(this.functionTypesById.get(pair.getKey().getFunctionTypeId()), pair.getValue()))
                        .collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toSet())));

        this.functionTypesByName =
                this.functionTypes.stream()
                        .collect(Collectors.toMap(FunctionType::getType, Function.identity()));

        this.maxConcurrencyByDeployment =
                this.functionDeployments.stream()
                        .map(d -> {
                            final var region = this.regionsById.get(d.getRegionId().intValue());
                            final var provider = this.detailedProvidersById.get(region.getDetailedProviderId().intValue());

                            return Pair.of(d, provider.getMaxConcurrency());

                        })
                        .collect(Collectors.toMap(Pair::getKey, p -> p.getValue().longValue()));
    }

    public Optional<DetailedProvider> getDetailedProviderFor(final Region region) {
        return Optional.ofNullable(region)
                .map(Region::getDetailedProviderId)
                .map(Long::intValue)
                .map(this.getDetailedProvidersById()::get);
    }

    public Optional<Region> getRegionFor(final FunctionDeployment deployment) {
        return Optional.ofNullable(deployment)
                .map(FunctionDeployment::getRegionId)
                .map(Long::intValue)
                .map(this.getRegionsById()::get);
    }

    public Optional<Integer> getRegionIdFor(final String provider, final String region) {
        return this.getRegions().stream()
                .filter(r -> r.getProvider() == Provider.valueOf(provider) && r.getRegion().equals(region))
                .findFirst()
                .map(Region::getId);
    }

    public List<FunctionType> getFunctionTypes() {
        return this.functionTypes;
    }

    public List<FunctionImplementation> getFunctionImplementations() {
        return this.functionImplementations;
    }

    public List<FunctionDeployment> getFunctionDeployments() {
        return this.functionDeployments;
    }

    public Map<Long, FunctionImplementation> getFunctionImplementationsById() {
        return this.functionImplementationsById;
    }

    public Map<Long, FunctionType> getFunctionTypesById() {
        return this.functionTypesById;
    }

    public Map<FunctionType, Set<FunctionDeployment>> getDeploymentsByFunctionType() {
        return this.deploymentsByFunctionType;
    }

    public Map<String, FunctionType> getFunctionTypesByName() {
        return this.functionTypesByName;
    }

    public Map<Long, FunctionDeployment> getDeploymentsById() {
        return this.deploymentsById;
    }

    public Map<FunctionDeployment, Long> getMaxConcurrencyByDeployment() {
        return this.maxConcurrencyByDeployment;
    }

    public List<Region> getRegions() {
        return this.regions;
    }

    public Map<Integer, Region> getRegionsById() {
        return this.regionsById;
    }

    public Map<String, Region> getRegionsByName() {
        return regionsByName;
    }

    public Map<Integer, DetailedProvider> getDetailedProvidersById() {
        return this.detailedProvidersById;
    }

    public List<DetailedProvider> getDetailedProviders() {
        return this.detailedProviders;
    }

    public List<DataTransfer> getDataTransfers() {
        return dataTransfers;
    }

    public List<DataTransfer> getDataTransfersDownload() {
        return dataTransfersDownload;
    }

    public List<DataTransfer> getDataTransfersUpload() {
        return dataTransfersUpload;
    }

    public Map<Long, DataTransfer> getDataTransfersById() {
        return dataTransfersById;
    }
}
