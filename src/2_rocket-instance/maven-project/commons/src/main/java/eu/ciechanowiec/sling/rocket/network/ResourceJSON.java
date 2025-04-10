package eu.ciechanowiec.sling.rocket.network;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Iterator;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ResourceJSON implements Resource {

    private final Resource resource;

    ResourceJSON(Resource resource) {
        this.resource = resource;
    }

    @Override
    public @NotNull String getPath() {
        return resource.getPath();
    }

    @Override
    public @NotNull String getName() {
        return resource.getName();
    }

    @Override
    @JsonIgnore
    public @Nullable Resource getParent() {
        return resource.getParent();
    }

    @Override
    @JsonIgnore
    public @NotNull Iterator<Resource> listChildren() {
        return resource.listChildren();
    }

    @Override
    @JsonIgnore
    public @NotNull Iterable<Resource> getChildren() {
        return resource.getChildren();
    }

    @Override
    @JsonIgnore
    public @Nullable Resource getChild(@NotNull String relPath) {
        return resource.getChild(relPath);
    }

    @Override
    public @NotNull String getResourceType() {
        return resource.getResourceType();
    }

    @Override
    public @Nullable String getResourceSuperType() {
        return resource.getResourceSuperType();
    }

    @Override
    public boolean hasChildren() {
        return resource.hasChildren();
    }

    @Override
    @JsonIgnore
    public boolean isResourceType(String resourceType) {
        return resource.isResourceType(resourceType);
    }

    @Override
    public @NotNull ResourceMetadata getResourceMetadata() {
        return resource.getResourceMetadata();
    }

    @Override
    @JsonIgnore
    public @NotNull ResourceResolver getResourceResolver() {
        return resource.getResourceResolver();
    }

    @Override
    public @NotNull ValueMap getValueMap() {
        return resource.getValueMap();
    }

    @Override
    @JsonIgnore
    public <AdapterType> @Nullable AdapterType adaptTo(@NotNull Class<AdapterType> type) {
        return resource.adaptTo(type);
    }
}
