package com.streamflex.core.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

object HtmlParser {

    fun parse(html: String): Document {
        return Jsoup.parse(html)
    }

    fun parse(html: String, baseUrl: String): Document {
        return Jsoup.parse(html, baseUrl)
    }

    fun select(document: Document, css: String): Elements {
        return document.select(css)
    }

    fun selectFirst(document: Document, css: String): Element? {
        return document.selectFirst(css)
    }

    fun text(element: Element?): String {
        return element?.text()?.trim().orEmpty()
    }

    fun attr(element: Element?, attribute: String): String {
        return element?.attr(attribute)?.trim().orEmpty()
    }

    fun html(element: Element?): String {
        return element?.html().orEmpty()
    }
}