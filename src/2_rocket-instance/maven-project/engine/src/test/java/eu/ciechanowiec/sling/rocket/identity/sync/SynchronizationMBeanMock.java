package eu.ciechanowiec.sling.rocket.identity.sync;

import org.apache.jackrabbit.oak.spi.security.authentication.external.impl.jmx.SynchronizationMBean;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

class SynchronizationMBeanMock implements SynchronizationMBean {

    private final String[] externalUsers;

    SynchronizationMBeanMock(String... externalUsers) {
        this.externalUsers = Arrays.copyOf(externalUsers, externalUsers.length);
    }

    @Override
    public @NotNull String getSyncHandlerName() {
        return "";
    }

    @Override
    public @NotNull String getIDPName() {
        return "";
    }

    @Override
    public @NotNull String[] syncUsers(@NotNull String[] userIds, boolean purge) {
        return new String[0];
    }

    @Override
    public @NotNull String[] syncAllUsers(boolean purge) {
        return new String[0];
    }

    @Override
    public @NotNull String[] syncExternalUsers(@NotNull String[] externalIds) {
        return new String[0];
    }

    @Override
    public @NotNull String[] syncAllExternalUsers() {
        return Arrays.copyOf(externalUsers, externalUsers.length);
    }

    @Override
    public @NotNull String[] listOrphanedUsers() {
        return new String[0];
    }

    @Override
    public @NotNull String[] purgeOrphanedUsers() {
        return new String[0];
    }

    @Override
    public @NotNull String[] convertToDynamicMembership() {
        return new String[0];
    }
}
