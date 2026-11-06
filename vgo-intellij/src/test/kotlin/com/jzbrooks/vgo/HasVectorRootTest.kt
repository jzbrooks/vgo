package com.jzbrooks.vgo

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class HasVectorRootTest {
    private fun hasVectorRoot(xml: String) = hasVectorRoot(xml.byteInputStream())

    @Test
    fun acceptsVectorRoot() {
        assertThat(hasVectorRoot("""<vector xmlns:android="http://schemas.android.com/apk/res/android"/>""")).isTrue()
    }

    @Test
    fun skipsPrologBeforeRoot() {
        val xml = """<?xml version="1.0" encoding="utf-8"?><!-- <shape/> --><!DOCTYPE vector><vector/>"""
        assertThat(hasVectorRoot(xml)).isTrue()
    }

    @Test
    fun ignoresBrokenContentAfterRoot() {
        assertThat(hasVectorRoot("<vector><path>")).isTrue()
    }

    @Test
    fun rejectsOtherRoots() {
        assertThat(hasVectorRoot("<shape><vector/></shape>")).isFalse()
    }

    @Test
    fun rejectsPrefixedRoot() {
        assertThat(hasVectorRoot("""<a:vector xmlns:a="urn:a"/>""")).isFalse()
    }

    @Test
    fun rejectsMalformedAndEmptyInput() {
        assertThat(hasVectorRoot("not xml")).isFalse()
        assertThat(hasVectorRoot("")).isFalse()
    }

    @Test
    fun toleratesDoctypeWithExternalEntity() {
        val xml = """<!DOCTYPE vector [<!ENTITY x SYSTEM "file:///etc/passwd">]><vector>&x;</vector>"""
        assertThat(hasVectorRoot(xml)).isTrue()
    }
}
