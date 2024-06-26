package site.xleon.future.ctp.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import site.xleon.future.ctp.core.MyException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
public class CompressUtils {
    /**
     * 将文件或文件夹压缩为tar.gz格式
     *
     * @param srcPath  压缩源路径
     * @param destPath 压缩文件路径
     * @throws IOException exception
     */
    public static void tar(Path srcPath, Path destPath) throws IOException {
        try (
                OutputStream output = new BufferedOutputStream(Files.newOutputStream(destPath));
                GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(output);
                TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzOut)
        ) {
            compress(srcPath.toFile(), tarOut, null);
        }
    }

    private static void compress(File file, TarArchiveOutputStream tarOut, String basePath) throws IOException {
        // 文件
        if (file.isFile()) {
            String entryName = file.getName();
            if (basePath != null && !basePath.isEmpty()) {
                entryName = Paths.get(basePath, file.getName()).toString();
            }
            TarArchiveEntry entry = new TarArchiveEntry(file, entryName);
            tarOut.putArchiveEntry(entry);
            try (
                    BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))
            ) {
                byte[] buffer = new byte[8096];
                int n;
                while ((n = inputStream.read(buffer)) != -1) {
                    tarOut.write(buffer, 0, n);
                }
                tarOut.closeArchiveEntry();
            }
            return;
        }

        // 目录
        File[] listFiles = file.listFiles();
        if (listFiles != null && listFiles.length > 0) {
            for (File subFile : listFiles) {
                String base = file.getName();
                if (basePath != null) {
                    base = Paths.get(basePath, file.getName()).toString();
                }
                compress(subFile, tarOut, base);
            }
        } else {
            String entryName = Paths.get(basePath, file.getName()).toString();
            tarOut.putArchiveEntry(new TarArchiveEntry(entryName + File.separator));
            tarOut.closeArchiveEntry();
        }
    }

    /**
     * 解压tar.gz文件
     * @param sourcePath 源文件
     * @param targetDir 目标目录
     * @throws IOException exception
     */
    public static void uncompress(Path sourcePath, Path targetDir) throws IOException, MyException {
        try (
                BufferedInputStream inputStream = IOUtils.buffer(Files.newInputStream(sourcePath));
                GzipCompressorInputStream gzInputStream = new GzipCompressorInputStream(inputStream);
                ArchiveInputStream archiveInput = new TarArchiveInputStream(gzInputStream)
        ) {
            ArchiveEntry entry;
            int count = 0;
            while((entry = archiveInput.getNextEntry()) != null) {
                count ++;

                Path subPath = Paths.get(targetDir.toString(), entry.getName());
                if (!subPath.getParent().toFile().isDirectory()) {
                    boolean success = subPath.getParent().toFile().mkdirs();
                    if (!success) {
                        throw new MyException("创建文件夹失败" + subPath.getParent().toString());
                    }
                }
                archiveInput.read();
                try (BufferedOutputStream outputStream = IOUtils.buffer(Files.newOutputStream(subPath,StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING)) ) {
                    IOUtils.copy(archiveInput, outputStream);
                }
            }
            log.info("entry total: {}", count );
        }
    }
}
