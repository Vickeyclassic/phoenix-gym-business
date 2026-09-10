package com.phoenix.business.web;

import com.phoenix.business.service.BrandingAssets;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BrandingAssetController {
    private final BrandingAssets assets;

    @GetMapping("/api/public/branding-assets/{name}")
    public ResponseEntity<byte[]> get(@PathVariable String name) throws IOException {
        byte[] bytes = assets.read(BrandingAssets.PREFIX + name);
        if (bytes == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(name.endsWith(".png")
                ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG).body(bytes);
    }
}
