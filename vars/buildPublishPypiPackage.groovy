import com.nextiva.PythonUtils
import static com.nextiva.SharedJobsStaticVars.*


def call(String extraPath='.', String deployEnvironment='dev', String pythonVersion='python3.6') {
    PythonUtils pythonUtils = new PythonUtils()
    
    pythonUtils.createVirtualEnv(pythonVersion)
    pythonUtils.venvSh("""
        pip install -U wheel
        cd ${extraPath}
        python setup.py sdist bdist bdist_egg bdist_wheel
        twine upload --config-file /etc/nexus_pypi_config_${deployEnvironment} dist/*
    """)
}
