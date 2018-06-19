package utils

import com.lesfurets.jenkins.unit.MethodCall

import static org.junit.Assert.assertEquals

trait Validator implements BasePipelineAccessor {

    void checkThatMockedMethodWasExecuted(String methodName, int numberOfTimes) {
        List<MethodCall> methodCalls = basePipelineTest.helper.callStack.findAll { call -> methodName == call.methodName }
        assertEquals 'The ' + methodName + 'method has not been executed the expected number of times',
                numberOfTimes, methodCalls.size()
    }

    void validateMockedParameter(String methodName,
                                 int parameterIndex = 0,
                                 int executionOrder = 0,
                                 Object expectedValue) {
        List<MethodCall> methodCalls = basePipelineTest.helper.callStack.findAll { call -> methodName == call.methodName }
        MethodCall methodCall = methodCalls.get executionOrder
        Object actualValue = methodCall.args[0]
        assertEquals 'The value passed to the mocked method is not equals to the expected', expectedValue, actualValue
    }
}