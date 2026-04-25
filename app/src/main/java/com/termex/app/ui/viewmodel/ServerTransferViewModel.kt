package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.termex.app.core.transfer.TermexArchiveExportResult
import com.termex.app.core.transfer.TermexArchiveImportResult
import com.termex.app.core.transfer.TermexArchiveTransfer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ServerTransferViewModel @Inject constructor(
    private val archiveTransfer: TermexArchiveTransfer
) : ViewModel() {

    suspend fun exportArchive(password: String): TermexArchiveExportResult {
        return archiveTransfer.exportArchive(password)
    }

    suspend fun importArchive(bytes: ByteArray, password: String): TermexArchiveImportResult {
        return archiveTransfer.importArchive(bytes, password)
    }
}
