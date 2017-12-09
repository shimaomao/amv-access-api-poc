package org.amv.access.util;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MoreHexTest {
    @Test
    public void isHex() throws Exception {
        assertThat(MoreHex.isHex(""), is(true));
        assertThat(MoreHex.isHex("abcdefABCDEF0123456789"), is(true));
    }

    @Test
    public void isNotHex() throws Exception {
        assertThat(MoreHex.isHex(null), is(false));
        assertThat(MoreHex.isHex("g"), is(false));
    }

}