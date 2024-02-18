package com.example.fluxplay;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipWriterState {
    private int fileIndex;
    private final byte[] buffer;
    private final List<Path> files;
    private final ZipOutputStream zos;
    private ZipEntry currentEntry;
    private InputStream currentFis;
    private boolean zosClosed;


    public ZipWriterState(List<Path> files, ZipOutputStream zos) {
        this.files = files;
        this.zos = zos;
        buffer = new byte[8192];
        fileIndex = 0;
        currentEntry = null;
        zosClosed = false;
    }

    public boolean writeNext() throws IOException {
        if ( currentEntry == null ) {
            if ( fileIndex >= files.size() ) {
                if ( zosClosed ) {
                    return false;
                }
                zos.close();
                zosClosed = true;
                return true;
            }
            Path file = files.get(fileIndex++);
            currentEntry = new ZipEntry(file.getFileName().toString());
            BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
            currentEntry.setCreationTime(FileTime.fromMillis(fileAttributes.creationTime().toMillis()));
            currentEntry.setLastAccessTime(FileTime.fromMillis(fileAttributes.lastAccessTime().toMillis()));
            currentEntry.setLastModifiedTime(FileTime.fromMillis(fileAttributes.lastModifiedTime().toMillis()));
            zos.putNextEntry(currentEntry);
            currentFis = Files.newInputStream(file);
        }
        int count = currentFis.read(buffer);
        if ( count < 0) {
            zos.closeEntry();
            currentFis.close();
            currentEntry = null;
            return true;
        }
        zos.write(buffer, 0, count);
        return true;
    }
}
