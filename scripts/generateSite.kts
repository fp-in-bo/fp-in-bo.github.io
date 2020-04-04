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
import java.io.FileInputStream
import java.io.FileWriter
import java.net.URL

val templateSimplePage: FileInputStream = FileInputStream(args[0])
val templateIndex: FileInputStream = FileInputStream(args[1])

val data = URL("https://fpinbo.dev/data/events/all.json").readText()
val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())
val events: List<EventModel> = mapper.readValue(data)

generateSinglePages(events, templateSimplePage)
generateIndex(events, templateIndex)

fun generateSinglePages(events: List<EventModel>, templateFile: FileInputStream) {

    val template = templateFile.bufferedReader().readText()

    events.map {
        val content = template
            .injectSinglePageTitle(it.title)
            .injectSinglePageVideo(it.videoUrl)
            .injectSinglePageDescription(it.description)
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
}

fun generateIndex(events: List<EventModel>, templateFile: FileInputStream) {
    val template = templateFile.bufferedReader().readText()

    val top = events.take(3)
    val others = events.drop(3)

    val templateWithTopResolved = top.foldIndexed(template) { index, currentTemplate, event ->
        currentTemplate.injectTopTitle(index, event.title)
            .injectTopLink(index, event.id)
            .injectTopVideo(index, event.videoUrl)
    }

    val indexBody = generateOtherEventsInIndex(templateWithTopResolved, others)

    FileWriter("index.html").apply {
        write(indexBody)
        close()
    }
    println("Wrote index.html")
}

fun generateOtherEventsInIndex(templateWithTopResolved: String, others: List<EventModel>): String {
    val forEachStart = "%FOR_EACH_OTHER_EVENT%{"
    val forEachEnd = "}"
    val startIndexForEach = templateWithTopResolved.indexOf(forEachStart)
    val endIndexForEach = templateWithTopResolved.indexOf(string = forEachEnd, startIndex = startIndexForEach)

    val forEachBody = templateWithTopResolved.substring(startIndexForEach..endIndexForEach)
        .replace(forEachStart, "")
        .replace(forEachEnd, "")

    val forEachResolved = others.joinToString(separator = "") {
        forEachBody
            .replace("%IT.TITLE%", it.title)
            .replace("%IT.LINK%", it.id.toString() + ".html")
    }

    return templateWithTopResolved.replaceRange(startIndexForEach..endIndexForEach, forEachResolved)
}

data class EventModel(
    @JsonProperty(required = true) val id: Int,
    @JsonProperty(required = true) val title: String,
    @JsonProperty(required = true) val speaker: String,
    @JsonProperty(required = true) val imageUrl: String,
    @JsonProperty(required = true) val description: String,
    val videoUrl: String?
)

fun String.injectTopTitle(index: Int, title: String) = replaceEscaping("%TITLE_${index + 1}%", title)
fun String.injectTopLink(index: Int, id: Int) = replaceEscaping("%LINK_${index + 1}%", "$id.html")
fun String.injectTopVideo(index: Int, videoUrl: String?): String {
    val videoId = videoUrl.findVideoId()
    val videoValue = if (videoId.isNullOrBlank()) {
        ""
    } else {
        """<iframe width="480" height="370" src="https://www.youtube.com/embed/$videoId" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>"""
    }
    return replace("%VIDEO_${index + 1}%", videoValue)
}

fun String.injectSinglePageTitle(title: String) = replaceEscaping("%TITLE%", title)
fun String.injectSinglePageVideo(videoUrl: String?): String {
    val videoId = videoUrl.findVideoId()
    val videoValue = if (videoId.isNullOrBlank()) {
        ""
    } else {
        """<iframe width="560" height="315" src="https://www.youtube.com/embed/$videoId" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>"""
    }
    return replace("%VIDEO%", videoValue)
}

fun String.injectSinglePageDescription(description: String) = replaceEscaping("%DESCRIPTION%", description)

fun String.replaceEscaping(oldValue: String, newValue: String) = replace(oldValue, escapeHtml4(newValue))

fun String?.findVideoId() = this?.split("v=")?.lastOrNull()

