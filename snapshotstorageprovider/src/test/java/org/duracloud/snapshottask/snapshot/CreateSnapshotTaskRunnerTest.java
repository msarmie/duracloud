/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshottask.snapshot;

import org.duracloud.common.constant.Constants;
import org.duracloud.common.model.AclType;
import org.duracloud.common.util.IOUtil;
import org.duracloud.common.web.RestHttpHelper;
import org.duracloud.snapshot.dto.CreateSnapshotBridgeResult;
import org.duracloud.snapshot.dto.CreateSnapshotTaskParameters;
import org.duracloud.snapshot.dto.CreateSnapshotTaskResult;
import org.duracloud.snapshot.dto.SnapshotStatus;
import org.duracloud.snapshotstorage.SnapshotStorageProvider;
import org.duracloud.storage.error.TaskException;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * @author Bill Branan
 *         Date: 1/29/14
 */
public class CreateSnapshotTaskRunnerTest {

    private SnapshotStorageProvider snapshotProvider;
    private RestHttpHelper restHelper;
    private CreateSnapshotTaskRunner taskRunner;

    private String dcHost = "instance-host";
    private String dcPort = "instance-port";
    private String dcStoreId = "store-id";
    private String dcAccountName = "account-name";
    private String dcSnapshotUser = "snapshot-user";
    private String bridgeHost = "bridge-host";
    private String bridgePort = "bridge-port";
    private String bridgeUser = "bridge-user";
    private String bridgePass = "bridge-pass";

    @Before
    public void setup() {
        snapshotProvider = EasyMock.createMock("SnapshotStorageProvider",
                                               SnapshotStorageProvider.class);
        restHelper = EasyMock.createMock("RestHttpHelper", RestHttpHelper.class);
        taskRunner =
            new CreateSnapshotTaskRunner(snapshotProvider, dcHost, dcPort,
                                         dcStoreId, dcAccountName,
                                         dcSnapshotUser, bridgeHost,
                                         bridgePort, bridgeUser, bridgePass);
    }

    private void replayMocks() {
        EasyMock.replay(snapshotProvider, restHelper);
    }

    @After
    public void tearDown() throws IOException {
        EasyMock.verify(snapshotProvider, restHelper);
    }

    @Test
    public void testGetName() {
        replayMocks();
        assertEquals("create-snapshot", taskRunner.getName());
    }

    @Test
    public void testBuildSnapshotURL() {
        replayMocks();

        String snapshotId = "snapshot-id";

        String snapshotUrl = taskRunner.buildSnapshotURL(snapshotId);
        String expectedUrl = "http://"+ bridgeHost + ":" + bridgePort +
                             "/bridge/snapshot/" + snapshotId;
        assertEquals(expectedUrl, snapshotUrl);
    }

    @Test
    public void testBuildSnapshotBody() {
        replayMocks();

        String spaceId = "space-id";
        String description = "description";
        String userEmail = "user-email";
        CreateSnapshotTaskParameters taskParams =
            new CreateSnapshotTaskParameters();
        taskParams.setSpaceId(spaceId);
        taskParams.setDescription(description);
        taskParams.setUserEmail(userEmail);

        String result = taskRunner.buildSnapshotBody(taskParams);
        String cleanResult = result.replaceAll("\\s+", "");

        assertThat(cleanResult, containsString("\"host\":\""+dcHost+"\""));
        assertThat(cleanResult, containsString("\"port\":\""+dcPort+"\""));
        assertThat(cleanResult, containsString("\"storeId\":\""+dcStoreId+"\""));
        assertThat(cleanResult, containsString("\"spaceId\":\""+spaceId+"\""));
        assertThat(cleanResult, containsString("\"description\":\""+description+"\""));
        assertThat(cleanResult, containsString("\"userEmail\":\""+userEmail+"\""));
    }

    @Test
    public void testSetSnapshotUserPermissions() {
        String spaceId = "space-id";
        String aclUserName = "acl-user-name";
        AclType aclValue = AclType.WRITE;

        Map<String, AclType> spaceACLs = new HashMap<>();
        spaceACLs.put(aclUserName, aclValue);
        EasyMock.expect(snapshotProvider.getSpaceACLs(spaceId))
                .andReturn(spaceACLs);

        Capture<Map<String, AclType>> spaceACLsCapture = new Capture<>();
        snapshotProvider.setSpaceACLs(EasyMock.eq(spaceId),
                                      EasyMock.capture(spaceACLsCapture));

        replayMocks();

        taskRunner.setSnapshotUserPermissions(spaceId);
        Map<String, AclType> capSpaceACLs = spaceACLsCapture.getValue();
        assertEquals(capSpaceACLs.get(aclUserName), aclValue);
        String user = StorageProvider.PROPERTIES_SPACE_ACL + dcSnapshotUser;
        assertEquals(capSpaceACLs.get(user), AclType.READ);
    }

    @Test
    public void testBuildSnapshotProps() throws Exception {
        replayMocks();

        Map<String, String> propsMap = new HashMap<>();
        propsMap.put("one", "two");
        propsMap.put("three", "four");
        String props = taskRunner.buildSnapshotProps(propsMap);
        assertTrue(props.contains("one=two"));
        assertTrue(props.contains("three=four"));

        Properties realProps = new Properties();
        realProps.load(new StringReader(props));
        assertEquals("two", realProps.getProperty("one"));
        assertEquals("four", realProps.getProperty("three"));
    }

    @Test
    public void testStoreSnapshotProps() {
        String spaceId = "space-id";
        String props = "one=two";

        EasyMock.expect(
            snapshotProvider.addContent(EasyMock.eq(spaceId),
                                     EasyMock.eq(Constants.SNAPSHOT_ID),
                                     EasyMock.eq("text/x-java-properties"),
                                     EasyMock.<Map<String, String>>isNull(),
                                     EasyMock.eq((long)props.length()),
                                     EasyMock.<String>anyObject(),
                                     EasyMock.<InputStream>anyObject()))
                .andReturn("success!");
        replayMocks();

        taskRunner.storeSnapshotProps(spaceId, props);
    }

    @Test
    public void testCallBridgeSuccess() throws Exception {
        String snapshotId = "snapshot-id";
        String snapshotURL = "snapshot-url";
        String snapshotBody = "snapshot-body";

        CreateSnapshotBridgeResult bridgeResult =
            new CreateSnapshotBridgeResult(snapshotId,
                                           SnapshotStatus.INITIALIZED);
        InputStream resultStream =
            IOUtil.writeStringToStream(bridgeResult.serialize());

        RestHttpHelper.HttpResponse response =
            new RestHttpHelper.HttpResponse(201, null, null, resultStream);
        EasyMock.expect(restHelper.put(snapshotURL, snapshotBody, null))
                .andReturn(response);

        replayMocks();

        String callResult =
            taskRunner.callBridge(restHelper, snapshotURL, snapshotBody);

        CreateSnapshotTaskResult taskResult =
            CreateSnapshotTaskResult.deserialize(callResult);
        assertEquals(snapshotId, taskResult.getSnapshotId());
        assertEquals(SnapshotStatus.INITIALIZED, taskResult.getStatus());
    }

    @Test
    public void testCallBridgeFailure() throws Exception {
        String snapshotURL = "snapshot-url";
        String snapshotBody = "snapshot-body";

        InputStream resultStream = IOUtil.writeStringToStream("Error");
        RestHttpHelper.HttpResponse response =
            new RestHttpHelper.HttpResponse(500, null, null, resultStream);
        EasyMock.expect(restHelper.put(snapshotURL, snapshotBody, null))
                .andReturn(response);

        replayMocks();

        try {
            taskRunner.callBridge(restHelper, snapshotURL, snapshotBody);
            fail("Exception expected on 500 response");
        } catch(TaskException e) {
        }
    }

}