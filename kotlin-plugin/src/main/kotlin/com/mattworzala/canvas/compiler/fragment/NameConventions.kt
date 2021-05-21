package com.mattworzala.canvas.compiler.fragment

import org.jetbrains.kotlin.name.Name

object NameConventions {
    val FRAGMENT = Name.identifier("fragment")
    val FRAGMENT_PARAMETER = Name.identifier("\$fragment")
    val CHANGED_PARAMETER = Name.identifier("\$changed")
    val STABILITY_FLAG = Name.identifier("\$stable")
    val DEFAULT_PARAMETER = Name.identifier("\$default")
    val JOINKEY = Name.identifier("joinKey")
    val STARTRESTARTGROUP = Name.identifier("startRestartGroup")
    val ENDRESTARTGROUP = Name.identifier("endRestartGroup")
    val UPDATE_SCOPE = Name.identifier("updateScope")
    val SOURCEINFORMATION = "sourceInformation"
    val SOURCEINFORMATIONMARKERSTART = "sourceInformationMarkerStart"
    val SOURCEINFORMATIONMARKEREND = "sourceInformationMarkerEnd"
}