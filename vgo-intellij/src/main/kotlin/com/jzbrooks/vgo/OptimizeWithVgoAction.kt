package com.jzbrooks.vgo

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jzbrooks.vgo.Vgo
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.PrintStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException

/**
 * Reads no further than the root element, so this stays cheap enough for action updates. Parsing
 * with the JDK rather than XML PSI spares the plugin a dependency on the IDE's XML support, which
 * moved from the platform into a bundled plugin in 2026.3.
 */
internal fun hasVectorRoot(input: InputStream): Boolean {
    val factory =
        XMLInputFactory.newDefaultFactory().apply {
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
            setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false)
        }

    return try {
        val reader = factory.createXMLStreamReader(input)
        while (reader.hasNext() && reader.next() != XMLStreamConstants.START_ELEMENT) {
            // Skip the prolog
        }
        reader.isStartElement && reader.localName == "vector"
    } catch (_: XMLStreamException) {
        false
    }
}

class OptimizeWithVgoAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && isSupported(file)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val sizeBefore = file.length

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Optimizing with vgo…", false) {
                override fun run(indicator: ProgressIndicator) {
                    val exitCode = runVgoSilently(file.path)

                    ApplicationManager.getApplication().invokeLater {
                        file.refresh(false, false)
                        val sizeAfter = file.length
                        notifyResult(project, file.name, exitCode, sizeBefore, sizeAfter)
                    }
                }
            },
        )
    }

    private fun runVgoSilently(path: String): Int {
        val originalOut = System.out
        val originalErr = System.err
        val sink = PrintStream(ByteArrayOutputStream())
        System.setOut(sink)
        System.setErr(sink)
        return try {
            Vgo(Vgo.Options(input = listOf(path), indent = 2)).run()
        } catch (_: Exception) {
            EXIT_FAILURE
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }

    private fun notifyResult(
        project: Project,
        fileName: String,
        exitCode: Int,
        sizeBefore: Long,
        sizeAfter: Long,
    ) {
        val group = NotificationGroupManager.getInstance().getNotificationGroup("vgo")
        if (exitCode != 0) {
            group
                .createNotification("vgo", "$fileName: optimization failed", NotificationType.ERROR)
                .notify(project)
            return
        }

        val delta = sizeBefore - sizeAfter
        val content =
            if (delta <= 0L) {
                "$fileName: already optimal (${formatBytes(sizeBefore)})"
            } else {
                val percent = (delta.toDouble() / sizeBefore) * 100
                "%s: %s → %s (saved %s, %.1f%%)".format(
                    fileName,
                    formatBytes(sizeBefore),
                    formatBytes(sizeAfter),
                    formatBytes(delta),
                    percent,
                )
            }
        group.createNotification("vgo", content, NotificationType.INFORMATION).notify(project)
    }

    private fun isSupported(file: VirtualFile): Boolean {
        if (file.isDirectory) return false
        return when (file.extension?.lowercase()) {
            "svg" -> {
                true
            }

            "xml" -> {
                try {
                    file.inputStream.use(::hasVectorRoot)
                } catch (_: IOException) {
                    false
                }
            }

            else -> {
                false
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < BYTES_PER_KB) return "$bytes B"
        val kb = bytes / BYTES_PER_KB.toDouble()
        if (kb < BYTES_PER_KB) return "%.1f KB".format(kb)
        return "%.1f MB".format(kb / BYTES_PER_KB)
    }

    private companion object {
        const val BYTES_PER_KB = 1024
        const val EXIT_FAILURE = 1
    }
}
