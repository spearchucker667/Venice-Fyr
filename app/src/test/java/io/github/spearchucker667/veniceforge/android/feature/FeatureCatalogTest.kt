package io.github.spearchucker667.veniceforge.android.feature

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureCatalogTest {
    @Test fun mirrorsCanonicalDesktopFeatureCount() {
        assertEquals(22, FeatureCatalog.all.size)
        assertTrue(FeatureCatalog.all.map { it.id }.toSet().size == FeatureCatalog.all.size)
    }
}
