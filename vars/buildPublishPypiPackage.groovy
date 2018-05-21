import static com.nextiva.SharedJobsStaticVars.*


def call(String extraPath='.', String deployEnvironment='test', String pythonName='python') {
    pythonUtils.createVirtualEnv(pythonName)
    pythonUtils.venvSh("""
        cd ${extraPath}
        pip install -U wheel

        echo 'DEPRECATED: publishing to old repo pypi.nextiva.xyz'
        pip install -U devpi-client
        devpi use http://pypi.nextiva.xyz
        devpi login root --password iampythonian
        devpi use root/dev
        python setup.py sdist bdist bdist_egg bdist_wheel
        devpi upload --from-dir dist

        echo 'Publishing pypi to Nexus'
        python setup.py sdist bdist bdist_egg bdist_wheel
        twine upload --config-file /etc/nexus_pypi_config_${deployEnvironment} dist/*
    """)
}
