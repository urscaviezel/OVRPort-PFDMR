package moe.crx.overport.pfdmr;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;

/** One desktop process per user workspace, including direct JAR starts. */
public final class DesktopInstance implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;
    private DesktopInstance(FileChannel channel, FileLock lock) { this.channel=channel; this.lock=lock; }
    public static DesktopInstance acquire(Path directory) throws IOException {
        Files.createDirectories(directory);
        FileChannel channel=FileChannel.open(directory.resolve("pfdmr-desktop.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        try {
            FileLock lock=channel.tryLock();
            if(lock!=null)return new DesktopInstance(channel,lock);
        } catch(OverlappingFileLockException busy) {
            // Another desktop entry point in this JVM owns the same workspace.
        } catch(IOException e) { channel.close(); throw e; }
        channel.close();return null;
    }
    public void close() throws IOException {
        try { if(lock.isValid())lock.release(); } finally { channel.close(); }
        // Keep the inode/path stable: deleting it would race with a new owner.
    }
}
