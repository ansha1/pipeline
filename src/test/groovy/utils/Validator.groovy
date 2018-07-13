package utils

import com.lesfurets.jenkins.unit.MethodCall

import java.util.regex.Pattern

import static org.junit.Assert.assertEquals

/**
 * Common validators
 */
trait Validator implements BasePipelineAccessor {

    /**
     * Checks that the method has been executed the expected number of times
     * @param methodName
     * @param executionCount
     */
    void checkThatMockedMethodWasExecuted(String methodName, int executionCount) {
        List<MethodCall> methodCalls = basePipelineTest.helper.callStack.findAll { call -> methodName == call.methodName }
        assertEquals 'The ' + methodName + 'method has not been executed the expected number of times',
                executionCount, methodCalls.size()
    }

    /**
     * Checks that the method was executed with the expected value of the argument
     * @param methodName name of the method
     * @param expectedValue expected argument value
     * @param executionCount expected number of executions
     * @param parameterIndex index of the argument ot validate
     */
    void checkThatMethodWasExecutedWithValue(String methodName,
                                             Object expectedValue,
                                             int executionCount = 1,
                                             int parameterIndex = 0) {
        List<MethodCall> methodCalls = basePipelineTest.helper.callStack.findAll { method ->
            ((methodName == method.methodName) && basePipelineTest.matchParameter(method.getArgs()[parameterIndex], expectedValue))
        }
        assertEquals('Method ' + methodName + ' was not executed with argument value ' + expectedValue, executionCount, methodCalls.size())

    }

    /**
     * Match expected and actual value.
     * String are compared by pattern
     * @param actual actual argument value
     * @param expected value or pattern
     * @return match result
     */
    boolean matchParameter(Object actual, Object expected) {
        if (actual instanceof GString || actual instanceof String) {
            return Pattern.compile(expected as String, Pattern.DOTALL).matcher(actual as CharSequence).matches()
        } else {
            return actual == expected
        }
    }
}