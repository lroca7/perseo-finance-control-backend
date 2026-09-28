package com.perseo.finance.pdf

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class PdfTextExtractor {

    /**
     * Extrae todo el texto plano de un PDF. sortByPosition = false (el
     * default) preserva el orden en que el contenido fue dibujado en el PDF,
     * que suele seguir mejor la lectura natural que reordenar por posición
     * X/Y — en extractos con cajas de varias columnas a la misma altura
     * (ej. "Cupo disponible" y "Cupo disponible para avances" lado a lado),
     * sortByPosition=true puede intercalar el texto de ambas cajas y romper
     * la cercanía "etiqueta -> valor" que buscan las expresiones regulares.
     */
    fun extractText(inputStream: InputStream): String {
        Loader.loadPDF(inputStream.readBytes()).use { document ->
            val stripper = PDFTextStripper()
            stripper.sortByPosition = false
            val raw = stripper.getText(document)
            // Colapsa TODO tipo de espacio (incluye espacios "no-break" \u00A0 y
            // separadores Unicode \p{Zs}, que PDFBox a veces usa en vez del
            // espacio normal y que \s de Kotlin/Java NO detecta por defecto).
            // Sin esto, expresiones regulares que dependen de "etiqueta + espacio
            // + valor" pueden fallar en silencio en ciertos PDFs.
            return raw.replace(Regex("""[\s\u00A0\p{Zs}]+"""), " ").trim()
        }
    }
}
