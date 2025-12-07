package com.dataliquid.passwordsuite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileIOServiceTest {

    private final FileIOService service = new FileIOService();

    @Test
    void shouldLoadFileContent(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.yaml").toFile();
        String content = "host: localhost\nport: 5432";
        Files.writeString(file.toPath(), content);

        String loaded = service.loadFile(file);

        assertThat(loaded).isEqualTo(content);
    }

    @Test
    void shouldSaveFileContent(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("output.yaml").toFile();
        String content = "host: localhost\nport: 5432";

        service.saveFile(file, content);

        assertThat(file).exists();
        String saved = Files.readString(file.toPath());
        assertThat(saved).isEqualTo(content);
    }

    @Test
    void shouldCreateParentDirectories(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("subdir/nested/test.yaml").toFile();
        String content = "test content";

        service.saveFile(file, content);

        assertThat(file).exists();
        assertThat(file.getParentFile()).exists();
    }

    @Test
    void shouldThrowExceptionForNullFile() {
        assertThatThrownBy(() -> service.loadFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");

        assertThatThrownBy(() -> service.saveFile(null, "content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void shouldThrowExceptionForNonExistentFile(@TempDir Path tempDir) {
        File file = tempDir.resolve("nonexistent.yaml").toFile();

        assertThatThrownBy(() -> service.loadFile(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void shouldThrowExceptionForDirectory(@TempDir Path tempDir) throws IOException {
        File dir = tempDir.resolve("testdir").toFile();
        dir.mkdirs();

        assertThatThrownBy(() -> service.loadFile(dir))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("not a file");
    }

    @Test
    void shouldThrowExceptionForNullContent(@TempDir Path tempDir) {
        File file = tempDir.resolve("test.yaml").toFile();

        assertThatThrownBy(() -> service.saveFile(file, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void shouldHandleEmptyContent(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("empty.txt").toFile();

        service.saveFile(file, "");

        assertThat(file).exists();
        String content = Files.readString(file.toPath());
        assertThat(content).isEmpty();
    }

    @Test
    void shouldHandleUnicodeContent(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("unicode.txt").toFile();
        String content = "Hällo Wörld 你好世界 🔒";

        service.saveFile(file, content);
        String loaded = service.loadFile(file);

        assertThat(loaded).isEqualTo(content);
    }
}
