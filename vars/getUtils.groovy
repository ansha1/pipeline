import com.nextiva.JavaUtils
import com.nextiva.JsUtils
import com.nextiva.PythonUtils

def call(String language, String pathToSrc='.') {

    switch (language) {
        case 'java':
            utils = new JavaUtils()
            break
        case 'python':
            utils = new PythonUtils()
            break
        case 'js':
            utils = new JsUtils()
            break
        default:
            error("""Incorrect programming language
                                        please set one of the
                                        supported languages:
                                        java
                                        python
                                        js""")
            break
    }
    utils.pathToSrc = pathToSrc
    return utils
}