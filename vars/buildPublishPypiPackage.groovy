import static com.nextiva.SharedJobsStaticVars.*


def call(String extraPath='.', String deployEnvironment='test', String pythonName='python') {
    pythonUtils.createVirtualEnv(pythonName)
    pythonUtils.venvSh("""
        pip install -U wheel
        cd ${extraPath}
        python setup.py sdist bdist bdist_egg bdist_wheel
        twine upload --config-file /etc/nexus_pypi_config_${deployEnvironment} dist/*
    """)
}
