package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;
import java.util.Optional;

class EmptyMetadata implements AssetMetadata {

    @Override
    public String mimeType() {
        return MediaType.WILDCARD;
    }

    @Override
    public Map<String, String> all() {
        return Map.of();
    }

    @Override
    public Optional<NodeProperties> properties() {
        return Optional.empty();
    }
}
