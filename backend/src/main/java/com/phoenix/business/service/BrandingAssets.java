package com.phoenix.business.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Local raster branding assets only; remote URLs are never fetched by the backend. */
@Service
public class BrandingAssets {
 public static final String PREFIX="/api/public/branding-assets/"; public static final int MAX_BYTES=2_000_000; private final Path directory;
 public BrandingAssets(@Value("${app.branding-directory:./branding}") String directory){this.directory=Path.of(directory).toAbsolutePath().normalize();}
 public String store(byte[] bytes)throws IOException{if(bytes==null||bytes.length==0)throw new IllegalArgumentException("Choose a PNG or JPEG logo");if(bytes.length>MAX_BYTES)throw new IllegalArgumentException("Logo must be 2 MB or smaller");String extension=imageExtension(bytes);if(extension==null)throw new IllegalArgumentException("Logo must be a valid PNG or JPEG image");Files.createDirectories(directory);String name="logo-"+UUID.randomUUID()+extension;Path target=directory.resolve(name).normalize();if(!target.getParent().equals(directory))throw new IllegalArgumentException("Invalid logo path");Files.write(target,bytes,StandardOpenOption.CREATE_NEW);return PREFIX+name;}
 public byte[] read(String url)throws IOException{if(url==null||!url.startsWith(PREFIX))return null;String name=url.substring(PREFIX.length());if(!name.matches("[A-Za-z0-9][A-Za-z0-9_-]*\\.(jpg|jpeg|png)"))return null;Path file=directory.resolve(name);if(!Files.isRegularFile(file)||!file.toRealPath().startsWith(directory.toRealPath())||Files.size(file)>MAX_BYTES)return null;return Files.readAllBytes(file);}
 private String imageExtension(byte[] b){if(b.length>=8&&(b[0]&255)==0x89&&b[1]==0x50&&b[2]==0x4e&&b[3]==0x47&&b[4]==0x0d&&b[5]==0x0a&&b[6]==0x1a&&b[7]==0x0a)return ".png";if(b.length>=3&&(b[0]&255)==0xff&&(b[1]&255)==0xd8&&(b[2]&255)==0xff)return ".jpg";return null;}
}
