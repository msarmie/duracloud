package org.duracloud.duraservice.config;

import org.duracloud.duraservice.mgmt.ServiceConfigUtil;
import org.duracloud.serviceconfig.ServiceInfo;
import org.duracloud.serviceconfig.SystemConfig;
import org.duracloud.serviceconfig.user.Option;
import org.duracloud.serviceconfig.user.SingleSelectUserConfig;
import org.duracloud.serviceconfig.user.TextUserConfig;
import org.duracloud.serviceconfig.user.UserConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Woods
 *         Date: Aug 3, 2010
 */
public class FixityServiceInfo extends AbstractServiceInfo {

    private final String userConfigVersion = "1.0";
    private final String displayName = "Fixity Service";

    private enum Mode {
        ALL_IN_ONE_LIST("all-in-one-for-list",
                        "Verify the fixity of a list of items"),
        ALL_IN_ONE_SPACE("all-in-one-for-space",
                         "Verify the fixity of an entire Space"),
        GENERATE_LIST("generate-for-list",
                      "Generate fixity information for a list of items"),
        GENERATE_SPACE("generate-for-space",
                       "Generate fixity information for an entire Space"),
        COMPARE("compare", "Compare two different fixity reports");

        private String key;
        private String desc;

        private Mode(String key, String desc) {
            this.key = key;
            this.desc = desc;
        }

        public String toString() {
            return getKey();
        }

        protected String getKey() {
            return key;
        }

        protected Option asOption() {
            return new Option(this.desc, this.key, false);
        }
    }

    @Override
    public ServiceInfo getServiceXml(int index, String version) {
        ServiceInfo fsService = new ServiceInfo();
        fsService.setId(index);
        fsService.setContentId("fixityservice-" + version + ".zip");
        fsService.setDescription(getDescription());
        fsService.setDisplayName(displayName);
        fsService.setUserConfigVersion(userConfigVersion);
        fsService.setServiceVersion(version);
        fsService.setMaxDeploymentsAllowed(1); // single deployment until otherwise supported

        fsService.setSystemConfigs(getSystemConfigs());
        fsService.setUserConfigs(getUserConfigs());
        fsService.setDeploymentOptions(getSimpleDeploymentOptions());

        return fsService;
    }

    private String getDescription() {
        return "The Fixity service provides the ability to verify " +
            "that the content held within DuraCloud has maintained its bit " +
            "integrity. There are five modes of operation: " +
            "(1) All-in-one generation of system MD5s for items in provided " +
            "listing and verification " +
            "(2) All-in-one generation of system MD5s over entire space(s) " +
            "and verification to provided listing " +
            "(3) Generation of system MD5 listing for items in provided listing" +
            "(4) Generation of system MD5 listing over entire space, and " +
            "(5) Comparison of two provided MD5 listings.";
    }

    private List<SystemConfig> getSystemConfigs() {
        List<SystemConfig> systemConfig = new ArrayList<SystemConfig>();

        SystemConfig host = new SystemConfig("duraStoreHost",
                                             ServiceConfigUtil.STORE_HOST_VAR,
                                             "localhost");
        SystemConfig port = new SystemConfig("duraStorePort",
                                             ServiceConfigUtil.STORE_PORT_VAR,
                                             "8080");
        SystemConfig context = new SystemConfig("duraStoreContext",
                                                ServiceConfigUtil.STORE_CONTEXT_VAR,
                                                "durastore");
        SystemConfig username = new SystemConfig("username",
                                                 ServiceConfigUtil.STORE_USER_VAR,
                                                 "no-username");
        SystemConfig password = new SystemConfig("password",
                                                 ServiceConfigUtil.STORE_PWORD_VAR,
                                                 "no-password");

        systemConfig.add(host);
        systemConfig.add(port);
        systemConfig.add(context);
        systemConfig.add(username);
        systemConfig.add(password);
        return systemConfig;
    }

    private List<UserConfig> getUserConfigs() {
        List<UserConfig> fsServiceUserConfig = new ArrayList<UserConfig>();

        fsServiceUserConfig.add(getModeDefinitionSelection());
        fsServiceUserConfig.add(getHashMethodSelection());
        fsServiceUserConfig.add(getSaltConfig());
        //fsServiceUserConfig.add(getFailFastBoolean()); option may not be needed.
        fsServiceUserConfig.add(getStorageProviderSelection());
        fsServiceUserConfig.add(getSpaceOfProvidedListingSelection());
        fsServiceUserConfig.add(getSpaceOfProvidedListingBSelection());
        fsServiceUserConfig.add(getSpaceOfGeneratedHashListingSelection());
        fsServiceUserConfig.add(getContentIdOfProvidedListingConfig());
        fsServiceUserConfig.add(getContentIdOfProvidedListingBConfig());
        fsServiceUserConfig.add(getSpaceOfOutputConfig());
        fsServiceUserConfig.add(getContentIdOfGeneratedListingConfig());
        fsServiceUserConfig.add(getContentIdOfReportConfig());

        return fsServiceUserConfig;
    }

    private SingleSelectUserConfig getModeDefinitionSelection() {
        List<Option> modeOptions = new ArrayList<Option>();
        modeOptions.add(Mode.ALL_IN_ONE_LIST.asOption());
        modeOptions.add(Mode.ALL_IN_ONE_SPACE.asOption());
        modeOptions.add(Mode.GENERATE_LIST.asOption());
        modeOptions.add(Mode.GENERATE_SPACE.asOption());
        modeOptions.add(Mode.COMPARE.asOption());

        SingleSelectUserConfig modeSelection = new SingleSelectUserConfig("mode",
                                                                          "Service Mode",
                                                                          modeOptions,
                                                                          ServiceConfigUtil.EXCLUSION_DEFINITION);
        return modeSelection;
    }

    private SingleSelectUserConfig getHashMethodSelection() {
        List<Option> trustLevelOptions = new ArrayList<Option>();
        trustLevelOptions.add(new Option("The storage provider",
                                         "stored",
                                         false));
        trustLevelOptions.add(new Option("The files themselves",
                                         "generated",
                                         false));
        trustLevelOptions.add(new Option("The files themselves, with salt",
                                         "salted",
                                         false));

        return new SingleSelectUserConfig("hashApproach",
                                          "Get fixity information from...",
                                          trustLevelOptions,
                                          or(Mode.ALL_IN_ONE_LIST,
                                             Mode.ALL_IN_ONE_SPACE,
                                             Mode.GENERATE_LIST,
                                             Mode.GENERATE_SPACE));
    }

    private UserConfig getSaltConfig() {
        String emptyValue = "";
        return new TextUserConfig("salt",
                                  "Salt",
                                  emptyValue,
                                  or(Mode.ALL_IN_ONE_SPACE,
                                     Mode.ALL_IN_ONE_LIST,
                                     Mode.GENERATE_SPACE,
                                     Mode.GENERATE_LIST));
    }

    private SingleSelectUserConfig getFailFastBoolean() {
        return new SingleSelectUserConfig("failFast",
                                          "Exit Immediately on Failure or Error",
                                          getBooleanOptions(),
                                          or(Mode.ALL_IN_ONE_SPACE,
                                             Mode.ALL_IN_ONE_LIST,
                                             Mode.COMPARE));
    }

    private List<Option> getBooleanOptions() {
        List<Option> booleanOptions = new ArrayList<Option>();
        booleanOptions.add(new Option("true", "true", false));
        booleanOptions.add(new Option("false", "false", false));
        return booleanOptions;
    }

    private SingleSelectUserConfig getStorageProviderSelection() {
        List<Option> storeOptions = new ArrayList<Option>();
        Option stores = new Option("Stores",
                                   ServiceConfigUtil.STORES_VAR,
                                   false);
        storeOptions.add(stores);
        return new SingleSelectUserConfig("storeId", "Store", storeOptions);
    }

    private SingleSelectUserConfig getSpaceOfProvidedListingSelection() {
        return new SingleSelectUserConfig("providedListingSpaceIdA",
                                          "Space with input listing",
                                          getSpaceOptions(),
                                          or(Mode.ALL_IN_ONE_SPACE,
                                             Mode.ALL_IN_ONE_LIST,
                                             Mode.GENERATE_LIST,
                                             Mode.COMPARE));
    }

    private SingleSelectUserConfig getSpaceOfProvidedListingBSelection() {
        return new SingleSelectUserConfig("providedListingSpaceIdB",
                                          "Space with second input listing",
                                          getSpaceOptions(),
                                          Mode.COMPARE.getKey());
    }

    private SingleSelectUserConfig getSpaceOfGeneratedHashListingSelection() {
        return new SingleSelectUserConfig("targetSpaceId",
                                          "Space containing content items",
                                          getSpaceOptions(),
                                          or(Mode.GENERATE_SPACE,
                                             Mode.ALL_IN_ONE_SPACE));
    }

    private TextUserConfig getContentIdOfProvidedListingConfig() {
        return new TextUserConfig("providedListingContentIdA",
                                  "Input listing name",
                                  "item-listing.csv",
                                  or(Mode.ALL_IN_ONE_LIST,
                                     Mode.ALL_IN_ONE_SPACE,
                                     Mode.GENERATE_LIST,
                                     Mode.COMPARE));
    }

    private TextUserConfig getContentIdOfProvidedListingBConfig() {
        return new TextUserConfig("providedListingContentIdB",
                                  "Second input listing name",
                                  "fingerprints.csv",
                                  Mode.COMPARE.getKey());
    }

    private SingleSelectUserConfig getSpaceOfOutputConfig() {
        return new SingleSelectUserConfig("outputSpaceId",
                                          "Output space",
                                          getSpaceOptions(),
                                          or(Mode.ALL_IN_ONE_LIST,
                                             Mode.ALL_IN_ONE_SPACE,
                                             Mode.GENERATE_LIST,
                                             Mode.GENERATE_SPACE,
                                             Mode.COMPARE));
    }

    private List<Option> getSpaceOptions() {
        List<Option> spaceOptions = new ArrayList<Option>();
        spaceOptions.add(new Option("Spaces",
                                    ServiceConfigUtil.SPACES_VAR,
                                    false));
        return spaceOptions;
    }

    private TextUserConfig getContentIdOfGeneratedListingConfig() {
        return new TextUserConfig("outputContentId",
                                  "Output listing name",
                                  "fingerprints.csv",
                                  or(Mode.ALL_IN_ONE_LIST,
                                     Mode.ALL_IN_ONE_SPACE,
                                     Mode.GENERATE_LIST,
                                     Mode.GENERATE_SPACE));
    }

    private UserConfig getContentIdOfReportConfig() {
        return new TextUserConfig("reportContentId",
                                  "Output report name",
                                  "fixity-report.csv",
                                  or(Mode.ALL_IN_ONE_LIST,
                                     Mode.ALL_IN_ONE_SPACE,
                                     Mode.COMPARE));
    }

    private String or(Mode... modes) {
        StringBuilder sb = new StringBuilder();
        for (Mode mode : modes) {
            sb.append(mode.getKey());
            sb.append("|");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

}