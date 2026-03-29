package com.akergez.superreader2000.shared.formats.fb2

import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.backend.previewGenerators.IPreviewGenerator
import com.akergez.superreader2000.shared.backend.previewGenerators.Image
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory

actual class Fb2PreviewGenerator : IPreviewGenerator {
    override suspend fun generatePreview(filePath: FileWrapper): Image? {
        return try {
            val doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(filePath.file)

            // Find coverpage image id: <coverpage><image l:href="#id"/></coverpage>
            val coverpages = doc.getElementsByTagName("coverpage")
            var coverId: String? = null
            if (coverpages.length > 0) {
                val images = coverpages.item(0).childNodes
                for (i in 0 until images.length) {
                    val node = images.item(i)
                    val href = node.attributes?.getNamedItem("l:href")?.nodeValue
                        ?: node.attributes?.getNamedItem("xlink:href")?.nodeValue
                    if (href != null && href.startsWith("#")) {
                        coverId = href.substring(1)
                        break
                    }
                }
            }

            // Find binary element with matching id
            val binaries = doc.getElementsByTagName("binary")
            for (i in 0 until binaries.length) {
                val node = binaries.item(i)
                val id = node.attributes?.getNamedItem("id")?.nodeValue
                val contentType = node.attributes?.getNamedItem("content-type")?.nodeValue
                if ((coverId == null || id == coverId) && contentType?.startsWith("image/") == true) {
                    val base64 = node.textContent?.trim() ?: continue
                    val bytes = Base64.getMimeDecoder().decode(base64)
                    val bufferedImage = ImageIO.read(ByteArrayInputStream(bytes)) ?: continue
                    return Image(bufferedImage)
                }
            }
            null
        } catch (e: Exception) {
            println(">>> Fb2PreviewGenerator error: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
