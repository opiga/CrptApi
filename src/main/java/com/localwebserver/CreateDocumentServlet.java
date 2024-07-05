package com.localwebserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@WebServlet("/create")
public class CreateDocumentServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CreateDocumentServlet.class);
    private CrptApi api;

    @Override
    public void init() {
        api = new CrptApi(TimeUnit.MINUTES, 2);
        logger.info("CrptApi initialized");
    }

    public CreateDocumentServlet() {
        super();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.info("Received POST request to create document");

        try {
            String contentType = request.getContentType();
            if (contentType == null || !contentType.startsWith("application/json")) {
                throw new IllegalArgumentException("Content-Type must be application/json");
            }

            BufferedReader reader = request.getReader();
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            logger.info("DOC: ", stringBuilder);

            ObjectMapper objectMapper = new ObjectMapper();
            CrptApi.Document document = objectMapper.readValue(stringBuilder.toString(), CrptApi.Document.class);

            String signature = request.getHeader("Signature");
            api.createDocument(document, signature);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("Document created successfully");
        } catch (Exception e) {
            logger.error("Error creating document", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error creating document: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.getWriter().write("CreateDocumentServlet is working");
    }

    @Override
    public void destroy() {
        api.shutdown();
    }
}