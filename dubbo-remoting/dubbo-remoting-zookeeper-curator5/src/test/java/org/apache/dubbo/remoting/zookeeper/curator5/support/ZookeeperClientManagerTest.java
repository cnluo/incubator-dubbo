/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.remoting.zookeeper.curator5.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.curator5.Curator5ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.curator5.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.curator5.ZookeeperClientManager;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstructionWithAnswer;
import static org.mockito.Mockito.when;

/**
 * ZookeeperManagerTest
 */
class ZookeeperClientManagerTest {
    private ZookeeperClient zookeeperClient;
    private ZookeeperClientManager zookeeperClientManager;
    private static Curator5ZookeeperClient mockedCurator5ZookeeperClient;
    private static MockedConstruction<Curator5ZookeeperClient> mockedCurator5ZookeeperClientConstruction;
    private static int zookeeperServerPort1 = 2181;
    private static int zookeeperServerPort2 = 2182;
    private static String zookeeperConnectionAddress1 = "zookeeper://127.0.0.1:2181";
    private static String zookeeperConnectionAddress2 = "zookeeper://127.0.0.1:2182";

    @BeforeAll
    public static void beforeAll() {
        mockedCurator5ZookeeperClient = mock(Curator5ZookeeperClient.class);
        mockedCurator5ZookeeperClientConstruction =
                mockConstructionWithAnswer(Curator5ZookeeperClient.class, invocationOnMock -> invocationOnMock
                        .getMethod()
                        .invoke(mockedCurator5ZookeeperClient, invocationOnMock.getArguments()));
    }

    @BeforeEach
    public void setUp() throws Exception {
        zookeeperClient = new ZookeeperClientManager().connect(URL.valueOf(zookeeperConnectionAddress1 + "/service"));
        zookeeperClientManager = new ZookeeperClientManager();
    }

    @Test
    void testZookeeperClient() {
        assertThat(zookeeperClient, not(nullValue()));
        zookeeperClient.close();
    }

    @Test
    void testGetURLBackupAddress() {
        URL url = URL.valueOf(
                zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService?backup=127.0.0.1:" + 9099
                        + "&application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        List<String> stringList = zookeeperClientManager.getURLBackupAddress(url);
        Assertions.assertEquals(stringList.size(), 2);
        Assertions.assertEquals(stringList.get(0), "127.0.0.1:" + zookeeperServerPort1);
        Assertions.assertEquals(stringList.get(1), "127.0.0.1:9099");
    }

    @Test
    void testGetURLBackupAddressNoBack() {
        URL url = URL.valueOf(
                zookeeperConnectionAddress1
                        + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        List<String> stringList = zookeeperClientManager.getURLBackupAddress(url);
        Assertions.assertEquals(stringList.size(), 1);
        Assertions.assertEquals(stringList.get(0), "127.0.0.1:" + zookeeperServerPort1);
    }

    @Test
    void testFetchAndUpdateZookeeperClientCache() {
        URL url = URL.valueOf(
                zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService?backup=127.0.0.1:"
                        + zookeeperServerPort1 + ",127.0.0.1:" + zookeeperServerPort2
                        + "&application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        ZookeeperClient newZookeeperClient = zookeeperClientManager.connect(url);
        // just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(zookeeperClientManager.getZookeeperClientMap().size(), 2);
        Assertions.assertEquals(
                zookeeperClientManager.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort1),
                newZookeeperClient);

        URL url2 = URL.valueOf(
                "zookeeper://127.0.0.1:" + zookeeperServerPort1
                        + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        checkFetchAndUpdateCacheNotNull(url2);
        URL url3 = URL.valueOf(
                "zookeeper://127.0.0.1:8778/org.apache.dubbo.metadata.store.MetadataReport?backup=127.0.0.1:"
                        + zookeeperServerPort2
                        + "&address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        checkFetchAndUpdateCacheNotNull(url3);
    }

    private void checkFetchAndUpdateCacheNotNull(URL url) {
        List<String> addressList = zookeeperClientManager.getURLBackupAddress(url);
        ZookeeperClient zookeeperClient = zookeeperClientManager.fetchAndUpdateZookeeperClientCache(addressList);
        Assertions.assertNotNull(zookeeperClient);
    }

    @Test
    void testRepeatConnect() {
        URL url = URL.valueOf(
                zookeeperConnectionAddress1
                        + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf(
                zookeeperConnectionAddress1
                        + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = zookeeperClientManager.connect(url);
        // just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(zookeeperClientManager.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(
                zookeeperClientManager.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort1),
                newZookeeperClient);
        when(mockedCurator5ZookeeperClient.isConnected()).thenReturn(true);
        Assertions.assertTrue(newZookeeperClient.isConnected());

        ZookeeperClient newZookeeperClient2 = zookeeperClientManager.connect(url2);
        // just for connected
        newZookeeperClient2.getContent("/dubbo/test");
        Assertions.assertEquals(newZookeeperClient, newZookeeperClient2);
        Assertions.assertEquals(zookeeperClientManager.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(
                zookeeperClientManager.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort1),
                newZookeeperClient);
    }

    @Test
    void testNotRepeatConnect() {
        URL url = URL.valueOf(
                zookeeperConnectionAddress1
                        + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf(
                zookeeperConnectionAddress2
                        + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = zookeeperClientManager.connect(url);
        // just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(zookeeperClientManager.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(
                zookeeperClientManager.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort1),
                newZookeeperClient);

        ZookeeperClient newZookeeperClient2 = zookeeperClientManager.connect(url2);
        // just for connected
        newZookeeperClient2.getContent("/dubbo/test");
        Assertions.assertNotEquals(newZookeeperClient, newZookeeperClient2);
        Assertions.assertEquals(zookeeperClientManager.getZookeeperClientMap().size(), 2);
        Assertions.assertEquals(
                zookeeperClientManager.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort2),
                newZookeeperClient2);
    }

    @Test
    void testRepeatConnectForBackUpAdd() {

        URL url = URL.valueOf(
                zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService?backup=127.0.0.1:"
                        + zookeeperServerPort1
                        + "&application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf(
                zookeeperConnectionAddress1 + "/org.apache.dubbo.metadata.store.MetadataReport?backup=127.0.0.1:"
                        + zookeeperServerPort2
                        + "&address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        when(mockedCurator5ZookeeperClient.isConnected()).thenReturn(true);
        ZookeeperClient newZookeeperClient = zookeeperClientManager.connect(url);
        // just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(zookeeperClientManager.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(
                zookeeperClientManager.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort1),
                newZookeeperClient);

        ZookeeperClient newZookeeperClient2 = zookeeperClientManager.connect(url2);
        // just for connected
        newZookeeperClient2.getContent("/dubbo/test");
        Assertions.assertEquals(newZookeeperClient, newZookeeperClient2);
        Assertions.assertEquals(zookeeperClientManager.getZookeeperClientMap().size(), 2);
        Assertions.assertEquals(
                zookeeperClientManager.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort2),
                newZookeeperClient2);
    }

    @Test
    void testRepeatConnectForNoMatchBackUpAdd() {

        URL url = URL.valueOf(
                zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService?backup=127.0.0.1:"
                        + zookeeperServerPort1
                        + "&application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf(
                zookeeperConnectionAddress2
                        + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = zookeeperClientManager.connect(url);
        // just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(zookeeperClientManager.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(
                zookeeperClientManager.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort1),
                newZookeeperClient);

        ZookeeperClient newZookeeperClient2 = zookeeperClientManager.connect(url2);
        // just for connected
        newZookeeperClient2.getContent("/dubbo/test");
        Assertions.assertNotEquals(newZookeeperClient, newZookeeperClient2);
        Assertions.assertEquals(zookeeperClientManager.getZookeeperClientMap().size(), 2);
        Assertions.assertEquals(
                zookeeperClientManager.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort2),
                newZookeeperClient2);
    }

    @Test
    void testSameHostWithDifferentUser() {
        URL url1 = URL.valueOf("zookeeper://us1:pw1@127.0.0.1:" + zookeeperServerPort1 + "/path1");
        URL url2 = URL.valueOf("zookeeper://us2:pw2@127.0.0.1:" + zookeeperServerPort1 + "/path2");
        ZookeeperClient client1 = zookeeperClientManager.connect(url1);
        ZookeeperClient client2 = zookeeperClientManager.connect(url2);
        assertThat(client1, not(client2));
    }

    @AfterAll
    public static void afterAll() {
        mockedCurator5ZookeeperClientConstruction.close();
    }
}
