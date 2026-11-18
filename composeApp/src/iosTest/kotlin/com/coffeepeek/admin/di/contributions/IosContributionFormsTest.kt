package com.coffeepeek.admin.di.contributions

import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.contributions.impl.*
import kotlinx.serialization.json.*
import kotlin.test.*

class IosContributionFormsTest {
    @Test fun allFourNativeFormRoutesRetainTheirArguments() {
        assertEquals(NativeContributionFormKind.AddShop, Navigator.Screen.AddShop.nativeFormArgs()?.kind)
        assertEquals(NativeContributionFormKind.AddRoaster, Navigator.Screen.AddRoaster.nativeFormArgs()?.kind)
        assertEquals("shop", Navigator.Screen.SuggestShopChange("shop").nativeFormArgs()?.slug)
        val editor = Navigator.Screen.ShopChangeEditor("shop", "Contacts", "request").nativeFormArgs()
        assertEquals(NativeContributionFormKind.Editor, editor?.kind)
        assertEquals("Contacts", editor?.section)
        assertEquals("request", editor?.requestId)
        assertNull(Navigator.Screen.ThemeSettings.nativeFormArgs())
    }

    @Test fun swiftSnapshotPayloadKeepsTypedFieldsMediaAndMapCoordinates() {
        val state = NativeContributionFormState("Editor", 7, 1, false,
            fields = listOf(NativeContributionInput("description", "Text", listOf("DescriptionTooLong"))),
            photos = listOf(NativeContributionPhoto("photo", null, "photo.jpg", false)),
            map = NativeContributionMap(53.9, 27.5, null, null, "Street", false, false, true))
        val json = Json.parseToJsonElement(state.iosJson()).jsonObject
        assertEquals(7, json.getValue("accountVersion").jsonPrimitive.int)
        val field = json.getValue("fields").jsonArray.single().jsonObject
        assertEquals("description", field.getValue("key").jsonPrimitive.content)
        assertEquals("DescriptionTooLong", field.getValue("issues").jsonArray.single().jsonPrimitive.content)
        assertEquals(false, json.getValue("photos").jsonArray.single().jsonObject.getValue("groupedRemoval").jsonPrimitive.boolean)
        assertEquals(53.9, json.getValue("map").jsonObject.getValue("latitude").jsonPrimitive.double)
        assertEquals(JsonNull, json.getValue("map").jsonObject.getValue("cameraLatitude"))
    }
}

