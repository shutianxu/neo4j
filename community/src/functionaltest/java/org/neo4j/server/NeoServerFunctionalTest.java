/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.server;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.server.logging.InMemoryAppender;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class NeoServerFunctionalTest {
    
    public NeoServer server;

    @Before
    public void setup() throws Exception {
        server = ServerTestUtils.initializeServerWithRandomTemporaryDatabaseDirectory();
    }

    @After
    public void tearDown() {
        ServerTestUtils.nukeServer();
    }

    @Test
    public void shouldDefaultToSensiblePortIfNoneSpecifiedInConfig() throws Exception {
        System.setProperty(NeoServer.NEO_CONFIG_FILE_KEY, configWithoutWebServerPort().getAbsolutePath());
        ServerTestUtils.nukeServer();
        ServerTestUtils.initializeServerWithRandomTemporaryDatabaseDirectoryOnDefaultPort();

        Client client = Client.create();
        ClientResponse response = client.resource(server.webadminUri()).get(ClientResponse.class);

        assertThat(response.getStatus(), is(200));
    }
   

    @Test
    public void shouldRedirectRootToWebadmin() throws Exception {

        Client client = Client.create();
        assertFalse( server.baseUri().toString().contains( "webadmin" ) );
        ClientResponse response = client.resource(server.baseUri()).get(ClientResponse.class);
        assertThat(response.getStatus(), is(200));
        assertThat(response.toString(), containsString( "webadmin" ));
    }
    
    @Ignore
    @Test
    public void shouldSurviveDoubleMounts() throws Exception {

        Client client = Client.create();
        assertFalse( server.baseUri().toString().contains( "testing" ) );
        ClientResponse response = client.resource(server.baseUri() + "db/manage/testing").get(ClientResponse.class);
        assertThat(response.getStatus(), is(200));
        assertThat(response.toString(), containsString( "dupOne" ));
    }
    

    @Test
    public void serverShouldProvideAWelcomePage() {
        Client client = Client.create();
        ClientResponse response = client.resource(server.webadminUri()).get(ClientResponse.class);

        assertThat(response.getStatus(), is(200));
        assertThat(response.getHeaders().getFirst("Content-Type"), containsString("html"));
    }

    @Test
    public void shouldMakeJAXRSClassesAvailableViaHTTP() throws URISyntaxException {
        ClientResponse response = Client.create().resource(server.restApiUri()).get(ClientResponse.class);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void shouldLogShutdown() {
        InMemoryAppender appender = new InMemoryAppender(NeoServer.log);
        NeoServer.shutdown();
        assertThat(appender.toString(), containsString("INFO - Successfully shutdown Neo Server on port [7474], database ["));
    }

    @Test
    public void shouldComplainIfServerPortIsAlreadyTaken() throws IOException {
        InMemoryAppender appender = new InMemoryAppender(NeoServer.log);
        NeoServer s1 = new NeoServer();
        s1.start();

        assertThat(appender.toString(), containsString(String.format("ERROR - Failed to start Neo Server on port [%s]", server.restApiUri()
                .getPort())));
        s1.stop();
    }

    private File configWithoutWebServerPort() throws IOException {
        File tempPropertyFile = ServerTestUtils.createTempPropertyFile();
        ServerTestUtils.writePropertyToFile("org.neo4j.server.database.location", "/tmp/neo/no-webserver-port.db", tempPropertyFile);
        ServerTestUtils.writePropertyToFile("org.neo4j.server.webservice.packages", "org.neo4j.server.web", tempPropertyFile);

        return tempPropertyFile;
    }
}
