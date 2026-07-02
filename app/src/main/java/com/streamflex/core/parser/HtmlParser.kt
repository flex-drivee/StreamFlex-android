package com.streamflex.core.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Central HTML parsing utility for StreamFlex.
 *
 * All providers should use this instead of interacting with Jsoup directly.
 */
object HtmlParser {

    /**
     * Parse raw HTML into a Jsoup Document.
     */
    fun parse(html: String): Document {
        return Jsoup.parse(html)
    }

    /**
     * Parse HTML from a URL.
     */
    fun parse(html: String, baseUrl: String): Document {
        return Jsoup.parse(html, baseUrl)
    }

    /**
     * Returns the first matching element.
     */
    fun selectFirst(
        document: Document,
        selector: String
    ): Element? {
        return document.selectFirst(selector)
    }

    /**
     * Returns all matching elements.
     */
    fun select(
        document: Document,
        selector: String
    ): List<Element> {
        return document.select(selector).toList()
    }

    /**
     * Safely returns element text.
     */
    fun text(element: Element?): String {
        return element?.text()?.trim().orEmpty()
    }

    /**
     * Safely returns HTML.
     */
    fun html(element: Element?): String {
        return element?.html().orEmpty()
    }

    /**
     * Safely returns outer HTML.
     */
    fun outerHtml(element: Element?): String {
        return element?.outerHtml().orEmpty()
    }

    /**
     * Safely returns an attribute.
     */
    fun attr(
        element: Element?,
        attribute: String
    ): String {
        return element?.attr(attribute)?.trim().orEmpty()
    }

    /**
     * Returns absolute URL of an attribute.
     */
    fun absUrl(
        element: Element?,
        attribute: String
    ): String {
        return element?.absUrl(attribute)?.trim().orEmpty()
    }

    /**
     * Returns true if selector exists.
     */
    fun exists(
        document: Document,
        selector: String
    ): Boolean {
        return document.selectFirst(selector) != null
    }

    /**
     * Returns page title.
     */
    fun title(document: Document): String {
        return document.title()
    }

    /**
     * Returns page body text.
     */
    fun body(document: Document): String {
        return document.body().text()
    }
}