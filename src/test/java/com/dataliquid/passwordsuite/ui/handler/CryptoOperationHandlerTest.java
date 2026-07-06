package com.dataliquid.passwordsuite.ui.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JTextArea;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.operation.EditValueOperation;
import com.dataliquid.passwordsuite.environment.EnvironmentManager;
import com.dataliquid.passwordsuite.service.CryptoService;
import com.dataliquid.passwordsuite.service.EditorService;

@ExtendWith(MockitoExtension.class)
class CryptoOperationHandlerTest {

    @Mock
    private CryptoService cryptoService;

    @Mock
    private EditorService editorService;

    @Mock
    private EnvironmentManager environmentManager;

    @Mock
    private JTextArea editor;

    private CryptoOperationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CryptoOperationHandler(cryptoService, editorService, environmentManager);
    }

    @Test
    void shouldReturnConfiguredStatusFromCryptoService() {
        when(cryptoService.isConfigured()).thenReturn(true);
        assertThat(handler.isConfigured()).isTrue();

        when(cryptoService.isConfigured()).thenReturn(false);
        assertThat(handler.isConfigured()).isFalse();
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
        boolean encrypted = handler.toggleEncryption(entry, editor, null);

        assertThat(encrypted).isTrue();
    }

    @Test
    void shouldUpdateEditorForToggledEntry() {
        ConfigEntry entry = new ConfigEntry("key", "value");
        EditValueOperation mockOp = mock(EditValueOperation.class);
        when(cryptoService.createEncryptOperation(entry)).thenReturn(mockOp);

        handler.toggleEncryption(entry, editor, () -> {
        });

        verify(editorService).replaceValueInContent(any(ConfigEntry.class), any(), any(), any());
    }
}
