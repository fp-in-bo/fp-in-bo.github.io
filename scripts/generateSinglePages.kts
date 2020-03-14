#!/bin/bash

//usr/bin/env echo '
/**** BOOTSTRAP kscript ****\'>/dev/null
command -v kscript >/dev/null 2>&1 || curl -L "https://git.io/fpF1K" | bash 1>&2
exec kscript $0 "$@"
\*** IMPORTANT: Any code including imports and annotations must come after this line ***/
//DEPS com.fasterxml.jackson.module:jackson-module-kotlin:2.10.1 org.apache.commons:commons-lang3:3.9

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.lang3.StringEscapeUtils.escapeHtml4
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.net.URL
import java.net.URLEncoder

val template = FileInputStream(args[0])

val text = URL("https://fp-in-bo.github.io/data/events/all.json").readText()
val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())
val validEvents: List<EventModel> = mapper.readValue(text)

val templateText = template.bufferedReader().readText()

validEvents.map {
    val content = templateText
        .injectTitle(it.title)
        .injectVideo(it.videoUrl)
        .injectDescription(it.description)
    Triple(
        it.id,
        content,
        it.title)
}.forEach {
    val fileName = "${it.first}.html"
    val fileWriter = FileWriter(fileName)
    fileWriter.write(it.second)
    fileWriter.close()
    println("Wrote ${it.third} into $fileName")
}

data class EventModel(
    @JsonProperty(required = true) val id: Int,
    @JsonProperty(required = true) val title: String,
    @JsonProperty(required = true) val speaker: String,
    @JsonProperty(required = true) val imageUrl: String,
    @JsonProperty(required = true) val description: String,
    val videoUrl: String?
)

fun String.injectTitle(title: String) = replace("%TITLE%", escapeHtml4(title))
fun String.injectVideo(videoUrl: String?): String {
    val videoId = videoUrl?.split("v=")?.lastOrNull()
    val videoValue = if (videoId.isNullOrBlank()) {
        ""
    } else {
        """<iframe width="560" height="315" src="https://www.youtube.com/embed/$videoId" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>"""
    }
    return replace("%VIDEO%", videoValue)
}

fun String.injectDescription(description: String) = replace("%DESCRIPTION%", escapeHtml4(description))