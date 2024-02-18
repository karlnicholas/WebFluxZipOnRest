package com.example.fluxplay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

@SpringBootApplication
@RestController
@RequestMapping("zip")
public class WebFluxZipOnRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebFluxZipOnRestApplication.class, args);
    }

    @GetMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Flux<DataBuffer>> getStuffFlux(ServerHttpResponse serverHttpResponse) throws Exception {
        URL url = this.getClass().getResource("/files");
        if ( url != null ) {
            try (Stream<Path> fileStream = Files.list(Paths.get(url.toURI()))) {
                DataBufferFactory dataBufferFactory = serverHttpResponse.bufferFactory();
                try (OutputStreamToDataBuffer outputStreamToDataBuffer = new OutputStreamToDataBuffer(dataBufferFactory)) {
                    List<Path> paths = fileStream.toList();
                    Flux<DataBuffer> flux = Flux.generate(() -> new ZipWriterState(paths, new ZipOutputStream(outputStreamToDataBuffer)),
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
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return zipWriterState;
                            });

                    return ResponseEntity
                            .ok()
                            .headers(httpHeaders -> httpHeaders.setContentDisposition(ContentDisposition.builder("attachment").filename("images.zip").build()))
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(flux);
                }
            }
        } else {
            return ResponseEntity.noContent().build();
        }
    }

}
