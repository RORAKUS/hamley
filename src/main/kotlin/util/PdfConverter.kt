package codes.rorak.hamley.util

import codes.rorak.hamley.util.Config.config
import khttp.post
import khttp.get
import khttp.structures.files.FileLike
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream

object PdfConverter {
    fun convert(pdf: File, name: String = pdf.nameWithoutExtension): List<File>? {
        val token = auth();
        val (server, task) = start(token);
        val serverFilename = upload(token, server, task, pdf);
        val isZip = process(token, server, task, serverFilename, pdf.name) ?: return null;
        val jpg = download(token, server, task, name);

        if (!isZip) return listOf(jpg);

        // unzip
        val jpgs = unzip(jpg);
        jpg.delete();
        return jpgs;
    }

    private fun auth() =
        post(
            "https://api.ilovepdf.com/v1/auth",
            json = mapOf("public_key" to config.reporting.pdfPK)
        ).jsonObject.getString("token");

    private fun start(token: String): Pair<String, String> {
        val response = get(
            "https://api.ilovepdf.com/v1/start/pdfjpg",
            headers = mapOf("Authorization" to "Bearer $token")
        ).jsonObject;
        return response.getString("server") to response.getString("task");
    }

    private fun upload(token: String, server: String, task: String, file: File): String {
        val response = post(
            "https://$server/v1/upload",
            headers = mapOf("Authorization" to "Bearer $token"),
            files = listOf(
                FileLike("file", file.name, file.readBytes())
            ),
            data = mapOf(
                "task" to task
            )
        );
        return response.jsonObject.getString("server_filename");
    }

    private fun process(token: String, server: String, task: String, serverFilename: String, originalFilename: String): Boolean? {
        @Suppress("VulnerableCodeUsages")
        val response = post(
            "https://$server/v1/process",
            headers = mapOf("Authorization" to "Bearer $token"),
            json = JSONObject()
                .put("task", task)
                .put("tool", "pdfjpg")
                .put("files", listOf(mapOf("server_filename" to serverFilename, "filename" to originalFilename)))
        ).jsonObject;
        if (response.getString("status") != "TaskSuccess") {
            warn("Converting to JPG failed... Error status: ${response.getString("status")}. Sending raw PDF...")
            return null;
        }
        return response.getString("download_filename").endsWith(".zip");
    }
    private fun download(token: String, server: String, task: String, name: String): File {
        val response = get(
            "https://$server/v1/download/$task",
            headers = mapOf("Authorization" to "Bearer $token")
        );
        val file = File(Config.ATTACHMENT_FOLDER, "$name.jpg");
        file.writeBytes(response.content);
        return file;
    }
    private fun unzip(file: File): List<File> {
        val files = mutableListOf<File>();

        val zis = ZipInputStream(file.inputStream());
        var zipEntry = zis.nextEntry;

        while (zipEntry != null) {
            if (zipEntry.isDirectory) continue;

            val newFile = File(Config.ATTACHMENT_FOLDER, "${file.nameWithoutExtension}-${files.size}.jpg");
            val data = zis.readAllBytes();
            newFile.writeBytes(data);
            files.add(newFile);

            zipEntry = zis.nextEntry;
        }


        zis.closeEntry();
        zis.close();

        return files;
    }
}