package no.nav.opptjening.nais;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class NaisHttpServer implements Runnable {
    public static final int DEFAULT_PORT = 8080;

    private static final Logger LOG = LoggerFactory.getLogger(NaisHttpServer.class);

    private final Server server;

    private final int port;
    private final CollectorRegistry registry;

    public NaisHttpServer() {
        this(DEFAULT_PORT, CollectorRegistry.defaultRegistry);
    }

    public NaisHttpServer(int port, CollectorRegistry registry) {
        this.port = port;
        this.registry = registry;
        this.server = createServer(port);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        handler.addServletWithMapping(new ServletHolder(new LivenessEndpoint()), "/isAlive");
        handler.addServletWithMapping(new ServletHolder(new ReadynessEndpoint()), "/isReady");

        HttpServlet metricsServlet = new MetricsServlet(this.registry);
        handler.addServletWithMapping(new ServletHolder(metricsServlet), "/metrics");
    }

    private static Server createServer(int port) {
        Server server = new Server(new QueuedThreadPool(5, 1));

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[]{connector});

        return server;
    }

    public void run() {
        DefaultExports.initialize();

        try {
            LOG.info("Starting http server on port " + port);
            server.start();
            server.setStopAtShutdown(true);
        } catch (Exception e) {
            LOG.error("Unable to start http server", e);
        }
    }

    static class LivenessEndpoint extends HttpServlet {
        @Override
        protected void doGet( HttpServletRequest request,
                              HttpServletResponse response ) throws ServletException,
                IOException {
            response.setContentType("text/plain");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("ALIVE");
        }
    }

    static class ReadynessEndpoint extends HttpServlet {
        @Override
        protected void doGet( HttpServletRequest request,
                              HttpServletResponse response ) throws ServletException,
                IOException {
            response.setContentType("text/plain");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("READY");
        }
    }
}
