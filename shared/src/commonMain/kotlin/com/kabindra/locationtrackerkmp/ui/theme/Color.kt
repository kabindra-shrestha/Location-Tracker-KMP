package com.kabindra.locationtrackerkmp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Palette derived from #A11E62
val primaryLight = Color(0xFF7F0049)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFA11E62)
val onPrimaryContainerLight = Color(0xFFFFB9D1)
val secondaryLight = Color(0xFF8C4964)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFFCA9C8)
val onSecondaryContainerLight = Color(0xFF7A3A54)
val tertiaryLight = Color(0xFF7B1A00)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFA22A07)
val onTertiaryContainerLight = Color(0xFFFFBCAB)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF93000A)
val backgroundLight = Color(0xFFFFF8F8)
val onBackgroundLight = Color(0xFF24181C)
val surfaceLight = Color(0xFFFFF8F8)
val onSurfaceLight = Color(0xFF24181C)
val surfaceVariantLight = Color(0xFFF9DBE3)
val onSurfaceVariantLight = Color(0xFF564148)
val outlineLight = Color(0xFF897178)
val outlineVariantLight = Color(0xFFDCBFC7)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF3A2D31)
val inverseOnSurfaceLight = Color(0xFFFFECF0)
val inversePrimaryLight = Color(0xFFFFB0CD)
val surfaceDimLight = Color(0xFFEAD5DA)
val surfaceBrightLight = Color(0xFFFFF8F8)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFFFF0F3)
val surfaceContainerLight = Color(0xFFFFE8EE)
val surfaceContainerHighLight = Color(0xFFF9E3E8)
val surfaceContainerHighestLight = Color(0xFFF3DDE3)

// Dark Palette derived from #A11E62
val primaryDark = Color(0xFFFFB0CD)
val onPrimaryDark = Color(0xFF640039)
val primaryContainerDark = Color(0xFFA11E62)
val onPrimaryContainerDark = Color(0xFFFFB9D1)
val secondaryDark = Color(0xFFFFB0CD)
val onSecondaryDark = Color(0xFF551C36)
val secondaryContainerDark = Color(0xFF73344F)
val onSecondaryContainerDark = Color(0xFFF3A0BF)
val tertiaryDark = Color(0xFFFFB4A1)
val onTertiaryDark = Color(0xFF611200)
val tertiaryContainerDark = Color(0xFFA22A07)
val onTertiaryContainerDark = Color(0xFFFFBCAB)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF1B1014)
val onBackgroundDark = Color(0xFFF3DDE3)
val surfaceDark = Color(0xFF1B1014)
val onSurfaceDark = Color(0xFFF3DDE3)
val surfaceVariantDark = Color(0xFF564148)
val onSurfaceVariantDark = Color(0xFFDCBFC7)
val outlineDark = Color(0xFFA48A92)
val outlineVariantDark = Color(0xFF564148)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFF3DDE3)
val inverseOnSurfaceDark = Color(0xFF3A2D31)
val inversePrimaryDark = Color(0xFFAB286A)
val surfaceDimDark = Color(0xFF1B1014)
val surfaceBrightDark = Color(0xFF43363A)
val surfaceContainerLowestDark = Color(0xFF160B0F)
val surfaceContainerLowDark = Color(0xFF24181C)
val surfaceContainerDark = Color(0xFF281C20)
val surfaceContainerHighDark = Color(0xFF33272B)
val surfaceContainerHighestDark = Color(0xFF3F3135)

val transparent = Color.Transparent
val unspecified = Color.Unspecified
val overlay = Color(0xFF000000)
val drawerBackground = Color(0xFF1F2937)
val cardBorder = Color(0xFF374151)
val imageBorder = Color(0xFF374151)
val divider = Color(0xFFD8D8D8)

// Functional Colors
val iconColorPrimary = primaryLight
val inputFieldDefault = outlineLight
val inputFieldActive = primaryLight
val inputFieldError = errorLight
val inputFieldTextDefault = onSurfaceVariantLight
val inputFieldTextError = errorLight
val inputFieldLabelDefault = onSurfaceVariantLight
val inputFieldLabelError = errorLight
val carouselSelected = primaryLight
val carouselUnselected = outlineVariantLight
val drawerBackgroundSelected = primaryLight
val drawerBackgroundUnselected = transparent
val drawerTextSelected = onPrimaryLight
val drawerTextUnselected = primaryLight
val tabSelected = primaryLight
val tabUnselected = outlineLight
val textSelected = onPrimaryLight
val textUnselected = onSurfaceVariantLight

// Custom composable colors
val customLight = Color(0xFFFFD9E2)
val customDark = Color(0xFF820047)

@Composable
fun custom() = if (isSystemInDarkTheme()) customDark else customLight
