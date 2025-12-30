package com.alflabs.milepost.op

import com.alflabs.milepost.main.GlobalState
import com.alflabs.rx.IPublisher
import com.alflabs.rx.Publishers
import com.alflabs.utils.ILogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Operator @Inject constructor(
    val logger: ILogger,
    val globalState: GlobalState
) {
    companion object {
        private val TAG = Operator::class.java.simpleName

        var operatorMode: OperatorMode = OperatorMode.IDLE
            private set
    }

    private val operatorModePublisher: IPublisher<OperatorMode?> = Publishers.latest()

    init {
        globalState.operatorModeStream.publishWith(operatorModePublisher)
    }

    fun changeMode(newMode: OperatorMode) {
        operatorMode = newMode
        operatorModePublisher.publish(operatorMode)
    }

}
