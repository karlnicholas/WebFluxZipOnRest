package com.example.fluxplay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
@RestController
@RequestMapping
public class WebFluxZipOnRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebFluxZipOnRestApplication.class, args);
    }

    @GetMapping(path = "zip", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> getStuffFlux(ServerHttpResponse serverHttpResponse) {
        return Mono.justOrEmpty(this.getClass().getResource("/files"))
                .map(url->ResponseEntity
                    .ok()
                    .headers(httpHeaders -> httpHeaders.setContentDisposition(ContentDisposition.builder("attachment").filename("images.zip").build()))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(getDataBufferFlux(url, serverHttpResponse.bufferFactory()))
                ).defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @Bean
    public RouterFunction<ServerResponse> getRoute() {
        return route()
                .GET("/zip2", this::getZipFlux)
                .build();
    }

    public Mono<ServerResponse> getZipFlux(ServerRequest request) {
        return Mono.justOrEmpty(this.getClass().getResource("/files"))
                .flatMap(url->ServerResponse
                        .ok()
                        .headers(httpHeaders -> httpHeaders.setContentDisposition(ContentDisposition.builder("attachment").filename("images.zip").build()))
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(BodyInserters.fromDataBuffers(getDataBufferFlux(url, request.exchange().getResponse().bufferFactory())))
                ).switchIfEmpty(ServerResponse.noContent().build());

    }

    private Flux<DataBuffer> getDataBufferFlux(URL url, DataBufferFactory dataBufferFactory) {
        try (Stream<Path> fileStream = Files.list(Paths.get(url.toURI()))) {
            try (OutputStreamToDataBuffer outputStreamToDataBuffer = new OutputStreamToDataBuffer(dataBufferFactory)) {
                List<Path> paths = fileStream.toList();
                return Flux.generate(() -> new ZipWriterState(paths, new ZipOutputStream(outputStreamToDataBuffer)),
                        (zipWriterState, synchronousSink) -> {
                            try {
                                if (zipWriterState.writeNext()) {
                                    DataBuffer db = outputStreamToDataBuffer.getDataBuffer();
                                    if (db != null) {
                                        synchronousSink.next(db);
                                        outputStreamToDataBuffer.resetDataBuffer();
                                    } else {
                                        synchronousSink.next(dataBufferFactory.allocateBuffer(0));
                                    }
                                } else
                                    synchronousSink.complete();
                            } catch (IOException e) {
                                synchronousSink.error(e);
                            }
                            return zipWriterState;
                        });
            }
        } catch (IOException | URISyntaxException e) {
            return Flux.error(e);
        }
    }

}
