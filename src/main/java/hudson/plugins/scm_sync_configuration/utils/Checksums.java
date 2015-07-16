package hudson.plugins.scm_sync_configuration.utils;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author fcamblor
 *         Utility class allowing to provide easy access to jenkins files checksums
 */
public class Checksums {
    private static byte[] readAllBytes(final File f) throws IOException {
        final RandomAccessFile r = new RandomAccessFile(f, "r");
        final byte[] b = new byte[(int) r.length()];
        r.readFully(b);
        return b;
    }

    private static boolean fileAndByteArrayContentAreEqual(final File file, final byte[] content, final HashFunction f)
            throws IOException {
        return file.exists() ?
                f.hashBytes(readAllBytes(file)).equals(f.hashBytes(content)) :
                content == null || content.length == 0;
    }

    public static boolean fileAndByteArrayContentAreEqual(final File file, final byte[] content)
            throws IOException {
        return fileAndByteArrayContentAreEqual(file, content, Hashing.crc32());
    }
}