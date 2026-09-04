package com.lumen.researchenglish.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderSelectionPanelTest {
    @Test
    fun upperSelectionDocksPanelAtBottom() {
        assertEquals(
            690f,
            selectionPanelDockY(
                containerHeightPx = 1_000f,
                panelHeightPx = 300f,
                selectionTopPx = 100f,
                selectionBottomPx = 140f,
                marginPx = 10f,
            ),
        )
    }

    @Test
    fun lowerSelectionDocksPanelAtTop() {
        assertEquals(
            10f,
            selectionPanelDockY(
                containerHeightPx = 1_000f,
                panelHeightPx = 300f,
                selectionTopPx = 700f,
                selectionBottomPx = 740f,
                marginPx = 10f,
            ),
        )
    }

    @Test
    fun oversizedPanelStaysInsideTopMargin() {
        assertEquals(
            10f,
            selectionPanelDockY(
                containerHeightPx = 500f,
                panelHeightPx = 600f,
                selectionTopPx = 40f,
                selectionBottomPx = 60f,
                marginPx = 10f,
            ),
        )
    }
}
