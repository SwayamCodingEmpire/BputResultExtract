package com.result.bputresultextract.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bputResultExtractOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Development Server");

        Contact contact = new Contact();
        contact.setName("BPUT Result Extract API");

        Info info = new Info()
                .title("BPUT Result Extraction API")
                .version("1.0.0")
                .description("API for extracting student result data from BPUT (Biju Patnaik University of Technology) " +
                        "across multiple academic sessions. The API processes registration numbers, fetches semester and " +
                        "subject details from BPUT servers, and returns the data in CSV format.")
                .contact(contact)
                .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT"));

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
