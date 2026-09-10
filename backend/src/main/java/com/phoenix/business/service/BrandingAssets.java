package com.phoenix.business.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Only operator-provisioned local raster assets; never fetch a settings URL. */
@Service
public class BrandingAssets {
    public static final String PREFIX = "/api/public/branding-assets/";
    private final Path directory;

    public BrandingAssets(@Value("${app.branding-directory:./branding}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public byte[] read(String url) throws IOException {
        if (url == null || !url.startsWith(PREFIX)) return null;
        String name = url.substring(PREFIX.length());
        if (!name.matches("[A-Za-z0-9][A-Za-z0-9_-]*\\.(jpg|jpeg|png)")) return null;
        Path file = directory.resolve(name);
        if (!Files.isRegularFile(file) || !file.toRealPath().startsWith(directory.toRealPath())
                || Files.size(file) > 2_000_000) return null;
        return Files.readAllBytes(file);
    }
}
