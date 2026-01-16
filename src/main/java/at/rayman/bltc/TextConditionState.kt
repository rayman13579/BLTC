package at.rayman.bltc

import com.intellij.openapi.components.BaseState

// State serialization to xml is simpler with Kotlin
class TextConditionState : BaseState() {
    var type by string()
    var name by string()
    var targetId by string()
    var triggerCondition by string()
    var triggerText by string()
}