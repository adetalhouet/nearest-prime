package io.skupper.restclient;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.axle.ext.web.client.WebClient;
import io.vertx.axle.core.Vertx;
import io.vertx.ext.web.client.WebClientOptions;

@Path("/set_load")
public class LoadGen {

    int concurrency = 0;
    int inFlight    = 0;
    int total       = 0;
    int failures    = 0;
    String lastStatus = "<none>";

    @Inject
    Vertx vertx;

    private WebClient client;

    void initialize() {
        if (client != null) {
            client.close();
            client = null;
        }

        client = WebClient.create(vertx,
            new WebClientOptions()
                .setDefaultHost("nearestprime")
                .setDefaultPort(8888));
    }

    private void sendRequest() {
        inFlight++;
        total++;
        client.get(String.format("/post_work?id=%d", total - 1))
            .send()
            .whenComplete((resp, exception) -> {
                inFlight--;
                if (exception == null) {
                    lastStatus = resp.statusMessage();
                } else {
                    failures++;
                }
                if (resp.statusCode() != 200) {
                    initialize();
                }
                if (inFlight < concurrency) {
                    sendRequest();
                }
            });
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{val}")
    public String setLoad(@PathParam("val") String val) {
        initialize();

        int newVal;
        try {
            newVal = Integer.parseInt(val);
        } catch (Exception e) {
            newVal = concurrency;
        }

        concurrency = newVal;

        while (concurrency > inFlight) {
            sendRequest();
        }

        return String.format("Load set to %d (in-flight: %d, total: %d, failures: %d, last_status: %s)",
            concurrency, inFlight, total, failures, lastStatus);
    }
}


