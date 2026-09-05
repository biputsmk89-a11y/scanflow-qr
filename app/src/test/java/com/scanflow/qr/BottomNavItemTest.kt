package com.scanflow.qr

import com.scanflow.qr.core.navigation.BottomNavItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavItemTest {

    @Test
    fun testBottomNavItemItemsNotNull() {
        val items = BottomNavItem.items
        assertEquals("BottomNavItem.items must have exactly 5 tabs", 5, items.size)
        for (item in items) {
            assertNotNull("Item in BottomNavItem.items must not be null!", item)
            assertNotNull("Item route must not be null!", item.route)
            assertNotNull("Item title must not be null!", item.title)
            assertNotNull("Item icon must not be null!", item.icon)
            assertTrue("Route must not be empty", item.route.isNotEmpty())
        }
    }

    @Test
    fun testIndividualBottomNavItems() {
        assertEquals("home_tab", BottomNavItem.Home.route)
        assertEquals("scan_tab", BottomNavItem.Scan.route)
        assertEquals("create_tab", BottomNavItem.Create.route)
        assertEquals("history_tab", BottomNavItem.History.route)
        assertEquals("profile_tab", BottomNavItem.Profile.route)
    }
}
