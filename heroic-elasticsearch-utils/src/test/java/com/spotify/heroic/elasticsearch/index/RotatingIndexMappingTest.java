package com.spotify.heroic.elasticsearch.index;

import com.spotify.heroic.common.Duration;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;

public class RotatingIndexMappingTest {
    private RotatingIndexMapping rotating;

    private Duration interval = new Duration(1000, TimeUnit.MILLISECONDS);
    private String pattern = "index-%s";
    private int maxReadIndices = 2;
    private int maxWriteIndices = 1;

    @Before
    public void setup() {
        rotating = new RotatingIndexMapping(interval, maxReadIndices, maxWriteIndices, pattern);
    }

    @Test
    public void testReadIndex() throws NoIndexSelectedException {
        final String[] indices = rotating.readIndices(8000);
        assertArrayEquals(new String[]{"index-8000", "index-7000"}, indices);
    }

    @Test
    public void testEmptyReadIndex() throws NoIndexSelectedException {
        final String[] indices = rotating.readIndices(0);
        assertArrayEquals(new String[]{"index-0"}, indices);
    }

    @Test
    public void testWriteIndex() {
        final String[] indices = rotating.writeIndices(8000);
        assertArrayEquals(new String[]{"index-8000"}, indices);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoIndexSelectedException() throws IllegalArgumentException {
        new RotatingIndexMapping(interval, 0, 0, pattern);
    }
}
