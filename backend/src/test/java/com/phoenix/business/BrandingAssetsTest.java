package com.phoenix.business;

import com.phoenix.business.service.BrandingAssets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BrandingAssetsTest {
 Path directory=Path.of("target","branding-test-"+UUID.randomUUID());
 @Test void readsOnlyProvisionedRasterFiles() throws Exception {
  byte[] expected={1,2,3}; Files.createDirectories(directory); Files.write(directory.resolve("gym.jpg"),expected); BrandingAssets assets=new BrandingAssets(directory.toString());
  assertArrayEquals(expected,assets.read(BrandingAssets.PREFIX+"gym.jpg"));
  for(String url:new String[]{"https://localhost/private.jpg",BrandingAssets.PREFIX+"../gym.jpg",BrandingAssets.PREFIX+"logo.svg",BrandingAssets.PREFIX+"missing.png"})assertNull(assets.read(url));
  Files.write(directory.resolve("large.png"),new byte[2_000_001]); assertNull(assets.read(BrandingAssets.PREFIX+"large.png"));
 }
 @Test void storesOnlyValidPngAndJpegFiles() throws Exception {
  BrandingAssets assets=new BrandingAssets(directory.toString()); byte[] png={(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a,1,2,3};
  String url=assets.store(png); assertTrue(url.matches("/api/public/branding-assets/logo-[A-Za-z0-9-]+\\.png")); assertArrayEquals(png,assets.read(url));
  assertThrows(IllegalArgumentException.class,()->assets.store("not an image".getBytes())); assertThrows(IllegalArgumentException.class,()->assets.store(new byte[2_000_001]));
 }
}
