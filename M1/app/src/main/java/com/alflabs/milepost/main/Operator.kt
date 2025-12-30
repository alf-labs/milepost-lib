package com.alflabs.milepost.main

import com.alflabs.utils.ILogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Operator @Inject constructor(
    val logger: ILogger
) {
    companion object {
        private val TAG = Operator::class.java.simpleName
    }

    var operatorMode: OperatorMode = OperatorMode.IDLE
        private set

    fun changeMode(newMode: OperatorMode) {
        operatorMode = newMode
    }

}
