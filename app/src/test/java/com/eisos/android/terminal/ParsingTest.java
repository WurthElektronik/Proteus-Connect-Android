/*
 * __          ________        _  _____
 * \ \        / /  ____|      (_)/ ____|
 *  \ \  /\  / /| |__      ___ _| (___   ___  ___
 *   \ \/  \/ / |  __|    / _ \ |\___ \ / _ \/ __|
 *    \  /\  /  | |____  |  __/ |____) | (_) \__ \
 *     \/  \/   |______|  \___|_|_____/ \___/|___/
 *
 * Copyright Wuerth Elektronik eiSos 2019
 *
 */

package com.eisos.android.terminal;

import com.eisos.android.terminal.utils.Parser;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ParsingTest {

    @Test
    public void parseHexStringToBinary() {
        String hex = "010800FF";
        byte[] val = new byte[hex.length() / 2];
        for (int i = 0; i < val.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hex.substring(index, index + 2), 16);
            val[i] = (byte) j;
        }
        assertEquals(Arrays.toString(val), Arrays.toString(Parser.parseHexBinary(hex)));
    }

    @Test
    public void parseBytesToHexString() {
        String hex = "010800FF";
        byte[] array = Parser.parseHexBinary(hex);
        assertEquals(hex, Parser.bytesToHex(array));
    }

    @Test
    public void charToByteArray() {
        String s = "0XFF";
        char[] chars = s.toCharArray();
        byte[] bytes = s.getBytes();
        assertEquals(Arrays.toString(bytes), Arrays.toString(Parser.charToByte(chars)));
    }
}
