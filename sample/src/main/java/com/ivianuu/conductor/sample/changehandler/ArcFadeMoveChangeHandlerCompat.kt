package com.ivianuu.conductor.sample.changehandler

import com.ivianuu.conductor.changehandler.FadeChangeHandler
import com.ivianuu.conductor.changehandler.TransitionChangeHandlerCompat

class ArcFadeMoveChangeHandlerCompat : TransitionChangeHandlerCompat {

    constructor() : super()

    constructor(vararg transitionNames: String) : super(
        ArcFadeMoveChangeHandler(ArrayList(transitionNames.toList())),
        FadeChangeHandler()
    )

}
