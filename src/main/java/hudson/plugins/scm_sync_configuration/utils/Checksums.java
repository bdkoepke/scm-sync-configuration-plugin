package hudson.plugins.scm_sync_configuration.utils;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author fcamblor
 * Utility class allowing to provide easy access to jenkins files checksums
 */
public class Checksums {
    private static byte[] readAllBytes(File f) throws IOException {
	    RandomAccessFile r = new RandomAccessFile(f, "r");
	    byte[] b = new byte[(int)r.length()];
	    r.readFully(b);
	    return b;
    }

    public static boolean fileAndByteArrayContentAreEqual(File file, byte[] content, HashFunction f) throws IOException {
	return file.exists() ?
		f.hashBytes(readAllBytes(file)).equals(f.hashBytes(content)) :
		content == null || content.length == 0;
    }

    public static boolean fileAndByteArrayContentAreEqual(File file, byte[] content) throws IOException {
		return fileAndByteArrayContentAreEqual(file, content, Hashing.crc32());
    }
}