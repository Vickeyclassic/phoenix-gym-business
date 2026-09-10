package com.phoenix.business;

import com.phoenix.business.service.BrandingAssets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrandingAssetsTest {
    @TempDir Path directory;
    @Test void readsOnlyProvisionedRasterFiles() throws Exception {
        byte[] expected = {1, 2, 3};
        Files.write(directory.resolve("gym.jpg"), expected);
        BrandingAssets assets = new BrandingAssets(directory.toString());
        assertArrayEquals(expected, assets.read(BrandingAssets.PREFIX + "gym.jpg"));
        for (String url : new String[]{"https://localhost/private.jpg", BrandingAssets.PREFIX + "../gym.jpg",
                BrandingAssets.PREFIX + "logo.svg", BrandingAssets.PREFIX + "missing.png"}) {
            assertNull(assets.read(url));
        }
        Files.write(directory.resolve("large.png"), new byte[2_000_001]);
        assertNull(assets.read(BrandingAssets.PREFIX + "large.png"));
    }
}
