package com.dataliquid.passwordsuite.ui.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JTextArea;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.operation.EditValueOperation;
import com.dataliquid.passwordsuite.service.CryptoService;
import com.dataliquid.passwordsuite.service.EditorService;

@ExtendWith(MockitoExtension.class)
class CryptoOperationHandlerTest {

    @Mock
    private CryptoService cryptoService;

    @Mock
    private EditorService editorService;

    @Mock
    private PasswordManager passwordManager;

    @Mock
    private JTextArea editor;

    private CryptoOperationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CryptoOperationHandler(cryptoService, editorService, passwordManager);
    }

    @Test
    void shouldReturnConfiguredStatusFromCryptoService() {
        when(cryptoService.isConfigured()).thenReturn(true);
        assertThat(handler.isConfigured()).isTrue();

        when(cryptoService.isConfigured()).thenReturn(false);
        assertThat(handler.isConfigured()).isFalse();
    }

    @Test
    void shouldProcessEntriesForEncryption() {
        ConfigEntry entry1 = new ConfigEntry("key1", "value1");
        ConfigEntry entry2 = new ConfigEntry("key2", "value2");
        entry2.setEncrypted(true); // Already encrypted, should be skipped
        List<ConfigEntry> entries = Arrays.asList(entry1, entry2);

        EditValueOperation mockOp = mock(EditValueOperation.class);
        when(cryptoService.createEncryptOperation(entry1)).thenReturn(mockOp);

        AtomicBoolean refreshCalled = new AtomicBoolean(false);

        int count = handler.processEntries(entries, true, editor, () -> refreshCalled.set(true));

        assertThat(count).isEqualTo(1); // Only entry1 should be processed
        verify(cryptoService).createEncryptOperation(entry1);
        verify(cryptoService, never()).createEncryptOperation(entry2);
        verify(editorService).executeOperation(mockOp);
        assertThat(refreshCalled.get()).isTrue();
    }

    @Test
    void shouldProcessEntriesForDecryption() {
        ConfigEntry entry1 = new ConfigEntry("key1", "ENC[encrypted]");
        entry1.setEncrypted(true);
        ConfigEntry entry2 = new ConfigEntry("key2", "plaintext"); // Not encrypted, should be skipped
        List<ConfigEntry> entries = Arrays.asList(entry1, entry2);

        EditValueOperation mockOp = mock(EditValueOperation.class);
        when(cryptoService.createDecryptOperation(entry1)).thenReturn(mockOp);

        AtomicBoolean refreshCalled = new AtomicBoolean(false);

        int count = handler.processEntries(entries, false, editor, () -> refreshCalled.set(true));

        assertThat(count).isEqualTo(1); // Only entry1 should be processed
        verify(cryptoService).createDecryptOperation(entry1);
        verify(cryptoService, never()).createDecryptOperation(entry2);
        verify(editorService).executeOperation(mockOp);
        assertThat(refreshCalled.get()).isTrue();
    }

    @Test
    void shouldReturnZeroForEmptyList() {
        int count = handler.processEntries(Collections.emptyList(), true, editor, () -> {
        });
        assertThat(count).isZero();
    }

    @Test
    void shouldToggleEncryptionFromPlainToEncrypted() {
        ConfigEntry entry = new ConfigEntry("key", "plaintext");
        entry.setEncrypted(false);

        EditValueOperation mockOp = mock(EditValueOperation.class);
        when(cryptoService.createEncryptOperation(entry)).thenReturn(mockOp);

        AtomicBoolean refreshCalled = new AtomicBoolean(false);

        boolean encrypted = handler.toggleEncryption(entry, editor, () -> refreshCalled.set(true));

        assertThat(encrypted).isTrue(); // Should return true for encryption
        verify(cryptoService).createEncryptOperation(entry);
        verify(editorService).executeOperation(mockOp);
        assertThat(refreshCalled.get()).isTrue();
    }

    @Test
    void shouldToggleEncryptionFromEncryptedToPlain() {
        ConfigEntry entry = new ConfigEntry("key", "ENC[encrypted]");
        entry.setEncrypted(true);

        EditValueOperation mockOp = mock(EditValueOperation.class);
        when(cryptoService.createDecryptOperation(entry)).thenReturn(mockOp);

        AtomicBoolean refreshCalled = new AtomicBoolean(false);

        boolean encrypted = handler.toggleEncryption(entry, editor, () -> refreshCalled.set(true));

        assertThat(encrypted).isFalse(); // Should return false for decryption
        verify(cryptoService).createDecryptOperation(entry);
        verify(editorService).executeOperation(mockOp);
        assertThat(refreshCalled.get()).isTrue();
    }

    @Test
    void shouldHandleNullRefreshCallback() {
        ConfigEntry entry = new ConfigEntry("key", "value");
        EditValueOperation mockOp = mock(EditValueOperation.class);
        when(cryptoService.createEncryptOperation(entry)).thenReturn(mockOp);

        // Should not throw
        int count = handler.processEntries(Collections.singletonList(entry), true, editor, null);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldUpdateEditorForEachProcessedEntry() {
        ConfigEntry entry1 = new ConfigEntry("key1", "value1");
        ConfigEntry entry2 = new ConfigEntry("key2", "value2");
        List<ConfigEntry> entries = Arrays.asList(entry1, entry2);

        EditValueOperation mockOp1 = mock(EditValueOperation.class);
        EditValueOperation mockOp2 = mock(EditValueOperation.class);
        when(cryptoService.createEncryptOperation(entry1)).thenReturn(mockOp1);
        when(cryptoService.createEncryptOperation(entry2)).thenReturn(mockOp2);

        handler.processEntries(entries, true, editor, () -> {
        });

        verify(editorService, times(2)).replaceValueInEditor(any(ConfigEntry.class), any(), any(), eq(editor));
    }
}
